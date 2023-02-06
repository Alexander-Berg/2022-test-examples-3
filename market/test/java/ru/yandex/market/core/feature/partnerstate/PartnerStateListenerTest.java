package ru.yandex.market.core.feature.partnerstate;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;

/**
 * Функциональный тест для {@link PartnerStateListener}.
 */
@DbUnitDataSet(before = "PartnerStateListenerTest.before.csv")
public class PartnerStateListenerTest extends FunctionalTest {

    @Autowired
    FeatureService featureService;

    private static Stream<Arguments> getArgs() {
        return Stream.of(
                Arguments.of("Успешный перевод статуса", 1L, FeatureType.SELLS_MEDICINE,
                        ParamCheckStatus.DONT_WANT, ParamCheckStatus.NEW),
                Arguments.of("Неуспешный перевод: фича в статусе FAIL", 2L, FeatureType.SELLS_MEDICINE,
                        ParamCheckStatus.FAIL, ParamCheckStatus.NEW),
                Arguments.of("Неуспешный перевод", 3L, FeatureType.SELLS_MEDICINE,
                        ParamCheckStatus.DONT_WANT, ParamCheckStatus.NEW),
                Arguments.of("Успешный перевод статуса", 1L, FeatureType.MEDICINE_COURIER,
                        ParamCheckStatus.DONT_WANT, ParamCheckStatus.NEW)
        );
    }

    @ParameterizedTest
    @MethodSource("getArgs")
    void testListener(String testName, long shopId, FeatureType featureType, ParamCheckStatus originalStatus,
                      ParamCheckStatus targetStatus) {
        final ShopFeature original = featureService.getFeature(shopId, featureType);
        Preconditions.condition(original.getStatus() == originalStatus, "Original status must be " + originalStatus +
                ", but it's " + original.getStatus());

        featureService.changeStatus(shopId, ShopFeature.of(shopId, featureType, ParamCheckStatus.NEW));

        final ShopFeature current = featureService.getFeature(shopId, featureType);
        Assertions.assertEquals(targetStatus, current.getStatus());
    }

    @Test
    @DisplayName("Проверка дефолтного статуса")
    void testDefault() {
        ShopFeature defaultFeature = featureService.getFeature(4L, FeatureType.SELLS_MEDICINE);
        Assertions.assertEquals(ParamCheckStatus.DONT_WANT, defaultFeature.getStatus());
        defaultFeature = featureService.getFeature(5L, FeatureType.SELLS_MEDICINE);
        Assertions.assertEquals(ParamCheckStatus.DONT_WANT, defaultFeature.getStatus());
    }
}
