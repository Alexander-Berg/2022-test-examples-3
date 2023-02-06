package ru.yandex.market.pers.tms.timer;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.tms.timer.blue.BlueBadReviewsExecutor;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.grade.core.model.core.BusinessIdEntityType.SHOP;
import static ru.yandex.market.pers.tms.timer.AbstractShopReviewsTicketMonitorExecutor.TicketAction.CREATE_TICKET_ACTION;
import static ru.yandex.market.pers.tms.timer.blue.BlueBadReviewsExecutor.BERU_BAD_REVIEW_OPEN_STATUSES;
import static ru.yandex.market.pers.tms.timer.blue.BlueBadReviewsExecutor.BERU_BAD_REVIEW_OPEN_TRANSITIONS;

public class BlueBadReviewTest extends AbstractBadReviewTest {

    private static final long FAKE_USER = 1L;
    private static final long YA_SHOP_ID = 431782;
    private static final long YA_BUSINESS_ID = 924574L;
    private static final Issue ISSUE_1 = new Issue("1", null, "BERUOPINION-1", null, 1, new EmptyMap(), null);
    private static final Issue ISSUE_2 = new Issue("2", null, "BERUOPINION-2", null, 1, new EmptyMap(), null);

    @Before
    public void setUp() throws Exception {
        pgJdbcTemplate.update("delete from blue_ticket where author_id = ?", FAKE_USER);
        pgJdbcTemplate.update("insert into ext_shop_business_id (shop_id, business_id, type) values (?, ?, ?)",
            YA_SHOP_ID, YA_BUSINESS_ID, SHOP.getValue());

        when(startrekService.createTicket(any(IssueCreate.class))).thenReturn(ISSUE_1, ISSUE_2);
        when(startrekService.getTicket("BERUOPINION-1")).thenReturn(ISSUE_1);
        when(startrekService.getTicket("BERUOPINION-2")).thenReturn(ISSUE_2);
        when(startrekService.createComment(any(Issue.class), any(CommentCreate.class))).thenReturn(null);
//        doNothing().when(startrekService).openTicket(any(Issue.class));

        when(blackBoxUserService.getUserInfo(anyLong())).thenReturn(new UserInfoImpl());
        configurationService.mergeValue("beruBadReviewEnabled", "true");

        executor = createExecutor();
    }

    @Test
    public void testTicketsPipeline() throws Exception {
        doTestTicketsPipeline(ISSUE_1.getKey());
    }

    @Test
    public void testPositiveGrade() throws Exception {
        doTestPositiveGrade();
    }

    @Test
    public void testFirstNegativeGrade() throws Exception {
        doTestFirstNegativeGrade();
    }

    @Test
    public void testTwoNegativeGrades() throws Exception {
        doTestTwoNegativeGrades(ISSUE_1.getKey());
    }

    @Test
    public void testNegativeAndPositiveGrades() throws Exception {
        doTestNegativeAndPositiveGrades(ISSUE_1.getKey());
    }

    @Test
    public void testNegativeAndTwoPositiveGrades() throws Exception {
        doTestNegativeAndTwoPositiveGrades(ISSUE_1.getKey());
    }

    @Test
    public void testTicketSummaryForGradeWithOrder() throws Exception {
        doTestTicketSummaryForGradeWithOrder();
    }

    @Test
    public void testTicketSummaryForGradeWithoutOrder() throws Exception {
        doTestTicketSummaryForGradeWithoutOrder();
    }

    @Test
    public void test5xxStartrek() throws Exception {
        doTest5xxStratrek();
    }

    @Test
    public void testWithAnotherGradesInTestedBranch() throws Exception {
        doTestWithAnotherGradesInTestedBranch(ISSUE_1.getKey(), ISSUE_2.getKey());
    }

    @Test
    public void testNotApprovedGrades() throws Exception {
        doTestNotApprovedGrades();
    }

    @Test
    public void testApprovedAfterUnapproved() throws Exception {
        doTestApprovedAfterUnapproved();
    }

    @Test
    public void testUnapprovedAfterApproved() throws Exception {
        doTestUnapprovedAfterApproved();
    }

    @Test
    public void testCommentAfterUnapprovedGrade() throws Exception {
        doTestCommentAfterUnapprovedGrade(ISSUE_1.getKey());
    }

    @Test
    public void testTagFactor() throws Exception {
        doTestTagFactor();
    }

    @Test
    public void testDeletionComment() throws Exception {
        doTestDeletionComment(ISSUE_1.getKey(), ISSUE_2.getKey());
    }

    @Test
    public void testOpenClosedTicket() throws Exception {
        configurationService.tryGetOrMergeVal(BERU_BAD_REVIEW_OPEN_STATUSES, String.class, OPEN_STATUS);
        configurationService.tryGetOrMergeVal(BERU_BAD_REVIEW_OPEN_TRANSITIONS, String.class, OPEN_TRANSITION);
        doTestOpenClosedTicket(ISSUE_1.getKey());
    }

    @Test
    public void testCantOpenTicket() {
        try {
            doTestCantOpenTicket(ISSUE_1.getKey());
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Can't process"));
        }
    }

    @Test
    public void testDuplicatesInBlueTicketDoesNotCauseDuplicatingComments() throws Exception {
        doTestDuplicatesInBlueTicketDoesNotDuplicateComments(ISSUE_1.getKey(), ISSUE_2.getKey());
    }

    @Test
    public void testDuplicateCommentsDoesntCauseDuplicatesIbBlueTicket() {
        doTestDuplicateCommentsDoesntCauseDuplicatesIbBlueTicket(ISSUE_1);
    }

    @Test
    public void testAnotherShopInBusinessTicketCreation() throws Exception {
        long anotherYaShopId = YA_SHOP_ID + 1;
        pgJdbcTemplate.update("insert into ext_shop_business_id (shop_id, business_id, type) values (?, ?, ?)",
            anotherYaShopId, YA_BUSINESS_ID, SHOP.getValue());

        ShopGrade shopGrade = createShopGradeInstance(5, TEST_ORDER_ID, FAKE_USER, anotherYaShopId);
        createAndModerateTestShopGrade(shopGrade, ModState.APPROVED);
        mockStartedGradeId(shopGrade);
        executor.runTmsJob();
        verify(startrekService).createTicket(argThat(new IssueCreateMatcher(shopGrade)));
        checkBlueTicketAction(shopGrade, CREATE_TICKET_ACTION);
    }

    private BlueBadReviewsExecutor createExecutor() {
        return new BlueBadReviewsExecutor(startrekService, configurationService, userService, factorService,
                pgJdbcTemplate, "reviews", "robot", "market.url",
                "abo.url", "ow.url", "pers-grade-admin.url", "partner.url",
            getComponentString());
    }

    @Override
    protected long getDefaultShopId() {
        return YA_SHOP_ID;
    }

    @Override
    protected String getComponentString() {
        return "[44778,44779,44780,44781,44782]";
    }
}
