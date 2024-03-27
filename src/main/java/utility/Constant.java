package utility;

public class Constant {
    public static final String BOX_PATH = "C:\\Users\\H896753\\Box\\THE BAY VENDOR PROVIDED IMAGES";
    public static final String BOX_EXECUTABLEFILE_PATH = "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\Box Drive.lnk";
    public static final String METARENAME_PATH = "C:\\Users\\H896753\\Documents\\Processed\\";
    public static final String HOME = System.getProperty("user.home");
    public static final String BOXDRIVE_PATH = "\\Box\\THE BAY VENDOR PROVIDED IMAGES\\RPA Test Folder\\";
    public static final String SQL_JDBC_URL = "jdbc:sqlserver://thebay-rds-uipath-dev.cyeuvydpkw6m.us-east-1.rds.amazonaws.com:1433;databaseName=TheBayUipathOrchestratorDev;encrypt=true;trustServerCertificate=true";
    public static final String SQL_USER_NAME = "bayrpasqladmin";
    public static final String SQL_PASS_WORD = "chlp7#r!b=sWa9&7";
    public static final String FETCH_QUEUE_iTEM_QUERY = "SELECT * FROM RPADev.TheBay_DigOps_Metarename_Box.workitem WHERE status = 'New' AND state = 'BoxSyncPerformer'";
}
