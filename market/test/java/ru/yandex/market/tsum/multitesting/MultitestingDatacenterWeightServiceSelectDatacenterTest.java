package ru.yandex.market.tsum.multitesting;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@ParametersAreNonnullByDefault
public class MultitestingDatacenterWeightServiceSelectDatacenterTest {
    private MultitestingDatacenterWeightService.WeightedDatacenters weightedDatacenters;
    private List<MultitestingDatacenterWeightService.DatacenterWithWeight> weights;

    @Before
    public void setUp() {
        weights = List.of(
            datacenterWithWeight("SAS", 2),
            datacenterWithWeight("IVA", 3)
        );
        weightedDatacenters = new MultitestingDatacenterWeightService.WeightedDatacenters(weights);
    }

    @Test
    public void totalWeightIsCorrect() {
        assertThat(weightedDatacenters.getTotalWeight()).isEqualTo(5);
    }

    @Test
    public void selectedDatacenterIsCorrect() {
        assertSoftly(softly -> {
            softly.assertThat(selectDatacenterByIndex(weights, 0)).isEqualTo("SAS");
            softly.assertThat(selectDatacenterByIndex(weights, 1)).isEqualTo("SAS");
            softly.assertThat(selectDatacenterByIndex(weights, 2)).isEqualTo("IVA");
            softly.assertThat(selectDatacenterByIndex(weights, 3)).isEqualTo("IVA");
            softly.assertThat(selectDatacenterByIndex(weights, 4)).isEqualTo("IVA");
        });
    }

    private static String selectDatacenterByIndex(
        List<MultitestingDatacenterWeightService.DatacenterWithWeight> weights,
        int selector) {
        return MultitestingDatacenterWeightService.WeightedDatacenters.selectDatacenterByIndex(
            weights, selector, "VLA");
    }

    private MultitestingDatacenterWeightService.DatacenterWithWeight datacenterWithWeight(String dc, int weight) {
        return new MultitestingDatacenterWeightService.DatacenterWithWeight(dc, weight);
    }
}
