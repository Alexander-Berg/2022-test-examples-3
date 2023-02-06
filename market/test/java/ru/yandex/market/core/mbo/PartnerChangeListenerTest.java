package ru.yandex.market.core.mbo;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.core.task.SyncTaskExecutor;

import ru.yandex.common.transaction.LocalTransactionListener;
import ru.yandex.market.core.mbo.model.PartnerChangeEvent;
import ru.yandex.market.core.mbo.model.PartnerChangeLogbrokerEvent;
import ru.yandex.market.core.mbo.model.PartnerChangeRecord;
import ru.yandex.market.logbroker.LogbrokerService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerChangeListenerTest {
    @Mock
    PartnerChangeDao dao;

    @Mock
    LogbrokerService logbrokerService;

    Executor executor = new SyncTaskExecutor();
    PartnerChangeRecord.UpdateType updateType = PartnerChangeRecord.UpdateType.SERVICE_LINK;
    PartnerChangeListener listener;

    @BeforeEach
    void setUp() {
        when(dao.getEventForPartner(any(), any())).thenAnswer(invocation -> {
            var parterIds = (Set<Long>) invocation.getArgument(0);
            return parterIds.stream()
                    .map(id -> PartnerChangeRecord.newBuilder()
                            .withId(id)
                            .withName(id.toString())
                            .withType(PartnerChangeRecord.PartnerType.SUPPLIER)
                            .withUpdateType(updateType)
                            .build())
                    .collect(Collectors.toList());
        });
    }

    @AfterEach
    void tearDown() {
        Optional.ofNullable(listener).ifPresent(PartnerChangeListener::close);
    }

    @Test
    void onApplicationEventNoBuffer() {
        // given
        var eventsCount = 5;
        var partnerIds = LongStream.range(0L, eventsCount).boxed().collect(Collectors.toSet());
        initListener(Duration.ZERO);

        // when
        partnerIds.forEach(i -> listener.onApplicationEvent(new PartnerChangeEvent(i, updateType, null)));

        // then
        verify(dao, times(eventsCount)).getEventForPartner(any(), eq(updateType));
        verifyNoMoreInteractions(dao);
        verifyEventsFired(eventsCount, partnerIds);
    }

    @Test
    void onApplicationEventBuffered() throws InterruptedException {
        // given
        var eventsCount = 5;
        var partnerIds = LongStream.range(0L, eventsCount).boxed().collect(Collectors.toSet());
        initListener(Duration.ofMillis(250L));

        // when
        partnerIds.forEach(i -> listener.onApplicationEvent(new PartnerChangeEvent(i, updateType, null)));
        TimeUnit.MILLISECONDS.sleep(500L);

        // then
        verify(dao, only()).getEventForPartner(partnerIds, updateType);
        verifyEventsFired(eventsCount, partnerIds);
    }

    private void initListener(Duration bufferFlushInterval) {
        var writer = new LogbrokerPartnerChangeEventsWriter(logbrokerService);
        listener = new PartnerChangeListener(dao, writer, executor, bufferFlushInterval,
                new LocalTransactionListener());
        listener.init();
    }

    private void verifyEventsFired(int eventsCount, Collection<Long> partnerIds) {
        var logbrokerEventCaptor = ArgumentCaptor.forClass(PartnerChangeLogbrokerEvent.class);
        verify(logbrokerService, times(eventsCount)).publishEvent(logbrokerEventCaptor.capture());
        verifyNoMoreInteractions(logbrokerService);
        assertThat(logbrokerEventCaptor.getAllValues().stream().map(i -> i.getPayload().getId()))
                .containsExactlyInAnyOrder(partnerIds.toArray(Long[]::new));
    }
}
