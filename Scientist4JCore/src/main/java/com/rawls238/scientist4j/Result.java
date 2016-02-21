package com.rawls238.scientist4j;

import com.rawls238.scientist4j.exceptions.MismatchException;

import java.util.Optional;

public class Result {
    private Experiment experiment;
    private Observation control;
    private Observation candidate;
    private boolean match;

    public Result(Experiment experiment, Observation control, Observation candidate) throws MismatchException {
        this.experiment = experiment;
        this.control = control;
        this.candidate = candidate;

        Optional<MismatchException> ex = Optional.empty();
        try {
            this.match = experiment.compare(control, candidate);
        } catch (MismatchException e) {
            ex = Optional.of(e);
            this.match = false;
        } finally {
            if (experiment.getRaiseOnMismatch() && ex.isPresent()) {
                throw ex.get();
            }
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
