package bots;

import exceptions.ApplicationException;
import exceptions.BusinessException;
import model.PojoClass;
import utility.*;
import java.nio.file.Path;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BoxSyncPerformer {

    PojoClass pojoClass;
    List<Path> allFiles;
    String status = null;
    String upc;
    Boolean duplicateFile;
    ResultSet resultSet;
    String specificData;
    int id ;
    int retry;
    String workitemId;
    String queueName;
    String output;
    String createTimestamp;
    DateTimeFormatter finalFormatter;
    DateTimeFormatter inputFormatter;
    LocalDateTime existingPostponeVal;
    LocalDateTime currentTimeVal;
    LocalDateTime formattedCreatedTime;
    String strCreatedTime;
    LocalDateTime finalCreatedTime;
    LocalDateTime postponeLimit;
    public void boxPerformer() throws BusinessException, ApplicationException{
        pojoClass = new PojoClass();

        //Initialize the logs
        Util.StartLog();

        //Launch the Box Drive Software
        Util.launchBox();

        /* Validate if the current date folder is present in Processed Folder
         * Throw Business Exception if the Current date folder is not present
         * Fetch all the files from Current date folder for further processing*/
        allFiles = Util.fetchAllFilesInProcessedFolder();

        //Read the Queue Item
        resultSet = DatabaseUtil.fetchDataFromDb(Constant.SQL_JDBC_URL, Constant.SQL_USER_NAME, Constant.SQL_PASS_WORD,Constant.FETCH_QUEUE_iTEM_QUERY);
        if (resultSet != null) {
            try {
                 /*Process the ResultSet here , for each row in database
                 until all the items are processed*/
                while (resultSet.next()) {
                    // Retrieve column values from the Database
                    specificData = resultSet.getString("detail");
                    id = resultSet.getInt("id");
                    retry = resultSet.getInt("retry");
                    workitemId = resultSet.getString("work_item_id");
                    queueName = resultSet.getString("queue_name");
                    output = resultSet.getString("output");
                    createTimestamp = resultSet.getString("create_timestamp");

                    /*Output column in SQL Contains the Postpone data,
                     * below logic is to check if the Transaction is within the postpone time*/
                    if (output != null) {
                        finalFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                        existingPostponeVal = LocalDateTime.parse(output, finalFormatter);
                        currentTimeVal = Util.currentTime();
                        formattedCreatedTime = LocalDateTime.parse(createTimestamp,inputFormatter);
                        strCreatedTime = formattedCreatedTime.format(finalFormatter);
                        finalCreatedTime = LocalDateTime.parse(strCreatedTime,finalFormatter);
                        int compareResult = existingPostponeVal.compareTo(currentTimeVal);

                        /*If the compareResult is >0 , it implies that the postponed time is greater
                         * than the current time , and hence this transaction will be skipped*/
                        if (compareResult > 0) {
                            System.out.println("Not processing this Transaction as it's postponed ," +
                                    "postponed until - "+existingPostponeVal);
                            continue;
                        }
                        /*Transaction will be failed if the Postpone is exceeding 6 days
                         * Hence the below Transaction creation time + 6 days
                         * to check if the current time exceeded the Postpone Limit*/
                        postponeLimit = finalCreatedTime.plusDays(6);
                        System.out.println("Postpone Limit - "+postponeLimit);
                        System.out.println("Current Time - "+currentTimeVal);
                        int compareResultThreschold = postponeLimit.compareTo(currentTimeVal);
                        if (compareResultThreschold < 0) {
                            String reason = "Failing this Transaction as it has exceeded the Threshold ," +
                                    "postpone Limit - "+postponeLimit;
                            System.out.println(reason);
                            DatabaseUtil.updateDatabase("status","Failed",id);
                            DatabaseUtil.updateDatabase("reason",reason,id);
                            continue;
                        }
                    }
                    //Set status to inprogress
                    DatabaseUtil.updateDatabase("status", "InProgress", id);

                     /*Deserialize the Specific data values from Database
                     Set each values to Pojo Setters */
                    Util.setSpecificDataToPojo(specificData);
                    upc = pojoClass.getStrUPC();
                    int counterCopied = 0;
                    for (Path path : allFiles) {
                        if (path.toString().contains(upc)) {
                            duplicateFile  = Util.checkDuplicates(path);
                            if (duplicateFile){
                                continue;
                            }
                            try {
                                counterCopied = counterCopied+1;
                                Util.copyFileToBox(path);
                                status = "Success";
                            } catch (IOException e) {
                                status = "Failed";
                                break;
                            }
                        }
                    }
                    /*If any Image is copied , Update the status accordingly
                    * Store the Count of number of images processed in "Comment column"*/
                    if (counterCopied>0){
                        DatabaseUtil.updateDatabase("status",status,id);
                        DatabaseUtil.updateDatabase("comment",
                                "Number of Images uploaded to Box - "+counterCopied,id);
                        /*Insert the data into the Workhorse Queue*/
                        DatabaseUtil.insertDataIntoDb(Constant.SQL_WORKITEM, workitemId, queueName,
                                "TheBay_Workhorse_Performer", "New", specificData, retry);
                    }else {
                        /*Images not processed , Postpone the Transaction by 30 minutes*/
                        DatabaseUtil.updateDatabase("status","New",id);
                        DatabaseUtil.updateDatabase("output",Util.postponeTime(),id);
                    }

                }
                System.out.println("No More transactions in the Queue to Process");
            } catch (SQLException e) {
                System.err.println("Error processing ResultSet: " + e.getMessage());
            } finally {
                try {
                    // Close the ResultSet, statement, and connection
                    resultSet.close();
                } catch (SQLException e) {
                    System.err.println("Error closing ResultSet: " + e.getMessage());
                }
            }
        }
        /*Merge the duplicate images in Box folder*/
        try {
            Util.FolderMerger();
        } catch (IOException e) {
            throw new RuntimeException("Unable to Merge the duplicate folders in Box folder due to "+e);
        }
    }
    public static void main(String[] args) throws BusinessException, ApplicationException{
        new BoxSyncPerformer().boxPerformer();
    }
}
