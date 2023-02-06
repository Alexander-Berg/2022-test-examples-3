package ru.yandex.market.pers.tms.timer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.framework.filter.SimpleQueryFilter;
import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.AuthorIdAndYandexUid;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.ugc.FactorService;
import ru.yandex.market.pers.grade.core.ugc.model.GradeFactor;
import ru.yandex.market.pers.grade.core.ugc.model.GradeFactorValue;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.util.db.ConfigurationService;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.StatusRef;
import ru.yandex.startrek.client.model.Transition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.tms.timer.AbstractShopReviewsTicketMonitorExecutor.TicketAction.COMMENT_GRADE_DELETED;
import static ru.yandex.market.pers.tms.timer.AbstractShopReviewsTicketMonitorExecutor.TicketAction.COMMENT_REOPEN_TICKET;
import static ru.yandex.market.pers.tms.timer.AbstractShopReviewsTicketMonitorExecutor.TicketAction.CREATE_TICKET_ACTION;

/**
 * @author vvolokh
 * 26.03.2019
 */
public abstract class AbstractBadReviewTest extends MockedPersTmsTest {

    protected static final long FAKE_USER = 1L;
    protected static final String USER_LOGIN = "login";
    protected static final String TEST_ORDER_ID = "-666777888";
    protected static final String USER_DELETED_GRADE_TEXT = "Пользователь удалил свой отзыв";

    protected static final String OPEN_TRANSITION = "open";
    protected static final String OPEN_STATUS = "new";
    protected static final String NOT_OPEN_STATUS = "close";

    @Autowired
    protected DbGradeService dbGradeService;
    @Autowired
    protected ConfigurationService configurationService;
    @Autowired
    protected StartrekService startrekService;
    @Autowired
    protected UserInfoService blackBoxUserService;
    @Autowired
    protected UserInfoService userService;
    @Autowired
    protected GradeModeratorModificationProxy moderatorModificationProxy;
    @Autowired
    protected FactorService factorService;
    @Autowired
    protected GradeCreator gradeCreator;

    protected AbstractShopReviewsTicketMonitorExecutor executor;

    protected abstract long getDefaultShopId();

    protected Map<Long, Long> getComponentMapping() {
        return AbstractShopReviewsTicketMonitorExecutor.parseComponentsString(getComponentString());
    }

    protected abstract String getComponentString();

    public void doTestTicketsPipeline(String expectedIssueKey) throws Exception {
        ShopGrade grade1 = createApprovedTestShopGrade(5, TEST_ORDER_ID);
        Issue expectedIssueMock = getIssueMockWithStatus(expectedIssueKey, OPEN_STATUS);
        mockStartedGradeId(grade1);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade1)));

        ShopGrade grade2 = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();
        // has ticket ISSUE_1 => comment and reopen ISSUE_1
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(grade2)));

        ShopGrade grade3 = createApprovedTestShopGrade(1, TEST_ORDER_ID);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();
        // has ticket ISSUE_1 => comment and reopen ISSUE_1
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(grade3)));

        ShopGrade grade4 = createApprovedTestShopGrade(grade3);
        executor.runTmsJob();
        // almost the same with grade3  => do nothing

        ShopGrade grade5 = createApprovedTestShopGrade(4, TEST_ORDER_ID);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();
        // has ticket ISSUE_1 => comment and reopen ISSUE_1
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(grade5)));
    }

    protected void doTestPositiveGrade() throws Exception {
        ShopGrade grade = createApprovedTestShopGrade(5, TEST_ORDER_ID);
        mockStartedGradeId(grade);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade)));
        checkBlueTicketAction(grade, CREATE_TICKET_ACTION);
    }

    protected void doTestFirstNegativeGrade() throws Exception {
        ShopGrade grade = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        mockStartedGradeId(grade);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade)));
        checkBlueTicketAction(grade, CREATE_TICKET_ACTION);
    }

    protected void doTestTwoNegativeGrades(String expectedTicketKey) throws Exception {
        ShopGrade grade1 = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        mockStartedGradeId(grade1);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade1)));
        checkBlueTicketAction(grade1, CREATE_TICKET_ACTION, expectedTicketKey);

        ShopGrade grade2 = createApprovedTestShopGrade(1, TEST_ORDER_ID);

        Issue expectedIssueMock = getIssueMockWithStatus(expectedTicketKey, OPEN_STATUS);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();
        // has ticket ISSUE_1 => comment and reopen ISSUE_1
        verify(startrekService, times(1)).createComment(eq(expectedIssueMock),
            argThat(new CommentCreateMatcher(grade2)));
        checkBlueTicketAction(grade2, COMMENT_REOPEN_TICKET, expectedTicketKey);
    }

    protected void doTestNegativeAndPositiveGrades(String expectedTicketKey) throws Exception {
        ShopGrade grade1 = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        mockStartedGradeId(grade1);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade1)));
        checkBlueTicketAction(grade1, CREATE_TICKET_ACTION, expectedTicketKey);

        Issue expectedIssueMock = getIssueMockWithStatus(expectedTicketKey, OPEN_STATUS);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        ShopGrade grade2 = createApprovedTestShopGrade(4, TEST_ORDER_ID);
        executor.runTmsJob();
        // has ticket ISSUE_1 => comment and reopen ISSUE_1
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(grade2)));
        checkBlueTicketAction(grade2, COMMENT_REOPEN_TICKET, expectedTicketKey);
    }

    protected void doTestNegativeAndTwoPositiveGrades(String expectedIssueKey) throws Exception {
        ShopGrade grade1 = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        mockStartedGradeId(grade1);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade1)));
        checkBlueTicketAction(grade1, CREATE_TICKET_ACTION, expectedIssueKey);

        ShopGrade grade2 = createApprovedTestShopGrade(4, TEST_ORDER_ID);
        Issue expectedIssueMock = getIssueMockWithStatus(expectedIssueKey, OPEN_STATUS);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();
        // has ticket ISSUE_1  => comment and reopen ISSUE_1
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(grade2)));
        checkBlueTicketAction(grade2, COMMENT_REOPEN_TICKET, expectedIssueKey);

        ShopGrade grade3 = createApprovedTestShopGrade(4, TEST_ORDER_ID);
        executor.runTmsJob();
        // has ticket ISSUE_1  => comment and reopen ISSUE_1
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(grade3)));
        checkBlueTicketAction(grade3, COMMENT_REOPEN_TICKET, expectedIssueKey);
    }

    protected void doTestTicketSummaryForGradeWithOrder() throws Exception {
        ShopGrade grade = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        mockStartedGradeId(grade);
        executor.runTmsJob();
        ArgumentCaptor<IssueCreate> argumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(argumentCaptor.capture());

        assertEquals(1, argumentCaptor.getAllValues().size());
        assertEquals(String.format("Заказ %s - пользователь %s оставил отзыв %s на " + executor.getShopDisplayName() + " на Маркете",
            grade.getOrderId(), USER_LOGIN, grade.getId()),
            argumentCaptor.getValue().getValues().getO("summary").get());
    }

    protected void doTestTicketSummaryForGradeWithoutOrder() throws Exception {
        ShopGrade grade = createApprovedTestShopGrade(2, null);
        mockStartedGradeId(grade);
        executor.runTmsJob();
        ArgumentCaptor<IssueCreate> argumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(argumentCaptor.capture());

        assertEquals(1, argumentCaptor.getAllValues().size());
        assertEquals(String.format("Пользователь %s оставил отзыв %s на " + executor.getShopDisplayName() + " на " +
                "Маркете",
            USER_LOGIN, grade.getId()),
            argumentCaptor.getValue().getValues().getO("summary").get());
    }

    protected void doTest5xxStratrek() throws Exception {
        ShopGrade grade = createApprovedTestShopGrade(2, null);
        mockStartedGradeId(grade);
        when(startrekService.createTicket(any(IssueCreate.class))).thenThrow(new RuntimeException("Did not create " +
            "ticket after retries"));
        try {
            executor.runTmsJob();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Can't process"));
        }
        long count = pgJdbcTemplate.queryForObject(
                "select count(*) from blue_ticket where grade_id = ?",
                Long.class,
                grade.getId());
        assertEquals(0, count);
    }

    protected void doTestWithAnotherGradesInTestedBranch(String ticketKey1, String ticketKey2) throws Exception {
        ShopGrade grade1 = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        mockStartedGradeId(grade1);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade1)));
        checkBlueTicketAction(grade1, CREATE_TICKET_ACTION, ticketKey1);

        // add grade aside from tested grade branch to account for bug that incorrectly retrieved
        // previous ticket key for grades
        ShopGrade anotherGrade = createApprovedTestShopGrade(3, TEST_ORDER_ID + 1, FAKE_USER + 1);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_2
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(anotherGrade)));
        checkBlueTicketAction(anotherGrade, CREATE_TICKET_ACTION, ticketKey2);

        ShopGrade grade2 = createApprovedTestShopGrade(4, TEST_ORDER_ID);
        Issue expectedIssueMock = getIssueMockWithStatus(ticketKey1, OPEN_STATUS);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();
        // has ticket ISSUE_1 => comment and reopen ISSUE_1
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(grade2)));
        checkBlueTicketAction(grade2, COMMENT_REOPEN_TICKET);
    }

    protected void doTestNotApprovedGrades() throws Exception {
        ShopGrade
            shopGrade = createAndModerateTestShopGrade(createShopGradeInstance(1, TEST_ORDER_ID, FAKE_USER),
            ModState.UNMODERATED);
        mockStartedGradeId(shopGrade);
        executor.runTmsJob();
        verifyZeroInteractions(startrekService);

        createAndModerateTestShopGrade(createShopGradeInstance(2, TEST_ORDER_ID, FAKE_USER), ModState.READY);
        executor.runTmsJob();
        verifyZeroInteractions(startrekService);

        createAndModerateTestShopGrade(createShopGradeInstance(3, TEST_ORDER_ID, FAKE_USER),
            ModState.AUTOMATICALLY_REJECTED);
        executor.runTmsJob();
        verifyZeroInteractions(startrekService);

        createAndModerateTestShopGrade(createShopGradeInstance(4, TEST_ORDER_ID, FAKE_USER), ModState.REJECTED);
        executor.runTmsJob();
        verifyZeroInteractions(startrekService);

        createAndModerateTestShopGrade(createShopGradeInstance(5, TEST_ORDER_ID, FAKE_USER), ModState.DELAYED);
        executor.runTmsJob();
        verifyZeroInteractions(startrekService);

        createAndModerateTestShopGrade(createShopGradeInstance(4, TEST_ORDER_ID, FAKE_USER), ModState.SPAMMER);
        executor.runTmsJob();
        verifyZeroInteractions(startrekService);

        createAndModerateTestShopGrade(createShopGradeInstance(3, TEST_ORDER_ID, FAKE_USER),
            ModState.REJECTED_BY_SHOP_CLAIM);
        executor.runTmsJob();
        verifyZeroInteractions(startrekService);

        createAndModerateTestShopGrade(createShopGradeInstance(2, TEST_ORDER_ID, FAKE_USER),
            ModState.AWAITS_PHOTO_MODERATION);
        executor.runTmsJob();
        verifyZeroInteractions(startrekService);
    }

    protected void doTestApprovedAfterUnapproved() throws Exception {
        ShopGrade shopGrade = createTestShopGrade(createShopGradeInstance(1, TEST_ORDER_ID, FAKE_USER));
        executor.runTmsJob();
        verifyZeroInteractions(startrekService);

        moderatorModificationProxy.moderateGradeReplies(Collections.singletonList(shopGrade.getId()),
            Collections.emptyList(), 1L, ModState.APPROVED);

        executor.runTmsJob();
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(shopGrade)));
        checkBlueTicketAction(shopGrade, CREATE_TICKET_ACTION);
    }

    protected void doTestUnapprovedAfterApproved() throws Exception {
        ShopGrade shopGrade = createApprovedTestShopGrade(1, TEST_ORDER_ID, FAKE_USER);
        executor.runTmsJob();
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(shopGrade)));
        checkBlueTicketAction(shopGrade, CREATE_TICKET_ACTION);

        createTestShopGrade(createShopGradeInstance(2, TEST_ORDER_ID, FAKE_USER));
        executor.runTmsJob();
        verifyNoMoreInteractions(startrekService);
    }

    protected void doTestCommentAfterUnapprovedGrade(String expectedIssueKey) throws Exception {
        ShopGrade shopGrade = createApprovedTestShopGrade(1, TEST_ORDER_ID, FAKE_USER);
        executor.runTmsJob();
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(shopGrade)));
        checkBlueTicketAction(shopGrade, CREATE_TICKET_ACTION, expectedIssueKey);

        createTestShopGrade(createShopGradeInstance(2, TEST_ORDER_ID, FAKE_USER));
        executor.runTmsJob();
        verifyNoMoreInteractions(startrekService);

        Issue expectedIssueMock = getIssueMockWithStatus(expectedIssueKey, OPEN_STATUS);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        shopGrade = createApprovedTestShopGrade(3, TEST_ORDER_ID, FAKE_USER);
        executor.runTmsJob();
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(shopGrade)));
        checkBlueTicketAction(shopGrade, COMMENT_REOPEN_TICKET, expectedIssueKey);

        createTestShopGrade(createShopGradeInstance(2, TEST_ORDER_ID, FAKE_USER));
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();

        verify(startrekService, times(1)).createComment(eq(expectedIssueMock),
            argThat(new CommentCreateMatcher(shopGrade)));
    }

    protected void doTestTagFactor() throws Exception {
        ShopGrade shopGradeInstance = createShopGradeInstance(2, TEST_ORDER_ID, FAKE_USER);
        shopGradeInstance.setGradeFactorValues(Collections.singletonList(new GradeFactorValue(1L, "Скорость и " +
            "качество доставки", "", 2)));
        ShopGrade grade = createApprovedTestShopGrade(shopGradeInstance);
        mockStartedGradeId(grade);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade)));
        checkBlueTicketAction(grade, CREATE_TICKET_ACTION);
    }

    protected void doTestDeletionComment(String expectedIssueKey1, String expectedIssueKey2) throws Exception {
        ShopGrade grade1 = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        mockStartedGradeId(grade1);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade1)));
        checkBlueTicketAction(grade1, CREATE_TICKET_ACTION, expectedIssueKey1);

        ShopGrade grade2 = createApprovedTestShopGrade(4, TEST_ORDER_ID);
        Issue expectedIssueMock = getIssueMockWithStatus(expectedIssueKey2, OPEN_STATUS);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();
        // has ticket ISSUE_1  => comment and reopen ISSUE_1
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(grade2)));
        checkBlueTicketAction(grade2, COMMENT_REOPEN_TICKET, expectedIssueKey2);

        SimpleQueryFilter filter = new SimpleQueryFilter();
        filter.addSelector("id", grade2.getId());
        final AuthorIdAndYandexUid authorIdAndYandexUid = new AuthorIdAndYandexUid(FAKE_USER, null);
        dbGradeService.killGrades(filter, authorIdAndYandexUid);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new DeletionCommentCreateMatcher()));
        checkBlueTicketAction(grade2, COMMENT_GRADE_DELETED, expectedIssueKey2);
    }

    protected void doTestOpenClosedTicket(String expectedTicketKey) throws Exception {
        ShopGrade grade1 = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        mockStartedGradeId(grade1);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade1)));
        checkBlueTicketAction(grade1, CREATE_TICKET_ACTION, expectedTicketKey);

        // ticket is closed, can open ticket
        Issue expectedIssueMock = getIssueMockWithStatus(expectedTicketKey, NOT_OPEN_STATUS);
        Transition openTransitionMock = mock(Transition.class);
        Mockito.when(startrekService.getPossibleTransition(eq(expectedIssueMock),
            argThat(o -> o.size() == 1 && o.contains(OPEN_TRANSITION)))).thenReturn(Optional.of(openTransitionMock));
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);

        ShopGrade grade2 = createApprovedTestShopGrade(4, TEST_ORDER_ID);
        executor.runTmsJob();
        // has ticket ISSUE_1 => comment ISSUE_1
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(grade2)));
        verify(startrekService).changeState(eq(expectedIssueMock), eq(openTransitionMock));
        checkBlueTicketAction(grade2, COMMENT_REOPEN_TICKET, expectedTicketKey);
    }

    protected void doTestCantOpenTicket(String expectedTicketKey) throws Exception {
        ShopGrade grade1 = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        mockStartedGradeId(grade1);
        executor.runTmsJob();
        // doesn't have ticket => create ticket - ISSUE_1
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade1)));
        checkBlueTicketAction(grade1, CREATE_TICKET_ACTION, expectedTicketKey);

        Issue expectedIssueMock = getIssueMockWithStatus(expectedTicketKey, "close");
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
//        when(startrekService.isCanChangeState(eq(expectedIssueMock), eq("new"))).thenReturn(false);
        ShopGrade grade2 = createApprovedTestShopGrade(4, TEST_ORDER_ID);
        executor.runTmsJob();
        // has ticket ISSUE_1 => comment and fail in check about open ISSUE_1
//        verify(startrekService, times(0)).openTicket(eq(expectedIssueMock));
    }

    protected void doTestDuplicatesInBlueTicketDoesNotDuplicateComments(String expectedIssueKey1,
                                                                        String expectedIssueKey2) throws Exception {
        ShopGrade grade1 = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        Issue expectedIssueMock = getIssueMockWithStatus(expectedIssueKey1, OPEN_STATUS);
        mockStartedGradeId(grade1);
        executor.runTmsJob();
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(grade1)));
        checkBlueTicketAction(grade1, CREATE_TICKET_ACTION, expectedIssueKey1);

        SimpleQueryFilter filter = new SimpleQueryFilter();
        filter.addSelector("id", grade1.getId());
        final AuthorIdAndYandexUid authorIdAndYandexUid = new AuthorIdAndYandexUid(FAKE_USER, null);
        dbGradeService.killGrades(filter, authorIdAndYandexUid);

        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new DeletionCommentCreateMatcher()));
        checkBlueTicketAction(grade1, COMMENT_GRADE_DELETED, expectedIssueKey1);

        ShopGrade grade2 = createApprovedTestShopGrade(4, TEST_ORDER_ID);
        when(startrekService.getTicket(anyString())).thenReturn(expectedIssueMock);
        executor.runTmsJob();
        verify(startrekService).createComment(eq(expectedIssueMock), argThat(new CommentCreateMatcher(grade2)));
        checkBlueTicketAction(grade2, COMMENT_REOPEN_TICKET, expectedIssueKey1);
    }

    protected void doTestDuplicateCommentsDoesntCauseDuplicatesIbBlueTicket(Issue issue) {
        ShopGrade grade1 = createApprovedTestShopGrade(2, TEST_ORDER_ID);
        AbstractShopReviewsTicketMonitorExecutor.ShopGradeWithTicket shopGradeWithTicket =
            new AbstractShopReviewsTicketMonitorExecutor.ShopGradeWithTicket(grade1, issue.getKey());
        executor.insertTicketInfo(issue, shopGradeWithTicket, CREATE_TICKET_ACTION);
        executor.insertTicketInfo(issue, shopGradeWithTicket, CREATE_TICKET_ACTION);
        Integer actionsCount = getBlueTicketActionsCount(grade1, CREATE_TICKET_ACTION, issue.getKey());
        assertEquals(1, (int) actionsCount);
    }

    private Integer getBlueTicketActionsCount(ShopGrade shopGrade,
                                           AbstractShopReviewsTicketMonitorExecutor.TicketAction expectedAction,
                                           String expectedTicketKey) {
        return pgJdbcTemplate.queryForObject(
                "select count(*) from blue_ticket\n" +
                        "where ticket_key = ? and grade_id = ? and action = ? and shop_id = ?",
                Integer.class,
                expectedTicketKey, shopGrade.getId(), expectedAction.getValue(), getDefaultShopId());
    }

    /**
     * @param shopGrade
     * @param action    0 - create ticket, 1 - comment and reopen ticket, 2 - good review
     */
    protected void checkBlueTicketAction(ShopGrade shopGrade,
                                         AbstractShopReviewsTicketMonitorExecutor.TicketAction action) {
        List<Integer> x = pgJdbcTemplate.queryForList(
                "select action from blue_ticket where grade_id = ?",
                Integer.class,
                shopGrade.getId());
        assertTrue(x.contains(action.getValue()));
    }

    /**
     * @param shopGrade
     * @param expectedAction    0 - create ticket, 1 - comment and reopen ticket, 2 - good review
     * @param expectedTicketKey
     */
    protected void checkBlueTicketAction(ShopGrade shopGrade,
                                         AbstractShopReviewsTicketMonitorExecutor.TicketAction expectedAction,
                                         String expectedTicketKey) {
        List<Integer> actions = pgJdbcTemplate.queryForList(
                "select action from blue_ticket where grade_id = ?",
                Integer.class,
                shopGrade.getId());
        assertTrue(actions.contains(expectedAction.getValue()));

        List<String> tickets = pgJdbcTemplate.queryForList(
                "select ticket_key from blue_ticket where grade_id = ?",
                String.class,
                shopGrade.getId());
        assertTrue(tickets.contains(expectedTicketKey));
    }

    protected void mockStartedGradeId(ShopGrade id1) {
        configurationService.mergeValue("blueBadReviewStartedId", id1.getId() - 1);
    }

    protected ShopGrade createTestShopGrade(ShopGrade shopGrade) {
        if (shopGrade.getId() != null) {
            shopGrade.setId(null);
        }
        long id = gradeCreator.createGrade(shopGrade);
        if (shopGrade.getGradeFactorValues() != null) {
            factorService.saveGradeFactorValuesWithCleanup(id, shopGrade.getGradeFactorValues());
            shopGrade.setGradeFactorValues(factorService.getFactorValuesByGrade(id));
        }
        shopGrade.setId(id);
        return shopGrade;
    }

    protected ShopGrade createAndModerateTestShopGrade(ShopGrade shopGrade, ModState modState) {
        ShopGrade createdShopGrade = createTestShopGrade(shopGrade);
        moderatorModificationProxy.moderateGradeReplies(Collections.singletonList(createdShopGrade.getId()),
            Collections.emptyList(), 1L, modState);
        return createdShopGrade;
    }

    protected ShopGrade createApprovedTestShopGrade(ShopGrade shopGrade) {
        return createAndModerateTestShopGrade(shopGrade, ModState.APPROVED);
    }

    protected ShopGrade createApprovedTestShopGrade(int gradeValue, String orderId) {
        return createApprovedTestShopGrade(gradeValue, orderId, FAKE_USER);
    }

    protected ShopGrade createApprovedTestShopGrade(int gradeValue, String orderId, long userId) {
        ShopGrade shopGrade = createShopGradeInstance(gradeValue, orderId, userId);
        return createApprovedTestShopGrade(shopGrade);
    }

    protected ShopGrade createShopGradeInstance(int gradeValue, String orderId, long userId) {
        return createShopGradeInstance(gradeValue, orderId, userId, getDefaultShopId());
    }

    protected ShopGrade createShopGradeInstance(int gradeValue, String orderId, long userId, long shopId) {
        ShopGrade shopGrade = GradeCreator.constructShopGrade(shopId, userId)
            .fillShopGradeCreationFields(orderId, Delivery.PICKUP);
        shopGrade.setText(UUID.randomUUID().toString());
        shopGrade.setModState(ModState.UNMODERATED);
        shopGrade.setAverageGrade(gradeValue);
        return shopGrade;
    }

    protected static class CommentCreateMatcher implements ArgumentMatcher<CommentCreate> {

        ShopGrade grade;

        CommentCreateMatcher(ShopGrade grade) {
            this.grade = grade;
        }

        @Override
        public boolean matches(CommentCreate argument) {
            if (argument instanceof CommentCreate) {
                CommentCreate commentCreate = (CommentCreate) argument;
                final String s = commentCreate.getComment().get();
                return containsGradeText(s, grade);
            }
            return false;
        }
    }

    protected static class DeletionCommentCreateMatcher implements ArgumentMatcher<CommentCreate> {

        @Override
        public boolean matches(CommentCreate argument) {
            if (argument instanceof CommentCreate) {
                CommentCreate commentCreate = (CommentCreate) argument;
                final String s = commentCreate.getComment().get();
                return s.equals(USER_DELETED_GRADE_TEXT);
            }
            return false;
        }
    }

    protected class IssueCreateMatcher implements ArgumentMatcher<IssueCreate> {

        ShopGrade grade;

        IssueCreateMatcher(ShopGrade grade) {
            this.grade = grade;
        }

        @Override
        public boolean matches(IssueCreate argument) {
            if (argument instanceof IssueCreate) {
                IssueCreate issueCreate = (IssueCreate) argument;
                final String s = (String) issueCreate.getValues().getOptional("description").get();
                return containsGradeText(s, grade) && containsComponentWithAvgGrade(issueCreate) && containsFactorTag(issueCreate);
            }
            return false;
        }

        private boolean containsFactorTag(IssueCreate issueCreate) {
            if (grade.getGradeFactorValues() == null || grade.getGradeFactorValues().isEmpty()) {
                return true;
            }

            if (issueCreate.getValues().containsKeyTs("tags")) {
                List<String> factors = grade.getGradeFactorValues().stream()
                    .map(GradeFactor::getTitle)
                    .map(tag -> tag.replaceAll(" ", "_").replaceAll(",", ""))
                    .collect(Collectors.toList());
                return Arrays.asList((String[]) issueCreate.getValues().getOrThrow("tags")).containsAll(factors);
            }

            return false;
        }

        private boolean containsComponentWithAvgGrade(IssueCreate issueCreate) {
            if (issueCreate.getValues().containsKeyTs("components")) {
                Object components = issueCreate.getValues().getOrThrow("components");
                long gradeValue = grade.getAverageGrade().longValue();
                return
                    components.getClass().getComponentType().isAssignableFrom(long.class)
                        && Arrays.stream((long[]) (components))
                        .anyMatch(val -> val == getComponentMapping().get(gradeValue));
            }
            return false;
        }
    }

    protected Issue getIssueMockWithStatus(String ticketKey, String statusKey) {
        StatusRef statusRefMock = mock(StatusRef.class);
        when(statusRefMock.getKey()).thenReturn(statusKey);
        Issue expectedIssueSpy = mock(Issue.class);
        when(expectedIssueSpy.getKey()).thenReturn(ticketKey);
        when(expectedIssueSpy.getStatus()).thenReturn(statusRefMock);
        return expectedIssueSpy;
    }

    protected static boolean containsGradeText(String s, ShopGrade grade) {
        return s.contains("Достоинства: " + Optional.ofNullable(grade.getPro()).orElse(""))
            && s.contains("Недостатки: " + Optional.ofNullable(grade.getContra()).orElse(""))
            && s.contains("Комментарий: " + Optional.ofNullable(grade.getText()).orElse(""))
            && s.contains("Оценка: " + (grade.getAverageGrade() - 3));
    }

    public class UserInfoImpl implements UserInfo {
        @Override
        public String getLogin() {
            return USER_LOGIN;
        }

        @Override
        public long getUserId() {
            return 0;
        }

        @Override
        public String getValue(UserInfoField field) {
            return null;
        }
    }
}
