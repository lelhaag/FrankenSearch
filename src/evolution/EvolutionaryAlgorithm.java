package evolution;

import algos.*;
import game.Game;
import other.AI;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import parser.ASTNode;
import parser.Compiler;
import parser.ExecutableSearchAlgorithm;
import utils.EvaluationFunctions;
import utils.FunctionRegistry;
import other.GameLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static evolution.Mutation.*;
import static evolution.Utils.*;
import static utils.SearchStatistics.reset;
import static utils.SearchStatistics.writeCurrentStats;

public class EvolutionaryAlgorithm {
    private static final int NUM_THREADS = 15;
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.95;
    private static final double CROSSOVER_RATE = 0.95;
    private static final double ELITE_POOL_RATIO = 0.75;
    private static final int GAMES_PER_MATCH = 50;
    private static final int GAMES_PER_MATCH_EVO = 10;
    private static final int EARLY_STOP_THRESHOLD = 5;
    private static final int SWISS_ROUNDS = 6;
    private static final double MAX_SECONDS = 1;
    private static final double MAX_SECONDS_EVO = 0.2;
    public static final int MAX_MUTATION_ATTEMPTS = 30;
    public static final int MAX_CROSSOVER_ATTEMPTS = 30;
    private static final double ORIG_ALGO_THRES = 0.3;
    private static final String GAME_PATH = "resources/LOA6x6.lud";
    private static final String[] SADL_FILES = {
            "resources/SADL/MCTS.txt",
            "resources/SADL/PN-MCTSdepth.txt",
            "resources/SADL/PN-MCTS-A.txt",
            "resources/SADL/PN-MCTS-B.txt",
            "resources/SADL/MCTS1_4.txt",
            "resources/SADL/MCTSx7x8x9x13x14x15x25x28x32x35x37x39x41x43x44x0x4.txt",
            "resources/SADL/MCTSx7x8x9x13x14x15x25x28x32x35x37x39x42.txt",
            "resources/SADL/PNS.txt"
    };
    private static final String[] SADL_FILES_INCLUDE = {
            "resources/SADL/MCTS.txt",
            "resources/SADL/PN-MCTSdepth.txt",
    };

    private static final String GENERATION_CHECKPOINT_FILE = "resources/SADL/generation.txt";

    private final Random random = new Random();

    private static final Logger logger = LogManager.getEvolutionLogger();
    private static final Logger swissLogger = LogManager.getSwissLogger();

    public static void main(String[] args) {
        try {
            FunctionRegistry.setEvalFunction("mctsEval", EvaluationFunctions.mctsEval);
            FunctionRegistry.setEvalFunction("pnsEval", EvaluationFunctions.pnsEval);

            EvolutionaryAlgorithm ea = new EvolutionaryAlgorithm();

            logger.info("Starting evolutionary algorithm");
            ea.evolve();

        } catch (Exception e) {
            e.printStackTrace();

            logger.log(Level.SEVERE, "Fatal error in evolutionary algorithm", e);
        }
    }

    public void evolve() {

        // Create an ExecutorService for offspring evaluation
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        try {
            List<Individual> population;
            int startGeneration = 1;

            // Check for existing generation file
            if (Files.exists(Paths.get(GENERATION_CHECKPOINT_FILE))) {
                logger.info("Found generation.txt. Loading checkpoint...");
                Pair<Integer, List<Individual>> checkpoint = loadGenerationFromFile(GENERATION_CHECKPOINT_FILE);
                startGeneration = checkpoint.first;
                population = checkpoint.second;

                population.sort((ind1, ind2) -> Double.compare(ind2.score, ind1.score)); // Higher score is better

                // Select top individuals to survive
                List<Individual> newPopulation = new ArrayList<>();

                // Select top individuals to survive
                int survivors = POPULATION_SIZE / 2;
                for (int i = 0; i < survivors; i++) {
                    newPopulation.add(population.get(i));
                }

                for (int i = 0; i < SADL_FILES_INCLUDE.length; i++) {
                    String filePath = SADL_FILES_INCLUDE[i];
                    String algoName = new File(filePath).getName().replace("resources/SADL/", "").replace(".txt", "");

                    boolean algoExists = false;
                    for (Individual ind : newPopulation) {
                        String indAlgoName = ind.astRoot.getChildren().get(0).getValue();
                        if (indAlgoName.equals(algoName)) {
                            algoExists = true;
                            break;
                        }
                    }

                    if (!algoExists) {
                        ASTNode astRoot = createASTFromFile(filePath);
                        Individual newInd = new Individual(astRoot);
                        newInd.id = i + 1;
                        newPopulation.add(newInd);
                    }
                }

                // Generate offspring through crossover and mutation
                CompletionService<Individual> completionService = new ExecutorCompletionService<>(executor);
                int pendingTasks = 0;

                // Generate offspring for the new generation
                while (newPopulation.size() < POPULATION_SIZE) {
                    while (pendingTasks < (NUM_THREADS * 2) && population.size() + pendingTasks < 4 * POPULATION_SIZE) {
                        Individual parent1 = newPopulation.get(random.nextInt((int) (newPopulation.size() * ELITE_POOL_RATIO)));
                        Individual parent2 = newPopulation.get(random.nextInt((int) (newPopulation.size() * ELITE_POOL_RATIO)));

                        Individual offspring;
                        if (random.nextDouble() < CROSSOVER_RATE) {
                            offspring = crossover(parent1, parent2);
                        } else {
                            offspring = new Individual(parent1.astRoot.clone());
                        }

                        if (random.nextDouble() < MUTATION_RATE) {
                            mutate(offspring);
                        }

                        offspring.astRoot.getChildren().get(0).setValue(offspring.astRoot.getChildren().get(0).getValue() + "x" + startGeneration);
                        completionService.submit(() -> offspringBeatsOriginal(offspring) ? offspring : null);
                        pendingTasks++;
                    }

                    pendingTasks = processCompletedTasks(newPopulation, completionService, pendingTasks);
                }

                if (newPopulation.size() > POPULATION_SIZE) {
                    newPopulation = newPopulation.subList(0, POPULATION_SIZE);
                }

                population = newPopulation;
                logInitialPopulation(population);

            } else {
                logger.info("No checkpoint found. Initializing new population...");
                population = initializePopulation(executor);
                logInitialPopulation(population);
            }

            for (int generation = startGeneration; generation < MAX_GENERATIONS + 1; generation++) {

                logger.info("Starting Generation " + (generation) + "/" + MAX_GENERATIONS);
                swissLogger.info("Starting Generation " + (generation) + "/" + MAX_GENERATIONS);


                // Conduct a Swiss-system tournament
                conductSwissTournament(population, executor);

                // Rank population based on score
                population.sort((ind1, ind2) -> Double.compare(ind2.score, ind1.score));  // Higher score is better
                // Log population ranking and scores
                logGenerationResults(generation, population);

                logger.info("Generation " + (generation) + " complete. Best score: " + population.get(0).score);
                swissLogger.info("Generation " + (generation) + " complete. Best score: " + population.get(0).score);

                // Select top individuals to survive
                List<Individual> newPopulation = new ArrayList<>();


                int survivors = POPULATION_SIZE / 2;
                for (int i = 0; i < survivors; i++) {
                    newPopulation.add(population.get(i));
                }

                for (int i = 0; i < SADL_FILES_INCLUDE.length; i++) {
                    String filePath = SADL_FILES_INCLUDE[i];
                    // Extract algorithm name from file path (removing .txt and path)
                    String algoName = new File(filePath).getName().replace("resources/SADL/", "").replace(".txt", "");

                    // Check if this algorithm already exists in population
                    boolean algoExists = false;
                    for (Individual ind : newPopulation) {
                        String indAlgoName = ind.astRoot.getChildren().get(0).getValue();
                        if (indAlgoName.equals(algoName)) {
                            algoExists = true;
                            break;
                        }
                    }

                    // If algorithm doesn't exist in population, add it from file
                    if (!algoExists) {
                        ASTNode astRoot = createASTFromFile(filePath);
                        Individual newInd = new Individual(astRoot);
                        newInd.id = i + 1;
                        newPopulation.add(newInd);
                    }
                }


                // Generate offspring through crossover and mutation
                CompletionService<Individual> completionService = new ExecutorCompletionService<>(executor);
                int pendingTasks = 0;

                while (newPopulation.size() < POPULATION_SIZE) {
                    // Submit new tasks if needed
                    while (pendingTasks < (NUM_THREADS * 2) && population.size() + pendingTasks < 4 * POPULATION_SIZE) {

                        Individual parent1 = newPopulation.get(random.nextInt((int) (newPopulation.size() * ELITE_POOL_RATIO)));
                        Individual parent2 = newPopulation.get(random.nextInt((int) (newPopulation.size() * ELITE_POOL_RATIO)));

                        Individual offspring;
                        if (random.nextDouble() < CROSSOVER_RATE) {
                            offspring = crossover(parent1, parent2);
                        } else {
                            offspring = new Individual(parent1.astRoot.clone());
                        }

                        if (random.nextDouble() < MUTATION_RATE) {
                            mutate(offspring);
                        }

                        offspring.astRoot.getChildren().get(0).setValue(offspring.astRoot.getChildren().get(0).getValue() + "x" + generation);
                        completionService.submit(() -> offspringBeatsOriginal(offspring) ? offspring : null);
                        pendingTasks++;
                    }

                    // Process any completed task
                    pendingTasks = processCompletedTasks(newPopulation, completionService, pendingTasks);
                }

                // Ensure population size
                if (newPopulation.size() > POPULATION_SIZE) {
                    newPopulation = newPopulation.subList(0, POPULATION_SIZE);
                }

                population = newPopulation;

            }

            // After evolution, get the best individual

            Individual bestIndividual = population.get(0);  // Already sorted
            logger.info("Evolution complete. Best algorithm found:");
            logger.info(bestIndividual.astRoot.getTreeString("", true));
            logger.info(bestIndividual.astRoot.toSADLString());
//        bestIndividual.astRoot.printTree("", true);

        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Executor shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void conductSwissTournament(List<Individual> population, ExecutorService executor) {
        // Initialize scores and reset opponent history
        for (Individual ind : population) {
            ind.score = 0;
            ind.opponentsPlayed.clear();
        }

        reset();

        try {
            for (int round = 1; round <= SWISS_ROUNDS; round++) {
                swissLogger.info("Starting Swiss Round " + round + "/" + SWISS_ROUNDS);

                List<Pair<Individual, Individual>> pairings = createSwissPairings(population);
                Semaphore gameSemaphore = new Semaphore(NUM_THREADS);
                CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);

                // Submit games as tasks
                for (Pair<Individual, Individual> pairing : pairings) {
                    Individual ind1 = pairing.first;
                    Individual ind2 = pairing.second;
                    final int[] wins1 = {0};
                    final int[] wins2 = {0};

                    for (int i = 0; i < GAMES_PER_MATCH; i++) {
                        gameSemaphore.acquire();
                        boolean swapPlayers = i >= GAMES_PER_MATCH / 2;

                        completionService.submit(() -> {
                            try {
                                int result = playSingleGameInMatch(ind1, ind2, swapPlayers);
//                                swissLogger.info("Game completed: Ind1=" + ind1.id + ", Ind2=" + ind2.id + ", Result=" + result);
//                                logMemoryUsage();
                                return result;
                            } catch (Exception e) {
                                swissLogger.log(Level.SEVERE, "Error in game execution", e);
                                return 0;
                            } finally {
                                gameSemaphore.release();
                            }
                        });
                    }

                    // Process results dynamically
                    int completedGames = 0;
                    while (completedGames < GAMES_PER_MATCH) {
                        Future<Integer> future = completionService.poll(10, TimeUnit.SECONDS);
                        if (future != null) {
                            try {
                                Integer result = future.get();
                                if (result == 1) wins1[0]++;
                                if (result == -1) wins2[0]++;
                                completedGames++;
                            } catch (Exception e) {
                                swissLogger.log(Level.SEVERE, "Error processing game future", e);
                            }
                        }
                    }

                    synchronized (this) {
                        ind1.score += wins1[0];
                        ind2.score += wins2[0];
                        ind1.opponentsPlayed.add(ind2.id);
                        ind2.opponentsPlayed.add(ind1.id);

                        swissLogger.info("Individual " + ind1.id + " - Wins: " + ind1.score +
                                ", Individual " + ind2.id + " - Wins: " + ind2.score);
                        logMemoryUsage();
                        System.gc();
                        Thread.sleep(3000); // Allow GC to complete
                        logMemoryUsage();
                    }
                }

                // Re-sort population by score
                population.sort((ind1, ind2) -> Double.compare(ind2.score, ind1.score));
                swissLogger.info("Results after Round " + round + ":");
                for (Individual ind : population) {
                    swissLogger.info("Individual " + ind.id + " - Total Wins: " + ind.score);
                }
            }

            writeCurrentStats();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Tournament terminated due to error", e);
        }
    }


    private boolean offspringBeatsOriginal(Individual offspring) {

        String offspringName = offspring.astRoot.getChildren().get(0).getValue();
        String originalName = offspringName.split("x")[0];

        // Load regular MCTS from resources/SADL/MCTS.txt
        ASTNode originalAstRoot = createASTFromFile("resources/SADL/" + originalName + ".txt");
        Individual originalIndividual = new Individual(originalAstRoot);

        Game localGame = GameLoader.loadGameFromFile(new File(GAME_PATH));

        int winsOffspring = 0;
        int winsOriginal = 0;

        try {
            ExecutableSearchAlgorithm algoOffspring = Compiler.compile(offspring.astRoot);
            ExecutableSearchAlgorithm algoOriginal = Compiler.compile(originalIndividual.astRoot);
            // Prepare AIs
            AI aiOffspring = new GeneralBestFirstSearch(algoOffspring);
            AI aiOriginal = new GeneralBestFirstSearch(algoOriginal);
            // Play half games as player 1, half as player 2

            for (int i = 0; i < GAMES_PER_MATCH_EVO; i++) {

                boolean swapPlayers = i >= GAMES_PER_MATCH_EVO / 2;
                List<AI> ais = new ArrayList<>();
                ais.add(null);
                if (!swapPlayers) {
                    ais.add(aiOffspring);
                    ais.add(aiOriginal);
                } else {
                    ais.add(aiOriginal);
                    ais.add(aiOffspring);
                }

                try {
                    double[] ranking = playSingleGame(localGame, ais, MAX_SECONDS_EVO);

                    if (!swapPlayers) {
                        if (ranking[1] == 1) winsOffspring++;
                        if (ranking[2] == 1) winsOriginal++;
                    } else {
                        if (ranking[1] == 1) winsOriginal++;
                        if (ranking[2] == 1) winsOffspring++;
                    }

                    // Early stopping if one algorithm wins the first x games in a row
                    if (winsOffspring >= EARLY_STOP_THRESHOLD && winsOriginal == 0) {
                        return true;
                    }
                    if (winsOriginal >= EARLY_STOP_THRESHOLD && winsOffspring == 0) {
                        return false;
                    }


//                    System.out.println("Game " + (i + 1) + "/" + GAMES_PER_MATCH_EVO + ": " +
//                            winsOffspring + " " + offspringName + " wins (" + String.format("%.02f", (float) winsOffspring / (i + 1) * 100) + "%); " +
//                            winsMCTS + " " + originalName + " wins (" + String.format("%.02f", (float) winsMCTS / (i + 1) * 100) + "%)");

                } catch (Exception e) {
//                    System.err.println("Error during game " + (i + 1) + ": " + e.getMessage());
//                    System.out.println(offspring.astRoot.toSADLString());
                    return false;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Compilation error for offspring or MCTS: ", e);
            return false;
        }

        double winRate = (double) winsOffspring / GAMES_PER_MATCH_EVO;
//        System.out.println("Match finished: " + offspringName + " won " + winsOffspring + " games, " + originalName + " won " + winsMCTS + " games.");
//        System.out.println(offspringName + " win rate: " + (winRate * 100) + "%");

        return winRate >= ORIG_ALGO_THRES;
    }

    private List<Pair<Individual, Individual>> createSwissPairings(List<Individual> population) {
        List<Pair<Individual, Individual>> pairings = new ArrayList<>();
        List<Individual> unpaired = new ArrayList<>(population);
        unpaired.sort((a, b) -> Double.compare(b.score, a.score));

        // Group players by score
        TreeMap<Double, List<Individual>> scoreGroups = new TreeMap<>(Collections.reverseOrder());
        for (Individual ind : unpaired) {
            scoreGroups.computeIfAbsent(ind.score, k -> new ArrayList<>()).add(ind);
        }

        StringBuilder logMessage = new StringBuilder("\n=== New Swiss Round Pairings ===\n");
        logMessage.append("Population size: ").append(population.size()).append("\n\n");
        logMessage.append("Score Groups:\n");
        for (Map.Entry<Double, List<Individual>> entry : scoreGroups.entrySet()) {
            logMessage.append(String.format("Score %.1f: %d players\n",
                    entry.getKey(), entry.getValue().size()));
        }

        while (!unpaired.isEmpty()) {
            Individual ind1 = unpaired.get(0);
            Individual ind2 = null;
            double bestScoreDiff = Double.MAX_VALUE;

            // First try same score group
            List<Individual> sameScoreGroup = scoreGroups.get(ind1.score);
            for (Individual potential : sameScoreGroup) {
                if (potential != ind1 && !ind1.opponentsPlayed.contains(potential.id)) {
                    ind2 = potential;
                    break;
                }
            }

            // If no match in same score group, look for closest score group
            if (ind2 == null) {
                for (Map.Entry<Double, List<Individual>> entry : scoreGroups.entrySet()) {
                    if (entry.getKey().equals(ind1.score)) continue;

                    double scoreDiff = Math.abs(entry.getKey() - ind1.score);
                    if (scoreDiff < bestScoreDiff) {
                        for (Individual potential : entry.getValue()) {
                            if (!ind1.opponentsPlayed.contains(potential.id)) {
                                ind2 = potential;
                                bestScoreDiff = scoreDiff;
                                break;
                            }
                        }
                    }
                }
            }

            if (ind2 != null) {
                pairings.add(new Pair<>(ind1, ind2));
                logMessage.append(String.format("Paired: %d (%.1f) vs %d (%.1f) [score diff: %.1f]\n",
                        ind1.id, ind1.score, ind2.id, ind2.score, Math.abs(ind1.score - ind2.score)));
                unpaired.remove(ind1);
                unpaired.remove(ind2);
                scoreGroups.get(ind1.score).remove(ind1);
                scoreGroups.get(ind2.score).remove(ind2);
            } else {
                logMessage.append(String.format("Bye for: %d (%.1f)\n", ind1.id, ind1.score));
                unpaired.remove(ind1);
                scoreGroups.get(ind1.score).remove(ind1);
                ind1.score += (double) GAMES_PER_MATCH / 2;
            }
        }

        swissLogger.info(logMessage.toString());
        return pairings;
    }



    private int playSingleGameInMatch(Individual ind1, Individual ind2, boolean swapPlayers) {
        try {
            ExecutableSearchAlgorithm algo1 = Compiler.compile(ind1.astRoot);
            ExecutableSearchAlgorithm algo2 = Compiler.compile(ind2.astRoot);

            Game localGame = GameLoader.loadGameFromFile(new File(GAME_PATH));
            AI ai1 = new GeneralBestFirstSearch(algo1);
            AI ai2 = new GeneralBestFirstSearch(algo2);

            List<AI> ais = new ArrayList<>();
            ais.add(null);  // Placeholder for AI 0 (no player)
            if (!swapPlayers) {
                ais.add(ai1);  // AI 1 plays for ind1
                ais.add(ai2);  // AI 2 plays for ind2
            } else {
                ais.add(ai2);  // AI 1 plays for ind2 (swapped)
                ais.add(ai1);  // AI 2 plays for ind1 (swapped)
            }

            double[] ranking = playSingleGame(localGame, ais, MAX_SECONDS);

            // Explicitly dereference large objects after use
            localGame = null;
            ais = null;
            ai1 = null;
            ai2 = null;

            if (!swapPlayers) {
                if (ranking[1] == 1) return 1;   // ind1 wins
                if (ranking[2] == 1) return -1;  // ind2 wins
            } else {
                if (ranking[1] == 1) return -1;  // ind2 wins
                if (ranking[2] == 1) return 1;   // ind1 wins
            }
        } catch (Exception e) {
            logger.severe("Error during game: " + e.getMessage());
        }

        return 0;  // Game is a draw
    }

    private double[] playSingleGame(Game game, List<AI> ais, double max_seconds) {
        final Trial trial = new Trial(game);
        final Context context = new Context(game, trial);
        game.start(context);

        ais.get(1).initAI(game, 1);
        ais.get(2).initAI(game, 2);

        final Model model = context.model();

        while (!trial.over()) {
            model.startNewStep(context, ais, max_seconds);
        }

        return trial.ranking();
    }

    // Initialize population by loading from SADL files, and then randomly filling up the remaining
    private List<Individual> initializePopulation( ExecutorService executor) {
        List<Individual> population = new ArrayList<>();
        // Load initial individuals from SADL files
        for (String sadlFile : SADL_FILES) {
            ASTNode astRoot = createASTFromFile(sadlFile);
            population.add(new Individual(astRoot));
        }

        CompletionService<Individual> completionService = new ExecutorCompletionService<>(executor);
        int pendingTasks = 0;
        while (population.size() < POPULATION_SIZE) {

            while (pendingTasks < (NUM_THREADS * 2) && population.size() + pendingTasks < 4 * POPULATION_SIZE) {

                int randomIndex = random.nextInt(SADL_FILES.length);
                ASTNode astRoot = createASTFromFile(SADL_FILES[randomIndex]);
                Individual mutated = new Individual(astRoot);
                mutate(mutated);
                mutated.astRoot.getChildren().get(0).setValue(mutated.astRoot.getChildren().get(0).getValue() + "x0");

                completionService.submit(() -> offspringBeatsOriginal(mutated) ? mutated : null);
                pendingTasks++;
            }

            // Process any completed task
            pendingTasks = processCompletedTasks(population, completionService, pendingTasks);
        }

        // Ensure that the population size does not exceed 20
        if (population.size() > POPULATION_SIZE) {
            // Truncate the population to the first 20 individuals
            population = population.subList(0, POPULATION_SIZE);
        }

        return population;
    }

    private int processCompletedTasks(List<Individual> population,
                                      CompletionService<Individual> completionService,
                                      int pendingTasks) {
        Future<Individual> completed;
        try {
            completed = completionService.poll(100, TimeUnit.MILLISECONDS);
            if (completed != null) {
                pendingTasks--;
                try {
                    Individual result = completed.get(3, TimeUnit.MINUTES);
                    if (result != null && population.size() < POPULATION_SIZE) {
                        population.add(result);
                        logger.info("Added individual " + result.id + " to population.");
                    }
                } catch (ExecutionException e) {
                    logger.severe("Task execution failed: " + e.getCause().getMessage());
                } catch (TimeoutException e) {
                    completed.cancel(true);
                    logger.severe("Task timed out and was cancelled");
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("Task polling interrupted");
        }

        return pendingTasks;
    }

    private void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        logger.info("Memory Usage - Total: " + totalMemory / (1024 * 1024) + " MB, Used: " + usedMemory / (1024 * 1024) + " MB, Free: " + freeMemory / (1024 * 1024) + " MB");
    }


}
