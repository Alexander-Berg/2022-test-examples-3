package ru.yandex.market.pers.qa.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Comparators;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import ru.yandex.market.pers.qa.PersQaApiTest;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.controller.api.external.VendorApiControllerQuestionTest;
import ru.yandex.market.pers.qa.controller.dto.AnswerDto;
import ru.yandex.market.pers.qa.controller.dto.ModelQuestionCountDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.mock.mvc.AnswerMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.PostMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.QuestionMvcMocks;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.mock.mvc.AbstractMvcMocks.invokeMvc;

public class ControllerTest extends PersQaApiTest {

    public static final long MODEL_ID = 123456;
    public static final long INTEREST_ID = 434231;
    public static final Long CATEGORY_HID = 912512L;
    public static final long UID = 123;
    public static final String YANDEXUID = "123abc";
    protected static final String UID_STR = String.valueOf(UID);

    protected static final String CATEGORY_QUESTION_BODY = "{\n" +
        "  \"text\": \"%s\"\n" +
        "}";

    protected static final String VENDOR_ANSWER_BODY = "{\n" +
        "  \"vendorId\":\"%s\",\n" +
        "  \"text\": \"%s\"\n" +
        "}";

    protected static final String COMPLAIN_BODY = "{\n" +
        "  \"reasonId\": %s,\n" +
        "  \"text\": \"%s\"\n" +
        "}";

    protected final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate qaJdbcTemplate;

    @Autowired
    protected CommentService commentService;

    @Autowired
    @Qualifier("mockMvc")
    protected MockMvc mockMvc;

    @Autowired
    protected AnswerMvcMocks answerMvc;

    @Autowired
    public QuestionMvcMocks questionMvc;

    @Autowired
    public PostMvcMocks postMvcMocks;

    protected String invokeAndRetrieveResponse(MockHttpServletRequestBuilder requestBuilder, ResultMatcher expected
    ) throws Exception {
        return invokeMvc(mockMvc, requestBuilder, expected);
    }

    protected static String fileToString(String bodyFileName) throws IOException {
        return IOUtils.toString(VendorApiControllerQuestionTest.class.getResourceAsStream(bodyFileName), "UTF-8");
    }

    protected Long tryCreateAnswer(long questionId, String text) {
        try {
            return createAnswer(questionId, text);
        } catch (Exception e) {
            return null;
        }
    }

    protected long createAnswer(long questionId) throws Exception {
        return answerMvc.createAnswer(questionId);
    }

    protected long createAnswer(long questionId, String text) throws Exception {
        return answerMvc.createAnswer(questionId, text);
    }

    protected long createAnswer(long questionId, long uid) throws Exception {
        return answerMvc.createAnswer(questionId, uid);
    }

    protected long createAnswer(long questionId, long uid, String text) throws Exception {
        return answerMvc.createAnswer(questionId, uid, text);
    }

    protected long createVendorAnswer(long vendorId, long questionId) throws Exception {
        return createVendorAnswer(vendorId, questionId, UUID.randomUUID().toString());
    }

    protected long createVendorAnswer(long vendorId, long questionId, String text) throws Exception {
        return createVendorAnswer(vendorId, UID, questionId, text);
    }

    protected long createVendorAnswer(long vendorId, long uid, long questionId, String text) throws Exception {
        final String response = invokeAndRetrieveResponse(
            post("/vendor/" + vendorId + "/question/" + questionId + "/answer?userId=" + uid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(VENDOR_ANSWER_BODY, vendorId, text))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return objectMapper.readValue(response, AnswerDto.class).getAnswerId();
    }

    protected long createModelQuestion(long modelId, String text, long hid) throws Exception {
        return questionMvc.createModelQuestion(modelId, text, hid);
    }

    //TODO rename later to createModelQuestion
    protected long createQuestion(long modelId) throws Exception {
        return questionMvc.createModelQuestion(modelId);
    }

    protected long createModelQuestionHid(long modelId, long hid) throws Exception {
        return questionMvc.createModelQuestionHid(modelId, hid);
    }

    //TODO rename later to createModelQuestion
    protected long createQuestion(String text) throws Exception {
        return questionMvc.createModelQuestion(text);
    }

    //TODO rename later to createModelQuestion
    protected long createQuestion() throws Exception {
        return questionMvc.createModelQuestion();
    }

    protected long createModelQuestion(long modelId, long userId) throws Exception {
        return questionMvc.createModelQuestion(modelId, userId);
    }

    //TODO rename later to createModelQuestion
    protected long createQuestion(long modelId, long userId, String text) throws Exception {
        return questionMvc.createModelQuestion(modelId, userId, text);
    }

    protected long createModelQuestion(long modelId, long userId, String text, Long hid) throws Exception {
        return questionMvc.createModelQuestion(modelId, userId, text, hid);
    }

    protected String createModelQuestion(long modelId, long userId, String text, Long hid, ResultMatcher resultMatcher)
        throws Exception {
        return questionMvc.createModelQuestion(modelId, userId, text, hid, resultMatcher);
    }

    protected long createCategoryQuestion() throws Exception {
        return createCategoryQuestion(UUID.randomUUID().toString());
    }

    protected long createCategoryQuestion(String text) throws Exception {
        return createCategoryQuestion(CATEGORY_HID, UID, text);
    }

    protected long createCategoryQuestion(long hid, String text) throws Exception {
        return createCategoryQuestion(hid, UID, text);
    }

    protected long createCategoryQuestion(long hid, long userId) throws Exception {
        return createCategoryQuestion(hid, userId, UUID.randomUUID().toString());
    }

    protected long createCategoryQuestion(long hid, long userId, String text) throws Exception {
        String response = createCategoryQuestion(hid, userId, text, status().is2xxSuccessful());
        return objectMapper.readValue(response, QuestionDto.class).getId();
    }

    protected String createCategoryQuestion(long hid, long userId, String text, ResultMatcher resultMatcher)
        throws Exception {
        return invokeAndRetrieveResponse(
            post("/question/category/" + hid + "/UID/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(CATEGORY_QUESTION_BODY, text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    protected List<ModelQuestionCountDto> getModelQuestionCount(List<Long> model) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/question/model/count")
                .param("userId", String.valueOf(UID))
                .param("modelId", model.stream().map(String::valueOf).toArray(String[]::new))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());

        return objectMapper.readValue(response, new TypeReference<List<ModelQuestionCountDto>>() {
        });
    }

    protected List<ModelQuestionCountDto> getModelQuestionCount(List<Long> model, boolean noAnswer) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/question/model/count")
                .param("userId", String.valueOf(UID))
                .param("modelId", model.stream().map(String::valueOf).toArray(String[]::new))
                .param("addNotAnswered", String.valueOf(noAnswer))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());

        return objectMapper.readValue(response, new TypeReference<List<ModelQuestionCountDto>>() {
        });
    }

    protected long createPartnerShopAnswer(long shopId, long questionId, String text) throws Exception {
        return createPartnerShopAnswer(shopId, UID, questionId, text);
    }

    protected long createPartnerShopAnswer(long shopId, long uid, long questionId, String text) throws Exception {
        final String response = createPartnerShopAnswer(shopId, uid, questionId, text, status().is2xxSuccessful());
        return objectMapper.readValue(response, AnswerDto.class).getAnswerId();
    }

    protected String createPartnerShopAnswer(long shopId,
        long uid,
        long questionId,
        String text,
        ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            post("/partner/shop/" + shopId + "/question/" + questionId + "/answer")
                .param("userId", String.valueOf(uid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(AnswerMvcMocks.ANSWER_BODY, text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    protected <T> void checkSorted(List<T> data, Comparator<T> cmp) {
        assertTrue(Comparators.isInStrictOrder(data, cmp));
    }

    protected void assertTheSameLongList(List<Long> sourceIds, List<Long> resultIds) {
        assertEquals(sourceIds.size(), resultIds.size());
        sourceIds.forEach(x -> assertTrue(resultIds.contains(x)));
        resultIds.forEach(x -> assertTrue(sourceIds.contains(x)));
    }

    protected void assertTheSameQuestionIds(List<Long> sourceIds, List<QuestionDto> result) {
        final List<Long> resultIds = ListUtils.toList(result, QuestionDto::getId);
        assertTheSameLongList(sourceIds, resultIds);
    }

    protected void assertTheSameAnswerIds(List<Long> sourceIds, List<AnswerDto> result) {
        final List<Long> resultIds = ListUtils.toList(result, AnswerDto::getAnswerId);
        assertTheSameLongList(sourceIds, resultIds);
    }

    protected <T> void checkPager(QAPager<T> pager, int curPageNum, int curPageSize, int pageCount, int totalCount) {
        assertEquals(curPageSize, pager.getData().size());
        assertEquals(curPageNum, pager.getPager().getPageNum());
        assertEquals(totalCount, pager.getPager().getCount());
        assertEquals(pageCount, pager.getPager().getTotalPageCount());
        assertTrue(pager.getPager().getPages().stream().filter(it -> it.num == curPageNum).findFirst().get().isCurrent());
        assertTrue(pager.getPager().getPages().stream().filter(it -> it.num != curPageNum).noneMatch(it -> it.current));
    }

    @SafeVarargs
    protected final <T> List<T> join(List<T>... sources) {
        return Arrays.stream(sources)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    protected static String convertToIsoLocalDateTime(Instant instant) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
            LocalDateTime.ofEpochSecond(
                instant.getEpochSecond(),
                instant.getNano(),
                ZoneOffset.UTC));
    }
}
