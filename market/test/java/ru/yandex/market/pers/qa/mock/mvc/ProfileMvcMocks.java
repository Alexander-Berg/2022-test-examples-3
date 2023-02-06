package ru.yandex.market.pers.qa.mock.mvc;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.PAGE_NUM_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.PAGE_SIZE_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.TOP_ANSWERS_COUNT_KEY;
import static ru.yandex.market.pers.qa.controller.QAControllerTest.DEF_PAGE_SIZE;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 25.12.2019
 */
@Service
public class ProfileMvcMocks extends AbstractMvcMocks {

    public QAPager<QuestionDto> getAuthorQuestions(long userId) throws Exception {
        return getAuthorQuestions(userId, 1, DEF_PAGE_SIZE);
    }

    public QAPager<QuestionDto> getAuthorQuestions(long userId, long pageNum, long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/profile/author/UID/" + userId + "/question")
                // do not check top answers in regular tests
                .param(TOP_ANSWERS_COUNT_KEY, "0")
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    public QAPager<QuestionDto> getAuthorPublicQuestionByUser(long authorId, long userId) throws Exception {
        return getAuthorPublicQuestionByUser(authorId, userId, 1, DEF_PAGE_SIZE);
    }

    public QAPager<QuestionDto> getAuthorPublicQuestionByUser(long authorId, long userId, long pageNum, long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
                get("/profile/author/UID/" + authorId + "/public/question/by/UID/" + userId)
                        // do not check top answers in regular tests
                        .param(TOP_ANSWERS_COUNT_KEY, "0")
                        .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                        .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
                status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    public QAPager<QuestionDto> getAuthorPublicAnswerByUid(long authorId, long userId) throws Exception {
        return getAuthorPublicAnswerByUid(authorId, userId, 1, DEF_PAGE_SIZE);
    }

    public QAPager<QuestionDto> getAuthorPublicAnswerByUid(long authorId, long userId, long pageNum, long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
                get("/profile/author/UID/" + authorId + "/public/answer/by/UID/" + userId)
                        // do not check top answers in regular tests
                        .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                        .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
                status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    public QAPager<QuestionDto> getAuthorPublicQuestionByYandexUid(long authorId, String yandexUid) throws Exception {
        return getAuthorPublicQuestionByYandexUid(authorId, yandexUid, 1, DEF_PAGE_SIZE);
    }

    public QAPager<QuestionDto> getAuthorPublicQuestionByYandexUid(long authorId, String yandexUid, long pageNum, long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
                get("/profile/author/UID/" + authorId + "/public/question/by/YANDEXUID/" + yandexUid)
                        // do not check top answers in regular tests
                        .param(TOP_ANSWERS_COUNT_KEY, "0")
                        .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                        .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
                status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    public CountDto getAuthorPublicAnswerCount(long userId) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
                get("/profile/author/UID/" + userId + "/public/answer/count"),
                status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }

    public QAPager<QuestionDto> getAuthorAnswers(long userId) throws Exception {
        return getAuthorAnswers(userId, 1, DEF_PAGE_SIZE);
    }

    public QAPager<QuestionDto> getAuthorAnswers(long userId, long pageNum, long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/profile/author/UID/" + userId + "/answer")
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    public CountDto getAuthorAnswerCount(long userId) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/profile/author/UID/" + userId + "/answer/count"),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }

    public QAPager<QuestionDto> getModelQuestionForAgitation(long userId, long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/profile/author/UID/" + userId + "/question/model/agitation")
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }
}
