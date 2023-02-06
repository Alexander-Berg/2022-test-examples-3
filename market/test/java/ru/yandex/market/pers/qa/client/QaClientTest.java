package ru.yandex.market.pers.qa.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CommentTreeDto;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.client.dto.UserBanInfoDto;
import ru.yandex.market.pers.qa.client.dto.complaint.ArticleCommentComplaintDto;
import ru.yandex.market.pers.qa.client.dto.complaint.QaCommentComplaintDto;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.ComplaintType;
import ru.yandex.market.pers.qa.client.model.QuestionType;
import ru.yandex.market.pers.qa.client.model.UserType;

import java.io.ByteArrayInputStream;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.and;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.mockResponse;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.mockResponseWithFile;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withMethod;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withQueryParam;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.COMMENT_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.COMMENT_TYPE_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.COMPLAINT_COMMENT_TYPE_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.FROM_USER_ID;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.MODERATOR_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.ROOT_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.TO_USER_ID;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.USER_TYPE_KEY;

class QaClientTest {

    public static final int UID = 1;
    private final HttpClient httpClient = mock(HttpClient.class);
    private final QaClient qaClient = new QaClient("localhost", 1234,
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)));

    @Test
    public void testGetCommentById() {
        long commentId = 100061275;

        mockResponseWithFile(
            httpClient,
            200,
            "/data/qa_get_comment_by_id.json",
            withPath("/internal/comment/get/" + commentId));

        CommentDto commentDto = qaClient.getCommentById(commentId);
        assertNotNull(commentDto);
        assertEquals(commentId, commentDto.getId());
    }

    @Test
    public void testGetCommentByIdNotFound() {
        long commentId = 2434;

        mockResponseWithFile(httpClient,
            404,
            "/data/qa_get_comment_by_id_not_found.json",
            withPath("/internal/comment/get/" + commentId));

        HttpClientErrorException e = assertThrows(HttpClientErrorException.class, () ->  qaClient.getCommentById(commentId));
        assertTrue(e.getStatusCode().is4xxClientError());
    }

    @Test
    public void testGetCommentBulk() {
        List<Long> sourceCommentIds = Arrays.asList(100061275L, 100061276L);

        mockResponseWithFile(
            httpClient,
            200,
            "/data/qa_get_comment_bulk.json",
            and(
                withPath("/internal/comment/get/bulk"),
                withQueryParam(COMMENT_TYPE_KEY, CommentProject.GRADE.getId()),
                withQueryParam(COMMENT_ID_KEY, sourceCommentIds.get(0)),
                withQueryParam(COMMENT_ID_KEY, sourceCommentIds.get(1))
            ));


        List<CommentDto> comments = qaClient.getCommentBulk(CommentProject.GRADE, sourceCommentIds);
        assertNotNull(comments);
        assertEquals(2, comments.size());
        Set<Long> resultCommentIds = comments.stream().map(CommentDto::getId).collect(Collectors.toSet());
        assertTrue(resultCommentIds.containsAll(sourceCommentIds));
    }

    @Test
    public void testGetLastCommentBulk() {
        List<Long> rootIds = Arrays.asList(1245135L, 34981L);
        List<Long> sourceCommentIds = Arrays.asList(100061275L, 100061276L);

        mockResponseWithFile(
            httpClient,
            200,
            "/data/qa_get_comment_bulk.json",
            and(
                withPath("/internal/comment/get/last/bulk"),
                withQueryParam(COMMENT_TYPE_KEY, CommentProject.GRADE.getId()),
                withQueryParam(ROOT_ID_KEY, rootIds.get(0)),
                withQueryParam(ROOT_ID_KEY, rootIds.get(1))
            ));

        List<CommentDto> comments = qaClient.getLastCommentBulk(CommentProject.GRADE, rootIds);
        assertNotNull(comments);
        assertEquals(2, comments.size());
        Set<Long> resultCommentIds = comments.stream().map(CommentDto::getId).collect(Collectors.toSet());
        assertTrue(resultCommentIds.containsAll(sourceCommentIds));
    }

    @Test
    public void testQaCommentComplaints() {
        mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/qa_comment_complaints_moderation.json",
            withQueryParam(COMPLAINT_COMMENT_TYPE_KEY, ComplaintType.COMMENT_QA.getValue()));

        QAPager<QaCommentComplaintDto> result = qaClient.getQaComplaints(UID);

        assertEquals(2, result.getData().size());
        assertEquals(QaCommentComplaintDto.class, result.getData().get(0).getClass(), "check parsed well");
        assertEquals(91, result.getData().get(0).getId());
        assertEquals("Другое: жалоба на родителя", result.getData().get(0).getComplaintText());
        assertEquals(QuestionType.CATEGORY, result.getData().get(0).getQuestionTypeEnum().orElse(null));
        assertEquals(123, result.getData().get(0).getQuestionEntityIdLong().longValue());
    }

    @Test
    public void testArticleCommentComplaints() {
        mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/article_comment_complaints_moderation.json",
            withQueryParam(COMPLAINT_COMMENT_TYPE_KEY, ComplaintType.COMMENT_ARTICLE.getValue()));

        QAPager<ArticleCommentComplaintDto> result = qaClient.getArticleComplaints(UID);

        assertEquals(2, result.getData().size());
        assertEquals(ArticleCommentComplaintDto.class, result.getData().get(0).getClass(), "check parsed well");
        assertEquals(91, result.getData().get(0).getId());
        assertEquals("Другое: жалоба на родителя", result.getData().get(0).getComplaintText());
        assertEquals("Какой-то текст", result.getData().get(0).getParentCommentText());
    }

    @Test
    public void testPapiGradeComments() {
        mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/papi_grade_comments.json",
            and(
                withQueryParam("gradeId", "1"),
                withQueryParam("gradeId", "2")
            ));

        long shopId = 9;
        List<Long> gradeIds = Arrays.asList(1L, 2L);

        List<CommentTreeDto> comments = qaClient.getPapiGradeComments(shopId, gradeIds).getData();

        assertEquals(2, comments.size());

        assertEquals(gradeIds.get(0), comments.get(0).getEntityId());
        assertEquals(4, comments.get(0).getComments().size());
        assertTrue(Sets.newHashSet(1L, 2L, 3L, 4L).containsAll(
            Lists.transform(comments.get(0).getComments(), CommentDto::getId)
        ));

        assertEquals(gradeIds.get(1), comments.get(1).getEntityId());
        assertEquals(3, comments.get(1).getComments().size());
        assertTrue(Sets.newHashSet(5L, 6L, 7L).containsAll(
            Lists.transform(comments.get(1).getComments(), CommentDto::getId)
        ));
    }

    @Test
    public void testBanUserForever() {
        long moderatorId = 111;
        UserType userType = UserType.UID;
        String userId = String.valueOf(222);
        String description = "очень важная причина";

        mockResponse(httpClient, HttpStatus.SC_OK, req -> new ByteArrayInputStream(new byte[0]),
            and(withMethod(HttpMethod.POST),
                withPath("/userlist/ban"),
                withQueryParam(MODERATOR_ID_KEY, moderatorId),
                withQueryParam(USER_TYPE_KEY, userType.name()),
                withQueryParam(ID_KEY, userId)
            )
        );

        qaClient.banUserForever(moderatorId, userType, userId, description);
    }

    @Test
    public void testBanUserToDate() {
        long moderatorId = 111;
        UserType userType = UserType.UID;
        String userId = String.valueOf(222);
        String description = "очень важная причина";

        mockResponse(httpClient, HttpStatus.SC_OK, req -> new ByteArrayInputStream(new byte[0]),
            and(withMethod(HttpMethod.POST),
                withPath("/userlist/ban"),
                withQueryParam(MODERATOR_ID_KEY, moderatorId),
                withQueryParam(USER_TYPE_KEY, userType.name()),
                withQueryParam(ID_KEY, userId)
            )
        );

        qaClient.banUser(moderatorId, userType, userId, description, Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    }

    @Test
    public void testUnbanUser() {
        long moderatorId = 111;
        UserType userType = UserType.UID;
        String userId = String.valueOf(222);

        mockResponse(httpClient, HttpStatus.SC_OK, req -> new ByteArrayInputStream(new byte[0]),
            and(withMethod(HttpMethod.DELETE),
                withPath("/userlist/ban"),
                withQueryParam(MODERATOR_ID_KEY, moderatorId),
                withQueryParam(USER_TYPE_KEY, userType.name()),
                withQueryParam(ID_KEY, userId)
            )
        );

        qaClient.unbanUser(moderatorId, userType, userId);
    }

    @Test
    public void testTrustUserForever() {
        long moderatorId = 111;
        UserType userType = UserType.UID;
        String userId = String.valueOf(222);
        String description = "очень важная причина";

        mockResponse(httpClient, HttpStatus.SC_OK, req -> new ByteArrayInputStream(new byte[0]),
            and(withMethod(HttpMethod.POST),
                withPath("/userlist/trust"),
                withQueryParam(MODERATOR_ID_KEY, moderatorId),
                withQueryParam(USER_TYPE_KEY, userType.name()),
                withQueryParam(ID_KEY, userId)
            )
        );

        qaClient.trustUserForever(moderatorId, userType, userId, description);
    }

    @Test
    public void testTrustUserToDate() {
        long moderatorId = 111;
        UserType userType = UserType.UID;
        String userId = String.valueOf(222);
        String description = "очень важная причина";

        mockResponse(httpClient, HttpStatus.SC_OK, req -> new ByteArrayInputStream(new byte[0]),
            and(withMethod(HttpMethod.POST),
                withPath("/userlist/trust"),
                withQueryParam(MODERATOR_ID_KEY, moderatorId),
                withQueryParam(USER_TYPE_KEY, userType.name()),
                withQueryParam(ID_KEY, userId)
            )
        );

        qaClient.trustUser(moderatorId, userType, userId, description, Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    }

    @Test
    public void testMistrustUser() {
        long moderatorId = 111;
        UserType userType = UserType.UID;
        String userId = String.valueOf(222);

        mockResponse(httpClient, HttpStatus.SC_OK, req -> new ByteArrayInputStream(new byte[0]),
            and(withMethod(HttpMethod.DELETE),
                withPath("/userlist/trust"),
                withQueryParam(MODERATOR_ID_KEY, moderatorId),
                withQueryParam(USER_TYPE_KEY, userType.name()),
                withQueryParam(ID_KEY, userId)
            )
        );

        qaClient.mistrustUser(moderatorId, userType, userId);
    }

    @Test
    public void testGetUserBanInfo() {
        UserType userType = UserType.UID;
        String userId = String.valueOf(222);

        mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/qa_ban_user_info.json",
            and(withMethod(HttpMethod.GET),
                withPath("/userlist/info"),
                withQueryParam(USER_TYPE_KEY, userType.name()),
                withQueryParam(ID_KEY, userId)
            )
        );

        final UserBanInfoDto banInfo = qaClient.getBanInfo(userType, userId);
        assertTrue(banInfo.isBanned());
        assertFalse(banInfo.isTrusted());
    }

    @Test
    public void testGetQaCommentComplaintsCount() {
        mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/qa_comment_complaints_count.json",
            and(withMethod(HttpMethod.GET),
                withPath("/moderation/complaint/comment/count"),
                withQueryParam(COMPLAINT_COMMENT_TYPE_KEY, ComplaintType.COMMENT_QA.getValue()))
        );

        final CountDto countDto = qaClient.getQaComplaintsCount();
        assertEquals(36, countDto.getCount());
    }

    @Test
    public void testGetArticleCommentComplaintsCount() {
        mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/article_comment_complaints_count.json",
            and(withMethod(HttpMethod.GET),
                withPath("/moderation/complaint/comment/count"),
                withQueryParam(COMPLAINT_COMMENT_TYPE_KEY, ComplaintType.COMMENT_ARTICLE.getValue()))
        );

        final CountDto countDto = qaClient.getArticleComplaintsCount();
        assertEquals(37, countDto.getCount());
    }

    @Test
    public void testMergeComments() {
        long userA = 122L;
        long userB = 133L;
        mockResponse(httpClient, HttpStatus.SC_OK, req -> new ByteArrayInputStream(new byte[0]),
                and(withMethod(HttpMethod.POST),
                        withPath("/comment/merge"),
                        withQueryParam(FROM_USER_ID, userA),
                        withQueryParam(TO_USER_ID, userB)
                )
        );
        qaClient.mergeComments(userA, userB);
    }
}
