package ru.yandex.market.deliveryintegrationtests.delivery.tests.dropoff;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import dto.CargoType;
import dto.responses.lms.CargoTypeDto;
import dto.responses.lms.LogisticSegmentServiceDto;
import dto.responses.nesu.LogisticPointDisabledReason;
import dto.responses.nesu.ShipmentLogisticPoint;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;
import toolkit.Delayer;

import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter;

@Slf4j
@Epic("Dropoff")
@Resource.Classpath("delivery.properties")
@DisplayName("Dropoff Cargo Types Test")
public class DropoffCargoTypesTest extends AbstractDropoffTest {

    private static final Set<Integer> DENIED_CARGO_TYPES = Stream.of(
            CargoType.BULKY_CARGO_20_KG,
            CargoType.POWDERS,
            CargoType.CHEMICALS
        )
        .map(CargoType::getCargoTypeCode)
        .collect(Collectors.toSet());
    private static final Set<Long> DENIED_CARGO_TYPE_IDS = Set.of(127L, 40L, 41L);

    @BeforeEach
    public void setUp() {
        clearLmsCargoTypes();
    }

    @AfterAll
    public static void tearDown() {
        clearLmsCargoTypes();
        syncCargoTypes();
    }

    @Test
    @TmsLink("logistic-98")
    @DisplayName("Dropoff: точка недоступна для подключения")
    public void pointUnavailable() {
        ShipmentLogisticPoint foundedOption = passCargoTypesThroughYtAndGetAvailableOption(DENIED_CARGO_TYPES);

        LogisticPointDisabledReason disabledReason = foundedOption.getDisabledReasons().stream()
            .filter(reason -> Objects.equals(reason.getType(), "UNSUPPORTED_CARGO_TYPES"))
            .findAny()
            .orElseThrow(
                () -> new AssertionError("Точка доступна для подключения, несмотря на недопустимые карго-типы")
            );

        Assertions.assertEquals(
            Set.of(301),
            disabledReason.getUnsupportedCargoTypes(),
            "Неверный список недопустимых карго-типов в причинах недоступности"
        );
    }

    @Test
    @TmsLink("logistic-107")
    @DisplayName("Dropoff: точка доступна для подключения")
    public void pointAvailable() {
        ShipmentLogisticPoint foundedOption = passCargoTypesThroughYtAndGetAvailableOption(Set.of(470, 480));
        Assertions.assertTrue(foundedOption.getDisabledReasons().isEmpty(), "Точка недоступна для подключения");
    }

    private ShipmentLogisticPoint passCargoTypesThroughYtAndGetAvailableOption(Set<Integer> cargoTypes) {
        cargoTypes.forEach(ct -> LMS_STEPS.denyCargoTypeForService(DROPOFF_LOGISTIC_SERVICE_ID, ct));
        Assertions.assertEquals(getServiceCargoTypes(), cargoTypes, "Карго-типы не добавились в сервис");

        syncCargoTypes();

        return NESU_STEPS.waitAvailableShipmentOption(
            SHOP_PARTNER_ID,
            SHOP_ID,
            USER_ID,
            DROPOFF_LOGISTIC_POINT_ID,
            (dropoff) -> Assertions.assertEquals(
                cargoTypes,
                dropoff.getForbiddenCargoTypes(),
                "Карго-типы не совпадают с ожидаемыми"
            )
        );
    }

    private static void clearLmsCargoTypes() {
        LMS_STEPS.allowCargoTypesForService(DROPOFF_LOGISTIC_SERVICE_ID, DENIED_CARGO_TYPE_IDS);
        Assertions.assertTrue(getServiceCargoTypes().isEmpty(), "У сервиса имеются непредусмотренные карго-типы");
    }

    private static void syncCargoTypes() {
        LMS_STEPS.updateCargoTypesFromLmsToYt();

        // ждем, пока lms-ная табличка реплицируется в yt
        Delayer.delay(10, TimeUnit.SECONDS);

        NESU_STEPS.runYtUpdateDropoffSegmentsInfoJob();
    }

    @Nonnull
    private static Set<Integer> getServiceCargoTypes() {
        LogisticSegmentServiceDto service = LMS_STEPS.searchLogisticSegments(
                LogisticSegmentFilter.builder()
                    .setIds(Set.of(DROPOFF_LOGISTIC_SEGMENT_ID))
                    .build()
            ).stream()
            .findAny()
            .orElseThrow(() -> new AssertionError("Не найден сегмент" + DROPOFF_LOGISTIC_SEGMENT_ID))
            .getServices().stream()
            .filter(s -> s.getId() == DROPOFF_LOGISTIC_SERVICE_ID)
            .findAny()
            .orElseThrow(() -> new AssertionError("Не найден сервис" + DROPOFF_LOGISTIC_SERVICE_ID));
        Assertions.assertNotNull(service.getRestrictedCargoTypes(), "Карго-типы сервиса не найдены");
        return service.getRestrictedCargoTypes().stream()
            .map(CargoTypeDto::getCargoType)
            .collect(Collectors.toSet());
    }
}
