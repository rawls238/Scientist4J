package com.github.rawls238.scientist4j;

import io.dropwizard.metrics5.Timer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Observation<T> {

    private String name;
    private Optional<Exception> exception;
    private T value;
    private Timer.Context timerContext;
    private Timer timer;
    private long duration;

    public Observation(String name, Timer timer) {
      this.name = name;
      this.timer = timer;
      this.exception = Optional.empty();
    }

    public String getName() {
        return name;
    }

    public void setValue(T o) {
        this.value = o;
    }

    public T getValue() {
        return value;
    }

    public void setException(Exception e) {
        this.exception = Optional.of(e);
    }

    public Optional<Exception> getException() {
        return exception;
    }

    public void startTimer() {
        timerContext = timer.time();
    }

    public void endTimer() {
        duration = timerContext.stop();
    }

    public long getDuration() {
        return duration;
    }
}
