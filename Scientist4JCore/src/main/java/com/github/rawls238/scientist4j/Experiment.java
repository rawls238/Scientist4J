package com.github.rawls238.scientist4j;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rawls238.scientist4j.exceptions.MismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Experiment<T> {

    private static final String NAMESPACE_PREFIX = "scientist";
    static final MetricRegistry metrics = new MetricRegistry();
    private final String name;
    private final boolean raiseOnMismatch;
    private Map<String, Object> context;
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

    public Experiment(String name, Map<String, Object> context) {
      this(name, context, false);
    }

    public Experiment(String name, boolean raiseOnMismatch) {
      this(name, new HashMap(), raiseOnMismatch);
    }

    public Experiment(String name, Map<String, Object> context, boolean raiseOnMismatch) {
      this.name = name;
      this.context = context;
      this.raiseOnMismatch = raiseOnMismatch;
      controlTimer = metrics.timer(MetricRegistry.name(NAMESPACE_PREFIX, this.name, "control"));
      candidateTimer = metrics.timer(MetricRegistry.name(NAMESPACE_PREFIX, this.name, "candidate"));
      mismatchCount = metrics.counter(MetricRegistry.name(NAMESPACE_PREFIX, this.name, "mismatch"));
      totalCount = metrics.counter(MetricRegistry.name(NAMESPACE_PREFIX, this.name, "total"));
    }

    public boolean getRaiseOnMismatch() {
        return raiseOnMismatch;
    }

    public String getName() {
        return name;
    }

    public T run(Supplier<T> control, Supplier<T> candidate) throws Exception {
      Observation<T> controlObservation;
      Optional<Observation> candidateObservation = Optional.empty();
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
      Result<T> result = new Result(this, controlObservation, candidateObservation, context);
      publish(result);
      return controlObservation.getValue();
    }

    public Observation executeResult(String name, Timer timer, Supplier<T> control, boolean shouldThrow) throws Exception {
      Observation<T> observation = new Observation(name, timer);
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
      return controlVal.equals(candidateVal);
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
      return;
    }

    protected boolean runIf() {
      return true;
    }

    protected boolean enabled() {
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
}
