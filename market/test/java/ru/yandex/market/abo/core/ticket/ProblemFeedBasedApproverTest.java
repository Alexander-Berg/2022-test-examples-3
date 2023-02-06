package ru.yandex.market.abo.core.ticket;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.feed.model.DbFeedOfferDetails;
import ru.yandex.market.abo.core.feed.search.task.FeedSearchManager;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTask;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTaskStatus;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.approve.OfferPriceOrStockProblem;
import ru.yandex.market.abo.core.problem.approve.ProblemFeedBasedApprover;
import ru.yandex.market.abo.core.problem.approve.ProblemFeedBasedService;
import ru.yandex.market.abo.core.problem.approve.checkstage.ApproveWithTag;
import ru.yandex.market.abo.core.problem.approve.checkstage.CancelledFeedSearchCheckStage;
import ru.yandex.market.abo.core.problem.approve.checkstage.EmptyFeedCheckStage;
import ru.yandex.market.abo.core.problem.approve.checkstage.EmptySearchTaskCheckStage;
import ru.yandex.market.abo.core.problem.approve.checkstage.FeedCheckStage;
import ru.yandex.market.abo.core.problem.approve.checkstage.IgnoreIdxApiCheckStage;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemType;
import ru.yandex.market.abo.core.ticket.model.TicketTag;
import ru.yandex.market.abo.util.FakeUsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 31.05.17.
 */
public class ProblemFeedBasedApproverTest extends EmptyTestWithTransactionTemplate {
    private static final Long TICKET_ID = 1L;
    private static final long PROBLEM_ID = 2L;

    @InjectMocks
    private ProblemFeedBasedApprover feedBasedApprover = new ProblemFeedBasedApprover() {
        @Override
        protected boolean approve(Problem problem,
                                  Offer storedOffer,
                                  FeedSearchTask task,
                                  OfferPriceOrStockProblem priceAndStockHolder) {
            return approveProblemResult;
        }
    };
    @Mock
    private ProblemService problemService;
    @Mock
    private ProblemFeedBasedService problemFeedBasedService;
    @Mock
    private ProblemTypeService problemTypeService;
    @Mock
    private TicketTagService ticketTagService;
    @Mock
    private FeedSearchManager feedSearchManager;
    @Mock
    private OfferDbService offerDbService;
    @Mock
    private Problem problem;
    @Mock
    private ProblemType problemType;
    @Mock
    private Offer storedOffer;
    @Mock
    private DbFeedOfferDetails dbOffer;
    @Mock
    private FeedSearchTask task;
    @Mock
    private OfferPriceOrStockProblem priceOrOnStockProblem;
    @Mock
    private IgnoreIdxApiCheckStage ignoreIdxApiCheckStage;
    @Mock
    private EmptyFeedCheckStage emptyFeedCheckStage;
    @Mock
    private CancelledFeedSearchCheckStage cancelledFeedSearchCheckStage;
    @Mock
    private EmptySearchTaskCheckStage emptySearchTaskCheckStage;
    @Mock
    private TicketTag tag;

    private Boolean approveProblemResult = true;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(problemTypeService.getProblemType(anyInt())).thenReturn(problemType);
        when(problemType.isWaitingBaseUpdate()).thenReturn(true);

        when(offerDbService.loadOfferByHypId(anyLong())).thenReturn(storedOffer);
        when(feedSearchManager.loadFinishedTask(anyLong(), any())).thenReturn(Optional.of(task));

        when(problem.getTicketId()).thenReturn(TICKET_ID);
        when(problem.getId()).thenReturn(PROBLEM_ID);
        when(task.getStatus()).thenReturn(FeedSearchTaskStatus.OFFER_FOUND);
        when(task.getOffer()).thenReturn(dbOffer);

        when(problemFeedBasedService.loadPriceOrStockProblem(PROBLEM_ID)).thenReturn(priceOrOnStockProblem);
        when(ticketTagService.createTagAndRememberOfferStateByHypId(eq(problem.getTicketId()), anyLong()))
                .thenReturn(tag);

        Stream.of(ignoreIdxApiCheckStage, emptyFeedCheckStage, cancelledFeedSearchCheckStage, emptySearchTaskCheckStage)
                .peek(stage -> when(stage.approve(any(), any(), any())).thenReturn(new ApproveWithTag(true)))
                .forEach(stage -> doAnswer(inv -> FeedCheckStage.and(stage, (FeedCheckStage) inv.getArguments()[0]))
                        .when(stage).and(any()));

        feedBasedApprover.initFeedCheckChain();
    }

    @Test
    public void ignoreIdxFlagOn() {
        when(ignoreIdxApiCheckStage.approve(any(), any(), any())).thenReturn(new ApproveWithTag(false));
        ApproveWithTag approveWithTag = feedBasedApprover.approve(problem);

        verify(emptySearchTaskCheckStage, never()).approve(any(), any(), any());
        verify(cancelledFeedSearchCheckStage, never()).approve(any(), any(), any());
        verify(emptyFeedCheckStage, never()).approve(any(), any(), any());
        verifyLastStageDidNotFired();
        assertFalse(approveWithTag.isApprove());
    }

    @Test
    public void emptySearchTask() {
        when(emptySearchTaskCheckStage.approve(any(), any(), any())).thenReturn(new ApproveWithTag(false));
        ApproveWithTag approveWithTag = feedBasedApprover.approve(problem);

        verify(ignoreIdxApiCheckStage).approve(any(), any(), any());
        verify(emptySearchTaskCheckStage).approve(any(), any(), any());
        verify(cancelledFeedSearchCheckStage, never()).approve(any(), any(), any());
        verify(emptyFeedCheckStage, never()).approve(any(), any(), any());
        verifyLastStageDidNotFired();
        assertFalse(approveWithTag.isApprove());
    }

    @Test
    public void emptyFeed() {
        when(emptyFeedCheckStage.approve(any(), any(), any())).thenReturn(new ApproveWithTag(false));
        ApproveWithTag approveWithTag = feedBasedApprover.approve(problem);

        verify(ignoreIdxApiCheckStage).approve(any(), any(), any());
        verify(cancelledFeedSearchCheckStage).approve(any(), any(), any());
        verify(emptySearchTaskCheckStage).approve(any(), any(), any());
        verify(emptyFeedCheckStage).approve(any(), any(), any());
        verifyLastStageDidNotFired();
        assertFalse(approveWithTag.isApprove());
    }

    @Test
    public void oldFeed_wait() {
        when(cancelledFeedSearchCheckStage.approve(any(), any(), any())).thenReturn(null);
        ApproveWithTag approveWithTag = feedBasedApprover.approve(problem);
        assertNull(approveWithTag);
        verifyLastStageDidNotFired();
    }

    @Test
    public void oldFeed_fail() {
        when(cancelledFeedSearchCheckStage.approve(any(), any(), any())).thenReturn(new ApproveWithTag(false));
        ApproveWithTag approveWithTag = feedBasedApprover.approve(problem);
        assertFalse(approveWithTag.isApprove());
        verifyLastStageDidNotFired();
    }

    @Test
    public void approvePositive() {
        approveProblem(ProblemStatus.APPROVED);
    }

    @Test
    public void approveNegative() {
        approveProblemResult = false;
        approveProblem(ProblemStatus.DISAPPROVED);
    }

    private void approveProblem(ProblemStatus expectedStatus) {
        ApproveWithTag approveWithTag = feedBasedApprover.approve(problem);
        verify(problemService).storeApproveInfo(eq(problem), eq(dbOffer), anyDouble());
        verify(ticketTagService).createTagAndRememberOfferStateByHypId(TICKET_ID, FakeUsers.PROBLEM_AUTO_APPROVER.getId());
        assertEquals(expectedStatus == ProblemStatus.APPROVED, approveWithTag.isApprove());
    }

    private void verifyLastStageDidNotFired() {
        verify(problemFeedBasedService, never()).loadPriceOrStockProblem(anyLong());
        verify(problemService, never()).storeApproveInfo(any(), any(), anyDouble());
        verifyNoMoreInteractions(ticketTagService);
    }
}
