package ru.yandex.market.pers.qa.mock.mvc;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.QUESTION_ID_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 15.11.2019
 */
@Service
public class QuestionMvcMocks extends AbstractMvcMocks {
    protected static final String MODEL_QUESTION_BODY = "{\n" +
        "  \"text\": \"%s\"\n" +
        "}";

    protected static final String MODEL_QUESTION_BODY_HID = "{\n" +
        "  \"hid\": %s,\n" +
        "  \"text\": \"%s\"\n" +
        "}";

    protected static final String MODEL_QUESTION_BODY_SKU = "{\n" +
        "  \"sku\": %s,\n" +
        "  \"text\": \"%s\"\n" +
        "}";

    protected static final String MODEL_QUESTION_BODY_FULL = "{\n" +
        "  \"hid\": %s,\n" +
        "  \"sku\": %s,\n" +
        "  \"text\": \"%s\"\n" +
        "}";

    public QuestionDto getQuestion(long questionId, long userId) throws Exception {
        return getQuestion(questionId, userId, status().is2xxSuccessful());
    }

    public QuestionDto getQuestion(long questionId, long userId, ResultMatcher resultMatcher) throws Exception {
        return FormatUtils.fromJson(invokeAndRetrieveResponse(
            get("/question/" + questionId + "/UID/" + userId),
            resultMatcher
        ), QuestionDto.class);
    }

    public QuestionDto getQuestionYandexUid(long questionId, String yandexUid) throws Exception {
        return getQuestionYandexUid(questionId, yandexUid, status().is2xxSuccessful());
    }

    public QuestionDto getQuestionYandexUid(long questionId,
                                            String yandexUid,
                                            ResultMatcher resultMatcher) throws Exception {
        return FormatUtils.fromJson(invokeAndRetrieveResponse(
            get("/question/" + questionId + "/YANDEXUID/" + yandexUid),
            resultMatcher
        ), QuestionDto.class);
    }

    public void deleteQuestion(long id) throws Exception {
        deleteQuestion(id, ControllerTest.UID, status().is2xxSuccessful());
    }

    public String deleteQuestion(long id, long uid, ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            delete("/question/" + id).param("userId", String.valueOf(uid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public List<QuestionDto> getQuestionsBulk(List<Long> questionIds,
                                              UserType type,
                                              ResultMatcher resultMatcher) throws Exception {
        String user = type == UserType.UID ? "UID/" + ControllerTest.UID : "YANDEXUID/" + ControllerTest.YANDEXUID;
        DtoList<QuestionDto> result = objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/" + user + "/bulk")
                .param(QUESTION_ID_KEY, questionIds.stream().map(Object::toString).toArray(String[]::new)),
            resultMatcher
        ), new TypeReference<DtoList<QuestionDto>>() {
        });
        return result.getData();
    }

    public long createModelQuestion() throws Exception {
        return createModelQuestion(ControllerTest.MODEL_ID, UUID.randomUUID().toString(), ControllerTest.CATEGORY_HID);
    }

    public long createModelQuestion(long modelId) throws Exception {
        return createModelQuestion(modelId, ControllerTest.UID);
    }

    public long createModelQuestionHid(long modelId, long hid) throws Exception {
        return createModelQuestion(modelId, UUID.randomUUID().toString(), hid);
    }

    public long createModelQuestion(long modelId, long userId) throws Exception {
        return createModelQuestion(modelId, userId, UUID.randomUUID().toString());
    }

    public long createModelQuestion(String text) throws Exception {
        final String response = createModelQuestion(ControllerTest.MODEL_ID,
            ControllerTest.UID,
            text,
            ControllerTest.CATEGORY_HID,
            status().is2xxSuccessful());
        return objectMapper.readValue(response, QuestionDto.class).getId();
    }

    public long createModelQuestion(long modelId, String text, long hid) throws Exception {
        return createModelQuestion(modelId, ControllerTest.UID, text, hid);
    }

    public long createModelQuestion(long modelId, long userId, String text) throws Exception {
        return createModelQuestion(modelId, userId, text, null);
    }

    public long createModelQuestion(long modelId, long userId, String text, Long hid) throws Exception {
        final String response = createModelQuestion(modelId, userId, text, hid, status().is2xxSuccessful());
        return objectMapper.readValue(response, QuestionDto.class).getId();
    }

    public String createModelQuestion(long modelId, long userId, String text, Long hid,
                                      ResultMatcher resultMatcher) throws Exception {
        return createModelQuestion(modelId, userId, text, hid, null, resultMatcher);
    }

    public String createModelQuestion(long modelId, long userId, String text, Long hid, Long sku,
                                      ResultMatcher resultMatcher) throws Exception {

        String body;
        if (hid != null && sku != null) {
            body = String.format(MODEL_QUESTION_BODY_FULL, hid, sku, text);
        } else if (hid != null) {
            body = String.format(MODEL_QUESTION_BODY_HID, hid, text);
        } else if (sku != null) {
            body = String.format(MODEL_QUESTION_BODY_SKU, sku, text);
        } else {
            body = String.format(MODEL_QUESTION_BODY, text);
        }

        return invokeAndRetrieveResponse(
            post("/question/UID/" + userId + "/model/" + modelId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public QuestionDto createModelQuestionWithIp(long modelId, long userId, String ip, Integer port) {
        String body = String.format(MODEL_QUESTION_BODY, UUID.randomUUID());

        MockHttpServletRequestBuilder builder = post("/question/UID/" + userId + "/model/" + modelId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .accept(MediaType.APPLICATION_JSON)
            .header(SecurityData.HEADER_X_REAL_IP, ip);

        if (port != null) {
            builder.header(SecurityData.HEADER_X_REAL_PORT, port);
        }

        return parseValue(invokeAndRetrieveResponse(
            builder,
            status().is2xxSuccessful()),
            QuestionDto.class);
    }
}
