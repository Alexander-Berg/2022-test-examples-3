package ru.yandex.calendar;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.val;
import org.joda.time.Duration;
import org.joda.time.Instant;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.misc.cache.tl.TlCache;
import ru.yandex.misc.env.Environment;
import ru.yandex.misc.support.tl.ThreadLocalHandle;
import ru.yandex.misc.test.TestUtils;

public class Hooks {
    private static Duration testTimeout() {
        return Environment.isDeveloperNotebook()
            ? Duration.standardHours(1)
            : Duration.standardMinutes(1);
    }

    @Before
    public void beforeScenario(Scenario scenario) {
        TlCache.reset();
        TlCache.push();
        TestUtils.tltBeforeTest(testTimeout());
    }

    @After
    public void afterScenario(Scenario scenario) {
        TlCache.reset();
        TestUtils.tltAfterTest();
    }

    @Before
    public void setupRequestContext(Scenario scenario) {
        val info = new RemoteInfo(Option.empty(), Option.empty());
        CalendarRequest.push(info, ActionSource.WEB, "test", "test", Instant.now(), true);
    }

    @After
    public void teardown(Scenario scenario) {
        CalendarRequest.getCurrentO()
            .ifPresent(ThreadLocalHandle::popSafely);
    }
}
