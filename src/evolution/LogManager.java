package evolution;

import java.io.IOException;
import java.util.logging.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {
    private static final Logger EVOLUTION_LOGGER = Logger.getLogger("Evolution");
    private static final Logger SWISS_LOGGER = Logger.getLogger("Swiss");
    private static final Logger STATS_LOGGER = Logger.getLogger("Stats");
    private static final Logger GENERATION_LOGGER = Logger.getLogger("Generation");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    static {
        try {
            // Remove all existing handlers
            for (Handler handler : Logger.getLogger("").getHandlers()) {
                Logger.getLogger("").removeHandler(handler);
            }

            // Set global logging level
            Logger.getLogger("").setLevel(Level.FINE);

            // Console handler setup for all loggers
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new CustomFormatter());

            // Evolution logger setup
            FileHandler evolutionHandler = new FileHandler("evolution_%g.log", 5_000_000, 10, true);
            evolutionHandler.setFormatter(new CustomFormatter());
            evolutionHandler.setLevel(Level.FINE);
            EVOLUTION_LOGGER.setLevel(Level.FINE);
            EVOLUTION_LOGGER.addHandler(evolutionHandler);
            EVOLUTION_LOGGER.addHandler(consoleHandler);

            // Swiss tournament logger setup
            FileHandler swissHandler = new FileHandler("swiss_%g.log", 5_000_000, 10, true);
            swissHandler.setFormatter(new CustomFormatter());
            swissHandler.setLevel(Level.FINE);
            SWISS_LOGGER.setLevel(Level.FINE);
            SWISS_LOGGER.addHandler(swissHandler);
            SWISS_LOGGER.addHandler(consoleHandler);

            // Stats logger setup
            FileHandler statsHandler = new FileHandler("stats_%g.log", 5_000_000, 10, true);
            statsHandler.setFormatter(new CustomFormatter());
            statsHandler.setLevel(Level.FINE);
            STATS_LOGGER.setLevel(Level.FINE);
            STATS_LOGGER.addHandler(statsHandler);
            STATS_LOGGER.addHandler(consoleHandler);

            // Generation logger setup
            FileHandler generationHandler = new FileHandler("generation_%g.log", 5_000_000, 10, true);
            generationHandler.setFormatter(new CustomFormatter());
            generationHandler.setLevel(Level.FINE);
            GENERATION_LOGGER.setLevel(Level.FINE);
            GENERATION_LOGGER.addHandler(generationHandler);
            GENERATION_LOGGER.addHandler(consoleHandler);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

static class CustomFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append(DATE_FORMAT.format(new Date(record.getMillis())))
                    .append(" [").append(record.getLevel()).append("] ")
                    .append(record.getLoggerName()).append(": ")
                    .append(formatMessage(record))
                    .append("\n");
            if (record.getThrown() != null) {
                try {
                    StringBuffer sb2 = new StringBuffer();
                    StackTraceElement[] trace = record.getThrown().getStackTrace();
                    for (StackTraceElement element : trace) {
                        sb2.append("\tat ").append(element).append("\n");
                    }
                    sb.append(sb2.toString());
                } catch (Exception ex) {
                    sb.append("Error formatting stack trace: ").append(ex.getMessage()).append("\n");
                }
            }
            return sb.toString();
        }
    }

    public static Logger getEvolutionLogger() {
        return EVOLUTION_LOGGER;
    }

    public static Logger getSwissLogger() {
        return SWISS_LOGGER;
    }

    public static Logger getStatsLogger() {
        return STATS_LOGGER;
    }

    public static Logger getGenerationLogger() {
        return GENERATION_LOGGER;
    }
}
