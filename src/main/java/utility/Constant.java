package utility;

public class Constant {
    public static final int MAX_RETRY = 2;
    public static final String DB_WORK_ITEM_TABLE_NAME = "RPADev.TheBay_DigOps_Metarename_Box.workitem";
    public static final String BOX_SYNC_PERFORMER_QUEUE_NAME = "BoxSyncPerformer";
    public static final String BOX_PATH = Util.HOME+"\\Box\\THE BAY VENDOR PROVIDED IMAGES";
    public static final String BOX_EXECUTABLEFILE_PATH = "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\Box Drive.lnk";
    public static final String PROCESSED_FOLDER_PATH = "\\\\10.124.234.5\\FileServer\\File Server\\HB-Meta Rename\\Processed\\";
    public static final String BOXDRIVE_PATH = "\\Box\\THE BAY VENDOR PROVIDED IMAGES\\RPA Test Folder\\";
    public static final String SQL_JDBC_URL = "jdbc:sqlserver://thebay-rds-uipath-dev.cyeuvydpkw6m.us-east-1.rds.amazonaws.com:1433;databaseName=TheBayUipathOrchestratorDev;encrypt=true;trustServerCertificate=true";
    public static final String SQL_USER_NAME = "bayrpasqladmin";
    public static final String SQL_PASS_WORD = "chlp7#r!b=sWa9&7";
    public static final String FETCH_QUEUE_iTEM_QUERY = "SELECT * FROM RPADev.TheBay_DigOps_Metarename_Box.workitem WHERE status = 'New' AND state = 'BoxSyncPerformer'";
    public static final int POSTPONE_MINUTES = 30;
    public static final String SQL_WORKITEM = "INSERT INTO RPADev.TheBay_DigOps_Metarename_Box.workitem (work_item_id, queue_name, state, status, detail, retry) VALUES (?, ?, ?, ?, ?, ?)";
    }
