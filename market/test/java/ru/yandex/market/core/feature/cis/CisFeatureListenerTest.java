package ru.yandex.market.core.feature.cis;

import java.time.Clock;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.Mockito.when;

/**
 * Функциональный тест для {@link CisFeatureListener}.
 */
@DbUnitDataSet(before = "CisFeatureListenerTest.before.csv")
public class CisFeatureListenerTest extends FunctionalTest {

    @Autowired
    FeatureService featureService;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }


    private static Stream<Arguments> getArgs() {
        return Stream.of(
                Arguments.of("Успешный перевод статуса для DBS", 1L, ParamCheckStatus.DONT_WANT, ParamCheckStatus.SUCCESS),
                Arguments.of("Неуспешный перевод: фича в статусе FAIL", 2L, ParamCheckStatus.FAIL, ParamCheckStatus.NEW),
                Arguments.of("Неуспешный перевод: не DBS", 3L, ParamCheckStatus.DONT_WANT, ParamCheckStatus.NEW)
        );
    }

    @ParameterizedTest
    @MethodSource("getArgs")
    void testListener(String testName, long shopId, ParamCheckStatus originalStatus, ParamCheckStatus targetStatus) {
        final ShopFeature original = featureService.getFeature(shopId, FeatureType.CIS);
        Preconditions.condition(original.getStatus() == originalStatus, "Original status must be " + originalStatus +
                ", but it's " + original.getStatus());

        featureService.changeStatus(shopId, ShopFeature.of(shopId, FeatureType.CIS, ParamCheckStatus.NEW));

        final ShopFeature current = featureService.getFeature(shopId, FeatureType.CIS);
        Assertions.assertEquals(targetStatus, current.getStatus());
    }

    @Test
    @DisplayName("Проверка дефолтного статуса")
    void testDefault() {
        //для FBY по умолчанию SUCCESS
        ShopFeature defaultFeature = featureService.getFeature(4L, FeatureType.CIS);
        Assertions.assertEquals(ParamCheckStatus.SUCCESS, defaultFeature.getStatus());
        //при включении программы DSBS статус выставляется в DONT_WANT
        defaultFeature = featureService.getFeature(5L, FeatureType.CIS);
        Preconditions.condition(defaultFeature.getStatus() == ParamCheckStatus.SUCCESS, "Default status for feature is SUCCESS");
        featureService.changeStatus(1L, new ShopFeature(100L, 5L, FeatureType.DROPSHIP_BY_SELLER, ParamCheckStatus.SUCCESS));
        Assertions.assertEquals(ParamCheckStatus.DONT_WANT, featureService.getFeature(5L, FeatureType.CIS).getStatus());
    }

    @Test
    @DisplayName("Проверка перевода фичи из DONT_WANT в DONT_WANT")
    void testSameStatus() {
        Preconditions.condition(featureService.getFeature(6L, FeatureType.CIS).getStatus() == ParamCheckStatus.DONT_WANT, "Feature must be in DONT_WANT status");
        featureService.changeStatus(1L, new ShopFeature(100L, 6L, FeatureType.DROPSHIP_BY_SELLER, ParamCheckStatus.SUCCESS));
        Assertions.assertEquals(ParamCheckStatus.DONT_WANT, featureService.getFeature(6L, FeatureType.CIS).getStatus());
    }
}
