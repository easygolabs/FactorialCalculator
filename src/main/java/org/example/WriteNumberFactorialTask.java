package org.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class WriteNumberFactorialTask implements Runnable {

    private static final String OUTPUT_TXT = "output.txt";
    private final ExecutorService executor;
    private final BlockingQueue<Integer> queue;
    private final ConcurrentMap<Integer, BigInteger> map;
    private final Logger logger;

    WriteNumberFactorialTask(
            ExecutorService executor,
            BlockingQueue<Integer> queue,
            ConcurrentMap<Integer, BigInteger> map,
            Logger logger) {
        this.executor = executor;
        this.queue = queue;
        this.map = map;
        this.logger = logger;
    }

    /**
     * Writes results to file until the queue is empty or executor is terminated.
     */
    public void run() {
        try (PrintWriter writer = new PrintWriter(OUTPUT_TXT, "UTF-8")) {
            while (!executor.isTerminated() || !queue.isEmpty()) {
                writeNumberFactorial(queue.poll(), writer);
            }
        } catch (IOException e) {
            logger.severe("Error while writing to file. " + e.getMessage());
        }
    }

    /**
     * @param number - number to calculate factorial
     * @param writer - writer to write results to file
     */
    private void writeNumberFactorial(Integer number, PrintWriter writer) {
        if (number == null) {
            logger.severe("The queue is empty.");
            return;
        }

        BigInteger factorial = map.get(number);

        if (factorial == null) {
            logger.severe("The factorial is null.");
            return;
        }

        if (number == FactorialCalculator.INVALID_INPUT) {
            writer.println("Factorial can't be calculated as an empty string|character|text was provided");
            return;
        }
        writer.println(number + " = " + factorial);
    }
}
