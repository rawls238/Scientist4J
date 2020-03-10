package com.github.rawls238.scientist4j.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.TimeUnit;

public class MicrometerMetricsProvider implements MetricsProvider<MeterRegistry> {

    private MeterRegistry registry;

    public MicrometerMetricsProvider() {
        this(new SimpleMeterRegistry());
    }

    public MicrometerMetricsProvider(MeterRegistry meterRegistry) {
        this.registry = meterRegistry;
    }

    @Override
    public Timer timer(String... nameComponents) {

        final io.micrometer.core.instrument.Timer timer = io.micrometer.core.instrument.Timer.builder(String.join(".", nameComponents)).register(this.registry);

        return new Timer() {
            @Override
            public void record(Runnable runnable) {
                timer.record(runnable);
            }

            @Override
            public long getDuration() {
                return (long)timer.totalTime(TimeUnit.NANOSECONDS);
            }
        };
    }

    @Override
    public Counter counter(String... nameComponents) {

        final io.micrometer.core.instrument.Counter counter = io.micrometer.core.instrument.Counter.builder(String.join(".", nameComponents)).register(this.registry);

        return new Counter() {
            @Override
            public void increment() {
                counter.increment();
            }
        };
    }

    @Override
    public MeterRegistry getRegistry() {
        return registry;
    }

    @Override
    public void setRegistry(MeterRegistry registry) {
        this.registry = registry;
    }
}
