package ru.yandex.market.pers.qa.controller.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.controller.dto.ResultDto;
import ru.yandex.market.pers.qa.mock.mvc.PostMvcMocks;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.service.QuestionService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Dionisy Yuzhakov / bahus@ / 20.03.2020
 */
public class ComplaintControllerPostTest extends ComplaintControllerTest {

    @Autowired
    private QuestionService questionService;
    @Autowired
    private PostMvcMocks postMvcMocks;

    @Test
    void testCreatePostComplaintUidUnprepared() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long postId = postMvcMocks.createInterestPost();

        boolean isOk = createPostComplaint(UserType.UID, String.valueOf(postId), REASON, text);

        // post was not ready for complaining change in moderation state
        assertFalse(isOk);

        // but complaint was registered anyway
        checkComplaint(UserType.UID, ControllerTest.UID_STR, QaEntityType.QUESTION, postId);
    }

    @Test
    void testCreatePostComplaintUid() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long postId = postMvcMocks.createInterestPost();

        // prepare post for complaining
        questionService.forceUpdateModState(postId, ModState.CONFIRMED);

        boolean isOk = createPostComplaint(UserType.UID, String.valueOf(postId), REASON, text);

        assertTrue(isOk);
        checkComplaint(UserType.UID, ControllerTest.UID_STR, QaEntityType.QUESTION, postId);
    }

    @Test
    void testCreatePostComplaintYandexUid() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long postId = postMvcMocks.createInterestPost();

        // prepare post for complaining
        questionService.forceUpdateModState(postId, ModState.CONFIRMED);

        boolean isOk = createPostComplaint(UserType.YANDEXUID, String.valueOf(postId), REASON, text);

        assertTrue(isOk);
        checkComplaint(UserType.YANDEXUID, ControllerTest.YANDEXUID, QaEntityType.QUESTION, postId);
    }

    private boolean createPostComplaint(UserType userType,
                                        String postId,
                                        int reasonId,
                                        String text) throws Exception {
        final MockHttpServletRequestBuilder requestBuilder = post(String
                .format("/complaint/%s/%s/post/%s",
                        userType.getDescription(),
                        UserType.UID == userType ? UID : YANDEXUID,
                        postId
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(COMPLAIN_BODY, reasonId, text))
                .accept(MediaType.APPLICATION_JSON);

        return objectMapper.readValue(invokeAndRetrieveResponse(requestBuilder, status().is2xxSuccessful()),
                ResultDto.class).getResult();
    }

}
