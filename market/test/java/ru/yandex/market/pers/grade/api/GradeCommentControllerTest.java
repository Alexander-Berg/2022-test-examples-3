package ru.yandex.market.pers.grade.api;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.comments.model.CommentModState;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.api.model.IdObject;
import ru.yandex.market.pers.grade.client.dto.GradeForCommentDto;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.GradeCommentDao;
import ru.yandex.market.pers.grade.core.model.Comment;
import ru.yandex.market.util.FormatUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author dinyat
 *         24/04/2017
 */
public class GradeCommentControllerTest extends MockedPersGradeTest {

    private static final long FAKE_USER = 12345L;
    private static final long MODEL_ID = 16568L;
    private static final long SHOP_ID = 53453L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GradeCommentDao gradeCommentDao;
    @Autowired
    private GradeCreator gradeCreator;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void save() throws Exception {
        String commentId = "child-1-9-012345";
        Comment comment = new Comment(commentId,
            1L,
            1L,
            null,
            CommentModState.UNMODERATED,
            false,
            Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now()));

        String jsonResponse = mockMvc.perform(put("/api/grade/comment/" + commentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(comment))
            .accept(MediaType.APPLICATION_JSON)).andExpect(status().is2xxSuccessful())
            .andDo(print())
            .andReturn().getResponse().getContentAsString();

        IdObject response = mapper.readValue(jsonResponse, IdObject.class);

        assertEquals(commentId, response.getId());
        assertEquals(comment, gradeCommentDao.get(commentId));
    }

    @Test
    public void testSignal() throws Exception {
        long[] comments = {
            1, 2, 3, 8, 5
        };

        int batchSize = 1000;
        List<Long> commentsToSynch = gradeCommentDao.getCommentsToSynch(batchSize);
        assertEquals(0, commentsToSynch.size());

        String jsonResponse = mockMvc.perform(post("/api/grade/comment/signal")
            .param("commentId", Arrays.stream(comments).mapToObj(Long::toString).toArray(String[]::new))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)).andExpect(status().is2xxSuccessful())
            .andDo(print())
            .andReturn().getResponse().getContentAsString();

        // test queue read
        commentsToSynch = gradeCommentDao.getCommentsToSynch(batchSize);
        assertEquals(comments.length, commentsToSynch.size());
        assertTrue(commentsToSynch.containsAll(Arrays.stream(comments).boxed().collect(Collectors.toList())));
    }

    @Test
    public void testGetGradeCommentWithModel() throws Exception {
        long modelGradeId = gradeCreator.createModelGrade(MODEL_ID, FAKE_USER);
        GradeForCommentDto grade = getGradeForComment(modelGradeId);
        assertGrade(grade, modelGradeId, modelGradeId, MODEL_ID, GradeType.MODEL_GRADE);
    }

    @Test
    public void testGetGradeCommentWithShop() throws Exception {
        long shopGradeId = gradeCreator.createShopGrade(FAKE_USER, SHOP_ID, 2);
        GradeForCommentDto grade = getGradeForComment(shopGradeId);
        assertGrade(grade, shopGradeId, shopGradeId, SHOP_ID, GradeType.SHOP_GRADE);
    }

    @Test
    public void testGetLastGradeCommentWithModel() throws Exception {
        long[] grade = {
            gradeCreator.createModelGrade(MODEL_ID, FAKE_USER),
            gradeCreator.createModelGrade(MODEL_ID, FAKE_USER, ModState.APPROVED, "haha=1"),
            gradeCreator.createModelGrade(MODEL_ID + 1, FAKE_USER),
        };

        // TODO this code would not be required when fix_id will be generated
        // fill fix_id for two grades
        pgJdbcTemplate.update(
            "update grade\n" +
                "set fix_id = ?\n" +
                "where id in (?, ?)",
            grade[0],
            grade[0],
            grade[1]
        );

        // clean up fix_id for last grade
        pgJdbcTemplate.update(
            "update grade\n" +
                "set fix_id = null\n" +
                "where id = ?",
            grade[2]
        );

        assertGrade(getLastGradeForComment(grade[0]), grade[1], grade[0], MODEL_ID, GradeType.MODEL_GRADE);
        assertGrade(getLastGradeForComment(grade[1]), grade[1], grade[0], MODEL_ID, GradeType.MODEL_GRADE);
        assertGrade(getLastGradeForComment(grade[2]), grade[2], null, MODEL_ID + 1, GradeType.MODEL_GRADE);
    }

    @Test
    public void testGetGradesForCommentWithModel() throws Exception {
        long[] grade = {
            gradeCreator.createModelGrade(MODEL_ID, FAKE_USER),
            gradeCreator.createModelGrade(MODEL_ID, FAKE_USER, ModState.APPROVED, "haha=1"),
            gradeCreator.createModelGrade(MODEL_ID, FAKE_USER),
        };

        List<GradeForCommentDto> result = getGradesForComment(grade);
        result.sort(Comparator.comparingLong(GradeForCommentDto::getId));

        assertEquals(grade.length, result.size());
        for (int idx = 0; idx < grade.length; idx++) {
            assertGrade(result.get(idx), grade[idx], grade[0], MODEL_ID, GradeType.MODEL_GRADE);
        }
    }

    private void assertGrade(GradeForCommentDto grade, long gradeId, Long fixId, long resourceId, GradeType type) {
        assertEquals(gradeId, grade.getId());
        assertEquals(fixId, grade.getFixId());
        assertEquals(type.value(), grade.getType());
        assertEquals(FAKE_USER, grade.getUser().getPassportUid().longValue());
        assertEquals(resourceId, grade.getResourceId());
    }

    private GradeForCommentDto getGradeForComment(long gradeId) {
        return FormatUtils.fromJson(invokeAndRetrieveResponse(
            get("/api/grade/" + gradeId + "/for/comments")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()), GradeForCommentDto.class);
    }

    private GradeForCommentDto getLastGradeForComment(long gradeId) {
        return FormatUtils.fromJson(invokeAndRetrieveResponse(
            get("/api/grade/" + gradeId + "/for/comments/last")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()), GradeForCommentDto.class);
    }

    private List<GradeForCommentDto> getGradesForComment(long[] gradeIds) {
        return FormatUtils.fromJson(invokeAndRetrieveResponse(
            get("/api/grade/for/comments")
                .param("gradeId", LongStream.of(gradeIds).mapToObj(Long::toString).toArray(String[]::new))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()), new TypeReference<List<GradeForCommentDto>>() {
        });
    }

    @Test
    public void testGetGradeCommentWithNonexistentGrade() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/grade/530345665432/for/comments")
                .accept(MediaType.APPLICATION_JSON),
            status().is4xxClientError());
    }

    @Test
    public void testGetLastGradeCommentWithNonexistentGrade() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/grade/530345665432/for/comments/last")
                .accept(MediaType.APPLICATION_JSON),
            status().is4xxClientError());
    }
}
