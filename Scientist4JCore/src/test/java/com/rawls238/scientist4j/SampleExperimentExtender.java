package com.rawls238.scientist4j;

import static org.assertj.core.api.Assertions.assertThat;

public class SampleExperimentExtender<Integer> extends Experiment<Integer> {
  SampleExperimentExtender(String name) {
    super(name);
  }

  @Override
  protected void publish(Result r) {
    assertThat(r.getCandidate().getDuration()).isGreaterThan(0L);
    assertThat(r.getControl().getDuration()).isGreaterThan(0L);
  }
}