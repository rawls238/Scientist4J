package com.github.rawls238.scientist4j;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rawls238.scientist4j.exceptions.MismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class Experiment<T> {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final String NAMESPACE_PREFIX = "scientist";
    private static final MetricRegistry metrics = new MetricRegistry();
    private final String name;
    private final boolean raiseOnMismatch;
    private Map<String, Object> context;
    private final Timer controlTimer;
    private final Timer candidateTimer;
    private final Counter mismatchCount;
    private final Counter candidateExceptionCount;
    private final Counter totalCount;
    private final BiFunction<T, T, Boolean> comparator;

    public Experiment() {
        this("Experiment");
    }

    public Experiment(String name) {
        this(name, false, null);
    }

    public Experiment(String name, MetricRegistry metricRegistry) {
        this(name, false, metricRegistry);
    }

    public Experiment(String name, Map<String, Object> context) {
        this(name, context, false, null);
    }

    public Experiment(String name, Map<String, Object> context, MetricRegistry metricRegistry) {
        this(name, context, false, metricRegistry);
    }

    public Experiment(String name, boolean raiseOnMismatch) {
        this(name, new HashMap<>(), raiseOnMismatch, null);
    }

    public Experiment(String name, boolean raiseOnMismatch, MetricRegistry metricRegistry) {
        this(name, new HashMap<>(), raiseOnMismatch, metricRegistry);
    }

    public Experiment(String name, Map<String, Object> context, boolean raiseOnMismatch, MetricRegistry metricRegistry) {
        this(name, context, raiseOnMismatch, metricRegistry, Objects::equals);
    }

    public Experiment(String name, Map<String, Object> context, boolean raiseOnMismatch, MetricRegistry metricRegistry, BiFunction<T, T, Boolean> comparator) {
        this.name = name;
        this.context = context;
        this.raiseOnMismatch = raiseOnMismatch;
        this.comparator = comparator;
        controlTimer = getMetrics(metricRegistry).timer(MetricRegistry.name(NAMESPACE_PREFIX, this.name, "control"));
        candidateTimer = getMetrics(metricRegistry).timer(MetricRegistry.name(NAMESPACE_PREFIX, this.name, "candidate"));
        mismatchCount = getMetrics(metricRegistry).counter(MetricRegistry.name(NAMESPACE_PREFIX, this.name, "mismatch"));
        candidateExceptionCount = getMetrics(metricRegistry).counter(MetricRegistry.name(NAMESPACE_PREFIX, this.name, "candidate.exception"));
        totalCount = getMetrics(metricRegistry).counter(MetricRegistry.name(NAMESPACE_PREFIX, this.name, "total"));
    }

    /**
     * Allow override here if extending the class, use the one passed into constructor if not null
     * or resort to the default created one internally
     */
    public MetricRegistry getMetrics(MetricRegistry metricRegistry) {
        return metricRegistry != null ? metricRegistry : metrics;
    }

    public boolean getRaiseOnMismatch() {
        return raiseOnMismatch;
    }

    public String getName() {
        return name;
    }

    public T run(Supplier<T> control, Supplier<T> candidate) throws Exception {
        if (isAsync()) {
            return runAsync(control, candidate);
        } else {
            return runSync(control, candidate);
        }
    }

    private T runSync(Supplier<T> control, Supplier<T> candidate) throws Exception {
        Observation<T> controlObservation;
        Optional<Observation<T>> candidateObservation = Optional.empty();
        if (Math.random() < 0.5) {
            controlObservation = executeResult("control", controlTimer, control, true);
            if (runIf() && enabled()) {
                candidateObservation = Optional.of(executeResult("candidate", candidateTimer, candidate, false));
            }
        } else {
            if (runIf() && enabled()) {
                candidateObservation = Optional.of(executeResult("candidate", candidateTimer, candidate, false));
            }
            controlObservation = executeResult("control", controlTimer, control, true);
        }

        countExceptions(candidateObservation, candidateExceptionCount);
        Result<T> result = new Result<T>(this, controlObservation, candidateObservation, context);
        publish(result);
        return controlObservation.getValue();
    }

    public T runAsync(Supplier<T> control, Supplier<T> candidate) throws Exception {
        Future<Optional<Observation<T>>> observationFutureCandidate = null;
        Future<Observation<T>> observationFutureControl;

        if (Math.random() < 0.5) {
            observationFutureControl = executor.submit(() -> executeResult("control", controlTimer, control, true));
            if (runIf() && enabled()) {
                observationFutureCandidate = executor.submit(() -> Optional.of(executeResult("candidate", candidateTimer, candidate, false)));
            }
        } else {
            if (runIf() && enabled()) {
                observationFutureCandidate = executor.submit(() -> Optional.of(executeResult("candidate", candidateTimer, candidate, false)));
            }
            observationFutureControl = executor.submit(() -> executeResult("control", controlTimer, control, true));
        }

        Observation<T> controlObservation;
        try {
            controlObservation = observationFutureControl.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        Optional<Observation<T>> candidateObservation = Optional.empty();
        if (observationFutureCandidate != null) {
            candidateObservation = observationFutureCandidate.get();
        }

        countExceptions(candidateObservation, candidateExceptionCount);
        Result<T> result = new Result<>(this, controlObservation, candidateObservation, context);
        publish(result);
        return controlObservation.getValue();
    }

    private void countExceptions(Optional<Observation<T>> observation, Counter exceptions) {
        if (observation.isPresent() && observation.get().getException().isPresent()) {
            exceptions.inc();
        }
    }

    public Observation<T> executeResult(String name, Timer timer, Supplier<T> control, boolean shouldThrow) throws Exception {
        Observation<T> observation = new Observation<>(name, timer);
        observation.startTimer();
        try {
            observation.setValue(control.get());
        } catch (Exception e) {
            observation.setException(e);
        } finally {
            observation.endTimer();
            if (shouldThrow && observation.getException().isPresent()) {
                throw observation.getException().get();
            }
            return observation;
        }
    }

    protected boolean compareResults(T controlVal, T candidateVal) {
        return comparator.apply(controlVal, candidateVal);
    }

    public boolean compare(Observation<T> controlVal, Observation<T> candidateVal) throws MismatchException {
        boolean resultsMatch = !candidateVal.getException().isPresent() && compareResults(controlVal.getValue(), candidateVal.getValue());
        totalCount.inc();
        if (!resultsMatch) {
            mismatchCount.inc();
            handleComparisonMismatch(controlVal, candidateVal);
        }
        return true;
    }

    protected void publish(Result<T> r) {
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

    private void handleComparisonMismatch(Observation<T> controlVal, Observation<T> candidateVal) throws MismatchException {
        String msg;
        if (candidateVal.getException().isPresent()) {
            String stackTrace = candidateVal.getException().get().getStackTrace().toString();
            String exceptionName = candidateVal.getException().get().getClass().getName();
            msg = new StringBuilder().append(candidateVal.getName()).append(" raised an exception: ")
                .append(exceptionName).append(" ").append(stackTrace).toString();
        } else {
            msg = new StringBuilder().append(candidateVal.getName()).append(" does not match control value (")
                .append(controlVal.getValue()).append(" != ").append(candidateVal.getValue()).append(")").toString();
        }
        throw new MismatchException(msg);
    }
}
