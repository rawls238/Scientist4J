package com.rawls238.scientist4j;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.rawls238.scientist4j.exceptions.MismatchException;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Experiment<T> {

    private static final String NAMESPACE_PREFIX = "scientist";
    static final MetricRegistry metrics = new MetricRegistry();
    private final String name;
    private final boolean raiseOnMismatch;
    private final Timer controlTimer;
    private final Timer candidateTimer;
    private final Counter mismatchCount;
    private final Counter totalCount;

    public Experiment() {
        this("Experiment");
    }

    public Experiment(String name) {
        this(name, false);
    }

    public Experiment(String name, boolean raiseOnMismatch) {
        this.name = name;
        this.raiseOnMismatch = raiseOnMismatch;
        controlTimer = metrics.timer(MetricRegistry.name(NAMESPACE_PREFIX, "control"));
        candidateTimer = metrics.timer(MetricRegistry.name(NAMESPACE_PREFIX, "candidate"));
        mismatchCount = metrics.counter(MetricRegistry.name(NAMESPACE_PREFIX, "mismatch"));
        totalCount = metrics.counter(MetricRegistry.name(NAMESPACE_PREFIX, "total"));
    }

    static void startReport() {
        startReport(1, TimeUnit.SECONDS);
    }

    static void startReport(int pollingInterval, TimeUnit timeUnit) {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(pollingInterval, timeUnit);
    }

    public boolean getRaiseOnMismatch() {
        return raiseOnMismatch;
    }

    public String getName() {
        return name;
    }

    public T run(String name, Supplier<T> control, Supplier<T> candidate) {
        Observation<T> controlObservation = executeResult("control", controlTimer, control, true);
        Observation<T> candidateObservation = executeResult(name, candidateTimer, candidate, false);
        Result result = new Result(this, controlObservation, candidateObservation);
        publish(result);
        return controlObservation.getValue();
    }

    public Observation executeResult(String name, Timer timer, Supplier<T> control, boolean shouldThrow) {
        Observation<T> observation = new Observation<T>(name, timer);
        observation.startTimer();
        try {
            observation.setValue(control.get());
        } catch (Exception e) {
            if (shouldThrow) {
                throw e;
            }
            observation.setException(e);
        } finally {
            observation.endTimer();
            return observation;
        }
    }

    public boolean compare(Observation<T> controlVal, Observation<T> candidateVal) throws MismatchException {
        boolean resultsMatch = !candidateVal.getException().isPresent() && controlVal.getValue().equals(candidateVal.getValue());
        totalCount.inc();
        if (!resultsMatch) {
            mismatchCount.inc();
            handleComparisonMismatch(controlVal, candidateVal);
        }
        return true;
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

    protected void publish(Result r) {
        return;
    }
}
