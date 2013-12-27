/* Copyright (c) 2009-2011 Matthias Kaeppler, ASF
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ignition.support;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.github.ignition.support.http.IgnitedHttp;

// contains code from the Apache Software foundation
public class IgnitedStrings {

    /**
     * Turns a camel case string into an underscored one, e.g. "HelloWorld"
     * becomes "hello_world".
     * 
     * @param camelCaseString
     *        the string to underscore
     * @return the underscored string
     */
    public static String underscore(String camelCaseString) {
        return underscore( camelCaseString, null );
    }

    /**
     * Turns a camel case string into an underscored one, e.g. "HelloWorld" becomes "hello_world".
     * This variant uses a specific locale, which can be useful to e.g. force conversion from I to i
     * on Turkish devices, where the lower case i uses a special character.
     * 
     * 
     * @param camelCaseString
     *            the string to underscore
     * @param locale
     *            the locale that should be used for the string conversion
     * @return the underscored string
     */
    public static String underscore(String camelCaseString, Locale locale) {
        String[] words = splitByCharacterTypeCamelCase(camelCaseString);
        
        if ( words.length == 0 )
    	{
        	return "";
    	}
        
        //start building string with first element
        StringBuilder joinedString = new StringBuilder(words[0]);
        
        for ( int i = 1; i < words.length; i++ )
        {
        	joinedString.append( String.format("_%s", words[i] ) );
        }
        
        return locale != null ?
        		joinedString.toString().toLowerCase(locale) :
        		joinedString.toString().toLowerCase() ;
    }

    /**
     * <p>
     * Splits a String by Character type as returned by
     * <code>java.lang.Character.getType(char)</code>. Groups of contiguous
     * characters of the same type are returned as complete tokens, with the
     * following exception: the character of type
     * <code>Character.UPPERCASE_LETTER</code>, if any, immediately preceding a
     * token of type <code>Character.LOWERCASE_LETTER</code> will belong to the
     * following token rather than to the preceding, if any,
     * <code>Character.UPPERCASE_LETTER</code> token.
     * 
     * <pre>
     * StringUtils.splitByCharacterTypeCamelCase(null)         = null
     * StringUtils.splitByCharacterTypeCamelCase("")           = []
     * StringUtils.splitByCharacterTypeCamelCase("ab de fg")   = ["ab", " ", "de", " ", "fg"]
     * StringUtils.splitByCharacterTypeCamelCase("ab   de fg") = ["ab", "   ", "de", " ", "fg"]
     * StringUtils.splitByCharacterTypeCamelCase("ab:cd:ef")   = ["ab", ":", "cd", ":", "ef"]
     * StringUtils.splitByCharacterTypeCamelCase("number5")    = ["number", "5"]
     * StringUtils.splitByCharacterTypeCamelCase("fooBar")     = ["foo", "Bar"]
     * StringUtils.splitByCharacterTypeCamelCase("foo200Bar")  = ["foo", "200", "Bar"]
     * StringUtils.splitByCharacterTypeCamelCase("ASFRules")   = ["ASF", "Rules"]
     * </pre>
     * 
     * @param str
     *        the String to split, may be <code>null</code>
     * @return an array of parsed Strings, <code>null</code> if null String
     *         input
     * @since 2.4
     */
    public static String[] splitByCharacterTypeCamelCase(String str) {
        return splitByCharacterType(str, true);
    }

    /**
     * <p>
     * Splits a String by Character type as returned by
     * <code>java.lang.Character.getType(char)</code>. Groups of contiguous
     * characters of the same type are returned as complete tokens, with the
     * following exception: if <code>camelCase</code> is <code>true</code>, the
     * character of type <code>Character.UPPERCASE_LETTER</code>, if any,
     * immediately preceding a token of type
     * <code>Character.LOWERCASE_LETTER</code> will belong to the following
     * token rather than to the preceding, if any,
     * <code>Character.UPPERCASE_LETTER</code> token.
     * 
     * @param str
     *        the String to split, may be <code>null</code>
     * @param camelCase
     *        whether to use so-called "camel-case" for letter types
     * @return an array of parsed Strings, <code>null</code> if null String
     *         input
     * @since 2.4
     */
    private static String[] splitByCharacterType(String str, boolean camelCase) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return new String[0];
        }
        char[] c = str.toCharArray();
        ArrayList<String> list = new ArrayList<String>();
        int tokenStart = 0;
        int currentType = Character.getType(c[tokenStart]);
        for (int pos = tokenStart + 1; pos < c.length; pos++) {
            int type = Character.getType(c[pos]);
            if (type == currentType) {
                continue;
            }
            if (camelCase && type == Character.LOWERCASE_LETTER
                    && currentType == Character.UPPERCASE_LETTER) {
                int newTokenStart = pos - 1;
                if (newTokenStart != tokenStart) {
                    list.add(new String(c, tokenStart, newTokenStart - tokenStart));
                    tokenStart = newTokenStart;
                }
            } else {
                list.add(new String(c, tokenStart, pos - tokenStart));
                tokenStart = pos;
            }
            currentType = type;
        }
        list.add(new String(c, tokenStart, c.length - tokenStart));
        return (String[]) list.toArray(new String[list.size()]);
    }
    
    public static String urlEncode( List<NameValuePair> parameters )
	{
		//@formatter:off
		String encodedParams = 
				URLEncodedUtils.format( parameters, IgnitedHttp.CHARSET );
		
		encodedParams = workaroundJavaSpaceEncoding( encodedParams );
		
		//@formatter:on
		
		return encodedParams;
	}
    
    public static String urlEncode( String value )
	{
		String encodedValue = null;
		
		try
		{
			encodedValue = URLEncoder.encode( value, IgnitedHttp.CHARSET );
			
			encodedValue = workaroundJavaSpaceEncoding( encodedValue );
		}
		catch ( UnsupportedEncodingException e )
		{
			System.err.println(String.format( "%s\nError encoding value: %s",e, value ) );
		}
		
		return encodedValue;
	}
    
    // Java URI doesn't decode + signs as space as the URI spec determines :\ let's use the unicode escaped version of space instead
 	private static String workaroundJavaSpaceEncoding( String encodedParams )
 	{
 		return encodedParams.replaceAll( "\\+", "%20" );
 	}

}
