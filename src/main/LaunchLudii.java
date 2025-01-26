package main;

import algos.*;
import app.StartDesktopApp;
import manager.ai.AIRegistry;
import parser.ASTNode;
import parser.Compiler;
import parser.ExecutableSearchAlgorithm;
import parser.Parser;
import parser.Token;
import parser.Tokenizer;
import utils.EvaluationFunctions;
import utils.FunctionRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * The main method of this launches the Ludii application with its GUI, and registers
 * the example AIs from this project such that they are available inside the GUI.
 *
 * @author Dennis Soemers
 */
public class LaunchLudii
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
		String SADL_input = """
				(SearchAlgorithm "MCTS"
				  (Selection "UCT"
				    (Condition (eq nodeType maxNode)
				      (SelectNode argmax
				        (+ valueEstimate (* 1.4 (sqrt (/ (log parentVisitCount) visitCount))))
				      )
				    )
				    (Condition (eq nodeType minNode)
				      (SelectNode argmax
				        (+ (- 0 valueEstimate) (* 1.4 (sqrt (/ (log parentVisitCount) visitCount))))
				      )
				    )
				  )
				  (Evaluation
				    (Set value (eval node))
				  )
				  (Backpropagation
				    (Set visitCount (+ visitCount 1))
				    (Set valueEstimate (+ valueEstimate (/ (- value valueEstimate) visitCount)))
				  )
				  (FinalMoveSelection
				    (SelectNode argmax visitCount)
				  )
				)
				 """;
		SADL_input = """
				(SearchAlgorithm "MCTS"
				      (Define C 0.6)
				      (Define value 0)
				      (Selection "UCT"
				        (Condition (eq nodeType maxNode)
				          (SelectNode argmax
				            (+ valueEstimate
				                (* C (sqrt (/ (log (Parent visitCount)) visitCount)))
				            )
				          )
				        )
				        (Condition (eq nodeType minNode)
				          (SelectNode argmax
				            (+ (- 0 valueEstimate)
				                (* C (sqrt (/ (log (Parent visitCount)) visitCount)))
				            )
				          )
				        )
				      )
				      (Evaluation
				        (Set value (ExternalFunction "mctsEval" node))
				      )
				      (Backpropagation
				        (Set valueEstimate (+ valueEstimate (/ (- value valueEstimate) visitCount)))
				      )
				      (FinalMoveSelection
				        (SelectNode argmax visitCount)
				      )
				    )

				 """;


		System.out.println(generateLUD("resources/endgame_positions/blixen.sbf", "resources/LOA8x8template.lud"));

		Tokenizer tokenizer = new Tokenizer(SADL_input);
		List<Token> tokens = tokenizer.tokenize();

		Parser parser = new Parser(tokens);
		ASTNode ast = parser.parse();


		System.out.println("Abstract Syntax Tree:");
//        System.out.println(ast.toString());
		ast.printTree("", true);


		ExecutableSearchAlgorithm algorithm = Compiler.compile(ast);
//		ExecutableSearchAlgorithm algorithm = Compiler.compile(ast);

		System.out.println("Compiled algorithm: " + algorithm);

//		FunctionRegistry.setEvalFunction(EvaluationFunctions.mctsEval);

		// Set the eval functions in the registry with flexible names
		FunctionRegistry.setEvalFunction("mctsEval", EvaluationFunctions.mctsEval);
		FunctionRegistry.setEvalFunction("pnsEval", EvaluationFunctions.pnsEval);


		// Register our example AIs
		if (!AIRegistry.registerAI("Example Random AIlolz", () -> {return new RandomAI();}, (game) -> {return true;}))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");
		
		if (!AIRegistry.registerAI("Example UCTlel", () -> {return new ExampleUCT();}, (game) -> {return new ExampleUCT().supportsGame(game);}))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");

		if (!AIRegistry.registerAI("GeneralBestFirstSearch [" + algorithm.getName() + "]", () -> {return new GeneralBestFirstSearch(algorithm);}, (game) -> {return new GeneralBestFirstSearch(algorithm).supportsGame(game);}))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");

		// Run Ludii
		StartDesktopApp.main(new String[0]);
	}

}
