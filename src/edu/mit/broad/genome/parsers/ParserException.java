/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import org.apache.commons.lang3.SystemUtils;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.StandardException;

/**
 * An exception related to parsing data.
 *
 * @author Aravind Subramanian
 * @author David Eby - Updated to use modern Java built-in RuntimeException, tie into StandardException handling for better help.
 * @version %I%, %G%
 */
public class ParserException extends StandardException {
    
    /**
     * Create an exception with a detail message.
     *
     * @param msg the message
     */
    public ParserException(final String msg, int errorCode) {
        super(buildMessageWithErrorCode(msg, errorCode), errorCode);
    }
    
    public ParserException(final String msg, int lineNumber, int errorCode) {
        super(buildMessageWithLineNumberAndErrorCode(msg, lineNumber, errorCode), errorCode);
    }

    /**
     * Create a chained exception.
     *
     * @param t the nested exception.
     */
    public ParserException(final Throwable t, int errorCode) {
        super(buildMessageWithErrorCode(t.getMessage(), errorCode), t, errorCode);
    }

    /**
     * Create a chained exception along with a message
     *
     * @param msg the message
     * @param t   the nested esception.
     */
    public ParserException(final String msg, final Throwable t, int errorCode) {
        super(buildMessageWithErrorCode(msg, errorCode), t, errorCode);
    }

    public ParserException(final String msg, int lineNumber, final Throwable t, int errorCode) {
        super(buildMessageWithLineNumberAndErrorCode(msg, lineNumber, errorCode), t, errorCode);
    }
    
    private static String buildMessageWithErrorCode(final String msg, int errorCode) {
        return msg + SystemUtils.LINE_SEPARATOR
                + "See " + JarResources.getWikiErrorURL(String.valueOf(errorCode))
                + " for more information." ;
    }
    
    private static String buildMessageWithLineNumberAndErrorCode(final String msg, int lineNumber, int errorCode) {
        return "Line " + lineNumber + ": " + msg + SystemUtils.LINE_SEPARATOR
                + "See " + JarResources.getWikiErrorURL(String.valueOf(errorCode))
                + " for more information." ;
    }
}    // End ParserException
