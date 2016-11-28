/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.Headers;
import edu.mit.broad.genome.math.StringMatrix;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.StringDataframe;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import xapps.gsea.GseaWebResources;

/**
 * Parses a StringStringDataframe
 * Format:
 * <p/>
 * row_names        col_0 col_1 col_2 ...
 * row_0                    34.5  45.6  56.8
 * row_1                    ...   ...   ...
 * row_2                    ...   ...   ...
 * <p/>
 * ...
 * Notes:
 * <p/>
 * NO desc fields
 * <p/>
 * First row is the column header
 * First column are the row names
 * Whitespace delimited - row names and col names cannot have whitespace
 * num of rows line is not required
 * scan line is not required
 * no A/P calls
 */
public class StringDataframeParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public StringDataframeParser() {
        super(StringDataframe.class);
    }

    /**
     * Export a StringDataframe to file in StringDataframe format
     * Only works with StringDataframes
     *
     * @see "Above for format"
     */
    public void export(final PersistentObject pob, final File file) throws Exception {

        PrintWriter pw = startExport(pob, file);
        _export(pob, pw);

    }

    public void export(final PersistentObject pob, final OutputStream os) throws Exception {
        PrintWriter pw = startExport(pob, os, null);
        _export(pob, pw);
    }

    // does the real stuff
    private void _export(final PersistentObject pob, final PrintWriter pw) throws Exception {
        try {
            final StringDataframe sdf = (StringDataframe) pob;
    
            String s = sdf.getRowLabelName();
            if (s == null) {
                s = Headers.NAME;
            }
    
            pw.print(s + "\t");
    
            for (int i = 0; i < sdf.getNumCol(); i++) {
                pw.print(sdf.getColumnName(i));
                pw.print('\t');
            }
    
            pw.println();
    
            for (int r = 0; r < sdf.getNumRow(); r++) {
                pw.print(sdf.getRowName(r));
                pw.print('\t');
                final int len = sdf.getNumCol();
                for (int c = 0; c < len; c++) {
                    pw.print(sdf.getElement(r, c));
                    pw.print('\t');
                }
                pw.println();
            }
        }
        finally {
            pw.close();
        }
        doneExport();

    }    // End export

    /**
     * @returns 1 StringDataframe object
     * NO ann buiisness
     * @see above for format
     */
    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);
        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        StringDataframe sdf = parseSdf(sourcepath, bin, nextNonEmptyLine(bin, 0));
        return unmodlist(new PersistentObject[]{sdf});

    }

    public StringDataframe parseSdf(File file) throws Exception {
        BufferedReader bin = new BufferedReader(new FileReader(file));
        return parseSdf(file.getName(), bin, nextNonEmptyLine(bin, 0));
    }

    protected StringDataframe parseSdf(String objname, BufferedReader bin, Line firstLine) throws Exception {
        try {
            Line currLine = firstLine;
            String currContent = currLine.getContent();
    
            // 1st  non-empty, non-comment line is the col header:
            // First fields is to be ignored
            List colNames = ParseUtils.string2stringsList(currContent, "\t");
    
            //log.debug("# cols found: " + colnames.size());
    
            colNames.remove(0);                                 // first elem is always nonsense
    
            // At this point, currLine should contain the first data line
            // data line: <row name> <tab> <ex1> <tab> <ex2> <tab>
            List lines = new ArrayList();
    
            currLine = nextLineTrimless(bin, currLine.getLineNumber());
            currContent = currLine.getContent();
            int firstDataLineNumber = currLine.getLineNumber();
            
            // save all rows so that we can determine how many rows exist
            while (currContent != null) {
                lines.add(currContent);
                currLine = nextLineTrimless(bin, currLine.getLineNumber()); /// so that last col(s) can be a tab
                currContent = currLine.getContent();
            }
    
            StringDataframe sdf = _parse(objname, lines, colNames, firstDataLineNumber);
            doneImport();
            return sdf;
        }
        finally {
            bin.close();
        }
    }

    private StringDataframe _parse(String objname, List lines, List colNames, int firstDataLineNumber) throws Exception {

        StringMatrix matrix = new StringMatrix(lines.size(), colNames.size());
        List rowNames = new ArrayList();
        String nstr = null;

        for (int i = 0, lineNumber = firstDataLineNumber; i < lines.size(); i++, lineNumber++) {
            String currLine = (String) lines.get(i);
            List fields = string2stringsV2(currLine, colNames.size() + 1); // + 1 for the name col

            if (fields.size() != colNames.size() + 1) {
                throw new ParserException("Bad SDF format: expected " + (colNames.size() + 1)
                        + "columns but found: " + fields.size(), lineNumber, GseaWebResources.SDF_PARSER_ERROR_CODE);
            }

            String rowname = (String) fields.get(0);

            rowNames.add(rowname);
            for (int f = 1; f < fields.size(); f++) {
                String elem = fields.get(f).toString();
                if (elem.equalsIgnoreCase(Constants.NULL)) {
                    matrix.setElement(i, f - 1, nstr);
                } else {
                    matrix.setElement(i, f - 1, elem);
                }
            }

        }

        StringDataframe sdf = new StringDataframe(objname, matrix, rowNames, colNames, true);
        sdf.addComment(fComment.toString());

        return sdf;

    }

}    // End of class StringDataframeParser
