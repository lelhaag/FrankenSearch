package utils;

import evolution.LogManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class SearchStatistics {
    private static final Logger logger = LogManager.getStatsLogger();
    private static final Map<String, AlgorithmStats> statsMap = new ConcurrentHashMap<>();

    public static class AlgorithmStats {
        private final String algorithmName;
        private final AtomicLong totalVisits = new AtomicLong(0);
        private final AtomicLong uniqueNodes = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicInteger searchCount = new AtomicInteger(0);

        public AlgorithmStats(String name) {
            this.algorithmName = name;
        }

        public void addSearch(long visits, long nodes, long timeMs) {
            totalVisits.addAndGet(visits);
            uniqueNodes.addAndGet(nodes);
            totalTime.addAndGet(timeMs);
            searchCount.incrementAndGet();
        }

        public String getStatsString() {
            double visitsPerSec = totalTime.get() > 0 ?
                    (totalVisits.get() * 1000.0) / totalTime.get() : 0;
            double nodesPerSec = totalTime.get() > 0 ?
                    (uniqueNodes.get() * 1000.0) / totalTime.get() : 0;
            return String.format("%s: %.2f visits/sec (%.2f nodes/sec) over %d searches",
                    algorithmName, visitsPerSec, nodesPerSec, searchCount.get());
        }
    }

    public static void recordSearch(String algoName, long visits, long nodes, long timeMs) {
        statsMap.computeIfAbsent(algoName, AlgorithmStats::new)
                .addSearch(visits, nodes, timeMs);
    }

    public static void writeCurrentStats(double maxSeconds, String gameVariant) {
        synchronized(statsMap) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== Stats for ").append(gameVariant)
                    .append(" (").append(String.format("%.1f", maxSeconds)).append(" seconds) ===\n");

            for (AlgorithmStats stats : statsMap.values()) {
                sb.append(stats.getStatsString()).append("\n");
            }
            sb.append("=====================\n");
            logger.info(sb.toString());
        }
    }

    public static void writeCurrentStats() {
        synchronized(statsMap) {
            StringBuilder sb = new StringBuilder("\n=== Search Stats ===\n");
            for (AlgorithmStats stats : statsMap.values()) {
                sb.append(stats.getStatsString()).append("\n");
            }
            sb.append("=====================\n");
            logger.info(sb.toString());
        }
    }

    public static void reset() {
        statsMap.clear();
    }
}