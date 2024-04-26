package com.mim.attacker.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.mim.attacker.customexception.LoggingException;
import org.springframework.stereotype.Service;

@Service
public class LoggingService {
    private final File file;
    private final FileWriter writer;

    public LoggingService() {
        try {
            // Specify the path to the static directory
            File directory = new File("src/main/resources/static");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            file = new File(directory, "intercepted_logs.csv");
            if (!file.exists()) {
                file.createNewFile();
                initializeCsv();
            }
            writer = new FileWriter(file, true); // Append mode
        } catch (IOException e) {
            throw new LoggingException("Failed to initialize logging service", e);
        }
    }

    private void initializeCsv() throws IOException {
        FileWriter initWriter = new FileWriter(file);
        initWriter.append("Type,Method,URI,Status,Headers,Payload\n"); // Define CSV columns
        initWriter.flush();
        initWriter.close();
    }

    public void logToCsv(String type, String method, String uri, String status, String headers, String payload) {
        try {
            writer.append(String.format("%s,%s,%s,%s,\"%s\",\"%s\"\n",
                    type, method, uri, status, headers.replace("\"", "\"\""), payload.replace("\"", "\"\""))); // Escape quotes
            writer.flush();
        } catch (IOException e) {
            throw new LoggingException("Failed to log to CSV file", e);
        }
    }
}
