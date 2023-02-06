package ru.yandex.market.logistics.utilizer.service.cycle;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.utilizer.base.SoftAssertionSupport;
import ru.yandex.market.logistics.utilizer.domain.entity.UtilizationCycle;
import ru.yandex.market.logistics.utilizer.domain.enums.UtilizationCycleStatus;
import ru.yandex.market.logistics.utilizer.repo.StockJpaRepository;
import ru.yandex.market.logistics.utilizer.repo.UtilizationCycleJpaRepository;
import ru.yandex.market.logistics.utilizer.service.StocksForUtilizationService;
import ru.yandex.market.logistics.utilizer.service.system.SystemPropertyService;
import ru.yandex.market.logistics.utilizer.service.system.keys.SystemPropertyIntegerKey;
import ru.yandex.market.logistics.utilizer.service.time.DateTimeService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UtilizationCycleServiceTest extends SoftAssertionSupport {

    private static final int DAYS_BEFORE_TRANSFER_CREATION_STUB = 32;
    public static final LocalDateTime MESSAGE_SENT_AT = LocalDateTime.of(2020, 12, 14, 17, 0);

    private StocksForUtilizationService stocksForUtilizationService;
    private StockJpaRepository stockRepository;
    private UtilizationCycleJpaRepository utilizationCycleRepository;
    private SystemPropertyService systemPropertyService;
    private DateTimeService dateTimeService;
    private UtilizationCycleService utilizationCycleService;

    @BeforeEach
    public void init() {
        stocksForUtilizationService = mock(StocksForUtilizationService.class);
        stockRepository = mock(StockJpaRepository.class);
        utilizationCycleRepository = mock(UtilizationCycleJpaRepository.class);
        systemPropertyService = mock(SystemPropertyService.class);
        dateTimeService = mock(DateTimeService.class);

        utilizationCycleService = new UtilizationCycleService(
                stocksForUtilizationService,
                stockRepository,
                utilizationCycleRepository,
                systemPropertyService,
                dateTimeService
        );
    }

    @Test
    void getUtilizationDateForCreated() {
        LocalDate now = LocalDate.of(2021, 1, 14);
        when(dateTimeService.localDateNow()).thenReturn(now);
        when(systemPropertyService.getProperty(SystemPropertyIntegerKey.DAYS_BEFORE_TRANSFER_CREATION))
                .thenReturn(DAYS_BEFORE_TRANSFER_CREATION_STUB);

        UtilizationCycle utilizationCycle = createUtilizationCycle(UtilizationCycleStatus.CREATED, null);
        LocalDate actual = utilizationCycleService.getUtilizationDate(utilizationCycle);
        LocalDate expected = now.plusDays(DAYS_BEFORE_TRANSFER_CREATION_STUB);

        softly.assertThat(actual).isEqualTo(expected);
        verify(systemPropertyService).getProperty(SystemPropertyIntegerKey.DAYS_BEFORE_TRANSFER_CREATION);
        verify(dateTimeService).localDateNow();
    }

    @Test
    void getUtilizationDateForFinalized() {
        when(systemPropertyService.getProperty(SystemPropertyIntegerKey.DAYS_BEFORE_TRANSFER_CREATION))
                .thenReturn(DAYS_BEFORE_TRANSFER_CREATION_STUB);

        UtilizationCycle utilizationCycle = createUtilizationCycle(UtilizationCycleStatus.FINALIZED, MESSAGE_SENT_AT);
        LocalDate actual = utilizationCycleService.getUtilizationDate(utilizationCycle);
        LocalDate expected =
                utilizationCycle.getMessageSentAt().toLocalDate().plusDays(DAYS_BEFORE_TRANSFER_CREATION_STUB);

        softly.assertThat(actual).isEqualTo(expected);
        verify(systemPropertyService).getProperty(SystemPropertyIntegerKey.DAYS_BEFORE_TRANSFER_CREATION);
        verifyNoMoreInteractions(dateTimeService);
    }

    @Test
    void getUtilizationDateForTransferred() {
        when(systemPropertyService.getProperty(SystemPropertyIntegerKey.DAYS_BEFORE_TRANSFER_CREATION))
                .thenReturn(DAYS_BEFORE_TRANSFER_CREATION_STUB);

        UtilizationCycle utilizationCycle = createUtilizationCycle(UtilizationCycleStatus.TRANSFERRED, MESSAGE_SENT_AT);
        LocalDate actual = utilizationCycleService.getUtilizationDate(utilizationCycle);
        LocalDate expected = utilizationCycle.getMessageSentAt().toLocalDate().plusDays(
                DAYS_BEFORE_TRANSFER_CREATION_STUB);

        softly.assertThat(actual).isEqualTo(expected);
        verify(systemPropertyService).getProperty(SystemPropertyIntegerKey.DAYS_BEFORE_TRANSFER_CREATION);
        verifyNoMoreInteractions(dateTimeService);
    }

    @Test
    void getUtilizationDateForFinalizedWithoutMessageSentAt() {
        when(systemPropertyService.getProperty(SystemPropertyIntegerKey.DAYS_BEFORE_TRANSFER_CREATION))
                .thenReturn(DAYS_BEFORE_TRANSFER_CREATION_STUB);

        UtilizationCycle utilizationCycle = createUtilizationCycle(UtilizationCycleStatus.FINALIZED, null);

        assertThrows(IllegalArgumentException.class,
                () -> utilizationCycleService.getUtilizationDate(utilizationCycle),
                "MessageSentAt is empty for cycle id=1 in FINALIZED status"
        );

        verify(systemPropertyService).getProperty(SystemPropertyIntegerKey.DAYS_BEFORE_TRANSFER_CREATION);
        verifyNoMoreInteractions(dateTimeService);
    }

    private UtilizationCycle createUtilizationCycle(UtilizationCycleStatus status, LocalDateTime messageSentAt) {
        return UtilizationCycle.builder()
                .id(1L)
                .messageSentAt(messageSentAt)
                .status(status)
                .build();
    }
}
