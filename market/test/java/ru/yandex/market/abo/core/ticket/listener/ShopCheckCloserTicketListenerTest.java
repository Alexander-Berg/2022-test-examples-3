package ru.yandex.market.abo.core.ticket.listener;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.check.ShopCheckManager;
import ru.yandex.market.abo.check.model.ShopCheckStatus;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemType;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.ticket.ProblemService;
import ru.yandex.market.abo.core.ticket.ProblemTypeService;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketStatus;
import ru.yandex.market.abo.gen.HypothesisService;
import ru.yandex.market.abo.gen.model.GenId;
import ru.yandex.market.abo.gen.model.Hypothesis;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.ticket.listener.ShopCheckCloserTicketListener.ACCEPTED_GEN_IDS;

/**
 * @author komarovns
 * @date 12.03.19
 */
class ShopCheckCloserTicketListenerTest {
    @InjectMocks
    ShopCheckCloserTicketListener shopCheckCloserTicketListener;

    @Mock
    HypothesisService hypothesisService;
    @Mock
    ShopCheckManager shopCheckManager;
    @Mock
    ProblemService problemService;
    @Mock
    ProblemTypeService problemTypeService;
    @Mock
    ShopInfoService shopInfoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(problemService.loadProblemsByTicketId(anyLong()))
                .thenReturn(Collections.singletonList(mock(Problem.class)));
    }

    @Test
    void ticketFinishedWithoutProblemsRecheckGenTest() {
        when(hypothesisService.loadHypothesis(anyLong())).thenReturn(createHypothesis(GenId.RECHECK));
        when(problemService.loadProblemsByTicketId(anyLong())).thenReturn(Collections.emptyList());

        shopCheckCloserTicketListener.notify(createTicket(TicketStatus.FINISHED));

        verify(shopCheckManager).updateCheckStatus(anyLong(), eq(ShopCheckStatus.FINISHED), anyLong());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void ticketFinishedWithProblemsRecheckGenTest(boolean critical) {
        when(hypothesisService.loadHypothesis(anyLong())).thenReturn(createHypothesis(GenId.RECHECK));
        when(problemTypeService.getProblemType(anyInt())).thenReturn(createProblemType(critical));

        shopCheckCloserTicketListener.notify(createTicket(TicketStatus.FINISHED));

        int updateCount = critical ? 0 : 1;
        verify(shopCheckManager, times(updateCount))
                .updateCheckStatus(anyLong(), eq(ShopCheckStatus.FINISHED), anyLong());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void ticketFinishedPostModerationGenTest(boolean critical) {
        when(hypothesisService.loadHypothesis(anyLong())).thenReturn(createHypothesis(GenId.POSTMODERATION));
        when(problemTypeService.getProblemType(anyInt())).thenReturn(createProblemType(critical));

        shopCheckCloserTicketListener.notify(createTicket(TicketStatus.FINISHED));

        verify(shopCheckManager).updateCheckStatus(anyLong(), eq(ShopCheckStatus.FINISHED), anyLong());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void ticketCancelledTest(boolean isActive) {
        ACCEPTED_GEN_IDS.forEach(genId -> {
            when(hypothesisService.loadHypothesis(anyLong())).thenReturn(createHypothesis(genId));
            when(shopInfoService.isActiveAndWithoutQualityCutoffs(anyLong())).thenReturn(isActive);

            shopCheckCloserTicketListener.notify(createTicket(TicketStatus.CANCELED));
        });

        int updateCount = isActive ? 0 : ACCEPTED_GEN_IDS.size();
        verify(shopCheckManager, times(updateCount))
                .updateCheckStatus(anyLong(), eq(ShopCheckStatus.CANCELLED), anyLong());
    }

    private static Ticket createTicket(TicketStatus status) {
        Ticket ticket = new Ticket(createHypothesis(0), null, 0, CheckMethod.BASKET);
        ticket.setId(0L);
        ticket.setStatus(status);
        return ticket;
    }

    private static Hypothesis createHypothesis(int genId) {
        return new Hypothesis(0, 0, genId, null, 0, 1, null);
    }

    private static ProblemType createProblemType(boolean critical) {
        ProblemType type = new ProblemType(0);
        type.setCritical(critical);
        return type;
    }
}
