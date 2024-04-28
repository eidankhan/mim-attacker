package com.mim.attacker.util;

public class ResponseAnalyzer {

    public static boolean containsSensitiveData(String responseData) {
        // Example check for sensitive data or error messages
        return responseData.contains("error") || responseData.contains("confidential");
    }
}
