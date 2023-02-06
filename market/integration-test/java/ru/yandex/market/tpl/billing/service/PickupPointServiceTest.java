package ru.yandex.market.tpl.billing.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.entity.PickupPoint;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;

import static ru.yandex.market.tpl.billing.model.PickupPointBrandingType.FULL;
import static ru.yandex.market.tpl.billing.model.PickupPointBrandingType.NONE;

public class PickupPointServiceTest extends AbstractFunctionalTest {

    @Autowired
    private PickupPointService pickupPointService;

    @Autowired
    TestableClock clock;

    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointservice/before/add_tariffs_to_one_pickup_point.csv",
            after = "/database/service/pickuppointservice/after/add_tariffs_to_one_pickup_point.csv")
    void addTariffToNewPickupPointTest() {
        List<PickupPoint> newPickupPoints = pickupPointService.separateNewPickupPoints(getPickupPoint(),
                pickupPointService.getIdOfAllPickupPoints());
        pickupPointService.upsert(getPickupPoint());
        pickupPointService.addTariffsForNewPickupPoints(newPickupPoints);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointservice/before/add_tariffs_to_two_pickup_points.csv",
            after = "/database/service/pickuppointservice/after/add_tariffs_to_two_pickup_points.csv")
    void addTariffToNewPickupPointsTest() {
        List<PickupPoint> newPickupPoints = pickupPointService.separateNewPickupPoints(getPickupPoints(),
                pickupPointService.getIdOfAllPickupPoints());
        pickupPointService.upsert(getPickupPoints());
        pickupPointService.addTariffsForNewPickupPoints(newPickupPoints);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointservice/before/add_tariffs_to_two_pickup_points.csv",
            after = "/database/service/pickuppointservice/before/add_tariffs_to_two_pickup_points.csv")
    void addTariffsEmptyListTest() {
        pickupPointService.addTariffsForNewPickupPoints(List.of());
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointservice/before/" +
                    "add_tariffs_to_pickup_points_with_branding_types_none_full.csv",
            after = "/database/service/pickuppointservice/after/" +
                    "add_tariffs_to_pickup_points_with_branding_types_none_full.csv")
    void addTariffToNewPickupPointsWithBrandingTypeFullTest() {
        clock.setFixed(Instant.parse("2021-07-30T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        List<PickupPoint> newPickupPoints = pickupPointService
                .separateNewPickupPoints(getPickupPointsWithBrandingTypeFull(),
                        pickupPointService.getIdOfAllPickupPoints());
        pickupPointService.upsert(getPickupPointsWithBrandingTypeFull());
        pickupPointService.addTariffsForNewPickupPoints(newPickupPoints);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointservice/before/update_tariffs.csv",
            after = "/database/service/pickuppointservice/after/update_tariffs.csv")
    void updateOldTariffs() {
        clock.setFixed(Instant.parse("2021-07-30T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        List<PickupPoint> verifiedPickupPoints = pickupPointService
                .getPickupPointsWithBrandingTypeChanges(getPickupPointsUpgradeTariffs());
        List<PickupPoint> newPickupPoints = pickupPointService
                .separateNewPickupPoints(getPickupPointsUpgradeTariffs(), pickupPointService.getIdOfAllPickupPoints());
        pickupPointService.upsert(getPickupPointsUpgradeTariffs());
        pickupPointService.updateTariffsWithChangedBrandingTypeToFull(verifiedPickupPoints);
        pickupPointService.addTariffsForNewPickupPoints(newPickupPoints);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointservice/before/check_pickup_point_without_partner.csv",
            after = "/database/service/pickuppointservice/after/check_pickup_point_without_partner.csv")
    void verifyPickupPointsWithOutPartners() {
        List<PickupPoint> pickupPointsWithPartners =
                pickupPointService.verifyPartners(getPickupPointsWithOutPartners());
        pickupPointService.upsert(pickupPointsWithPartners);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointservice/before/add_tariffs_to_pickup_points_null_offer_date.csv",
            after = "/database/service/pickuppointservice/after/add_tariffs_to_pickup_points_null_offer_date.csv")
    void addTariffToNewPickupPointsWithNullOfferDate() {
        List<PickupPoint> newPickupPoints = pickupPointService
                .separateNewPickupPoints(
                        getPickupPoints(),
                        pickupPointService.getIdOfAllPickupPoints());
        pickupPointService.upsert(getPickupPoints());
        pickupPointService.addTariffsForNewPickupPoints(newPickupPoints);
    }

    @Test
    @DbUnitDataSet(before = "/database/service/pickuppointservice/before/add_tariffs_to_two_pickup_points.csv")
    void checkPickupPointsWithoutBrandRegionId() {
        Assertions.assertThatThrownBy(() ->
                        pickupPointService.addTariffsForNewPickupPoints(getPickupPointsWithoutBrandRegionId()))
                .isInstanceOf(TplIllegalArgumentException.class)
                .hasMessage("These pickupPoints without brand_region_id id's: [1, 2]");
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/importpickuppoints/before/tariff.csv",
                    "/database/service/pickuppointservice/before/create_tariffs_for_old_pp.csv"},
            after = "/database/service/pickuppointservice/after/create_tariffs_for_old_pp.csv")
    public void createTariffsForOldPickupPoints() {
        pickupPointService.createTariffsForOldPickupPoints();
    }


    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointservice/before/add_tariffs_to_ignored_pickup_point.csv",
            after = "/database/service/pickuppointservice/after/add_tariffs_to_ignored_pickup_point.csv"
    )
    public void addTariffsForIgnoredPickupPoint() {
        pickupPointService.addTariffsForNewPickupPoints(getPickupPointOfIgnoredPartner());
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpickuppoints/before/savingDropoffTariffs.csv",
            after = "/database/service/importpickuppoints/after/savingDropoffTariffs.csv")
    void savingDropoffTariffs() {
        pickupPointService.createDropoffReturnTariffsForPickupPoints();
    }


    @Nonnull
    private List<PickupPoint> getPickupPoint() {
        return List.of(new PickupPoint()
                .setId(1L)
                .setBrandingType(NONE)
                .setPartnerId(1L)
                .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                .setBrandedSince(LocalDate.parse("2020-11-01"))
        );
    }

    @Nonnull
    private List<PickupPoint> getPickupPointOfIgnoredPartner() {
        return List.of(new PickupPoint()
                .setId(1L)
                .setBrandingType(FULL)
                .setPartnerId(1006518L)
                .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                .setBrandedSince(LocalDate.parse("2020-11-01"))
        );
    }

    @Nonnull
    private List<PickupPoint> getPickupPoints() {
        return List.of(new PickupPoint()
                        .setId(1L)
                        .setBrandingType(NONE)
                        .setPartnerId(2L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2020-11-01")),
                new PickupPoint()
                        .setId(2L)
                        .setBrandingType(NONE)
                        .setPartnerId(3L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2020-11-01")),
                new PickupPoint()
                        .setId(23L)
                        .setBrandingType(NONE)
                        .setPartnerId(1L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2020-11-01"))
        );
    }

    @Nonnull
    private List<PickupPoint> getPickupPointsWithBrandingTypeFull() {
        return List.of(new PickupPoint()
                        .setId(1L)
                        .setBrandingType(NONE)
                        .setPartnerId(2L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2020-11-01")),
                new PickupPoint()
                        .setId(2L)
                        .setBrandingType(NONE)
                        .setPartnerId(3L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2020-11-01")),
                new PickupPoint()
                        .setId(23L)
                        .setBrandingType(NONE)
                        .setPartnerId(1L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2020-11-01")),
                new PickupPoint()
                        .setId(26L)
                        .setBrandingType(FULL)
                        .setPartnerId(1L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2020-11-01"))
                        .setBrandRegionId(1L),
                new PickupPoint()
                        .setId(33L)
                        .setBrandingType(FULL)
                        .setPartnerId(1L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2020-11-01"))
                        .setBrandRegionId(2L)
        );
    }

    @Nonnull
    private List<PickupPoint> getPickupPointsUpgradeTariffs() {
        return List.of(new PickupPoint()
                        .setId(1L)
                        .setBrandingType(FULL)
                        .setPartnerId(2L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2021-07-01"))
                        .setBrandRegionId(2L),
                new PickupPoint()
                        .setId(2L)
                        .setBrandingType(NONE)
                        .setPartnerId(3L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00")),
                new PickupPoint()
                        .setId(23L)
                        .setBrandingType(FULL)
                        .setPartnerId(1L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2021-07-01"))
                        .setBrandRegionId(1L)
        );
    }

    @Nonnull
    private List<PickupPoint> getPickupPointsUpgradeTariffsWithChanges() {
        return List.of(new PickupPoint()
                        .setId(23L)
                        .setBrandingType(FULL)
                        .setPartnerId(1L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2021-12-21"))
                        .setBrandRegionId(2L),
                new PickupPoint()
                        .setId(1L)
                        .setBrandingType(FULL)
                        .setPartnerId(2L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2021-12-01"))
                        .setBrandRegionId(1L),
                new PickupPoint()
                        .setId(2L)
                        .setBrandingType(NONE)
                        .setPartnerId(3L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
        );
    }

    @Nonnull
    private List<PickupPoint> getPickupPointsWithOutPartners() {
        return List.of(new PickupPoint()
                        .setId(23L)
                        .setBrandingType(FULL)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2021-12-21"))
                        .setBrandRegionId(2L),
                new PickupPoint()
                        .setId(1L)
                        .setBrandingType(NONE)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setPartnerId(1L),
                new PickupPoint()
                        .setId(2L)
                        .setBrandingType(NONE)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
        );
    }

    @Nonnull
    private List<PickupPoint> getPickupPointsWithoutBrandRegionId() {
        return List.of(
                new PickupPoint()
                        .setId(1L)
                        .setBrandingType(FULL)
                        .setPartnerId(1L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2021-12-21")),
                new PickupPoint()
                        .setId(2L)
                        .setBrandingType(FULL)
                        .setPartnerId(2L)
                        .setCreatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setUpdatedAt(OffsetDateTime.parse("2021-03-23T10:17:44.490+03:00"))
                        .setBrandedSince(LocalDate.parse("2021-12-21"))
                        .setBrandRegionId(0L));

    }

}
