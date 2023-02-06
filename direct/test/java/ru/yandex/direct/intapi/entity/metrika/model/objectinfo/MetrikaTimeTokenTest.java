package ru.yandex.direct.intapi.entity.metrika.model.objectinfo;

import java.time.LocalDateTime;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MetrikaTimeTokenTest {

    @Test
    public void constructorFromString_FullDateAndId_WorksFine() {
        LocalDateTime dateTime = LocalDateTime.parse("2017-01-22T17:03:40", MetrikaTimeToken.DATE_TIME_FORMATTER);
        MetrikaTimeToken timeToken = new MetrikaTimeToken("2017-01-22T17:03:40/123");
        assertThat(timeToken.getLastChange(), is(dateTime));
        assertThat(timeToken.getLastId(), is(123L));
    }

    @Test(expected = RuntimeException.class)
    public void constructorFromString_FullDateWithSeparatorWithoutId_ThrowsException() {
        new MetrikaTimeToken("2017-01-22T17:03:40/");
    }

    @Test(expected = RuntimeException.class)
    public void constructorFromString_FullDateWithoutSeparatorWithoutId_ThrowsException() {
        new MetrikaTimeToken("2017-01-22T17:03:40");
    }

    @Test
    public void toString_WorksFine() {
        String timeTokenStr = "2017-01-22T17:03:40/123";
        MetrikaTimeToken timeToken = new MetrikaTimeToken(timeTokenStr);
        assertThat(timeToken.toString(), is(timeTokenStr));
    }
}
