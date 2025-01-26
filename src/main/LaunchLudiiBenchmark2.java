package main;

import algos.*;
import game.Game;
import manager.ai.AIRegistry;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import parser.ASTNode;
import parser.Compiler;
import parser.ExecutableSearchAlgorithm;
import parser.Parser;
import parser.Token;
import parser.Tokenizer;
import utils.EvaluationFunctions;
import utils.FunctionRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * The main method of this launches the Ludii application with its GUI, and registers
 * the example AIs from this project such that they are available inside the GUI.
 *
 */
public class LaunchLudiiBenchmark2
{
	/**
	 * The main method
	 * @param args
	 */
	public static void main(final String[] args)
	{
		// File paths
		String game_path = "resources/LOA6x6.lud";
		String SADL_path = "resources/SADL/MCTS.txt";

		// Max seconds settings
		double[] maxSecondsList = {0.1, 0.5, 1.0, 3.0};

		// Number of games
		final int NUM_GAMES = 1000;
		final int NUM_GAMES_PER_SIDE = NUM_GAMES / 2;

		// Number of threads
		final int NUM_THREADS = 8;

		try {
			// Read and compile the SADL file
			String SADL_input = new String(Files.readAllBytes(Paths.get(SADL_path)));
			Tokenizer tokenizer = new Tokenizer(SADL_input);
			List<Token> tokens = tokenizer.tokenize();
			Parser parser = new Parser(tokens);
			ASTNode ast = parser.parse();
			ExecutableSearchAlgorithm algorithm = Compiler.compile(ast);

			// Set the eval functions in the registry with flexible names
			FunctionRegistry.setEvalFunction("mctsEval", EvaluationFunctions.mctsEval);
			FunctionRegistry.setEvalFunction("pnsEval", EvaluationFunctions.pnsEval);

			// Register Random AI
			if (!AIRegistry.registerAI("Example Random AI", () -> {return new RandomAI();}, (game) -> {return true;}))
				System.err.println("WARNING! Failed to register AI because one with that name already existed!");

			// Register GeneralBestFirstSearch AI with MCTS
			if (!AIRegistry.registerAI("GeneralBestFirstSearch [" + algorithm.getName() + "]", () -> {return new GeneralBestFirstSearch(algorithm);}, (game) -> {return new GeneralBestFirstSearch(algorithm).supportsGame(game);}))
				System.err.println("WARNING! Failed to register AI because one with that name already existed!");

			// Load the game
			final Game game = GameLoader.loadGameFromFile(new File(game_path));

			// Loop over different max_seconds settings
			for (double MAX_SECONDS : maxSecondsList) {
				System.out.println("\nRunning games with MAX_SECONDS = " + MAX_SECONDS + "\n");

				int randomWins = 0;
				int gbfsWins = 0;
				int draws = 0;

				ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
				CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);

				// Submit games
				for (int i = 0; i < NUM_GAMES; i++) {
					boolean swapPlayers = i >= NUM_GAMES_PER_SIDE;
					completionService.submit(new GameTask(game, MAX_SECONDS, swapPlayers, algorithm));
				}

				// Collect results
				for (int i = 0; i < NUM_GAMES; i++) {
					try {
						Future<Integer> future = completionService.take();
						int result = future.get();
						if (result == 1) randomWins++;
						else if (result == -1) gbfsWins++;
						else draws++;

						// Calculate percentages
						int totalGamesSoFar = i + 1;
						double randomWinPercentage = (100.0 * randomWins) / totalGamesSoFar;
						double gbfsWinPercentage = (100.0 * gbfsWins) / totalGamesSoFar;
						double drawPercentage = (100.0 * draws) / totalGamesSoFar;

						System.out.println("Game " + totalGamesSoFar + "/" + NUM_GAMES + ": "
								+ "RandomAI wins = " + randomWins + " (" + String.format("%.2f", randomWinPercentage) + "%), "
								+ "GeneralBestFirstSearch wins = " + gbfsWins + " (" + String.format("%.2f", gbfsWinPercentage) + "%), "
								+ "Draws = " + draws + " (" + String.format("%.2f", drawPercentage) + "%)");
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				}

				executor.shutdown();

				// Print results
				System.out.println("\nResults for MAX_SECONDS = " + MAX_SECONDS + ":");
				System.out.println("RandomAI wins: " + randomWins + " (" + String.format("%.2f", (100.0 * randomWins / NUM_GAMES)) + "%)");
				System.out.println("GeneralBestFirstSearch wins: " + gbfsWins + " (" + String.format("%.2f", (100.0 * gbfsWins / NUM_GAMES)) + "%)");
				System.out.println("Draws: " + draws + " (" + String.format("%.2f", (100.0 * draws / NUM_GAMES)) + "%)");
			}

		} catch (IOException e) {
			System.err.println("Error reading SADL file: " + e.getMessage());
		}
	}

	static class GameTask implements Callable<Integer> {
		private final Game game;
		private final double maxSeconds;
		private final boolean swapPlayers;
		private final ExecutableSearchAlgorithm algorithm;

		public GameTask(Game game, double maxSeconds, boolean swapPlayers, ExecutableSearchAlgorithm algorithm) {
			this.game = game;
			this.maxSeconds = maxSeconds;
			this.swapPlayers = swapPlayers;
			this.algorithm = algorithm;
		}

		@Override
		public Integer call() {
			// Play a single game between RandomAI and GeneralBestFirstSearch(algorithm)
			try {
				// Create new instances of the AIs
				AI aiRandom = new RandomAI();
				AI aiGBFS = new GeneralBestFirstSearch(algorithm);
				List<AI> ais = new ArrayList<>();
				ais.add(null); // Player 0 placeholder
				if (!swapPlayers) {
					ais.add(aiRandom);
					ais.add(aiGBFS);
				} else {
					ais.add(aiGBFS);
					ais.add(aiRandom);
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
					if (ranking[1] == 1) return 1; // aiRandom wins
					if (ranking[2] == 1) return -1; // aiGBFS wins
				} else {
					if (ranking[1] == 1) return -1; // aiGBFS wins
					if (ranking[2] == 1) return 1; // aiRandom wins
				}

				return 0; // Draw

			} catch (Exception e) {
				e.printStackTrace();
				return 0;
			}
		}
	}

}
