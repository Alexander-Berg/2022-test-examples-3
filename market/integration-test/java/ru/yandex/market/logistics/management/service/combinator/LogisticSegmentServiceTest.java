package ru.yandex.market.logistics.management.service.combinator;

import javax.persistence.PersistenceException;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.vladmihalcea.hibernate.type.array.LongArrayType;
import org.hibernate.jpa.TypedParameterValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.service.yt.YtLogisticsServicesUpdater;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TypedParameterValueMatcher;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тесты создания/изменения логистических сегментов")
@DatabaseSetup({
    "/data/service/combinator/db/before/regions.xml",
    "/data/service/combinator/db/before/service_codes.xml",
    "/data/service/combinator/db/before/cargo_types.xml",
    "/data/service/combinator/db/before/platform_client.xml",
    "/data/service/combinator/db/before/partner_subtypes.xml",
    "/data/service/combinator/db/before/partner_external_param_type.xml",
    "/data/service/combinator/db/before/logistic_segments_services_meta_keys.xml",
})
class LogisticSegmentServiceTest extends AbstractContextualAspectValidationTest {

    private static final TypedParameterValue SERVICE_IDS = new TypedParameterValue(
        LongArrayType.INSTANCE,
        new Long[]{1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L}
    );

    @Autowired
    private LogisticSegmentService logisticSegmentService;
    @Autowired
    private FeatureProperties featureProperties;
    @Autowired
    private YtLogisticsServicesUpdater ytLogisticsServicesUpdater;

    @BeforeEach
    void setup() {
        featureProperties.setGenerateBackwardMovementSegmentsForExpress(true);
    }

    @AfterEach
    void after() {
        try {
            logisticSegmentService.updateYtServices();
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Тест на создание логистических сегментов интервалов доставки")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_intervals.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delivery_intervals_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @CleanDatabase
    void testDeliveryIntervalsBuild() {
        logisticSegmentService.buildDeliveryIntervalSegments();
    }

    @Test
    @DisplayName("Синхронизация расписаний логистических сервисов с расписанием в интервалах доставки")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_intervals_sync_service_schedule.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delivery_intervals_sync_service_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDeliveryIntervalsBuild_shouldSyncServiceSchedule() {
        logisticSegmentService.buildDeliveryIntervalSegments();
    }

    @Test
    @DisplayName("Тест на создание логистических сегментов складов")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouses_build_segment.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouse_segments.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_cargo_types.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_relations.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/warehouses_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testWarehousesBuild() {
        logisticSegmentService.buildWarehousesSegments();
    }

    @Test
    @DisplayName("Тест на создание логистических сегментов складов (регион в сроках обработки и катоффах - 10000)")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouses_region_10000.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouse_segments.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_cargo_types.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_relations_region_10000.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/warehouses_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testWarehousesBuildRegion10000() {
        logisticSegmentService.buildWarehousesSegments();
    }

    @Test
    @DisplayName("Тест на создание логистических сегментов склада с партнером без расписания")
    @DatabaseSetup("/data/service/combinator/db/before/warehouse_without_schedule.xml")
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/warehouses_segments_without_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testWarehouseBuildWithPartnerWithoutSchedule() {
        logisticSegmentService.buildWarehousesSegments();
    }

    @Test
    @DisplayName(
        "Тест на создание логистических сегментов склада с партнером без расписания " +
            "(регион в сроках обработки и катоффах - 10000)"
    )
    @DatabaseSetup("/data/service/combinator/db/before/warehouse_without_schedule_region_10000.xml")
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/warehouses_segments_without_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testWarehouseBuildWithPartnerWithoutScheduleRegion10000() {
        logisticSegmentService.buildWarehousesSegments();
    }

    @Test
    @DisplayName(
        "Тест на создание логистических сегментов склада с партнером и объединенным календарем партнера "
            + "и логистической точки, при повторном запуске новый календарь не создается"
    )
    @DatabaseSetup("/data/service/combinator/db/before/warehouses_merging_calendars.xml")
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/warehouses_segments_merging_calendars.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testWarehouseBuildWithPartnerMergingCalendarsNoNewCreated() {
        logisticSegmentService.buildWarehousesSegments();
        logisticSegmentService.buildWarehousesSegments();
    }

    @Test
    @DisplayName("Тест на добавление DROPSHIP_EXPRESS сервиса")
    @DatabaseSetup("/data/service/combinator/db/warehouses/dropshipservice/before/warehouses_with_express.xml")
    @ExpectedDatabase(
        value = "/data/service/combinator/db/warehouses/dropshipservice/after/warehouses_segments_with_express.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testAddDropshipService() {
        logisticSegmentService.buildWarehousesSegments();
    }

    @Test
    @DisplayName("Тест на удаление DROPSHIP_EXPRESS сервиса")
    @DatabaseSetup({
        "/data/service/combinator/db/warehouses/dropshipservice/before/warehouses.xml",
        "/data/service/combinator/db/warehouses/dropshipservice/before/warehouses_segments_with_express.xml"
    })
    @ExpectedDatabase(
        value = "/data/service/combinator/db/warehouses/dropshipservice/after/warehouses_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testDeleteDropshipService() {
        logisticSegmentService.buildWarehousesSegments();
    }

    @Test
    @DisplayName(
        "Тест на создание логистических сегментов магистралей с флагом для SHIPMENT сервисов"
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/linehauls.xml",
        type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/linehauls_segments_with_new_shipment_services.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testLinehaulBuild() {
        logisticSegmentService.buildLinehaulSegments();
    }

    @Test
    @DisplayName(
        "Тест на создание логистических сегментов магистралей без флага с SHIPMENT сервисами"
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/linehauls.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/linehauls_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testLinehaulBuildOld() {
        featureProperties.setCreateShipmentForLinehaul(false);
        logisticSegmentService.buildLinehaulSegments();
        featureProperties.setCreateShipmentForLinehaul(true);
    }

    @Test
    @DisplayName(
        "Синхронизация расписаний логистических сервисов с расписанием в магистрали партнёра"
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/linehauls_sync_service_schedule.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/linehauls_sync_service_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testLinehaulBuild_shouldSyncServiceSchedule() {
        logisticSegmentService.buildLinehaulSegments();
    }

    @Test
    @DisplayName("Обновление сервисов при удалении pickup расписания")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/linehauls_pickup_without_schedule.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/linehauls_pickup_without_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testLinehaulRemovePickupSchedule() {
        logisticSegmentService.buildLinehaulSegments();
    }

    @Test
    @DisplayName("Обновление сервисов при добавления pickup расписания")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/linehauls_pickup_with_schedule.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/linehauls_pickup_with_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testLinehaulAddPickupSchedule() {
        logisticSegmentService.buildLinehaulSegments();
    }

    @Test
    @DisplayName("Создание и обновление логистических сегментов из связок партнёров - добавление возвратных сегментов")
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/warehouses.xml",
            "/data/service/combinator/db/before/partner_cargo_types.xml",
            "/data/service/combinator/db/before/partner_relations.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/partner_relations_from_dropship_to_sc.xml",
            "/data/service/combinator/db/before/partner_relations_segments_with_return.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_dropoff.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_relations_segments_with_backward.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void partnerRelationSegmentsBuildAndUpdate() {
        logisticSegmentService.buildPartnerRelationSegments();
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName("Экспресс без сц. Для сц построены возвратные маршруты в дропшипа. Ребро не удаляется")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouse.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/bmv_for_express.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/express_after_return_sc_remove.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void expressWithoutScAllReturnRoutesRemoved() {
        logisticSegmentService.buildPartnerRelationSegments();
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName(
        "Экспресс с сц. Несколько возвратных маршрутов. Должен остаться только возвратный маршрут из выбранного сц"
    )
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/warehouse.xml",
            "/data/service/combinator/db/before/return_sc_for_express.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/bmv_for_express.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/express_with_sc_removed_bmv.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void expressWithScAllReturnRoutesExceptChosenRemoved() {
        logisticSegmentService.buildPartnerRelationSegments();
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName(
        "Экспресс с сц. Замена сц. Должен остаться только возвратный маршрут из заново выбранного сц"
    )
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/warehouse.xml",
            "/data/service/combinator/db/before/another_return_sc_for_express.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/bmv_for_express.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/express_with_sc_changed_bmv.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void expressWithScChanged() {
        logisticSegmentService.buildPartnerRelationSegments();
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName(
        "Экспресс с сц. Возвратных маршрутов нет. Должен построиться возвратный маршрут только из выбранного сц"
    )
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/warehouse.xml",
            "/data/service/combinator/db/before/return_sc_for_express.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value =
            "/data/service/combinator/db/after/partner_relations_segments_with_return_sc_express_bmv_and_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void expressWithScCreateReturnRoutesOnlyForChosen() {
        logisticSegmentService.buildPartnerRelationSegments();
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName("Экспресс с сц, но релешн не в такси. Маршрут не строится")
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/warehouse.xml",
            "/data/service/combinator/db/before/return_sc_for_express.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/not_taxi.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value =
            "/data/service/combinator/db/after/partner_relations_segments_with_return_sc_express_without_bmv.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void expressWithScNotToTaxi() {
        logisticSegmentService.buildPartnerRelationSegments();
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName("Экспресс с сц. Возвратных маршрутов нет. Связка неактивна. Маршрут не создается")
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/warehouse.xml",
            "/data/service/combinator/db/before/return_sc_for_express.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/disable_express_relation.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/express_without_any_bmv.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void expressWithScDisableRelation() {
        logisticSegmentService.buildPartnerRelationSegments();
    }

    @Test
    @DisplayName("Экспресс без сц. Возвратных маршрутов нет и не будет")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouse.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/express_without_any_bmv.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void expressWithoutScDoNotCreateReturnRoutes() {
        logisticSegmentService.buildPartnerRelationSegments();
    }

    @Test
    @DisplayName("Экспресс без сц. Есть рилейшн в СЦ, бмв не должны создаться")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouse.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/relation_for_express.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/express_without_any_bmv.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void expressWithoutScDoNotCreateReturnRoutesWithRelation() {
        logisticSegmentService.buildPartnerRelationSegments();
    }

    @Test
    @DisplayName(
        "Создание и обновление логистических сегментов из связок партнёров - добавление возвратных сегментов, "
            + "существует не привязанный никуда BMV сегмент, игнорируем его"
    )
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/warehouses.xml",
            "/data/service/combinator/db/before/partner_cargo_types.xml",
            "/data/service/combinator/db/before/partner_relations.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/partner_relations_from_dropship_to_sc.xml",
            "/data/service/combinator/db/before/partner_relations_segments_with_return.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/partner_dropoff.xml",
            "/data/service/combinator/db/before/bmv_exist.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_relations_segments_with_backward_existed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void partnerRelationSegmentsBuildAndUpdateWithBackwardSegmentsOneExist() {
        logisticSegmentService.buildPartnerRelationSegments();
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName(
        "Тест на создание и обновление логистических сегментов из связок партнёров " +
            "(регион в сроках обработки и катоффах - 10000)"
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouses_region_10000.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_cargo_types.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_relations_region_10000.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_relations_from_dropship_to_sc.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_relations_segments.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_relations_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testPartnerRelationSegmentsBuildAndUpdateRegion10000() {
        logisticSegmentService.buildPartnerRelationSegments();
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName("Создание и обновление логистических сегментов из связок партнёров с добавлением CALL_COURIER")
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/warehouses.xml",
            "/data/service/combinator/db/before/partner_cargo_types.xml",
            "/data/service/combinator/db/before/partner_relations.xml",
            "/data/service/combinator/db/before/partner_relations_segments.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/taxi_express.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_relations_segments_with_call_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void partnerRelationSegmentsBuildAndUpdateCallCourier() {
        logisticSegmentService.buildPartnerRelationSegments();
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName("Тест на обновление логистических сегментов из выключенных связок партнёров")
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/warehouses.xml",
            "/data/service/combinator/db/before/partner_cargo_types.xml",
            "/data/service/combinator/db/before/partner_relations.xml",
            "/data/service/combinator/db/before/inactive_partner_relations_segments.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/inactive_partner_relations_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testInactivePartnerRelationSegmentsBuildAndUpdate() {
        logisticSegmentService.buildPartnerRelationSegments();
    }

    @Test
    @DisplayName(
        "Тест на обновление логистических сегментов из выключенных связок партнёров " +
            "(регион в сроках обработки и катоффах - 10000)"
    )
    @DatabaseSetup(
        value = {
            "/data/service/combinator/db/before/warehouses_region_10000.xml",
            "/data/service/combinator/db/before/partner_cargo_types.xml",
            "/data/service/combinator/db/before/partner_relations_region_10000.xml",
            "/data/service/combinator/db/before/inactive_partner_relations_segments.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/inactive_partner_relations_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testInactivePartnerRelationSegmentsBuildAndUpdateRegion10000() {
        logisticSegmentService.buildPartnerRelationSegments();
    }

    @Test
    @DisplayName("Тест на создание логистических сегментов пвз")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/pickup_points.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/pickup_points_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @CleanDatabase
    void testPickupPointBuild() {
        logisticSegmentService.buildPickupPointSegments();
    }

    @Test
    @DisplayName("Тест на обновление логистических сегментов пвз")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/pickup_points.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/pickup_points_deferred_courier.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/pickup_points_segments_to_update.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/pickup_points_segments_after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @CleanDatabase
    void testPickupPointUpdate() {
        logisticSegmentService.buildPickupPointSegments();
    }

    @Test
    @DisplayName("Синхронизация расписаний логистических сервисов с расписаниями ПВЗ")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/pickup_points_sync_service_schedule.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/pickup_points_sync_service_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @CleanDatabase
    void testPickupPointUpdate_shouldSyncServiceSchedule() {
        logisticSegmentService.buildPickupPointSegments();
    }

    @Test
    @DisplayName("Тест на проставление карготипов сервисам")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouses.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouses_segments.xml",
        type = DatabaseOperation.INSERT,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_cargo_types_to_update.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/updated_cargo_types.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testCargoTypesUpdate() {
        logisticSegmentService.updateYtServices();

        verify(ytLogisticsServicesUpdater).findUpdatedServiceIds();
        verify(ytLogisticsServicesUpdater)
            .updateLogisticsServices(argThat(new TypedParameterValueMatcher(SERVICE_IDS)));
        verifyNoMoreInteractions(ytLogisticsServicesUpdater);
    }

    @Test
    @DisplayName("Тест на отсеивание сервисов партнеров DROPSHIP_BY_SELLER при проставлении карготипов")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouses_with_DBS.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouses_segments.xml",
        type = DatabaseOperation.INSERT,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_cargo_types_to_update.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/updated_cargo_types_with_DBS.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testCargoTypesUpdateFilter() {
        logisticSegmentService.updateYtServices();

        verify(ytLogisticsServicesUpdater).findUpdatedServiceIds();
        verify(ytLogisticsServicesUpdater)
            .updateLogisticsServices(argThat(new TypedParameterValueMatcher(SERVICE_IDS)));
        verifyNoMoreInteractions(ytLogisticsServicesUpdater);
    }

    @Test
    @DisplayName("Тест механизма ретрая апдейта")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouses.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/warehouses_segments.xml",
        type = DatabaseOperation.INSERT,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_cargo_types_to_update.xml",
        type = DatabaseOperation.INSERT
    )
    void testYtServiceUpdateRetries() {
        doThrow(new PersistenceException("test exception"))
            .when(ytLogisticsServicesUpdater)
            .updateLogisticsServices(argThat(new TypedParameterValueMatcher(SERVICE_IDS)));

        assertThrows(RuntimeException.class, logisticSegmentService::updateYtServices);

        verify(ytLogisticsServicesUpdater).findUpdatedServiceIds();
        verify(ytLogisticsServicesUpdater, Mockito.times(3))
            .updateLogisticsServices(argThat(new TypedParameterValueMatcher(SERVICE_IDS)));
        verifyNoMoreInteractions(ytLogisticsServicesUpdater);
    }

    @Test
    @DisplayName("Проверка миграции дэйоффов для пикап точки")
    @DatabaseSetup("/data/service/combinator/db/before/pickup/logistics_point_with_day_offs.xml")
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/pickup/logistics_point_with_day_offs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void addDayOffToService() {
        logisticSegmentService.buildPickupPointSegments();
    }
}
