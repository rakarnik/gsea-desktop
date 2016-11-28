/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.gsea;

import edu.mit.broad.genome.parsers.AuxUtils;

/**
 * @author Aravind Subramanian
 */
public class GseaWebResources {

    private final static String BROAD_FTP_SERVER = "ftp.broadinstitute.org";
    private final static String GSEA_FTP_SERVER = "ftp.broadinstitute.org";
    private final static String GSEA_FTP_SERVER_USERNAME = "anonymous";
    private final static String GSEA_FTP_SERVER_PASSWORD = "gsea@broadinstitute.org";
    private final static String GSEA_FTP_SERVER_BASE_DIR = "/pub/gsea";
    private final static String GSEA_FTP_SERVER_CHIPFILES_SUB_DIR = "annotations";
    private final static String GSEA_FTP_SERVER_GENESETS_SUB_DIR = "gene_sets";

    public static final int CHIP_PARSER_ERROR_CODE = 5001;
    public static final int CLS_PARSER_ERROR_CODE = 5002;
    public static final int DATAFRAME_PARSER_ERROR_CODE = 5003;
    public static final int EDB_PARSER_ERROR_CODE = 5004;
    public static final int GCT_PARSER_ERROR_CODE = 5005;
    public static final int GMT_PARSER_ERROR_CODE = 5006;
    public static final int GMX_PARSER_ERROR_CODE = 5007;
    public static final int MISC_PARSER_ERROR_CODE = 5008;
    public static final int PCL_PARSER_ERROR_CODE = 5009;
    public static final int RES_PARSER_ERROR_CODE = 5010;
    public static final int RNK_PARSER_ERROR_CODE = 5011;
    public static final int RPT_PARSER_ERROR_CODE = 5012;
    public static final int SDF_PARSER_ERROR_CODE = 5013;
    public static final int TXT_PARSER_ERROR_CODE = 5014;

    public static String getGseaFTPServer() {
        return GSEA_FTP_SERVER;
    }

    public static String getGseaFTPServerUserName() {
        return GSEA_FTP_SERVER_USERNAME;
    }

    public static String getGseaFTPServerPassword() {
        return GSEA_FTP_SERVER_PASSWORD;
    }

    public static String getBroadFTPBase() {
        return "ftp://" + BROAD_FTP_SERVER + GSEA_FTP_SERVER_BASE_DIR;
    }

    public static String getGseaFTPServerChipDir() {
        return GSEA_FTP_SERVER_BASE_DIR + "/" + GSEA_FTP_SERVER_CHIPFILES_SUB_DIR;
    }

    public static String getGseaFTPServerGeneSetsDir() {
        return GSEA_FTP_SERVER_BASE_DIR + "/" + GSEA_FTP_SERVER_GENESETS_SUB_DIR;
    }

    public static String getGseaBaseURL() {
        if (edu.mit.broad.genome.Conf.isDebugMode()) {
            return "http://dev.broadinstitute.org/gsea";
        } else {
            return "http://www.broadinstitute.org/gsea";
        }
    }

    public static String getGseaEmail() {
        return "gsea@broadinstitute.org";
    }

    public static String getGseaURLDisplayName() {
        return "www.broadinstitute.org/GSEA";
    }

    public static String getGseaHelpURL() {
        return getGseaBaseURL() + "/wiki";
    }

    public static String getGseaContactURL() {
        return getGseaBaseURL() + "/contact.jsp";
    }

    public static String getGseaDataFormatsHelpURL() {
        return getGseaBaseURL() + "/wiki/index.php/Data_formats";
    }

    public static String getArrayAnnotationsURL() {
        return getBroadFTPBase() + "/annotations";
    }

    public static String getGseaExamplesURL() {
        return getGseaBaseURL() + "/datasets.jsp";
    }

    public static String getGeneSetURL(String gsetName) {
        gsetName = AuxUtils.getAuxNameOnlyNoHash(gsetName);
        return getGseaBaseURL() + "/msigdb/cards/" + gsetName + ".html";
    }

} // End classes AppWebResources

