package main;

import algos.*;
import game.Game;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.trial.Trial;
import parser.Compiler;
import parser.ExecutableSearchAlgorithm;
import parser.Parser;
import parser.Tokenizer;
import utils.EvaluationFunctions;
import utils.FunctionRegistry;
import other.model.Model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * Main class to launch Ludii experiments with parallel execution for different MAX_SECONDS settings and additional SADL configurations.
 */
public class LaunchLudiiExpParallel
{
	/**
	 * The main method
	 * @param args
	 */
	public static void main(final String[] args)
	{
		// File paths
		String game_path = "resources/LOA6x6.lud";
		String SADL_path1 = "resources/SADL/MCTS.txt";
		String SADL_path2 = "resources/SADL/MCTSx7x8x9x13x14x15x25x28x32x35x37x39x41x43x44x0x4.txt";
		String SADL_path3 = "resources/SADL/MCTS1_4.txt";
		String SADL_path4 = "resources/SADL/PN-MCTS-A.txt";

		// Experiment settings
		final boolean run12 = false;
		final boolean run13 = true;
		final boolean run14 = false;
		final boolean run23 = true;
		final boolean run24 = false;
		final boolean run34 = false;
		final int NUM_TRIALS = 1000;
		final int NUM_THREADS = 14;

		// Different max seconds settings
		double[] maxSecondsList = {0.1, 1.0, 5.0};

		try {
			// Read and compile SADL files
			ExecutableSearchAlgorithm algorithm1 = compileSADL(SADL_path1);
			ExecutableSearchAlgorithm algorithm2 = compileSADL(SADL_path2);
			ExecutableSearchAlgorithm algorithm3 = compileSADL(SADL_path3);
			ExecutableSearchAlgorithm algorithm4 = compileSADL(SADL_path4);

			// Load the game
			final Game game = GameLoader.loadGameFromFile(new File(game_path));

			// Set the eval functions in the registry with flexible names
			FunctionRegistry.setEvalFunction("mctsEval", EvaluationFunctions.mctsEval);
			FunctionRegistry.setEvalFunction("pnsEval", EvaluationFunctions.pnsEval);

			// Loop over different max_seconds settings
			for (double MAX_SECONDS : maxSecondsList) {
				System.out.println("\nRunning experiments with MAX_SECONDS = " + MAX_SECONDS + "\n");

				ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
				CompletionService<int[]> completionService = new ExecutorCompletionService<>(executor);

				if (run12) runExperiment(completionService, executor, NUM_TRIALS, MAX_SECONDS, game, algorithm1, algorithm2, algorithm1.getName(), algorithm2.getName());
				if (run13) runExperiment(completionService, executor, NUM_TRIALS, MAX_SECONDS, game, algorithm1, algorithm3, algorithm1.getName(), algorithm3.getName());
				if (run14) runExperiment(completionService, executor, NUM_TRIALS, MAX_SECONDS, game, algorithm1, algorithm4, algorithm1.getName(), algorithm4.getName());
				if (run23) runExperiment(completionService, executor, NUM_TRIALS, MAX_SECONDS, game, algorithm2, algorithm3, algorithm2.getName(), algorithm3.getName());
				if (run24) runExperiment(completionService, executor, NUM_TRIALS, MAX_SECONDS, game, algorithm2, algorithm4, algorithm2.getName(), algorithm4.getName());
				if (run34) runExperiment(completionService, executor, NUM_TRIALS, MAX_SECONDS, game, algorithm3, algorithm4, algorithm3.getName(), algorithm4.getName());

				executor.shutdown();
			}

		} catch (IOException e) {
			System.err.println("Error reading SADL files: " + e.getMessage());
		}
	}

	/**
	 * Helper method to compile a SADL file.
	 */
	private static ExecutableSearchAlgorithm compileSADL(String path) throws IOException {
		String SADL_input = new String(Files.readAllBytes(Paths.get(path)));
		return Compiler.compile(new Parser(new Tokenizer(SADL_input).tokenize()).parse());
	}

	/**
	 * Helper method to run an experiment with two AIs in parallel.
	 */
	private static void runExperiment(
			CompletionService<int[]> completionService,
			ExecutorService executor,
			int numTrials,
			double maxSeconds,
			Game game,
			ExecutableSearchAlgorithm alg1,
			ExecutableSearchAlgorithm alg2,
			String alg1Name,
			String alg2Name
	) {
//		System.out.println("\nRunning experiment: " + alg1Name + " vs " + alg2Name);

		int p1Wins = 0, p2Wins = 0, totalGames = 0;

		// Submit tasks
		for (int i = 0; i < numTrials; i++) {
			boolean swapPlayers = i >= numTrials / 2;
			completionService.submit(new GameTask(game, maxSeconds, swapPlayers, alg1, alg2));
		}

		// Collect results
		for (int i = 0; i < numTrials; i++) {
			try {
				Future<int[]> future = completionService.take();
				int[] result = future.get();
				p1Wins += result[0];
				p2Wins += result[1];
				totalGames++;

				// Print intermediate results
				double p1WinPercentage = (100.0 * p1Wins) / totalGames;
				double p2WinPercentage = (100.0 * p2Wins) / totalGames;

//				System.out.println("Game " + (i + 1) + "/" + numTrials + ": "
//						+ alg1Name + " wins = " + p1Wins + " (" + String.format("%.2f", p1WinPercentage) + "%), "
//						+ alg2Name + " wins = " + p2Wins + " (" + String.format("%.2f", p2WinPercentage) + "%)");
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

		System.out.println("\nSeconds per move: " + maxSeconds);
		System.out.println("\nFinal results for " + alg1Name + " vs " + alg2Name + ":");
		System.out.println(alg1Name + " wins: " + p1Wins + " (" + String.format("%.2f", (100.0 * p1Wins / numTrials)) + "%)");
		System.out.println(alg2Name + " wins: " + p2Wins + " (" + String.format("%.2f", (100.0 * p2Wins / numTrials)) + "%)\n");
	}

	/**
	 * Task class to execute a single game between two AIs.
	 */
	static class GameTask implements Callable<int[]> {
		private final Game game;
		private final double maxSeconds;
		private final boolean swapPlayers;
		private final ExecutableSearchAlgorithm alg1;
		private final ExecutableSearchAlgorithm alg2;

        public GameTask(Game game, double maxSeconds, boolean swapPlayers, ExecutableSearchAlgorithm alg1, ExecutableSearchAlgorithm alg2) {
			this.game = game;
			this.maxSeconds = maxSeconds;
			this.swapPlayers = swapPlayers;
			this.alg1 = alg1;
			this.alg2 = alg2;
        }

		@Override
		public int[] call() {
			try {
				// Initialize AIs
				AI ai1 = new GeneralBestFirstSearch(alg1);
				AI ai2 = new GeneralBestFirstSearch(alg2);

				List<AI> ais = new ArrayList<>();
				ais.add(null); // Player 0 placeholder
				if (!swapPlayers) {
					ais.add(ai1);
					ais.add(ai2);
				} else {
					ais.add(ai2);
					ais.add(ai1);
				}

				final Trial trial = new Trial(game);
				final Context context = new Context(game, trial);
				game.start(context);

				for (int p = 1; p <= game.players().count(); ++p) {
					ais.get(p).initAI(game, p);
				}

				final Model model = context.model();

				while (!trial.over()) {
					model.startNewStep(context, ais, maxSeconds);
				}

				final double[] ranking = trial.ranking();

				if (!swapPlayers) {
					return ranking[1] == 1 ? new int[]{1, 0} : new int[]{0, 1};
				} else {
					return ranking[1] == 1 ? new int[]{0, 1} : new int[]{1, 0};
				}
			} catch (Exception e) {
				e.printStackTrace();
				return new int[]{0, 0}; // Return a draw on error
			}
		}
	}
}
