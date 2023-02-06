package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.BusinessProcessStateEntityId;
import ru.yandex.market.logistics.lom.entity.BusinessProcessStateReportData;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.enums.EntityType;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.filter.BusinessProcessStateReportDataFilter;
import ru.yandex.market.logistics.lom.jobs.model.EntityId;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPartnerIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.ydb.BusinessProcessStateStatusHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.service.process.BusinessProcessStateService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static ru.yandex.market.logistics.lom.admin.enums.AdminBusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED;

@DisplayName("Работа с состоянием бизнес-процесса")
class BusinessProcessStateServiceTest extends AbstractContextualYdbTest {
    private static final OrderHistoryEventAuthor AUTHOR = new OrderHistoryEventAuthor()
        .setTvmServiceId(222L)
        .setYandexUid(BigDecimal.ONE);

    @Autowired
    private BusinessProcessStateService businessProcessStateService;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription ydbHistoryTable;

    @Autowired
    private BusinessProcessStateStatusHistoryYdbRepository ydbHistoryRepository;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-08-30T11:12:13.00Z"), clock.getZone());
    }

    @Test
    @DisplayName("Создание нового состояния бизнес-процесса")
    @ExpectedDatabase(
        value = "/service/process/after/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void create() throws Exception {
        OrderIdPartnerIdPayload payload = PayloadFactory.createOrderIdPartnerIdPayload(1L, 2L, 1L);
        payload.setSequenceId(1001L);
        businessProcessStateService.save(
            new BusinessProcessState()
                .setQueueType(QueueType.GET_ORDER_LABEL)
                .setEntityIds(List.of(
                    BusinessProcessStateEntityId.of(EntityType.ORDER, 1L),
                    BusinessProcessStateEntityId.of(EntityType.PARTNER, 2L)
                ))
                .setSequenceId(payload.getSequenceId())
                .setStatus(BusinessProcessStatus.ENQUEUED)
                .setAuthor(AUTHOR)
                .setPayload(objectMapper.writeValueAsString(payload)),
            payload.getRequestId()
        );

        softly.assertThat(
                ydbHistoryRepository.getBusinessProcessStatusHistory(payload.getSequenceId(), Pageable.unpaged())
            )
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setSequenceId(payload.getSequenceId())
                    .setId(1L)
                    .setStatus(BusinessProcessStatus.ENQUEUED)
                    .setCreated(clock.instant())
                    .setRequestId(payload.getRequestId())
            ));
    }

    @Test
    @DisplayName("Обновление состояния бизнес-процесса")
    @DatabaseSetup("/service/process/before/update.xml")
    @ExpectedDatabase(
        value = "/service/process/after/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void update() {
        long sequenceId = 1L;
        businessProcessStateService.update(
            sequenceId,
            QueueType.GET_ORDER_LABEL,
            List.of(EntityId.of(EntityType.ORDER, 1L), EntityId.of(EntityType.PARTNER, 2L)),
            BusinessProcessStatus.QUEUE_TASK_ERROR,
            "Error happened"
        );

        softly.assertThat(ydbHistoryRepository.getBusinessProcessStatusHistory(sequenceId, Pageable.unpaged()))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setSequenceId(sequenceId)
                    .setId(1L)
                    .setStatus(BusinessProcessStatus.QUEUE_TASK_ERROR)
                    .setMessage("Error happened")
                    .setCreated(clock.instant())
            ));
    }

    @Test
    @DisplayName("Получение данных для отчета по бизнес-процессам")
    @DatabaseSetup("/service/process/before/report.xml")
    void getReportData() {
        List<BusinessProcessStateReportData> businessProcesses =
            businessProcessStateService.searchFailedBusinessProcesses(
                BusinessProcessStateReportDataFilter.builder()
                    .orderId(1L)
                    .externalOrderId("777")
                    .orderStatus(OrderStatus.PROCESSING)
                    .partnerId(48L)
                    .partnerType(PartnerType.DELIVERY)
                    .businessProcessStatus(ERROR_RESPONSE_PROCESSING_SUCCEEDED)
                    .shipmentDate(LocalDate.of(2019, 6, 11))
                    .waybillSegmentId(2L)
                    .errorMessage("expected to be here")
                .build(),
            Pageable.unpaged()
        );

        Map<Long, BusinessProcessStateReportData> resultData = businessProcesses.stream()
            .collect(Collectors.toMap(BusinessProcessStateReportData::getId, Function.identity()));

        softly.assertThat(resultData.values())
            .usingElementComparatorOnFields(
                "id",
                "orderId",
                "partnerId",
                "orderStatus",
                "partnerType",
                "businessProcessStatus",
                "waybillSegmentId",
                "errorMessage"
            ).contains(
                new BusinessProcessStateReportData()
                    .setId(2L)
                    .setOrderId(1L)
                    .setExternalOrderId("777")
                    .setOrderStatus(OrderStatus.PROCESSING)
                    .setPartnerId(48L)
                    .setPartnerType(PartnerType.DELIVERY)
                    .setBusinessProcessStatus(ERROR_RESPONSE_PROCESSING_SUCCEEDED)
                    .setErrorMessage("error message expected to be here")
                    .setShipmentDate(LocalDate.of(2019, 6, 11))
                    .setWaybillSegmentId(2L)
            );
    }

    @Test
    @DisplayName("Ошибка обновления состояния бизнес-процесса")
    @DatabaseSetup("/service/process/before/update.xml")
    @ExpectedDatabase(
        value = "/service/process/before/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateError() {
        businessProcessStateService.update(
            0L,
            QueueType.CREATE_ORDER_EXTERNAL,
            List.of(),
            BusinessProcessStatus.QUEUE_TASK_ERROR,
            "Error happened"
        );
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(ydbHistoryTable);
    }
}
