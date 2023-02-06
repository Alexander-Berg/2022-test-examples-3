package ru.yandex.market.logistics.management.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.management.repository.export.dynamic.DynamicLogRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class OldDataCleanerTest {
    @Test
    public void cleanup() {
        final DynamicLogRepository dynamicLogRepository = mock(DynamicLogRepository.class);
        final TestableClock clock = new TestableClock();

        clock.setFixed(
            LocalDateTime.of(2021, 3, 15, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );

        final OldDataCleaner cleaner = new OldDataCleaner(dynamicLogRepository, clock);
        cleaner.cleanup();

        verify(dynamicLogRepository)
            .deleteByValidatedBefore(Mockito.eq(LocalDateTime.of(2020, 12, 15, 10, 0)));
        verifyNoMoreInteractions(dynamicLogRepository);
    }
}
