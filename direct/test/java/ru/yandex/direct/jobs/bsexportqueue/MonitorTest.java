package ru.yandex.direct.jobs.bsexportqueue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.jobs.bsexportqueue.Monitor.CAMPAIGN_BINS;
import static ru.yandex.direct.jobs.bsexportqueue.Monitor.CLIENT_BINS;

class MonitorTest {

    @Test
    void clientBinsIsSubsetOfCampaignBins() {
        Set<Integer> ints = IntStream.of(CAMPAIGN_BINS)
                .boxed()
                .collect(Collectors.toSet());
        boolean isOk = Arrays.stream(CLIENT_BINS)
                .boxed()
                .allMatch(ints::contains);
        assertThat(isOk)
                .describedAs("CLIENT_BINS must be subset of CAMPAIGN_BINS")
                .isTrue();
    }
}
