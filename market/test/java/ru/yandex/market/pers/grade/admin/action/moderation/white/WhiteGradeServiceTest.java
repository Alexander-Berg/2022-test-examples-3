package ru.yandex.market.pers.grade.admin.action.moderation.white;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.admin.MockedPersGradeAdminTest;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.service.GradeQueueService;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         29.06.16
 */
public class WhiteGradeServiceTest extends MockedPersGradeAdminTest {
    private static final long FAKE_MODEL_1 = -923847529487L;
    private static final long FAKE_AUTHOR_ID = -82352050155L;
    private static final long FAKE_AUTHOR_ID_2 = FAKE_AUTHOR_ID + 1;

    @Autowired
    private WhiteGradeService whiteGradeService;

    @Autowired
    private GradeQueueService gradeQueueService;

    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void addWhiteGradeSimgle() {
        gradeQueueService.clearAll();

        final long gradeId = gradeCreator.createGrade(
            createTestModelGrade(FAKE_MODEL_1, FAKE_AUTHOR_ID, 0));

        Assert.assertTrue(gradeQueueService.isEmpty());

        whiteGradeService.addWhiteGrade(gradeId, FAKE_AUTHOR_ID_2);

        Assert.assertFalse(gradeQueueService.isEmpty());
        Assert.assertEquals(1, gradeQueueService.getAll().size());
        Assert.assertTrue(gradeQueueService.getAll().contains(gradeId));
    }

    @Test
    public void addWhiteGradesBatch() {
        gradeQueueService.clearAll();

        final long gradeId = gradeCreator.createGrade(
            createTestModelGrade(FAKE_MODEL_1, FAKE_AUTHOR_ID, 0));

        final long gradeId2 = gradeCreator.createGrade(
            createTestModelGrade(FAKE_MODEL_1 + 1, FAKE_AUTHOR_ID, 0));

        Assert.assertTrue(gradeQueueService.isEmpty());

        whiteGradeService.addWhiteGrades(Arrays.asList(gradeId, gradeId2), FAKE_AUTHOR_ID_2, true);

        Assert.assertFalse(gradeQueueService.isEmpty());
        Assert.assertEquals(2, gradeQueueService.getAll().size());
        Assert.assertTrue(gradeQueueService.getAll().contains(gradeId));
        Assert.assertTrue(gradeQueueService.getAll().contains(gradeId2));
    }

    @Test
    public void addWhiteGradesBatchNoQueuing() {
        gradeQueueService.clearAll();

        final long gradeId = gradeCreator.createGrade(createTestModelGrade(FAKE_MODEL_1, FAKE_AUTHOR_ID, 0));

        final long gradeId2 = gradeCreator.createGrade(
            createTestModelGrade(FAKE_MODEL_1 + 1, FAKE_AUTHOR_ID, 0));

        Assert.assertTrue(gradeQueueService.isEmpty());

        whiteGradeService.addWhiteGrades(Arrays.asList(gradeId, gradeId2), FAKE_AUTHOR_ID_2, false);

        Assert.assertTrue(gradeQueueService.isEmpty());
    }

    private ModelGrade createTestModelGrade(long modelId, long authorId, int grade) {
        ModelGrade result = GradeCreator.constructModelGrade(modelId, authorId);
        result.setGr0(grade);
        return result;
    }

}
