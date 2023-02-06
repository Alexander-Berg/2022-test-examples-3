package ru.yandex.market.core.state;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.protobuf.Timestamp;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.core.task.SyncTaskExecutor;

import ru.yandex.common.transaction.LocalTransactionListener;
import ru.yandex.market.core.state.event.BusinessChangesProtoLBEvent;
import ru.yandex.market.core.state.writer.BusinessDataChangesToLBWriter;
import ru.yandex.market.core.state.writer.DataChangesToLBWriter;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.data.BusinessDataOuterClass;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.data.outer.BusinessDataOuterService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataChangesEventListenerTest {
    private final Executor executor = new SyncTaskExecutor();
    @Mock
    private BusinessDataOuterService businessDataOuterService;
    @Mock
    private LogbrokerEventPublisher<BusinessChangesProtoLBEvent> logbrokerBusinessEventPublisher;

    private DataChangesEventListener listener;
    private Map<DataChangesEvent.PartnerDataType, DataChangesToLBWriter> eventsTypeToLbWriter;

    @BeforeEach
    void setUp() {
        when(businessDataOuterService.getBusinessDataForExport(anyLong(), any(Instant.class),
                any()))
                .thenAnswer(invocation -> {
                    var businessId = (Long) invocation.getArgument(0);
                    var timeInstant = (Instant) invocation.getArgument(1);
                    var operation = (DataChangesEvent.PartnerDataOperation) invocation.getArgument(2);
                    return prepareBusinessData(businessId, timeInstant, operation);
                });
        when(businessDataOuterService.getBusinessDataForExport(anyMap(), any(Instant.class),
                any()))
                .thenAnswer(invocation -> {
                    var businessId = (Long) invocation.getArgument(0);
                    var timeInstant = (Instant) invocation.getArgument(1);
                    var operation = (DataChangesEvent.PartnerDataOperation) invocation.getArgument(2);
                    return Map.of(businessId, prepareBusinessData(businessId, timeInstant, operation));
                });
        BusinessDataChangesToLBWriter businessDataChangesToLBWriter =
                new BusinessDataChangesToLBWriter(logbrokerBusinessEventPublisher, businessDataOuterService);
        eventsTypeToLbWriter =
                Map.of(DataChangesEvent.PartnerDataType.BUSINESS_DATA, businessDataChangesToLBWriter);
    }

    private BusinessDataOuterClass.BusinessData prepareBusinessData(Long businessId, Instant timeInstant,
                                                                    DataChangesEvent.PartnerDataOperation operation) {
        return BusinessDataOuterClass.BusinessData.newBuilder()
                .setGeneralInfo(GeneralData.GeneralDataInfo.newBuilder()
                        .setActionType(GeneralData.ActionType.valueOf(operation.name()))
                        .setUpdatedAt(Timestamp.newBuilder()
                                .setSeconds(timeInstant.getEpochSecond())
                                .setNanos(timeInstant.getNano())
                                .build())
                        .build())
                .setName("business name")
                .setBusinessId(businessId)
                .addAllPartnerIds(List.of(123L))
                .build();
    }

    @AfterEach
    void tearDown() {
        Optional.ofNullable(listener).ifPresent(DataChangesEventListener::close);
    }

    @Test
    void onApplicationEventNoBuffer() {
        initListener(Duration.ZERO);
        var eventsCount = 5;
        var businessIds = LongStream.range(0L, eventsCount).boxed().collect(Collectors.toSet());

        businessIds.forEach(businessId ->
                listener.onApplicationEvent(new DataChangesEvent(Instant.ofEpochSecond(1111111L), businessId,
                        DataChangesEvent.PartnerDataType.BUSINESS_DATA,
                        DataChangesEvent.PartnerDataOperation.UPDATE)));

        verify(businessDataOuterService, times(eventsCount)).getBusinessDataForExport(anyLong(), any(Instant.class),
                any(DataChangesEvent.PartnerDataOperation.class));
        verifyNoMoreInteractions(businessDataOuterService);
        verifyEventsFired(eventsCount, businessIds);
    }

    @Test
    void onApplicationEventBuffered() throws InterruptedException {
        initListener(Duration.ofMillis(1L));
        var eventsCount = 2;
        var businessIds = LongStream.range(0L, eventsCount).boxed().collect(Collectors.toSet());

        businessIds.forEach(businessId ->
                listener.onApplicationEvent(new DataChangesEvent(Instant.ofEpochSecond(1111111L), businessId,
                        DataChangesEvent.PartnerDataType.BUSINESS_DATA,
                        DataChangesEvent.PartnerDataOperation.UPDATE)));

        TimeUnit.MILLISECONDS.sleep(500L);

        verify(businessDataOuterService).getBusinessDataForExport(eq(0L), any(Instant.class),
                eq(DataChangesEvent.PartnerDataOperation.UPDATE));
        verify(businessDataOuterService).getBusinessDataForExport(eq(1L), any(Instant.class),
                eq(DataChangesEvent.PartnerDataOperation.UPDATE));
        verifyEventsFired(eventsCount, businessIds);
    }

    private void initListener(Duration bufferFlushInterval) {
        listener = new DataChangesEventListener(eventsTypeToLbWriter, executor, bufferFlushInterval,
                new LocalTransactionListener());
        listener.init();
    }

    private void verifyEventsFired(int eventsCount, Collection<Long> businessIds) {
        var logbrokerEventCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessEventPublisher, times(eventsCount)).publishEventAsync(logbrokerEventCaptor.capture());
        verifyNoMoreInteractions(logbrokerBusinessEventPublisher);
        List<BusinessChangesProtoLBEvent> allValues = logbrokerEventCaptor.getAllValues();
        assertThat(allValues.stream().map(i -> i.getPayload().getBusinessId()))
                .containsExactlyInAnyOrder(businessIds.toArray(Long[]::new));

        assertThat(allValues.stream().map(i -> i.getPayload().getName()))
                .allMatch(name -> name.equals("business name"));

        assertThat(allValues.stream().map(i -> i.getPayload().getPartnerIds(0)))
                .allMatch(partner -> partner == 123L);

    }

}
