package parser;

import other.context.Context;
import other.move.Move;

public class LudiiNode extends Node<LudiiNode> {
    private final Integer playerId;
    private final Context context;
    private final Move moveFromParent;

    public LudiiNode(final LudiiNode parent, final Context context, final Integer playerId, final Move moveFromParent) {
        super(parent);
        this.playerId = playerId;
        this.context = context;
        this.moveFromParent = moveFromParent;
        setNodeType(determineNodeType());
    }

    private String determineNodeType() {
        if (context.trial().over()) {
            return context.state().prev() == playerId ? "andNode" : "orNode";
        } else {
            return context.state().mover() == playerId ? "orNode" : "andNode";
        }
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public Context getContext() {
        return context;
    }

    public Move getMoveFromParent() {
        return moveFromParent;
    }
}
