package bots;

import exceptions.ApplicationException;
import exceptions.BusinessException;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import utility.Constant;
import utility.ExcelUtils;
import utility.Log;
import utility.Util;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;

public class BoxSyncPerformer {
    ExcelUtils fetchInput;
    XSSFSheet sheet;
    public void boxPerformer() throws BusinessException, ApplicationException, IOException {
        fetchInput = new ExcelUtils();
        List<Path> allFiles;
        int numOfInputRows;
        int i;
        XSSFRow row;
        String upc;
        Boolean duplicateFile;

        Util.StartLog();
        //Launch the Box Drive Software
        Util.launchBox();
        /* Validate if the current date folder is present in Processed Folder
         * Throw Business Exception if the Current date folder is not present
         * Fetch all the files from Current date folder for further processing*/
        allFiles = Util.fetchAllFilesInProcessedFolder();
        //Read the Queue Item
        try {
            sheet = fetchInput.readInputExcel(Constant.INPUT_EXCEL_FILE_PATH,Constant.INPUT_SHEET_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        numOfInputRows = sheet.getLastRowNum() - sheet.getFirstRowNum();
        Log.info("Number of Rows in the input file - " + numOfInputRows);
        if (numOfInputRows > 0) {
            for (i = 1; i <= numOfInputRows; i++) {
                row=sheet.getRow(i);
                upc = Util.fetchUpc(row);
                for (Path path : allFiles) {
                    if (path.toString().contains(upc)) {
                        System.out.println(path);
                        duplicateFile  = Util.checkDuplicates(path);
                        if (duplicateFile){
                            continue;
                        }
                        Util.copyFileToBox(path);
                    }
                }
            }
            Util.FolderMerger();
        }

        else {
            System.out.println("Input Excel is empty");
            Log.info("Input Excel is empty");
        }

    }
    public static void main(String[] args) throws BusinessException, ApplicationException, IOException {
        new BoxSyncPerformer().boxPerformer();
    }
}
