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
    ExcelUtils fetchInput;
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
    String state;
    String output;
    String createTimestamp;
    public void boxPerformer() throws BusinessException, ApplicationException{
        fetchInput = new ExcelUtils();
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
                    state = resultSet.getString("state");
                    output = resultSet.getString("output");
                    createTimestamp = resultSet.getString("create_timestamp");

                    if (output != null) {
                        DateTimeFormatter finalFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                        LocalDateTime existingPostponeVal = LocalDateTime.parse(output, finalFormatter);
                        LocalDateTime currentTimeVal = Util.currentTime();
                        LocalDateTime formattedCreatedTime = LocalDateTime.parse(createTimestamp,inputFormatter);
                        String strCreatedTime = formattedCreatedTime.format(finalFormatter);
                        LocalDateTime finalCreatedTime = LocalDateTime.parse(strCreatedTime,finalFormatter);
                        int compareResult = existingPostponeVal.compareTo(currentTimeVal);
                        if (compareResult > 0) {
                            System.out.println("Not processing this Transaction as it's postponed ," +
                                    "postponed until - "+existingPostponeVal);
                            continue;
                        }
                        LocalDateTime postponeLimit = finalCreatedTime.plusSeconds(6);
                       /* LocalDateTime postponeLimit = finalCreatedTime.plusSeconds(6);*/
                        System.out.println("Postpone Limit - "+postponeLimit);
                        System.out.println("Current Time - "+currentTimeVal);
                        int compareResultThreschold = postponeLimit.compareTo(currentTimeVal);
                        if (compareResultThreschold < 0) {
                            System.out.println("Failing this Transaction as it has exceeded the Threshold ," +
                                    "postpone Limit - "+postponeLimit);
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
                    if (counterCopied>0){
                        DatabaseUtil.updateDatabase("status",status,id);
                        DatabaseUtil.updateDatabase("comment",
                                "Number of Images uploaded to Box - "+String.valueOf(counterCopied),id);
                        DatabaseUtil.insertDataIntoDb(Constant.SQL_WORKITEM, workitemId, queueName, "TheBay_Workhorse_Performer", "New", specificData, retry);
                    }else {
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
