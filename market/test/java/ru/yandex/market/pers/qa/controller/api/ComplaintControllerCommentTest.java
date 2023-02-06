package ru.yandex.market.pers.qa.controller.api;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.dto.ResultDto;
import ru.yandex.market.pers.qa.mock.mvc.PostMvcMocks;
import ru.yandex.market.pers.qa.model.QaEntityFeature;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.service.CommentService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.QaApiUtils.toCommentIdInController;

public class ComplaintControllerCommentTest extends ComplaintControllerTest {

    @Autowired
    private CommentService commentService;
    @Autowired
    private PostMvcMocks postMvcMocks;

    @Test
    void testInvalidQaEntityTypeOnCommentComplaint() throws Exception {
        EnumSet<QaEntityType> types = EnumSet.allOf(QaEntityType.class);
        types.removeAll(getComplanableCommentsStream().collect(Collectors.toList()));
        for (QaEntityType entityType : types) {
            String response = createCommentComplaintByUid(
                entityType,
                "child-0-123456",
                REASON,
                UUID.randomUUID().toString(),
                status().is4xxClientError()
            );
            Assertions.assertTrue(response.contains("Project does not supports complaining"));

            response = createCommentComplaintByYandexUid(
                entityType,
                "child-0-123456",
                REASON,
                UUID.randomUUID().toString(),
                status().is4xxClientError()
            );
            Assertions.assertTrue(response.contains("Project does not supports complaining"));
        }
    }

    @ParameterizedTest
    @MethodSource("createComplaintProject")
    public void testCreateComplaintUid(QaEntityType entityType) throws Exception {
        final String text = UUID.randomUUID().toString();
        final long commentId = buildCommentId(entityType);

        boolean isNowComplained = createComplaintByUid(entityType, toCommentIdInController(commentId), REASON, text);

        assertTrue(isNowComplained);
        checkComplaint(UserType.UID, Long.toString(UID), entityType, commentId);
    }

    @ParameterizedTest
    @MethodSource("createComplaintProject")
    public void testCreateComplaintUidRootNumber(QaEntityType entityType) throws Exception {
        final String text = UUID.randomUUID().toString();
        final long commentId = buildCommentId(entityType);

        boolean isNowComplained = createComplaintByUid(entityType, String.valueOf(commentId), REASON, text);

        assertTrue(isNowComplained);
        checkComplaint(UserType.UID, Long.toString(UID), entityType, commentId);
    }

    @ParameterizedTest
    @MethodSource("createComplaintProject")
    public void testCreateComplaintYandexUid(QaEntityType entityType) throws Exception {
        final String text = UUID.randomUUID().toString();
        final long commentId = buildCommentId(entityType);

        boolean isNowComplained = createComplaintByYandexUid(entityType, toCommentIdInController(commentId), REASON, text);

        assertTrue(isNowComplained);
        checkComplaint(UserType.YANDEXUID, YANDEXUID, entityType, commentId);
    }

    @ParameterizedTest
    @MethodSource("createComplaintProject")
    public void testCreateComplaintUidAndYandexUid(QaEntityType entityType) throws Exception {
        // create with UID
        String textForUID = UUID.randomUUID().toString();
        final long commentIdForUID = buildCommentId(entityType);

        boolean isNowComplainedUID = createComplaintByUid(
            entityType,
            toCommentIdInController(commentIdForUID),
            REASON,
            textForUID
        );

        assertTrue(isNowComplainedUID);
        checkComplaint(UserType.UID, Long.toString(UID), entityType, commentIdForUID);

        // create with YandexUID
        String textForYandexUID = UUID.randomUUID().toString();
        final long commentIdYandexUID = buildCommentId(entityType);

        boolean isNowComplainedYandexUID = createComplaintByYandexUid(
            entityType,
            toCommentIdInController(commentIdYandexUID),
            REASON,
            textForYandexUID
        );

        assertTrue(isNowComplainedYandexUID);
        checkComplaint(UserType.YANDEXUID, YANDEXUID, entityType, commentIdYandexUID);
    }

    @Test
    public void testCreateComplaintYandexUidInvalidId() throws Exception {
        String response = createComplaintByYandexUid4xx(
            QaEntityType.COMMENT,
            "child-0-111hfjdienf111",
            REASON,
            UUID.randomUUID().toString()
        );
        assertThat(response, containsString("comment id must be a number or match to child-0-"));
    }

    @Test
    public void testCreateComplaintUidInvalidCommentId() throws Exception {
        String response = createComplaintByUid4xx(
            QaEntityType.COMMENT,
            "child-0-111hfjdienf111",
            REASON,
            UUID.randomUUID().toString()
        );
        assertThat(response, containsString("comment id must be a number or match to child-0-"));
    }

    @ParameterizedTest
    @MethodSource("createComplaintProject")
    public void testCreateDuplicateComplaintUid(QaEntityType entityType) throws Exception {
        final long commentId = buildCommentId(entityType);
        boolean isNowComplained = createComplaintByUid(
            entityType,
            toCommentIdInController(commentId),
            REASON,
            UUID.randomUUID().toString()
        );

        assertTrue(isNowComplained);
        checkComplaint(UserType.UID, Long.toString(UID), entityType, commentId);

        // try to complain again
        String response = createComplaintByUid4xx(
            entityType,
            toCommentIdInController(commentId),
            REASON,
            UUID.randomUUID().toString()
        );
        assertThat(response, containsString("already exist"));
    }

    @ParameterizedTest
    @MethodSource("createComplaintProject")
    public void testCreateDuplicateComplaintYandexUid(QaEntityType entityType) throws Exception {
        final long commentId = buildCommentId(entityType);
        boolean isNowComplained = createComplaintByYandexUid(
            entityType,
            toCommentIdInController(commentId),
            REASON,
            UUID.randomUUID().toString()
        );

        assertTrue(isNowComplained);
        checkComplaint(UserType.YANDEXUID, YANDEXUID, entityType, commentId);

        // try to complain again
        String response = createComplaintByYandexUid4xx(
            entityType,
            toCommentIdInController(commentId),
            REASON,
            UUID.randomUUID().toString()
        );
        assertThat(response, containsString("already exist"));
    }

    @ParameterizedTest
    @MethodSource("createComplaintProjectNew")
    public void testCreateArticleCommentComplaintByUid(QaEntityType entityType) throws Exception {
        String pathName = QaEntityType.getCommentProjectByType(entityType).getName();
        final String text = UUID.randomUUID().toString();
        final long commentId = buildCommentId(entityType);

        boolean isNowComplained = objectMapper.readValue(
            invokeAndRetrieveResponse(
                post(String.format("/complaint/%s/comment/%s/UID/%s", pathName, commentId, UID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format(COMPLAIN_BODY, REASON, text))
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()),
            ResultDto.class
        ).getResult();

        assertTrue(isNowComplained);
        checkComplaint(UserType.UID, Long.toString(UID), entityType, commentId);
    }

    @ParameterizedTest
    @MethodSource("createComplaintProjectNew")
    public void testCreateArticleCommentComplaintByYandexUid(QaEntityType entityType) throws Exception {
        String pathName = QaEntityType.getCommentProjectByType(entityType).getName();
        final String text = UUID.randomUUID().toString();
        final long commentId = buildCommentId(entityType);

        boolean isNowComplained = objectMapper.readValue(
            invokeAndRetrieveResponse(
                post(String.format("/complaint/%s/comment/%s/YANDEXUID/%s", pathName, commentId, YANDEXUID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format(COMPLAIN_BODY, REASON, text))
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()),
            ResultDto.class
        ).getResult();

        assertTrue(isNowComplained);
        checkComplaint(UserType.YANDEXUID, YANDEXUID, entityType, commentId);
    }

    private long getRootIdForTest(QaEntityType entityType) throws Exception {
        switch (entityType) {
            case COMMENT:
                final long questionId = createQuestion("Question! " + UUID.randomUUID().toString());
                return createAnswer(questionId, "answer!");
            case COMMENT_POST:
                return postMvcMocks.createInterestPost();
            default:
                return 123415;
        }
    }

    private long buildCommentId(QaEntityType entityType) throws Exception {
        return buildCommentId(entityType, getRootIdForTest(entityType));
    }

    private long buildCommentId(QaEntityType entityType, long rootId) {
        try {
            CommentProject project = QaEntityType.getCommentProjectByType(entityType);
            return commentService.createComment(project, UID, "//Comment", rootId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testComplainableComments() {
        Set<QaEntityType> types = getComplanableCommentsStream()
            .collect(Collectors.toSet());
        List<QaEntityType> expectedTypes = Arrays.asList(
            QaEntityType.COMMENT,
            QaEntityType.COMMENT_ARTICLE,
            QaEntityType.COMMENT_VERSUS,
            QaEntityType.COMMENT_POST,
            QaEntityType.COMMENT_VIDEO,
            QaEntityType.COMMENT_GRADE
        );
        assertTrue(types.containsAll(expectedTypes));
        assertEquals(expectedTypes.size(), types.size());
    }

    @NotNull
    private static Stream<QaEntityType> getComplanableCommentsStream() {
        return Arrays.stream(QaEntityType.values())
            .filter(QaEntityType::isComment)
            .filter(x -> x.support(QaEntityFeature.COMPLAINING));
    }

    private static Stream<Arguments> createComplaintProject() {
        return getComplanableCommentsStream()
            .map(Arguments::of);
    }

    private static Stream<Arguments> createComplaintProjectNew() {
        return Stream.of(QaEntityType.COMMENT_ARTICLE)
            .map(Arguments::of);
    }

}
