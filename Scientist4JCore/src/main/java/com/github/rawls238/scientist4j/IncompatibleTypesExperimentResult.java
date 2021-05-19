package com.github.rawls238.scientist4j;

import com.github.rawls238.scientist4j.exceptions.MismatchException;

import java.util.Map;
import java.util.Optional;

/**
 * @param <T> The return type of the control function
 * @param <U> The return type of the candidate function.
 */
public class IncompatibleTypesExperimentResult<T, U> {
    private final Observation<T> control;
    private final Optional<Observation<U>> candidate;
    private Optional<Boolean> match;
    private final Map<String, Object> context;

    public IncompatibleTypesExperimentResult(final IncompatibleTypesExperiment<T, U> experiment, final Observation<T> control,
                                             final Optional<Observation<U>> candidate, final Map<String, Object> context) throws MismatchException {
        this.control = control;
        this.candidate = candidate;
        this.context = context;
        this.match = Optional.empty();

        if (candidate.isPresent()) {
            try {
                this.match = Optional.of(experiment.compare(control, candidate.get()));
            } catch (MismatchException e) {
                this.match = Optional.of(false);
                throw e;
            }
        }
    }

    public Optional<Boolean> getMatch() {
        return match;
    }

    public Observation<T> getControl() {
        return control;
    }

    public Optional<Observation<U>> getCandidate() {
        return candidate;
    }

    public Map<String, Object> getContext() {
        return context;
    }
}
