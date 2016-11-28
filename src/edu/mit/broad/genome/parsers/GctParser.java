/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.utils.ParseException;
import edu.mit.broad.vdb.sampledb.SampleAnnot;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import xapps.gsea.GseaWebResources;

/**
 * Parses a gct formatted dataset -- similar to dataframe except that formatted
 * so that legacy MIT gct data is accepted
 * <p/>
 * Format:
 * <p/>
 * #1.2
 * 12488	41
 * Name	Description	col_0 col_1 col_2 ...
 * row_o
 * row_1
 * ...
 * <p/>
 * NO AP calls
 */
public class GctParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public GctParser() {
        super(Dataset.class);
    }

    /**
     * Export a Dataset to file in gct format
     * Only works with Datasets
     *
     * @see "Above for format"
     */
    public void export(final PersistentObject pob, final File file) throws Exception {
        _export(pob, startExport(pob, file));
    }

    public void export(final PersistentObject pob, final OutputStream os) throws Exception {
        _export(pob, startExport(pob, os, null));
    }

    private void _export(final PersistentObject pob, final PrintWriter pw) throws Exception {
        try {
            final Dataset ds = (Dataset) pob;
            FeatureAnnot ann = null;
            if (ds.getAnnot() != null) {
                ann = ds.getAnnot().getFeatureAnnot();
            }
    
            //log.debug("Annotation is: " + ann);
    
            pw.println("#1.2"); // not sure what the # means, but give the people what they want
            pw.println(ds.getNumRow() + "\t" + ds.getNumCol());
            pw.print(Constants.NAME + "\t" + Constants.DESCRIPTION + "\t");
    
            for (int i = 0; i < ds.getNumCol(); i++) {
                pw.print(ds.getColumnName(i));
                if (i != ds.getNumCol() - 1) {
                    pw.print('\t');
                }
            }
    
            pw.println();
    
            // Give preference to Native desc if it exists
            // If not, use the symbol desc
            for (int r = 0; r < ds.getNumRow(); r++) {
                StringBuffer buf = new StringBuffer();
                String rowName = ds.getRowName(r);
                buf.append(rowName).append('\t');
                String desc = Constants.NA;
                if (ann != null) {
                    if (ann.hasNativeDescriptions()) {
                        desc = ann.getNativeDesc(rowName);
                    } else {
                        String symbol = ann.getGeneSymbol(rowName);
                        if (symbol != null) {
                            desc = symbol + ":" + ann.getGeneTitle(rowName);
                        }
                    }
                }
    
                if (desc == null) {
                    desc = Constants.NA;
                }
    
                buf.append(desc).append('\t');
                buf.append(ds.getRow(r).toString('\t'));
                pw.println(buf.toString());
            }
        }
        finally {
            pw.close();
        }
        
        doneExport();
    }    // End export

    /**
     * @returns 1 Dataset object
     * Always DatasetAnnotation object produced , but if underlying df has none then na is used
     * @see above for format
     */
    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);
        sourcepath = NamingConventions.removeExtension(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        try {
            // TODO: fix this header handling code.  GCT does not allow comment lines, and the header has a specific form.
            Line currLine = nextLine(bin, 0);
            String currContent = currLine.getContent();
    
            // 1st  non-empty, non-comment line is numrows and numcols
            int[] nstuff = null;
            try {
                nstuff = ParseUtils.string2ints(currContent, " \t");
            
                int rowColHeaderLineNumber = currLine.getLineNumber();
                if (nstuff.length != 2) {
                    throw new ParserException("Bad GCT format: expecting two integer values for row/column info, found "
                            + nstuff.length + " value(s).", rowColHeaderLineNumber, GseaWebResources.GCT_PARSER_ERROR_CODE);
                }
        
                int nrows = nstuff[0];
                int ncols = nstuff[1];
        
                // First 2 fields name and desc are to be ignored
                // Note: leaving it this way for legacy purposes.  The GCT format requires "NAME\tDescription",
                // but this parser is more lenient and ignores the first two values so long as they are present.
                currLine = nextLine(bin, currLine.getLineNumber());
                currContent = currLine.getContent();
                List colnames = ParseUtils.string2stringsList(currContent, "\t"); // colnames can have spaces
                colnames.remove(0);                                 // first elem is always nonsense
                colnames.remove(0);
                if (colnames.size() != ncols) {
                    throw new ParserException("Bad GCT format: expected " + ncols
                            + " columns based on header line specification but found " + colnames.size(),
                            rowColHeaderLineNumber, GseaWebResources.GCT_PARSER_ERROR_CODE);
                }
        
                // At this point, currContent should be just before the first data line
                // data line: <row name> <tab> <ex1> <tab> <ex2> <tab>
                List lines = new ArrayList();
    
                currLine = nextLineTrimless(bin, currLine.getLineNumber());
                int firstDataLineNumber = currLine.getLineNumber();
                currContent = currLine.getContent();
        
                // save all rows so that we can determine how many rows exist
                // TODO: Can possibly restructure.  We can process as we go and just signal an error if nrows is exceeded.
                while (currContent != null) {
                    lines.add(currContent);
                    currLine = nextLineTrimless(bin, currLine.getLineNumber());
                    currContent = currLine.getContent(); /// imp for mv datasets -> last col(s) can be a tab
                }
        
                if (lines.size() != nrows) {
                    throw new ParserException("Bad GCT format: expected " + nrows
                            + " rows based on header line specification but found " + lines.size(),
                            rowColHeaderLineNumber, GseaWebResources.GCT_PARSER_ERROR_CODE);
                }
    
                return _parseHasDesc(sourcepath, lines, colnames, firstDataLineNumber);
            }
            catch (ParseException pe) {
                throw new ParserException(pe.getMessage(), currLine.getLineNumber(), pe,
                        GseaWebResources.GCT_PARSER_ERROR_CODE);
            }
        }
        finally {
            bin.close();
        }
    }

    private List _parseHasDesc(String objName, List lines, List colNames, int firstDataLineNumber) throws Exception {
        objName = NamingConventions.removeExtension(objName);
        Matrix matrix = new Matrix(lines.size(), colNames.size());
        List rowNames = new ArrayList();
        List rowDescs = new ArrayList();

        for (int i = 0, lineNumber = firstDataLineNumber; i < lines.size(); i++, lineNumber++) {
            String currLine = (String) lines.get(i);
            List fields = string2stringsV2(currLine, colNames.size() + 2); // spaces allowed in name & desc field so DONT tokenize them

            if (fields.size() != colNames.size() + 1 + 1) {
                throw new ParserException("Bad GCT format: expected " + (colNames.size() + 1 + 1)
                        + " columns based on header line specification but found " + fields.size()
                        + ".\nIf this dataset has missing values, use ImputeDataset to fill these in before importing as a Dataset",
                        lineNumber, GseaWebResources.GCT_PARSER_ERROR_CODE);
            }

            String rowname = fields.get(0).toString().trim();
            if (rowname.length() == 0) {
                throw new ParserException("Bad GCT format: row name is empty ", lineNumber, GseaWebResources.GCT_PARSER_ERROR_CODE);
            }

            String desc = fields.get(1).toString().trim();
            if (desc.length() == 0) {
                desc = Constants.NA; // dont exception out - genecluster like behavior
            }

            rowDescs.add(desc);
            rowNames.add(rowname);

            // TODO: should this be a utility method somewhere?
            for (int f = 2; f < fields.size(); f++) {
                String s = fields.get(f).toString().trim();
                float val;
                if (s.length() == 0) {
                    val = Float.NaN;
                } else {
                    try {
                        val = Float.parseFloat(s);
                    }
                    catch (NumberFormatException nfe) {
                        throw new ParserException("Bad GCT Format: could not parse field '"
                                + s + "' in column " + (f+1) + " as a float value.", lineNumber, nfe,
                                GseaWebResources.GCT_PARSER_ERROR_CODE);
                    }
                }
                matrix.setElement(i, f - 2, val);
            }
        }

        final FeatureAnnot ann = new FeatureAnnotImpl(objName, rowNames, rowDescs);
        ann.addComment(fComment.toString());
        final SampleAnnot sann = new SampleAnnotImpl(objName, colNames, null);

        final Dataset ds = new DefaultDataset(objName, matrix, rowNames, colNames, true, new AnnotImpl(ann, sann));
        ds.addComment(fComment.toString());
        doneImport();
        return unmodlist(new PersistentObject[]{ds});
    }

}    // End of class GctParser
