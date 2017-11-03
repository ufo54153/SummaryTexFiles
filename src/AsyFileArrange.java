import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsyFileArrange {
    private File figureFolder;
    private JTextArea logField;
    private Logger log;

    /**
     * Create an instance of asy file arrange agent.
     * @param figureFolder the folder of asy files
     * @param logField a JTextArea to write log, this could be null, in which case no log will be output.
     */
    AsyFileArrange(File figureFolder, JTextArea logField) {
        this.figureFolder = figureFolder;
        this.logField = logField;
        this.log = Logger.getLog(logField);
    }

    /**
     * Create an instance of asy file arrange agent without log output.
     * @param figureFolder the folder of asy files
     */
    AsyFileArrange(File figureFolder) {
        this(figureFolder, null);
    }

    AsyFileArrange(String figurePath, JTextArea logField) {
        this(new File(figurePath), logField);
    }

    AsyFileArrange(String figurePath) {
        this(figurePath, null);
    }

    /**
     * Start the arrangement of asy files and the corresponding pdf files.
     */
    public void arrangeAsyFiles() {
        log.println("============================================Arrange start============================================");
        ArrayList<File> asyFileList = new ArrayList<>();
        ArrayList<File> folderList = new ArrayList<>();
        try {
            for (File file : figureFolder.listFiles()) {
                if (file.getName().endsWith(".asy")) asyFileList.add(file);
                if (file.isDirectory() && file.getName().matches("^size([\\d]+)$")) {
                    folderList.add(file);
                }
            }
            correctionFiles(folderList);
            moveAsyFiles(categorizeAsyFiles(asyFileList));
        } catch (NullPointerException e) {
            log.println("Figure folder: " + figureFolder.getName() + "does not exist or is empty.");
        }
    }

    private void correctionFiles(ArrayList<File> folderList) {
        for (File folder : folderList) {
            ArrayList<File> asyFileList = new ArrayList<>();
            for (File file : folder.listFiles()) {
                if (file.getName().endsWith(".asy")) asyFileList.add(file);
            }
            HashMap<String, ArrayList<File>> map = categorizeAsyFiles(asyFileList);
            if (map.size() >= 1) {
                moveAsyFiles(map);
            }
        }
    }

    /**
     * Move the asy files (and the corresponding pdf files) into the right folder, according to the map generated by
     * method categorizeAsyFiles.
     * @param map a map of size and List of asy file with certain size.
     */
    private void moveAsyFiles(HashMap<String, ArrayList<File>> map) {
        boolean flag;
        for (String size : map.keySet()) {
            File folder = new File(figureFolder.getPath() + File.separator + "size" + size);
            // if the folder does not exist, create it.
            if (!folder.exists()) {
                folder.mkdir();
            }
            // folder exists now
            for (File file : map.get(size)) {
                // move file to the corresponding folder
                String newPath = folder + File.separator;
                flag = file.renameTo(new File(newPath + file.getName()));
                if (flag) {
                    log.println("Moved file: " + file.getName() + " to: " + newPath);
                } else {
                    log.println("Move file: " + file.getName() + " failed.");
                }
                // move the pdf file (if exists) to the corresponding folder
                String filename = file.getPath();
                File pdfFile = new File(filename.substring(0, filename.length() - 3) + "pdf");
                if (pdfFile.exists()) {
                    flag = pdfFile.renameTo(new File(newPath + pdfFile.getName()));
                    if (flag) {
                        log.println("Moved file: " + pdfFile.getName() + " to: " + newPath);
                    }
                }
            }
        }
    }

    /**
     * Create a map from size to a list of files of that size.
     * @param asyFileList all the files need to categorize.
     * @return a categorized map
     */
    private HashMap<String, ArrayList<File>> categorizeAsyFiles(ArrayList<File> asyFileList) {
        HashMap<String, ArrayList<File>> map = new HashMap<>();
        BufferedReader reader = null;
        String lineContent;
        String key;
        for (File file : asyFileList) {
            try {
                reader = new BufferedReader(new FileReader(file));
                boolean sizeFound = false;
                while ((lineContent = reader.readLine()) != null) {
                    Pattern pattern = Pattern.compile("^size\\(([\\d]+)\\);$");
                    Matcher matcher = pattern.matcher(lineContent);
                    if (matcher.find()) {
                        sizeFound = true;
                        key = matcher.group(1);
                        if (!map.containsKey(key)) {
                            ArrayList<File> files = new ArrayList<>();
                            files.add(file);
                            map.put(key, files);
                        } else {
                            map.get(key).add(file);
                        }
                        break;
                    }
                }
                if (!sizeFound) {
                    log.println("Asy file: " + file.getPath() + " does not contains size information. This file has been ignored.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return map;
    }
}