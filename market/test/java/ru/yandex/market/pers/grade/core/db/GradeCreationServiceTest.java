package ru.yandex.market.pers.grade.core.db;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.framework.filter.SimpleQueryFilter;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.AuthorIdAndYandexUid;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.VerifiedType;
import ru.yandex.market.pers.grade.core.service.VerifiedGradeService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GradeCreationServiceTest extends DbGradeServiceTestBase {
    private static final long MODEL_ID = 123134134;
    private static final long USER_ID = 454308134;

    @Autowired
    private VerifiedGradeService verifiedGradeService;

    @Test
    public void ignoreEmptyGradeIfExistsNonEmptyGrade() {
        ModelGrade param = GradeCreator.constructModelGradeRnd();
        long gradeIdWithText = gradeCreator.createGrade(param);

        List<AbstractGrade> userGrades = gradeService.findUserGrades(param.getAuthorUid(), null);
        assertEquals(1, userGrades.size());

        ModelGrade almostEmptyGradeWithoutText = GradeCreator.constructModelGradeNoText(param.getResourceId(),
            param.getAuthorUid(),
            param.getModState());
        long almostEmptyGradeWithoutTextId = gradeCreator.createGrade(almostEmptyGradeWithoutText);

        userGrades = gradeService.findUserGrades(param.getAuthorUid(), null);
        assertEquals(1, userGrades.size());
        // new grade didn't create because of empty text content and non empty factor/photo content
        assertEquals(gradeIdWithText, almostEmptyGradeWithoutTextId);

        ModelGrade almostEmptyGradeWithText = GradeCreator.constructModelGradeNoText(param.getResourceId(),
            param.getAuthorUid(),
            param.getModState());
        almostEmptyGradeWithText.setText("text");
        long almostEmptyGradeWithTextId = gradeCreator.createGrade(almostEmptyGradeWithText);

        userGrades = gradeService.findUserGrades(param.getAuthorUid(), null);
        assertEquals(1, userGrades.size());
        // new grade created because of non empty content (factor/photo exists) and text
        // on new and existing content on actual
        assertNotEquals(gradeIdWithText, almostEmptyGradeWithTextId);

        ModelGrade emptyGrade = GradeCreator.constructModelGradeNoText(param.getResourceId(),
            param.getAuthorUid(), param.getModState());
        emptyGrade.setPhotos(Collections.emptyList());
        emptyGrade.setGradeFactorValues(Collections.emptyList());
        long emptyGradeId = gradeCreator.createGrade(almostEmptyGradeWithoutText);

        userGrades = gradeService.findUserGrades(param.getAuthorUid(), null);
        assertEquals(1, userGrades.size());
        // no new grade created because of empty content on new and existing content on actual
        assertEquals(almostEmptyGradeWithTextId, emptyGradeId);
    }

    @Test
    public void testParamInheritance() {
        ModelGrade gradeTemplate = GradeCreator.constructModelGrade(MODEL_ID, USER_ID);
        Long orderId = 1234L;
        long gradeId = gradeCreator.createGrade(gradeTemplate);
        setModelOrderId(orderId, gradeId);

        assertNull(getPubGrade(USER_ID).getSource());
        assertFalse(getPubGrade(USER_ID).getCpa());
        assertFalse(getPubGrade(USER_ID).getVerified());
        assertNull(getCpaInDb(gradeId));
        assertNull(getVerifiedInDb(gradeId));
        assertEquals(orderId, getModelOrderId(gradeId));

        verifiedGradeService.setVerified(List.of(gradeId), VerifiedType.ANTIFRAUD);
        verifiedGradeService.setCpaInDB(List.of(gradeId), true);

        assertTrue(getPubGrade(USER_ID).getCpa());
        assertTrue(getPubGrade(USER_ID).getVerified());

        // check cpa inherited, add initial source
        gradeTemplate.setSource("init");
        gradeTemplate.setText(UUID.randomUUID().toString());
        gradeId = gradeCreator.createGrade(gradeTemplate);
        assertEquals("init", getPubGrade(USER_ID).getSource());
        assertEquals("init", getPubGrade(USER_ID).getRealSource());
        assertTrue(getPubGrade(USER_ID).getCpa());
        assertTrue(getPubGrade(USER_ID).getVerified());
        assertEquals(1, getCpaInDb(gradeId).intValue());
        assertEquals(1, getVerifiedInDb(gradeId).intValue());
        assertEquals(orderId, getModelOrderId(gradeId));

        // reset cpa
        verifiedGradeService.changeVerified(List.of(getPubGrade(USER_ID).getId()), false, VerifiedType.ANTIFRAUD, 0L);
        verifiedGradeService.setCpaInDB(List.of(getPubGrade(USER_ID).getId()), false);

        assertFalse(getPubGrade(USER_ID).getCpa());
        assertFalse(getPubGrade(USER_ID).getVerified());

        // check cpa=0 is not inherited
        gradeTemplate.setSource("second");
        gradeTemplate.setText(UUID.randomUUID().toString());
        gradeId = gradeCreator.createGrade(gradeTemplate);
        assertEquals("init", getPubGrade(USER_ID).getSource());
        assertEquals("second", getPubGrade(USER_ID).getRealSource());
        assertFalse(getPubGrade(USER_ID).getCpa());
        assertFalse(getPubGrade(USER_ID).getVerified());
        assertNull(getCpaInDb(gradeId));
        assertNull(getVerifiedInDb(gradeId));
        assertEquals(orderId, getModelOrderId(gradeId));

        gradeService.killGrades(new SimpleQueryFilter(), new AuthorIdAndYandexUid(USER_ID, null));
        checkNoGrades(USER_ID);

        gradeTemplate.setSource("third");
        gradeTemplate.setText(UUID.randomUUID().toString());
        gradeCreator.createGrade(gradeTemplate);
        assertEquals("init", getPubGrade(USER_ID).getSource());
        assertEquals("third", getPubGrade(USER_ID).getRealSource());
        assertEquals(orderId, getModelOrderId(gradeId));

        gradeTemplate.setSource(null);
        gradeTemplate.setText(UUID.randomUUID().toString());
        gradeCreator.createGrade(gradeTemplate);
        assertEquals("init", getPubGrade(USER_ID).getSource());
        assertNull(getPubGrade(USER_ID).getRealSource());
        assertEquals(orderId, getModelOrderId(gradeId));
    }

    private void setModelOrderId(long orderId, long gradeId) {
        pgJdbcTemplate.update("update grade.grade_model set model_order_id = ? where grade_id = ?", orderId, gradeId);
    }

    private Long getModelOrderId(long gradeId) {
        return pgJdbcTemplate.queryForObject("select model_order_id from grade_model where grade_id = ?", Long.class, gradeId);
    }

    private AbstractGrade getPubGrade(long userId) {
        List<AbstractGrade> userGrades = gradeService.findUserGrades(userId, null);
        assertEquals(1, userGrades.size());
        return userGrades.get(0);
    }

    private Integer getCpaInDb(long gradeId) {
        return pgJdbcTemplate.queryForObject("select cpa from grade where id = ?", Integer.class, gradeId);
    }

    private Integer getVerifiedInDb(long gradeId) {
        return pgJdbcTemplate.queryForObject("select verified from grade where id = ?", Integer.class, gradeId);
    }

    private void checkNoGrades(long userId) {
        List<AbstractGrade> userGrades = gradeService.findUserGrades(userId, null);
        assertEquals(0, userGrades.size());
    }

}
