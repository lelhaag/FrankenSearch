package algos;

import game.Game;
import main.collections.FastArrayList;
import other.AI;
import other.context.Context;
import other.move.Move;
import parser.ExecutableSearchAlgorithm;
import parser.LudiiNode;
import utils.GlobalVariableRegistry;

import static utils.SearchStatistics.recordSearch;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class GeneralBestFirstSearch extends AI {
    protected int playerId = -1;
    private final ExecutableSearchAlgorithm searchAlgorithm;

    // Tracking variables
    private int maxDepthEncountered = 0;
    private int totalNodeCount = 0;
    private int totalNodeVisits = 0; 

    public GeneralBestFirstSearch(ExecutableSearchAlgorithm searchAlgorithm) {
        this.searchAlgorithm = searchAlgorithm;
        this.friendlyName = searchAlgorithm.getName();
    }

    @Override
    public Move selectAction(final Game game, final Context context, final double maxSeconds, final int maxIterations, final int maxDepth) {
        final LudiiNode root = new LudiiNode(null, context, playerId, null);

        // Initialize search-specific global variables
        GlobalVariableRegistry.createInstanceSpace(root.getSearchId(), searchAlgorithm.getGlobalVariables());

        long startTime = System.currentTimeMillis();
        final long stopTime = (maxSeconds > 0.0) ? startTime + (long) (maxSeconds * 1000L) : Long.MAX_VALUE;
        final int maxIts = (maxIterations >= 0) ? maxIterations : Integer.MAX_VALUE;
        int numIterations = 0;
        LudiiNode current = root;

        Runtime runtime = Runtime.getRuntime();

        // Reset tracking variables for each search
        maxDepthEncountered = 0;
        totalNodeCount = 0;
        totalNodeVisits = 0;

        long lastPrintTime = System.currentTimeMillis();

        while (numIterations < maxIts && (System.currentTimeMillis() < stopTime && !wantsInterrupt)) {

            // comment in to Log root proof number, disproof number, max depth, and memory usage (useful for tracking PNS position proofs)
//            long currentTime = System.currentTimeMillis();
//
//            if (currentTime - lastPrintTime >= 150_000) { // 150 seconds in milliseconds
//                // Log root proof number, disproof number, max depth, and memory usage
//                double rootProofNumber = root.getValue("proofNumber");
//                double rootDisproofNumber = root.getValue("disproofNumber");
//                System.gc();
//
//                long totalMemory = runtime.totalMemory() / (1024 * 1024);
//                long freeMemory = runtime.freeMemory() / (1024 * 1024);
//                long usedMemory = totalMemory - freeMemory;
//
//                System.out.printf(
//                        "Iteration %d: Proof = %.2f, Disproof = %.2f, Max Depth = %d | Memory (MB): Total = %d, Used = %d, Free = %d%n",
//                        numIterations, rootProofNumber, rootDisproofNumber, maxDepthEncountered, totalMemory, usedMemory, freeMemory
//                );
//                lastPrintTime = currentTime;
//            }


            if (root.getValue("proofNumber") == 0.0 || root.getValue("disproofNumber") == 0.0) {
                long searchTime = System.currentTimeMillis() - startTime;
                String result = root.getValue("proofNumber") == 0.0 ? "proven" : "disproven";
                System.out.println("Tree was " + result + " in " + searchTime / 1000 + " s.");
                break;
            }

            while (!current.getContext().trial().over()) {
                current = searchAlgorithm.getSelection().execute(current);
                totalNodeVisits++; // Count each node visit
                if (current.getChildren().isEmpty()) {
                    expandNode(current);
                    // Track total nodes and maximum depth
                    maxDepthEncountered = Math.max(maxDepthEncountered, current.getDepth());
                    break;
                }
            }
            current = searchAlgorithm.getEvaluation().execute(current);
            current = searchAlgorithm.getBackpropagation().execute(current);

            numIterations++;
        }

        long searchTime = System.currentTimeMillis() - startTime;
        recordSearch(
            searchAlgorithm.getName(),
            totalNodeVisits,
            totalNodeCount,
            searchTime
        );

        // comment in to Log root proof number, disproof number, max depth, and memory usage (useful for tracking PNS position proofs)
//        double rootProofNumber = root.getValue("proofNumber");
//        double rootDisproofNumber = root.getValue("disproofNumber");
//        long totalMemory = runtime.totalMemory() / (1024 * 1024);
//        long freeMemory = runtime.freeMemory() / (1024 * 1024);
//        long usedMemory = totalMemory - freeMemory;
//
//        System.out.printf(
//                "Iteration %d: Proof = %.2f, Disproof = %.2f, Max Depth = %d | Memory (MB): Total = %d, Used = %d, Free = %d%n",
//                numIterations, rootProofNumber, rootDisproofNumber, maxDepthEncountered, totalMemory, usedMemory, freeMemory
//        );

        // Perform final move selection
        LudiiNode bestChild;
        if (searchAlgorithm.getFinalMoveSelection() != null) {
            bestChild = searchAlgorithm.getFinalMoveSelection().execute(root);
        } else {
            bestChild = searchAlgorithm.getSelection().execute(root);
        }

        // Log tree size information to file
//        logTreeSizeInfo(System.currentTimeMillis() - startTime);
        GlobalVariableRegistry.cleanupInstance(root.getSearchId());

        // Fallback to first child if bestChild is null or has no move
        if (bestChild == null || bestChild.getMoveFromParent() == null) {
            if (!root.getChildren().isEmpty()) {
                return root.getChildren().get(0).getMoveFromParent();
            }
        }

        return bestChild.getMoveFromParent();
    }

    private void expandNode(LudiiNode node) {
        final FastArrayList<Move> legalMoves = node.getContext().game().moves(node.getContext()).moves();
        for (Move move : legalMoves) {
            Context newContext = new Context(node.getContext());
            newContext.game().apply(newContext, move);
            LudiiNode childNode = new LudiiNode(node, newContext, playerId, move);
            node.addChild(childNode);
            totalNodeVisits++;
            totalNodeCount++;
        }
    }

    private void logTreeSizeInfo(long totalTimeMillis) {
        double totalTimeSeconds = totalTimeMillis / 1000.0;
        double nodesPerSecond = totalNodeVisits / totalTimeSeconds;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("tree_size.txt", true))) {
            writer.write("Algorithm: " + searchAlgorithm.getName() + "\n"); // Add algorithm name
            writer.write("Max Depth Encountered: " + maxDepthEncountered + "\n");
            writer.write("Total Unique Nodes: " + totalNodeCount + "\n");
            writer.write("Total Node Visits: " + totalNodeVisits + "\n");
            writer.write("Nodes per Second: " + String.format("%.2f", nodesPerSecond) + "\n");
            writer.write("--------\n");
        } catch (IOException e) {
            System.err.println("Error writing tree size info: " + e.getMessage());
        }
    }


    @Override
    public void initAI(final Game game, final int playerID) {
        this.playerId = playerID;
    }

    public boolean supportsGame(final Game game)
    {
        if (game.isStochasticGame())
            return false;

        return game.isAlternatingMoveGame();
    }
}
