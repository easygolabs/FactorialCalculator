package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        logger.info("Enter the thread pool size:");

        Reader inputStreamReader = new InputStreamReader(System.in);

        Calculator calculator = new FactorialCalculator(getPoolSize(inputStreamReader));
        try {
            calculator.execute();
        } catch (Exception e) {
            logger.severe("Error while executing calculator. " + e.getMessage());
        }
    }

    private static int getPoolSize(Reader inputStreamReader) {
        int poolSize = 1;

        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            poolSize = Integer.parseInt(reader.readLine());
        } catch (NumberFormatException e) {
            logger.info("Pool size should be a number. Set 1 by default. " + e.getMessage());
        } catch (IOException e) {
            logger.severe("Error while reading pool size. " + e.getMessage());
        }

        if (poolSize <= 0) {
            logger.info("Pool size is empty. Set 1 by default.");
            poolSize = 1;
        }
        return poolSize;
    }
}