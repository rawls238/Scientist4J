package com.github.rawls238.scientist4j;

import com.github.rawls238.scientist4j.exceptions.MismatchException;
import com.github.rawls238.scientist4j.metrics.DropwizardMetricsProvider;
import com.github.rawls238.scientist4j.metrics.MicrometerMetricsProvider;
import com.github.rawls238.scientist4j.metrics.NoopMetricsProvider;
import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.MetricName;
import org.junit.Test;

import java.util.Date;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ExperimentTest {

    private Integer exceptionThrowingFunction() {
        throw new ExpectingAnException("throw an exception");
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

    @Test(expected = ExpectingAnException.class)
    public void itThrowsAnExceptionWhenControlFails() throws Exception {
        new Experiment<Integer>("test", new NoopMetricsProvider())
                .run(this::exceptionThrowingFunction, this::exceptionThrowingFunction);
    }

    @Test
    public void itDoesntThrowAnExceptionWhenCandidateFails() throws Exception {
        Experiment<Integer> experiment = new Experiment<>("test", new NoopMetricsProvider());
        Integer val = experiment.run(this::safeFunction, this::exceptionThrowingFunction);
        assertThat(val).isEqualTo(3);
    }

    @Test(expected = MismatchException.class)
    public void itThrowsOnMismatch() throws Exception {
        new Experiment<Integer>("test", true, new NoopMetricsProvider())
                .run(this::safeFunction, this::safeFunctionWithDifferentResult);
    }

    @Test
    public void itDoesNotThrowOnMatch() throws Exception {
        Integer val = new Experiment<Integer>("test", true, new NoopMetricsProvider())
                .run(this::safeFunction, this::safeFunction);

        assertThat(val).isEqualTo(3);
    }

    @Test
    public void itHandlesNullValues() throws Exception {
        Integer val = new Experiment<Integer>("test", true, new NoopMetricsProvider())
                .run(() -> null, () -> null);

        assertThat(val).isNull();
    }

    @Test
    public void nonAsyncRunsLongTime() throws Exception {
        Experiment<Integer> exp = new Experiment<>("test", true, new NoopMetricsProvider());
        Date date1 = new Date();
        Integer val = exp.run(this::sleepFunction, this::sleepFunction);
        Date date2 = new Date();
        long difference = date2.getTime() - date1.getTime();

        assertThat(difference).isGreaterThanOrEqualTo(2000);
        assertThat(val).isEqualTo(3);
    }

    @Test
    public void itWorksWithAnExtendedClass() throws Exception {
        Experiment<Integer> exp = new TestPublishExperiment<>("test", new NoopMetricsProvider());
        exp.run(this::safeFunction, this::safeFunction);
    }

    @Test
    public void candidateExceptionsAreCounted_dropwizard() throws Exception {
        final DropwizardMetricsProvider provider = new DropwizardMetricsProvider();
        Experiment<Integer> exp = new Experiment<>("test", provider);

        exp.run(() -> 1, this::exceptionThrowingFunction);

        Counter result = provider.getRegistry().getCounters().get(MetricName.build("scientist", "test", "candidate", "exception"));
        assertThat(result.getCount()).isEqualTo(1);
    }

    @Test
    public void candidateExceptionsAreCounted_micrometer() throws Exception {
        final MicrometerMetricsProvider provider = new MicrometerMetricsProvider();
        Experiment<Integer> exp = new Experiment<>("test", provider);

        exp.run(() -> 1, this::exceptionThrowingFunction);

        io.micrometer.core.instrument.Counter result = provider.getRegistry().get("scientist.test.candidate.exception").counter();
        assertThat(result.count()).isEqualTo(1);
    }

    @Test
    public void shouldUseCustomComparator() throws Exception {
        @SuppressWarnings("unchecked") final BiFunction<Integer, Integer, Boolean> comparator = mock(BiFunction.class);
        when(comparator.apply(1, 2)).thenReturn(false);
        final Experiment<Integer> e = new ExperimentBuilder<Integer>()
                .withName("test")
                .withComparator(comparator)
                .withMetricsProvider(new NoopMetricsProvider())
                .build();

        e.run(() -> 1, () -> 2);

        verify(comparator).apply(1, 2);
    }
}

class ExpectingAnException extends RuntimeException {
    ExpectingAnException(final String message) {
        super(message);
    }
}
