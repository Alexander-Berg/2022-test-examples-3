package ru.yandex.market.olap2.dao;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LoggingJdbcTemplateTest {
    @Test
    public void testPrependStack() {
        assertThat(stackElement1(),
                is("testPrependStack → stackElement1 → stackElement2 → stackElement3 → stackElement4\n" +
                "zzz"));
    }

    @Test
    public void testPrependShortStack() {
        assertThat(stackShortElement(),
                is("testPrependShortStack → stackShortElement\nzzz"));
    }

    @Test
    public void testPrependSingleStack() {
        assertThat(LoggingJdbcTemplate.prependStack("zzz"),
                is("testPrependSingleStack\nzzz"));
    }

    @Test
    public void testPrependStackOfThree() {
        assertThat(stackElementOfThree1(),
                is("testPrependStackOfThree → stackElementOfThree1 → stackElementOfThree2\nzzz"));
    }

    @Test
    public void testPrependStackWithInvoke() {
        assertThat(invoke(),
                is("testPrependStackWithInvoke\nzzz"));
    }

    private String testPrependStackWithInvoke(boolean returnString) {
        return LoggingJdbcTemplate.prependStack("zzz");
    }

    private String invoke() {
        return testPrependStackWithInvoke(true);
    }

    private String stackElementOfThree1() {
        return stackElementOfThree2();
    }

    private String stackElementOfThree2() {
        return LoggingJdbcTemplate.prependStack("zzz");
    }

    private String stackElement1() {
        return stackElement2();
    }

    private String stackElement2() {
        return stackElement3();
    }

    private String stackElement3() {
        return stackElement4();
    }

    private String stackElement4() {
        return LoggingJdbcTemplate.prependStack("zzz");
    }

    private String stackShortElement() {
        return LoggingJdbcTemplate.prependStack("zzz");
    }
}
