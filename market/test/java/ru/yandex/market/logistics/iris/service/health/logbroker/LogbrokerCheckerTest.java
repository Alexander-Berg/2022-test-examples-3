package ru.yandex.market.logistics.iris.service.health.logbroker;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.iris.configuration.logbroker.LogBrokerProperties;
import ru.yandex.market.logistics.iris.service.health.HealthUtil;
import ru.yandex.market.logistics.iris.service.logbroker.consumer.dao.LogbrokerOffsetDao;

public class LogbrokerCheckerTest {

    private static final String TOPIC = "some-topic";
    private static final String SOURCE = "some-prefix";
    private static final String ENVIRONMENT = "env";
    private static final String TOPIC_PATH = SOURCE + "/" + ENVIRONMENT + "/" + TOPIC;
    private static final String TOPIC_NAME = "rt3.kafka-bs--" + SOURCE + "@" + ENVIRONMENT + "--" + TOPIC;

    private final LogbrokerOffsetDao logbrokerOffsetDao = Mockito.mock(LogbrokerOffsetDao.class);
    private final LogBrokerProperties logBrokerProperties = Mockito.mock(LogBrokerProperties.class);
    private final LogbrokerChecker logbrokerChecker = new LogbrokerCheckerImpl(logbrokerOffsetDao, logBrokerProperties);


    @BeforeEach
    public void init() {
        Mockito.when(logBrokerProperties.getMdmTopicToPull()).thenReturn(TOPIC_PATH);
        Mockito.when(logBrokerProperties.getDataCampTopic()).thenReturn(TOPIC_PATH);
    }

    @ParameterizedTest
    @EnumSource(LogbrokerCheckType.class)
    public void recent(LogbrokerCheckType type) {
        Mockito.when(logbrokerOffsetDao.getLastUpdate(TOPIC)).thenReturn(new LogbrokerLastUpdate(
                LocalDateTime.now(),
                TOPIC_NAME
        ));

        Assertions.assertEquals(
                HealthUtil.OK_ANSWER,
                logbrokerChecker.check(type)
        );
    }

    @ParameterizedTest
    @EnumSource(LogbrokerCheckType.class)
    public void absent(LogbrokerCheckType type) {
        Mockito.when(logbrokerOffsetDao.getLastUpdate(TOPIC)).thenReturn(new LogbrokerLastUpdate(
                null,
                null
        ));

        Assertions.assertEquals(
                HealthUtil.OK_ANSWER,
                logbrokerChecker.check(type)
        );
    }

    @ParameterizedTest
    @EnumSource(LogbrokerCheckType.class)
    public void outdated(LogbrokerCheckType type) {
        LocalDateTime lastUpdate = LocalDateTime.now().minusHours(1);
        Mockito.when(logbrokerOffsetDao.getLastUpdate(TOPIC)).thenReturn(new LogbrokerLastUpdate(
                lastUpdate,
                TOPIC_NAME
        ));

        Assertions.assertEquals(
                HealthUtil.errorToAnswer(String.format(
                        LogbrokerCheckerImpl.LAST_SUCCESSFUL_READING_FROM_LOGBROKER_WAS_TOO_LONG_AGO_MESSAGE,
                        TOPIC_NAME,
                        lastUpdate,
                        LogbrokerCheckerImpl.MINUTES_READING_FROM_LOGBROKER_COULD_BE_UNSUCCESSFUL
                )),
                logbrokerChecker.check(type)
        );
    }
}
