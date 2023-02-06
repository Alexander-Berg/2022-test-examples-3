package ru.yandex.market.abo.core.ticket.listener.problem;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.forecast.ShopForecast;
import ru.yandex.market.abo.api.entity.problem.ProblemClass;
import ru.yandex.market.abo.core.forecast.ShopForecastManager;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemHistory;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemType;
import ru.yandex.market.abo.core.problem.model.ProblemTypeId;
import ru.yandex.market.abo.core.ticket.ProblemService;
import ru.yandex.market.abo.core.ticket.ProblemTypeService;
import ru.yandex.market.abo.core.ticket.TicketService;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType.MASS_SUSPECTED;

/**
 * @author artemmz
 * @date 30/10/18.
 */
class SuspectMassProblemManagerTest {
    private static final long PROBLEM_ID = 1L;
    private static final long TICKET_ID = 2L;
    private static final long SHOP_ID = 3L;
    @InjectMocks
    SuspectMassProblemManager suspectMassProblemManager;
    @Mock
    ProblemTypeService problemTypeService;
    @Mock
    TicketService ticketService;
    @Mock
    RecheckTicketManager recheckTicketManager;
    @Mock
    ShopForecastManager shopForecastManager;
    @Mock
    ProblemService problemService;

    @Mock
    Problem problem;
    @Mock
    ProblemType problemType;
    @Mock
    ShopForecast shopForecast;
    @Mock
    ProblemHistory pHistory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(problem.getStatus()).thenReturn(ProblemStatus.APPROVED);
        when(problem.getId()).thenReturn(PROBLEM_ID);
        when(problem.getTicketId()).thenReturn(TICKET_ID);

        when(problemTypeService.getProblemType(anyInt())).thenReturn(problemType);
        when(problemType.getProblemClass()).thenReturn(ProblemClass.PRICE);
        when(problemType.getId()).thenReturn(ProblemTypeId.GOOD_NOT_AVAILABLE);

        when(ticketService.getShopId(anyLong())).thenReturn(SHOP_ID);
        when(shopForecastManager.getForecastNonCached(anyLong(), any())).thenReturn(shopForecast);
        when(shopForecast.needSwitchOff()).thenReturn(false);
    }

    @Test
    void massNonFinal() {
        when(problemType.canBeMass()).thenReturn(true);
        suspectMassProblemManager.freezeProblemIfNeeded(problem);

        verify(recheckTicketManager)
                .addTicketIfNotExistsWithLink(eq(SHOP_ID), eq(MASS_SUSPECTED), any(), eq(TICKET_ID));
        verify(problem).setStatus(ProblemStatus.APPROVED_HOLD);
    }

    @Test
    void massFinal() {
        when(shopForecast.needSwitchOff()).thenReturn(true);
        when(problemType.canBeMass()).thenReturn(true);
        suspectMassProblemManager.freezeProblemIfNeeded(problem);

        verifyNoMoreInteractions(recheckTicketManager);
        verify(problem, never()).setStatus(any());
    }

    @Test
    void massByAssessor() {
        when(problemType.canBeMass()).thenReturn(false);
        when(recheckTicketManager.ticketExists(any())).thenReturn(true);
        suspectMassProblemManager.freezeProblemIfNeeded(problem);

        verify(recheckTicketManager, never())
                .addTicketIfNotExistsWithLink(eq(SHOP_ID), eq(MASS_SUSPECTED), any(), eq(TICKET_ID));
        verify(problem).setStatus(ProblemStatus.APPROVED_HOLD);
    }

    @Test
    void nonMassAtAll() {
        when(problemType.canBeMass()).thenReturn(false);
        when(recheckTicketManager.ticketExists(any())).thenReturn(false);
        suspectMassProblemManager.freezeProblemIfNeeded(problem);

        verify(recheckTicketManager, never())
                .addTicketIfNotExistsWithLink(eq(SHOP_ID), eq(MASS_SUSPECTED), any(), eq(TICKET_ID));
        verify(problem, never()).setStatus(any());
    }

    @Test
    void wasAlreadyFrozen() {
        when(pHistory.getStatus()).thenReturn(ProblemStatus.DRAFT, ProblemStatus.NEW, ProblemStatus.APPROVED_HOLD);
        when(problemService.loadHistoryList(PROBLEM_ID)).thenReturn(Arrays.asList(pHistory, pHistory, pHistory));
        suspectMassProblemManager.freezeProblemIfNeeded(problem);
        verifyNoMoreInteractions(ticketService, problemTypeService, recheckTicketManager);
        verify(problem, never()).setStatus(any());
    }
}
