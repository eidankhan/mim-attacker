package com.mim.attacker.util;

public class DataManipulator {

    // Example of manipulating the amount by a certain factor
    public static double manipulateAmount(double amount, double factor) {
        return amount * factor; // Increase or decrease the amount by a factor
    }

    // Example of redirecting the transfer to another account
    public static String redirectAccount(String toAccountId) {
        String modifiedAccountId = "10000005";
        System.out.println("Modifying account " + toAccountId+" to " + modifiedAccountId);
        return modifiedAccountId; // Replace with an actual account id in real scenarios
    }
}
