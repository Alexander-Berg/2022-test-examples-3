package ru.yandex.market.pers.tms.yt;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.saas.grade.GradeIndexingUtils;
import ru.yandex.market.pers.tms.yt.dumper.dumper.reader.saas.SaasDocumentYt;

import static org.junit.Assert.assertTrue;
import static ru.yandex.market.pers.grade.core.GradeCreator.constructModelGrade;
import static ru.yandex.market.pers.grade.core.GradeCreator.constructShopGrade;

@WebAppConfiguration
public class SaasYtIndexingIntegrationTest extends SaasYtDumperExecutorGradesTest {
    private static final String TEST_IMPORT_SOURCE = "import;vendor;kotiki";
    private static final String TEST_GRADE_TEXT = "62fa88b4-de0c-46bf-925e-7671bc9246c5";
    private static final String TEST_GRADE_PRO = "f55fc909-dc50-44a2-b636-e1916ebebfcc";
    private static final String TEST_GRADE_CONTRA = "3c9c746e-fe26-4478-9287-2e05e4c29531";
    private long gradeCreatedTime;

    @Test
    public void testPublishModelGradeAndSummary() throws Exception {
        createApprovedModelGrade();
        ArgumentCaptor<List<SaasDocumentYt>> ytDumpedDocuments = dumpedDiffDocs();

        // summary is disabled in diff
        assertDumpToYtDocs(ytDumpedDocuments, 1);
        assertDumpToYtDoc(ytDumpedDocuments, "testdata/saas/model_grade_modify.json");
    }

    @Test
    public void testPublishImportedModelGradeAndSummary() throws Exception {
        createApprovedImportedModelGrade(TEST_IMPORT_SOURCE);
        pgJdbcTemplate.update("INSERT INTO GRADE_IMPORT_PARTNER_NAME (source, partner_name) VALUES (?, ?)", TEST_IMPORT_SOURCE, "Kotiki");
        ArgumentCaptor<List<SaasDocumentYt>> ytDumpedDocuments = dumpedDiffDocs();

        // summary is disabled in diff
        assertDumpToYtDocs(ytDumpedDocuments, 1);
        assertDumpToYtDoc(ytDumpedDocuments, "testdata/saas/imported_model_grade_modify.json");
    }

    @Test
    public void testPublishShopGradeAndDeleteSummary() throws Exception {
        createApprovedShopGrade();

        // summary is disabled in diff
        ArgumentCaptor<List<SaasDocumentYt>> ytDumpedDocuments = dumpedDiffDocs();
        assertDumpToYtDocs(ytDumpedDocuments, 1);
        assertDumpToYtDoc(ytDumpedDocuments, "testdata/saas/shop_grade_modify.json");
    }

    private void createApprovedShopGrade() {
        ShopGrade grade = constructShopGrade(SHOP_ID, AUTHOR_ID);
        setGradeFieldsForTest(grade);
        AbstractGrade createdGrade = gradeCreator.createAndReturnGrade(grade);
        initTestParamsByGrade(createdGrade);
    }

    private void createApprovedModelGrade() {
        ModelGrade grade = constructModelGrade(MODEL_ID, AUTHOR_ID);
        grade.setPhotos(List.of());
        setGradeFieldsForTest(grade);
        grade.setCreated(new Date(Instant.now().minus(100, ChronoUnit.DAYS).toEpochMilli()));
        AbstractGrade createdGrade = gradeCreator.createAndReturnGrade(grade);
        factorService.saveGradeFactorValuesWithCleanup(createdGrade.getId(), grade.getGradeFactorValues());
        pgJdbcTemplate.update(
            "insert into GRADE.KARMA_GRADE_VOTE(GRADE_ID, AGREE, REJECT, LAST_UPDATE_TABLE) values (?, ?, ?, '1')",
            createdGrade.getId(),
            42,
            12);
        initTestParamsByGrade(createdGrade);
    }

    private void createApprovedImportedModelGrade(String source) {
        ModelGrade grade = constructModelGrade(MODEL_ID, AUTHOR_ID);
        setGradeFieldsForTest(grade);
        grade.setSource(source);
        grade.setPhotos(List.of());
        grade.setCreated(new Date(Instant.now().minus(100, ChronoUnit.DAYS).toEpochMilli()));
        AbstractGrade createdGrade = gradeCreator.createAndReturnGrade(grade);
        factorService.saveGradeFactorValuesWithCleanup(createdGrade.getId(), grade.getGradeFactorValues());
        initTestParamsByGrade(createdGrade);
    }

    private void setGradeFieldsForTest(AbstractGrade grade) {
        grade.setText(TEST_GRADE_TEXT);
        grade.setPro(TEST_GRADE_PRO);
        grade.setContra(TEST_GRADE_CONTRA);
    }

    private void initTestParamsByGrade(AbstractGrade createdGrade) {
        this.gradeId = createdGrade.getId();
        this.resourceId = SHOP_ID;
        this.gradeCreatedTime = createdGrade.getCreated().getTime();
    }

    private void assertDumpToYtDoc(ArgumentCaptor<List<SaasDocumentYt>> captor, String expectedJsonFileName) {
        String expectedJson = getAdjustedJson(expectedJsonFileName);
        List<SaasDocumentYt> allDocs = captor.getAllValues().stream().flatMap(List::stream).collect(Collectors.toList());
        assertTrue("Expected to find:\n" + expectedJson + "\nActual docs:\n" + allDocs,
                allDocs.stream().anyMatch(doc -> documentMatches(expectedJson, (SaasDocumentYt) doc)));
    }

    private String getResourceAsString(String filename) {
        try {
            return IOUtils.readInputStream(getClass().getClassLoader().getResourceAsStream(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean documentMatches(String expectedJson, SaasDocumentYt doc) {
        try {
            return JSONCompare.compareJSON(expectedJson, doc.toString(), JSONCompareMode.LENIENT).passed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getAdjustedJson(String filename) {
        return getResourceAsString(filename)
                .replace("?gradeId?", String.valueOf(gradeId))
                .replace("?creationTime?", String.valueOf(gradeCreatedTime))
                .replace("?creationTimeRated?",
                        String.valueOf(GradeIndexingUtils.getCreationTimeRated(gradeCreatedTime)));
    }

}
