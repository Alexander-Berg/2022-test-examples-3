package ru.yandex.market.olap2.step.model;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class StepEventParamsTest {
    @Test
    public void partitionToInt() throws Exception {
        StepEventParams pnull = new StepEventParams();
        pnull.setPartition(null);
        assertThat(pnull.partitionToInt(), nullValue());
        StepEventParams pfilled = new StepEventParams();
        pfilled.setPartition("2018-03");
        assertThat(pfilled.partitionToInt(), is(201803));
    }

    @Test(expected = NumberFormatException.class)
    public void partitionToIntmustFail() throws Exception {
        StepEventParams pnull = new StepEventParams();
        pnull.setPartition("2018_03");
        pnull.partitionToInt();
    }

}
