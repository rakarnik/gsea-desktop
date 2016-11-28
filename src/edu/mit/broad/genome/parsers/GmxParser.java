/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.objects.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import xapps.gsea.GseaWebResources;

/**
 * Parses a file in GeneSetMatrix format to produce a single GeneSetMatrix object and several
 * FSets
 * <p/>
 * Format Supported:
 * <p/>
 * Type I: One FSet file -> 1 FSet object
 * <br><pre>
 * fset0_name    fset1_name    ...
 * fset0_color   fset1_color   ...
 * member        member
 * member        ...
 * member
 * ...
 * <p/>
 * Need NOT be equal number of members in each column
 * Comments allowed as usual with the # sign
 * <pre>
 * <br>
 * <p/>
 * ...
 * <pre>
 * <br>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GmxParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public GmxParser() {
        super(GeneSetMatrix.class);
    }

    /**
     * Only accepts GeneSetMatrix
     */
    public void export(PersistentObject gmpob, File file) throws Exception {

        PrintWriter pw = startExport(gmpob, file);
        try {
            GeneSetMatrix gm = (GeneSetMatrix) gmpob;
    
            StringBuffer buf = new StringBuffer();
            GeneSet[] gsets = gm.getGeneSets();
    
            for (int f = 0; f < gsets.length; f++) {
                //System.out.println(">" + fsets[f].getName() + "<");
                buf.append(gsets[f].getName()).append('\t');
            }
    
            buf.append('\n');
    
            for (int f = 0; f < gsets.length; f++) {
                String ne = gsets[f].getNameEnglish();
                if (isNullorNa(ne)) {
                    ne = Constants.NA;
                }
                buf.append(ne).append('\t');
            }
    
            buf.append('\n');
    
            int max = gm.getMaxGeneSetSize();
    
            pw.print(buf.toString());
            pw.flush();
    
            for (int i = 0; i < max; i++) {
                buf = new StringBuffer();
                for (int f = 0; f < gsets.length; f++) {
                    if (i < gsets[f].getNumMembers()) {
                        buf.append(gsets[f].getMember(i)).append('\t');
                    } else {
                        // @note comm out as not needed anymore
                        //buf.append("null\t");
                        buf.append("\t");
                    }
                }
    
                buf.append('\n');
                pw.print(buf.toString());
                pw.flush(); // IMP else doesnt do it properly -- east up last bit
            }
    
            doneExport();
        }
        finally {
            pw.close();
        }
    }

    private boolean fCheckForDuplicates = true;


    protected void setCheckForDuplicates(boolean check) {
        this.fCheckForDuplicates = check;
    }

    /**
     * Parses in a GeneSetMatrix files.
     * First name is blindly taken as col headers
     * second line is assumed to be colors
     * third line onwards data -- need NOT be equal number of rows
     * <p/>
     * all columns need to have same number of rows!
     * Else very hard to parse robustly. So use the null method.
     */
    public List parse(String sourcepath, InputStream is) throws Exception {

        startImport(sourcepath);

        final BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        try {
            Line currLine = nextLine(bin, 0);
            String currContent = currLine.getContent();
    
            // get the names from the first line
            final String[] gsetNames = parseNames(currContent);
    
            currLine = nextLine(bin, currLine.getLineNumber());
            currContent = currLine.getContent();
            final String[] namesEnglish = parseNames(currContent); // Or color
    
            final List[] members = new ArrayList[gsetNames.length];
            // init
            for (int i = 0; i < gsetNames.length; i++) {
                members[i] = new ArrayList();
            }
    
            // cant trim data lines as tabs are meaningful
            currLine = nextLine(bin, currLine.getLineNumber());
            currContent = currLine.getContent();
            int lineNum = 3;
            final int expected = gsetNames.length;
            while (currContent != null) {
                // added no need for padding Oct 2005
                final List fields = string2stringsV2(currContent, gsetNames.length);
    
                if (fields.size() != expected) {
                    throw new ParserException("Bad GMX format: expected " + expected
                            + " names but found " + fields.size(), currLine.getLineNumber(),
                            GseaWebResources.GMX_PARSER_ERROR_CODE);
                }
                for (int i = 0; i < gsetNames.length; i++) {
                    if (isNull(fields.get(i))) {
                        // dont add
                    } else {
                        members[i].add(fields.get(i));
                    }
                }
    
                currLine = nextLineTrimless(bin, currLine.getLineNumber()); /// so that last col(s) can be a tab
                currContent = currLine.getContent();
                lineNum++;
            }

            final GeneSet[] gsets = new GeneSet[members.length];
            for (int i = 0; i < members.length; i++) {
                //@note convention
                final String gsetName = sourcepath.concat("#").concat(gsetNames[i].toUpperCase()); // @note the UC'ing 
                gsets[i] = new FSet(gsetName, namesEnglish[i], members[i], fCheckForDuplicates);
            }

            doneImport();

            return unmodlist(new DefaultGeneSetMatrix(sourcepath, gsets));
        }
        finally {
            bin.close();
        }
    }

    private String[] parseNames(final String nameline) {
        return ParseUtils.string2strings(nameline, "\t", false);
    }
}    // End GmxParser
