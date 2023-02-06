package ru.yandex.direct.web.testing.data;

import java.time.LocalDate;

import ru.yandex.direct.web.core.entity.inventori.model.CampaignStrategy;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastRequest;
import ru.yandex.direct.web.core.entity.inventori.model.ImpressionLimit;


public class TestCpmForecastRequest {

    private TestCpmForecastRequest() {
    }

    public static CpmForecastRequest defaultRequest(Long campaignId) {
        return defaultRequest(campaignId, defaultStrategy());
    }

    public static CpmForecastRequest defaultRequest(Long campaignId, CampaignStrategy strategy) {
        return new CpmForecastRequest(campaignId, null, strategy);
    }

    public static CampaignStrategy defaultStrategy() {
        return new CampaignStrategy("MAX_REACH",
                100.0,
                LocalDate.now(),
                LocalDate.now().plusMonths(1L),
                defaultImpressionLimit(),
                50.0,
                1L);
    }

    public static ImpressionLimit defaultImpressionLimit() {
        return new ImpressionLimit(100L, 5L);
    }
}
