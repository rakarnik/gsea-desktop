/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.parsers.AbstractParser.Line;
import edu.mit.broad.vdb.sampledb.SampleAnnot;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import xapps.gsea.GseaWebResources;

/**
 * Simple dataset format with no res, gct cdt etc frills. Description field is optional.
 * TXT Dataset format -> no obvious extension
 * (txt is used for a lot of different things)
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class TxtDatasetParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public TxtDatasetParser() {
        super(Dataset.class);
    }

    /**
     * Only accepts Dataset objects.
     * see below for format produced
     */
    public void export(PersistentObject pob, File file) throws Exception {

        PrintWriter pw = startExport(pob, file);
        try {
            Dataset ds = (Dataset) pob;
            FeatureAnnot ann = ds.getAnnot().getFeatureAnnot();
    
            pw.print(Constants.NAME + "\t" + Constants.DESCRIPTION + "\t");
    
            for (int i = 0; i < ds.getNumCol(); i++) {
                pw.print(ds.getColumnName(i));
                pw.print('\t');
            }
    
            pw.println();
    
            for (int r = 0; r < ds.getNumRow(); r++) {
                String featname = ds.getRowName(r);
                pw.print(featname);
                pw.print('\t');
                String desc = ann.getNativeDesc(featname);
                if (desc != null) {
                    pw.print(desc);
                } else {
                    pw.print(Constants.NA);
                }
                pw.print('\t');
                pw.println(ds.getRow(r).toString('\t'));
            }
        }
        finally {
            pw.close();
        }
        doneExport();
    }

    /**
     * Expects a TXT data format compatible store (no specific extension known)
     * Produces a Dataset and a DatasetAnnotation object.
     * <p/>
     * BASIC TXT FORMAT (DESCRIPTION field is optional -> auto detected)
     * <p/>
     * NAME       DESCRIPTION	spo0	spo30	spo2	spo5	spo7	spo9	spo11
     * YAL003W	    some	    0.23	-1.79	-1.29	-1.56	-2.12	-1.09	-1.12
     * YAL004W	    some        0.41	-0.38	-0.89	-1.06	-1.6	-1.84	-1.6
     * YAL005C	    some	    0.61	-0.07	-1.29	-1.29	-2	    -1.84	-2.25
     */
    /// does the real parsing
    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        try {
            sourcepath = NamingConventions.removeExtension(sourcepath);
            Line currLine = nextLine(bin, 0);
            String currContent = currLine.getContent();
    
            // 1st  non-empty, non-comment line are the column names
            List colnames = ParseUtils.string2stringsList(currContent, "\t"); // colnames can have spaces
    
            colnames.remove(0);                                 // first elem is always nonsense
    
            boolean hasDesc = false;
            if (colnames.get(0).toString().equalsIgnoreCase(Constants.DESCRIPTION)
                    ||
                    colnames.get(0).toString().equalsIgnoreCase("DESC")
                    ) {
                colnames.remove(0);
                hasDesc = true;
            }
    
            log.debug("HAS DESC: " + hasDesc);
    
            // At this point, currLine should contain the first data line
            // data line: <row name> <tab> <ex1> <tab> <ex2> <tab>
            List lines = new ArrayList();
    
            currLine = nextLineTrimless(bin, currLine.getLineNumber());
            currContent = currLine.getContent();
            int firstDataLineNumber = currLine.getLineNumber();
    
            // save all rows so that we can determine how many rows exist
            while (currContent != null) {
                lines.add(currContent);
                currLine = nextLineTrimless(bin, currLine.getLineNumber()); /// imp for mv datasets -> last col(s) can be a tab
                currContent = currLine.getContent();
            }
    
            if (hasDesc) {
                return _parseHasDesc(sourcepath, lines, colnames, firstDataLineNumber);
            }
            return _parseNoDesc(sourcepath, lines, colnames,firstDataLineNumber);
        }
        finally {
            bin.close();
        }
    }

    private List _parseNoDesc(String objName, List lines, List colNames, int firstDataLineNumber) throws Exception {
        objName = NamingConventions.removeExtension(objName);
        Matrix matrix = new Matrix(lines.size(), colNames.size());
        List rowNames = new ArrayList();
        List rowDescs = new ArrayList();

        for (int i = 0, lineNumber = firstDataLineNumber; i < lines.size(); i++, lineNumber++) {
            String currLine = (String) lines.get(i);
            List fields = string2stringsV2(currLine, colNames.size() + 1); // spaces allowed in name & desc field so DONT tokenize them

            if (fields.size() != colNames.size() + 1) {
                throw new ParserException("Bad TXT format: expected " + (colNames.size() + 1 + 1)
                        + " columns but found " + fields.size()
                        + ".\nIf this dataset has missing values, use ImputeDataset to fill these in before importing as a Dataset",
                        lineNumber, GseaWebResources.TXT_PARSER_ERROR_CODE);
            }

            String rowname = fields.get(0).toString().trim();
            if (rowname.length() == 0) {
                throw new ParserException("Bad TXT format: rowname is empty", lineNumber, GseaWebResources.TXT_PARSER_ERROR_CODE);
            }

            String desc = Constants.NA;

            rowDescs.add(desc);
            rowNames.add(rowname);

            int coln = 0;
            for (int f = 1; f < fields.size(); f++) {
                String s = fields.get(f).toString().trim();
                float val;
                if (s.length() == 0) {
                    val = Float.NaN;
                } else {
                    try {
                        val = Float.parseFloat(s);
                    }
                    catch (NumberFormatException nfe) {
                        throw new ParserException("Bad TXT Format: could not parse field '"
                                + s + "' in column " + (f+1) + " as a float value.", lineNumber, nfe,
                                GseaWebResources.TXT_PARSER_ERROR_CODE);
                    }
                }
                matrix.setElement(i, coln++, val);
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

    // TODO: merge the above method with this one.
    // They are identical except for a few numeric constants used, code called, etc.  Could easily
    // parameterize with a boolean hasDesc flag.
    private List _parseHasDesc(String objName, List lines, List colNames, int firstDataLineNumber) throws Exception {
        objName = NamingConventions.removeExtension(objName);
        Matrix matrix = new Matrix(lines.size(), colNames.size());
        List rowNames = new ArrayList();
        List rowDescs = new ArrayList();

        for (int i = 0, lineNumber = firstDataLineNumber; i < lines.size(); i++, lineNumber++) {
            String currLine = (String) lines.get(i);
            List fields = string2stringsV2(currLine, colNames.size() + 2); // spaces allowed in name & desc field so DONT tokenize them

            if (fields.size() != colNames.size() + 2) {
                throw new ParserException("Bad TXT format: expected " + (colNames.size() + 2)
                        + " columns but found " + fields.size()
                        + ".\nIf this dataset has missing values, use ImputeDataset to fill these in before importing as a Dataset",
                        lineNumber, GseaWebResources.TXT_PARSER_ERROR_CODE);
            }

            String rowname = fields.get(0).toString().trim();
            if (rowname.length() == 0) {
                throw new ParserException("Bad TXT format: rowname is empty", lineNumber, GseaWebResources.TXT_PARSER_ERROR_CODE);
            }

            String desc = fields.get(1).toString().trim();

            if (desc.length() == 0) {
                throw new ParserException("Bad TXT format: desc is empty", lineNumber, GseaWebResources.TXT_PARSER_ERROR_CODE);
            }

            rowDescs.add(desc);
            rowNames.add(rowname);

            int coln = 0;
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
                        throw new ParserException("Bad TXT Format: could not parse field '"
                                + s + "' in column " + (f+1) + " as a float value.", lineNumber, nfe,
                                GseaWebResources.TXT_PARSER_ERROR_CODE);
                    }
                }
                matrix.setElement(i, coln++, val);
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

}    // End TxtDatasetParser

