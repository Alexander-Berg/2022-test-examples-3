package ru.yandex.market.pers.grade.admin.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.grade.admin.base.BaseGradeAdminDbTest;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.AntifraudBanLevel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author varvara
 * 18.10.2019
 */
public class WhiteGradeControllerTest extends BaseGradeAdminDbTest {

    private static final long MODEL_ID_1 = 1;
    private static final long USER_ID = 666;

    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void testAddToWhiteListBannedByAntifraud() throws Exception {
        long gradeIdBan = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID);
        long gradeIdGreyBan = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID + 1);
        long gradeIdNotBan = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID + 2);
        long gradeIdOldBan = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID + 4);

        pgJdbcTemplate.update("insert into antifraud_ban_grade_last(grade_id, ban_level) values (?, ?)",
            gradeIdBan, AntifraudBanLevel.FRAUD.getCode());
        pgJdbcTemplate.update("insert into antifraud_ban_grade_last(grade_id, ban_level) values (?, ?)",
            gradeIdGreyBan, AntifraudBanLevel.GREY.getCode());
        pgJdbcTemplate.update("insert into antifraud_ban_grade_last(grade_id, ban_level) values (?, null)",
            gradeIdOldBan);

        markGradeWhite(gradeIdBan, status().is2xxSuccessful());
        markGradeWhite(gradeIdGreyBan, status().is2xxSuccessful());
        markGradeWhite(gradeIdNotBan, status().is2xxSuccessful());
        markGradeWhite(gradeIdOldBan, status().is2xxSuccessful());

        assertTrue(isGradeInWhiteList(gradeIdBan));
        assertTrue(isGradeInWhiteList(gradeIdGreyBan));
        assertTrue(isGradeInWhiteList(gradeIdNotBan));
        assertTrue(isGradeInWhiteList(gradeIdOldBan));
    }

    @Test
    public void testTransactional() throws Exception {
        long gradeIdBan = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID);
        markGradeWhite(gradeIdBan, status().is2xxSuccessful());
        markGradeWhite(gradeIdBan, status().is2xxSuccessful());
    }

    @Test
    public void testIndexingApprovedGradesAfterWhitening() throws Exception {
        long gradeIdBan = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID, ModState.APPROVED);
        long gradeIdRejected = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID + 1, ModState.REJECTED);
        addToGradeIndexing(gradeIdBan);
        addToGradeIndexing(gradeIdRejected);

        pgJdbcTemplate.update("insert into antifraud_ban_grade_last(grade_id, ban_level) values (?, ?)",
            gradeIdBan, AntifraudBanLevel.FRAUD.getCode());
        pgJdbcTemplate.update("insert into antifraud_ban_grade_last(grade_id, ban_level) values (?, ?)",
            gradeIdRejected, AntifraudBanLevel.FRAUD.getCode());

        markGradeWhite(gradeIdBan, status().is2xxSuccessful());
        markGradeWhite(gradeIdRejected, status().is2xxSuccessful());

        assertTrue(isResetIndexTime(gradeIdBan));
        assertFalse(isResetIndexTime(gradeIdRejected));
    }

    private boolean isGradeInWhiteList(long gradeId) {
        return pgJdbcTemplate.queryForObject("select count(*) from WHITE_GRADE where grade_id = ?", Long.class, gradeId) > 0;
    }

    private void markGradeWhite(long gradeId, ResultMatcher resultMatcher) throws Exception {
            mvc.perform(post("/api/white-grade/mark?grade-id=" + gradeId))
                .andDo(print())
                .andExpect(resultMatcher)
                .andReturn().getResponse().getContentAsString();
    }

    private void addToGradeIndexing(long gradeId) {
        pgJdbcTemplate.update(
            "insert into saas_indexing(grade_id, mod_time, index_time) " +
                "values (?, now() - interval '50' hour, now() - interval '48' hour)",
            gradeId);
    }

    private boolean isResetIndexTime(long gradeId) {
        return pgJdbcTemplate.queryForObject(
            "select count(*) " +
                "from saas_indexing " +
                "where grade_id = ? and index_time is null",
            Long.class, gradeId) > 0;
    }
}
