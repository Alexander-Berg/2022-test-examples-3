package ru.yandex.market.tsum.pipelines.apps.jobs.speed;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class GetPrevVersionRegressInfoJobTest {

    @Parameterized.Parameter
    public String input;

    @Parameterized.Parameter(1)
    public String output;

    private GetPrevVersionRegressInfoJob getPrevVersionRegressInfoJob;

    @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"0", "0"},
            {"1", "0"},
            {"1.0", "0.9"},
            {"0.9", "0.8"},
            {"0.0", "0.0"},
            {"1.00", "0.99"},
            {"0.79", "0.78"},
            {"1.0.1", "1.0.0"},
            {"1.1.0", "1.0.9"},
            {"2.00", "1.99"}
        });
    }

    @Before
    public void setUp() {
        getPrevVersionRegressInfoJob = new GetPrevVersionRegressInfoJob();
    }

    @Test
    public void decrementVersion() {
        assertEquals(getPrevVersionRegressInfoJob.decrementVersion(input), output);
    }
}
