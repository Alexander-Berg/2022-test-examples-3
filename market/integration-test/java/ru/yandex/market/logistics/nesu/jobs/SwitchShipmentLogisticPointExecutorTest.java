package ru.yandex.market.logistics.nesu.jobs;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.filter.ExistingOrderSearchFilter;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.ActivityStatus;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.nesu.jobs.executor.SwitchShipmentLogisticPointExecutor;
import ru.yandex.market.logistics.nesu.jobs.producer.SwitchShipmentLogisticPointProducer;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.utils.PartnerUtils;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.IS_DROPOFF;

@DatabaseSetup({
    "/jobs/executors/switch_shipment_logistic_point/shop.xml",
    "/jobs/executors/switch_shipment_logistic_point/shop_notification.xml",
    "/jobs/executors/switch_shipment_logistic_point/shop_partner_settings.xml",
})
@DisplayName("Смена точки сдачи дропшип-магазинов в связи с отсутствием заказов")
public class SwitchShipmentLogisticPointExecutorTest extends AbstractContextualTest {

    private static final String ENABLED_PARTNER_EXTERNAL_PARAM_VALUE = "1";

    private static final LogisticSegmentFilter LOGISTIC_SEGMENT_DROPSHIPS_FILTER = LogisticSegmentFilter.builder()
        .setPartnerIds(Set.of(1002L, 1003L))
        .setTypes(Set.of(LogisticSegmentType.WAREHOUSE))
        .build();

    private static final LogisticSegmentFilter LOGISTIC_SEGMENT_MOVEMENT_FILTER = LogisticSegmentFilter.builder()
        .setIds(Set.of(12L, 13L))
        .setTypes(Set.of(LogisticSegmentType.MOVEMENT))
        .setServiceStatuses(Set.of(ActivityStatus.ACTIVE))
        .setServiceCodes(Set.of(ServiceCodeName.MOVEMENT))
        .build();

    private static final SearchPartnerFilter SEARCH_PARTNER_FILTER = SearchPartnerFilter.builder()
        .setStatuses(PartnerUtils.VALID_GLOBAL_STATUSES)
        .setExternalParamsIntersection(Set.of(new PartnerExternalParamRequest(
            IS_DROPOFF,
            ENABLED_PARTNER_EXTERNAL_PARAM_VALUE
        )))
        .build();

    private static final ExistingOrderSearchFilter EXISTING_ORDER_SEARCH_FILTER_1 = ExistingOrderSearchFilter.builder()
        .partnerIds(List.of(1002L))
        .createdFrom(Instant.parse("2021-05-27T14:00:00.00Z"))
        .build();

    private static final ExistingOrderSearchFilter EXISTING_ORDER_SEARCH_FILTER_2 = ExistingOrderSearchFilter.builder()
        .partnerIds(List.of(1003L))
        .createdFrom(Instant.parse("2021-05-27T14:00:00.00Z"))
        .build();

    private static final LogisticsPointFilter LOGISTICS_POINT_FILTER = LogisticsPointFilter.newBuilder()
        .type(PointType.WAREHOUSE)
        .partnerIds(Set.of(1002L))
        .active(true)
        .build();

    private static final Instant NOW = Instant.parse("2021-06-10T14:00:00.00Z");

    @Autowired
    private SwitchShipmentLogisticPointExecutor switchShipmentLogisticPointExecutor;

    @Autowired
    private SwitchShipmentLogisticPointProducer switchShipmentLogisticPointProducer;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LomClient lomClient;

    @RegisterExtension
    final BackLogCaptor backLogCaptor = new BackLogCaptor();

    @BeforeEach
    void setup() {
        clock.setFixed(NOW, ZoneId.systemDefault());
        doNothing().when(switchShipmentLogisticPointProducer).produceTask(anyLong(), anyLong(), anyString());

        when(lmsClient.searchLogisticSegments(LOGISTIC_SEGMENT_DROPSHIPS_FILTER))
            .thenReturn(List.of(
                LmsFactory.logisticSegmentDto(
                    2L,
                    1002L,
                    LogisticSegmentType.WAREHOUSE
                )
                    .setNextSegmentIds(List.of(12L)),
                LmsFactory.logisticSegmentDto(
                    3L,
                    1003L,
                    LogisticSegmentType.WAREHOUSE
                )
                    .setNextSegmentIds(List.of(13L))
            ));

        when(lmsClient.searchLogisticSegments(LOGISTIC_SEGMENT_MOVEMENT_FILTER))
            .thenReturn(List.of(
                LmsFactory.logisticSegmentDto(
                    12L,
                    1002L,
                    LogisticSegmentType.MOVEMENT
                )
                    .setServices(List.of(
                        LogisticSegmentServiceDto.builder()
                            .setCode(ServiceCodeName.MOVEMENT)
                            .setStatus(ActivityStatus.ACTIVE)
                            .build()
                    ))
                    .setNextSegmentPartnerIds(List.of(22L))
                    .setPreviousSegmentPartnerIds(List.of(1002L)),
                LmsFactory.logisticSegmentDto(
                    13L,
                    1003L,
                    LogisticSegmentType.MOVEMENT
                )
                    .setServices(List.of(
                        LogisticSegmentServiceDto.builder()
                            .setCode(ServiceCodeName.MOVEMENT)
                            .setStatus(ActivityStatus.ACTIVE)
                            .build()
                    ))
                    .setNextSegmentPartnerIds(List.of(23L))
                    .setPreviousSegmentPartnerIds(List.of(1003L))
            ));

        when(lmsClient.searchPartners(SEARCH_PARTNER_FILTER))
            .thenReturn(List.of(
                LmsFactory.createPartner(
                    22,
                    PartnerType.SORTING_CENTER,
                    PartnerStatus.ACTIVE,
                    List.of(new PartnerExternalParam(
                        IS_DROPOFF.name(),
                        ENABLED_PARTNER_EXTERNAL_PARAM_VALUE,
                        "Является ли служба дропоффом."
                    ))
                ),
                LmsFactory.createPartner(
                    23,
                    PartnerType.SORTING_CENTER,
                    PartnerStatus.ACTIVE,
                    List.of(new PartnerExternalParam(
                        IS_DROPOFF.name(),
                        ENABLED_PARTNER_EXTERNAL_PARAM_VALUE,
                        "Является ли служба дропоффом."
                    ))
                )
            ));

        when(lomClient.checkOrdersExisting(EXISTING_ORDER_SEARCH_FILTER_1))
            .thenReturn(Map.of(1002L, false));

        when(lomClient.checkOrdersExisting(EXISTING_ORDER_SEARCH_FILTER_2))
            .thenReturn(Map.of(1003L, true));

        when(lmsClient.getLogisticsPoints(LOGISTICS_POINT_FILTER))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder()
                    .partnerId(1002L)
                    .name("Самый лучший склад")
                    .build()
            ));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, lomClient, switchShipmentLogisticPointProducer);
    }

    @Test
    @DisplayName("Найти магазины, которые можно переключать на СЦ")
    void findShopsToSwitchLogisticPoint() {
        switchShipmentLogisticPointExecutor.doJob(null);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Found 2 partners to switch logistic point: [1002, 1003]")
            .contains("2 active movement segments found")
            .contains("Found 2 partners related with dropoffs to switch logistic point: [1002, 1003]")
            .contains("Found information about orders of 2 shops: {1002=false, 1003=true}")
            .contains("1 dropships without orders found: [1002]");

        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_DROPSHIPS_FILTER);
        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_MOVEMENT_FILTER);
        verify(lmsClient).searchPartners(SEARCH_PARTNER_FILTER);
        verify(lomClient).checkOrdersExisting(EXISTING_ORDER_SEARCH_FILTER_1);
        verify(lomClient).checkOrdersExisting(EXISTING_ORDER_SEARCH_FILTER_2);
        verify(lmsClient).getLogisticsPoints(LOGISTICS_POINT_FILTER);
        verify(switchShipmentLogisticPointProducer).produceTask(1002L, 2L, "Самый лучший склад");
    }

    @Test
    @DisplayName("Не найдено ни одного сегмента склада дропшипа")
    void dropshipWarehouseSegmentsNotFound() {
        doReturn(List.of())
            .when(lmsClient)
            .searchLogisticSegments(LOGISTIC_SEGMENT_DROPSHIPS_FILTER);

        softly.assertThatThrownBy(() -> switchShipmentLogisticPointExecutor.doJob(null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage(
                "Failed to find %s segments for partners %s",
                Set.of(LogisticSegmentType.WAREHOUSE),
                List.of(1002L, 1003L)
            );

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Found 2 partners to switch logistic point: [1002, 1003]");
        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_DROPSHIPS_FILTER);
    }

    @Test
    @DisplayName("Не найдено ни одного идентификатора следующего сегмента")
    @DatabaseSetup("/jobs/executors/switch_shipment_logistic_point/shop_partner_settings.xml")
    void dropshipNextSegmentsNotFound() {
        doReturn(List.of(
            LmsFactory.logisticSegmentDto(
                1L,
                1L,
                LogisticSegmentType.WAREHOUSE
            )
                .setNextSegmentIds(List.of())
        ))
            .when(lmsClient)
            .searchLogisticSegments(LOGISTIC_SEGMENT_DROPSHIPS_FILTER);

        switchShipmentLogisticPointExecutor.doJob(null);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Found 2 partners to switch logistic point: [1002, 1003]");
        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_DROPSHIPS_FILTER);
    }

    @Test
    @DisplayName("Не найдено ни одного сегмента перемещения")
    void movementSegmentsNotFound() {
        doReturn(List.of())
            .when(lmsClient)
            .searchLogisticSegments(LOGISTIC_SEGMENT_MOVEMENT_FILTER);

        switchShipmentLogisticPointExecutor.doJob(null);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Found 2 partners to switch logistic point: [1002, 1003]")
            .contains("0 active movement segments found");
        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_DROPSHIPS_FILTER);
        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_MOVEMENT_FILTER);
    }

    @Test
    @DisplayName("Не найдено ни одного дропоффа")
    @DatabaseSetup("/jobs/executors/switch_shipment_logistic_point/shop_partner_settings.xml")
    void dropoffsNotFound() {
        doReturn(List.of())
            .when(lmsClient)
            .searchPartners(SEARCH_PARTNER_FILTER);

        softly.assertThatThrownBy(() -> switchShipmentLogisticPointExecutor.doJob(null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find partners with %s == 1", IS_DROPOFF);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Found 2 partners to switch logistic point: [1002, 1003]")
            .contains("2 active movement segments found");
        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_DROPSHIPS_FILTER);
        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_MOVEMENT_FILTER);
        verify(lmsClient).searchPartners(SEARCH_PARTNER_FILTER);
    }

    @Test
    @DisplayName("Не найдено ни одного дропшипа, всё ещё подключенного к дропоффу")
    void dropshipsRelatedWithDropoffsNotFound() {
        doReturn(List.of(LmsFactory.createPartner(
            24,
            PartnerType.SORTING_CENTER,
            PartnerStatus.ACTIVE
        )))
            .when(lmsClient)
            .searchPartners(SEARCH_PARTNER_FILTER);

        switchShipmentLogisticPointExecutor.doJob(null);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Found 2 partners to switch logistic point: [1002, 1003]")
            .contains("2 active movement segments found")
            .contains("Found 0 partners related with dropoffs to switch logistic point: []");
        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_DROPSHIPS_FILTER);
        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_MOVEMENT_FILTER);
        verify(lmsClient).searchPartners(SEARCH_PARTNER_FILTER);
    }

    @Test
    @DisplayName("Не найдено ни одного дропшипа без заказов")
    void dropshipsWithoutOrdersNotFound() {
        doReturn(Map.of(1002L, true))
            .when(lomClient).checkOrdersExisting(EXISTING_ORDER_SEARCH_FILTER_1);

        switchShipmentLogisticPointExecutor.doJob(null);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Found 2 partners to switch logistic point: [1002, 1003]")
            .contains("2 active movement segments found")
            .contains("Found 2 partners related with dropoffs to switch logistic point: [1002, 1003]")
            .contains("Found information about orders of 2 shops: {1002=true, 1003=true}")
            .contains("0 dropships without orders found: []");

        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_DROPSHIPS_FILTER);
        verify(lmsClient).searchLogisticSegments(LOGISTIC_SEGMENT_MOVEMENT_FILTER);
        verify(lmsClient).searchPartners(SEARCH_PARTNER_FILTER);
        verify(lomClient).checkOrdersExisting(EXISTING_ORDER_SEARCH_FILTER_1);
        verify(lomClient).checkOrdersExisting(EXISTING_ORDER_SEARCH_FILTER_2);
    }

    @Test
    @DisplayName("Не найдено ни одного магазина")
    void shopsNotFound() {
        clock.setFixed(Instant.parse("2020-06-10T14:00:00.00Z"), ZoneId.systemDefault());
        switchShipmentLogisticPointExecutor.doJob(null);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Found 0 partners to switch logistic point: []");
    }
}
