package ru.yandex.market.tpl.billing.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.entity.PickupPoint;

import static ru.yandex.market.tpl.billing.model.PickupPointBrandingType.FULL;
import static ru.yandex.market.tpl.billing.model.PickupPointBrandingType.NONE;

public class PickupPointHistoryServiceTest extends AbstractFunctionalTest {

    @Autowired
    PickupPointHistoryService pickupPointHistoryService;

    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointchangeslogservice/before/add_pickup_point_log.csv",
            after = "/database/service/pickuppointchangeslogservice/before/add_pickup_point_log.csv")
    void logPickupPointChangesEmptyListTest() {
        pickupPointHistoryService.logPickupPointChanges(List.of());
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointchangeslogservice/before/add_pickup_point_log.csv",
            after = "/database/service/pickuppointchangeslogservice/after/add_pickup_point_log.csv")
    void logPickupPointChangesTest() {
        pickupPointHistoryService.logPickupPointChanges(getPickupPoints());
    }

    private List<PickupPoint> getPickupPoints() {
        return List.of(new PickupPoint()
                        .setId(1L)
                        .setBrandingType(FULL)
                        .setPartnerId(1L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2020-11-01"))
                        .setBrandRegionId(1L),
                new PickupPoint()
                        .setId(2L)
                        .setBrandingType(NONE)
                        .setPartnerId(2L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
        );
    }
}
