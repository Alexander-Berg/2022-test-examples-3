package ru.yandex.market.logshatter.reader.logbroker.monitoring;

import com.github.fakemongo.junit.FongoRule;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.market.logbroker.pull.LogBrokerClient;
import ru.yandex.market.logbroker.pull.LogBrokerOffset;
import ru.yandex.market.logbroker.pull.LogBrokerPartition;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.config.ConfigValidationException;
import ru.yandex.market.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.config.LogSource;
import ru.yandex.market.logshatter.reader.logbroker.LogBrokerConfigurationService;
import ru.yandex.market.logshatter.reader.logbroker.PartitionDao;
import ru.yandex.market.logshatter.reader.logbroker.PartitionManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.monitoring.MonitoringStatus.CRITICAL;
import static ru.yandex.market.monitoring.MonitoringStatus.OK;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 25.09.2018
 */
public class LogBrokerLagWriterOldApiTest {
    @Rule
    public FongoRule fongoRule = new FongoRule();

    private PartitionDao partitionDao;

    private final LogBrokerClient logBrokerClientMock = mock(LogBrokerClient.class);
    private final PartitionManager partitionManagerMock = mock(PartitionManager.class);
    private final LogShatterMonitoring logShatterMonitoring = new LogShatterMonitoring();

    private LogBrokerConfigurationService logBrokerConfigurationService;

    @Before
    public void setUp() throws ConfigValidationException {
        partitionDao = new PartitionDao(fongoRule.getDatabase());

        when(logBrokerClientMock.getDc()).thenReturn("dc1");

        logBrokerConfigurationService = new LogBrokerConfigurationService(
            Collections.singletonList(
                LogShatterConfig.newBuilder()
                    .setConfigFileName("config.json")
                    .setSources(Collections.singletonList(LogSource.create("logbroker://ident1--logType1")))
                    .build()
            ),
            ""
        );
    }

    @Test
    public void shouldInsert_whenThereIsNoOffsetInMongo() throws Exception {
        LogBrokerOffset offset1 = new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 3, 2, 4, 1, "owner1", "dc1");
        givenOffsetsInMongo();
        givenOffsetsInLogBroker("ident1", offset1);
        runLogBrokerLagWriter();
        assertThatOffsetsInMongoAre(offset1);
    }

    @Test
    public void shouldUpdate_whenOffsetIsGreaterInMongo() throws Exception {
        givenOffsetsInMongo(new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 3, 2, 4, 1, "owner1", "dc1"));
        givenOffsetsInLogBroker("ident1", new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 40, 10, "owner2", "dc2"));
        runLogBrokerLagWriter();
        assertThatOffsetsInMongoAre(new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 40, 10, "owner2", "dc2"));
    }

    @Test
    public void shouldDoNothing_whenOffsetIsGreaterInLogBroker() throws Exception {
        givenOffsetsInMongo(new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 40, 10, "owner1", "dc1"));
        givenOffsetsInLogBroker("ident1", new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 3, 2, 4, 1, "owner2", "dc2"));
        runLogBrokerLagWriter();
        assertThatOffsetsInMongoAre(new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 40, 10, "owner1", "dc1"));
    }

    @Test
    public void shouldUpdate_whenOffsetsAreEqual_andLogSizeIsGreaterInLogBroker() throws Exception {
        givenOffsetsInMongo(new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 40, 10, "owner1", "dc1"));
        givenOffsetsInLogBroker("ident1", new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 41, 10, "owner2", "dc2"));
        runLogBrokerLagWriter();
        assertThatOffsetsInMongoAre(new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 41, 10, "owner2", "dc2"));
    }

    @Test
    public void shouldDoNothing_whenOffsetsAreEqual_andLogSizeIsGreaterInMongo() throws Exception {
        givenOffsetsInMongo(new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 41, 10, "owner1", "dc1"));
        givenOffsetsInLogBroker("ident1", new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 40, 10, "owner2", "dc2"));
        runLogBrokerLagWriter();
        assertThatOffsetsInMongoAre(new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 41, 10, "owner1", "dc1"));
    }

    @Test
    public void shouldDoNothing_whenOffsetsAreEqual_andLogSizesAreEqual() throws Exception {
        givenOffsetsInMongo(new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 40, 10, "owner1", "dc1"));
        givenOffsetsInLogBroker("ident1", new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 40, 10, "owner2", "dc2"));
        runLogBrokerLagWriter();
        assertThatOffsetsInMongoAre(new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 30, 20, 40, 10, "owner2", "dc2"));
    }

    @Test
    public void shouldRetryIfThingsFail() throws Exception {
        PartitionDao partitionDaoMock = mock(PartitionDao.class);

        when(logBrokerClientMock.getDc()).thenReturn("dc1");

        when(logBrokerClientMock.getOffsets("ident1", "logType1"))
            .thenThrow(RuntimeException.class)
            .thenReturn(Arrays.asList(
                new LogBrokerOffset("rt3.dc1--ident1--logType1:1", 10, 0, 30, 20, "owner1", "dc1"),
                new LogBrokerOffset("rt3.dc1--ident1--logType1:2", 10, 0, 30, 20, "owner1", "dc1")
            ));

        when(logBrokerClientMock.getTopics("ident1", "logType1"))
            .thenThrow(RuntimeException.class)
            .thenReturn(Arrays.asList("rt3.dc1--ident1--logType1"));

        when(logBrokerClientMock.getSuggestPartitions(Arrays.asList("rt3.dc1--ident1--logType1")))
            .thenThrow(RuntimeException.class)
            .thenReturn(Arrays.asList(
                new LogBrokerPartition("rt3.dc1--ident1--logType1:1", "host1"),
                new LogBrokerPartition("rt3.dc1--ident1--logType1:2", "host1")
            ));

        doThrow(RuntimeException.class)
            .doNothing()
            .when(partitionDaoMock).advanceOffsets(any());

        doThrow(RuntimeException.class)
            .doNothing()
            .when(partitionManagerMock).updatePartitions(any(), any());


        new LogBrokerLagWriterOldApi(
            logBrokerClientMock,
            partitionDaoMock,
            partitionManagerMock,
            logBrokerConfigurationService,
            logShatterMonitoring,
            6,
            0
        ).run();


        assertEquals(OK, logShatterMonitoring.getOverallResult().getStatus());
    }

    @Test
    public void shouldCritIfRetryingDidNotHelp() throws Exception {
        when(logBrokerClientMock.getOffsets("ident1", "logType1"))
            .thenThrow(RuntimeException.class);

        new LogBrokerLagWriterOldApi(
            logBrokerClientMock,
            mock(PartitionDao.class),
            partitionManagerMock,
            logBrokerConfigurationService,
            logShatterMonitoring,
            3,
            0
        ).run();

        assertEquals(CRITICAL, logShatterMonitoring.getOverallResult().getStatus());
    }


    private void givenOffsetsInMongo(LogBrokerOffset... offsets) {
        Stream.of(offsets).forEach(partitionDao::save);
    }

    private void givenOffsetsInLogBroker(String ident, LogBrokerOffset... offsets) throws IOException {
        when(logBrokerClientMock.getOffsets("ident1", "logType1"))
            .thenReturn(Arrays.asList(offsets));
    }

    private void runLogBrokerLagWriter() throws Exception {
        new LogBrokerLagWriterOldApi(
            logBrokerClientMock,
            partitionDao,
            partitionManagerMock,
            logBrokerConfigurationService,
            logShatterMonitoring,
            5,
            0
        ).run();

        assertEquals(OK, logShatterMonitoring.getOverallResult().getStatus());
    }

    private void assertThatOffsetsInMongoAre(LogBrokerOffset... offsets) {
        assertThat(partitionDao.getAll())
            .extracting(
                LogBrokerOffset::getPartition, LogBrokerOffset::getOffset, LogBrokerOffset::getLogStart,
                LogBrokerOffset::getLogEnd, LogBrokerOffset::getLag, LogBrokerOffset::getDc
            )
            .contains(
                Stream.of(offsets)
                    .map(offset -> tuple(
                        offset.getPartition(),
                        offset.getOffset(),
                        offset.getLogStart(),
                        offset.getLogEnd(),
                        offset.getLag(),
                        offset.getDc()
                    ))
                    .toArray(Tuple[]::new)
            );
    }
}
