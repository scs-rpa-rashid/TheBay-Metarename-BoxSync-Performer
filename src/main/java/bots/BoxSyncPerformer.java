package bots;

import com.scs.exceptionutil.BusinessException;
import com.scs.model.QueueItem;
import com.scs.queueutils.QueueItemUtils;
import utility.*;
import java.nio.file.Path;
import java.util.List;
public class BoxSyncPerformer {
    List<Path> lstAllFilesInProcessedFolder;
    String strSpecificData;
    int intId;
    int intRetry;
    String strWorkItemId;
    String strQueueName;
    String strOutput;
    String strCreateTimestamp;
    String strState;
    Boolean queueItemPresent;
    QueueItem queueItem;
    QueueItemUtils queueItemUtils;
    public void run() throws Exception {
        try{
            queueItemPresent = true;
            queueItemUtils = new QueueItemUtils();
            //Initialize the logs
            Util.StartLog();
            //Launch the Box Drive Software
            Util.launchBox();
            /* Validate if the current date folder is present in Processed Folder
             * Throw Business Exception if the Current date folder is not present
             * Fetch all the files from Current date folder for further processing*/
            lstAllFilesInProcessedFolder = Util.fetchAllFilesInProcessedFolder();
            while (queueItemPresent) {
                //Read the Queue Item
                queueItem = queueItemUtils.getQueueItem(Constant.DB_WORK_ITEM_TABLE_NAME,Constant.BOX_SYNC_PERFORMER_QUEUE_NAME);
                // Retrieve column values from the Database
                strSpecificData = queueItem.getDetail();
                intId = queueItem.getId();
                intRetry = queueItem.getRetry();
                strWorkItemId = queueItem.getWorkItemId();
                strQueueName = queueItem.getQueueName();
                strState = queueItem.getState();
                strOutput = queueItem.getOutput();
                strCreateTimestamp = queueItem.getCreateTimeStamp();
                if (strSpecificData != null) {
                     /*Deserialize the Specific data values from Database
                     Set each values to Pojo Setters */
                    Util.setSpecificDataToPojo(strSpecificData);
                    /*Output column in SQL Contains the Postpone data,
                     * below logic is to check if the Transaction is within the postpone time*/
                    boolean skipTransaction = Util.checkIfTransactionPostponed(strOutput, strCreateTimestamp, intId);
                    if (skipTransaction) {
                        /*Skip this transaction if the QueueItem is either Postponed or marked as Failed*/
                        queueItemUtils.updateQueueItem(Constant.DB_WORK_ITEM_TABLE_NAME,List.of("status"),List.of("New"),intId);
                        continue;
                    }
                    /*Copy Images to Box if present in processed folder , mark status as successful,
                     * Postpone the transaction if no Images are available in processed folder
                     * Fail the transaction if postpone is exceeded for 6 days*/
                    Util.copyImagesAndUpdateStatus(lstAllFilesInProcessedFolder,intId
                            , strWorkItemId, strSpecificData);
                }else{
                    queueItemPresent =false;
                    System.out.println("No More transactions in the Queue to Process");
                    Log.info("No More transactions in the Queue to Process");
                }
            }
            System.out.println("No More transactions in the Queue to Process");
            Log.info("No More transactions in the Queue to Process");
            /*Merge the duplicate images in Box folder*/
            Util.FolderMerger();
        }
        catch (BusinessException be){
            throw new BusinessException(be.getMessage());
        }
        catch (Exception e) {
            Util.updateDbStatusAndRetry(queueItem, intRetry, intId, strWorkItemId,e, strSpecificData, strState);
        }
    }
    public static void main(String[] args) throws Exception {
        new BoxSyncPerformer().run();
    }
}
