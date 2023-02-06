package ru.yandex.market.pers.qa.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.client.model.SortField;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.client.utils.QaApiUtils;
import ru.yandex.market.pers.qa.controller.dto.AnswerDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionAgitationInfoDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.mock.mvc.AnswerMvcMocks;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityAction;
import ru.yandex.market.pers.qa.model.Sort;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.MIX_MODEL_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.PAGE_NUM_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.PAGE_SIZE_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.QUESTION_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.TOP_ANSWERS_COUNT_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.YANDEX_UID_KEY;

public abstract class QAControllerTest extends ControllerTest {
    public static int DEF_PAGE_SIZE = 10;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    protected static final String NOT_ENOUGH_RIGHTS_ERROR_RESPONSE_FIELD =
        "\"error\":\"User=%s hasn't rights to remove %s with id=%s\"";

    protected static final String VENDOR_COMMENT_TEXT = "ответ от вендора(или нет)";

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected Map<Long, QuestionDto> map(QAPager<QuestionDto> questions) {
        return questions.getData().stream()
            .collect(Collectors.toMap(
                QuestionDto::getId,
                x -> x
            ));
    }

    protected QAPager<QuestionDto> getModelQuestions(long modelId) throws Exception {
        return getModelQuestions(modelId, 1, DEF_PAGE_SIZE);
    }

    protected QAPager<QuestionDto> getModelQuestions(long modelId, long pageNum, long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/model/" + modelId + "/UID/" + UID)
                // do not check top answers in regular tests
                .param(TOP_ANSWERS_COUNT_KEY, "0")
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected QAPager<QuestionDto> getModelQuestionsLite(long modelId) throws Exception {
        return getModelQuestionsLite(modelId, 1, DEF_PAGE_SIZE);
    }

    protected QAPager<QuestionDto> getModelQuestionsLite(long modelId, long pageNum, long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/model/" + modelId + "/UID/" + UID + "/lite")
                // do not check top answers in regular tests
                .param(TOP_ANSWERS_COUNT_KEY, "0")
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected QAPager<QuestionDto> getModelQuestionsWithAnswers(long modelId,
                                                                long pageNum,
                                                                long pageSize,
                                                                int topAnswers) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/model/" + modelId + "/UID/" + UID)
                // do not check top answers in regular tests
                .param(TOP_ANSWERS_COUNT_KEY, String.valueOf(topAnswers))
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected QAPager<QuestionDto> getModelQuestionsYandexUid(long modelId, String yandexUid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/model/" + modelId + "/YANDEXUID/" + yandexUid)
                // do not check top answers in regular tests
                .param(TOP_ANSWERS_COUNT_KEY, "0"),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected QAPager<QuestionDto> getModelQuestionsYandexUidLite(long modelId,
                                                                  long pageNum,
                                                                  long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/model/" + modelId + "/YANDEXUID/" + YANDEX_UID_KEY + "/lite")
                // do not check top answers in regular tests
                .param(TOP_ANSWERS_COUNT_KEY, "0")
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected CountDto getModelQuestionsCountYandexUid(long modelId, String yandexUid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/model/" + modelId + "/YANDEXUID/" + yandexUid + "/count"),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }

    protected CountDto getModelQuestionsCountUid(long modelId, long uid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/model/" + modelId + "/UID/" + uid + "/count"),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }

    protected long getModelQuestionsCount(long modelId) throws Exception {
        final CountDto dto = objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/model/" + modelId + "/count"),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
        return dto.getCount();
    }

    protected QAPager<QuestionDto> getCategoryQuestionsUid(long hid, boolean mixModel) throws Exception {
        return getCategoryQuestionsUid(hid, mixModel, 1, DEF_PAGE_SIZE);
    }

    protected QAPager<QuestionDto> getCategoryQuestionsUid(long hid,
                                                           boolean mixModel,
                                                           long pageNum,
                                                           long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/category/" + hid + "/UID/" + UID)
                .param(TOP_ANSWERS_COUNT_KEY, "0")
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize))
                .param(MIX_MODEL_KEY, Boolean.toString(mixModel)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected QAPager<QuestionDto> getCategoryQuestionsYandexUid(long hid, String yandexUid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/category/" + hid + "/YANDEXUID/" + yandexUid)
                .param(TOP_ANSWERS_COUNT_KEY, "0"),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected long getCategoryQuestionsCountUid(long hid, Long uid, boolean mixModel) throws Exception {
        CountDto count = objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/category/" + hid + "/UID/" + uid + "/count")
                .param(MIX_MODEL_KEY, Boolean.toString(mixModel)),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
        return count.getCount();
    }

    protected long getCategoryQuestionsCountYandexUid(long hid, String yandexUid) throws Exception {
        CountDto count = objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/category/" + hid + "/YANDEXUID/" + yandexUid + "/count"),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
        return count.getCount();
    }

    protected QAPager<QuestionDto> getCategoryQuestionsUidLite(long hid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/category/" + hid + "/UID/" + UID + "/lite"),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected QAPager<QuestionDto> getCategoryQuestionsYandexUidLite(long hid, String yandexUid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/category/" + hid + "/YANDEXUID/" + yandexUid+ "/lite"),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected long getCategoryQuestionsCountUidLite(long hid, Long uid, boolean mixModel) throws Exception {
        CountDto count = objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/category/" + hid + "/UID/" + uid + "/lite/count")
                .param(MIX_MODEL_KEY, Boolean.toString(mixModel)),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
        return count.getCount();
    }

    protected long getCategoryQuestionsCountYandexUidLite(long hid, String yandexUid) throws Exception {
        CountDto count = objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/category/" + hid + "/YANDEXUID/" + yandexUid + "/lite/count"),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
        return count.getCount();
    }

    protected DtoList<QuestionDto> getSimilarQuestionsUid(long questionId,
                                                          long userId,
                                                          long pageNum,
                                                          long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/" + questionId + "/UID/" + userId + "/similar")
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<DtoList<QuestionDto>>() {
        });
    }

    protected DtoList<QuestionDto> getSimilarQuestionsYandexUid(long questionId,
                                                                String yandexUid,
                                                                long pageNum,
                                                                long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/" + questionId + "/YANDEXUID/" + yandexUid + "/similar")
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<DtoList<QuestionDto>>() {
        });
    }

    protected Map<Long, AnswerDto> getAnswersMap(long questionId) throws Exception {
        return getAnswersMap(questionId, 1, DEF_PAGE_SIZE);
    }

    protected Map<Long, AnswerDto> getAnswersMap(long questionId, long pageNum, long pageSize) throws Exception {
        return getAnswers(questionId, pageNum, pageSize).getData().stream()
            .collect(Collectors.toMap(
                AnswerDto::getAnswerId,
                x -> x
            ));
    }

    protected QAPager<AnswerDto> getAnswers(long questionId) throws Exception {
        return getAnswers(questionId, 1, DEF_PAGE_SIZE);
    }

    protected QAPager<AnswerDto> getAnswers(long questionId, long pageNum, long pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/answers/" + questionId + "/UID/" + UID)
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<AnswerDto>>() {
        });
    }

    protected QAPager<AnswerDto> getAnswersUid(long questionId, long uid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/answers/" + questionId + "/UID/" + uid)
                .param(PAGE_NUM_KEY, String.valueOf(1))
                .param(PAGE_SIZE_KEY, String.valueOf(DEF_PAGE_SIZE)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<AnswerDto>>() {
        });
    }

    protected QAPager<AnswerDto> getAnswersYandexUid(long questionId, String yandexUid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/answers/" + questionId + "/YANDEXUID/" + yandexUid),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<AnswerDto>>() {
        });
    }

    protected List<QuestionDto> getQuestionsBulk(List<Long> questionIds,
                                                UserType type,
                                                ResultMatcher resultMatcher) throws Exception {
        String user = type == UserType.UID ? "UID/" + UID : "YANDEXUID/" + YANDEXUID;
        DtoList<QuestionDto> result = objectMapper.readValue(invokeAndRetrieveResponse(
            get("/question/" + user + "/bulk")
                .param(QUESTION_ID_KEY, questionIds.stream().map(Object::toString).toArray(String[]::new)),
            resultMatcher
        ), new TypeReference<DtoList<QuestionDto>>() {
        });
        return result.getData();
    }

    protected String getQuestionAgitationInfoString(long questionId) throws Exception {
        return invokeAndRetrieveResponse(
            get("/question/" + questionId + "/agitation"),
            status().is2xxSuccessful()
        );
    }

    protected QuestionAgitationInfoDto getQuestionAgitationInfo(long questionId) throws Exception {
        return FormatUtils.fromJson(getQuestionAgitationInfoString(questionId), QuestionAgitationInfoDto.class);
    }

    protected AnswerDto getAnswer(long answerId, long userId) throws Exception {
        return getAnswer(answerId, userId, status().is2xxSuccessful());
    }

    protected AnswerDto getAnswer(long answerId, long userId, ResultMatcher resultMatcher) throws Exception {
        String source = invokeAndRetrieveResponse(
            get("/answer/" + answerId + "/UID/" + userId),
            resultMatcher
        );
        return FormatUtils.fromJson(source, AnswerDto.class);
    }

    protected AnswerDto getAnswerYandexUid(long answerId, String yandexUid) throws Exception {
        return getAnswerYandexUid(answerId, yandexUid, status().is2xxSuccessful());
    }

    protected AnswerDto getAnswerYandexUid(long answerId,
                                           String yandexuid,
                                           ResultMatcher resultMatcher) throws Exception {
        return FormatUtils.fromJson(invokeAndRetrieveResponse(
            get("/answer/" + answerId + "/YANDEXUID/" + yandexuid),
            resultMatcher
        ), AnswerDto.class);
    }

    protected List<AnswerDto> getAnswersForVendor(long questionId,
                                                  long brandId,
                                                  SortField sortField,
                                                  boolean isAscending) throws Exception {
        return getAnswersForVendor(questionId, brandId, new Sort(sortField, isAscending)).getData();
    }

    protected QAPager<AnswerDto> getAnswersForVendor(long questionId, long brandId, Sort sort) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/vendor/" + brandId + "/answers/" + questionId)
                .param("userId", String.valueOf(UID))
                .param("sortField", sort.getField().toString())
                .param("asc", Boolean.toString(sort.isAscending())),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<AnswerDto>>() {
        });
    }

    protected QAPager<AnswerDto> getAnswersForVendor(long questionId,
                                                     long brandId,
                                                     Sort sort,
                                                     int page,
                                                     int pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/vendor/" + brandId + "/answers/" + questionId)
                .param("userId", String.valueOf(UID))
                .param("sortField", sort.getField().toString())
                .param("asc", Boolean.toString(sort.isAscending()))
                .param("pageNum", String.valueOf(page))
                .param("pageSize", String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<AnswerDto>>() {
        });
    }

    protected long createQuestion(ModState modState,
                                  State state,
                                  Callable<Long> createQuestionCall) throws Exception {
        long id = createQuestionCall.call();
        questionService.forceUpdateModState(id, modState);
        questionService.forceUpdateState(id, state);
        return id;
    }

    protected long createAnswer(ModState modState,
                                State state,
                                Callable<Long> createAnswerCall) throws Exception {
        long id = createAnswerCall.call();
        answerService.forceUpdateModState(id, modState);
        answerService.forceUpdateState(id, state);
        return id;
    }

    protected long createModelQuestion(ModState modState, State state) throws Exception {
        return createModelQuestion(modState, state, UID);
    }

    protected long createModelQuestion(ModState modState, State state, Long userId) throws Exception {
        return createQuestion(modState, state, () -> createModelQuestion(MODEL_ID, userId));
    }

    protected long createCategoryQuestion(ModState modState, State state) throws Exception {
        return createCategoryQuestion(modState, state, UID);
    }

    protected long createCategoryQuestion(ModState modState, State state, Long userId) throws Exception {
        return createQuestion(modState, state, () -> createCategoryQuestion(CATEGORY_HID, userId));
    }

    protected void deleteQuestion(long id) throws Exception {
        deleteQuestion(id, UID, status().is2xxSuccessful());
    }

    protected String deleteQuestion(long id, long uid, ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            delete("/question/" + id).param("userId", String.valueOf(uid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    protected void deleteAnswer(long id) throws Exception {
        deleteAnswer(id, UID, status().is2xxSuccessful());
    }

    protected void deleteAnswer(long id, long uid, ResultMatcher resultMatcher) throws Exception {
        invokeAndRetrieveResponse(
            delete("/answer/" + id).param("userId", String.valueOf(uid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    protected void deleteVendorAnswer(long id, long brandId) throws Exception {
        invokeAndRetrieveResponse(
            delete(String.format("/vendor/%s/answer/%d", brandId, id))
                .param("userId", String.valueOf(UID))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    protected void editVendorAnswer(long answerId, long brandId, String text) throws Exception {
        final String response = invokeAndRetrieveResponse(
            patch(String.format("/vendor/%s/answer/%s", brandId, answerId))
                .param("userId", String.valueOf(UID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(AnswerMvcMocks.ANSWER_BODY, text))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    protected void commentAnswerByVendor(long commentId, long answerId, long brandId, QaEntityAction event) throws Exception {
        invokeAndRetrieveResponse(
            post(String.format("/answer/%s/comment", answerId))
                .param("userId", String.valueOf(UID))
                .param("event", event.toString())
                .param("brandId", String.valueOf(brandId))
                .param("id", QaApiUtils.toCommentIdInController(commentId))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    protected long createAnswer(long questionId, ModState modState, State state) throws Exception {
        long id = createAnswer(questionId, UUID.randomUUID().toString());
        qaJdbcTemplate.update("update qa.answer q set mod_state = ?, state = ? where q.id = ?",
            modState.getValue(), state.getValue(), id);
        return id;
    }

    protected String getNotEnoughRightsErrorField(String entity, long uid, long entityId) {
        return String.format(NOT_ENOUGH_RIGHTS_ERROR_RESPONSE_FIELD, uid, entity, entityId);
    }

    protected List<Long> createModelQuestionsReturnPublic() throws Exception {
        return createModelQuestionsReturnPublic(UID);
    }

    protected List<Long> createModelQuestionsReturnPublic(Long userId) throws Exception {
        return createQuestionsReturnPublic(() -> createModelQuestion(MODEL_ID, userId));
    }

    protected List<Long> createCategoryQuestionsReturnPublic(Long userId) throws Exception {
        return createQuestionsReturnPublic(() -> createCategoryQuestion(CATEGORY_HID, userId));
    }

    /**
     * Создает вопросы со всевозможными комбинациями state и modState для определенного UID.
     * @return опубликованные вопросы
     * @throws Exception
     */
    protected List<Long> createQuestionsReturnPublic(Callable<Long> createQuestionCall) throws Exception {
        for (ModState modState : ModState.values()) {
            createQuestion(modState, State.DELETED, createQuestionCall);
        }
        createQuestion(ModState.AUTO_FILTER_REJECTED, State.NEW, createQuestionCall);
        createQuestion(ModState.TOLOKA_REJECTED, State.NEW, createQuestionCall);

        List<Long> pubQuestions = new ArrayList<>();
        for (ModState modState : ModState.PUBLISHED) {
            pubQuestions.add(createQuestion(modState, State.NEW, createQuestionCall));
        }
        return pubQuestions;
    }

    /**
     * Создает вопросы со всевозможными комбинациями state и modState для определенного UID.
     * @return все вопросы
     * @throws Exception
     */
    protected List<Long> createQuestionsReturnAll(Callable<Long> createQuestionCall) throws Exception {
        List<Long> questions = new ArrayList<>();
        for (State state : State.values()) {
            for (ModState modState : ModState.values()) {
                questions.add(createQuestion(modState, state, createQuestionCall));
            }
        }
        return questions;
    }

    /**
     * Создает ответы на вопрос questionId со всевозможными комбинациями state и modState.
     *
     * @param questionId
     * @return опубликованные ответы
     * @throws Exception
     */
    protected List<Long> createAnswersAndReturnPublic(long questionId) throws Exception {
        for (ModState modState : ModState.values()) {
            createAnswer(questionId, modState, State.DELETED);
        }
        createAnswer(questionId, ModState.AUTO_FILTER_REJECTED, State.NEW);
        createAnswer(questionId, ModState.TOLOKA_REJECTED, State.NEW);

        List<Long> pubAnswerIds = new ArrayList<>();
        for (ModState modState : ModState.PUBLISHED) {
            pubAnswerIds.add(createAnswer(questionId, modState, State.NEW));
        }
        return pubAnswerIds;
    }

    /**
     * Создает ответы со всевозможными комбинациями state и modState для определенного UID.
     * @return все ответы
     * @throws Exception
     */
    protected List<Long> createAnswersReturnAll(Callable<Long> createAnswerCall) throws Exception {
        List<Long> answers = new ArrayList<>();
        for (State state : State.values()) {
            for (ModState modState : ModState.values()) {
                answers.add(createAnswer(modState, state, createAnswerCall));
            }
        }
        return answers;
    }
}
