package ru.yandex.market.logistics.utilizer.service.transfer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.logistics.utilizer.base.SoftAssertionSupport;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.transfer.CreateTransferDbqueuePayload;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.transfer.CreateTransferDbqueueProducer;
import ru.yandex.market.logistics.utilizer.domain.entity.UtilizationCycle;
import ru.yandex.market.logistics.utilizer.domain.enums.UtilizationCycleStatus;
import ru.yandex.market.logistics.utilizer.repo.UtilizationCycleJpaRepository;
import ru.yandex.market.logistics.utilizer.service.system.NamedSystemPropertyService;
import ru.yandex.market.logistics.utilizer.service.system.SystemPropertyService;
import ru.yandex.market.logistics.utilizer.service.system.keys.SystemPropertyIntegerKey;
import ru.yandex.market.logistics.utilizer.service.time.DateTimeService;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransferTasksCreationServiceUnitTest extends SoftAssertionSupport {

    private TransferTasksCreationService transferTasksCreationService;
    private SystemPropertyService systemPropertyService;
    private DateTimeService dateTimeService;
    private UtilizationCycleJpaRepository utilizationCycleJpaRepository;
    private CreateTransferDbqueueProducer createTransferDbqueueProducer;
    private NamedSystemPropertyService namedSystemPropertyService;

    @BeforeEach
    public void init() {
        systemPropertyService = mock(SystemPropertyService.class);
        dateTimeService = mock(DateTimeService.class);
        utilizationCycleJpaRepository = mock(UtilizationCycleJpaRepository.class);
        createTransferDbqueueProducer = mock(CreateTransferDbqueueProducer.class);
        namedSystemPropertyService = mock(NamedSystemPropertyService.class);

        transferTasksCreationService = new TransferTasksCreationService(
                systemPropertyService,
                dateTimeService,
                utilizationCycleJpaRepository,
                createTransferDbqueueProducer,
                namedSystemPropertyService
        );
    }

    @Test
    public void createUtilizationTransferTasks() {
        LocalDateTime now = LocalDateTime.of(2020, 12, 14, 17, 0);
        when(dateTimeService.localDateTimeNow()).thenReturn(now);
        when(systemPropertyService.getProperty(SystemPropertyIntegerKey.DAYS_BEFORE_TRANSFER_CREATION)).thenReturn(32);
        when(utilizationCycleJpaRepository.findAllByStatusAndMessageSentAtLessThan(
                UtilizationCycleStatus.FINALIZED,
                LocalDateTime.of(2020, 11, 12, 17, 0)
        )).thenReturn(List.of(
                createUtilizationCycle(1, 1),
                createUtilizationCycle(6, 1),
                createUtilizationCycle(7, 2),
                createUtilizationCycle(8, 3)
        ));
        when(namedSystemPropertyService.isTransferCreationEnabled(1)).thenReturn(true);
        when(namedSystemPropertyService.isUtilizationApplicableForVendor(1)).thenReturn(true);
        when(namedSystemPropertyService.isTransferCreationEnabled(2)).thenReturn(false);
        when(namedSystemPropertyService.isUtilizationApplicableForVendor(2)).thenReturn(true);
        when(namedSystemPropertyService.isTransferCreationEnabled(3)).thenReturn(true);
        when(namedSystemPropertyService.isUtilizationApplicableForVendor(3)).thenReturn(false);

        transferTasksCreationService.createUtilizationTransferTasks();

        ArgumentCaptor<EnqueueParams<CreateTransferDbqueuePayload>> payloadCaptor =
                ArgumentCaptor.forClass(EnqueueParams.class);
        verify(createTransferDbqueueProducer, times(2)).enqueue(payloadCaptor.capture());
        List<Long> cycleIdsInPayload = payloadCaptor.getAllValues().stream()
                .map(EnqueueParams::getPayload)
                .map(CreateTransferDbqueuePayload::getUtilizationCycleId)
                .collect(Collectors.toList());
        softly.assertThat(cycleIdsInPayload).containsExactlyInAnyOrder(1L, 6L);
    }

    private UtilizationCycle createUtilizationCycle(long id, long vendorId) {
        return UtilizationCycle.builder().id(id).vendorId(vendorId).build();
    }
}
