package ru.yandex.market.pers.qa.mock.mvc;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CommentTreeDto;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.utils.ControllerConstants;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.controller.dto.AnswerDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.ANSWER_ID_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 15.08.2019
 */
@Service
public class AnswerMvcMocks extends AbstractMvcMocks {

    public static final String ANSWER_BODY = "{\n" +
        "  \"text\": \"%s\"\n" +
        "}";
    protected static final String COMMENT_BODY = "{\n" +
        "  \"text\": \"%s\"\n" +
        "}";

    public long createAnswer(long questionId) throws Exception {
        return createAnswer(questionId, ControllerTest.UID);
    }

    public long createAnswer(long questionId, String text) throws Exception {
        return createAnswer(questionId, ControllerTest.UID, text);
    }

    public long createAnswer(long questionId, long uid) throws Exception {
        return createAnswer(questionId, uid, UUID.randomUUID().toString());
    }

    public long createAnswer(long questionId, long uid, String text) throws Exception {
        final String response = invokeAndRetrieveResponse(
            post("/answer/UID/" + uid + "/question/" + questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(ANSWER_BODY, text))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return objectMapper.readValue(response, AnswerDto.class).getAnswerId();
    }

    public long createComment(long uid, long answerId, String text) throws Exception {
        final String response = createComment(uid, answerId, text, status().is2xxSuccessful());
        return objectMapper.readValue(response, CommentDto.class).getId();
    }

    public String createComment(long uid,
                                long answerId,
                                String text,
                                ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            post("/comment/answer/" + answerId + "/UID/" + uid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(COMMENT_BODY, text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public String deleteComment(long uid,
                                long commentId,
                                ResultMatcher resultMatcher) throws Exception {
        final String response = invokeAndRetrieveResponse(

            delete("/comment/answer/" + commentId)
                .param(ControllerConstants.UID_KEY, String.valueOf(uid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
        return response;
    }

    public DtoList<CommentTreeDto> getCommentsBulk(long uid, long[] answerIds) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/comment/answer/UID/" + uid)
                .param(ANSWER_ID_KEY, Arrays.stream(answerIds).mapToObj(String::valueOf).toArray(String[]::new))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return objectMapper.readValue(response, new TypeReference<DtoList<CommentTreeDto>>() {
        });
    }

    public DtoList<CommentTreeDto> getCommentsBulkYandexuid(String yandexUid, long[] answerIds) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/comment/answer/YANDEXUID/" + yandexUid)
                .param(ANSWER_ID_KEY, Arrays.stream(answerIds).mapToObj(String::valueOf).toArray(String[]::new))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return objectMapper.readValue(response, new TypeReference<DtoList<CommentTreeDto>>() {
        });
    }

    public Map<Long, CountDto> getCommentsBulkCount(long[] answerIds, ResultMatcher expected) throws Exception {
        return objectMapper.readValue(getCommentsBulkCountWithoutMapping(answerIds, expected),
            new TypeReference<Map<Long, CountDto>>() {
            });
    }

    public String getCommentsBulkCountWithoutMapping(long[] answerIds, ResultMatcher expected) throws Exception {
        return invokeAndRetrieveResponse(
            get("/comment/answer/count")
                .param("answerId", Arrays.stream(answerIds)
                    .boxed()
                    .map(String::valueOf)
                    .toArray(String[]::new)),
            expected);
    }
}
