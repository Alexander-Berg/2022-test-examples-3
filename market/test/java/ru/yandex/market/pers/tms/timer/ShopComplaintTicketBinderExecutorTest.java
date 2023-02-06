package ru.yandex.market.pers.tms.timer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.common.framework.filter.SimpleQueryFilter;
import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.AuthorIdAndYandexUid;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.util.StaffClient;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.util.db.ConfigurationService;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.Resolution;
import ru.yandex.startrek.client.model.ResolutionRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.tms.timer.ShopComplaintTicketBinderExecutor.LAST_PROCESSED_ID_KEY;

/**
 * @author vvolokh
 * 29.01.2019
 */
public class ShopComplaintTicketBinderExecutorTest extends MockedPersTmsTest {
    private static final long AUTHOR_ID = 1L;
    private static final String TICKET_KEY = "MARKETQUALITY-1";
    private static final String TICKET_KEY_2 = "MARKETQUALITY-2";
    private static final String TICKET_KEY_2_1 = "MARKETQUALITY-2_1";
    private static final String TICKET_KEY_3 = "MARKETQUALITY-3";
    private static final String TICKET_KEY_4 = "MARKETQUALITY-4";
    public static final long MODEL_ID = 9868713L;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DbGradeService gradeService;

    @Autowired
    private StartrekService startrekService;

    @Autowired
    private ShopComplaintTicketBinderExecutor shopComplaintTicketBinderExecutor;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private StaffClient staffClient;

    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void test() throws Exception {
        Long id1 = createShopGrade("[1]grade with ticket");
        createShopComplaintTicket(TICKET_KEY, id1);
        Long id2 = createShopGrade("[1]changed grade, should be associated with ticket");
        Long id3 = createShopGrade("[1]changed grade, which has another ticket created for it");
        createShopComplaintTicket(TICKET_KEY_2, id3);
        createShopComplaintTicketWithType(TICKET_KEY_2_1, id3, 1);
        Long id4 = createShopGrade("[1]changed grade, should be associated with second ticket");
        Long id5 = createResolvedShopGrade("[1]changed grade with resolved, should be associated with second ticket");

        Long id6 = createShopGrade("[2]another grade for this shop, created after grade with resolved; should not be associated with tickets");
        Long id7 = createResolvedShopGrade("[2]resolved grade for 2nd case with ticket");
        createShopComplaintTicket(TICKET_KEY_3, id7);

        Long id8 = createShopGrade("[3]yet another grade after resolved, no tickets should be assigned");
        Long id9 = createShopGrade("[3]grade with ticket");
        createShopComplaintTicket(TICKET_KEY_4, id9);
        Long id10 = createShopGrade("[3]changed grade, should be associated with ticket");
        Long id11 = createResolvedShopGrade("[3]changed resolved grade, should be associated with ticket");

        createShopGradesWithAnotherAuthor();

        configurationService.mergeValue(LAST_PROCESSED_ID_KEY, id1 - 1);

        shopComplaintTicketBinderExecutor.runTmsJob();

        assertHasTicket(TICKET_KEY, id1, id2);
        assertHasTicket(TICKET_KEY_2, id3, id4, id5);
        assertHasTicket(TICKET_KEY_2_1, id3, id4, id5);
        assertHasNoTickets(id6);
        assertHasTicket(TICKET_KEY_3, id7);
        assertHasNoTickets(id8);
        assertHasTicket(TICKET_KEY_4, id10, id11);
        verify(startrekService, Mockito.times(7)).createComment(any(), any(CommentCreate.class));
        assertIsAdvert(id2, id4, id5, id10, id11);
    }

    @Test
    public void testModelGrade() throws Exception {
        long gradeWithTicket = createModelGrade("[1]grade with ticket");
        createShopComplaintTicket(TICKET_KEY, gradeWithTicket);

        long gradeWithoutTicket = createModelGrade("[2]grade without ticket", AUTHOR_ID + 1);
        long gradeWithoutTicketToBind = createModelGrade("[3]grade without ticket to bind");
        long gradeWithoutTicketNoBind = createModelGrade("[3]grade without ticket no bind", AUTHOR_ID + 1);

        configurationService.mergeValue(LAST_PROCESSED_ID_KEY, gradeWithTicket - 1);

        shopComplaintTicketBinderExecutor.runTmsJob();

        assertHasTicket(TICKET_KEY, gradeWithTicket, gradeWithoutTicketToBind);
        assertHasNoTickets(gradeWithoutTicket);
        assertHasNoTickets(gradeWithoutTicketNoBind);

        verify(startrekService, Mockito.times(1)).createComment(any(), any(CommentCreate.class));
        assertIsAdvert(gradeWithoutTicketToBind);
    }

    @Test
    public void testCreateComment() throws Exception {
        Long id1 = createShopGrade("grade with ticket");
        createShopComplaintTicket(TICKET_KEY, id1);
        createShopGrade("changed grade, should be associated with ticket");

        shopComplaintTicketBinderExecutor.runTmsJob();

        ArgumentCaptor<CommentCreate> commentCreateArgumentCaptor = ArgumentCaptor.forClass(CommentCreate.class);
        verify(startrekService, times(2)).getTicket(eq(TICKET_KEY));
        verify(startrekService).createComment(isNull(), commentCreateArgumentCaptor.capture());
        assertEquals(
            commentCreateArgumentCaptor.getValue().getComment().get(),
            String.format("Получена новая версия отзыва: <[  %s\n  Преимущества: %s\n  Недостатки: %s]>",
                "changed grade, should be associated with ticket",
                "pro",
                "contra"));
    }

    @Test
    public void testDeletedGrade() throws Exception {
        long id1 = createShopGrade("grade with ticket");
        createShopComplaintTicket(TICKET_KEY, id1);
        createShopComplaintTicketWithType(TICKET_KEY_2_1, id1, 1);
        configurationService.mergeValue(LAST_PROCESSED_ID_KEY, id1 - 1);

        gradeService.killGrades(new SimpleQueryFilter(), new AuthorIdAndYandexUid(AUTHOR_ID, null));
        shopComplaintTicketBinderExecutor.runTmsJob();

        ArgumentCaptor<CommentCreate> commentCreateArgumentCaptor = ArgumentCaptor.forClass(CommentCreate.class);
        verify(startrekService).getTicket(eq(TICKET_KEY));
        verify(startrekService).getTicket(eq(TICKET_KEY_2_1));
        verify(startrekService, times(2)).createComment(isNull(), commentCreateArgumentCaptor.capture());
        assertEquals(
            commentCreateArgumentCaptor.getValue().getComment().get(),
            String.format("Отзыв %d был удалён.", id1));

        pgJdbcTemplate.execute("update mod_grade_complaint_ticket set state = 0 where ticket_key = '" + TICKET_KEY +
            "' and grade_id = " + id1);
        when(startrekService.getTicket(any())).thenThrow(RuntimeException.class);

        try {
            shopComplaintTicketBinderExecutor.runTmsJob();
        } catch (Exception ignored) {
            // should happens!
        }
    }

    @Test
    public void testStaffUser() throws Exception {
        testStaffUserCase(true, true);
        testStaffUserCase(true, false);
        testStaffUserCase(false, true);
        testStaffUserCase(false, false);
    }

    public void testStaffUserCase(boolean isResolved, boolean isStaff) throws Exception {
        Long id1 = createShopGrade("grade with ticket");
        createShopComplaintTicket(TICKET_KEY, id1);
        createShopGrade("changed grade, should be associated with ticket");

        // ticket is not resolved
        Issue mockTicket = mock(Issue.class);
        Option<ResolutionRef> resolution = isResolved ? Option.of(mock(Resolution.class)) : Option.empty();
        when(mockTicket.getResolution()).thenReturn(resolution);
        when(startrekService.getTicket(TICKET_KEY)).thenReturn(mockTicket);

        // mock user service to get login
        UserInfo userInfoMock = mock(UserInfo.class);
        when(userInfoMock.getLogin()).thenReturn("some-login");
        when(userInfoMock.getUserId()).thenReturn(AUTHOR_ID);
        when(userInfoService.getUserInfo(AUTHOR_ID)).thenReturn(userInfoMock);

        // user is staff
        Optional<String> staffResult = isStaff ? Optional.of("internal-login") : Optional.empty();
        when(staffClient.getPersonByExternalLoginAccurate("some-login")).thenReturn(staffResult);

        shopComplaintTicketBinderExecutor.runTmsJob();

        // check followers were updated
        if (!isResolved) {
            verify(staffClient).getPersonByExternalLoginAccurate("some-login");
        }

        if (!isResolved && isStaff) {
            verify(mockTicket).update(any());
        }
    }

    private void createShopGradesWithAnotherAuthor() {
        createShopGrade("grade", AUTHOR_ID + 1);
        long id = createResolvedShopGrade("and grade", AUTHOR_ID + 1);
        createShopComplaintTicket(TICKET_KEY_4, id);
        createShopGrade("and grade", AUTHOR_ID + 2);
        createShopGrade("grade", AUTHOR_ID + 3);
    }

    private void assertIsAdvert(Long... gradeIds) {
        List<Long> advertGradeIds = new ArrayList<>();
        Arrays.stream(gradeIds).forEach(gradeId -> {
            List<Integer> isAdvert = pgJdbcTemplate.queryForList("SELECT is_advert FROM grade WHERE id=?", Integer.class, gradeId);
            if (!isAdvert.isEmpty() && isAdvert.get(0) == 1) {
                advertGradeIds.add(gradeId);
            }
        });
        assertTrue(advertGradeIds.containsAll(Arrays.asList(gradeIds)));
    }

    private void assertHasNoTickets(long gradeId) {
        List<String> tickets = pgJdbcTemplate.queryForList("select ticket_key from mod_grade_complaint_ticket where grade_id=?", String.class, gradeId);
        assertEquals(0, tickets.size());
    }

    private long createResolvedShopGrade(String text) {
        return createResolvedShopGrade(text, AUTHOR_ID);
    }

    private long createResolvedShopGrade(String text, long authorId) {
        long id = createShopGrade(text, authorId);
        gradeService.resolveGrade(id, authorId, true);
        return id;
    }

    private void assertHasTicket(String ticketKey, Long... gradeIds) {
        List<Long>
            gradeIdsWithTicket =
            pgJdbcTemplate.queryForList("select grade_id from mod_grade_complaint_ticket where ticket_key=?", Long.class,
                ticketKey);

        assertTrue(gradeIdsWithTicket.containsAll(Arrays.asList(gradeIds)));
    }

    private void createShopComplaintTicket(String ticketKey, Long id) {
        createShopComplaintTicketWithType(ticketKey, id, 0);
    }

    private void createShopComplaintTicketWithType(String ticketKey, Long id, int ticketType) {
        pgJdbcTemplate.update(
            "insert into mod_grade_complaint_ticket (ticket_key, grade_id, ticket_type) values (?,?,?)",
            ticketKey, id, ticketType);
    }

    private long createShopGrade(String text) {
        return createShopGrade(text, AUTHOR_ID);
    }

    private long createModelGrade(String text) {
        return createModelGrade(text, AUTHOR_ID);
    }

    private long createShopGrade(String text, long authorId) {
        ShopGrade grade = GradeCreator.constructShopGrade(1L, authorId);
        grade.setModState(ModState.READY);
        grade.setText(text);
        grade.setPro("pro");
        grade.setContra("contra");
        grade.setAverageGrade(4);
        return gradeCreator.createGrade(grade);
    }

    private long createModelGrade(String text, long authorId) {
        ModelGrade grade = GradeCreator.constructModelGrade(MODEL_ID, authorId);
        grade.setModState(ModState.READY);
        grade.setText(text);
        grade.setPro("pro");
        grade.setContra("contra");
        grade.setAverageGrade(4);
        return gradeCreator.createGrade(grade);
    }

}
