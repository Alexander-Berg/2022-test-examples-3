package ru.yandex.market.delivery.mdbapp.components.logging.json;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import ru.yandex.market.delivery.mdbapp.components.logging.AbstractLogger;
import ru.yandex.market.logistics.lom.model.dto.AuthorDto;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;

import static org.mockito.Mockito.spy;

public class LomEventsSuccessLoggerTest {
    private static final Instant CREATED = LocalDate.of(2020, 10, 14)
        .atStartOfDay()
        .toInstant(ZoneOffset.ofHours(3));
    private static final Instant ENTITY_CREATED = LocalDate.of(2020, 10, 13)
        .atStartOfDay()
        .toInstant(ZoneOffset.ofHours(3));
    public static final long ENTITY_ID = 1L;
    public static final long ID = 1L;
    public static final long LOGBROKER_ID = 1L;
    public static final EntityType ENTITY_TYPE = EntityType.ORDER;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private LomEventsSuccessLogger logger;
    private Logger dataLoggerMock;

    @Before
    public void setUp() throws Exception {
        logger = spy(new LomEventsSuccessLogger());

        dataLoggerMock = Mockito.mock(Logger.class);

        Field dataLogger = AbstractLogger.class.getDeclaredField("dataLogger");
        dataLogger.setAccessible(true);
        dataLogger.set(logger, dataLoggerMock);
    }

    @Test
    public void testLogging() {
        logger.logEvent(new EventDto()
            .setAuthor(AuthorDto.builder().abcServiceId(1L).yandexUid(BigDecimal.ONE).build())
            .setCreated(CREATED)
            .setDiff(BooleanNode.getFalse())
            .setEntityCreated(ENTITY_CREATED)
            .setEntityId(ENTITY_ID)
            .setEntityType(ENTITY_TYPE)
            .setId(ID)
            .setLogbrokerId(LOGBROKER_ID)
            .setSnapshot(BooleanNode.getTrue())
        );

        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher()));
        Mockito.verifyNoMoreInteractions(dataLoggerMock);
    }

    @RequiredArgsConstructor
    @Slf4j
    @Ignore
    static class ArgumentMatcher implements org.mockito.ArgumentMatcher<String> {
        private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        private final SimpleDateFormat sdf;

        ArgumentMatcher() {
            sdf = new SimpleDateFormat(AbstractLogger.LOGGER_DATE_FORMAT);
            sdf.setTimeZone(DateUtils.UTC_TIME_ZONE);
        }

        @Override
        public boolean matches(String argument) {
            try {
                Map<String, Object> data = om.readValue(argument, new TypeReference<>() {
                });
                return Objects.nonNull(data.get("date"))
                    && Objects.equals(data.get("entityType"), ENTITY_TYPE.name())
                    && Objects.equals(data.get("entityId"), (int) ENTITY_ID)
                    && Objects.equals(data.get("created"), sdf.format(Date.from(CREATED)))
                    && Objects.equals(data.get("entityCreated"), sdf.format(Date.from(ENTITY_CREATED)))
                    && Objects.equals(data.get("abcServiceId"), 1)
                    && Objects.equals(data.get("yandexUid"), 1)
                    && Objects.equals(data.get("logbrokerId"), (int) LOGBROKER_ID);
            } catch (JsonProcessingException e) {
                log.error("Invalid json {}: ", argument, e);
                return false;
            }
        }
    }
}
