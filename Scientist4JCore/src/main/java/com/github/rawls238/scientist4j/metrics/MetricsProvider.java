package com.github.rawls238.scientist4j.metrics;

public interface MetricsProvider<T> {

    Timer timer(String... nameComponents);

    Counter counter(String... nameComponents);

    interface Timer {

        void record(Runnable runnable);

        /**
         * The duration recorded by this timer
         *
         * @return timer duration in nanoseconds
         */
        long getDuration();
    }

    interface Counter {

        void increment();
    }

    T getRegistry();

    void setRegistry(T registry);
}
