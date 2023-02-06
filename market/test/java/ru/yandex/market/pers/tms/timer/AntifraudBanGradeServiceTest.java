package ru.yandex.market.pers.tms.timer;

import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.AntifraudBanLevel;
import ru.yandex.market.pers.grade.core.model.core.AntifraudVerdictType;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.filter.AntifraudBanGradeService;
import ru.yandex.market.pers.tms.filter.grade.DbGradeSpamFilterService;
import ru.yandex.market.pers.tms.imp.AntifraudBanGradeYtImportService;
import ru.yandex.market.pers.tms.imp.dto.FraudInfoDto;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author vvolokh
 * 02.07.2018
 */
public class AntifraudBanGradeServiceTest extends MockedPersTmsTest {

    private static final long PROCESSED_STATE = 1;
    private static final long MODEL_ID_1 = 1;
    private static final long MODEL_ID_2 = 2;
    private static final long MODEL_ID_3 = 3;
    private static final long MODEL_ID_4 = 4;
    private static final long USER_ID = 666;

    @Autowired
    private GradeCreator gradeCreator;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private DbGradeSpamFilterService spamFilterService;
    @Autowired
    private AntifraudBanGradeYtImportService antifraudBanGradeYtImportService;
    @Autowired
    private AntifraudBanGradeService antifraudBanGradeService;

    long gradeIdBan;
    long gradeIdGreyBan;
    long gradeIdBanWhitened;
    long gradeIdGreyBanWhitened;
    long gradeIdNotBan;
    long gradeIdUnban;

    public void createData() {
        gradeIdBan = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID);
        gradeIdGreyBan = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID + 1);
        gradeIdBanWhitened = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID + 2);
        gradeIdGreyBanWhitened = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID + 3);
        gradeIdNotBan = gradeCreator.createModelGrade(MODEL_ID_2, USER_ID);
        gradeIdUnban = gradeCreator.createModelGrade(MODEL_ID_3, USER_ID);

        banGradeByAntifraud(gradeIdBan, AntifraudBanLevel.FRAUD);
        banGradeByAntifraud(gradeIdGreyBan, AntifraudBanLevel.GREY);
        banGradeByAntifraud(gradeIdBanWhitened, AntifraudBanLevel.FRAUD);
        banGradeByAntifraud(gradeIdGreyBanWhitened, AntifraudBanLevel.GREY);

        pgJdbcTemplate.update("insert into WHITE_GRADE(grade_id, user_id) values (?, 1)", gradeIdBanWhitened);
        pgJdbcTemplate.update("insert into WHITE_GRADE(grade_id, user_id) values (?, 1)", gradeIdGreyBanWhitened);
    }

    @Test
    public void testBanGrades() throws Exception {
        createData();

        antifraudBanGradeService.banGradesByAntifraud();

        assertTrue(isSpam(gradeIdBan));
        assertTrue(isSpam(gradeIdGreyBan));

        // 100% спам и в белом списке = спам
        assertTrue(isSpam(gradeIdBanWhitened));
        // в серой зоне и в белом списке = не спам
        assertFalse(isSpam(gradeIdGreyBanWhitened));

        assertFalse(isSpam(gradeIdNotBan));
    }

    @Test
    public void testBanGradesAgain() throws Exception {
        createData();

        // try to ban
        antifraudBanGradeService.banGradesByAntifraud();

        // should work fine
        assertTrue(isProcessedGrade(gradeIdBan));
        assertTrue(isSpam(gradeIdBan));

        // unban grade manually
        pgJdbcTemplate.update("update grade set grade_state = null where id = ?", gradeIdBan);
        assertFalse(isSpam(gradeIdBan));

        // try to process again - should not be processed
        antifraudBanGradeService.banGradesByAntifraud();
        assertFalse(isSpam(gradeIdBan));
    }

    @Test
    public void testDifferenStatesFromTable() {
        long gradeIdFraudSpam = gradeCreator.createModelGrade(MODEL_ID_1, USER_ID); // level = 1, fraud = 1 - ban
        banGradeByAntifraud(gradeIdFraudSpam, AntifraudBanLevel.FRAUD, AntifraudVerdictType.SPAM);
        long gradeIdFraud = gradeCreator.createModelGrade(MODEL_ID_2, USER_ID); // level = 1, fraud = null - ban
        banGradeByAntifraud(gradeIdFraud, AntifraudBanLevel.FRAUD);

        long gradeIdGreySpam = gradeCreator.createModelGrade(MODEL_ID_3, USER_ID);  // level = 2, fraud = 1 - ban
        banGradeByAntifraud(gradeIdGreySpam, AntifraudBanLevel.GREY, AntifraudVerdictType.SPAM);
        long gradeIdGrey = gradeCreator.createModelGrade(MODEL_ID_4, USER_ID); // level = 2, fraud = null - ban
        banGradeByAntifraud(gradeIdGrey, AntifraudBanLevel.GREY);

        long gradeIdFraudNotSpam = gradeCreator.createModelGrade(MODEL_ID_1,
            USER_ID + 1); // level = 1, fraud = 0 - unban
        pgJdbcTemplate.update("update grade set grade_state = 0 where id = ?", gradeIdFraudNotSpam);
        banGradeByAntifraud(gradeIdFraudNotSpam, AntifraudBanLevel.FRAUD, AntifraudVerdictType.NOT_SPAM);

        long gradeIdGreyNotSpam = gradeCreator.createModelGrade(MODEL_ID_2,
            USER_ID + 1); // level = 2, fraud = 0 - unban
        pgJdbcTemplate.update("update grade set grade_state = 0 where id = ?", gradeIdGreyNotSpam);
        banGradeByAntifraud(gradeIdGreyNotSpam, AntifraudBanLevel.GREY, AntifraudVerdictType.NOT_SPAM);

        long gradeIdNotSpam = gradeCreator.createModelGrade(MODEL_ID_3, USER_ID + 1); // level = null, fraud = 0 - unban
        pgJdbcTemplate.update("update grade set grade_state = 0 where id = ?", gradeIdNotSpam);
        banGradeByAntifraud(gradeIdNotSpam, AntifraudVerdictType.NOT_SPAM);


        try {
            antifraudBanGradeService.banGradesByAntifraud();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unexpected values in antifraud ban table"));
        }

        assertTrue(isSpam(gradeIdFraudSpam));
        assertTrue(isSpam(gradeIdFraud));
        assertTrue(isSpam(gradeIdGreySpam));
        assertTrue(isSpam(gradeIdGrey));

        assertFalse(isSpam(gradeIdFraudNotSpam));
        assertFalse(isSpam(gradeIdGreyNotSpam));
        assertFalse(isSpam(gradeIdNotSpam));
    }

    private boolean isProcessedGrade(long gradeIdBan) {
        final Long state = pgJdbcTemplate.queryForObject(
            "select coalesce(state, -1) from antifraud_ban_grade where grade_id = ?",
            Long.class,
            gradeIdBan
        );
        return state == PROCESSED_STATE;
    }

    private boolean isSpam(long gradeId) {
        final Long gradeState = pgJdbcTemplate.queryForObject("select coalesce(grade_state, -1) from grade where id =" +
                " ?",
            Long.class, gradeId);
        return gradeState != -1;
    }

    private void banGradeByAntifraud(long gradeIdBan, AntifraudBanLevel banLevel) {
        antifraudBanGradeService.saveGradeIdsForProcessing(
            Collections.singletonList(new FraudInfoDto(gradeIdBan, banLevel.getCode(),
                AntifraudVerdictType.SPAM.getCode())));
    }

    private void banGradeByAntifraud(long gradeIdBan, AntifraudBanLevel banLevel, AntifraudVerdictType fraudStatus) {
        antifraudBanGradeService.saveGradeIdsForProcessing(
            Collections.singletonList(new FraudInfoDto(gradeIdBan, banLevel.getCode(), fraudStatus.getCode())));
    }

    private void banGradeByAntifraud(long gradeIdBan, AntifraudVerdictType fraudStatus) {
        antifraudBanGradeService.saveGradeIdsForProcessing(
            Collections.singletonList(new FraudInfoDto(gradeIdBan, null, fraudStatus.getCode())));
    }
}
