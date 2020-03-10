package com.github.rawls238.scientist4j.metrics;

import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.Timer.Context;

import java.util.Arrays;

public class DropwizardMetricsProvider implements MetricsProvider<MetricRegistry> {

    private MetricRegistry registry;

    public DropwizardMetricsProvider() {
        this(new MetricRegistry());
    }

    public DropwizardMetricsProvider(MetricRegistry metricRegistry) {
        this.registry = metricRegistry;
    }

    @Override
    public Timer timer(String... nameComponents) {
        final io.dropwizard.metrics5.Timer timer = registry.timer(MetricRegistry.name(nameComponents[0], Arrays.copyOfRange(nameComponents, 1, nameComponents.length)));

        return new Timer() {

            long duration;

            @Override
            public void record(Runnable runnable) {

                final Context context = timer.time();

                try {
                    runnable.run();
                } finally {
                    duration = context.stop();
                }
            }

            @Override
            public long getDuration() {
                return duration;
            }
        };
    }

    @Override
    public Counter counter(String... nameComponents) {

        final io.dropwizard.metrics5.Counter counter = registry.counter(MetricRegistry.name(nameComponents[0], Arrays.copyOfRange(nameComponents, 1, nameComponents.length)));

        return new Counter() {

            @Override
            public void increment() {
                counter.inc();
            }
        };
    }

    @Override
    public MetricRegistry getRegistry() {
        return this.registry;
    }

    @Override
    public void setRegistry(MetricRegistry registry) {
        this.registry = registry;
    }
}
