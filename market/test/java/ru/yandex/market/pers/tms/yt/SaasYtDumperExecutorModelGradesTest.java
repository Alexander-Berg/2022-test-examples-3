package ru.yandex.market.pers.tms.yt;

import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.tms.yt.dumper.dumper.reader.saas.SaasDocumentYt;
import ru.yandex.market.saas.indexer.SaasIndexerAction;

import static ru.yandex.market.pers.grade.core.GradeCreator.constructModelGrade;

@WebAppConfiguration
public class SaasYtDumperExecutorModelGradesTest extends SaasYtDumperExecutorGradesTest {

    private static final String PUBLIC_GRADE = "SELECT id, RESOURCE_ID from v_saas_model_grade_for_index\n" +
            "FETCH FIRST 1 ROWS ONLY";
    private static final String DELETE_GRADE = "SELECT id, RESOURCE_ID from GRADE g\n" +
            "WHERE g.type in (1, 2)\n" +
            "and g.id not in (select id from v_saas_model_grade_for_index)\n" +
            "FETCH FIRST 1 ROWS ONLY";

    @Test
    public void testPubGradePubSummary() throws Exception {
        createModelGradeWithFactorValues();
        test(PUBLIC_GRADE, SaasIndexerAction.MODIFY);
    }

    @Test
    public void testDelGradePubSummary() throws Exception {
        deleteGrade(createModelGrade(), AUTHOR_ID);
        createModelGradeWithFactorValues();
        test(DELETE_GRADE, SaasIndexerAction.DELETE);
    }
    public void test(String sqlQueryForGradeAndResource, SaasIndexerAction gradeAction) throws Exception {
        initGradeIdAndResource(sqlQueryForGradeAndResource);
        ArgumentCaptor<List<SaasDocumentYt>> ytDumpedDocuments = dumpedDiffDocs();

        // summary is disabled in diff
        assertDumpToYtDocs(ytDumpedDocuments, 1);

        assertDumpToYtDocWithAction(ytDumpedDocuments, String.valueOf(gradeId), gradeAction);
    }

    public void testSnapshot(String sqlQueryForGradeAndResource, SaasIndexerAction gradeAction, SaasIndexerAction summaryAction) throws Exception {
        initGradeIdAndResource(sqlQueryForGradeAndResource);
        ArgumentCaptor<List<SaasDocumentYt>> ytDumpedDocuments = dumpedSnapshotDocs();

        // summary is enabled in snapshot
        assertDumpToYtDocs(ytDumpedDocuments, 2);

        assertDumpToYtDocWithAction(ytDumpedDocuments, String.valueOf(gradeId), gradeAction);
        assertDumpToYtDocWithAction(ytDumpedDocuments, "model" + resourceId, summaryAction);
    }

    private Long createModelGradeWithFactorValues() {
        ModelGrade modelGrade = constructModelGrade(MODEL_ID, AUTHOR_ID);
        AbstractGrade createdGrade = gradeCreator.createAndReturnGrade(modelGrade);
        factorService.saveGradeFactorValuesWithCleanup(createdGrade.getId(), modelGrade.getGradeFactorValues());
        return createdGrade.getId();
    }

    private Long createModelGrade() {
        ModelGrade modelGrade = constructModelGrade(MODEL_ID, AUTHOR_ID);
        AbstractGrade createdGrade = gradeCreator.createAndReturnGrade(modelGrade);
        return createdGrade.getId();
    }
}
