package ru.yandex.autotests.direct.web.api.tests.mediareach;

import java.time.LocalDate;

import ru.yandex.autotests.direct.web.api.models.CampaignStrategy;
import ru.yandex.autotests.direct.web.api.models.CpmForecastRequest;
import ru.yandex.autotests.direct.web.api.models.ImpressionLimit;

public class CampaignForecastTestData {

    public static CpmForecastRequest createRequest(Integer newCampaignExampleType, CampaignStrategy strategy) {
        return new CpmForecastRequest()
                .withNewCampaignExampleType(newCampaignExampleType)
                .withStrategy(strategy);
    }

    public static CpmForecastRequest createRequest(Long campaignId, CampaignStrategy strategy) {
        return new CpmForecastRequest()
                .withCampaignId(campaignId)
                .withStrategy(strategy);
    }

    public static CampaignStrategy defaultStrategy() {
        return new CampaignStrategy()
                .withImpressionLimit(defaultImpressionLimit())
                .withCpm(1000.0)
                .withEndDate(LocalDate.now().plusDays(6))
                .withStartDate(LocalDate.now())
                .withBudget(500000.0)
                .withType("MAX_REACH");
    }

    public static ImpressionLimit defaultImpressionLimit() {
        return new ImpressionLimit()
                .withImpressions(30L)
                .withDays(5L);
    }
}
