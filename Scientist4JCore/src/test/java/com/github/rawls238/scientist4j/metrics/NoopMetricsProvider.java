package com.github.rawls238.scientist4j.metrics;

import com.github.rawls238.scientist4j.metrics.MetricsProvider;

/**
 * A  minimal in-memory {@link MetricsProvider} implementation, suitable for test environments.
 */
public class NoopMetricsProvider implements MetricsProvider<Object> {

    @Override
    public Timer timer(String... nameComponents) {

        return new Timer() {

            long duration;

            @Override
            public void record(Runnable runnable) {
                long now = System.nanoTime();
                runnable.run();

                duration = System.nanoTime() - now;
            }

            @Override
            public long getDuration() {
                return duration;
            }
        };
    }

    @Override
    public Counter counter(String... nameComponents) {
        return () -> {

        };
    }

    @Override
    public Object getRegistry() {
        return new Object();
    }

    @Override
    public void setRegistry(Object registry) {

    }
}
