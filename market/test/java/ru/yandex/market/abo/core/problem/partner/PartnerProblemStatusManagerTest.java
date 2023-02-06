package ru.yandex.market.abo.core.problem.partner;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.problem.partner.PartnerProblemStatus;
import ru.yandex.market.abo.core.ticket.SimpleTicketTagService;
import ru.yandex.market.abo.core.ticket.model.TicketTag;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 09.08.17.
 */
public class PartnerProblemStatusManagerTest {
    private static final long PROBLEM_ID = 0L;

    @InjectMocks
    private PartnerProblemStatusManager partnerProblemStatusManager;
    @Mock
    private PartnerStatusAware problemService;
    @Mock
    private SimpleTicketTagService simpleTicketTagService;
    @Mock
    private PartnerProblemManager partnerProblemManager;
    @Mock
    private TicketTag ticketTag;

    private final Map<Long, PartnerProblemStatus> coreProblemMap = new HashMap<>();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        coreProblemMap.clear();

        when(problemService.loadPartnerStatuses(any())).thenReturn(coreProblemMap);
        when(simpleTicketTagService.createTag(anyLong())).thenReturn(ticketTag);
    }

    @Test
    public void coreProblem() {
        coreProblemMap.put(PROBLEM_ID, PartnerProblemStatus.APPROVED);
        partnerProblemStatusManager.resolveProblem(PROBLEM_ID);
        verify(problemService).updateProblemStatus(PROBLEM_ID, PartnerProblemStatus.RESOLVED, ticketTag);
        verify(partnerProblemManager).cleanCache(PROBLEM_ID);
    }

    @Test
    public void resolvedAlready() {
        coreProblemMap.put(PROBLEM_ID, PartnerProblemStatus.RESOLVED);
        partnerProblemStatusManager.resolveProblem(PROBLEM_ID);
        verify(partnerProblemManager).cleanCache(PROBLEM_ID);
        verify(problemService, never()).updateProblemStatus(anyInt(), any(), any());
    }

    @Test
    public void notFound() {
        assertThrows(IllegalArgumentException.class, () ->
                partnerProblemStatusManager.resolveProblem(PROBLEM_ID));
    }
}
