package ru.yandex.market.checkout.checkouter.report;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimentsTest {

    @Test
    void shouldCreateEmptyExperimentsWhenNullProvided() {
        Experiments experiments = Experiments.fromString(List.of());

        assertEquals("", experiments.toExperimentString());
    }

    @Test
    void shouldCreateEmptyExperimentsWhenEmptyStringProvided() {
        Experiments experiments = Experiments.fromString("");

        assertEquals("", experiments.toExperimentString());
    }

    @Test
    void shouldCreateEmptyExperiments() {
        Experiments experiments = Experiments.empty();

        assertEquals("", experiments.toExperimentString());
    }


    @Test
    void shouldCreateExperimentsWhenSingleExperimentProvided() {
        Experiments experiments = Experiments.fromString("exp=1");

        assertEquals("exp=1", experiments.toExperimentString());
    }

    @Test
    void shouldCreateExperimentsWhenSeveralExperimentProvided() {
        Experiments experiments = Experiments.fromString("exp1=1; ; exp2=2 ; exp3=3");

        assertThat(Arrays.asList(experiments.toExperimentString().split(";")),
                containsInAnyOrder("exp1=1", "exp2=2", "exp3=3"));
    }

    @Test
    void shouldAddExperiment() {
        Experiments experiments = Experiments.fromString("exp1=1");
        experiments.addExperiment("exp2", "2");

        assertThat(Arrays.asList(experiments.toExperimentString().split(";")),
                containsInAnyOrder("exp1=1", "exp2=2"));
    }

    @Test
    void shouldAddMultiValueExperiments() {
        Experiments experiments = Experiments.fromString("exp1=1;exp1=2;exp1=3");
        experiments.addExperiment("exp1", "4");

        assertTrue(experiments.hasExperiment("exp1", "1"));
        assertTrue(experiments.hasExperiment("exp1", "2"));
        assertTrue(experiments.hasExperiment("exp1", "3"));
        assertTrue(experiments.hasExperiment("exp1", "4"));

        assertThat(Arrays.asList(experiments.toExperimentString().split(";")),
                containsInAnyOrder("exp1=1", "exp1=2", "exp1=3", "exp1=4"));

    }

    @Test
    void shouldReturnTrueOnHasExperimentWhenHasExperiment() {
        Experiments experiments = Experiments.fromString("exp1=1;exp2=2");

        assertTrue(experiments.hasExperiment("exp1", "1"));
    }

    @Test
    void shouldReturnFalseOnHasExperimentWhenDoesNotHaveExperiment() {
        Experiments experiments = Experiments.fromString("exp1=1;exp2=2");

        assertFalse(experiments.hasExperiment("exp3", "3"));
        assertFalse(experiments.hasExperiment("exp2", "3"));
        assertFalse(experiments.hasExperiment("exp2", null));
    }

    @Test
    void shouldReturnTrueOnIsEmptyWhenEmpty() {
        Experiments experiments = Experiments.empty();

        assertTrue(experiments.isEmpty());
        assertFalse(experiments.isNotEmpty());
    }

    @Test
    void shouldReturnFalseOnIsEmptyWhenEmpty() {
        Experiments experiments = Experiments.fromString("exp1=1");

        assertTrue(experiments.isNotEmpty());
        assertFalse(experiments.isEmpty());
    }

    @Test
    void emptyValueAcceptable() {
        Experiments experiments = Experiments.fromString("exp1");

        assertTrue(experiments.hasExperiment("exp1"));
        assertTrue(experiments.hasExperiment("exp1", null));
        assertNull(experiments.getExperimentValue("exp1"));
    }

    @Test
    void emptyValueSerializationWithoutEqualSign() {
        Experiments experiments = Experiments.fromString("exp1");

        assertEquals("exp1", experiments.toExperimentString());
    }
}
