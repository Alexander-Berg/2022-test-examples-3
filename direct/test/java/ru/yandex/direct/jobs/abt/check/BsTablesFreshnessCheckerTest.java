package ru.yandex.direct.jobs.abt.check;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.ytcomponents.repository.StatsDynClusterFreshnessRepository;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BsTablesFreshnessCheckerTest {

    @Test
    void syncTableFreshBeforeNextDay() {
        var statsDynClusterFreshnessRepository = mock(StatsDynClusterFreshnessRepository.class);
        var ytProvider = mock(YtProvider.class);
        var bsTablesFreshnessChecker = new BsTablesFreshnessChecker(ytProvider, statsDynClusterFreshnessRepository);
        Instant calculateDate = Instant.ofEpochSecond(1586267683L); // 2020-04-07 13:54:43
        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(any(), anyString()))
                .thenReturn(ZonedDateTime.ofInstant(Instant.ofEpochSecond(1586303999L), // 2020-04-07 23:59:59
                        ZoneOffset.UTC));

        var isFresh = bsTablesFreshnessChecker.check(YtCluster.ZENO, "table", calculateDate);
        assertThat(isFresh).isFalse();

    }

    @Test
    void syncTableFreshInNextDay() {
        var statsDynClusterFreshnessRepository = mock(StatsDynClusterFreshnessRepository.class);
        var ytProvider = mock(YtProvider.class);
        var bsTablesFreshnessChecker = new BsTablesFreshnessChecker(ytProvider, statsDynClusterFreshnessRepository);
        Instant calculateDate = Instant.ofEpochSecond(1586267683L); // 2020-04-07 13:54:43
        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(any(), anyString()))
                .thenReturn(ZonedDateTime.ofInstant(Instant.ofEpochSecond(1586304000L), // 2020-04-07 23:59:59
                        ZoneOffset.UTC));

        var isFresh = bsTablesFreshnessChecker.check(YtCluster.ZENO, "table", calculateDate);
        assertThat(isFresh).isTrue();

    }
}
