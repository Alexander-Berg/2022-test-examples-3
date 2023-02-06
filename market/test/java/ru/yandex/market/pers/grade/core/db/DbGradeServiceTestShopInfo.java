package ru.yandex.market.pers.grade.core.db;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.common.framework.pager.Pager;
import ru.yandex.market.pers.grade.client.dto.GradePager;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.db.model.ShopGradesFilter;
import ru.yandex.market.pers.grade.core.db.model.Sort;
import ru.yandex.market.pers.grade.core.db.model.SortType;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;

import static org.junit.Assert.assertEquals;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.01.2019
 */
public class DbGradeServiceTestShopInfo extends DbGradeServiceTestBase {

    private void prepareMatView() {
        pgJdbcTemplate.update("refresh materialized view mv_partner_grade");
    }

    @Test
    public void testGetSimpleShopGrades() {
        long shopId = 384719247;

        createTestGradeOk(UID, shopId, 2);
        createTestGradeOk(UID + 1, shopId, 4);

        // sort desc
        ShopGradesFilter filter = new ShopGradesFilter(shopId)
            .pager(new Pager(1, 10))
            .sort(Sort.desc(SortType.ID));

        prepareMatView();
        GradePager<AbstractGrade> shopGrades = partnerShopGradeService.getShopGrades(filter);
        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), 4, buildTestText(UID, 1));
        assertGradeSimple(shopGrades.getData().get(1), 2, buildTestText(UID, 0));

        // sort asc
        filter = new ShopGradesFilter(shopId)
            .pager(new Pager(1, 10))
            .sort(Sort.asc(SortType.ID));

        shopGrades = partnerShopGradeService.getShopGrades(filter);
        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), 2, buildTestText(UID, 0));
        assertGradeSimple(shopGrades.getData().get(1), 4, buildTestText(UID, 1));
    }

    private void assertGradeSimple(AbstractGrade grade, int avgGrade, String text) {
        int gr0 = avgGrade - 3;

        assertEquals(avgGrade, grade.getAverageGrade().intValue());
        assertEquals(gr0, grade.getGradeValue().intValue());
        assertEquals(text, grade.getText());
    }

    private void createTestGradeOk(long uid, long shopId, int avgGrade) {
        doSaveGrade(buildTestGrade(uid, shopId, avgGrade, ModState.APPROVED));
    }

    @NotNull
    private ShopGrade buildTestGrade(long uid, long shopId, int avgGrade, ModState modState) {
        ShopGrade testGrade = createTestShopGrade(uid, shopId, buildTestText(uid, 0), avgGrade);
        testGrade.setModState(modState);
        return testGrade;
    }

    private long doSaveGrade(ShopGrade testGrade) {
        return gradeCreator.createGrade(testGrade);
    }

    private String buildTestText(long uid, int shift) {
        return "test grade " + (uid + shift);
    }
}
