package ru.yandex.market.partner.mvc.controller.feature.converter;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffMessage;
import ru.yandex.market.core.feature.model.FeatureCutoffReason;
import ru.yandex.market.core.feature.model.ShopFeatureInfo;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.partner.mvc.controller.feature.model.FeatureCutoffInfoDto;
import ru.yandex.market.partner.mvc.controller.feature.model.ShopFeatureInfoDto;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.core.feature.model.FeatureCutoffReason.PINGER_API;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.MANAGER;
import static ru.yandex.market.core.feature.model.FeatureType.FULFILLMENT;

public class ShopFeatureInfoDtoConverterTest {

    private final ShopFeatureInfoDtoConverter tested = new ShopFeatureInfoDtoConverter();

    /**
     * Тест для {@link ShopFeatureInfoDtoConverter#map(ShopFeatureInfo)}.
     */
    @Test
    public void testMapAllFields() {
        ShopFeatureInfo model = new ShopFeatureInfo(1l, FULFILLMENT, ParamCheckStatus.FAIL,
                createTestMessage(), false, asList("precondition1", "precondition2"),
                asList(createTestCutoff(MANAGER, PINGER_API), createTestCutoff(MANAGER, null)));

        ShopFeatureInfoDto actual = tested.map(model);

        assertNotNull(actual);
        assertEquals(model.getShopId(), actual.getShopId());
        assertEquals(model.getFeatureId(), actual.getFeatureId());
        assertEquals("FULFILLMENT", actual.getFeatureName());
        assertEquals(model.getStatus(), actual.getStatus());
        assertNotNull(actual.getRecentMessage());
        assertEquals(model.getRecentMessage().getShopId(), actual.getRecentMessage().getShopId());
        assertEquals(model.getRecentMessage().getFeatureType(), actual.getRecentMessage().getFeatureType());
        assertEquals(model.getRecentMessage().getSubject(), actual.getRecentMessage().getSubject());
        assertEquals(model.getRecentMessage().getBody(), actual.getRecentMessage().getBody());
        assertNotNull(actual.getFailedPreconditions());
        assertEquals(2, actual.getFailedPreconditions().size());
        assertThat(actual.getFailedPreconditions(), contains("precondition1", "precondition2"));
        assertEquals(model.getCanEnable(), actual.getCanEnable());
        assertNotNull(actual.getCutoffs());
        assertEquals(2, actual.getCutoffs().size());
        assertThat(actual.getCutoffs(), containsInAnyOrder(
                new FeatureCutoffInfoDto("MANAGER", PINGER_API),
                new FeatureCutoffInfoDto("MANAGER", null)));
    }

    private FeatureCutoffMessage createTestMessage() {
        return new FeatureCutoffMessage(1l, FULFILLMENT, "subject", "body", List.of());
    }

    private FeatureCutoffInfo createTestCutoff(FeatureCustomCutoffType cutoffType, FeatureCutoffReason cutoffReason) {
        return new FeatureCutoffInfo.Builder()
                .setFeatureType(FULFILLMENT)
                .setDatasourceId(1l)
                .setStartDate(DateUtil.now())
                .setFeatureCutoffType(cutoffType)
                .setReason(cutoffReason)
                .build();
    }
}
