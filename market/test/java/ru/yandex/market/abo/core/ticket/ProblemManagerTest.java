package ru.yandex.market.abo.core.ticket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.problem.approve.ProblemApprover;
import ru.yandex.market.abo.core.problem.approve.ProblemApproverFactory;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemType;
import ru.yandex.market.abo.core.shop.CommonShopInfoService;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.ticket.listener.problem.ProblemGenerationSaver;
import ru.yandex.market.abo.core.ticket.listener.problem.ProblemListener;
import ru.yandex.market.abo.core.ticket.listener.problem.SuspectMassProblemManager;
import ru.yandex.market.abo.core.ticket.model.TicketTag;
import ru.yandex.market.abo.gen.HypothesisService;
import ru.yandex.market.abo.test.TestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 18.10.18.
 */
class ProblemManagerTest extends EmptyTestWithTransactionTemplate {
    private static final long TICKET_ID = 1L;
    private static final long SHOP_ID = 0L;
    private static final int PROBLEM_TYPE = 42;

    @InjectMocks
    private ProblemManager problemManager;
    @Mock
    private TicketService ticketService;
    @Mock
    private CommonShopInfoService shopInfoService;
    @Mock
    private ProblemService problemService;
    @Mock
    private TicketTagService ticketTagService;
    @Mock
    private ProblemTypeService problemTypeService;
    @Mock
    private ProblemApproverFactory problemApproverFactory;
    @Mock
    private HypothesisService hypothesisService;
    @Mock
    private ExecutorService pool;

    private Problem problem;
    @Mock
    private ShopInfo shopInfo;
    @Mock
    private TicketTag tag;
    @Mock
    private ProblemType problemType;
    @Mock
    private ProblemApprover problemApprover;
    @Mock
    private ProblemListener problemListener;
    @Mock
    private SuspectMassProblemManager massProblemManager;
    @Mock
    private ProblemGenerationSaver generationSaver;
    private final List<ProblemStatus> savedStatuses = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        problemManager.setProblemListeners(Collections.singletonList(problemListener));

        problem = initProblem();
        savedStatuses.clear();
        when(problemService.getProblemsForAutoApprove()).thenReturn(Collections.singletonList(problem));
        doAnswer(inv -> {
            Problem p = (Problem) inv.getArguments()[0];
            savedStatuses.add(p.getStatus());
            return p;
        }).when(problemService).saveProblem(any(), any());

        when(shopInfoService.getFromDbOrApi(SHOP_ID)).thenReturn(shopInfo);
        when(shopInfo.isInCpcOrInCpa()).thenReturn(true);

        when(ticketTagService.createTag(anyLong())).thenReturn(tag);

        when(problemTypeService.getProblemType(PROBLEM_TYPE)).thenReturn(problemType);
        when(problemApproverFactory.getApprover(problemType)).thenReturn(problemApprover);
        when(ticketService.getShopId(TICKET_ID)).thenReturn(SHOP_ID);
        when(hypothesisService.hypIdToShopId(Collections.singletonList(TICKET_ID)))
                .thenReturn(Collections.singletonMap(TICKET_ID, SHOP_ID));
        TestHelper.mockExecutorService(pool);
    }

    @Test
    void shopIsOff() {
        when(shopInfo.isInCpcOrInCpa()).thenReturn(false);
        problemManager.autoApproveProblems();

        verify(problemService, atLeastOnce()).logApproveProcess(eq(problem), eq(ProblemStatus.DISAPPROVED), any());
        assertEquals(ProblemStatus.DISAPPROVED, problem.getStatus());
    }

    @Test
    void exceptionWhileApproving() {
        when(shopInfoService.getFromDbOrApi(anyLong())).thenThrow(new RuntimeException());
        problemManager.autoApproveProblems();

        verify(problemService).logApproveProcess(eq(problem), eq(ProblemStatus.NEW), any());
        verify(problemService, never()).saveProblem(any(), any());
    }

    @Test
    void doApprove() {
        problemManager.autoApproveProblems();
        verify(problemApprover).approve(problem);
    }

    @Test
    void approveImmediately() {
        problem.setForceApprove(true);
        problemManager.saveProblem(problem, tag);
        assertProblemSaved(ProblemStatus.NEW, ProblemStatus.APPROVED);
    }

    @Test
    void approveWithFreeze() {
        ProblemStatus hold = ProblemStatus.APPROVED_HOLD;
        doAnswer(invocation -> {
            problem.setStatus(hold);
            return null;
        }).when(massProblemManager).freezeProblemIfNeeded(problem);

        problemManager.saveProblem(problem, tag);
        assertProblemSaved(hold);
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void testResolveCritical(boolean isTypeCritical, boolean hasErrors) {
        problem.setStatus(ProblemStatus.NEW);
        when(problemType.isCritical()).thenReturn(isTypeCritical);
        when(problemService.shopHasProblemsWithType(isNotNull(), anyLong())).thenReturn(hasErrors);
        problemManager.saveProblem(problem, tag);
        assertEquals(isTypeCritical && hasErrors, problem.isCritical());
    }

    private void assertProblemSaved(ProblemStatus... withStatuses) {
        verify(problemService, times(withStatuses.length)).saveProblem(eq(problem), any());
        assertEquals(withStatuses.length, savedStatuses.size());

        IntStream.range(0, withStatuses.length).forEach(i -> assertEquals(withStatuses[i], savedStatuses.get(i)));
        assertEquals(withStatuses[withStatuses.length - 1], problem.getStatus());
        verify(generationSaver, atLeastOnce()).saveGeneration(problem);
    }

    private static Problem initProblem() {
        return Problem.newBuilder()
                .ticketId(TICKET_ID)
                .problemTypeId(PROBLEM_TYPE)
                .status(ProblemStatus.NEW)
                .build();
    }
}
