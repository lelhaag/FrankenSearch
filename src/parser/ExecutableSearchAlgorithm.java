package parser;

import java.util.HashMap;
import java.util.Map;

public class ExecutableSearchAlgorithm {
    private String name;
    private ExecutableSelection selection;
    private ExecutableEvaluation evaluation;
    private ExecutableBackpropagation backpropagation;
    private ExecutableFinalMoveSelection finalMoveSelection;
    private Map<String, Double> globalVariables = new HashMap<>();

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return this.name;
    }

    public void setSelection(ExecutableSelection selection) {
        this.selection = selection;
    }

    public ExecutableSelection getSelection() {
        return this.selection;
    }

    public void setEvaluation(ExecutableEvaluation evaluation) {
        this.evaluation = evaluation;
    }

    public ExecutableEvaluation getEvaluation() {
        return this.evaluation;
    }

    public void setBackpropagation(ExecutableBackpropagation backpropagation) {
        this.backpropagation = backpropagation;
    }

    public ExecutableBackpropagation getBackpropagation() {
        return this.backpropagation;
    }


    public void setFinalMoveSelection(ExecutableFinalMoveSelection finalMoveSelection) {
        this.finalMoveSelection = finalMoveSelection;
    }

    public ExecutableFinalMoveSelection getFinalMoveSelection() {
        return this.finalMoveSelection;
    }

    public void addGlobalVariable(String name, Double value) {
        globalVariables.put(name, value);
    }

    public Map<String, Double> getGlobalVariables() {
        return globalVariables;
    }


    @Override
    public String toString() {
        return "ExecutableSearchAlgorithm{" +
                "name='" + name + '\'' +
                ", selection=" + selection +
                ", evaluation=" + evaluation +
                ", backpropagation=" + backpropagation +
                '}';
    }
}
