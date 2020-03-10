package com.github.rawls238.scientist4j;

import com.github.rawls238.scientist4j.metrics.MetricsProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPublishExperiment<Integer> extends Experiment<Integer> {
  TestPublishExperiment(String name, MetricsProvider<?> metricsProvider) {
    super(name, metricsProvider);
  }

  @Override
  protected void publish(Result<Integer> r) {
    assertThat(r.getCandidate().get().getDuration()).isGreaterThan(0L);
    assertThat(r.getControl().getDuration()).isGreaterThan(0L);
  }

}
