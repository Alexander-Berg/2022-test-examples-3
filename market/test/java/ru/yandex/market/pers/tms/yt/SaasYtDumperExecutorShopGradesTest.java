package ru.yandex.market.pers.tms.yt;

import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.tms.yt.dumper.dumper.reader.saas.SaasDocumentYt;
import ru.yandex.market.saas.indexer.SaasIndexerAction;

import static ru.yandex.market.pers.grade.core.GradeCreator.constructShopGrade;

@WebAppConfiguration
public class SaasYtDumperExecutorShopGradesTest extends SaasYtDumperExecutorGradesTest {

    private static String getSql(boolean grade) {
        return "SELECT id, RESOURCE_ID from GRADE g\n" +
            "WHERE g.TYPE = 0\n" +
            "AND g.id " + (grade ? "" : "not") + " in (select id FROM v_saas_shop_grade_for_index)\n" +
            "FETCH FIRST 1 ROWS ONLY";
    }

    @Test
    public void testPubGradePubSummaryDelRecSummary() throws Exception {
        createShopGradeWithFactorValues();
        test(true);
    }

    @Test
    public void testDelGradePubSummaryDelRecSummary() throws Exception {
        deleteGrade(createShopGradeWithFactorValues(), AUTHOR_ID);
        createShopGradeWithFactorValues();
        test(false);
    }

    public void test(boolean pubGrade) throws Exception {
        String sql = getSql(pubGrade);
        SaasIndexerAction gradeAction = getAction(pubGrade);

        initGradeIdAndResource(sql);
        ArgumentCaptor<List<SaasDocumentYt>> ytDumpedDocuments = dumpedDiffDocs();

        // summary is disabled in diff
        assertDumpToYtDocs(ytDumpedDocuments, 1);
        assertDumpToYtDocWithAction(ytDumpedDocuments, String.valueOf(gradeId), gradeAction);
    }

    public void testSnapshot(boolean pubGrade, boolean pubSummary) throws Exception {
        String sql = getSql(pubGrade);
        SaasIndexerAction gradeAction = getAction(pubGrade);
        SaasIndexerAction summaryAction = getAction(pubSummary);

        initGradeIdAndResource(sql);
        ArgumentCaptor<List<SaasDocumentYt>> ytDumpedDocuments = dumpedSnapshotDocs();

        // summary is enabled in snapshot
        assertDumpToYtDocs(ytDumpedDocuments, 2);
        assertDumpToYtDocWithAction(ytDumpedDocuments, String.valueOf(gradeId), gradeAction);
        assertDumpToYtDocWithAction(ytDumpedDocuments, "shop" + resourceId, summaryAction);
    }

    private SaasIndexerAction getAction(boolean isPublic) {
        return isPublic ? SaasIndexerAction.MODIFY : SaasIndexerAction.DELETE;
    }

    private Long createShopGradeWithFactorValues() {
        ShopGrade shopGrade = constructShopGrade(SHOP_ID, AUTHOR_ID);
        AbstractGrade createdGrade = gradeCreator.createAndReturnGrade(shopGrade);
        factorService.saveGradeFactorValuesWithCleanup(createdGrade.getId(), shopGrade.getGradeFactorValues());
        return createdGrade.getId();
    }
}
