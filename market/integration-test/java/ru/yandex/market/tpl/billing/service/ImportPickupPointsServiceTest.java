package ru.yandex.market.tpl.billing.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.pvz.client.billing.PvzClient;
import ru.yandex.market.pvz.client.billing.dto.BillingPickupPointDto;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportPickupPointsServiceTest extends AbstractFunctionalTest {

    private static final long DS_ID_OF_YANDEX_OWN_PICKUP_POINT = 1005474;

    @Autowired
    private PvzClient pvzClient;

    @Autowired
    private TestableClock clock;

    @Autowired
    private ImportPickupPointsService importPickupPointsService;

    @BeforeEach
    void setUp() {
        when(pvzClient.getPickupPoints()).thenReturn(getPickupPoints());
    }

    @AfterEach
    void tearDown() {
        verify(pvzClient).getPickupPoints();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpickuppoints/before/empty_pickup_points.csv",
            after = "/database/service/importpickuppoints/after/pickup_points_imported.csv")
    void testImportPickupPointsToEmptyTable() {
        importPickupPointsService.importPickupPoints();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpickuppoints/before/two_pickup_points.csv",
            after = "/database/service/importpickuppoints/after/pickup_points_imported.csv")
    void testImportPickupPointsAddNewToNonEmptyTable() {
        importPickupPointsService.importPickupPoints();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpickuppoints/before/three_pickup_points.csv",
            after = "/database/service/importpickuppoints/after/pickup_points_imported.csv")
    void testImportPickupPointsUpdateExisting() {
        importPickupPointsService.importPickupPoints();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/pickuppointservice/before/check_pickup_point_without_partner.csv",
            after = "/database/service/pickuppointservice/after/check_pickup_point_without_partner.csv")
    void importPickupPointAndUpdateTariff() {
        when(pvzClient.getPickupPoints()).thenReturn(getPickupPointsWithOutPartners());
        importPickupPointsService.importPickupPoints();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpickuppoints/before/import_pickup_points_with_all_tariff_zones.csv",
            after = "/database/service/importpickuppoints/after/import_pickup_points_with_all_tariff_zones.csv")
    void importPickupPointsWithAllTariffZones() {
        when(pvzClient.getPickupPoints()).thenReturn(getPickupPointsOfAllTariffZones());
        importPickupPointsService.importPickupPoints();
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/importpickuppoints/before/add_new_tariffs.csv",
                    "/database/service/importpickuppoints/before/tariff.csv"},
            after = "/database/service/importpickuppoints/after/add_new_tariffs.csv")
    public void newPickupPointsTariffsTest() {
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        when(pvzClient.getPickupPoints()).thenReturn(getPickupPointForNewTariffs());
        importPickupPointsService.importPickupPoints();
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/importpickuppoints/before/add_new_tariffs.csv",
                    "/database/service/importpickuppoints/before/tariff.csv"},
            after = "/database/service/importpickuppoints/after/add_new_and_old_tariffs.csv")


    public void newAndOldPickupPointsTariffsTest() {
        clock.setFixed(Instant.parse("2021-07-30T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        when(pvzClient.getPickupPoints()).thenReturn(getPickupPointForNewAndOldTariffs());
        importPickupPointsService.importPickupPoints();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpickuppoints/before/test_with_create_billing_branded_since.csv",
            after = "/database/service/importpickuppoints/after/test_with_create_billing_branded_since.csv")
    void testWithCreateBillingBrandedSince() {
        when(pvzClient.getPickupPoints()).thenReturn(List.of(
                getFullBrandBuilder(1, LocalDate.of(2021, 1, 1)).build(),
                getFullBrandBuilder(2, LocalDate.of(2021, 10, 31)).build(),
                getFullBrandBuilder(3, LocalDate.of(2021, 11, 1)).build(),
                getFullBrandBuilder(4, LocalDate.of(2022, 1, 31)).build(),
                getFullBrandBuilder(5, LocalDate.of(2022, 2, 19)).build(),
                getFullBrandBuilder(6, LocalDate.of(2022, 2, 20)).build(),
                getFullBrandBuilder(7, LocalDate.of(2022, 2, 21)).build(),
                getNoneBrandedBuilder(8).build()
        ));
        importPickupPointsService.importPickupPoints();
    }

    private BillingPickupPointDto.BillingPickupPointDtoBuilder getFullBrandBuilder(int id, LocalDate brandedSince) {
        return BillingPickupPointDto.builder()
                .id(id)
                .deliveryServiceId(1)
                .legalPartnerId(1)
                .name("pvz_" + id)
                .active(true)
                .returnAllowed(true)
                .brandingType("FULL")
                .brandedSince(brandedSince)
                .brandRegionId(3L);
    }

    private BillingPickupPointDto.BillingPickupPointDtoBuilder getNoneBrandedBuilder(int id) {
        return BillingPickupPointDto.builder()
                .id(id)
                .deliveryServiceId(1)
                .legalPartnerId(1)
                .name("pvz_" + id)
                .active(true)
                .returnAllowed(false)
                .brandingType("NONE")
                .brandedSince(null);
    }

    @Nonnull
    private List<BillingPickupPointDto> getPickupPoints() {
        return List.of(
                BillingPickupPointDto.builder()
                        .id(1)
                        .deliveryServiceId(1)
                        .legalPartnerId(1)
                        .name("AAA")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(3L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(2)
                        .deliveryServiceId(2)
                        .legalPartnerId(2)
                        .name("BBB")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(3L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(3)
                        .deliveryServiceId(2)
                        .legalPartnerId(2)
                        .name("CCC")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(3L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(4)
                        .deliveryServiceId(DS_ID_OF_YANDEX_OWN_PICKUP_POINT)
                        .legalPartnerId(DS_ID_OF_YANDEX_OWN_PICKUP_POINT)
                        .name("YNDX")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(3L)
                        .build()
        );
    }

    @Nonnull
    private List<BillingPickupPointDto> getPickupPointsWithOutPartners() {
        return List.of(
                BillingPickupPointDto.builder()
                        .id(23L)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.parse("2021-12-21"))
                        .brandRegionId(2L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(1L)
                        .brandingType("NONE")
                        .deliveryServiceId(1)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(2L)
                        .brandingType("NONE")
                        .build()
        );
    }

    @Nonnull
    private List<BillingPickupPointDto> getPickupPointsOfAllTariffZones() {
        return List.of(
                BillingPickupPointDto.builder()
                        .id(1)
                        .deliveryServiceId(1)
                        .legalPartnerId(1)
                        .name("AAA")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(1L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(2)
                        .deliveryServiceId(2)
                        .legalPartnerId(2)
                        .name("BBB")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(2L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(3)
                        .deliveryServiceId(3)
                        .legalPartnerId(3)
                        .name("CCC")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(4L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(4)
                        .deliveryServiceId(4)
                        .legalPartnerId(4)
                        .name("YNDX")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(5L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(5)
                        .deliveryServiceId(5)
                        .legalPartnerId(5)
                        .name("FED")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(8L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(6)
                        .deliveryServiceId(6)
                        .legalPartnerId(6)
                        .name("XED")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(3L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(7)
                        .deliveryServiceId(7)
                        .legalPartnerId(7)
                        .name("EDX")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(15L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(8)
                        .deliveryServiceId(8)
                        .legalPartnerId(8)
                        .name("HED")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 1, 1))
                        .brandRegionId(41L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(9)
                        .deliveryServiceId(9)
                        .legalPartnerId(9)
                        .name("NONO")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("NONE")
                        .build()
        );
    }

    @Nonnull
    private List<BillingPickupPointDto> getPickupPointForNewTariffs() {
        return List.of(
                BillingPickupPointDto.builder()
                        .id(1)
                        .deliveryServiceId(1)
                        .legalPartnerId(1)
                        .name("AAA")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 8, 1))
                        .brandRegionId(1L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(2)
                        .deliveryServiceId(2)
                        .legalPartnerId(2)
                        .name("BBB")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 8, 5))
                        .brandRegionId(2L)
                        .build());
    }

    @Nonnull
    private List<BillingPickupPointDto> getPickupPointForNewAndOldTariffs() {
        return List.of(
                BillingPickupPointDto.builder()
                        .id(1)
                        .deliveryServiceId(1)
                        .legalPartnerId(1)
                        .name("AAA")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2020, 7, 30))
                        .brandRegionId(1L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(2)
                        .deliveryServiceId(2)
                        .legalPartnerId(2)
                        .name("BBB")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 7, 28))
                        .brandRegionId(2L)
                        .build(),
                BillingPickupPointDto.builder()
                        .id(3)
                        .deliveryServiceId(3)
                        .legalPartnerId(3)
                        .name("BBB")
                        .active(true)
                        .returnAllowed(true)
                        .brandingType("FULL")
                        .brandedSince(LocalDate.of(2021, 7, 20))
                        .brandRegionId(5L)
                        .build());
    }
}
