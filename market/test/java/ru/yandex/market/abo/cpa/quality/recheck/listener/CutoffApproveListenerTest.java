package ru.yandex.market.abo.cpa.quality.recheck.listener;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.ShopPlacement;
import ru.yandex.market.abo.api.entity.problem.partner.PartnerProblem;
import ru.yandex.market.abo.api.entity.problem.partner.ShopPartnerProblems;
import ru.yandex.market.abo.core.forecast.ShopForecastManager;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.partner.PartnerProblemManager;
import ru.yandex.market.abo.core.ticket.ProblemManager;
import ru.yandex.market.abo.core.ticket.TicketTagService;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicket;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketStatus;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 14.12.18
 */
public class CutoffApproveListenerTest {
    private static final long SHOP_ID = 774;

    @InjectMocks
    CutoffApproveListener cutoffApproveListener;
    @Mock
    ShopForecastManager shopForecastManager;
    @Mock
    PartnerProblemManager partnerProblemManager;
    @Mock
    ProblemManager problemManager;
    @Mock
    TicketTagService ticketTagService;
    @Captor
    ArgumentCaptor<Long> captor;

    private static final Set<PartnerProblem> PROBLEMS = ImmutableSet.of(
            createProblem(0, 0, true),
            createProblem(1, 1, false),
            createProblem(2, 1, true),
            createProblem(3, 2, true),
            createProblem(4, 2, true)
    );
    private static final Set<Long> REJECT_PROBLEMS_IDS = ImmutableSet.of(2L, 3L, 4L);
    private static final int OVERFLOW_COUNT = 2;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void passByTimeoutTest() {
        when(shopForecastManager.getErrorsCountOverflow(anyLong(), any())).thenReturn(OVERFLOW_COUNT);
        when(partnerProblemManager.getShopProblemsNonCached(anyLong(), any()))
                .thenReturn(new ShopPartnerProblems(SHOP_ID, false, ShopPlacement.CPC, PROBLEMS));
        when(problemManager.loadProblem(anyLong())).thenAnswer(inv ->
                Problem.newBuilder().id((long) inv.getArguments()[0]).status(ProblemStatus.APPROVED).build());

        cutoffApproveListener.notify(createTicket(RecheckTicketStatus.PASS_BY_TIMEOUT));

        verify(problemManager, times(3)).loadProblem(captor.capture());
        assertEquals(REJECT_PROBLEMS_IDS, new HashSet<>(captor.getAllValues()));
    }

    private static RecheckTicket createTicket(RecheckTicketStatus status) {
        RecheckTicket ticket = new RecheckTicket.Builder()
                .withShopId(SHOP_ID)
                .withType(RecheckTicketType.CUTOFF_APPROVE)
                .withStatus(status)
                .build();
        ticket.setId(0);
        return ticket;
    }

    private static PartnerProblem createProblem(long id, int genId, boolean critical) {
        PartnerProblem problem = new PartnerProblem();
        problem.setId(id);
        problem.setGenerationId(genId);
        problem.setCritical(critical);
        return problem;
    }
}
