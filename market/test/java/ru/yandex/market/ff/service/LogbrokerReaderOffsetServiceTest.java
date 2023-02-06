package ru.yandex.market.ff.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.repository.LogbrokerReaderOffsetRepository;
import ru.yandex.market.ff.service.implementation.LogbrokerReaderOffsetServiceImpl;

/**
 * Unit-тесты для {@link LogbrokerReaderOffsetService}.
 */
public class LogbrokerReaderOffsetServiceTest {
    private static final int FIVE_MINUTE = 5;

    private LogbrokerReaderOffsetRepository logbrokerReaderOffsetRepository;
    private LogbrokerReaderOffsetService logbrokerReaderOffsetService;
    private SoftAssertions assertions;
    private DateTimeService dateTimeService;

    @BeforeEach
    public void init() {
        logbrokerReaderOffsetRepository = Mockito.mock(LogbrokerReaderOffsetRepository.class);
        dateTimeService = Mockito.mock(DateTimeService.class);
        logbrokerReaderOffsetService = new LogbrokerReaderOffsetServiceImpl(logbrokerReaderOffsetRepository,
                dateTimeService);
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void reset() {
        assertions.assertAll();
        Mockito.reset(logbrokerReaderOffsetRepository, dateTimeService);
    }

    @Test
    public void getLastUpdatedTimeWorksCorrect() {
        Optional<LocalDateTime> lastUpdatedForCheckouter = Optional.of(LocalDateTime.of(2019, 9, 11, 11, 11));
        Optional<LocalDateTime> lastUpdatedForOther = Optional.of(LocalDateTime.of(2019, 11, 11, 11, 11));
        Mockito.when(logbrokerReaderOffsetRepository.getLastUpdateTime("checkouter_event"))
                .thenReturn(lastUpdatedForCheckouter);
        Mockito.when(logbrokerReaderOffsetRepository.getLastUpdateTime("other_event"))
                .thenReturn(lastUpdatedForOther);
        Optional<LocalDateTime> actualResult = logbrokerReaderOffsetService.getLastUpdateTime("checkouter_event");
        assertions.assertThat(actualResult).isEqualTo(lastUpdatedForCheckouter);
        Mockito.verify(logbrokerReaderOffsetRepository).getLastUpdateTime("checkouter_event");
    }

    @Test
    void shouldGotOk() {
        LocalDateTime lastUpdated = LocalDateTime.of(2019, 11, 11, 11, 11, 11);
        Mockito.when(logbrokerReaderOffsetRepository.getLastUpdateTime("checkouter_event"))
                .thenReturn(Optional.<LocalDateTime>of(lastUpdated));
        Mockito.when(dateTimeService.localDateTimeNow())
                .thenReturn(lastUpdated.plusMinutes(FIVE_MINUTE).minusSeconds(1));
        Assertions.assertThat(logbrokerReaderOffsetService.checkLogbrokerReading("checkouter_event", FIVE_MINUTE))
                .isEqualTo("0;ok");
    }

    @Test
    void shouldGotError() {
        LocalDateTime lastUpdated = LocalDateTime.of(2019, 11, 11, 11, 11, 11);
        Mockito.when(logbrokerReaderOffsetRepository.getLastUpdateTime("checkouter_event"))
                .thenReturn(Optional.<LocalDateTime>of(lastUpdated));
        Mockito.when(dateTimeService.localDateTimeNow())
                .thenReturn(lastUpdated.plusMinutes(FIVE_MINUTE).plusSeconds(1));

        Assertions.assertThat(logbrokerReaderOffsetService.checkLogbrokerReading("checkouter_event", FIVE_MINUTE))
                .isEqualTo("2;Last successful reading from Logbroker for checkouter_event was at 2019-11-11T11:11:11," +
                        " more than 5 minutes ago");
    }

}
