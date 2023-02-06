package ru.yandex.market.logistics.management.service.export.dynamic;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.googlecode.protobuf.format.JsonFormat;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.DynamicFault;
import ru.yandex.market.logistics.management.domain.entity.DynamicLog;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.repository.export.dynamic.DynamicFaultRepository;
import ru.yandex.market.logistics.management.repository.export.dynamic.DynamicLogRepository;
import ru.yandex.market.logistics.management.service.export.FileContent;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TestableClock;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus.FAILED;
import static ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus.OK;
import static ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus.WARN;

@CleanDatabase
@SuppressWarnings("checkstyle:MagicNumber")
class ReportDynamicBuilderTest extends AbstractContextualTest {

    @Autowired
    private ReportDynamicBuilder builder;

    @Autowired
    private TestableClock clock;

    @Autowired
    private DynamicLogRepository dynamicLogRepository;

    @Autowired
    private DynamicFaultRepository faultRepository;

    @BeforeEach
    void setup() {
        clock.setFixed(ZonedDateTime.of(2018, 10, 4, 12, 0, 0, 0,
            ZoneId.systemDefault()
        ).toInstant(), ZoneId.systemDefault());
        builder.setDateOffset(0);
    }

    @Test
    @Sql("/data/service/export/dynamic/prepare_data.sql")
    void testZeroOffset() {
        builder.setDateOffset(0);
        assertDynamicByJson("data/service/export/dynamic/mds.json");
    }

    @Test
    @Sql("/data/service/export/dynamic/prepare_data_disabled.sql")
    void testActiveInactiveAndDisabledAreLoadedWithBoolean() {
        builder.setDateOffset(0);
        assertDynamicByJson("data/service/export/dynamic/mds_disabled.json");
    }

    @Test
    @Sql("/data/service/export/dynamic/prepare_data.sql")
    void testNegativeOffset() {
        builder.setDateOffset(-10);
        assertDynamicByJson("data/service/export/dynamic/mds_negative_offset.json");
    }

    @Test
    @Sql("/data/service/export/dynamic/validation/relations_all_ok.sql")
    void testOkDynamicSaved() {
        builder.setDateOffset(0);
        builder.buildFilesContent();

        List<DynamicLog> dynamicLogs = dynamicLogRepository.findAll();
        softly.assertThat(dynamicLogs).hasSize(2);
        assertValidDynamic(dynamicLogs.get(0), OK, 1L);
        assertValidDynamic(dynamicLogs.get(1), OK, 2L);

        List<DynamicFault> faults = faultRepository.findAll();
        softly.assertThat(faults).isEmpty();
    }

    @Test
    @Sql("/data/service/export/dynamic/validation/relations_all_ok.sql")
    void testOkDynamicBuild() {
        builder.setDateOffset(0);
        ArrayList<FileContent<Logistics.MetaInfo>> contentList = new ArrayList<>(builder.buildFilesContent());

        // todo исправить индексы, когда выпилят временный динамик
        assertThatJson(JsonFormat.printToString(contentList.get(1).getContent())).
            when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(TestUtil.pathToJson("data/service/export/dynamic/validation/mds_beru_ok.json"));

        assertThatJson(JsonFormat.printToString(contentList.get(2).getContent())).
            when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(TestUtil.pathToJson("data/service/export/dynamic/validation/mds_bringly_ok.json"));
    }

    @Test
    @Sql("/data/service/export/dynamic/validation/relations_warn_ignored.sql")
    void testWarnDynamicIgnored() {
        builder.setDateOffset(0);
        builder.buildFilesContent();

        List<DynamicLog> dynamicLogs = dynamicLogRepository.findAll();
        softly.assertThat(dynamicLogs).hasSize(1);
        assertValidDynamic(dynamicLogs.get(0), WARN, 1L);

        List<DynamicFault> faults = faultRepository.findAll();
        softly.assertThat(faults).hasSize(2);
        softly.assertThat(faults).extracting(DynamicFault::getStatus).containsOnly(WARN);
        softly.assertThat(faults).extracting(DynamicFault::getEntityId).containsExactlyInAnyOrder(2L, 3L);
        softly.assertThat(faults).extracting(DynamicFault::getReasons)
            .containsExactlyInAnyOrder(
                "Для связки [id=3] 'Fulfillment service 2 + Delivery service 2' mock cause, #связканевыгружается.",
                "Для связки [id=2] 'Fulfillment service 1 + Delivery service 2' mock cause, #связканевыгружается."
            );
    }

    @Test
    @Sql("/data/service/export/dynamic/validation/relations_warn_ignored.sql")
    void testWarnDynamicBuild() {
        builder.setDateOffset(0);
        assertDynamicByJson("data/service/export/dynamic/validation/mds_warn_ignored.json");
    }

    @Test
    @Sql("/data/service/export/dynamic/validation/relations_failed.sql")
    void testFailedDynamic() {
        builder.buildFilesContent();
        List<DynamicLog> logs = dynamicLogRepository.findAll();
        softly.assertThat(logs).hasSize(1);
        softly.assertThat(logs).extracting(DynamicLog::getStatus).containsOnly(FAILED);

        List<DynamicFault> faults = faultRepository.findAll();
        softly.assertThat(faults).hasSize(3);

        faults.sort(Comparator.comparingLong(DynamicFault::getEntityId));
        softly.assertThat(faults).extracting(DynamicFault::getEntityId).containsExactly(2L, 3L, 4L);
        softly.assertThat(faults).extracting(DynamicFault::getStatus).containsExactly(FAILED, WARN, FAILED);
        softly.assertThat(faults)
            .extracting(DynamicFault::getReasons)
            .containsExactly(
                "Для связки [id=2] 'Fulfillment service 2 + Delivery service 2' mock cause, #динамикневыгружается.",
                "Для связки [id=3] 'Fulfillment service 3 + Delivery service 3' mock cause, #связканевыгружается.",
                "Для связки [id=4] 'Fulfillment service 4 + Delivery service 4' mock cause, #динамикневыгружается."
            );
    }

    @Test
    @Sql("/data/service/export/dynamic/partner_relations_with_inbound_transfer_time.sql")
    void testInboundAndTransferTimeAdded() {
        builder.buildFilesContent();

        List<DynamicLog> dynamicLogs = dynamicLogRepository.findAll();
        assertValidDynamic(dynamicLogs.get(0),
            OK, 1L
        );
    }

    @Test
    @Sql("/data/service/export/dynamic/partner_relations_warehouses_and_deliveries.sql")
    void testWarehousesAndDeliveryLinksAdded() {
        builder.buildFilesContent();

        List<DynamicLog> dynamicLogs = dynamicLogRepository.findAll();
        assertValidDynamic(
            dynamicLogs.get(0),
            OK,
            1L
        );
    }

    @Test
    @Sql("/data/service/export/dynamic/holidays_from_logistic_point_schedule.sql")
    void testPartnerHolidaysInferredFromWarehouseHolidays() {
        assertDynamicByJson("data/service/export/dynamic/mds_patched_holidays.json");
    }

    @Test
    @Sql("/data/service/export/dynamic/holidays_from_logistic_point_schedule_fail.sql")
    void testPartnerHolidaysInferredFromWarehouseHolidaysFails() {
        softly.assertThatThrownBy(() -> builder.buildFilesContent())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Partner '6' has more than one active warehouse");
    }

    @Test
    @Sql("/data/service/export/dynamic/whwh_link_for_disabled_relation.sql")
    void disableWhWhLinkForDisabledRelation() {
        assertDynamicByJson("data/service/export/dynamic/whwh_link_for_disabled_relation.json");
    }

    @Test
    @Sql("/data/service/export/dynamic/shipment_schedule_for_dropship_express.sql")
    void testShipmentScheduleForDropshipExpress() {
        assertDynamicByJson("data/service/export/dynamic/mds_shipment_schedule.json");
    }

    @Test
    @Sql("/data/service/export/dynamic/shipment_schedule_for_hide_offers_at_non_working_time.sql")
    void testHideOffersAtNonWorkingTime() {
        assertDynamicByJson("data/service/export/dynamic/mds_hide_offers_at_non_working_time.json");
    }

    @Test
    @Sql("/data/service/export/dynamic/shipment_schedule_for_dropship_express_and_retail.sql")
    void retailDoesntLoadAsExpress() {
        assertDynamicByJson("data/service/export/dynamic/mds_shipment_schedule.json");
    }

    private void assertDynamicByJson(String relativePath) {
        Logistics.MetaInfo metaInfo = builder.buildFilesContent().stream().findFirst()
            .map(FileContent::getContent)
            .orElseThrow(() -> new RuntimeException("Not file content found"));

        String protoAsString = JsonFormat.printToString(metaInfo);
        assertThatJson(protoAsString)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(TestUtil.pathToJson(relativePath));
    }

    private void assertValidDynamic(DynamicLog dynamicLog, ValidationStatus status, Long platformClientId) {
        softly.assertThat(dynamicLog).extracting(DynamicLog::getStatus).isEqualTo(status);
        softly.assertThat(dynamicLog)
            .extracting(log -> log.getPlatformClient().getId()).isEqualTo(platformClientId);
    }
}
