package ru.yandex.market.abo.core.startrek;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.abo.core.startrek.model.StartrekTicket;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.startrek.StartrekTicketManager.LOCAL_DATE_INTERVAL;

/**
 * @author artemmz
 * created on 22.03.17.
 */
class StartrekTicketManagerTest {
    @InjectMocks
    private StartrekTicketManager startrekTicketManager;
    @Mock
    private StartrekTicketRepository startrekTicketRepository;
    @Mock
    private StartrekSessionProvider sessionProvider;
    @Mock
    private Session session;
    @Mock
    private Issues issues;
    @Mock
    private ExecutorService pool;
    @Spy
    private RetryTemplate retryTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(sessionProvider.session()).thenReturn(session);
        when(session.issues()).thenReturn(issues);
        TestHelper.mockExecutorService(pool);
    }

    @Test
    void hasNew() {
        when(startrekTicketRepository.findLastTicket(any(), any())).thenReturn(init(new Date()));
        assertFalse(startrekTicketManager.hasNoNewTickets(0, StartrekTicketReason.SHOP_CHANGED_CATEGORIES, 1));
    }

    @Test
    void hasNoNew() {
        when(startrekTicketRepository.findLastTicket(any(), any())).thenReturn(init(new Date(0))).thenReturn(null);
        for (int i = 0; i < 2; i++) {
            assertTrue(startrekTicketManager.hasNoNewTickets(0, StartrekTicketReason.SHOP_CHANGED_CATEGORIES, 1));
        }
    }


    @ParameterizedTest
    @SuppressWarnings("unchecked")
    @CsvSource({"0, 1, 1", "2, -1, 2", "2, 1, 3"})
    void loadTicketsTest(int multiplier, int offset, int batchCount) {
        var issue = mock(Issue.class);
        var iteratorF = (IteratorF<Issue>) mock(IteratorF.class);
        when(issues.find(anyString())).thenReturn(iteratorF);
        when(iteratorF.nextO()).thenReturn(Option.of(issue));
        when(iteratorF.toList()).thenReturn(new ArrayListF<>(List.of(issue)));
        int minusDays = LOCAL_DATE_INTERVAL * multiplier + offset;
        when(issue.getCreatedAt()).thenReturn(Instant.now().minus(Duration.standardDays(minusDays)));

        startrekTicketManager.loadTickets(StartrekQuery.builder().build());
        verify(issues, times(batchCount + 1)).find(anyString());

    }

    private StartrekTicket init(Date crTime) {
        StartrekTicket startrekTicket = new StartrekTicket();
        startrekTicket.setCreationTime(crTime);
        return startrekTicket;
    }
}
