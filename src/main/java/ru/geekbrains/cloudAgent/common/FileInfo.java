package ru.geekbrains.cloudAgent.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInfo {
    private String filename;
    private long length;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public FileInfo(Path path) {
        try {
            this.filename = path.getFileName().toString();
            if(Files.isDirectory(path)) {
                this.length = -1;
            } else {
                this.length = Files.size(path);
            }
        } catch (IOException e) {
           throw new RuntimeException("Something wrong with file: " + path.toAbsolutePath().toString());
        }
    }
}
