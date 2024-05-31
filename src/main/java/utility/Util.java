package utility;

import bots.BoxSyncPerformer;
import com.scs.fileutils.FileUtil;
import com.scs.model.QueueItem;
import com.scs.dateutils.*;
import com.scs.exceptionutil.*;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import com.scs.queueutils.QueueItemUtils;
import com.scs.sysutils.CaptureScreenshot;
import model.InputDataModel;
import model.Product;
import org.apache.log4j.xml.DOMConfigurator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Util {
    static QueueItemUtils queueItemUtils = new QueueItemUtils();
    public static void setSpecificDataToPojo(String specificData) {
        try {
            // Create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            // Deserialize JSON string to Java object
            Product product = objectMapper.readValue(specificData, Product.class);
            // Create an instance of PojoClass
            InputDataModel objInputDataModel = new InputDataModel();
            // Set values from Product object to PojoClass using setters
            objInputDataModel.setStrVendorFileName1(product.getVendorFileName1());
            objInputDataModel.setStrDmmVal(product.getDmmGrpId());
            objInputDataModel.setStrColor(product.getColor());
            objInputDataModel.setStrClassName(product.getClassName());
            objInputDataModel.setStrVPN(product.getVendorStyleVpn());
            objInputDataModel.setStrbrandName(product.getBrandName());
            objInputDataModel.setStrSVS(product.getSvs());
            objInputDataModel.setStrImageUploadDate(product.getImageUploadDate());
            objInputDataModel.setStrUPC(product.getPhotoOverrideUpc());
            objInputDataModel.setStrOH(product.getEcomOhUnits());
            objInputDataModel.setStrOO(product.getEcomOoUnits());
            objInputDataModel.setStrGmmVal(product.getGmmDivId());
            objInputDataModel.setStrStyleDesc(product.getStyleDescription());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void updateTransactionStatus(int counterCopied, String status, int id, String workitemId, String specificData,String upc,List<String> lstNewFileNames) throws ApplicationException {
        if (counterCopied>0){
            System.out.println("Transaction Successful");
            Log.info("Transaction Successful");
            queueItemUtils.updateQueueItem(Constant.DB_WORK_ITEM_TABLE_NAME,List.of("status"),List.of(status),id);
            //DatabaseUtil.updateDatabase("status",status,id);
            /*Insert the data into the Workhorse Queue*/
            queueItemUtils.addQueueItem(Constant.DB_WORK_ITEM_TABLE_NAME,List.of("work_item_id","queue_name","state","status","detail","retry"),
                    List.of(workitemId,"TheBay_Workhorse_Performer","TheBay_Workhorse_Performer","New",specificData,0));
            String updateQuery = "UPDATE RPADev.TheBay_DigOps_Metarename_Box.workitem Set comment = '"+"Number of Images Copied - "+counterCopied+":New Vendor File Name - "+String.join(", ", lstNewFileNames)+"' WHERE" +
                    " state ='TheBay_Workhorse_Performer' And detail like '%"+upc+"%' And work_item_id = '"+workitemId+"'";
            DatabaseUtil.updateDatabaseCustom(updateQuery);
        }else {
            /*Images not processed , Postpone the Transaction by 30 minutes*/
            System.out.println("Transaction postponed");
            Log.info("Transaction postponed");
            queueItemUtils.updateQueueItem(Constant.DB_WORK_ITEM_TABLE_NAME,List.of("status","postpone"),List.of("New",Util.postponeTime()),id);
        }
    }
    public static boolean checkIfTransactionPostponed(String strCreateTimestamp, int intId) throws ApplicationException {
        System.out.println("Processing Transaction - "+InputDataModel.strUPC);
        /*Eliminate milliseconds from create time stamp*/
        int lastDotIndex = strCreateTimestamp.lastIndexOf(".");
        strCreateTimestamp = strCreateTimestamp.substring(0,lastDotIndex);
        boolean skipTheLoop = false;
        DateTimeFormatter finalFormatter = DateUtil.dateFormatter("yyyy-MM-dd HH:mm:ss");
        LocalDateTime currentTimeVal = LocalDateTime.parse(DateUtil.currentDateTime("yyyy-MM-dd HH:mm:ss"),finalFormatter);
        LocalDateTime formattedCreatedTime = LocalDateTime.parse(strCreateTimestamp,finalFormatter);
        String strCreatedTime = formattedCreatedTime.format(finalFormatter);
        LocalDateTime finalCreatedTime = LocalDateTime.parse(strCreatedTime,finalFormatter);
        /*Transaction will be failed if the Postpone is exceeding 6 days
         * Hence the below Transaction creation time + 6 days
         * to check if the current time exceeded the Postpone Limit*/
        LocalDateTime postponeLimit = finalCreatedTime.plusDays(6);
        if (currentTimeVal.isAfter(postponeLimit)) {
            String reason = "Business Exception - Failing this Transaction as it has exceeded the Threshold ," +
                    "postpone Limit - "+postponeLimit+" current time - "+currentTimeVal;
            System.out.println(reason);
            Log.info(reason);
            queueItemUtils.updateQueueItem(Constant.DB_WORK_ITEM_TABLE_NAME,List.of("status","reason"),List.of("Failed",reason),intId);
            skipTheLoop = true;
        }
        return skipTheLoop;
    }
    public static void copyImagesAndUpdateStatus(List<Path> lstAllFilesInProcessedFolder,int intId,
                                                 String strWorkItemId,String strSpecificData) throws ApplicationException {
        String strUpc = InputDataModel.strUPC;
        int counterCopied = 0;
        String strStatus = null;
        List<String> lstNewFileNames = new ArrayList<>();
        for (Path path : lstAllFilesInProcessedFolder) {
            if (path.toString().contains(strUpc)) {
                Boolean boolDuplicateFile = Util.checkDuplicates(path);
                if (boolDuplicateFile){
                    continue;
                }
                try {
                    counterCopied = counterCopied+1;
                    Util.copyFileToBox(path);
                    lstNewFileNames.add(path.getFileName().toString());
                    strStatus = "Successful";
                } catch (Exception e) {
                    strStatus = "Failed";
                    break;
                }
            }
        }
        /*If any Image is copied , Update the status accordingly
         * Store the Count of number of images processed in "Comment column"*/
        Util.updateTransactionStatus(counterCopied, strStatus, intId, strWorkItemId, strSpecificData, strUpc,lstNewFileNames);
    }
    public static final String HOME = System.getProperty("user.home");
    public static void StartLog() {
        DOMConfigurator.configure("log4j.xml"); //Configure Log 4j
        Log.startTestCase("MetaRename Box Sync");
    }
    public static void launchBox() throws ApplicationException {
        try {
            // Specify the path to the executable or application file
            String filePath = Constant.BOX_EXECUTABLEFILE_PATH;
            String cmdPrmptBoxLaunch = Constant.COMMAND_PROMPT_SCRIPT;
            File boxPath = new File(Constant.BOX_PATH);
            // Create a file object for the executable or application
            File exeFile = new File(filePath);
            // Check if the file exists and is executable
            if (exeFile.exists() && exeFile.canExecute() && !boxPath.exists()) {
                // Open the file using the system default application
                //Desktop.getDesktop().open(exeFile);
                Process process = Runtime.getRuntime().exec(cmdPrmptBoxLaunch);
                /*wait for launch to complete*/
                process.waitFor();
                CaptureScreenshot.captureScreenshot(Constant.PATH_EXCEPTION_SCREENSHOT+"testBox.png");
                Log.info("Application launch triggered successfully.");
                System.out.println("Application launch triggered successfully.");
            } else {
                Log.info("File does not exist , cannot be executed or Box Drive is Already Opened");
                System.out.println("File does not exist , cannot be executed or Box Drive is Already Opened");
            }
            do {
                Log.info("Waiting for the Box Drive Application launch to complete");
                System.out.println("Waiting for the Box Drive Application launch to complete");
                Thread.sleep(5000);
            } while (!boxPath.exists());
            Log.info("Box Drive Application Launched Successfully");
            System.out.println("Box Drive Application Launched Successfully");
        } catch (Exception e) {
            Log.error("Error launching application: " + e.getMessage());
            throw new ApplicationException("Error launching application: " + e.getMessage());
        }
    }
    public static List<Path> fetchAllFilesInProcessedFolder() throws BusinessException, ApplicationException {
        List<Path> fileList;
        String formattedCurrentDate = DateUtil.currentDateTime("MMMM_d_yyyy");
        File currentDatePath = new File(Constant.PROCESSED_FOLDER_PATH + formattedCurrentDate);
        if (currentDatePath.exists()) {
            System.out.println("Current date folder exists in Processed folder");
            Log.info("Current date folder exists in Processed folder");
            try {
                fileList = new ArrayList<>();
                Files.walk(Paths.get(currentDatePath.toURI()), FileVisitOption.FOLLOW_LINKS)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(fileList::add);
            } catch (Exception e) {
                Log.error("Error while fetching files from Processed folder due to : " + e.getMessage());
                throw new ApplicationException("Error while fetching files from Processed folder due to : " + e.getMessage());
            }
        } else {
            Log.error("Current date folder doesn't exists in Processed folder - " + Constant.PROCESSED_FOLDER_PATH);
            throw new BusinessException("Current date folder doesn't exists in Processed folder - " + Constant.PROCESSED_FOLDER_PATH);
        }
        return fileList;
    }
    public static void copyFileToBox(Path path) throws Exception{
        String formattedCurrentDate = DateUtil.currentDateTime("M_MMMM_yyyy");
        int indexOfProcessed = path.toString().indexOf("Processed");
        String fromDatePath = path.toString().substring(indexOfProcessed + 9);
        String boxPath = HOME + Constant.BOXDRIVE_PATH + formattedCurrentDate + "_HB_VPI\\" + fromDatePath;
        File destinationFile = new File(boxPath);
        /*If Destination folder does not exist - create*/
        if (!destinationFile.getParentFile().exists()) {
            System.out.println(destinationFile.getParentFile() + " does not exists , creating the file");
            Files.createDirectories(destinationFile.getParentFile().toPath());
        }
        FileUtil.copyFile(path.toString(),destinationFile.getParentFile().toString());
    }
    public static Boolean checkDuplicates(Path path) {
        boolean boolVal = false;
        String patternString = "\\((.*?)\\)";
        if (path.toString().contains("(") && path.toString().contains(")")) {
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(path.toString());
            while (matcher.find()) {
                String valueInsideParentheses = matcher.group(1);
                System.out.println("Value inside parentheses: " + valueInsideParentheses);
                // Check if the value inside parentheses is numeric
                if (isNumeric(valueInsideParentheses)) {
                    System.out.println("Value inside parentheses is numeric.");
                    boolVal = true;
                } else {
                    System.out.println("Value inside parentheses is not numeric.");
                    boolVal = false;
                }
            }
        } else if (path.toString().toLowerCase().contains("copy")) {
            boolVal = true;
        }
        return boolVal;
    }
    public static boolean isNumeric (String str){
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static void FolderMerger() throws Exception {
        try {
        String formattedMonthDate = DateUtil.currentDateTime("M_MMMM_yyyy");
        String formattedCurrDate = DateUtil.currentDateTime("MMMM_d_yyyy");
        String boxPath = HOME + Constant.BOXDRIVE_PATH + formattedMonthDate + "_HB_VPI\\" + formattedCurrDate;
        // Get the list of files in the duplicate folder
        List<Path> filesInDateFolder;
        List<Path> divisionFolders = new ArrayList<>();
        filesInDateFolder = getFilesInFolder(Path.of(boxPath));
        for (Path imageFiles : filesInDateFolder) {
            divisionFolders.add(imageFiles.getParent());
        }
        // Convert the list to a set to remove duplicates
        Set<Path> uniquePaths = new HashSet<>(divisionFolders);
        // Convert the set back to a list
        List<Path> uniqueDivisionFolders = new ArrayList<>(uniquePaths);
        for (Path division : uniqueDivisionFolders) {
            if (division.getFileName().toString().toLowerCase().startsWith("division")
                    && division.toString().contains("(")){
                // Specify the paths of the folders to be merged
                Path duplicateFolder = Paths.get(division.toUri());
                int indexOfDot = division.toString().indexOf("(");
                Path originalFolder = Paths.get(division.toString().substring(0,indexOfDot));

                    // Check if the original folder exists
                    if (!Files.exists(originalFolder)) {
                        System.out.println("Original folder does not exist.");
                        return;
                    }
                    // Get the list of files in the duplicate folder
                    List<Path> duplicateFiles = getFilesInFolder(duplicateFolder);
                    // Move each file from the duplicate folder to the original folder
                    for (Path file : duplicateFiles) {
                        FileUtil.moveFile(file.toString(),originalFolder.resolve(file.getFileName()).toString());
                    }
                    // Delete the duplicate folder if it is empty after moving files
                    FileUtil.deleteFile(duplicateFiles.toString());
                }
            }
        }catch (Exception e) {
            throw new Exception(e);
        }
    }
    // Helper method to get the list of files in a folder
    private static List<Path> getFilesInFolder(Path folder) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                files.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }
    public static String postponeTime() {
        String currentDateTimeString = DateUtil.currentDateTime("yyyy-MM-dd HH:mm:ss");
        try {
            // Parse the current date-time string to LocalDateTime
            LocalDateTime parsedDateTime = LocalDateTime.parse(currentDateTimeString, DateUtil.dateFormatter("yyyy-MM-dd HH:mm:ss"));

            // Assume the current date-time string is in the system's default time zone
            ZonedDateTime parsedDateTimeWithZone = parsedDateTime.atZone(ZoneId.systemDefault());

            // Convert the parsed date-time to IST
            ZonedDateTime parsedDateTimeIST = parsedDateTimeWithZone.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));

            // Postpone the time by the specified minutes
            ZonedDateTime postponedDateTime = parsedDateTimeIST.withNano(0).plusMinutes(Constant.POSTPONE_MINUTES);

            // Format the postponed date-time to the desired format
            return postponedDateTime.format(DateUtil.dateFormatter("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            System.err.println("Parsing error: " + e.getMessage());
            return null;
        }
    }
    public static void updateDbStatusAndRetry(QueueItem queueItem, int retry, int id, String workitemId, Exception e, String specificData, String state) throws Exception {
        if (queueItem.getDetail()!= null) {
            if (retry < Constant.MAX_RETRY) {
                System.out.println("Transaction Retried");
                Log.info("Transaction Retried");
                queueItemUtils.updateQueueItem(Constant.DB_WORK_ITEM_TABLE_NAME,List.of("status","reason"),List.of("Retried",e.getMessage().replace("'", "")),id);
                retry = retry + 1;
                queueItemUtils.addQueueItem(Constant.DB_WORK_ITEM_TABLE_NAME,List.of("work_item_id","queue_name","state","status","detail","retry"),
                        List.of(workitemId,state,state,"New",specificData,retry));
                new BoxSyncPerformer().run();
                Log.error(e.getMessage());
                /*throw new ApplicationException(e.getMessage());*/
            } else {
                System.out.println("Transaction Failed");
                Log.info("Transaction Failed");
                queueItemUtils.updateQueueItem(Constant.DB_WORK_ITEM_TABLE_NAME,List.of("status","reason"),List.of("Failed",e.getMessage().replace("'", "")),id);
                new BoxSyncPerformer().run();
                Log.error(e.getMessage());
                /*throw new ApplicationException(e.getMessage());*/
            }
        }else {
            Log.info("Not Retrying as the Queue Item is not yet Fetched");
            throw new ApplicationException(e.getMessage());
        }
    }
}

