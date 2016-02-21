package com.rawls238.scientist4j;

import com.rawls238.scientist4j.exceptions.MismatchException;

public class Result {
    private Experiment experiment;
    private Observation control;
    private Observation candidate;
    private boolean match;

    public Result(Experiment experiment, Observation control, Observation candidate) {
        this.experiment = experiment;
        this.control = control;
        this.candidate = candidate;

        try {
            this.match = experiment.compare(control, candidate);
        } catch (MismatchException e) {
            if (experiment.getRaiseOnMismatch()) {
                throw new RuntimeException(e);
            }
            this.match = false;
        }
    }

    public boolean getMatch() {
        return match;
    }

    public Observation getControl() {
        return control;
    }

    public Observation getCandidate() {
        return candidate;
    }
}
