/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.*;
import edu.mit.broad.vdb.meg.AliasDb;
import edu.mit.broad.vdb.meg.Gene;
import gnu.trove.THashSet;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FilenameUtils;

import xapps.gsea.GseaWebResources;

/**
 * Parses a chip file
 */
public class ChipParser extends AbstractParser {

    // These are for the .chip parsing

    // Thes are for the TAF parsing
    // @maint see below if changing
    private static final String PROBE_SET_ID = "Probe Set ID";
    private static final String GENE_TITLE = "Gene Title";
    private static final String GENE_SYMBOL = "Gene Symbol";
    private static final String ALIASES = "Aliases";
    /**
     * Class Constructor.
     */
    public ChipParser() {
        super(Chip.class);
    }

    public void export(final PersistentObject pob, final File file) throws Exception {
        export((Chip) pob, file, true);
    }

    /**
     * Export a chip
     * Only works for export to .chip format
     *
     * @see "Above for format"
     */
    public void export(final Chip chip, final File file, final boolean withTitles) throws Exception {

        String[] colNames;

        if (withTitles) {
            colNames = new String[]{PROBE_SET_ID, GENE_SYMBOL, GENE_TITLE};
        } else {
            colNames = new String[]{PROBE_SET_ID, GENE_SYMBOL};
        }

        final PrintWriter pw = new PrintWriter(new FileOutputStream(file));
        try {
            for (int i = 0; i < colNames.length; i++) {
                pw.print(colNames[i]);
                if (i != colNames.length) {
                    pw.print('\t');
                }
            }
    
            pw.println();
    
            for (int r = 0; r < chip.getNumProbes(); r++) {
                Probe probe = chip.getProbe(r);
                pw.print(probe.getName());
                pw.print('\t');
    
                Gene gene = probe.getGene();
                String symbol = null;
                String title = null;
                if (gene != null) {
                    symbol = gene.getSymbol();
                    title = gene.getTitle();
                }
    
                if (symbol == null) {
                    symbol = Constants.NULL;
                }
    
                if (title == null) {
                    title = Constants.NULL;
                }
    
                pw.print(symbol);
    
                if (withTitles) {
                    pw.print('\t');
                    pw.print(title);
                }
    
                pw.println();
            }
        }
        finally {
            pw.close();
        }
        
        doneExport();
    }    // End export

    public List parse(String sourcepath, InputStream is) throws Exception {

        String sp_tmp = sourcepath.toUpperCase();

        if ((sp_tmp.indexOf(Constants.UNIGENE) != -1)
                || (sp_tmp.indexOf(Constants.GENE_SYMBOL) != -1)) { // @note hack maybe fix later
            return _parse_from_unigene_or_gene_symbol(sourcepath, is);
        } else if (sp_tmp.indexOf(Constants.SEQ_ACCESSION) != -1) {
            return _parse_from_seq_accession(sourcepath, is);
        } else if (sourcepath.endsWith(Constants.CHIP)) {
            return _parse_from_dot_chip(sourcepath, is);
        } else if (sourcepath.endsWith(Constants.CSV)) {
            // TODO: confirm we can actually accept CSV
            return _parse_from_csv(sourcepath, is);
        } else {
            throw new IllegalArgumentException("Unknown chip file type for parsing: " + sourcepath);
        }

    }

    /**
     * Parse from netaffx csv files
     */
    private List _parse_from_csv(String sourcepath, InputStream is) throws Exception {

        startImport(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        try {
            Line currLine = nextLine(bin, 0);
            String currContent = currLine.getContent();
    
            final List colHeaders = ParseUtils.string2stringsList_csv(currContent);
            int lineNumber = currLine.getLineNumber();
            final int ps_index = indexOf(PROBE_SET_ID, colHeaders, true, lineNumber);
            final int symbol_index = indexOf(GENE_SYMBOL, colHeaders, true, lineNumber);
            final int title_index = indexOf(GENE_TITLE, colHeaders, true, lineNumber);
    
            // save all rows so that we can determine how many rows exist
            // TODO: verify if this structure is necessary if not and reorganize to process as we go.
            Set probes = new THashSet();
            currLine = nextLine(bin, currLine.getLineNumber());
            currContent = currLine.getContent();
    
            while (currContent != null) {
                String[] fields = ParseUtils.string2strings_csv(currContent);
    
                String symbol = fields[symbol_index];
                String title = fields[title_index];
    
                symbol = NamingConventions.symbolize(symbol);
    
                if (title.equals("---")) {
                    title = null;
                }
    
                Probe probe = new SimpleProbe(fields[ps_index], symbol, title);
                probes.add(probe);
                currLine = nextLine(bin, currLine.getLineNumber());
                currContent = currLine.getContent();
            }
    
            final Probe[] keeps = (Probe[]) probes.toArray(new Probe[probes.size()]);
            final Chip chip = new FileInMemoryChip(FilenameUtils.getName(sourcepath), sourcepath, keeps);
    
            doneImport();
    
            return unmodlist(chip);
        }
        finally {
            bin.close();
        }
    }
    
    private boolean isSymbolProbes(String sourcepath) {
        return sourcepath.indexOf("Gene_Symbol") != -1;
    }

    private List _parse_from_unigene_or_gene_symbol(String sourcepath, InputStream is) throws Exception {

        startImport(sourcepath);

        boolean isSymbolProbes = isSymbolProbes(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        try {
            Line currLine = nextLine(bin, 0);
            String currContent = currLine.getContent();
    
            List colHeaders = ParseUtils.string2stringsList(currContent, "\t");
            int lineNumber = currLine.getLineNumber();
            int ps_index = indexOf(PROBE_SET_ID, colHeaders, true, lineNumber);
            int symbol_index = indexOf(GENE_SYMBOL, colHeaders, true, lineNumber);
            int title_index = indexOf(GENE_TITLE, colHeaders, true, lineNumber);
            int alias_index = indexOf(ALIASES, colHeaders, false, lineNumber); // @note optional
    
            // save all rows so that we can determine how many rows exist
            List probesList = new ArrayList();
            currContent = nextLine(bin);
            Set symbols = new HashSet();
    
            while (currContent != null) {
                final String[] fields = ParseUtils.string2stringsV2(currContent);
                if (fields.length != 3 && fields.length != 4) {
                    throw new ParserException("Bad UNIGENE or GENE_SYMBOL CHIP format: expecting 3 or 4 fields but found: " + fields.length,
                            currLine.getLineNumber(), GseaWebResources.CHIP_PARSER_ERROR_CODE);
                }
    
                String probeName = fields[ps_index];
    
                // make the probe a symbol too
                if (isSymbolProbes) {
                    probeName = NamingConventions.symbolize(probeName);
                }
    
                if (probeName != null && !symbols.contains(probeName)) {
                    String symbol = fields[symbol_index];
                    String title = fields[title_index];
                    symbol = NamingConventions.symbolize(symbol);
    
                    if (title != null && title.equals("---")) {
                        title = null;
                    }
    
                    Set aliases = null;
                    if (alias_index != -1 && alias_index < fields.length) {
                        aliases = ParseUtils.string2stringsSet(fields[alias_index], Constants.INTRA_FIELD_DELIM_S, false);
                    }
    
                    Probe probe = new SimpleProbe3(symbol, title, aliases);
                    probesList.add(probe);
                    symbols.add(symbol);
                }
    
                currLine = nextLine(bin, currLine.getLineNumber());
                currContent = currLine.getContent();
            }
    
            // clean up by removing all aliases that are actually valid gene symbols
            final SimpleProbe3[] probes = (SimpleProbe3[]) probesList.toArray(new SimpleProbe3[probesList.size()]);
            for (int i = 0; i < probes.length; i++) {
                probes[i].removeAnyAliasesThatMatch(symbols);
            }
            final Chip chip = new FileInMemoryChip(FilenameUtils.getName(sourcepath), sourcepath, probes);
            log.info("Parsed from unigene / gene symbol: " + probes.length);
            doneImport();
    
            return unmodlist(chip);
        }
        finally {
            bin.close();
        }
    }

    private List _parse_from_dot_chip(String sourcepath, InputStream is) throws Exception {

        startImport(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));

        try {
            Line currLine = nextLine(bin, 0);
            String currContent = currLine.getContent();

            List colHeaders = ParseUtils.string2stringsList(currContent, "\t");
            int lineNumber = currLine.getLineNumber();
            int ps_index = indexOf(PROBE_SET_ID, colHeaders, true, lineNumber);
            int symbol_index = indexOf(GENE_SYMBOL, colHeaders, true, lineNumber);
            int title_index = indexOf(GENE_TITLE, colHeaders, true, lineNumber);
    
            // save all rows so that we can determine how many rows exist
            List probesList = new ArrayList();
            currContent = nextLine(bin);
            Set names = new HashSet();
    
            while (currContent != null) {
                final String[] fields = ParseUtils.string2strings(currContent, "\t", true);
    
                String probeName = fields[ps_index];
    
                if (probeName != null && !names.contains(probeName)) {
                    String symbol = fields[symbol_index];
                    symbol = NamingConventions.symbolize(symbol);
    
                    String title = null;
                    try {
                        title = fields[title_index];
                        if (title != null && title.equals("---")) {
                            title = null;
                        }
                    } catch (Throwable t) {
    
                    }
    
                    Probe probe = new SimpleProbe(probeName, symbol, title);
                    probesList.add(probe);
                }
    
                if (probeName != null) {
                    names.add(probeName);
                }
    
                currLine = nextLine(bin, currLine.getLineNumber());
                currContent = currLine.getContent();
            }
    
            final Probe[] probes = (Probe[]) probesList.toArray(new Probe[probesList.size()]);
            final Chip chip = new FileInMemoryChip(FilenameUtils.getName(sourcepath), sourcepath, probes);
    
            log.info("Parsed from dotchip : " + probes.length);
            doneImport();
    
            return unmodlist(chip);
        }
        finally {
            bin.close();
        }
    }


    // ditto as symbol or unigene except no titles in the file to save memory
    // so fill them in from the gene symbol chip as needed
    // also add the Gene_Symbol probes always
    // Then add aliases the same way
    private List _parse_from_seq_accession(final String sourcepath, final InputStream is) throws Exception {

        startImport(sourcepath);

        final BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        try {
            Line currLine = nextLine(bin, 0);
            String currContent = currLine.getContent();
    
            List colHeaders = ParseUtils.string2stringsList(currContent, "\t");
            int lineNumber = currLine.getLineNumber();
            int ps_index = indexOf(PROBE_SET_ID, colHeaders, true, lineNumber);
            int symbol_index = indexOf(GENE_SYMBOL, colHeaders, true, lineNumber);
            final Chip chip_gene_symbol = VdbRuntimeResources.getChip_Gene_Symbol();
    
            // save all rows so that we can determine how many rows exist
            List probes = new ArrayList();
            Set probeNamesAdded = new HashSet();
    
            // First add all gene symbols ditto
    
            for (int i = 0; i < chip_gene_symbol.getNumProbes(); i++) {
                probes.add(chip_gene_symbol.getProbe(i));
                probeNamesAdded.add(chip_gene_symbol.getProbe(i).getName());
            }
    
            log.debug("# of seq probes (from symbol): " + probes.size());
    
            currLine = nextLine(bin, currLine.getLineNumber());
            currContent = currLine.getContent();
            while (currContent != null) {
                String[] fields = ParseUtils.string2strings(currContent, "\t", true);
    
                final String symbol = NamingConventions.symbolize(fields[symbol_index]);
                final String probeName = fields[ps_index];
                if (probeNamesAdded.contains(probeName) == false) { // @note add only if its not already in
                    probeNamesAdded.add(probeName);
                    final Probe probe = new SimpleProbe2(probeName, symbol, chip_gene_symbol); // @note always null for title
                    probes.add(probe);
                }
    
                currLine = nextLine(bin, currLine.getLineNumber());
                currContent = currLine.getContent();
            }
    
            log.debug("# of seq probes: " + probes.size());
    
            // Then add aliases the same way
            final AliasDb aliasdb = VdbRuntimeResources.getAliasDb();
            final Probe[] alias_probes = aliasdb.getAliasesAsProbes();
            for (int i = 0; i < alias_probes.length; i++) { // @note add an alias only if it is not already in
                if (probeNamesAdded.contains(alias_probes[i].getName()) == false) {
                    probes.add(alias_probes[i]);
                }
            }
    
            log.debug("FINAL # of seq probes: " + probes.size());
    
            final Probe[] keeps = (Probe[]) probes.toArray(new Probe[probes.size()]);
            final Chip chip = new FileInMemoryChip(FilenameUtils.getName(sourcepath), sourcepath, keeps);
    
            log.info("Parser from Seq_Accession: " + keeps.length);
            doneImport();
    
            return unmodlist(chip);
        }
        finally {
            bin.close();
        }
    }
}    // End of class ChipParser