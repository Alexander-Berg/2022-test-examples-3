package ru.yandex.market.api.controller.v2;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.DeliveryAvailableResult;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.matchers.NearestRegionMatcher;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

/**
 * Created by fettsery on 18.09.18.
 */
@WithContext
public class GeoControllerV2Test extends BaseTest {

    @Inject
    GeoControllerV2 controller;
    @Inject
    ReportTestClient reportTestClient;

    @Test
    public void testDeliveryAvailability() {
        reportTestClient.isDeliveryAvailable("check-delivery-available.json", 213);
        DeliveryAvailableResult result = controller.isDeliveryAvailable().waitResult();

        Assert.assertTrue(result.getDeliveryAvailable());

        Assert.assertEquals("Москву", result.getRegion().getNameAccusative());
    }

    @Test
    public void shouldNotReturnSameRegionIfDeliveryUnavailable() {
        ContextHolder.get().getRegionInfo().setRawRegionId(10000);
        reportTestClient.isDeliveryAvailable("report_check-delivery-available__negative.json", 10000);

        DeliveryAvailableResult result = controller.isDeliveryAvailable().waitResult();

        Assert.assertFalse(result.getDeliveryAvailable());

        Assert.assertNotNull(result.getDeliveryAvailable());
        Assert.assertThat(result.getNearestRegions(),
                Matchers.not(Matchers.contains(NearestRegionMatcher.regionId(10000))));
    }
}
