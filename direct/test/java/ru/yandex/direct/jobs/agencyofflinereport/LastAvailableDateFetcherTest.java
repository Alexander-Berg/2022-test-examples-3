package ru.yandex.direct.jobs.agencyofflinereport;


import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportParametersService;
import ru.yandex.direct.juggler.check.JugglerNumericEventsClient;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.jobs.agencyofflinereport.LastAvailableDateFetcher.getPathForDate;

class LastAvailableDateFetcherTest {

    private static final LocalDate BASE = LocalDate.of(2018, 8, 24);
    private static final int MAX_STEPS = 3;

    private final Set<YPath> existsTables = new HashSet<>();

    private AgencyOfflineReportParametersService parametersService;
    private LastAvailableDateFetcher lastAvailableDateFetcher;


    @BeforeEach
    void initMocks() {
        Cypress cypress = mock(Cypress.class);
        when(cypress.exists(any(YPath.class))).thenAnswer(a -> existsTables.contains((a.<YPath>getArgument(0))));

        Yt yt = mock(Yt.class);
        when(yt.cypress()).thenReturn(cypress);

        YtProvider ytProvider = mock(YtProvider.class);
        when(ytProvider.get(any())).thenReturn(yt);

        parametersService = mock(AgencyOfflineReportParametersService.class);
        when(parametersService.getMaximumAvailableDate()).thenReturn(BASE);

        lastAvailableDateFetcher = new LastAvailableDateFetcher(parametersService,
                mock(JugglerNumericEventsClient.class),
                mock(LastAvailableDateCheck.class),
                ytProvider,
                MAX_STEPS);
    }

    @Test
    void searchLastDate_NoTables_ReturnsNull() {
        assertNull(lastAvailableDateFetcher.searchLastDate(BASE));
    }

    @Test
    void searchLastDate_CurrentTableExists_ReturnsCurrent() {
        mockTableExists(BASE);
        assertEquals(BASE, lastAvailableDateFetcher.searchLastDate(BASE));
    }

    @Test
    void searchLastDate_PreviousTableExists_ReturnsPrevious() {
        LocalDate expected = BASE.minusDays(1);
        mockTableExists(expected);
        assertEquals(expected, lastAvailableDateFetcher.searchLastDate(BASE));
    }

    @Test
    void searchLastDate_TwoPreviousTablesExists_ReturnsFirst() {
        LocalDate expected = BASE.minusDays(1);
        mockTableExists(expected);
        mockTableExists(expected.minusDays(1));
        assertEquals(expected, lastAvailableDateFetcher.searchLastDate(BASE));
    }

    @Test
    void searchLastDate_BorderTableInPastExists_ReturnsBorder() {
        LocalDate expected = BASE.minusDays(MAX_STEPS);
        mockTableExists(expected);
        assertEquals(expected, lastAvailableDateFetcher.searchLastDate(BASE));
    }

    @Test
    void searchLastDate_TableBeforeBorderInPastExists_ReturnsNull() {
        LocalDate expected = BASE.minusDays(MAX_STEPS + 1);
        mockTableExists(expected);
        assertNull(lastAvailableDateFetcher.searchLastDate(BASE));
    }

    @Test
    void searchLastDate_CurrentAndFutureTableExists_ReturnsNext() {
        LocalDate expected = BASE.plusDays(1);
        mockTableExists(BASE);
        mockTableExists(expected);
        assertEquals(expected, lastAvailableDateFetcher.searchLastDate(BASE));
    }

    @Test
    void searchLastDate_AllFutureTablesExists_ReturnsBorder() {
        LocalDate expected = BASE.plusDays(MAX_STEPS);
        for (int i = 0; i < MAX_STEPS * 2; i++) {
            mockTableExists(BASE.plusDays(i));
        }
        assertEquals(expected, lastAvailableDateFetcher.searchLastDate(BASE));
    }

    @Test
    void execute_NoTables_NoPropertyUpdate() {
        lastAvailableDateFetcher.execute();
        verify(parametersService, never()).setMaximumAvailableDate(any());
    }

    @Test
    void execute_CurrentTableExists_NoPropertyUpdate() {
        mockTableExists(BASE);
        lastAvailableDateFetcher.execute();
        verify(parametersService, never()).setMaximumAvailableDate(any());
    }

    @Test
    void execute_PreviousTableExists_PropertyUpdatedToPrevious() {
        LocalDate expected = BASE.minusDays(1);
        mockTableExists(expected);
        lastAvailableDateFetcher.execute();
        verify(parametersService).setMaximumAvailableDate(eq(expected));
    }

    @Test
    void execute_CurrentAndFutureTableExists_PropertyUpdatedToFuture() {
        LocalDate expected = BASE.plusDays(1);
        mockTableExists(BASE);
        mockTableExists(expected);
        lastAvailableDateFetcher.execute();
        verify(parametersService).setMaximumAvailableDate(eq(expected));
    }

    private void mockTableExists(LocalDate date) {
        existsTables.add(getPathForDate(date));
    }
}
