/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.reports.api.Report;
import xapps.gsea.GseaWebResources;
import xtools.api.DefaultReport;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Parses a Report
 * Format:
 * <p/>
 * file\tfo/bar/path
 * file\tfoo/bar/path
 * ...
 * param\tname\tvalue
 * param\tname\tvalue
 * ...
 */
public class ReportParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public ReportParser() {
        super(Report.class);
    }

    /**
     * Only files and params are expoer
     *
     * @see "Above for format"
     */
    public void export(PersistentObject pob, File file) throws Exception {

        PrintWriter pw = startExport(pob, file);
        try {
            Report rep = (Report) pob;
    
            StringBuffer buf = new StringBuffer();
            buf.append(Report.PRODUCER_CLASS_ENTRY).append('\t').append(rep.getProducer().getName()).append('\n');
            buf.append(Report.TIMESTAMP_ENTRY).append('\t').append(rep.getTimestamp()).append('\n');
    
            Properties prp = rep.getParametersUsed();
            Enumeration en = prp.keys();
            while (en.hasMoreElements()) {
                String key = en.nextElement().toString();
                String val = prp.getProperty(key);
                buf.append(Report.PARAM_ENTRY).append('\t').append(key).append('\t').append(val).append('\n');
            }
    
            buf.append('\n');
    
            File[] files = rep.getFilesProduced();
            for (int f = 0; f < files.length; f++) {
                buf.append(Report.FILE_ENTRY).append('\t').append(files[f].getPath()).append('\n');
            }
    
            pw.print(buf.toString());
        }
        finally {
            pw.close();
        }
        doneExport();

    }    // End export


    /**
     * @returns 1 Report object
     * NO ann buiisness
     * @see above for format
     */
    public List parse(final String sourcepath, final InputStream is) throws Exception {

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        try {
            Line currLine = nextLine(bin, 0);
            String currContent = currLine.getContent();
    
            List filesList = new ArrayList();
            Properties params = new Properties();
            Class cl = null;
            long ts = 0; // beginning of time if no ts info available
    
            while (currContent != null) {
    
                String[] fields = ParseUtils.string2strings(currContent, "\t", false); // no spaces -- valid for file names!
    
                if ((fields.length != 2) && (fields.length != 3)) {
                    throw new ParserException("Bad RPT format: expected 2 or 3 fields, found: " + fields.length, currLine.getLineNumber(),
                            GseaWebResources.RPT_PARSER_ERROR_CODE);
                }
    
                if (fields[0].equalsIgnoreCase(Report.PRODUCER_CLASS_ENTRY)) {
                    if (fields.length != 2) {
                        throw new ParserException("Bad RPT format: >2 fields for " + Report.PRODUCER_CLASS_ENTRY, currLine.getLineNumber(),
                                GseaWebResources.RPT_PARSER_ERROR_CODE);
                    }
    
                    cl = Class.forName(fields[1]);
                }
    
                if (fields[0].equalsIgnoreCase(Report.TIMESTAMP_ENTRY)) {
                    if (fields.length != 2) {
                        throw new ParserException("Bad RPT format: >2 fields for " + Report.TIMESTAMP_ENTRY, currLine.getLineNumber(),
                                GseaWebResources.RPT_PARSER_ERROR_CODE);
                    }
    
                    try {
                        ts = Long.parseLong(fields[1]);
                    }
                    catch (NumberFormatException nfe) {
                        throw new ParserException("Bad RPT Format: could not parse field '"
                                + fields[1] + "' as a long value.", currLine.getLineNumber(), nfe,
                                GseaWebResources.RPT_PARSER_ERROR_CODE);
                    }
                }
    
                if (fields[0].equalsIgnoreCase(Report.FILE_ENTRY)) {
                    if (fields.length != 2) {
                        throw new ParserException("Bad RPT format: >2 fields for " + Report.FILE_ENTRY, currLine.getLineNumber(),
                                GseaWebResources.RPT_PARSER_ERROR_CODE);
                    }
                    filesList.add(new File(fields[1]));
                } else if (fields[0].equalsIgnoreCase(Report.PARAM_ENTRY)) {
                    if (fields.length != 3) {
                        throw new ParserException("Bad RPT format: Insufficient fields for " + Report.PARAM_ENTRY, currLine.getLineNumber(),
                                GseaWebResources.RPT_PARSER_ERROR_CODE);
                    }
                    params.put(fields[1], fields[2]);
                }
    
                currLine = nextLine(bin, currLine.getLineNumber());
                currContent = currLine.getContent();
            }
            File[] files = (File[]) filesList.toArray(new File[filesList.size()]);

            Report report = new DefaultReport(sourcepath, ts, cl, files, params, false);

            return unmodlist(report);
        }
        finally {
            bin.close();
        }
    }


}    // End of class ReportParser
