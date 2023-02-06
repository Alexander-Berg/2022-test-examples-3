package ru.yandex.market.antifraud.yql.model;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSortedSet;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import ru.yandex.market.antifraud.yql.validate.YqlSessionHelper;
import ru.yandex.market.antifraud.yql.yt.YtTablesHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class YqlSessionTest {

    @Test
    public void testTwoDaysPartitions() {
        YtConfig ytConfigMock = mock(YtConfig.class);
        when(ytConfigMock.getLogPath(
            ArgumentMatchers.any(YtLogConfig.class),
            ArgumentMatchers.any(UnvalidatedDay.Scale.class),
            anyString())).thenAnswer((Answer<String>) invocation -> {
            UnvalidatedDay.Scale scale = invocation.getArgument(1);
            return "//logs/testlog/" +
                (scale == UnvalidatedDay.Scale.ARCHIVE ?
                    "1d/" : "30min/") + invocation.<String>getArgument(2);

        });

        YtTablesHelper ytTablesHelperMock = mock(YtTablesHelper.class);
        when(ytTablesHelperMock.exists(ArgumentMatchers.startsWith("//logs/testlog/1d/")))
            .thenReturn(false);
        when(ytTablesHelperMock.exists(ArgumentMatchers.startsWith("//logs/testlog/30min")))
            .thenReturn(true);

        YtLogConfig ytLogConfigMock = mock(YtLogConfig.class);
        when(ytLogConfigMock.getScales()).thenReturn(ImmutableBiMap.of(
            UnvalidatedDay.Scale.RECENT, "30min",
            UnvalidatedDay.Scale.ARCHIVE, "1d"));

        YqlSessionHelper yqlSessionHelper = new YqlSessionHelper(ytConfigMock, ytTablesHelperMock, ytLogConfigMock);
        List<String> prevDayPartitions = YqlSession.generatePrevDayPartitions(
            new UnvalidatedDay(20171028, -1L, UnvalidatedDay.Scale.RECENT),
            ImmutableSortedSet.of("2018-10-28T00:00:00"),
            yqlSessionHelper
        );

        assertEquals(RECENT_PARTITIONS,prevDayPartitions);
    }

    private static final List<String> RECENT_PARTITIONS = Arrays.asList("//logs/testlog/30min/2018-10-27T00:30:00",
        "//logs/testlog/30min/2018-10-27T00:00:00",
        "//logs/testlog/30min/2018-10-27T01:30:00",
        "//logs/testlog/30min/2018-10-27T01:00:00",
        "//logs/testlog/30min/2018-10-27T02:30:00",
        "//logs/testlog/30min/2018-10-27T02:00:00",
        "//logs/testlog/30min/2018-10-27T03:30:00",
        "//logs/testlog/30min/2018-10-27T03:00:00",
        "//logs/testlog/30min/2018-10-27T04:30:00",
        "//logs/testlog/30min/2018-10-27T04:00:00",
        "//logs/testlog/30min/2018-10-27T05:30:00",
        "//logs/testlog/30min/2018-10-27T05:00:00",
        "//logs/testlog/30min/2018-10-27T06:30:00",
        "//logs/testlog/30min/2018-10-27T06:00:00",
        "//logs/testlog/30min/2018-10-27T07:30:00",
        "//logs/testlog/30min/2018-10-27T07:00:00",
        "//logs/testlog/30min/2018-10-27T08:30:00",
        "//logs/testlog/30min/2018-10-27T08:00:00",
        "//logs/testlog/30min/2018-10-27T09:30:00",
        "//logs/testlog/30min/2018-10-27T09:00:00",
        "//logs/testlog/30min/2018-10-27T10:30:00",
        "//logs/testlog/30min/2018-10-27T10:00:00",
        "//logs/testlog/30min/2018-10-27T11:30:00",
        "//logs/testlog/30min/2018-10-27T11:00:00",
        "//logs/testlog/30min/2018-10-27T12:30:00",
        "//logs/testlog/30min/2018-10-27T12:00:00",
        "//logs/testlog/30min/2018-10-27T13:30:00",
        "//logs/testlog/30min/2018-10-27T13:00:00",
        "//logs/testlog/30min/2018-10-27T14:30:00",
        "//logs/testlog/30min/2018-10-27T14:00:00",
        "//logs/testlog/30min/2018-10-27T15:30:00",
        "//logs/testlog/30min/2018-10-27T15:00:00",
        "//logs/testlog/30min/2018-10-27T16:30:00",
        "//logs/testlog/30min/2018-10-27T16:00:00",
        "//logs/testlog/30min/2018-10-27T17:30:00",
        "//logs/testlog/30min/2018-10-27T17:00:00",
        "//logs/testlog/30min/2018-10-27T18:30:00",
        "//logs/testlog/30min/2018-10-27T18:00:00",
        "//logs/testlog/30min/2018-10-27T19:30:00",
        "//logs/testlog/30min/2018-10-27T19:00:00",
        "//logs/testlog/30min/2018-10-27T20:30:00",
        "//logs/testlog/30min/2018-10-27T20:00:00",
        "//logs/testlog/30min/2018-10-27T21:30:00",
        "//logs/testlog/30min/2018-10-27T21:00:00",
        "//logs/testlog/30min/2018-10-27T22:30:00",
        "//logs/testlog/30min/2018-10-27T22:00:00",
        "//logs/testlog/30min/2018-10-27T23:30:00",
        "//logs/testlog/30min/2018-10-27T23:00:00");

}
