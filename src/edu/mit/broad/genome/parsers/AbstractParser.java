/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.utils.ClassUtils;
import edu.mit.broad.genome.utils.ParseException;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Base class for Parser implementations.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public abstract class AbstractParser implements Parser {

    protected final Logger log;

    protected static final Logger klog = XLogger.getLogger(AbstractParser.class);

    protected final Comment fComment;

    private Class fRepClass;

    private String fRepClassName;

    // not always filled -- used ONLY for logging
    private File _importFile;
    private Object _importObjName;

    // not always filled for exports
    private PrintWriter _exportPw;

    private boolean fSilentMode;

    /**
     * Class Constructor.
     */
    protected AbstractParser(Class repClass) {
        if (repClass == null) {
            throw new IllegalArgumentException("Parameter repClass cannot be null");
        }

        //this.log = XLogger.getLogger(this.getClass());
        this.log = XLogger.getLogger(AbstractParser.class);
        this.fComment = new Comment();
        this.fRepClass = repClass;
        this.fRepClassName = ClassUtils.shorten(fRepClass);
    }

    public void export(final PersistentObject pob, final OutputStream os) throws Exception {
        throw new NotImplementedException();
    }

    public List parse(String objname, File file) throws Exception {
        this._importFile = file;
        this._importObjName = objname;
        return parse(objname, new FileInputStream(file));
    }

    public void setSilentMode(boolean silent) {
        this.fSilentMode = silent;
    }

    protected boolean isSilentMode() {
        return fSilentMode;
    }

    /**
     * Utility method to tourn a pob into an unmodifiable list
     */
    protected static List unmodlist(PersistentObject pob) {

        List list = new ArrayList(1);

        list.add(pob);

        return Collections.unmodifiableList(list);
    }

    /**
     * Utility method to place specified pobs in an unmodifiable list
     */
    protected static List unmodlist(PersistentObject[] pobs) {

        List list = new ArrayList(pobs.length);

        for (int i = 0; i < pobs.length; i++) {
            list.add(pobs[i]);
        }

        return Collections.unmodifiableList(list);
    }

    /**
     * adds any comment lines found to the fComnent class var
     *
     * @param bin
     * @return
     * @throws IOException
     */
    protected Line nextLine(final BufferedReader bin, int prevLineNumber) throws IOException {
        int lineNumber = prevLineNumber;
        String currLine = bin.readLine();
        lineNumber++;
        if (currLine == null) {
            return new Line(lineNumber, null);
        }

        currLine = currLine.trim();

        // TODO: fix parsing of comment characters.
        // We currently *always* handle '#' as a comment, even for files that don't treat it as such.
        while ((currLine != null) && ((currLine.length() == 0) || (currLine.startsWith(Constants.COMMENT_CHAR)))) {
            if (currLine.startsWith(Constants.COMMENT_CHAR)) {
                fComment.add(currLine);
            }

            currLine = bin.readLine();
            lineNumber++;
            if (currLine != null) {
                currLine = currLine.trim();
            }
        }

        return new Line(lineNumber, currLine);
    }
    
    /**
     * adds any comment lines found to the fComnent class var
     *
     * @param bin
     * @return
     * @throws IOException
     */
    // Temporary proxy to new Line method for legacy callers
    protected String nextLine(final BufferedReader bin) throws IOException {
        Line currLine = nextLine(bin, 0);
        return currLine.getContent();
    }

    // comments are not next'ed over
    protected Line nextNonEmptyLine(BufferedReader bin, int prevLineNumber) throws IOException {

        int lineNumber = prevLineNumber;
        String currLine = bin.readLine();
        lineNumber++;
        if (currLine == null) {
            return new Line(lineNumber, null);
        }

        currLine = currLine.trim();

        while ((currLine != null) && ((currLine.length() == 0))) {
            currLine = bin.readLine();
            lineNumber++;
            if (currLine != null) {
                currLine = currLine.trim();
            }
        }

        return new Line(lineNumber, currLine);
    }
    
    // comments are not next'ed over
    // Temporary proxy to new Line method for legacy callers
    protected String nextNonEmptyLine(BufferedReader bin) throws IOException {
        Line currLine = nextNonEmptyLine(bin, 0);
        return currLine.getContent();
    }

    protected Line nextLineTrimless(BufferedReader bin, int prevLineNumber) throws IOException {
        int lineNumber = prevLineNumber;
        String currLine = bin.readLine();
        lineNumber++;
        if (currLine == null) {
            return new Line(lineNumber, null);
        }

        // TODO: fix parsing of comment characters.
        // We currently *always* handle '#' as a comment, even for files that don't treat it as such.
        while ((currLine != null) && ((currLine.length() == 0) || (currLine.startsWith(Constants.COMMENT_CHAR)))) {
            if (currLine.startsWith(Constants.COMMENT_CHAR)) {
                fComment.add(currLine);
            }
            currLine = bin.readLine();
            lineNumber++;
        }

        return new Line(lineNumber, currLine);
    }
    
    // Temporary proxy to new Line method for legacy callers
    protected String nextLineTrimless(BufferedReader bin) throws IOException {
        Line currLine = nextLineTrimless(bin, 0);
        return currLine.getContent();
    }

    protected boolean isNull(Object obj) {
        if (obj == null) {
            return true;
        }

        String s = obj.toString();
        s = s.trim();
        if (s.length() == 0) {
            return true;
        }

        return Constants.NULL.equalsIgnoreCase(s.trim());
    }

    protected boolean isNullorNa(final String s) {
        if (isNull(s)) {
            return true;
        }

        return Constants.NA.equalsIgnoreCase(s.trim());
    }

    protected PrintWriter startExport(PersistentObject pob, File file) throws IOException {

        if (file == null) {
            throw new IllegalArgumentException("Parameter file cannot be null");
        }

        return startExport(pob, new FileOutputStream(file), file.getName());
    }

    protected PrintWriter startExport(PersistentObject pob, OutputStream os, String toName) throws IOException {
        if (pob == null) {
            throw new IllegalArgumentException("Parameter pob cannot be null");
        }

        if (os == null) {
            throw new IllegalArgumentException("Parameter os cannot be null");
        }

        if (!fSilentMode) {
            //TraceUtils.showTrace();
            log.debug("Exporting: " + pob.getName() + " to: " + toName + " " + pob.getClass());
        }

        _exportPw = new PrintWriter(os);
        return _exportPw;
    }

    protected void doneExport() {
        if (_exportPw != null) {
            _exportPw.flush();
            _exportPw.close();
        }
    }

    protected void startImport(String sourcepath) {
        if (!fSilentMode) {
            log.info("Begun importing: " + fRepClassName + " from: " + sourcepath);
        }
    }

    protected void doneImport() {
    }

    protected class Line {
        private int lineNumber;
        private String content;

        public Line(int lineNumber, String content) {
            this.lineNumber = lineNumber;
            this.content = content;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
    
    
    protected class Comment {

        private List fLines;

        private Map fKeyValues;

        protected void add(String s) {
            if (s == null || s.length() == 0) {
                return;
            }

            s = s.substring(1, s.length()); // get rid of comm char

            if (s.length() == 0) {
                return;
            }

            if (s.indexOf('=') != -1) {
                if (fKeyValues == null) {
                    fKeyValues = new HashMap();
                }
                String[] fields = ParseUtils.string2strings(s, "= ", true);

                if (fields.length == 1) {
                    // nothing
                } else if (fields.length == 2) {
                    fKeyValues.put(fields[0].toUpperCase(), fields[1]);
                } else {
                    log.warn("Bad comment KEY=VALUE field: Got more tokens than expected: " + fields.length);
                }
            } else {
                if (fLines == null) {
                    fLines = new ArrayList();
                }
                fLines.add(s);
            }
        }

        public String toString() {
            if (fLines == null && fKeyValues == null) {
                return "";
            }

            StringBuffer buf = new StringBuffer();

            if (fLines != null && !fLines.isEmpty()) {
                for (int i = 0; i < fLines.size(); i++) {
                    buf.append(fLines.get(i).toString()).append('\n');
                }
            }

            if (fKeyValues != null && !fKeyValues.isEmpty()) {
                buf.append(fKeyValues.toString());
            }

            return buf.toString();
        }
    }

    // TODO: review whether this is always used for NAME\tDescription only and if so optimize. 
    protected static List string2stringsV2(String s, int expectedLen) throws ParseException {

        if (null == s) {
            throw new ParseException("Internal parsing error: attempt to work on null String");
        }

        String delim = "\t"; // @note, always
        StringTokenizer tok = new StringTokenizer(s, delim, true); // note including the delim in rets
        List ret = new ArrayList();
        String prev = null;

        int cnt = 0;
        while (tok.hasMoreTokens()) {
            final String curr = tok.nextToken(); // dont trim as curr might be a tab!

            if (cnt == 0) { // the first field
                ret.add(curr.trim()); // always add it, empty or not
            } else {
                if (curr.equals(delim)) {
                    if (prev.equals(delim)) { // 2 consecutive tabs
                        ret.add(""); //empty field
                    } else { // omit because its _the delim_

                    }
                } else {
                    ret.add(curr.trim()); // a real word, ok to trim. Then add
                }
            }

            prev = curr;
            cnt++;
        }

        if (ret.size() == expectedLen) {
            return ret;
        } else if (ret.size() < expectedLen) {
            /// fill out whatever's left with empty
            for (int i = ret.size(); i < expectedLen; i++) {
                ret.add("");
            }
            return ret;

        } else {
            // @note added Nov 28, 2005
            // delete any extra tabs (ret.size() > expectedLen
            List real_ret = new ArrayList();
            for (int i = 0; i < ret.size(); i++) {
                if (i < expectedLen) {
                    real_ret.add(ret.get(i));
                } else {
                    // dont add empty
                    Object obj = ret.get(i);
                    if (obj == null || obj.toString().trim().length() == 0) {
                        // dont add
                    } else {
                        real_ret.add(obj); // cant do anything  might be a genuine format error
                    }
                }
            }

            return real_ret;
        }
    }

    protected static int indexOf(final String s, final List list, final boolean barfIfMising, int lineNumberForError) throws ParserException {
        int index = list.indexOf(s);
        if (barfIfMising && index == -1) {
            throw new ParserException("Expected column not found: " + s, lineNumberForError);
        }
        return index;
    }
}    // End AbstractParser
