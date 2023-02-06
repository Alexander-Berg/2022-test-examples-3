package ru.yandex.market.logistics.mybatis.handler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.postgresql.util.PGInterval;

import ru.yandex.market.logistics.mybatis.AbstractTest;

public class DurationTypeHandlerTest extends AbstractTest {

    private final DurationTypeHandler durationTypeHandler = new DurationTypeHandler();

    private ResultSet resultSetMock = Mockito.mock(ResultSet.class);

    @Test
    void testNull() throws SQLException {
        Duration duration = durationTypeHandler.getNullableResult(resultSetMock, "duration");
        softly.assertThat(duration).isNull();
    }

    @Test
    void testNotNull() throws SQLException {
        Duration parameter = Duration.ZERO.plusDays(1).plusHours(22).plusMinutes(12).plusSeconds(2);

        PGInterval intervalObject = new PGInterval();
        intervalObject.setSeconds(2);
        intervalObject.setMinutes(12);
        intervalObject.setHours(22);
        intervalObject.setDays(1);

        Mockito.when(resultSetMock.getObject("duration")).thenReturn(intervalObject);

        Duration duration = durationTypeHandler.getNullableResult(resultSetMock, "duration");
        softly.assertThat(duration.getSeconds()).isEqualTo(parameter.getSeconds());
    }

}
