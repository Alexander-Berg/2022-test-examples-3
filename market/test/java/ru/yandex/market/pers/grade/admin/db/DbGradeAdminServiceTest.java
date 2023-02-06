package ru.yandex.market.pers.grade.admin.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.framework.db.xmlmapping.DataRowMapperSqlAware;
import ru.yandex.common.framework.filter.QueryFilter;
import ru.yandex.common.util.db.DbUtil;
import ru.yandex.common.util.db.OrderByClause;
import ru.yandex.market.pers.grade.admin.action.monitoring.moderator.EmptyQueryFilter;
import ru.yandex.market.pers.grade.admin.base.BaseGradeAdminDbTest;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.model.core.ModReason;
import ru.yandex.market.pers.grade.core.model.core.ModerationType;
import ru.yandex.market.pers.grade.core.moderation.Object4Moderation;
import ru.yandex.market.util.JudgementUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public class DbGradeAdminServiceTest extends BaseGradeAdminDbTest {
    @Autowired
    private DbGradeAdminService gradeAdminService;
    @Autowired
    private DataRowMapperSqlAware modGradeStatMapper;
    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void testFilterRegexp() {
        assertFalse(JudgementUtils.isJudgementOk("aksjdfk fUck ", Pattern.compile("fuck",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
    }

    @Test
    public void testModGradeStat() {
        QueryFilter filter = new EmptyQueryFilter(getServRequest(), new OrderByClause("moderator_id", false));
        printXmlConvertableList(gradeAdminService.findFilteredRowsPg(filter, null, modGradeStatMapper));
        gradeAdminService.countFilteredRowsPg(filter, modGradeStatMapper);
    }

    @Test
    public void testChangeModStates() throws Exception {
        DbGradeAdminService gradeAdminServiceSpy = spy(gradeAdminService);
        Object4Moderation notModeratedObject = Object4Moderation.moderated(1, ModState.APPROVED);
        Object4Moderation moderatedObject = Object4Moderation.moderated(2, ModState.AUTOMATICALLY_REJECTED);

        Set<Long> moderatedObjects = new HashSet<>();
        moderatedObjects.add(moderatedObject.getId());
        doReturn(moderatedObjects).when(gradeAdminServiceSpy).getModeratedObjects(Arrays.asList(notModeratedObject, moderatedObject));
        doNothing().when(gradeAdminServiceSpy).moderate(any(), any());

        gradeAdminServiceSpy.changeModStatesByAutomoderator(new ArrayList<>(asList(
            notModeratedObject,
            moderatedObject)));

        verify(gradeAdminServiceSpy, times(1))
            .moderate(singletonList(notModeratedObject), DbGradeAdminService.FAKE_MODERATOR);
    }

    @Test
    public void testChangeModStatesIT() throws Exception {
        Object4Moderation notModeratedObject = createGradeForModeration(ModState.APPROVED);
        Object4Moderation moderatedObject = createGradeForModeration(ModState.REJECTED);

        // moderate one grade manually
        gradeAdminService.moderate(singletonList(moderatedObject), 1L);

        // try to moderate by auto-moderator
        gradeAdminService.changeModStatesByAutomoderator(new ArrayList<>(asList(
            Object4Moderation.moderated(notModeratedObject.getId(), ModState.APPROVED),
            Object4Moderation.moderated(moderatedObject.getId(), ModState.APPROVED)
        )));

        // moderated manually should be OK
        Assert.assertEquals(ModState.REJECTED.value(), getGradeState(moderatedObject.getId()));
        Assert.assertEquals(ModState.APPROVED.value(), getGradeState(notModeratedObject.getId()));
    }

    @Test
    public void testAutomoderationTracking() {
        Object4Moderation readyGrade = createGradeForModeration(ModState.READY);
        Object4Moderation rejectedGrade = createGradeForModeration(ModState.AUTOMATICALLY_REJECTED);
        gradeAdminService.changeModStatesByAutomoderator(Arrays.asList(readyGrade, rejectedGrade));
        assertNotNull(getAutomoderationTime(readyGrade.getId(), readyGrade.getModState()));
        checkModerationType(ModerationType.MODERATION, readyGrade.getId());
        assertNotNull(getAutomoderationTime(rejectedGrade.getId(), rejectedGrade.getModState()));
        checkModerationType(ModerationType.MODERATION, rejectedGrade.getId());
    }

    @Test
    public void testModGrade() {
        long userId = 2498629;
        int shopId = 720;
        long moderatorId = 3465725;
        long gradeId = gradeCreator.createShopGrade(userId, shopId, 2);

        // check grade is not moderated yet
        assertNull(getModStateFromTable(gradeId));

        // moderate
        Object4Moderation gradeModerated = Object4Moderation.moderated(gradeId, ModState.APPROVED, ModReason.RUDE.forShop());
        gradeAdminService.moderate(singletonList(gradeModerated), moderatorId);

        assertModerationStatus(gradeModerated, getModStateFromTable(gradeId));
        checkModerationType(ModerationType.MODERATION, gradeId);

        // change moderation state
        gradeModerated = Object4Moderation.moderated(gradeId, ModState.REJECTED, ModReason.RUDE.forShop());
        gradeAdminService.moderate(singletonList(gradeModerated), moderatorId);

        assertModerationStatus(gradeModerated, getModStateFromTable(gradeId));
        checkModerationType(ModerationType.MODERATION, gradeId);
    }

    @Test
    public void testDoNotChangeModeratorIdOnFakeModeratorOnApprove() {
        long userId = 1L;
        long shopId = 123L;
        long moderatorId = 9876L;
        long gradeId = gradeCreator.createShopGrade(userId, shopId, 2);

        // check grade is not moderated yet
        assertNull(getModStateFromTable(gradeId));

        //moderate by moderator
        Object4Moderation gradeModerated = Object4Moderation.moderated(gradeId, ModState.READY_TO_PUBLISH, ModReason.RUDE.forShop());
        gradeAdminService.moderate(singletonList(gradeModerated), moderatorId);

        assertModerationStatus(gradeModerated, getModStateFromTable(gradeId));
        checkModeratorId(moderatorId, gradeId);

        //approve grade by automoderation
        Object4Moderation gradeApproved = Object4Moderation.moderated(gradeId, ModState.APPROVED, ModReason.RUDE.forShop());
        gradeAdminService.moderate(singletonList(gradeApproved), DbGradeAdminService.FAKE_MODERATOR);

        assertModerationStatus(gradeApproved, getModStateFromTable(gradeId));
        checkModeratorId(moderatorId, gradeId);
    }

    @Test
    public void testChangeModeratorIdOnFakeModeratorOnNotApprove() {
        long userId = 1L;
        long shopId = 322L;
        long moderatorId = 9876L;
        long gradeId = gradeCreator.createShopGrade(userId, shopId, 2);

        // check grade is not moderated yet
        assertNull(getModStateFromTable(gradeId));

        //approve grade by moderator
        Object4Moderation gradeModerated = Object4Moderation.moderated(gradeId, ModState.APPROVED, ModReason.RUDE.forShop());
        gradeAdminService.moderate(singletonList(gradeModerated), moderatorId);

        assertModerationStatus(gradeModerated, getModStateFromTable(gradeId));
        checkModeratorId(moderatorId, gradeId);

        //reject grade by auto moderator
        Object4Moderation gradeApproved = Object4Moderation.moderated(gradeId, ModState.REJECTED, ModReason.RUDE.forShop());
        gradeAdminService.moderate(singletonList(gradeApproved), DbGradeAdminService.FAKE_MODERATOR);

        assertModerationStatus(gradeApproved, getModStateFromTable(gradeId));
        checkModeratorId(DbGradeAdminService.FAKE_MODERATOR, gradeId);
    }

    @Test
    public void testChangeModeratorIdOnOtherModeratorId() {
        long userId = 1L;
        long shopId = 322L;
        long moderatorId1 = 9876L;
        long moderatorId2 = 1234L;
        long gradeId = gradeCreator.createShopGrade(userId, shopId, 2);

        // check grade is not moderated yet
        assertNull(getModStateFromTable(gradeId));

        //approve grade by first moderator
        Object4Moderation gradeModerated = Object4Moderation.moderated(gradeId, ModState.APPROVED, ModReason.RUDE.forShop());
        gradeAdminService.moderate(singletonList(gradeModerated), moderatorId1);

        assertModerationStatus(gradeModerated, getModStateFromTable(gradeId));
        checkModeratorId(moderatorId1, gradeId);

        //reject grade by second moderator
        Object4Moderation gradeApproved = Object4Moderation.moderated(gradeId, ModState.REJECTED, ModReason.RUDE.forShop());
        gradeAdminService.moderate(singletonList(gradeApproved), moderatorId2);

        assertModerationStatus(gradeApproved, getModStateFromTable(gradeId));
        checkModeratorId(moderatorId2, gradeId);
    }

    private void assertModerationStatus(Object4Moderation gradeModerated, Object4Moderation modStateFromView) {
        assertNotNull(modStateFromView);
        assertEquals(gradeModerated.getModState(), modStateFromView.getModState());
        assertEquals(gradeModerated.getModReason(), modStateFromView.getModReason());
    }

    private Object4Moderation getModStateFromTable(long gradeId) {
        List<Object4Moderation> result = pgJdbcTemplate.query(
            "select grade_id, mod_state, mod_reason\n" +
                "from MOD_GRADE_LAST\n" +
                "where GRADE_ID = ?",
            (rs, rowNum) -> Object4Moderation.moderated(
                rs.getLong("grade_id"),
                ModState.byValue(rs.getInt("mod_state")),
                DbUtil.getLong(rs,"mod_reason")
            ),
            gradeId
        );
        return result.isEmpty() ? null : result.get(0);
    }

    private Object4Moderation createGradeForModeration(ModState modState) {
        long gradeId = gradeCreator.createShopGrade(319479234, 720, 2);
        return Object4Moderation.moderated(gradeId, modState);
    }

    private int getGradeState(long gradeId) {
        return pgJdbcTemplate.queryForObject("SELECT MOD_STATE FROM GRADE WHERE ID = " + gradeId, Integer.class);
    }

    private Date getAutomoderationTime(long gradeId, ModState modState) {
        return pgJdbcTemplate.queryForObject(
            "select mod_time from mod_grade_last where grade_id = ? and mod_state = ?",
            Date.class, gradeId, modState.value());
    }

    private void checkModerationType(ModerationType expectedModerationType, Long gradeId) {
        final Integer moderationType = pgJdbcTemplate.queryForObject(
            "select moderation_type from mod_grade where grade_id = ? order by mod_time desc limit 1",
            Integer.class, gradeId);
        assertEquals(expectedModerationType.value(), moderationType.intValue());
    }

    private void checkModeratorId(Long moderatorId, long gradeId) {
        Long moderatorIdFromTable = pgJdbcTemplate.queryForObject(
            "select moderator_id from mod_grade_last where grade_id = ? order by mod_time limit 1",
            Long.class, gradeId);
        assertEquals(moderatorId, moderatorIdFromTable);
    }

}
