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

import static utils.SearchStatistics.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * The main method of this launches the Ludii application with its GUI, and registers
 * the example AIs from this project such that they are available inside the GUI.
 *
 * @author Leon Haag
 */
public class LaunchLudiiBenchmark
{
	public static String generateLUD(String sbfPath, String templatePath) throws IOException {
	List<String> sbfLines = Files.readAllLines(Paths.get(sbfPath));
	String templateContent = new String(Files.readAllBytes(Paths.get(templatePath)));

	// Parse the board (first 64 lines)
	int[] board = new int[64];
	for (int i = 0; i < 64; i++) {
		board[i] = Integer.parseInt(sbfLines.get(i).trim());
	}

	// Parse the player to move (65th line)
	int playerToMove = Integer.parseInt(sbfLines.get(64).trim());

	// If the starting player is -1, flip the board
	if (playerToMove == -1) {
		for (int i = 0; i < 64; i++) {
			if (board[i] == 1) {
				board[i] = -1;
			} else if (board[i] == -1) {
				board[i] = 1;
			}
		}
	}

	// Build the placement section
	StringBuilder placementBuilder = new StringBuilder();
	for (int idx = 0; idx < 64; idx++) {
		// Flip the index vertically
		int flippedIdx = (7 - (idx / 8)) * 8 + (idx % 8);
		int value = board[flippedIdx];
		if (value == 1) {
			placementBuilder.append("(place \"Disc1\" " + idx + ")\n");
		} else if (value == -1) {
			placementBuilder.append("(place \"Disc2\" " + idx + ")\n");
		}
	}

	// Combine placement into the starting position
	String startingPosition = placementBuilder.toString();

	// Replace the placeholder in the template
	return templateContent.replace("STARTING_POSITION", startingPosition);
}
	/**
	 * The main method
	 * @param args
	 */
	public static void main(final String[] args) throws IOException {

//		// Loop for processing all files in the directory
//		String inputDir = "resources/endgame_positions/";
//		String outputDir = "resources/endgame_luds/";
//		String templatePath = "resources/LOA8x8template.lud";
//
//
//		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(inputDir), "*.sbf")) {
//			for (Path sbfFile : stream) {
//				String ludContent = generateLUD(sbfFile.toString(), templatePath);
//				String outputFileName = outputDir + sbfFile.getFileName().toString().replace(".sbf", ".lud");
//				Files.write(Paths.get(outputFileName), ludContent.getBytes());
//			}
//		}

//		String[] gamePaths = {
//				"resources/endgame_luds/billings.lud",
//				"resources/endgame_luds/blixen.lud",
//		};


		// Gather all .lud files into gamePaths
		List<String> gamePaths = new ArrayList<>();
		try (DirectoryStream<Path> ludStream = Files.newDirectoryStream(Paths.get("resources/endgame_luds/"), "*.lud")) {
			for (Path ludFile : ludStream) {
				gamePaths.add(ludFile.toString());
			}
		}


		String SADL_path = "resources/SADL/PNS.txt";

		// Max seconds settings
		double[] MAX_SECONDS = {60*60*24*7};

		// Number of games
		final int NUM_GAMES = 2;
		final int NUM_GAMES_PER_SIDE = NUM_GAMES / 2;

		// Number of threads
		final int NUM_THREADS = 1;

		try {
			// Read and compile the SADL file
			String SADL_input = new String(Files.readAllBytes(Paths.get(SADL_path)));
			Tokenizer tokenizer = new Tokenizer(SADL_input);
			List<Token> tokens = tokenizer.tokenize();
			Parser parser = new Parser(tokens);
			ASTNode ast = parser.parse();
//			ast.printTree("", true);
			ExecutableSearchAlgorithm algorithm = Compiler.compile(ast);

			// Set the eval functions in the registry with flexible names
			FunctionRegistry.setEvalFunction("mctsEval", EvaluationFunctions.mctsEval);
			FunctionRegistry.setEvalFunction("pnsEval", EvaluationFunctions.pnsEval);

			// Register Random AI
			if (!AIRegistry.registerAI("Example Random AI", () -> {return new RandomAI();}, (game) -> {return true;}))
				System.err.println("WARNING! Failed to register AI because one with that name already existed!");

			// Register ExampleUCT AI
			if (!AIRegistry.registerAI("Example UCTlel", () -> {return new ExampleUCT();}, (game) -> {return new ExampleUCT().supportsGame(game);}))
				System.err.println("WARNING! Failed to register AI because one with that name already existed!");

			// Register GeneralBestFirstSearch AI with PNS
			if (!AIRegistry.registerAI("GeneralBestFirstSearch [" + algorithm.getName() + "]", () -> {return new GeneralBestFirstSearch(algorithm);}, (game) -> {return new GeneralBestFirstSearch(algorithm).supportsGame(game);}))
				System.err.println("WARNING! Failed to register AI because one with that name already existed!");


			// Loop over different max_seconds settings
			for (double maxSeconds : MAX_SECONDS) {
				System.out.println("\nRunning games with maxSeconds = " + maxSeconds + "\n");

				// Loop over different game variants
				for (String gamePath : gamePaths) {
					System.out.println("\nTesting " + gamePath);
					reset();


					// Load the game
					final Game game = GameLoader.loadGameFromFile(new File(gamePath));

					int randomWins = 0;
					int gbfsWins = 0;
					int draws = 0;

					ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
					CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);

					// Submit games
					for (int i = 0; i < NUM_GAMES; i++) {
						boolean swapPlayers = i >= NUM_GAMES_PER_SIDE;
						completionService.submit(new GameTask(game, maxSeconds, swapPlayers, algorithm));
					}

					// Collect results
					for (int i = 0; i < NUM_GAMES; i++) {
						try {
							Future<Integer> future = completionService.take();
							int result = future.get();
							if (result == 1) randomWins++;
							else if (result == -1) gbfsWins++;
							else draws++;

						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					}

					executor.shutdown();

					// Print results
					System.out.println("\nResults for maxSeconds = " + maxSeconds + ":");
					System.out.println("Random wins: " + randomWins + " (" + String.format("%.2f", (100.0 * randomWins / NUM_GAMES)) + "%)");
					System.out.println("GeneralBestFirstSearch wins: " + gbfsWins + " (" + String.format("%.2f", (100.0 * gbfsWins / NUM_GAMES)) + "%)");
					System.out.println("Draws: " + draws + " (" + String.format("%.2f", (100.0 * draws / NUM_GAMES)) + "%)");

					writeCurrentStats(maxSeconds, gamePath);


					System.gc();
//					Thread.sleep(3000); // Allow GC to complete

				}
			}

		} catch (IOException e) {
			System.err.println("Error reading SADL file: " + e.getMessage());
//		} catch (InterruptedException e) {
//            throw new RuntimeException(e);
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
			// Play a single game between ExampleUCT and GeneralBestFirstSearch(algorithm)
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

				System.gc();
				Thread.sleep(3000); // Allow GC to complete

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
