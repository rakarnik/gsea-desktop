/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.RankedListGenerators;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.RankedList;
import gnu.trove.TFloatArrayList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import xapps.gsea.GseaWebResources;

public class RankedListParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public RankedListParser() {
        super(RankedList.class);
    }

    /**
     * Export a Dataset to file in gct format
     * Only works with Datasets
     *
     * @see "Above for format"
     */
    public void export(PersistentObject pob, File file) throws Exception {

        RankedList rl = (RankedList) pob;
        PrintWriter pw = startExport(pob, file);
        try {
            for (int i = 0; i < rl.getSize(); i++) {
                //System.out.println(">>>> " + m.getMember(i));
                pw.println(rl.getRankName(i) + "\t" + rl.getScore(i));
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

        BufferedReader buf = new BufferedReader(new InputStreamReader(is));
        try {
            // TODO: confirm comment lines in RNK.
            Line currLine = nextLine(buf, 0);
            String currContent = currLine.getContent();
            sourcepath = NamingConventions.removeExtension(new File(sourcepath).getName());
    
            List names = new ArrayList();
            TFloatArrayList floats = new TFloatArrayList();
            int cnt = 0;
    
            while (currContent != null) {
    
                String[] fields = ParseUtils.string2strings(currContent, "\t", false); // DONT USE SPACES
                if (fields.length != 2) {
                    throw new ParserException("Bad RNK format: expected 2 fields but found " + fields.length, currLine.getLineNumber(),
                            GseaWebResources.RNK_PARSER_ERROR_CODE);
                }
                
                boolean doParse = true;
    
    
                String nameField = fields[0];
                String rankField = fields[1];
                if (StringUtils.equalsIgnoreCase("Name", nameField) || StringUtils.equalsIgnoreCase("Rank", rankField)) {
                    doParse = false;
                }
    
                if (cnt == 0) { // @note sometimes the first line is a header -- ignore that error
                    try {
                        Float.parseFloat(rankField);
                    } catch (Throwable t) {
                        doParse = false; // skip line on error
                    } finally {
                        cnt++;
                    }
                }
    
                if (doParse) {
    
                    names.add(nameField);
                    try {
                      floats.add(Float.parseFloat(rankField));
                    }
                    catch (NumberFormatException nfe) {
                        throw new ParserException("Bad RNK Format: could not parse rank field '"
                                + rankField + "'  as a float value.", currLine.getLineNumber(), nfe,
                                GseaWebResources.RNK_PARSER_ERROR_CODE);
                    }
                }
    
                currLine = nextLine(buf, currLine.getLineNumber());
                currContent = currLine.getContent();
            }

            doneImport();
            
            // changed march 2006 for the sorting
            RankedList rl = RankedListGenerators.createBySorting(sourcepath, (String[]) names.toArray(new String[names.size()]), floats.toNativeArray(), SortMode.REAL, Order.DESCENDING);

            return unmodlist(rl);
            // return unmodlist(new DefaultRankedList(objname, (String[]) names.toArray(new String[names.size()]), floats));
        }
        finally {
            buf.close();
        }
    }
}    // End of class RankedListParser