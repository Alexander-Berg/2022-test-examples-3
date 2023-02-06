package ru.yandex.market.stat.dicts.services;


import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;

import java.time.LocalDate;

import static ru.yandex.market.stat.dicts.config.TestConstants.TEST_DICT_NAME;

public class StepEventSenderTest {


    private static final String PATH = "//home/market/testing/mstat/dictionaries/" + TEST_DICT_NAME;
    private static final String STEP_REAL_URL = "http://step.sandbox.yandex-team.ru/api/v1/events";

    @Test
    @Ignore("Turn off just to disable step ddos")
    public void rebuildEventSendable() {
        StepEventsSender sender = new StepEventsSender(STEP_REAL_URL, "testing", null);

        sender.sendEvent(TEST_DICT_NAME, LocalDate.now().atStartOfDay(), "hahn", YPath.simple(PATH), LoaderScale.DEFAULT,true);
    }

    @Test
    @Ignore("Works with step-token only")
    public void publishEventSendableDefault() {
        StepEventsSender sender = new StepEventsSender("http://step.sandbox.yandex-team.ru/api/v1/events",
            "testing", "get_token_in_support_page");

        sender.sendEvent(TEST_DICT_NAME, LocalDate.now().atStartOfDay(), "hahn",
            YPath.simple("//home/market/testing/mstat/dictionaries/" + TEST_DICT_NAME), LoaderScale.DEFAULT, false);
    }

    @Test
    @Ignore("Works with step-token only")
    public void publishEventSendableHourly() {
        StepEventsSender sender = new StepEventsSender("http://step.sandbox.yandex-team.ru/api/v1/events",
            "testing", "get_token_in_support_page");

        sender.sendEvent(TEST_DICT_NAME, LocalDate.now().atStartOfDay().withHour(10), "hahn",
            YPath.simple("//home/market/testing/mstat/dictionaries/" + TEST_DICT_NAME), LoaderScale.HOURLY, false);
    }
}
