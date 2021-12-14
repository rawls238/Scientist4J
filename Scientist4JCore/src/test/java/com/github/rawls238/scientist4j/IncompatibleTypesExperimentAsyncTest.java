package com.github.rawls238.scientist4j;

import com.github.rawls238.scientist4j.exceptions.MismatchException;
import com.github.rawls238.scientist4j.metrics.NoopMetricsProvider;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class IncompatibleTypesExperimentAsyncTest {

    private Integer exceptionThrowingFunction() {
        throw new RuntimeException("throw an exception");
    }

    private String exceptionThrowingCandidateFunction() {
        throw new RuntimeException("throw an exception");
    }

    private Integer sleepFunction() {
        try {
            Thread.sleep(1001);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return 3;
    }

    private String sleepClandidateFunction() {
        try {
            Thread.sleep(1001);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return "3";
    }

    private Integer safeFunction() {
        return 3;
    }

    private String safeCandidateFunction() {
        return "3";
    }

    private String safeCandidateFunctionWithDifferentResult() {
        return "4";
    }

    @Test
    public void itThrowsAnExceptionWhenControlFails() throws Exception {
        IncompatibleTypesExperiment<Integer, String> experiment = new IncompatibleTypesExperiment<>("test",
                new NoopMetricsProvider(), (integer, s) -> String.valueOf(integer).equals(s));
        boolean controlThrew = false;
        try {
            experiment.runAsync(this::exceptionThrowingFunction, this::exceptionThrowingCandidateFunction);
        } catch (RuntimeException e) {
            controlThrew = true;
        }
        assertThat(controlThrew).isTrue();
    }

    @Test
    public void itDoesntThrowAnExceptionWhenCandidateFails() {
        IncompatibleTypesExperiment<Integer, String> experiment = new IncompatibleTypesExperiment<>("test",
                new NoopMetricsProvider(), (integer, s) -> String.valueOf(integer).equals(s));
        boolean candidateThrew = false;
        Integer val = 0;
        try {
            val = experiment.runAsync(this::safeFunction, this::exceptionThrowingCandidateFunction);
        } catch (Exception e) {
            candidateThrew = true;
        }
        assertThat(candidateThrew).isFalse();
        assertThat(val).isEqualTo(3);
    }

    @Test
    public void itThrowsOnMismatch() {
        IncompatibleTypesExperiment<Integer, String> experiment = new IncompatibleTypesExperiment<>("test", true,
                new NoopMetricsProvider(), (integer, s) -> String.valueOf(integer).equals(s));
        boolean candidateThrew = false;
        try {
            experiment.runAsync(this::safeFunction, this::safeCandidateFunctionWithDifferentResult);
        } catch (MismatchException e) {
            candidateThrew = true;
        } catch (Exception e) {

        }

        assertThat(candidateThrew).isTrue();
    }

    @Test
    public void itDoesNotThrowOnMatch() {
        IncompatibleTypesExperiment<Integer, String> experiment = new IncompatibleTypesExperiment<>("test",
                new NoopMetricsProvider(), (integer, s) -> String.valueOf(integer).equals(s));
        boolean candidateThrew = false;
        Integer val = 0;
        try {
            val = experiment.runAsync(this::safeFunction, this::safeCandidateFunction);
        } catch (Exception e) {
            candidateThrew = true;
        }

        assertThat(val).isEqualTo(3);
        assertThat(candidateThrew).isFalse();
    }

    @Test
    public void itWorksWithAnExtendedClass() {
        IncompatibleTypesExperiment<Integer, String> exp = new TestPublishIncompatibleTypesExperiment("test",
                new NoopMetricsProvider(), (integer, s) -> String.valueOf(integer).equals(s));
        try {
            exp.run(this::safeFunction, this::safeCandidateFunction);
        } catch (Exception e) {

        }
    }

    @Test
    public void asyncRunsFaster() {
        IncompatibleTypesExperiment<Integer, String> exp = new IncompatibleTypesExperiment<>("test",
                new NoopMetricsProvider(), (integer, s) -> String.valueOf(integer).equals(s));
        boolean candidateThrew = false;
        Integer val = 0;
        Date date1 = new Date();

        try {
            val = exp.runAsync(this::sleepFunction, this::sleepClandidateFunction);
        } catch (Exception e) {
            candidateThrew = true;
        }
        Date date2 = new Date();
        long difference = date2.getTime() - date1.getTime();

        assertThat(difference).isLessThan(2000);
        assertThat(difference).isGreaterThanOrEqualTo(1000);
        assertThat(val).isEqualTo(3);
        assertThat(candidateThrew).isFalse();
    }
}
