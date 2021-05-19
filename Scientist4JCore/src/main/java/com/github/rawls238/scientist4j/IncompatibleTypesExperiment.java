package com.github.rawls238.scientist4j;

import com.github.rawls238.scientist4j.exceptions.MismatchException;
import com.github.rawls238.scientist4j.metrics.MetricsProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BiPredicate;

/**
 * An Experiment that can handle a control and candidate function that return incompatible types.
 * <p>
 * Note that this requires a comparator function to be passed in to the constructor because the existing default of
 * Objects::equals doesn't make any sense when the objects are of different types.
 *
 * @param <T> The return type of the control function.
 * @param <U> The return type of the candidate function.
 */
public class IncompatibleTypesExperiment<T, U> {
    private static final String CONTROL = "control";
    private static final String CANDIDATE = "candidate";
    private final ExecutorService executor;
    private static final String NAMESPACE_PREFIX = "scientist";
    private final MetricsProvider<?> metricsProvider;
    private final String name;
    private final boolean raiseOnMismatch;
    private final Map<String, Object> context;
    private final MetricsProvider.Timer controlTimer;
    private final MetricsProvider.Timer candidateTimer;
    private final MetricsProvider.Counter mismatchCount;
    private final MetricsProvider.Counter candidateExceptionCount;
    private final MetricsProvider.Counter totalCount;
    private final BiPredicate<T, U> comparator;

    public IncompatibleTypesExperiment(final MetricsProvider<?> metricsProvider, final BiPredicate<T, U> comparator) {
        this("Experiment", metricsProvider, comparator);
    }

    public IncompatibleTypesExperiment(final String name, final MetricsProvider<?> metricsProvider,
                                       final BiPredicate<T, U> comparator) {
        this(name, false, metricsProvider, comparator);
    }

    public IncompatibleTypesExperiment(final String name, final Map<String, Object> context,
                                       final MetricsProvider<?> metricsProvider, final BiPredicate<T, U> comparator) {
        this(name, context, false, metricsProvider, comparator);
    }

    public IncompatibleTypesExperiment(final String name, final boolean raiseOnMismatch,
                                       final MetricsProvider<?> metricsProvider, final BiPredicate<T, U> comparator) {
        this(name, new HashMap<>(), raiseOnMismatch, metricsProvider, comparator);
    }

    public IncompatibleTypesExperiment(final String name, final Map<String, Object> context,
                                       final boolean raiseOnMismatch, final MetricsProvider<?> metricsProvider, final BiPredicate<T, U> comparator) {
        this(name, context, raiseOnMismatch, metricsProvider, comparator, Executors.newFixedThreadPool(2));
    }

    public IncompatibleTypesExperiment(final String name, final Map<String, Object> context,
                                       final boolean raiseOnMismatch, final MetricsProvider<?> metricsProvider, final BiPredicate<T, U> comparator,
                                       final ExecutorService executorService) {
        this.name = name;
        this.context = context;
        this.raiseOnMismatch = raiseOnMismatch;
        this.comparator = comparator;
        this.metricsProvider = metricsProvider;
        controlTimer = getMetricsProvider().timer(NAMESPACE_PREFIX, this.name, CONTROL);
        candidateTimer = getMetricsProvider().timer(NAMESPACE_PREFIX, this.name, CANDIDATE);
        mismatchCount = getMetricsProvider().counter(NAMESPACE_PREFIX, this.name, "mismatch");
        candidateExceptionCount = getMetricsProvider().counter(NAMESPACE_PREFIX, this.name, "candidate.exception");
        totalCount = getMetricsProvider().counter(NAMESPACE_PREFIX, this.name, "total");
        executor = executorService;
    }

    /**
     * Allow override here if extending the class
     */
    public MetricsProvider<?> getMetricsProvider() {
        return this.metricsProvider;
    }

    /**
     * Note that if {@code raiseOnMismatch} is true, {@link #runAsync(Callable, Callable)} will block waiting for
     * the candidate function to complete before it can raise any resulting errors. In situations where the candidate
     * function may be significantly slower than the control, it is <em>not</em> recommended to raise on mismatch.
     */
    public boolean getRaiseOnMismatch() {
        return raiseOnMismatch;
    }

    public String getName() {
        return name;
    }

    public T run(final Callable<T> control, final Callable<U> candidate) throws Exception {
        if (isAsync()) {
            return runAsync(control, candidate);
        } else {
            return runSync(control, candidate);
        }
    }

    private T runSync(final Callable<T> control, final Callable<U> candidate) throws Exception {
        Observation<T> controlObservation;
        Optional<Observation<U>> candidateObservation = Optional.empty();
        if (Math.random() < 0.5) {
            controlObservation = executeResult(CONTROL, controlTimer, control, true);
            if (runIf() && enabled()) {
                candidateObservation = Optional.of(executeResult(CANDIDATE, candidateTimer, candidate, false));
            }
        } else {
            if (runIf() && enabled()) {
                candidateObservation = Optional.of(executeResult(CANDIDATE, candidateTimer, candidate, false));
            }
            controlObservation = executeResult(CONTROL, controlTimer, control, true);
        }

        countExceptions(candidateObservation, candidateExceptionCount);
        IncompatibleTypesExperimentResult<T, U> result =
                new IncompatibleTypesExperimentResult<>(this, controlObservation, candidateObservation, context);
        publish(result);
        return controlObservation.getValue();
    }

    public T runAsync(final Callable<T> control, final Callable<U> candidate) throws Exception {
        Future<Optional<Observation<U>>> observationFutureCandidate;
        Future<Observation<T>> observationFutureControl;

        if (runIf() && enabled()) {
            if (Math.random() < 0.5) {
                observationFutureControl =
                        executor.submit(() -> executeResult(CONTROL, controlTimer, control, true));
                observationFutureCandidate = executor.submit(
                        () -> Optional.of(executeResult(CANDIDATE, candidateTimer, candidate, false)));
            } else {
                observationFutureCandidate = executor.submit(
                        () -> Optional.of(executeResult(CANDIDATE, candidateTimer, candidate, false)));
                observationFutureControl =
                        executor.submit(() -> executeResult(CONTROL, controlTimer, control, true));
            }
        } else {
            observationFutureControl = executor.submit(() -> executeResult(CONTROL, controlTimer, control, true));
            observationFutureCandidate = null;
        }

        Observation<T> controlObservation;
        try {
            controlObservation = observationFutureControl.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        Future<Void> publishedResult =
                executor.submit(() -> publishAsync(controlObservation, observationFutureCandidate));

        if (raiseOnMismatch) {
            try {
                publishedResult.get();
            } catch (ExecutionException e) {
                throw (Exception) e.getCause();
            }
        }

        return controlObservation.getValue();
    }

    private Void publishAsync(final Observation<T> controlObservation,
                              final Future<Optional<Observation<U>>> observationFutureCandidate) throws Exception {
        Optional<Observation<U>> candidateObservation = Optional.empty();
        if (observationFutureCandidate != null) {
            candidateObservation = observationFutureCandidate.get();
        }

        countExceptions(candidateObservation, candidateExceptionCount);
        IncompatibleTypesExperimentResult<T, U> result =
                new IncompatibleTypesExperimentResult<>(this, controlObservation, candidateObservation, context);
        publish(result);
        return null;
    }

    private void countExceptions(final Optional<Observation<U>> observation, final MetricsProvider.Counter exceptions) {
        if (observation.isPresent() && observation.get().getException().isPresent()) {
            exceptions.increment();
        }
    }

    public <X> Observation<X> executeResult(final String name, final MetricsProvider.Timer timer,
                                            final Callable<X> control, final boolean shouldThrow) throws Exception {
        Observation<X> observation = new Observation<>(name, timer);

        observation.time(() ->
        {
            try {
                observation.setValue(control.call());
            } catch (Exception e) {
                observation.setException(e);
            }
        });

        Optional<Exception> exception = observation.getException();
        if (shouldThrow && exception.isPresent()) {
            throw exception.get();
        }

        return observation;
    }

    protected boolean compareResults(final T controlVal, final U candidateVal) {
        return this.comparator.test(controlVal, candidateVal);
    }

    public boolean compare(final Observation<T> controlVal, final Observation<U> candidateVal)
            throws MismatchException {
        boolean resultsMatch = !candidateVal.getException().isPresent() &&
                compareResults(controlVal.getValue(), candidateVal.getValue());
        totalCount.increment();
        if (!resultsMatch) {
            mismatchCount.increment();
            handleComparisonMismatch(controlVal, candidateVal);
        }
        return true;
    }

    protected void publish(final IncompatibleTypesExperimentResult<T, U> result) {
    }

    protected boolean runIf() {
        return true;
    }

    protected boolean enabled() {
        return true;
    }

    protected boolean isAsync() {
        return false;
    }

    private void handleComparisonMismatch(final Observation<T> controlVal, final Observation<U> candidateVal)
            throws MismatchException {
        String msg;
        Optional<Exception> exception = candidateVal.getException();
        if (exception.isPresent()) {
            String stackTrace = Arrays.toString(exception.get().getStackTrace());
            String exceptionName = exception.get().getClass().getName();
            msg = candidateVal.getName() + " raised an exception: " + exceptionName + " " + stackTrace;
        } else {
            msg =
                    candidateVal.getName() + " does not match control value (" + controlVal.getValue().toString() + " != " +
                            candidateVal.getValue().toString() + ")";
        }
        throw new MismatchException(msg);
    }
}
