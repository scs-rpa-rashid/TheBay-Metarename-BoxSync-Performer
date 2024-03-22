package utility;

import exceptions.ApplicationException;
import exceptions.BusinessException;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class Util {
    public static DecimalFormat decimalFormat = new DecimalFormat("#");

    public static String fetchUpc(XSSFRow row) {
        String upc;
        try {
            upc = Util.decimalFormat.format(row.getCell(Constant.UPC_COLUMN_NUMBER).getNumericCellValue());
        } catch (Exception e) {
            upc = row.getCell(Constant.UPC_COLUMN_NUMBER).toString();
        }
        return upc;
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
                System.out.println("Application launch triggered successfully.");
            } else {
                System.out.println("File does not exist , cannot be executed or Box Drive is Already Opened");
            }
            do {
                System.out.println("Waiting for the Box Drive Application launch to complete");
                Thread.sleep(5000);
            } while (!boxPath.exists());
            System.out.println("Box Drive Application Launched Successfully");
        } catch (IOException e) {
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
            try {
                fileList = new ArrayList<>();
                Files.walk(Paths.get(currentDatePath.toURI()), FileVisitOption.FOLLOW_LINKS)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(fileList::add);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new BusinessException("Current date folder doesn't exists in Processed folder - " + Constant.METARENAME_PATH);
        }
        return fileList;
    }

    public static void copyFileToBox(Path path) {
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M_MMMM_yyyy");
        String formattedCurrentDate = date.format(formatter);
        int indexOfProcessed = path.toString().indexOf("Processed");
        String fromDatePath = path.toString().substring(indexOfProcessed + 9);
        String boxPath = Constant.HOME + Constant.BOXDRIVE_PATH + formattedCurrentDate + "_HB_VPI\\" + fromDatePath;

        File destinationFile = new File(boxPath);
        if (destinationFile.exists()) {
            System.out.println(destinationFile + " already exists. Overwriting.");
        } else {
            try {
                Files.createDirectories(destinationFile.getParentFile().toPath());
            } catch (IOException e) {
                throw new RuntimeException("Error creating directories: " + e.getMessage());
            }
        }

        try {
            Files.copy(path, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied successfully to: " + destinationFile);
        } catch (IOException e) {
            throw new RuntimeException("Error copying file: " + e.getMessage());
        }
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
}

