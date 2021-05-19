package com.github.rawls238.scientist4j;

import com.github.rawls238.scientist4j.metrics.MetricsProvider;

import java.util.function.BiPredicate;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPublishIncompatibleTypesExperiment extends IncompatibleTypesExperiment<Integer, String> {
    TestPublishIncompatibleTypesExperiment(String name, MetricsProvider<?> metricsProvider, BiPredicate<Integer, String> comparator) {
        super(name, metricsProvider, comparator);
    }

    @Override
    protected void publish(IncompatibleTypesExperimentResult<Integer, String> result) {
        assertThat(result.getCandidate().get().getDuration()).isGreaterThan(0L);
        assertThat(result.getControl().getDuration()).isGreaterThan(0L);
    }
}
