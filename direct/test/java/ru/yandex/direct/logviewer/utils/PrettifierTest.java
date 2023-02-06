package ru.yandex.direct.logviewer.utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class PrettifierTest {

    public static List<Object[]> parametersForDataToStringTest() {
        return asList(new Object[][]{
                {"Timestamp до полудня", "datetime",
                        new Timestamp(LocalDateTime.of(2021, Month.JUNE, 8, 0, 0).atZone(ZoneOffset.systemDefault()).toEpochSecond() * 1000),
                        "2021-06-08 00:00:00"},
                {"Timestamp после полудня", "datetime",
                        new Timestamp(LocalDateTime.of(2021, Month.JUNE, 8, 13, 0).atZone(ZoneOffset.systemDefault()).toEpochSecond() * 1000),
                        "2021-06-08 13:00:00"}
        });
    }

    @Test
    @Parameters
    @TestCaseName("{0}")
    public void dataToStringTest(String ignore, String column, Object input, String expected) {
        String actual = Prettifier.dataToString(column, input);
        assertThat(actual).isEqualTo(expected);
    }
}
