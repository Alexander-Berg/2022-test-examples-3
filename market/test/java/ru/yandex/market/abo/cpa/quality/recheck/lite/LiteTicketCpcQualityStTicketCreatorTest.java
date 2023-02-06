package ru.yandex.market.abo.cpa.quality.recheck.lite;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;

import ru.yandex.market.abo.core.startrek.StartrekTicketManager;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason;
import ru.yandex.market.abo.core.ticket.ProblemManager;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 13.05.2020
 */
class LiteTicketCpcQualityStTicketCreatorTest {

    private static final long TICKET_ID = 123L;

    @InjectMocks
    private LiteTicketCpcQualityStTicketCreator liteTicketCpcQualityStTicketCreator;

    @Mock
    private StartrekTicketManager startrekTicketManager;
    @Mock
    private LiteTicketCpcQualityRepo liteTicketCpcQualityRepo;
    @Mock
    private ProblemManager problemManager;
    @Mock
    private RecheckTicketService recheckTicketService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        var liteTicketCpcQuality = mock(LiteTicketCpcQuality.class);
        when(liteTicketCpcQuality.getTicketId()).thenReturn(TICKET_ID);
        when(liteTicketCpcQuality.getTicketApproveTime()).thenReturn(LocalDateTime.now().minusDays(10));
        when(liteTicketCpcQuality.getCutoffStartDate()).thenReturn(LocalDateTime.now().minusDays(9));

        when(liteTicketCpcQualityRepo.findAllByCutoffStartDateAfter(any())).thenReturn(List.of(liteTicketCpcQuality));

        var page = mock(Page.class);
        when(page.getContent()).thenReturn(Collections.emptyList());

        when(problemManager.load(any(), any())).thenReturn(page);

        when(recheckTicketService.findAll(any())).thenReturn(Collections.emptyList());
    }

    @ParameterizedTest
    @CsvSource({"false, 0", "true, 1"})
    void createStTicketsTest(boolean hasNoStTickets, int expectedTicketsCount) {
        when(startrekTicketManager.hasNoTickets(TICKET_ID, StartrekTicketReason.LITE_TICKET_CPC_QUALITY))
                .thenReturn(hasNoStTickets);
        liteTicketCpcQualityStTicketCreator.createTickets();

        verify(startrekTicketManager, times(expectedTicketsCount)).createTicket(any());
    }
}
