package com.github.rawls238.scientist4j;

import com.github.rawls238.scientist4j.metrics.MetricsProvider.Timer;

import java.util.Optional;

public class Observation<T> {

    private String name;
    private Optional<Exception> exception;
    private T value;
    private Timer timer;

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

    public long getDuration() {
        return timer.getDuration();
    }

    public void time(Runnable runnable) {
        timer.record(runnable);
    }
}
