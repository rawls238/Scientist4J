package com.github.rawls238.scientist4j;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.github.rawls238.scientist4j.exceptions.MismatchException;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class ExperimentTest {

    private Integer exceptionThrowingFunction() {
        throw new RuntimeException("throw an exception");
    }

    private Integer safeFunction() {
        return 3;
    }

    private Integer sleepFunction() {
        try {
            Thread.sleep(1001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
            experiment.run(this::exceptionThrowingFunction, this::exceptionThrowingFunction);
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
            val = experiment.run(this::safeFunction, this::exceptionThrowingFunction);
        } catch (RuntimeException e) {
            candidateThrew = true;
        } catch (Exception e) {

        }
        assertThat(candidateThrew).isEqualTo(false);
        assertThat(val).isEqualTo(3);
    }

    @Test
    public void itThrowsOnMismatch() {
        Experiment<Integer> experiment = new Experiment("test", true);
        boolean candidateThrew = false;
        try {
            experiment.run(this::safeFunction, this::safeFunctionWithDifferentResult);
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
            val = exp.run(this::safeFunction, this::safeFunction);
        } catch (Exception e) {
            candidateThrew = true;
        }

        assertThat(val).isEqualTo(3);
        assertThat(candidateThrew).isEqualTo(false);
    }

    @Test
    public void nonAsyncRunsLongTime() {
        Experiment<Integer> exp = new Experiment("test", true);
        boolean candidateThrew = false;
        Integer val = 0;
        Date date1 = new Date();

        try {
            val = exp.run(this::sleepFunction, this::sleepFunction);
        } catch (Exception e) {
            candidateThrew = true;
        }
        Date date2 = new Date();
        long difference = date2.getTime() - date1.getTime();

        assertThat(difference).isGreaterThanOrEqualTo(2000);
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
    public void candidateExceptionsAreCounted() throws Exception {
        MetricRegistry metrics = new MetricRegistry();
        Experiment<Integer> exp = new Experiment("test", metrics);

        exp.run(() -> { return 1; }, this::exceptionThrowingFunction);

        Counter result = metrics.getCounters().get("scientist.test.candidate.exception");
        assertThat(result.getCount()).isEqualTo(1);
    }
}
