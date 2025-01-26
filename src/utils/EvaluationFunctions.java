package utils;

import game.Game;
import other.RankUtils;
import other.context.Context;
import parser.LudiiNode;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class EvaluationFunctions {
    // MCTS Evaluation (playout)
    public static Function<LudiiNode, Double> mctsEval = (ludiiNode) -> {
        Context context = ludiiNode.getContext();
        Game game = context.game();

        if (!context.trial().over()) {
            context = new Context(context);
            game.playout(context, null, -1.0, null, 0, -1, ThreadLocalRandom.current());
        }

        double[] utilities = RankUtils.utilities(context);
        return utilities[ludiiNode.getPlayerId()];
    };

    // PNS Evaluation
    public static Function<LudiiNode, Double> pnsEval = (ludiiNode) -> {
        Context context = ludiiNode.getContext();
        int proofPlayer = ludiiNode.getPlayerId();
        double bestPossibleRank = 1.0;
        double worstPossibleRank = 2.0;

        if (context.trial().over()) {
            double rank = context.trial().ranking()[proofPlayer];
            return rank == bestPossibleRank ? 1.0 : 0.0;
//            return rank == bestPossibleRank || rank != worstPossibleRank ? 1.0 : 0.0; // allow draw
        }
        return -1.0;
    };
}