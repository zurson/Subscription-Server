package org.example.api.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileHandler {

    private final String filename;


    public FileHandler(String filename) {
        String baseDir = System.getProperty("user.dir");
        this.filename = Paths.get(baseDir, filename).toString();
//        this.filePath = filePath;
    }


    public String readFile() throws IOException {
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(filename));
            return new String(fileBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error during reading file: " + e.getMessage());
            throw e;
        }
    }


    public void writeFile(String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Error during saving file: " + e.getMessage());
            throw e;
        }
    }


    public String getFilename() {
        return new File(filename).getName();
    }


}
