package org.example;

import java.io.*;
import java.math.BigInteger;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class FactorialCalculator implements Calculator {

    public static final int INVALID_INPUT = 0;
    private static final String INPUT_TXT = "src/main/resources/input.txt";
    private static final int MAX_QUEUE_CAPACITY = 130000;
    private static final int MAX_MINUTES_AWAIT_TIMEOUT = 15;
    private static final Logger logger = Logger.getLogger(FactorialCalculator.class.getName());
    public static final int MILLIS_IN_SECOND = 1000;
    public static final int FACTORIALS_PER_SECOND = 100;
    private final int poolSize;
    private final int eachCalculationSleepMillis;
    private final ConcurrentMap<Integer, BigInteger> map = new ConcurrentHashMap<>();
    private final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(MAX_QUEUE_CAPACITY);
    private final Semaphore semaphore = new Semaphore(100);
    private ExecutorService calculationExecutor = null;

    /**
     * @param poolSize - number of threads to calculate factorial
     */
    public FactorialCalculator(int poolSize) {
        this.poolSize = poolSize;
        this.eachCalculationSleepMillis = poolSize * MILLIS_IN_SECOND / FACTORIALS_PER_SECOND;
    }

    /**
     * Creates executors to read file, calculate factorials and to write results to file.
     */
    public void execute() {
        calculationExecutor = Executors.newFixedThreadPool(poolSize);

        ExecutorService readExecutor = Executors.newSingleThreadExecutor();
        
        readExecutor.submit(() -> {
            long start = System.nanoTime();

            try (BufferedReader fileReader = new BufferedReader(new FileReader(INPUT_TXT))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    processFileLine(line);
                }
            } catch (FileNotFoundException e) {
                logger.severe("File is not found!" + e.getMessage());
            } catch (IOException e) {
                logger.severe("Error while reading file. " + e.getMessage());
            } catch (InterruptedException e) {
                logger.severe("Putting number to queue was interrupted. " + e.getMessage());
                Thread.currentThread().interrupt();
            } finally {
                shutdownExecutor(calculationExecutor);
                logger.info("Calculation executor was shutdown.");
            }

            writeResults();

            long elapsed = System.nanoTime() - start;
            logger.info("Elapsed sec = " + TimeUnit.NANOSECONDS.toSeconds(elapsed));
        });

        shutdownExecutor(readExecutor);
    }

    /**
     * @param line - line from file
     * @throws InterruptedException - if putting number to queue was interrupted
     *                              Puts number to queue to save an order and submits task to calculate factorial.
     */
    private void processFileLine(String line) throws InterruptedException {
        int number;

        try {
            number = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            number = INVALID_INPUT;
        }

        queue.put(number);
        submitCalculateFactorialTask(number);
    }

    /**
     * @param number - number to calculate factorial
     *               Calculates 100 factorials per second by multiple threads and puts result to map.
     */
    private void submitCalculateFactorialTask(int number) {
        if (map.containsKey(number)) {
            return;
        }
        calculationExecutor.submit(() -> {
            long startTime = System.currentTimeMillis();

            try {
                semaphore.acquire();

                BigInteger factorial = calculateFactorial(number);
                map.put(number, factorial);

                sleep(startTime);
            } catch (InterruptedException e) {
                logger.severe("Thread was interrupted while calculating factorial. " + e.getMessage());
                Thread.currentThread().interrupt();
            } finally {
                semaphore.release();
            }
        });
    }

    private void sleep(long startTime) throws InterruptedException {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime < eachCalculationSleepMillis) {
            Thread.sleep(eachCalculationSleepMillis - elapsedTime);
        }
    }

    /**
     * Writes results to file by using single thread executor.
     */
    private void writeResults() {
        ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

        try {
            writeExecutor.submit(new WriteNumberFactorialTask(calculationExecutor, queue, map, logger));
        } finally {
            shutdownExecutor(writeExecutor);
            logger.info("Write executor was shutdown.");
        }
    }

    /**
     * @param number - number to calculate factorial
     * @return - factorial of number
     */
    private BigInteger calculateFactorial(int number) {
        BigInteger result = BigInteger.ONE;

        for (int i = 2; i <= number; i++)
            result = result.multiply(BigInteger.valueOf(i));

        return result;
    }

    /**
     * @param executor - executor to shut down
     */
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(MAX_MINUTES_AWAIT_TIMEOUT, TimeUnit.MINUTES)) {
                logger.severe("Executor did not terminate in the specified time.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.severe("Executor was interrupted while waiting. " + e.getMessage());
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
