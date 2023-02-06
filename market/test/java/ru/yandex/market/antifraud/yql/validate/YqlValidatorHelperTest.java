package ru.yandex.market.antifraud.yql.validate;

import com.google.common.collect.ImmutableBiMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.yql.dayclose.YqlAfDayclose;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YqlSessionType;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.model.YtLogConfig;
import ru.yandex.market.antifraud.yql.yt.YtTablesHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YqlValidatorHelperTest {
    private static final int VALIDATION_DAY = 20100730;
    private YqlValidatorHelper yqlValidatorHelper;
    private YqlAfDayclose yqlAfDayclose;
    private LoggingJdbcTemplate jdbcTemplate;

    @Before
    public void initYqlValidatorHelper() {
        YtLogConfig logConfigMock = mock(YtLogConfig.class);
        when(logConfigMock.getScales()).thenReturn(ImmutableBiMap.of(
                UnvalidatedDay.Scale.ARCHIVE, "1d",
                UnvalidatedDay.Scale.RECENT, "30min"
        ));
        when(logConfigMock.getStepEventPublish()).thenReturn(new YtLogConfig.StepEventName("sename"));
        when(logConfigMock.getScales()).thenReturn(ImmutableBiMap.of(UnvalidatedDay.Scale.ARCHIVE, "any"));
        when(logConfigMock.getStepEventPublish()).thenReturn(new YtLogConfig.StepEventName("sename"));
        when(logConfigMock.getStepEventPublish()).thenReturn(new YtLogConfig.StepEventName("sename"));
        when(logConfigMock.getLogName()).thenReturn(new YtLogConfig.LogName("any"));

        yqlAfDayclose = mock(YqlAfDayclose.class);
        jdbcTemplate = mock(LoggingJdbcTemplate.class);
        when(jdbcTemplate.<Long>query(
            Mockito.anyString(),
            Mockito.anyString(), Mockito.any(), // :/
            Mockito.anyString(), Mockito.any(), // :)
            Mockito.anyString(), Mockito.any(), // ͡°ᴥ͡°
            Mockito.anyString(), Mockito.any(), // ʕ·͡ᴥ·ʔ
            Mockito.anyString(), Mockito.any(), // ʕっ•ᴥ•ʔっ
            Mockito.anyString(), Mockito.any(), // ʕノ•ᴥ•ʔノ ︵ ┻━┻
            Mockito.any(Class.class)))
            .thenReturn(1L);

        YtConfig ytConfig = mock(YtConfig.class);
        when(ytConfig.getCluster()).thenReturn("any");

        yqlValidatorHelper = new YqlValidatorHelper(
                ytConfig,
                jdbcTemplate,
                logConfigMock,
                mock(YtTablesHelper.class),
                Collections.emptyList(),
                yqlAfDayclose,
                mock(YqlSessionToTxMap.class)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustFailArchive() {
        yqlValidatorHelper.checkSeenPartitions(
                new UnvalidatedDay(VALIDATION_DAY, 0L, UnvalidatedDay.Scale.ARCHIVE),
                new TreeSet<>(Arrays.asList("2010-07-30", "2010-07-31")));
    }

    @Test(expected = RuntimeException.class)
    public void mustFailRecent() {
        yqlValidatorHelper.checkSeenPartitions(
                new UnvalidatedDay(VALIDATION_DAY, 0L, UnvalidatedDay.Scale.RECENT),
                new TreeSet<>(IntStream
                        .range(0, 49)
                        .mapToObj((i) -> "2010-07-30T" + i)
                        .collect(Collectors.toList())));
    }

    @Test
    public void testGetNormalSessionType() {
        when(yqlAfDayclose.isClosed(Mockito.anyInt())).thenReturn(true);
        assertThat(yqlValidatorHelper.getSessionType(
            VALIDATION_DAY),
            is(YqlSessionType.NORMAL));
    }

    @Test
    public void testGetDayclosingSessionType() {
        when(yqlAfDayclose.isClosed(Mockito.anyInt())).thenReturn(false);
        assertThat(yqlValidatorHelper.getSessionType(VALIDATION_DAY),
            is(YqlSessionType.DAYCLOSING));
    }
}
