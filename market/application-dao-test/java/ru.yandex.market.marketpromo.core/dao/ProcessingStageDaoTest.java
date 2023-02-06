package ru.yandex.market.marketpromo.core.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.application.context.CategoryInterfacePromo;
import ru.yandex.market.marketpromo.core.dao.internal.ProcessingStageDao;
import ru.yandex.market.marketpromo.core.data.processing.JsonProcessingStage;
import ru.yandex.market.marketpromo.core.data.processing.result.PublishingRequest;
import ru.yandex.market.marketpromo.core.service.processor.ExportProcessor;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.filter.PromoFilter;
import ru.yandex.market.marketpromo.filter.PromoRequest;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.PromoKey;
import ru.yandex.market.marketpromo.model.processing.ProcessingRequestStatus;
import ru.yandex.market.marketpromo.model.processing.ProcessingRequestType;
import ru.yandex.market.marketpromo.model.processing.PublishingStatus;
import ru.yandex.market.marketpromo.processing.ProcessId;
import ru.yandex.market.marketpromo.processing.ProcessingTask;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.marketpromo.model.processing.ProcessingRequestStatus.IN_QUEUE;
import static ru.yandex.market.marketpromo.model.processing.ProcessingRequestStatus.PROCESSING;
import static ru.yandex.market.marketpromo.model.processing.ProcessingRequestType.EXPORT_ASSORTMENT;

public class ProcessingStageDaoTest extends ServiceTestBase {

    private static final long SHOP_ID = 12L;
    private static final String PROMO = "some promo";
    private static final String SSKU_1 = "ssku-123";
    private static final PromoKey PROMO_KEY = PromoKey.of(UUID.randomUUID().toString(), MechanicsType.DIRECT_DISCOUNT);

    @Autowired
    private ProcessingStageDao processingRequestDao;
    @Autowired
    private ExportProcessor exportProcessor;

    @Autowired
    @CategoryInterfacePromo
    private ObjectMapper objectMapper;

    @Test
    void shouldInsertNewTask() {
        final ProcessId processId = ProcessId.of(EXPORT_ASSORTMENT, UUID.randomUUID().toString());
        final ProcessingTask<ProcessingRequestStatus> task = processingRequestDao.initStage(
                processId,
                JsonProcessingStage.<ProcessingRequestStatus>builder()
                        .state(IN_QUEUE)
                        .data(AssortmentRequest.builder(PROMO_KEY)
                                .build())
                        .build(),
                (pid, stage) -> {
                    throw new RuntimeException();
                }
        );

        assertThat(task, allOf(
                hasProperty("processId", is(processId)),
                hasProperty("updatedAt"),
                hasProperty("stage", hasProperty("data", allOf(
                        hasProperty("promoKey", is(PROMO_KEY)),
                        hasProperty("filterValues", not(empty()))
                )))
        ));
    }

    @Test
    void shouldInsertDifferentTypeTask() {
        final String sameToken = "some token";
        final ProcessId processId1 = ProcessId.of(EXPORT_ASSORTMENT, sameToken);
        final ProcessId processId2 = ProcessId.of(ProcessingRequestType.EXPORT_PROMO, sameToken);
        final ProcessId processId3 = ProcessId.of(ProcessingRequestType.PUBLISH_ASSORTMENT, sameToken);

        final ProcessingTask<ProcessingRequestStatus> task1 = processingRequestDao.initStage(
                processId1,
                JsonProcessingStage.<ProcessingRequestStatus>builder()
                        .state(IN_QUEUE)
                        .data(AssortmentRequest.builder(PROMO_KEY)
                                .build())
                        .build(),
                (pid, stage) -> {
                    throw new RuntimeException();
                }
        );

        final ProcessingTask<ProcessingRequestStatus> task2 = processingRequestDao.initStage(
                processId2,
                JsonProcessingStage.<ProcessingRequestStatus>builder()
                        .state(IN_QUEUE)
                        .data(PromoRequest.builder()
                                .filter(PromoFilter.ANAPLAN_ID, PROMO)
                                .build())
                        .build(),
                (pid, stage) -> {
                    throw new RuntimeException();
                }
        );

        final ProcessingTask<PublishingStatus> task3 = processingRequestDao.initStage(
                processId3,
                JsonProcessingStage.<PublishingStatus>builder()
                        .state(PublishingStatus.PUBLISHING)
                        .data(PublishingRequest.builder()
                                .promoKey(PROMO_KEY)
                                .createdAt(clock.dateTime())
                                .batches(Map.of(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID),
                                        Set.of(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))))
                                .build())
                        .build(),
                (pid, stage) -> {
                    throw new RuntimeException();
                }
        );

        assertThat(task1, notNullValue());
        assertThat(task2, notNullValue());
        assertThat(task3, notNullValue());
    }

    @Test
    void shouldReturnTaskByProcessId() {
        final ProcessId processId = ProcessId.of(EXPORT_ASSORTMENT, UUID.randomUUID().toString());
        processingRequestDao.initStage(
                processId,
                JsonProcessingStage.<ProcessingRequestStatus>builder()
                        .state(IN_QUEUE)
                        .data(AssortmentRequest.builder(PROMO_KEY)
                                .build())
                        .build(),
                (pid, stage) -> {
                    throw new RuntimeException();
                }
        );

        ProcessingTask<ProcessingRequestStatus> task = processingRequestDao.selectOne(processId,
                exportProcessor::mapStageForAssortmentExport).orElseThrow();

        assertThat(task, notNullValue());
        assertThat(task.getProcessId(), is(processId));
        assertThat(task.getStage(), notNullValue());
        assertThat(task.getStage().getState(), is(IN_QUEUE));
        assertThat(task.getStage().getDataAs(AssortmentRequest.class), notNullValue());
    }

    @Test
    void shouldReturnTaskListByTypeAndStatus() {
        processingRequestDao.initStage(
                ProcessId.of(EXPORT_ASSORTMENT, UUID.randomUUID().toString()),
                JsonProcessingStage.<ProcessingRequestStatus>builder()
                        .state(IN_QUEUE)
                        .data(AssortmentRequest.builder(PROMO_KEY)
                                .build())
                        .build(),
                (pid, stage) -> {
                    throw new RuntimeException();
                }
        );

        processingRequestDao.initStage(
                ProcessId.of(EXPORT_ASSORTMENT, UUID.randomUUID().toString()),
                JsonProcessingStage.<ProcessingRequestStatus>builder()
                        .state(PROCESSING)
                        .data(AssortmentRequest.builder(PROMO_KEY)
                                .build())
                        .build(),
                (pid, stage) -> {
                    throw new RuntimeException();
                }
        );

        List<ProcessingTask<ProcessingRequestStatus>> tasks =
                processingRequestDao.select(EXPORT_ASSORTMENT, Set.of(PROCESSING),
                        exportProcessor::mapStageForAssortmentExport, 10);

        assertThat(tasks, notNullValue());
        assertThat(tasks, hasSize(1));
    }

    @Test
    void shouldUpdateTaskStage() {
        final ProcessId processId = ProcessId.of(EXPORT_ASSORTMENT, UUID.randomUUID().toString());
        processingRequestDao.initStage(
                processId,
                JsonProcessingStage.<ProcessingRequestStatus>builder()
                        .state(IN_QUEUE)
                        .data(AssortmentRequest.builder(PROMO_KEY)
                                .build())
                        .build(),
                (pid, stage) -> {
                    throw new RuntimeException();
                }
        );

        processingRequestDao.saveStage(processId, JsonProcessingStage.<ProcessingRequestStatus>builder()
                .state(PROCESSING)
                .data(AssortmentRequest.builder(PROMO_KEY)
                        .build())
                .build());

        ProcessingTask<ProcessingRequestStatus> task = processingRequestDao.selectOne(processId,
                exportProcessor::mapStageForAssortmentExport).orElseThrow();

        assertThat(task, notNullValue());
        assertThat(task.getProcessId(), is(processId));
        assertThat(task.getStage(), notNullValue());
        assertThat(task.getStage().getState(), is(PROCESSING));
        assertThat(task.getStage().getDataAs(AssortmentRequest.class), notNullValue());
    }

    @Test()
    void shouldFailOnCreatingTaskTwice() {
        final ProcessId processId = ProcessId.of(EXPORT_ASSORTMENT, UUID.randomUUID().toString());
        processingRequestDao.initStage(
                processId,
                JsonProcessingStage.<ProcessingRequestStatus>builder()
                        .state(IN_QUEUE)
                        .data(AssortmentRequest.builder(PROMO_KEY)
                                .build())
                        .build(),
                (pid, stage) -> {
                    throw new RuntimeException();
                }
        );

        Assertions.assertThrows(RuntimeException.class, () -> {
            processingRequestDao.initStage(
                    processId,
                    JsonProcessingStage.<ProcessingRequestStatus>builder()
                            .state(IN_QUEUE)
                            .data(AssortmentRequest.builder(PROMO_KEY)
                                    .build())
                            .build(),
                    (pid, stage) -> {
                        throw new RuntimeException();
                    }
            );
        });
    }
}
