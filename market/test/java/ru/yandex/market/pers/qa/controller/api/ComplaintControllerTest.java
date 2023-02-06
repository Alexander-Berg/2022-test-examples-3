package ru.yandex.market.pers.qa.controller.api;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.ResultDto;
import ru.yandex.market.pers.qa.model.QaEntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.10.2018
 */
public class ComplaintControllerTest extends QAControllerTest {
    static final int REASON = 1;
    static final String REASON_NAME = "Другое";

    void checkComplaint(UserType userType, String userId, QaEntityType type, long entityId) {
        long count = qaJdbcTemplate.queryForObject("SELECT count(*) FROM qa.complaint " +
                "WHERE user_type = ? AND user_id = ? AND type = ? AND entity_id = ?",
            Long.class, userType.getValue(), userId, type.getValue(), String.valueOf(entityId));
        assertEquals(1, count);
    }

    public boolean createComplaintByUid(QaEntityType entityType,
                                 String entityId,
                                 int reasonId,
                                 String text) throws Exception {
        return objectMapper.readValue(
            createComplaintByUid(
                entityType,
                entityId,
                reasonId,
                text,
                status().is2xxSuccessful()
            ),
            ResultDto.class
        ).getResult();
    }

    String createComplaintByUid4xx(QaEntityType entityType,
                                   String entityId,
                                   int reasonId,
                                   String text) throws Exception {
        return createComplaintByUid(entityType, entityId, reasonId, text, status().is4xxClientError());
    }

    boolean createComplaintByYandexUid(QaEntityType entityType,
                                       String entityId,
                                       int reasonId,
                                       String text) throws Exception {
        return objectMapper.readValue(
            createComplaintByYandexUid(
                entityType,
                entityId,
                reasonId,
                text,
                status().is2xxSuccessful()
            ),
            ResultDto.class
        ).getResult();
    }

    String createComplaintByYandexUid4xx(QaEntityType entityType,
                                         String entityId,
                                         int reasonId,
                                         String text) throws Exception {
        return createComplaintByYandexUid(entityType, entityId, reasonId, text, status().is4xxClientError());
    }

    String createComplaintByUid(QaEntityType entityType,
                                String entityId,
                                int reasonId,
                                String text,
                                ResultMatcher resultMatcher) throws Exception {
        final MockHttpServletRequestBuilder requestBuilder = post(String
            .format("/complaint/UID/%d/%s/%s",
                UID,
                getControllerEntityType(entityType),
                entityId
            ));

        if (entityType.isComment()) {
            requestBuilder.param("complaintCommentType", String.valueOf(entityType.getValue()));
        }

        return invokeAndRetrieveResponse(
            requestBuilder
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(COMPLAIN_BODY, reasonId, text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    String createComplaintByYandexUid(QaEntityType entityType,
                                      String entityId,
                                      int reasonId,
                                      String text,
                                      ResultMatcher resultMatcher) throws Exception {
        final MockHttpServletRequestBuilder requestBuilder = post(String.format(
            "/complaint/YANDEXUID/%s/%s/%s",
            YANDEXUID,
            getControllerEntityType(entityType),
            entityId
        ));

        if (entityType.isComment()) {
            requestBuilder.param("complaintCommentType", String.valueOf(entityType.getValue()));
        }

        return invokeAndRetrieveResponse(
            requestBuilder
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(COMPLAIN_BODY, reasonId, text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    String createCommentComplaintByUid(QaEntityType entityType,
                                       String entityId,
                                       int reasonId,
                                       String text,
                                       ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            post(String.format("/complaint/UID/%d/comment/%s", UID, entityId))
                .param("complaintCommentType", String.valueOf(entityType.getValue()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(COMPLAIN_BODY, reasonId, text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    String createCommentComplaintByYandexUid(QaEntityType entityType,
                                             String entityId,
                                             int reasonId,
                                             String text,
                                             ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            post(String.format("/complaint/YANDEXUID/%s/comment/%s", YANDEXUID, entityId))
                .param("complaintCommentType", String.valueOf(entityType.getValue()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(COMPLAIN_BODY, reasonId, text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    String getControllerEntityType(QaEntityType entityType) {
        if (entityType.isComment()) {
            return QaEntityType.COMMENT.getSimpleName();
        }
        return entityType.getSimpleName();
    }

}
