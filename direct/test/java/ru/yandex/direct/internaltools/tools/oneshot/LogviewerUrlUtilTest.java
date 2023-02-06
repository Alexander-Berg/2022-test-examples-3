package ru.yandex.direct.internaltools.tools.oneshot;

import java.time.LocalDateTime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LogviewerUrlUtilTest {

    @Test
    public void formatToLogviewer() {
        String result = LogviewerUrlUtil.formatToLogviewer(LocalDateTime.of(2019, 7, 10, 16, 25, 34));
        String expected = "20190710T162534";
        assertEquals(expected, result);
    }
}
