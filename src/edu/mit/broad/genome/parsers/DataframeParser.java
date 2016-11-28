/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.Dataframe;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.AbstractParser.Line;
import gnu.trove.TFloatArrayList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import xapps.gsea.GseaWebResources;

/**
 * Parses a Dataframe
 * Format:
 * <p/>
 * row_names        col_0 col_1 col_2 ...
 * row_0                    34.5  45.6  56.8
 * row_1                    ...   ...   ...
 * row_2                    ...   ...   ...
 * ...
 * <p/>
 * Notes:
 * <p/>
 * First row is the column header
 * First column are the row names
 * Whitespace delimited - row names and col names cannot have whitespace
 * num of rows line is not required
 * scan line is not required
 * no A/P calls
 */

public class DataframeParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public DataframeParser() {
        super(Dataframe.class);
    }

    public void export(final PersistentObject pob, final File file) throws Exception {

        PrintWriter pw = startExport(pob, file);
        _export(pob, pw);

    }

    public void export(final PersistentObject pob, final OutputStream os) throws Exception {
        PrintWriter pw = startExport(pob, os, null);
        _export(pob, pw);
    }

    /**
     * Export a Dataframe to file in Dataframe format
     * Only works with Dataframes
     *
     * @see "Above for format"
     */
    // Does the real work
    private void _export(final PersistentObject pob, final PrintWriter pw) throws Exception {

        try {
            final Dataframe df = (Dataframe) pob;
    
            pw.print(Constants.NAME + "\t");
    
            for (int i = 0; i < df.getNumCol(); i++) {
                pw.print(df.getColumnName(i));
                pw.print('\t');
            }
    
            pw.println();
    
            for (int r = 0; r < df.getNumRow(); r++) {
                pw.print(df.getRowName(r));
                pw.print('\t');
                pw.println(df.getRow(r).toString('\t'));
            }
        }
        finally {
            pw.close();
        }
        
        doneExport();
    }    // End export

    public List parse(String sourcepath, InputStream is) throws Exception {
        /// does the real parsing
        // expects the bin to be untouched
        // KEEP THIS LEAN AND MEAN

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));

        startImport(sourcepath);
        Line currLine = nextLine(bin, 0);
        String currContent = currLine.getContent();
        
        // FIRST GET THE COL NAMES
        // 1st  non-empty, non-comment line is the col header:
        // First fields is to be ignored
        List colNames = ParseUtils.string2stringsList(currContent, " \t");
        
        colNames.remove(0); // first elem is always nonesense (i.e Name)
        
        // At this point, currLine should contain the first data line
        
        currLine = nextLine(bin, currLine.getLineNumber());
        currContent = currLine.getContent();
        
        TFloatArrayList floats = new TFloatArrayList();
        List rowNames = new ArrayList();
        
        // save all rows so that we can determine how many rows exist
        while (currContent != null) {
            List fields = ParseUtils.string2stringsList(currContent, " \t"); // spaces NOT allowed in name field, so tokenize them
        
            if (fields.size() != colNames.size() + 1) {
                throw new ParserException("Bad DataFrame format: expected " + (colNames.size() + 1)
                        + "columns but found: " + fields.size(),
                        currLine.getLineNumber(), GseaWebResources.DATAFRAME_PARSER_ERROR_CODE);
            }
        
            String rowName = (String) fields.get(0);
            rowNames.add(rowName);
        
            for (int f = 1; f < fields.size(); f++) { // from 1 as first field is the Name
                String field = fields.get(f).toString();
                try {
                    floats.add(Float.parseFloat(field));
                }
                catch (NumberFormatException nfe) {
                    throw new ParserException("Bad DataFrame Format: could not parse field '"
                            + field + "' in column " + (f+1) + " as a float value.", 
                            currLine.getLineNumber(), nfe, GseaWebResources.DATAFRAME_PARSER_ERROR_CODE);
                }
            }
        
            currLine = nextLine(bin, currLine.getLineNumber());
            currContent = currLine.getContent();
        }
        
        bin.close();
        
        doneImport();
        
        log.info("Completed parsing DATAFRAME");
        
        Matrix matrix = new Matrix(rowNames.size(), colNames.size(), floats);
        
        Dataframe df1 = new Dataframe(sourcepath, matrix, rowNames, colNames, true, true, true);
        df1.addComment(fComment.toString());
        Dataframe df = df1;

        return unmodlist(new PersistentObject[]{df});

    }

}    // End of class DataframeParser

