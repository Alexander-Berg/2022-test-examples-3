package ru.yandex.market.papi.requests.limits;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.logprocessor.client.MbiLogProcessorClient;
import ru.yandex.market.mbi.logprocessor.client.model.HitRateLimitResourcesResponse;
import ru.yandex.market.mbi.logprocessor.client.model.HitRateParallelResource;
import ru.yandex.market.mbi.logprocessor.client.model.HitRatePartnerResource;
import ru.yandex.market.mbi.logprocessor.client.model.HitRateResource;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

@DbUnitDataSet(before = "ApiLog.before.csv")
public class PapiHitRateLimitsNotificationSenderExecutorTest extends FunctionalTest {

    @Autowired
    private PapiHitRateLimitsNotificationSenderExecutor executor;

    @Autowired
    private MbiLogProcessorClient logProcessorClient;

    @Test
    @SuppressWarnings("unchecked")
    void testNotifyExecutor() {
        when(logProcessorClient.getHitRateLimitResourcesStat()).thenReturn(getFilledResponse());
        executor.doJob(null);

        verifySentNotificationType(partnerNotificationClient, 2, PapiHitRateLimitsNotificationService.TEMPLATE_ID);
    }

    private static HitRateLimitResourcesResponse getFilledResponse() {
        return getHitRateLimitResourcesResponse(List.of(
                new HitRatePartnerResource()
                        .campaignId(10774L)
                        .addResourcesWithParallelRequestLimitItem(new HitRateParallelResource().papiResource("p"))
                        .addResourcesWithRequestLimitItem(new HitRateResource()
                                .papiResource("pr")
                                .count(3L)),
                new HitRatePartnerResource()
                        .campaignId(4444L)
                        .addResourcesWithParallelRequestLimitItem(new HitRateParallelResource().papiResource("p1"))
                        .addResourcesWithRequestLimitItem(new HitRateResource()
                                .papiResource("pr2")
                                .count(3131L))
        ));
    }

    private static HitRateLimitResourcesResponse getHitRateLimitResourcesResponse(List<HitRatePartnerResource> responseList) {
        HitRateLimitResourcesResponse result = new HitRateLimitResourcesResponse();
        result.setResourcesWithHitRate(responseList);
        return result;
    }

    @Test
    void testNoNotificationSent() {
        when(logProcessorClient.getHitRateLimitResourcesStat()).thenReturn(getEmptyResponse());
        executor.doJob(null);

        verifyNoInteractions(partnerNotificationClient);
    }

    private static HitRateLimitResourcesResponse getEmptyResponse() {
        return getHitRateLimitResourcesResponse(List.of());
    }
}
