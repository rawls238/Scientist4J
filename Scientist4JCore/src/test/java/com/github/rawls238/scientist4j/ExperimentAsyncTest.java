package com.github.rawls238.scientist4j;

import com.github.rawls238.scientist4j.exceptions.MismatchException;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class ExperimentAsyncTest {

    private Integer exceptionThrowingFunction() {
      throw new RuntimeException("throw an exception");
    }

  private Integer sleepFunction() {
    try {
      Thread.sleep(1001);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return 3;
  }

    private Integer safeFunction() {
      return 3;
    }

    private Integer safeFunctionWithDifferentResult() {
      return 4;
    }

    @Test
    public void itThrowsAnExceptionWhenControlFails() {
      Experiment experiment = new Experiment("test");
      boolean controlThrew = false;
      try {
        experiment.runAsync(this::exceptionThrowingFunction, this::exceptionThrowingFunction);
      } catch (RuntimeException e) {
        controlThrew = true;
      } catch (Exception e) {

      }
      assertThat(controlThrew).isEqualTo(true);
    }

  @Test
  public void itDoesntThrowAnExceptionWhenCandidateFails() {
    Experiment<Integer> experiment = new Experiment("test");
    boolean candidateThrew = false;
    Integer val = 0;
    try {
      val = experiment.runAsync(this::safeFunction, this::exceptionThrowingFunction);
    } catch (Exception e) {
      candidateThrew = true;
    }
    assertThat(candidateThrew).isEqualTo(false);
    assertThat(val).isEqualTo(3);
  }

  @Test
  public void itThrowsOnMismatch() {
    Experiment<Integer> experiment = new Experiment("test", true);
    boolean candidateThrew = false;
    try {
      experiment.runAsync(this::safeFunction, this::safeFunctionWithDifferentResult);
    } catch (MismatchException e) {
      candidateThrew = true;
    } catch (Exception e) {

    }

    assertThat(candidateThrew).isEqualTo(true);
  }

  @Test
  public void itDoesNotThrowOnMatch() {
    Experiment<Integer> exp = new Experiment("test", true);
    boolean candidateThrew = false;
    Integer val = 0;
    try {
      val = exp.runAsync(this::safeFunction, this::safeFunction);
    } catch (Exception e) {
      candidateThrew = true;
    }

    assertThat(val).isEqualTo(3);
    assertThat(candidateThrew).isEqualTo(false);
  }

  @Test
  public void itWorksWithAnExtendedClass() {
    Experiment<Integer> exp = new TestPublishExperiment("test");
    try {
      exp.run(this::safeFunction, this::safeFunction);
    } catch (Exception e) {

    }
  }

  @Test
  public void asyncRunsFaster() {
    Experiment<Integer> exp = new Experiment("test", true);
    boolean candidateThrew = false;
    Integer val = 0;
    Date date1 = new Date();

    try {
      val = exp.runAsync(this::sleepFunction, this::sleepFunction);
    } catch (Exception e) {
      candidateThrew = true;
    }
    Date date2 = new Date();
    long difference = date2.getTime() - date1.getTime();

    assertThat(difference).isLessThan(2000);
    assertThat(difference).isGreaterThanOrEqualTo(1000);
    assertThat(val).isEqualTo(3);
    assertThat(candidateThrew).isEqualTo(false);
  }

}
