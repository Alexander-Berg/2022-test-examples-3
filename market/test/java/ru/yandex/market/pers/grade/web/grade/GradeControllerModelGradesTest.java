package ru.yandex.market.pers.grade.web.grade;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GradeControllerModelGradesTest extends MockedPersGradeTest {

    public static final long UNKNOWN_REJECTION_REASON = -1;
    private static final long FAKE_MODEL = 2;
    private static final long FAKE_MODEL_1 = 3;
    private long USER_ID = 0;

    @Autowired
    private GradeCreationHelper gradeCreationHelper;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private DbGradeService dbGradeService;

    private void createRejectedGradesWithBadReasons() {
        List<Long> badReasonIds = Arrays.asList(18L, 160L, 200L, 280L);
        addBanReasons(badReasonIds);
        badReasonIds.forEach(reason -> gradeCreationHelper.createGradeAndReject(createTestModelGrade(FAKE_MODEL, USER_ID++), reason));
    }

    @Test
    public void testRatedModelGrades1() throws Exception {
        // grades included in the rating
        gradeCreationHelper.createApprovedVerifiedGrade(createTestModelGrade(FAKE_MODEL, USER_ID++));
        createApprovedGradeWithPreviousVersion();
        gradeCreationHelper.createGradeAndAutoReject(createTestModelGrade(FAKE_MODEL, USER_ID++));

        // not in the rating
        gradeCreationHelper.createApprovedVerifiedGrade(createTestModelGrade(FAKE_MODEL, null));
        createRejectedGradesWithBadReasons();
        gradeCreationHelper.createSpamGrade(createTestModelGrade(FAKE_MODEL, USER_ID++));
        gradeCreationHelper.createGradeAndReject(createTestModelGrade(FAKE_MODEL, USER_ID++), UNKNOWN_REJECTION_REASON);


        for (int pageNum = 1; pageNum < 5; pageNum++) {
            assertJsonEquals(file("/data/model_grades" + (pageNum - 1) + ".json"), getModelGrades(pageNum, 1));
        }
    }

    private void assertJsonEquals(String expected, String actual) {
        JSONAssert.assertEquals(expected, actual,
            new CustomComparator(JSONCompareMode.STRICT,
                new Customization("data[*].created", (o1, o2) -> true),
                new Customization("data[*].id", (o1, o2) -> true)));
    }

    private void createApprovedGradeWithPreviousVersion() {
        gradeCreationHelper.createApprovedGrade(createTestModelGrade(FAKE_MODEL, USER_ID)); //previous
        ModelGrade grade = createTestModelGrade(FAKE_MODEL, USER_ID++);
        grade.setText("new_contra");
        gradeCreationHelper.createApprovedGrade(grade);
    }

    private String getModelGrades(int page, int pageSize) {
        return invokeAndRetrieveResponse(
            get("/api/grade/model/" + FAKE_MODEL)
                .param("page_num", String.valueOf(page))
                .param("page_size", String.valueOf(pageSize)),
            status().is2xxSuccessful());
    }

    private static ModelGrade createTestModelGrade(long modelId, Long userId) {
        return GradeCreationHelper.constructModelGrade(modelId, userId, null);
    }

    private String file(String file) throws IOException {
        return IOUtils.readInputStream(
            getClass().getResourceAsStream(file));
    }

    private void addBanReasons(List<Long> badReasonIds) {
        pgJdbcTemplate.batchUpdate(
            "insert into grade.mod_rejection_reason (id, type, name, recomendation, active, rated)\n" +
                "values (?, 1, ?, ?, 1, 0)",
            badReasonIds,
            badReasonIds.size(),
            (ps, reasonId) -> {
                int idx = 1;
                ps.setLong(idx++, reasonId);
                ps.setString(idx++, String.valueOf(reasonId));
                ps.setString(idx++, String.valueOf(reasonId));
            });
    }

}
