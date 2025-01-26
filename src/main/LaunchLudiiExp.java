package main;

import algos.*;
import app.StartDesktopApp;
import game.Game;
import manager.ai.AIRegistry;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.trial.Trial;
import parser.ASTNode;
import parser.Compiler;
import parser.ExecutableSearchAlgorithm;
import parser.Parser;
import parser.Token;
import parser.Tokenizer;
import utils.EvaluationFunctions;
import utils.FunctionRegistry;
import other.model.Model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.io.File;
import java.util.List;

/**
 * The main method of this launches the Ludii application with its GUI, and registers
 * the example AIs from this project such that they are available inside the GUI.
 *
 * @author Dennis Soemers
 */
public class LaunchLudiiExp
{
	
	/**
	 * The main method
	 * @param args
	 */
	public static void main(final String[] args)
	{
		//run Ludii UI, or run trials?
		final boolean RUN_GUI = false;

		// File paths
		String game_path = "resources/LOA6x6.lud";
		String SADL_path1 = "resources/SADL/PNS.txt";
		String SADL_path2 = "resources/SADL/MCTS.txt";
		String SADL_path3 = "resources/SADL/MCTSx0x1x4x6x6x9x9x12x14.txt";

		// Experiemnt Settings
		final boolean run12 = false;
		final boolean run31 = false;
		final boolean run23 = true;

		final int NUM_TRIALS = 1000;
		final double MAX_SECONDS = 3;

		//

		try {
			String SADL_input1 = new String(Files.readAllBytes(Paths.get(SADL_path1)));
			String SADL_input2 = new String(Files.readAllBytes(Paths.get(SADL_path2)));
			String SADL_input3 = new String(Files.readAllBytes(Paths.get(SADL_path3)));
			Tokenizer tokenizer1 = new Tokenizer(SADL_input1);
			Tokenizer tokenizer2 = new Tokenizer(SADL_input2);
			Tokenizer tokenizer3 = new Tokenizer(SADL_input3);

			List<Token> tokens1 = tokenizer1.tokenize();
			List<Token> tokens2 = tokenizer2.tokenize();
			List<Token> tokens3 = tokenizer3.tokenize();

			Parser parser1 = new Parser(tokens1);
			Parser parser2 = new Parser(tokens2);
			Parser parser3 = new Parser(tokens3);

			ASTNode ast1 = parser1.parse();
			ASTNode ast2 = parser2.parse();
			ASTNode ast3 = parser3.parse();


	//		System.out.println("Abstract Syntax Tree:");
	//        System.out.println(ast.toString());
	//		ast.printTree("", true);


			ExecutableSearchAlgorithm algorithm1 = Compiler.compile(ast1);
			ExecutableSearchAlgorithm algorithm2 = Compiler.compile(ast2);
			ExecutableSearchAlgorithm algorithm3 = Compiler.compile(ast3);

	//		System.out.println("Compiled algorithm: " + algorithm);

			// Set the eval functions in the registry with flexible names
			FunctionRegistry.setEvalFunction("mctsEval", EvaluationFunctions.mctsEval);
			FunctionRegistry.setEvalFunction("pnsEval", EvaluationFunctions.pnsEval);


			// Register our example AIs
			if (!AIRegistry.registerAI("Example Random AIlolz", () -> {return new RandomAI();}, (game) -> {return true;}))
				System.err.println("WARNING! Failed to register AI because one with that name already existed!");

			if (!AIRegistry.registerAI("Example UCTlel", () -> {return new ExampleUCT();}, (game) -> {return new ExampleUCT().supportsGame(game);}))
				System.err.println("WARNING! Failed to register AI because one with that name already existed!");

			if (!AIRegistry.registerAI("GeneralBestFirstSearch [" + algorithm1.getName() + "]", () -> {return new GeneralBestFirstSearch(algorithm1);}, (game) -> {return new GeneralBestFirstSearch(algorithm1).supportsGame(game);}))
				System.err.println("WARNING! Failed to register AI because one with that name already existed!");

			if (!AIRegistry.registerAI("GeneralBestFirstSearch [" + algorithm2.getName() + "]", () -> {return new GeneralBestFirstSearch(algorithm2);}, (game) -> {return new GeneralBestFirstSearch(algorithm2).supportsGame(game);}))
				System.err.println("WARNING! Failed to register AI because one with that name already existed!");

			if (!AIRegistry.registerAI("GeneralBestFirstSearch [" + algorithm3.getName() + "]", () -> {return new GeneralBestFirstSearch(algorithm3);}, (game) -> {return new GeneralBestFirstSearch(algorithm3).supportsGame(game);}))
				System.err.println("WARNING! Failed to register AI because one with that name already existed!");

			// Run Ludii UI
			if (RUN_GUI) {

				StartDesktopApp.main(new String[0]);

				// Run trials
			} else {

				// init
				final String AI1_name = algorithm1.getName();
				final String AI2_name = algorithm2.getName();
				final String AI3_name = algorithm3.getName();

				final Game game = GameLoader.loadGameFromFile(new File(game_path));
				final Trial trial = new Trial(game);
				final Context context = new Context(game, trial);


				int p1_wins = 0, p2_wins = 0;
				List<AI> ais = new ArrayList<AI>();
				List<AI> ais2 = new ArrayList<AI>();

				if (run12) {
					ais.add(null);
					ais.add(new GeneralBestFirstSearch(algorithm1));
					ais.add(new GeneralBestFirstSearch(algorithm2));

					for (int i = 0; i < NUM_TRIALS/2; ++i)
					{
						game.start(context);

						for (int p = 1; p <= game.players().count(); ++p)
						{
							ais.get(p).initAI(game, p);
						}

						final Model model = context.model();

						while (!trial.over())
						{
							model.startNewStep(context, ais, MAX_SECONDS);
						}

						final double[] ranking = trial.ranking();

						if (ranking[1] == 1) {
							p1_wins++;
						} else {
							p2_wins++;
						}

						System.out.println("Trial " + (i+1) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI1_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI2_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");
					}

					System.out.println("Trial " + (NUM_TRIALS/2) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI1_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI2_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");

					// flip sides after half the trials
					ais2.add(null);
					ais2.add(ais.get(2));
					ais2.add(ais.get(1));

					for (int i = 0; i < NUM_TRIALS/2; ++i)
					{
						game.start(context);

						for (int p = 1; p <= game.players().count(); ++p)
						{
							ais2.get(p).initAI(game, p);
						}

						final Model model = context.model();

						while (!trial.over())
						{
							model.startNewStep(context, ais2, 1.0);
						}

						final double[] ranking = trial.ranking();

						if (ranking[1] == 1) {
							p2_wins++;
						} else {
							p1_wins++;
						}

						System.out.println("Trial " + (NUM_TRIALS/2+i+1) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI1_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI2_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");
					}

					System.out.println("Trial " + (NUM_TRIALS) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI1_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI2_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");
				}

				if (run31) {
					p1_wins = 0;
					p2_wins = 0;
					ais = new ArrayList<AI>();
					ais.add(null);
					ais.add(new GeneralBestFirstSearch(algorithm3));
					ais.add(new GeneralBestFirstSearch(algorithm1));

					for (int i = 0; i < NUM_TRIALS/2; ++i)
					{
						game.start(context);

						for (int p = 1; p <= game.players().count(); ++p)
						{
							ais.get(p).initAI(game, p);
						}

						final Model model = context.model();

						while (!trial.over())
						{
							model.startNewStep(context, ais, MAX_SECONDS);
						}

						final double[] ranking = trial.ranking();

						if (ranking[1] == 1) {
							p1_wins++;
						} else {
							p2_wins++;
						}

						System.out.println("Trial " + (i+1) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI3_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI1_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");
					}

					System.out.println("Trial " + (NUM_TRIALS/2) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI3_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI1_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");

					// flip sides after half the trials
					ais2 = new ArrayList<AI>();
					ais2.add(null);
					ais2.add(ais.get(2));
					ais2.add(ais.get(1));

					for (int i = 0; i < NUM_TRIALS/2; ++i)
					{
						game.start(context);

						for (int p = 1; p <= game.players().count(); ++p)
						{
							ais2.get(p).initAI(game, p);
						}

						final Model model = context.model();

						while (!trial.over())
						{
							model.startNewStep(context, ais2, 1.0);
						}

						final double[] ranking = trial.ranking();

						if (ranking[1] == 1) {
							p2_wins++;
						} else {
							p1_wins++;
						}

						System.out.println("Trial " + (NUM_TRIALS/2+i+1) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI3_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI1_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");
					}

					System.out.println("Trial " + (NUM_TRIALS) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI3_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI1_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");
				}

				if (run23) {
					p1_wins = 0;
					p2_wins = 0;
					ais = new ArrayList<AI>();
					ais.add(null);
					ais.add(new GeneralBestFirstSearch(algorithm2));
					ais.add(new GeneralBestFirstSearch(algorithm3));

					for (int i = 0; i < NUM_TRIALS/2; ++i)
					{
						game.start(context);

						for (int p = 1; p <= game.players().count(); ++p)
						{
							ais.get(p).initAI(game, p);
						}

						final Model model = context.model();

						while (!trial.over())
						{
							model.startNewStep(context, ais, MAX_SECONDS);
						}

						final double[] ranking = trial.ranking();

						if (ranking[1] == 1) {
							p1_wins++;
						} else {
							p2_wins++;
						}

						System.out.println("Trial " + (i+1) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI2_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI3_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");
					}

					System.out.println("Trial " + (NUM_TRIALS/2) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI2_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI3_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");

					// flip sides after half the trials
					ais2 = new ArrayList<AI>();
					ais2.add(null);
					ais2.add(ais.get(2));
					ais2.add(ais.get(1));

					for (int i = 0; i < NUM_TRIALS/2; ++i)
					{
						game.start(context);

						for (int p = 1; p <= game.players().count(); ++p)
						{
							ais2.get(p).initAI(game, p);
						}

						final Model model = context.model();

						while (!trial.over())
						{
							model.startNewStep(context, ais2, 1.0);
						}

						final double[] ranking = trial.ranking();

						if (ranking[1] == 1) {
							p2_wins++;
						} else {
							p1_wins++;
						}

						System.out.println("Trial " + (NUM_TRIALS/2+i+1) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI2_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI3_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");
					}

					System.out.println("Trial " + (NUM_TRIALS) + "/" + NUM_TRIALS + ": " + p1_wins + " " + AI2_name + " wins (" + String.format("%.02f", (float) p1_wins/(p1_wins+p2_wins)*100) + "%); "+p2_wins + " " + AI3_name + " wins (" + String.format("%.02f", (float) p2_wins/(p1_wins+p2_wins)*100) + "%)");

				}
			}
		} catch (IOException e) {
			System.err.println("Error reading SADL files: " + e.getMessage());
		}
	}

}