package com.mim.attacker.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mim.attacker.customexception.LoggingException;
import org.springframework.stereotype.Service;

@Service
public class LoggingService {
    private final File csvFile;
    private  File jsonFile;
    private final FileWriter csvFileWriter;
    private  FileWriter jsonFileWriter;

    public LoggingService() {
        try {
            // Specify the path to the static directory
            File directory = new File("src/main/resources/static");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            csvFile = new File(directory, "intercepted_logs.csv");
            csvFileWriter = initializeFile(csvFile);
            jsonFile = new File(directory, "intercepted_logs.json");
            jsonFileWriter = initializeFile(jsonFile);

        } catch (IOException e) {
            throw new LoggingException("Failed to initialize logging service", e);
        }
    }

    private FileWriter initializeFile(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            initializeCsv();
        }
        return new FileWriter(file, true); // Append mode
    }

    private void initializeCsv() throws IOException {
        FileWriter initWriter = new FileWriter(csvFile);
        initWriter.append("Type,Method,URI,Status,Headers,Payload\n"); // Define CSV columns
        initWriter.flush();
        initWriter.close();
    }

    public void logToCsv(String type, String method, String uri, String status, String headers, String payload) {
        try {
            csvFileWriter.append(String.format("%s,%s,%s,%s,\"%s\",\"%s\"\n",
                    type, method, uri, status, headers.replace("\"", "\"\""), payload.replace("\"", "\"\""))); // Escape quotes
            csvFileWriter.flush();
        } catch (IOException e) {
            throw new LoggingException("Failed to log to CSV file", e);
        }
    }

    public void logToJsonFile(ObjectNode jsonNode) throws IOException {
        jsonFileWriter.write(jsonNode.toString() + ",\n"); // Append each JSON as a new line
        jsonFileWriter.flush(); // Flush after write to ensure data is written to file
    }


}
