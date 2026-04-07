package com.pasp.ict.salescrm.testutil;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Utility class for performance testing and benchmarking.
 * Provides methods to measure execution time and validate performance requirements.
 */
public class PerformanceTestUtil {
    
    /**
     * Measures the execution time of a runnable operation.
     * 
     * @param operation The operation to measure
     * @return The execution time in milliseconds
     */
    public static long measureExecutionTime(Runnable operation) {
        Instant start = Instant.now();
        operation.run();
        Instant end = Instant.now();
        return Duration.between(start, end).toMillis();
    }
    
    /**
     * Measures the execution time of a callable operation.
     * 
     * @param operation The operation to measure
     * @return A result containing the return value and execution time
     */
    public static <T> TimedResult<T> measureExecutionTime(Callable<T> operation) throws Exception {
        Instant start = Instant.now();
        T result = operation.call();
        Instant end = Instant.now();
        long executionTime = Duration.between(start, end).toMillis();
        return new TimedResult<>(result, executionTime);
    }
    
    /**
     * Measures the execution time of a supplier operation.
     * 
     * @param operation The operation to measure
     * @return A result containing the return value and execution time
     */
    public static <T> TimedResult<T> measureExecutionTime(Supplier<T> operation) {
        Instant start = Instant.now();
        T result = operation.get();
        Instant end = Instant.now();
        long executionTime = Duration.between(start, end).toMillis();
        return new TimedResult<>(result, executionTime);
    }
    
    /**
     * Asserts that an operation completes within the specified time limit.
     * 
     * @param operation The operation to test
     * @param maxTimeMs The maximum allowed execution time in milliseconds
     * @throws AssertionError if the operation takes longer than the specified time
     */
    public static void assertExecutionTime(Runnable operation, long maxTimeMs) {
        long executionTime = measureExecutionTime(operation);
        if (executionTime > maxTimeMs) {
            throw new AssertionError(
                String.format("Operation took %d ms, but should complete within %d ms", 
                            executionTime, maxTimeMs)
            );
        }
    }
    
    /**
     * Asserts that a callable operation completes within the specified time limit.
     * 
     * @param operation The operation to test
     * @param maxTimeMs The maximum allowed execution time in milliseconds
     * @return The result of the operation
     * @throws AssertionError if the operation takes longer than the specified time
     */
    public static <T> T assertExecutionTime(Callable<T> operation, long maxTimeMs) throws Exception {
        TimedResult<T> result = measureExecutionTime(operation);
        if (result.getExecutionTime() > maxTimeMs) {
            throw new AssertionError(
                String.format("Operation took %d ms, but should complete within %d ms", 
                            result.getExecutionTime(), maxTimeMs)
            );
        }
        return result.getResult();
    }
    
    /**
     * Asserts that a supplier operation completes within the specified time limit.
     * 
     * @param operation The operation to test
     * @param maxTimeMs The maximum allowed execution time in milliseconds
     * @return The result of the operation
     * @throws AssertionError if the operation takes longer than the specified time
     */
    public static <T> T assertExecutionTime(Supplier<T> operation, long maxTimeMs) {
        TimedResult<T> result = measureExecutionTime(operation);
        if (result.getExecutionTime() > maxTimeMs) {
            throw new AssertionError(
                String.format("Operation took %d ms, but should complete within %d ms", 
                            result.getExecutionTime(), maxTimeMs)
            );
        }
        return result.getResult();
    }
    
    /**
     * Runs a performance benchmark by executing an operation multiple times.
     * 
     * @param operation The operation to benchmark
     * @param iterations The number of iterations to run
     * @return Performance statistics for the benchmark
     */
    public static PerformanceStats benchmark(Runnable operation, int iterations) {
        long[] executionTimes = new long[iterations];
        
        for (int i = 0; i < iterations; i++) {
            executionTimes[i] = measureExecutionTime(operation);
        }
        
        return new PerformanceStats(executionTimes);
    }
    
    /**
     * Runs a performance benchmark by executing a callable operation multiple times.
     * 
     * @param operation The operation to benchmark
     * @param iterations The number of iterations to run
     * @return Performance statistics for the benchmark
     */
    public static <T> PerformanceStats benchmark(Callable<T> operation, int iterations) throws Exception {
        long[] executionTimes = new long[iterations];
        
        for (int i = 0; i < iterations; i++) {
            TimedResult<T> result = measureExecutionTime(operation);
            executionTimes[i] = result.getExecutionTime();
        }
        
        return new PerformanceStats(executionTimes);
    }
    
    /**
     * Container class for timed operation results.
     */
    public static class TimedResult<T> {
        private final T result;
        private final long executionTime;
        
        public TimedResult(T result, long executionTime) {
            this.result = result;
            this.executionTime = executionTime;
        }
        
        public T getResult() {
            return result;
        }
        
        public long getExecutionTime() {
            return executionTime;
        }
        
        @Override
        public String toString() {
            return String.format("TimedResult{result=%s, executionTime=%d ms}", result, executionTime);
        }
    }
    
    /**
     * Performance statistics for benchmark results.
     */
    public static class PerformanceStats {
        private final long[] executionTimes;
        private final long minTime;
        private final long maxTime;
        private final double avgTime;
        private final double medianTime;
        
        public PerformanceStats(long[] executionTimes) {
            this.executionTimes = executionTimes.clone();
            
            long sum = 0;
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            
            for (long time : executionTimes) {
                sum += time;
                min = Math.min(min, time);
                max = Math.max(max, time);
            }
            
            this.minTime = min;
            this.maxTime = max;
            this.avgTime = (double) sum / executionTimes.length;
            
            // Calculate median
            java.util.Arrays.sort(this.executionTimes);
            int middle = executionTimes.length / 2;
            if (executionTimes.length % 2 == 0) {
                this.medianTime = (this.executionTimes[middle - 1] + this.executionTimes[middle]) / 2.0;
            } else {
                this.medianTime = this.executionTimes[middle];
            }
        }
        
        public long getMinTime() { return minTime; }
        public long getMaxTime() { return maxTime; }
        public double getAvgTime() { return avgTime; }
        public double getMedianTime() { return medianTime; }
        public int getIterations() { return executionTimes.length; }
        
        /**
         * Asserts that the average execution time is within the specified limit.
         */
        public void assertAvgTimeWithin(long maxTimeMs) {
            if (avgTime > maxTimeMs) {
                throw new AssertionError(
                    String.format("Average execution time %.2f ms exceeds limit of %d ms", 
                                avgTime, maxTimeMs)
                );
            }
        }
        
        /**
         * Asserts that the maximum execution time is within the specified limit.
         */
        public void assertMaxTimeWithin(long maxTimeMs) {
            if (maxTime > maxTimeMs) {
                throw new AssertionError(
                    String.format("Maximum execution time %d ms exceeds limit of %d ms", 
                                maxTime, maxTimeMs)
                );
            }
        }
        
        /**
         * Asserts that the median execution time is within the specified limit.
         */
        public void assertMedianTimeWithin(long maxTimeMs) {
            if (medianTime > maxTimeMs) {
                throw new AssertionError(
                    String.format("Median execution time %.2f ms exceeds limit of %d ms", 
                                medianTime, maxTimeMs)
                );
            }
        }
        
        @Override
        public String toString() {
            return String.format(
                "PerformanceStats{iterations=%d, min=%d ms, max=%d ms, avg=%.2f ms, median=%.2f ms}",
                getIterations(), minTime, maxTime, avgTime, medianTime
            );
        }
    }
}