package utility;

import com.fasterxml.jackson.annotation.JsonProperty;
import exceptions.ApplicationException;
import exceptions.BusinessException;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import model.PojoClass;
import org.apache.log4j.xml.DOMConfigurator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Util {
    public static void setSpecificDataToPojo(String specificData) {
        try {
            // Create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();

            // Deserialize JSON string to Java object
            Product product = objectMapper.readValue(specificData, Product.class);

            // Create an instance of PojoClass
            PojoClass pojo = new PojoClass();

            // Set values from Product object to PojoClass using setters
            pojo.setStrVendorFileName1(product.getVendorFileName1());
            pojo.setStrDmmVal(product.getDmmGrpId());
            pojo.setStrColor(product.getColor());
            pojo.setStrClassName(product.getClassName());
            pojo.setStrVPN(product.getVendorStyleVpn());
            pojo.setStrbrandName(product.getBrandName());
            pojo.setStrSVS(product.getSvs());
            pojo.setStrImageUploadDate(product.getImageUploadDate());
            pojo.setStrUPC(product.getPhotoOverrideUpc());
            pojo.setStrOH(product.getEcomOhUnits());
            pojo.setStrOO(product.getEcomOoUnits());
            pojo.setStrGmmVal(product.getGmmDivId());
            pojo.setStrStyleDesc(product.getStyleDescription());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateTransactionStatus(int counterCopied, String status, int id, String workitemId, String queueName, String specificData,String upc,List<String> lstNewFileNames) {
        if (counterCopied>0){
            DatabaseUtil.updateDatabase("status",status,id);
            /*Insert the data into the Workhorse Queue*/
            DatabaseUtil.insertDataIntoDb(Constant.SQL_WORKITEM, workitemId, queueName,
                    "TheBay_Workhorse_Performer", "New", specificData, 0);
            String updateQuery = "UPDATE RPADev.TheBay_DigOps_Metarename_Box.workitem Set comment = '"+"Number of Images Copied - "+counterCopied+",New Vendor File Name - "+String.join(", ", lstNewFileNames)+"' WHERE" +
                    " state ='TheBay_Workhorse_Performer' And detail like '%"+upc+"%' And work_item_id = '"+workitemId+"'";
            DatabaseUtil.updateDatabaseCustom(updateQuery);
            }else {
            /*Images not processed , Postpone the Transaction by 30 minutes*/
            DatabaseUtil.updateDatabase("status","New",id);
            DatabaseUtil.updateDatabase("output",Util.postponeTime(),id);
        }
    }

    public static class Product {
        @JsonProperty("Vendor File Name 1")
        public String vendorFileName1;

        @JsonProperty("DMM/GRP ID")
        public String dmmGrpId;

        @JsonProperty("Color")
        public String color;

        @JsonProperty("Class Name")
        public String className;

        @JsonProperty("Vendor Style/VPN")
        public String vendorStyleVpn;

        @JsonProperty("Brand Name")
        public String brandName;

        @JsonProperty("SVS")
        public String svs;

        @JsonProperty("Image Upload Date in Workhorse")
        public String imageUploadDate;

        @JsonProperty("Photo Override UPC")
        public String photoOverrideUpc;

        @JsonProperty("Ecom OH Units")
        public String ecomOhUnits;

        @JsonProperty("Ecom OO Units")
        public String ecomOoUnits;

        @JsonProperty("GMM/DIV ID")
        public String gmmDivId;

        @JsonProperty("Style Description")
        public String styleDescription;

        // Getters and setters
        public String getVendorFileName1() {
            return vendorFileName1;
        }
        public String getDmmGrpId() {
            return dmmGrpId;
        }

        public String getColor() {
            return color;
        }

        public String getClassName() {
            return className;
        }

        public String getVendorStyleVpn() {
            return vendorStyleVpn;
        }

        public String getBrandName() {
            return brandName;
        }

        public String getSvs() {
            return svs;
        }

        public String getImageUploadDate() {
            return imageUploadDate;
        }

        public String getPhotoOverrideUpc() {
            return photoOverrideUpc;
        }

        public String getEcomOhUnits() {
            return ecomOhUnits;
        }

        public String getEcomOoUnits() {
            return ecomOoUnits;
        }

        public String getGmmDivId() {
            return gmmDivId;
        }

        public String getStyleDescription() {
            return styleDescription;
        }
    }

    public static void StartLog() {
        DOMConfigurator.configure("log4j.xml"); //Configure Log 4j
        Log.startTestCase("MetaRename Box Sync");
    }

    public static void launchBox() throws ApplicationException {
        try {
            // Specify the path to the executable or application file
            String filePath = Constant.BOX_EXECUTABLEFILE_PATH;
            File boxPath = new File(Constant.BOX_PATH);

            // Create a file object for the executable or application
            File exeFile = new File(filePath);

            // Check if the file exists and is executable

            if (exeFile.exists() && exeFile.canExecute() && !boxPath.exists()) {
                // Open the file using the system default application
                Desktop.getDesktop().open(exeFile);
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
        } catch (IOException e) {
            Log.error("Error launching application: " + e.getMessage());
            throw new ApplicationException("Error launching application: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Path> fetchAllFilesInProcessedFolder() throws BusinessException {
        List<Path> fileList = null;
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM_d_yyyy");
        String formattedCurrentDate = date.format(formatter);
        File currentDatePath = new File(Constant.METARENAME_PATH + formattedCurrentDate);
        if (currentDatePath.exists()) {
            System.out.println("Current date folder exists in Processed folder");
            Log.info("Current date folder exists in Processed folder");
            try {
                fileList = new ArrayList<>();
                Files.walk(Paths.get(currentDatePath.toURI()), FileVisitOption.FOLLOW_LINKS)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(fileList::add);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.error("Current date folder doesn't exists in Processed folder - " + Constant.METARENAME_PATH);
            throw new BusinessException("Current date folder doesn't exists in Processed folder - " + Constant.METARENAME_PATH);
        }
        return fileList;
    }
    public static void copyFileToBox(Path path) throws IOException {
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M_MMMM_yyyy");
        String formattedCurrentDate = date.format(formatter);
        int indexOfProcessed = path.toString().indexOf("Processed");
        String fromDatePath = path.toString().substring(indexOfProcessed + 9);
        String boxPath = Constant.HOME + Constant.BOXDRIVE_PATH + formattedCurrentDate + "_HB_VPI\\" + fromDatePath;

        File destinationFile = new File(boxPath);
        if (destinationFile.exists()) {
            System.out.println(destinationFile + " already exists.");
        } else {
                Files.createDirectories(destinationFile.getParentFile().toPath());
        }
            Files.copy(path, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied successfully to: " + destinationFile);
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

    public static void FolderMerger() throws IOException {
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M_MMMM_yyyy");
        DateTimeFormatter formatterCurrentDate = DateTimeFormatter.ofPattern("MMMM_d_yyyy");
        String formattedMonthDate = date.format(formatter);
        String formattedCurrDate = date.format(formatterCurrentDate);
        String boxPath = Constant.HOME + Constant.BOXDRIVE_PATH + formattedMonthDate + "_HB_VPI\\" + formattedCurrDate;

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

                try {
                    // Check if the original folder exists
                    if (!Files.exists(originalFolder)) {
                        System.out.println("Original folder does not exist.");
                        return;
                    }

                    // Get the list of files in the duplicate folder
                    List<Path> duplicateFiles = getFilesInFolder(duplicateFolder);

                    // Move each file from the duplicate folder to the original folder
                    for (Path file : duplicateFiles) {
                        Files.move(file, originalFolder.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    }
    
                    // Delete the duplicate folder if it is empty after moving files
                    Files.deleteIfExists(duplicateFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
    public static String postponeTime (){
        LocalDateTime dateTime = LocalDateTime.now().withNano(0);
        dateTime = dateTime.plusMinutes(Constant.POSTPONE_MINUTES);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(dateTimeFormatter);
    }
    public static LocalDateTime currentTime(){
        LocalDateTime dateTime = LocalDateTime.now().withNano(0);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = dateTime.format(dateTimeFormatter);
        return LocalDateTime.parse(formattedDateTime,dateTimeFormatter);
    }
}

