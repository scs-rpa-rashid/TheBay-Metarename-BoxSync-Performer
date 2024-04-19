package bots;

import exceptions.ApplicationException;
import exceptions.BusinessException;
import model.PojoClass;
import utility.*;
import java.nio.file.Path;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class BoxSyncPerformer {
    PojoClass pojoClass;
    List<Path> lstAllFilesInProcessedFolder;
    String strUpc;
    ResultSet resultSet;
    String strSpecificData;
    int intId;
    int intRetry;
    String strWorkitemId;
    String strQueueName;
    String strOutput;
    String strCreateTimestamp;
    String strState;

    public void run() throws BusinessException, ApplicationException{
        try{
            pojoClass = new PojoClass();
            //Initialize the logs
            Util.StartLog();
            //Launch the Box Drive Software
            Util.launchBox();
            /* Validate if the current date folder is present in Processed Folder
             * Throw Business Exception if the Current date folder is not present
             * Fetch all the files from Current date folder for further processing*/
            lstAllFilesInProcessedFolder = Util.fetchAllFilesInProcessedFolder();
            //Read the Queue Item
            resultSet = DatabaseUtil.fetchDataFromDb(Constant.SQL_JDBC_URL, Constant.SQL_USER_NAME, Constant.SQL_PASS_WORD,Constant.FETCH_QUEUE_iTEM_QUERY);
            if (resultSet != null) {
                try {
                 /*Process the ResultSet here , for each row in database
                 until all the items are processed*/
                    while (resultSet.next()) {
                        // Retrieve column values from the Database
                        strSpecificData = resultSet.getString("detail");
                        intId = resultSet.getInt("id");
                        intRetry = resultSet.getInt("retry");
                        strWorkitemId = resultSet.getString("work_item_id");
                        strQueueName = resultSet.getString("queue_name");
                        strOutput = resultSet.getString("output");
                        strCreateTimestamp = resultSet.getString("create_timestamp");
                        strState = resultSet.getString("state");

                        /*Output column in SQL Contains the Postpone data,
                         * below logic is to check if the Transaction is within the postpone time*/
                        boolean skipTransaction =  Util.checkIfTransactionPostponed(strOutput,strCreateTimestamp,intId);
                        if (skipTransaction){
                            /*Skip this transaction if the QueueItem is either Postponed or marked as Failed*/
                            continue;
                        }
                        /*if (strOutput != null) {
                            finalFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                            existingPostponeVal = LocalDateTime.parse(strOutput, finalFormatter);
                            currentTimeVal = Util.currentTime();
                            formattedCreatedTime = LocalDateTime.parse(strCreateTimestamp,inputFormatter);
                            strCreatedTime = formattedCreatedTime.format(finalFormatter);
                            finalCreatedTime = LocalDateTime.parse(strCreatedTime,finalFormatter);
                            int compareResult = existingPostponeVal.compareTo(currentTimeVal);
                            *//*If the compareResult is >0 , it implies that the postponed time is greater
                         * than the current time , and hence this transaction will be skipped
                         * If current time > postpone time then the transaction will be picked*//*
                            if (compareResult > 0) {
                                System.out.println("Not processing this Transaction as it's postponed ," +
                                        "postponed until - "+existingPostponeVal);
                                Log.info("Not processing this Transaction as it's postponed ," +
                                        "postponed until - "+existingPostponeVal);
                                continue;
                            }
                            *//*Transaction will be failed if the Postpone is exceeding 6 days
                         * Hence the below Transaction creation time + 6 days
                         * to check if the current time exceeded the Postpone Limit*//*
                            postponeLimit = finalCreatedTime.plusDays(6);
                            int compareResultThreschold = postponeLimit.compareTo(currentTimeVal);
                            if (compareResultThreschold < 0) {
                                String reason = "Business Exception - Failing this Transaction as it has exceeded the Threshold ," +
                                        "postpone Limit - "+postponeLimit;
                                System.out.println(reason);
                                Log.info(reason);
                                DatabaseUtil.updateDatabase("status","Failed", intId);
                                DatabaseUtil.updateDatabase("reason",reason, intId);
                                continue;
                            }
                        }*/
                        //Set status to inprogress
                        DatabaseUtil.updateDatabase("status", "InProgress", intId);
                     /*Deserialize the Specific data values from Database
                     Set each values to Pojo Setters */
                        Util.setSpecificDataToPojo(strSpecificData);
                        strUpc = pojoClass.getStrUPC();
                        Log.info("Processing UPC - "+ strUpc);
                        /*Copy Images to Box if present in processed folder , mark status as successful,
                         * Postpone the transaction if no Images are available in processed folder
                         * Fail the transaction if postpone is exceeded for 6 days*/
                        Util.copyImagesAndUpdateStatus(lstAllFilesInProcessedFolder,strUpc,intId
                                ,strWorkitemId,strQueueName,strSpecificData);
                    /*int counterCopied = 0;
                    List<String> lstNewFileNames = new ArrayList<>();
                    for (Path path : lstAllFilesInProcessedFolder) {
                        if (path.toString().contains(strUpc)) {
                            boolDuplicateFile = Util.checkDuplicates(path);
                            if (boolDuplicateFile){
                                continue;
                            }
                            try {
                                counterCopied = counterCopied+1;
                                Util.copyFileToBox(path);
                                lstNewFileNames.add(path.getFileName().toString());
                                strStatus = "Success";
                            } catch (IOException e) {
                                strStatus = "Failed";
                                break;
                            }
                        }
                    }
                    *//*If any Image is copied , Update the status accordingly
                         * Store the Count of number of images processed in "Comment column"*//*
                    Util.updateTransactionStatus(counterCopied, strStatus, intId, strWorkitemId, strQueueName, strSpecificData, strUpc,lstNewFileNames);*/
                    }
                    System.out.println("No More transactions in the Queue to Process");
                    Log.info("No More transactions in the Queue to Process");
                } catch (SQLException e) {
                    System.err.println("Error processing ResultSet: " + e.getMessage());
                    Log.error("Error processing ResultSet: " + e.getMessage());
                } finally {
                    try {
                        // Close the ResultSet, statement, and connection
                        resultSet.close();
                    } catch (SQLException e) {
                        System.err.println("Error closing ResultSet: " + e.getMessage());
                        Log.error("Error closing ResultSet: " + e.getMessage());
                    }
                }
            }
            /*Merge the duplicate images in Box folder*/
            try {
                Util.FolderMerger();
            } catch (IOException e) {
                Log.error("Unable to Merge the duplicate folders in Box folder due to " + e);
                throw new RuntimeException("Unable to Merge the duplicate folders in Box folder due to " + e);
            }
        } catch (Exception e) {
            Util.updateDbStatusAndRetry(resultSet, intRetry, intId, strWorkitemId, strQueueName,e, strSpecificData, strState);
        }
    }
    public static void main(String[] args) throws BusinessException, ApplicationException{
        new BoxSyncPerformer().run();
    }
}
