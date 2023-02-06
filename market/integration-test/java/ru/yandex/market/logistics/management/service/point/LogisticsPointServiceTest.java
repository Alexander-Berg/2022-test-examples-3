package ru.yandex.market.logistics.management.service.point;

import java.math.BigDecimal;
import java.util.Map;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.Address;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointCreateRequest;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.repository.LogisticsPointRepository;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.service.client.LogisticsPointService;

@DisplayName("Тесты сервиса LogisticsPointService")
@DatabaseSetup("/data/service/export/dynamic/db/before/prepare.xml")
class LogisticsPointServiceTest extends AbstractContextualTest {

    @Autowired
    LogisticsPointService logisticsPointService;

    @Autowired
    LogisticsPointRepository logisticsPointRepository;

    @Autowired
    PartnerRepository partnerRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Создание ПВЗ")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/yt_outlet_after_insertion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void pickupPointInsertion() {
        //do nothing
    }

    @Test
    @DisplayName("Создание нового склада")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/yt_outlet_after_insertion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createWarehouse() {
        logisticsPointService.create(point(PointType.WAREHOUSE, null), address());
        checkBuildWarehouseSegmentTask(7L);
    }

    @Test
    @DisplayName("Создание нового ПВЗ")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/yt_outlet_after_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createPickupPoint() {
        logisticsPointService.create(point(PointType.PICKUP_POINT, null), address());
    }

    @Test
    @DisplayName("Создание нового ПВЗ с указанием partnerId")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/yt_outlet_after_creation_with_partner_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createPickupPointWithPartnerId() {
        logisticsPointService.create(point(PointType.PICKUP_POINT, 2L), address());
    }

    @Test
    @DisplayName("Проставление пермалинка логистической точке")
    @DatabaseSetup(
        value = "/data/service/export/dynamic/db/before/prepare_update_permalink.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/after_permalink_was_set.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setLogisticPointPermalink() {
        logisticsPointService.updatePermalinks(Map.of(
            1L, 12345L,
            2L, 54321L,
            3L, 99999L
        ));
    }

    @Test
    @DisplayName("Активация ПВЗ")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/after_activation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activatePickupPoint() {
        logisticsPointService.activate(ImmutableSet.of(3L));
    }

    @Test
    @DisplayName("Активация точки должна деактивировать конфликтующие")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/after_activation_conflicted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activePointShouldDeactivateConflicted() {
        logisticsPointService.activate(ImmutableSet.of(6L));
        checkBuildWarehouseSegmentTask(4L, 6L);
    }

    @Test
    @DisplayName("Деактивация ПВЗ")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/after_deactivation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deactivatePickupPoint() {
        logisticsPointService.deactivate(ImmutableSet.of(1L));
    }

    @Test
    @DisplayName("Обновление ПВЗ")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updatePickupPoint() {
        Address address = updatedAddress();

        logisticsPointService.update(
            2L,
            logisticsPoint -> logisticsPoint
                .setAddress(address)
                .setActive(false)
                .setFrozen(true)
                .setType(ru.yandex.market.logistics.management.domain.entity.type.PointType.PICKUP_POINT),
            address
        );
    }

    @Test
    @DisplayName("Обновление склада")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/after_update_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateWarehouse() {
        Address address = updatedAddress();

        logisticsPointService.update(
            4L,
            logisticsPoint -> logisticsPoint
                .setAddress(address)
                .setActive(false)
                .setFrozen(true)
                .setType(ru.yandex.market.logistics.management.domain.entity.type.PointType.WAREHOUSE),
            address
        );
        checkBuildWarehouseSegmentTask(4L);
    }

    @Test
    @DisplayName("Обновление склада и координат в адресе")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/after_update_warehouse_with_coordinates.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateWarehouseWithCoordinates() {
        Address address = updatedAddress().setLatitude(BigDecimal.valueOf(10)).setLongitude(BigDecimal.valueOf(20));

        logisticsPointService.update(
            4L,
            logisticsPoint -> logisticsPoint
                .setAddress(address)
                .setType(ru.yandex.market.logistics.management.domain.entity.type.PointType.WAREHOUSE),
            address
        );
        checkBuildWarehouseSegmentTask(4L);
    }

    @Test
    @DisplayName("Обновление склада - изменился locationId")
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/after_update_warehouse_location_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateWarehouseLocationId() {
        Address address = updatedAddress().setLocationId(10);

        logisticsPointService.update(
            4L,
            logisticsPoint -> logisticsPoint
                .setAddress(address)
                .setActive(false)
                .setFrozen(true)
                .setType(ru.yandex.market.logistics.management.domain.entity.type.PointType.WAREHOUSE),
            address
        );
        checkBuildWarehouseSegmentTask(4L, 7L);
    }

    @Test
    @DisplayName("Обновление склада - склад не экспресс")
    @DatabaseSetup(value = "/data/service/export/dynamic/db/before/prepare_5.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/data/service/export/dynamic/db/after/after_update_warehouse_not_express.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateWarehouseNoLinkWithZones() {
        Address address = updatedAddress();

        logisticsPointService.update(
            5L,
            logisticsPoint -> logisticsPoint
                .setAddress(address)
                .setName("CHANGED_DELETED_POINT")
                .setType(ru.yandex.market.logistics.management.domain.entity.type.PointType.WAREHOUSE),
            address
        );
        checkBuildWarehouseSegmentTask(5L, 7L);
    }

    @Test
    @DisplayName("Проверка является ли лог. точка дропоффом")
    @DatabaseSetup(
        value = "/data/service/dropoff/logistic_point_is_dropoff.xml",
        type = DatabaseOperation.INSERT
    )
    void checkIfLogisticPointIsDropoff() {
        LogisticsPoint dropoff = logisticsPointService.findByIdOrThrow(1L);
        LogisticsPoint notDropoffMultiWarehouseSegments = logisticsPointService.findByIdOrThrow(2L);

        softly.assertThat(logisticsPointService.logisticsPointIsActiveDropoff(dropoff)).isTrue();
        softly.assertThat(logisticsPointService.logisticsPointIsActiveDropoff(notDropoffMultiWarehouseSegments))
            .isFalse();
    }

    @Test
    @DisplayName("Проверка является ли лог. точка дропоффом")
    @DatabaseSetup(
        value = "/data/service/dropoff/logistic_point_is_inactive_dropoff.xml",
        type = DatabaseOperation.INSERT
    )
    void checkIfLogisticPointIsInactiveDropoff() {
        LogisticsPoint activeDropoff = logisticsPointService.findByIdOrThrow(1L);
        LogisticsPoint inactiveDropoff = logisticsPointService.findByIdOrThrow(2L);
        LogisticsPoint notDropoff = logisticsPointService.findByIdOrThrow(3L);

        softly.assertThat(logisticsPointService.logisticsPointIsDropoff(activeDropoff)).isTrue();
        softly.assertThat(logisticsPointService.logisticsPointIsDropoff(inactiveDropoff)).isTrue();
        softly.assertThat(logisticsPointService.logisticsPointIsDropoff(notDropoff)).isFalse();
    }

    @Nonnull
    private Address updatedAddress() {
        return new Address()
            .setLocationId(12345)
            .setSettlement("Ульяновск")
            .setPostCode("555666")
            .setStreet("Московское шоссе")
            .setEstate("newEstate")
            .setKm(1)
            .setHouse("11Б")
            .setHousing("3")
            .setBuilding("2")
            .setApartment("1")
            .setAddressString("Строка адреса 1")
            .setShortAddressString("Строка адреса 1")
            .setComment("comment1");
    }

    @Nonnull
    private LogisticsPointCreateRequest point(PointType type, Long partnerId) {
        return LogisticsPointCreateRequest.newBuilder()
            .partnerId(partnerId)
            .externalId("123-456")
            .address(getAddressDto())
            .type(type)
            .build();
    }

    @Nonnull
    private Address address() {
        return new Address();
    }

    @Nonnull
    private ru.yandex.market.logistics.management.entity.response.core.Address getAddressDto() {
        return ru.yandex.market.logistics.management.entity.response.core.Address.newBuilder()
            .locationId(12345)
            .settlement("Москва")
            .postCode("555666")
            .latitude(new BigDecimal("100"))
            .longitude(new BigDecimal("200"))
            .street("Октябрьская")
            .house("5")
            .housing("3")
            .building("2")
            .apartment("1")
            .comment("comment")
            .region("region")
            .subRegion("subRegion")
            .addressString("Строка адреса")
            .shortAddressString("Строка адреса")
            .build();
    }
}
