package utility;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelUtils {
    FileInputStream fileInputStream;
    XSSFWorkbook wb;
    XSSFSheet Sheet;
    File Inputfile;

    public XSSFSheet readInputExcel(String filePath,String sheetName) throws IOException {
        Inputfile = new File(filePath); //Pathname to the Input Excel
        fileInputStream = new FileInputStream(Inputfile);
        wb = new XSSFWorkbook(fileInputStream); //Object of the workbook
        Sheet = wb.getSheet(sheetName); //Fetch the sheet name
        return Sheet;
    }

}
