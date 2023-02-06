package ru.yandex.market.logistics.management.facade;

import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.transportation.TransportationMovementSegmentsBuilder;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;
import static com.github.springtestdbunit.annotation.DatabaseOperation.UPDATE;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@CleanDatabase
@DisplayName("Создание/изменение логистических сегментов для Transport Manager")
@DatabaseSetup({
    "/data/service/transportation/before/service_codes.xml",
    "/data/service/transportation/before/platform_client.xml",
    "/data/service/transportation/before/partners.xml",
    "/data/service/transportation/before/logistic_points.xml"
})
@DatabaseSetup(value = "/data/service/transportation/before/partner_relations.xml", type = INSERT)
class TransportationSegmentsFacadeTest extends AbstractContextualTest {

    @Autowired
    private TransportationSegmentsFacade transportationSegmentsFacade;

    @Autowired
    private TransportationMovementSegmentsBuilder transportationMovementSegmentsBuilder;

    @Test
    @DisplayName("Создание сегментов для связки fulfillment > delivery")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_ff_ds.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/segments.xml")
    @ExpectedDatabase(
        value = "/data/service/transportation/after/ff_ds_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createSegmentsAndServicesFfDs() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Не создаем TM сервисы, если еще не сгенерировались сервисы складов")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_dr_sc_ds.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/movement_not_generated_for_sc.xml")
    @ExpectedDatabase(
        value = "/data/service/transportation/after/segments_without_tm_movement.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void doNotCreateTmServicesWithoutWarehouseSegments() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Создание сегментов для связок dropship > sorting_center > delivery с виртуальным складом")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_dr_sc_ds.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/segments.xml")
    @ExpectedDatabase(
        value = "/data/service/transportation/after/dr_sc_ds_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createSegmentsAndServicesDrScDs() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Обновляем расписание в сервисе при его изменении в связке")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_dr_sc_ds.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/dr_sc_ds_created.xml")
    @DatabaseSetup(value = "/data/service/transportation/before/update_schedule.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/data/service/transportation/after/updated_schedule.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateScheduleWhenIfChanged() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Отключаем сервисы TM, если связка отключена")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_dr_sc_ds.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/dr_sc_ds_created.xml")
    @DatabaseSetup(value = "/data/service/transportation/before/disable_sc_ds_relation.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/data/service/transportation/after/sc_ds_disabled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void doNotDisableServicesWhenRelationDisabled() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Отключаем сервисы TM, если партнер неактивен")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_dr_sc_ds.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/dr_sc_ds_created.xml")
    @DatabaseSetup(value = "/data/service/transportation/before/disable_sc_ds_partners.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/data/service/transportation/after/dr_sc_ds_disabled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void disableServicesWhenPartnerInactive() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Отключаем сервисы TM, если появилось расписание лайнхола")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_dr_sc_ds.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/dr_sc_ds_created.xml")
    @DatabaseSetup(value = "/data/service/transportation/before/interwarehouse_schedule.xml")
    @ExpectedDatabase(
        value = "/data/service/transportation/after/dr_sc_ds_linehaul_disabled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void disableServiceWithLinehaulSchedule() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Можем отключить больше чем SMALLINT партнеров")
    void allowToDeactivateLotsOfPartners() {
        transportationMovementSegmentsBuilder.deactivateAllExcept(LongStream.range(1, 65536)
            .boxed()
            .collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("Отключаем сервисы TM, у связки нет расписания")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_dr_sc_ds.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/dr_sc_ds_created.xml")
    @DatabaseSetup(value = "/data/service/transportation/before/relation_without_schedule.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/data/service/transportation/after/dr_sc_ds_disabled_without_schedule.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void disableServicesForRelationWithoutSchedule() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Отключаем сервисы TM, если партнеры пропали из конфига TM")
    @DatabaseSetup("/data/service/transportation/before/dr_sc_ds_created.xml")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_disable.xml", type = REFRESH)
    @ExpectedDatabase(
        value = "/data/service/transportation/after/dr_sc_ds_disabled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void disableServicesWhenPartnersNotInTmConfig() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Не отключаем зафриженные сервисы TM, если партнеры пропали из конфига TM")
    @DatabaseSetup("/data/service/transportation/before/dr_sc_ds_created.xml")
    @DatabaseSetup(value = "/data/service/transportation/before/freeze_services.xml", type = UPDATE)
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_disable.xml", type = REFRESH)
    @ExpectedDatabase(
        value = "/data/service/transportation/before/dr_sc_ds_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void doNotDisableFrozenServicesWhenRelationDisabled() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Отключаем старые сервисы TM, создаем новые, если партнеры сменили склады")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_dr_sc_ds.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/dr_sc_ds_created.xml")
    @DatabaseSetup(value = "/data/service/transportation/before/switch_logistic_points.xml", type = REFRESH)
    @ExpectedDatabase(
        value = "/data/service/transportation/after/dr_sc_ds_recreated.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void recreateServicesWhenPartnerSwitchedOutboundWarehouse() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Создание логистического сегмента с типом Movement для TPL связки")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_ff_ds_and_sc_ds.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/segments.xml")
    @DatabaseSetup(value = "/data/service/transportation/before/tpl_relation.xml", type = REFRESH)
    @ExpectedDatabase(
        value = "/data/service/transportation/after/ff_ds_tpl_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createMovingSegmentForTplRelation() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Приоритет сегменту с лог. точкой приемки указанной в связке (import, delivery)")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_ff_ds.xml", type = REFRESH)
    @DatabaseSetup(value = "/data/service/transportation/after/with_to_warehouse.xml", type = UPDATE)
    @DatabaseSetup("/data/service/transportation/before/segments.xml")
    @DatabaseSetup(value = "/data/service/transportation/after/virtual_segment.xml", type = REFRESH)
    @ExpectedDatabase(
        value = "/data/service/transportation/after/ff_ds.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void chooseSegmentWithRelationWarehouse() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }

    @Test
    @DisplayName("Создание сегментов для связок dropship > dropoff")
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_disable.xml", type = REFRESH)
    @DatabaseSetup(value = "/data/service/transportation/before/partner_relations_dr_df.xml", type = REFRESH)
    @DatabaseSetup("/data/service/transportation/before/dr_df_segments.xml")
    @ExpectedDatabase(
        value = "/data/service/transportation/after/dr_df_services.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createSegmentsAndServicesDrDf() {
        transportationSegmentsFacade.buildTransportManagerSegments();
    }
}
