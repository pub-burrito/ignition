package com.github.ignition.support.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

public class IgnitedHttpRequestRetryHandler implements HttpRequestRetryHandler {

    // TODO: make configurable
    private static final int RETRY_SLEEP_TIME_MILLIS = 1500;

    private static HashSet<Class<?>> exceptionWhitelist = new HashSet<Class<?>>();

    private static HashSet<Class<?>> exceptionBlacklist = new HashSet<Class<?>>();

    static {
        // Retry if the server dropped connection on us
        exceptionWhitelist.add(NoHttpResponseException.class);
        // retry-this, since it may happens as part of a Wi-Fi to 3G failover
        exceptionWhitelist.add(UnknownHostException.class);
        // retry-this, since it may happens as part of a Wi-Fi to 3G failover
        exceptionWhitelist.add(SocketException.class);

        // never retry timeouts
        // TODO: this doesn't actually capture all timeouts; I've seen timeouts being thrown as a
        // plain SocketExceptiion
        exceptionBlacklist.add(InterruptedIOException.class);
        // never retry SSL handshake failures
        exceptionBlacklist.add(SSLHandshakeException.class);
    }

    private int maxRetries, timesRetried;

    public IgnitedHttpRequestRetryHandler(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        boolean retry;

        this.timesRetried = executionCount;

        Boolean b = (Boolean) context.getAttribute(ExecutionContext.HTTP_REQ_SENT);
        boolean sent = (b != null && b.booleanValue());

        if (executionCount > maxRetries) {
            // Do not retry if over max retry count
            retry = false;
        } else if (exceptionBlacklist.contains(exception.getClass())) {
            // immediately cancel retry if the error is blacklisted
            retry = false;
        } else if (exceptionWhitelist.contains(exception.getClass())) {
            // immediately retry if error is whitelisted
            retry = true;
        } else if (!sent) {
            // for all other errors, retry only if request hasn't been fully sent yet
            // TODO: refine to resend all idempotent requests
            retry = true;
        } else {
            // otherwise do not retry
            retry = false;
        }

        if (retry) {
            System.err.printf("%s - %s", IgnitedHttp.LOG_TAG, "request failed (" + exception.getClass().getCanonicalName()
                    + ": " + exception.getMessage() + " / attempt " + executionCount
                    + "), will retry in " + RETRY_SLEEP_TIME_MILLIS / 1000.0 + " seconds");
            sleep(RETRY_SLEEP_TIME_MILLIS);
        } else {
        	System.err.printf("%s - %s", IgnitedHttp.LOG_TAG, "request failed after " + executionCount + " attempts");
            exception.printStackTrace();
        }

        return retry;
    }

    /**
     * Waits a given number of milliseconds (of uptimeMillis) before returning.
     * Similar to {@link java.lang.Thread#sleep(long)}, but does not throw
     * {@link InterruptedException}; {@link Thread#interrupt()} events are
     * deferred until the next interruptible operation.  Does not return until
     * at least the specified number of milliseconds has elapsed.
     * 
	 * @param retrySleepTimeMillis Time to sleep
	 * @since TODO
	 */
	private void sleep( long retrySleepTimeMillis )
	{
		long currentTime = new Date().getTime();
		long timeSlept = retrySleepTimeMillis;
		boolean interrupted = false;
		do {
			try
			{
				Thread.sleep( timeSlept );
			}
			catch ( InterruptedException e )
			{	
				interrupted = true;
			}
			
			long newTime = new Date().getTime();
			timeSlept = currentTime + retrySleepTimeMillis - newTime;
		
		//keeping looping while timeSlept is greater than 0 which represents that the total time slept has not been completeled
		} while ( timeSlept > 0 );
		
		if ( interrupted )
		{
			Thread.currentThread().interrupt();
		}
	}

	public int getTimesRetried() {
        return timesRetried;
    }
}
