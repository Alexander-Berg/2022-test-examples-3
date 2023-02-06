package ru.yandex.direct.jobs.statistics.rollbacknotifications;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class StatRollbackGetPeriodsTest {
    static Object[][] testData() {
        return new Object[][]{
                {List.of("20200130"), "30.01.2020"},
                {List.of("20200130", "20200129", "20200130"), "29.01.2020, 30.01.2020"},
                {List.of("20200128", "20200129"), "28.01.2020, 29.01.2020"},
                {List.of("20200128", "20200129", "20200130"), "28.01.2020-30.01.2020"},
                {List.of("20200126", "20200128", "20200130"), "26.01.2020, 28.01.2020, 30.01.2020"},
                {List.of("20200126", "20200127", "20200130"), "26.01.2020-27.01.2020, 30.01.2020"},
                {List.of("20200127", "20200129", "20200130"), "27.01.2020, 29.01.2020-30.01.2020"},
                {List.of("20200125", "20200126", "20200127", "20200129", "20200130"), "25.01.2020-27.01.2020, 29.01" +
                        ".2020-30.01.2020"},

                {List.of("20200125", "20200126", "20200128", "20200130", "20200131"), "25.01.2020-26.01.2020, 28.01" +
                        ".2020, 30.01.2020-31.01.2020"},

                {List.of("20200125", "20200127", "20200128", "20200129", "20200131"), "25.01.2020, 27.01.2020-29.01" +
                        ".2020, 31.01.2020"},
                {List.of("20200130", "20200131", "20200201", "20200202"), "30.01.2020-02.02.2020"},
                {List.of("20200131", "20200202", "20200130", "20200201"), "30.01.2020-02.02.2020"},
        };
    }

    @ParameterizedTest(name = "result = {1}")
    @MethodSource("testData")
    void do_test(List<String> input, String expectedResult) {
        String actual = StatRollbackEmailSenderJob.getPeriod(input);
        assertThat(actual, is(expectedResult));
    }
}
