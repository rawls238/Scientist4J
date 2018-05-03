package com.github.rawls238.scientist4j;

import io.dropwizard.metrics5.MetricRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

public class ExperimentBuilder<T> {
    private String name;
    private MetricRegistry registry;
    private BiFunction<T, T, Boolean> comparator;
    private Map<String, Object> context;
    private ExecutorService executorService;

    public ExperimentBuilder() {
        context = new HashMap<>();
        comparator = Object::equals;
        registry = new MetricRegistry();
    }

    public ExperimentBuilder<T> withName(final String name) {
        this.name = name;
        return this;
    }

    public ExperimentBuilder<T> withRegistry(final MetricRegistry registry) {
        this.registry = registry;
        return this;
    }

    public ExperimentBuilder<T> withComparator(final BiFunction<T, T, Boolean> comparator) {
        this.comparator = comparator;
        return this;
    }

    public ExperimentBuilder<T> withExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    public Experiment<T> build() {
        assert name != null;
        return new Experiment<>(name, context, false, registry, comparator,
            executorService);
    }
}
