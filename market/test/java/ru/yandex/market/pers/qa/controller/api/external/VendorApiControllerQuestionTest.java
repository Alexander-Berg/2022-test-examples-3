package ru.yandex.market.pers.qa.controller.api.external;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.model.QaEntityAction;
import ru.yandex.market.pers.qa.client.model.SortField;
import ru.yandex.market.pers.qa.service.UpdateModelVendorIdService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author varvara
 * 23.08.2018
 */
public class VendorApiControllerQuestionTest extends QAControllerTest {

    @Autowired
    private UpdateModelVendorIdService modelVendorIdService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .withZone(ZoneId.systemDefault());

    //like in "/data/vendor_question_request.json"
    private static final long HID_1 = 1;
    private static final long HID_2 = 2;

    private static final long HID_3 = 3;
    private static final long VENDOR_ID = 124;
    private static final long VENDOR_ID_2 = 1245;

    @Test
    void testNoHids() throws Exception {
        long vendorId = VENDOR_ID;
        long hid1 = HID_1;
        long hid2 = HID_2;
        long model1 = 1;
        long model2 = 2;
        long id1 = createModelQuestionHid(model1, hid1); // вопрос вендора в категории hid1
        long id2 = createModelQuestionHid(model2, hid2); // вопрос вендора в категории hid2

        modelVendorIdService.saveModelVendor(model1, vendorId);
        modelVendorIdService.saveModelVendor(model2, vendorId);

        final String dateFrom = DATE_TIME_FORMATTER.format(Instant.now().minus(1, DAYS));
        final String dateTo = DATE_TIME_FORMATTER.format(Instant.now().plus(1, DAYS));
        final List<Long> questions = Arrays.asList(id1, id2);

        final QAPager<QuestionDto> vendorQuestions1 = getQuestionsForVendorNoBody(vendorId,
            1, 10, "DATE", true, dateFrom, dateTo, false);
        assertTheSameQuestionIds(questions, vendorQuestions1.getData());
        final CountDto count1 = getQuestionsCountForVendorNoBody(vendorId,
            1, 10, "DATE", true, dateFrom, dateTo, false);
        assertEquals(questions.size(), count1.getCount());

        final QAPager<QuestionDto> vendorQuestions2 = getQuestionsForVendorWithBody(vendorId,
            1, 10, "DATE", true, dateFrom, dateTo, false, "");
        assertTheSameQuestionIds(questions, vendorQuestions2.getData());
        final CountDto count2 = getQuestionsCountForVendorWithBody(vendorId,
            1, 10, "DATE", true, dateFrom, dateTo, false, "");
        assertEquals(questions.size(), count2.getCount());
    }

    @Test
    void testFilterByVendorReplyAnswer() throws Exception {
        long vendorId = VENDOR_ID;
        long hid = HID_1;
        long model = 1;

        final Instant now = Instant.now();

        long idNoVendorReply = createQuestion(now, model, hid);

        long idVendorAnswer = createQuestion(now, model, hid);
        createVendorAnswer(vendorId, idVendorAnswer);

        long idVendorComment = createQuestion(now, model, hid);
        final long answerId = createAnswer(idVendorComment);

        long commentId = commentService.createVendorQaComment(UID, UUID.randomUUID().toString(), answerId, vendorId);
        commentAnswerByVendor(commentId, answerId, VENDOR_ID, QaEntityAction.SAVE);

        modelVendorIdService.saveModelVendor(model, vendorId);

        final String dateFrom = DATE_TIME_FORMATTER.format(now.minus(1, DAYS));
        final String dateTo = DATE_TIME_FORMATTER.format(now.plus(1, DAYS));

        final QAPager<QuestionDto> questionsVendorReply = getQuestionsForVendorWithBody(vendorId,
            1, 10, "DATE", true, dateFrom, dateTo, true, fileToString("/data/vendor_question_request.json"));
        assertTheSameQuestionIds(Arrays.asList(idVendorAnswer, idVendorComment), questionsVendorReply.getData());
        final CountDto countQuestionsVendorReply = getQuestionsCountForVendorWithBody(vendorId,
            1, 10, "DATE", true, dateFrom, dateTo, true, fileToString("/data/vendor_question_request.json"));
        assertEquals(2, countQuestionsVendorReply.getCount());

        final QAPager<QuestionDto> questionsNoVendorReply = getQuestionsForVendor(vendorId, dateFrom, dateTo);
        assertTheSameQuestionIds(Collections.singletonList(idNoVendorReply), questionsNoVendorReply.getData());
        final CountDto countQuestionsNoVendorReply = getQuestionsCountForVendor(vendorId, dateFrom, dateTo);
        assertEquals(1, countQuestionsNoVendorReply.getCount());

        final QAPager<QuestionDto> questionsNoVendorReplyFilter = getQuestionsForVendor(vendorId,
            fileToString("/data/vendor_question_request.json"), 1, 10, SortField.DATE, true, dateFrom, dateTo);
        assertTheSameQuestionIds(Arrays.asList(idVendorAnswer, idVendorComment, idNoVendorReply), questionsNoVendorReplyFilter.getData());
        final CountDto questionsCountNoVendorReplyFilter = getQuestionsCountForVendor(vendorId,
            fileToString("/data/vendor_question_request.json"), dateFrom, dateTo);
        assertEquals(3, questionsCountNoVendorReplyFilter.getCount());
    }

    @Test
    void testFilterByDate() throws Exception {
        long vendorId = VENDOR_ID;
        long hid = HID_1;
        long model = 1;

        final Instant now = Instant.now();
        final Instant from = now.minus(1, DAYS);
        final Instant to = now.plus(1, DAYS);

        //before chosen interval
        long id1 = createQuestion(from.minus(1, DAYS), model, hid);
        long id2 = createQuestion(from.minus(1, HOURS), model, hid);

        //in chosen interval
        long id3 = createQuestion(from.plus(1, HOURS), model, hid);
        long id4 = createQuestion(to.minus(1, HOURS), model, hid);

        //after chosen interval
        long id5 = createQuestion(to.plus(1, HOURS), model, hid);
        long id6 = createQuestion(to.plus(1, DAYS), model, hid);

        modelVendorIdService.saveModelVendor(model, vendorId);

        final String dateFrom = DATE_TIME_FORMATTER.format(from);
        final String dateTo = DATE_TIME_FORMATTER.format(to);

        final QAPager<QuestionDto> vendorQuestionsInterval = getQuestionsForVendor(vendorId, dateFrom, dateTo);
        assertTheSameQuestionIds(Arrays.asList(id3, id4), vendorQuestionsInterval.getData());
        final CountDto countVendorQuestionsInterval = getQuestionsCountForVendor(vendorId, dateFrom, dateTo);
        assertEquals(2, countVendorQuestionsInterval.getCount());

        final QAPager<QuestionDto> vendorQuestionsBeforeDate = getQuestionsForVendorDateTo(vendorId,
            fileToString("/data/vendor_question_request.json"), 1, 10, "DATE", true, dateTo, false);
        assertTheSameQuestionIds(Arrays.asList(id1, id2, id3, id4), vendorQuestionsBeforeDate.getData());
        final CountDto countVendorQuestionsBeforeDate = getQuestionsCountForVendorDateTo(vendorId,
            fileToString("/data/vendor_question_request.json"), dateTo, false);
        assertEquals(4, countVendorQuestionsBeforeDate.getCount());

        final QAPager<QuestionDto> vendorQuestionsAfterDate = getQuestionsForVendorDateFrom(vendorId,
            fileToString("/data/vendor_question_request.json"), 1, 10, "DATE", true, dateFrom, false);
        assertTheSameQuestionIds(Arrays.asList(id3, id4, id5, id6), vendorQuestionsAfterDate.getData());
        final CountDto countVendorQuestionsAfterDate = getQuestionsCountForVendorDateFrom(vendorId,
            fileToString("/data/vendor_question_request.json"), dateFrom, false);
        assertEquals(4, countVendorQuestionsAfterDate.getCount());

        final QAPager<QuestionDto> vendorQuestionsNoFilter = getQuestionsForVendorNoDate(vendorId,
            fileToString("/data/vendor_question_request.json"), 1, 10, "DATE", true, false);
        assertTheSameQuestionIds(Arrays.asList(id1, id2, id3, id4, id5, id6), vendorQuestionsNoFilter.getData());
        final CountDto countVendorQuestionsNoFilter = getQuestionsCountForVendorNoDate(vendorId,
            fileToString("/data/vendor_question_request.json"), false);
        assertEquals(6, countVendorQuestionsNoFilter.getCount());
    }

    @Test
    void testSortByDate() throws Exception {
        long vendorId = VENDOR_ID;
        long hid = HID_1;
        long model = 1;

        List<Long> questions = new ArrayList<>();
        int questionCount = 5;
        final Instant now = Instant.now();
        for (int i = 0; i < questionCount; i++) {
            final long id = createModelQuestionHid(model, hid);
            questions.add(id);
            qaJdbcTemplate.update("update qa.question set cr_time = ? where id = ?",
                new java.sql.Timestamp(now.plus(i, MINUTES).toEpochMilli()), id);
        }

        modelVendorIdService.saveModelVendor(model, vendorId);

        final String dateFrom = DATE_TIME_FORMATTER.format(now.minus(1, MINUTES));
        final String dateTo = DATE_TIME_FORMATTER.format(now.plus(questionCount + 1, MINUTES));
        final Comparator<QuestionDto> crTimeCmp = Comparator.comparing(QuestionDto::getCreationTime);

        final QAPager<QuestionDto> vendorQuestionsAscDate = getQuestionsForVendor(vendorId, dateFrom, dateTo);
        assertTheSameQuestionIds(questions, vendorQuestionsAscDate.getData());
        checkSorted(vendorQuestionsAscDate.getData(), crTimeCmp);

        final QAPager<QuestionDto> vendorQuestionsDescDate = getQuestionsForVendorWithBody(vendorId,
            1, 10, "DATE", false, dateFrom, dateTo, false, fileToString("/data/vendor_question_request.json"));
        assertTheSameQuestionIds(questions, vendorQuestionsDescDate.getData());
        checkSorted(vendorQuestionsDescDate.getData(), crTimeCmp.reversed());
    }

    @Test
    void testGetOnlyVendorQuestions() throws Exception {
        long vendorId = VENDOR_ID;
        long otherVendorId = VENDOR_ID_2;
        long hid1 = HID_1;
        long hid2 = HID_2;
        long hid3 = HID_3;
        long model1 = 1;
        long model2 = 2;
        long model3 = 3;
        long model4 = 4;
        long id1 = createModelQuestionHid(model1, hid1); // вопрос вендора в категории hid1
        long id2 = createModelQuestionHid(model2, hid2); // вопрос вендора в категории hid2
        long id3 = createModelQuestionHid(model3, hid3); // вопрос без вендора в категории hid3
        long id4 = createModelQuestionHid(model4, hid2); // вопрос другого вендора в категории hid2
        modelVendorIdService.saveModelVendor(model1, vendorId);
        modelVendorIdService.saveModelVendor(model2, vendorId);
        modelVendorIdService.saveModelVendor(model4, otherVendorId);

        final String dateFrom = DATE_TIME_FORMATTER.format(Instant.now().minus(1, DAYS));
        final String dateTo = DATE_TIME_FORMATTER.format(Instant.now().plus(1, DAYS));

        final QAPager<QuestionDto> vendorQuestions = getQuestionsForVendor(vendorId, dateFrom, dateTo);
        assertTheSameQuestionIds(Arrays.asList(id1, id2), vendorQuestions.getData());

        final CountDto countVendorQuestions = getQuestionsCountForVendor(vendorId, dateFrom, dateTo);
        assertEquals(2, countVendorQuestions.getCount());
    }

    @Test
    void testPagingGetVendorQuestions() throws Exception {
        long vendorId = VENDOR_ID;
        long hid = HID_1;
        long modelId = 1;

        int questionCount = 25;
        int pageSize = 10;
        int firstPageSize = 10;
        int secondPageSize = 10; // = 10
        int thirdPageSize = 5; // = 25 - 10 - 10
        int pageCount = 3; // = 25 div 10

        for (int i = 0; i < questionCount; i++) {
            createModelQuestionHid(modelId, hid);
        }
        modelVendorIdService.saveModelVendor(modelId, vendorId);
        final String dateFrom = DATE_TIME_FORMATTER.format(Instant.now().minus(1, DAYS));
        final String dateTo = DATE_TIME_FORMATTER.format(Instant.now().plus(1, DAYS));

        final QAPager<QuestionDto> firstPage = getQuestionsForVendorWithBody(vendorId,
            1, pageSize, "DATE", true, dateFrom, dateTo, false, fileToString("/data/vendor_question_request.json"));
        checkPager(firstPage, 1, firstPageSize, pageCount, questionCount);

        final QAPager<QuestionDto> secondPage = getQuestionsForVendorWithBody(vendorId,
            2, pageSize, "DATE", true, dateFrom, dateTo, false, fileToString("/data/vendor_question_request.json"));
        checkPager(secondPage, 2, secondPageSize, pageCount, questionCount);

        final QAPager<QuestionDto> thirdPage = getQuestionsForVendorWithBody(vendorId,
            3, pageSize, "DATE", true, dateFrom, dateTo, false, fileToString("/data/vendor_question_request.json"));
        checkPager(thirdPage, 3, thirdPageSize, pageCount, questionCount);
    }

    @Test
    void testManyHids10() throws Exception {
        manyHidsTest(10);
    }

    @Test
    void testManyHids100() throws Exception {
        manyHidsTest(100);
    }

    @Test
    void testManyHids1000() throws Exception {
        manyHidsTest(1000);
    }

    @Test
    @Disabled("slow test")
    void testManyHids5000() throws Exception {
        manyHidsTest(5000);
    }

    @Test
    @Disabled("slow test")
    void testManyHids10000() throws Exception {
        manyHidsTest(10000);
    }

    @Test
    void testWithModelFilter() throws Exception {
        final long modelId = -1348139;
        final long modelIdNoQuestion = modelId + 1;
        final long hid = 123;
        final long hidWrong = hid + 1;

        final long questionId = createModelQuestionHid(modelId, hid);
        modelVendorIdService.saveModelVendor(modelId, VENDOR_ID);

        // try load with correct parameters
        final QAPager<QuestionDto> resultOk = getQuestionsForVendorWithModel(VENDOR_ID, hid, modelId);
        assertEquals(1, resultOk.getData().size());
        assertEquals(questionId, resultOk.getData().get(0).getId());

        // try check that nothing founds for invalid model
        final QAPager<QuestionDto> resultWrongModel = getQuestionsForVendorWithModel(VENDOR_ID, hid, modelIdNoQuestion);
        assertEquals(0, resultWrongModel.getData().size());

        // try check, that nothing founds for invalid hid
        final QAPager<QuestionDto> resultWrongHid = getQuestionsForVendorWithModel(VENDOR_ID, hidWrong, modelId);
        assertEquals(0, resultWrongHid.getData().size());
    }

    private void manyHidsTest(long hidsCount) throws Exception {
        long vendorId = VENDOR_ID;
        long model = 1;
        List<Long> hids = new ArrayList<>();

        int hidBound = 100_000;
        Random r = new Random();
        for (int i = 0; i < (int) hidsCount; i++) {
            long hid = (long) r.nextInt(hidBound);
            hids.add(hid);
        }

        int questionCount = 10;
        for (int i = 0; i < questionCount; i++) {
            createModelQuestionHid(model, hids.get(i));
        }
        modelVendorIdService.saveModelVendor(model, vendorId);

        String vendorRequest = createVendorRequest(hids);

        final Instant now = Instant.now();
        final String dateFrom = DATE_TIME_FORMATTER.format(now.minus(1, DAYS));
        final String dateTo = DATE_TIME_FORMATTER.format(now.plus(1, DAYS));

        final QAPager<QuestionDto> vendorQuestions = getQuestionsForVendorWithBody(vendorId, 1, 10, "DATE", true,
            dateFrom, dateTo, false, vendorRequest);
        assertEquals(questionCount, vendorQuestions.getData().size());

        final CountDto countVendorQuestions = getQuestionsCountForVendorWithBody(vendorId, 1, 10, "DATE", true,
            dateFrom, dateTo, false, vendorRequest);
        assertEquals(questionCount, countVendorQuestions.getCount());
    }

    private long createQuestion(Instant crTime, Long model, long hid) throws Exception {
        long id = createModelQuestionHid(model, hid);
        qaJdbcTemplate.update("update qa.question set cr_time = ? where id = ?", new Date(crTime.toEpochMilli()), id);
        return id;
    }

    private String createVendorRequest(List<Long> hids) {
        return String.format("{\n" +
            "  \"hids\": [\n" +
            "%s" +
            "  ]\n" +
            "}", hids.stream().map(String::valueOf).collect(Collectors.joining(",\n")));
    }

    protected QAPager<QuestionDto> getQuestionsForVendor(long brandId,
                                                         String hids,
                                                         int pageNum,
                                                         int pageSize,
                                                         SortField sortField,
                                                         boolean isAscending,
                                                         String dateFrom,
                                                         String dateTo) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question")
                .param("userId", String.valueOf(UID))
                .param("sortField", sortField.toString())
                .param("asc", String.valueOf(isAscending))
                .param("pageNum", String.valueOf(pageNum))
                .param("pageSize", String.valueOf(pageSize))
                .param("dateFrom", dateFrom)
                .param("dateTo", dateTo)
                .contentType(MediaType.APPLICATION_JSON)
                .content(hids),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected QAPager<QuestionDto> getQuestionsForVendorWithModel(long brandId,
                                                                  long hid,
                                                                  long modelId) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question")
                .param("userId", String.valueOf(UID))
                .param("modelId", String.valueOf(modelId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"hids\":[%s]}", hid)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected CountDto getQuestionsCountForVendor(long brandId,
                                                  String hids,
                                                  String dateFrom,
                                                  String dateTo) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question/count")
                .param("userId", String.valueOf(UID))
                .param("dateFrom", dateFrom)
                .param("dateTo", dateTo)
                .contentType(MediaType.APPLICATION_JSON)
                .content(hids),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }

    protected QAPager<QuestionDto> getQuestionsForVendorDateFrom(long brandId,
                                                         String hids,
                                                         int pageNum,
                                                         int pageSize,
                                                         String sortField,
                                                         boolean isAscending,
                                                         String dateFrom,
                                                         boolean withVendorAction) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question")
                .param("userId", String.valueOf(UID))
                .param("sortField", sortField)
                .param("asc", String.valueOf(isAscending))
                .param("pageNum", String.valueOf(pageNum))
                .param("pageSize", String.valueOf(pageSize))
                .param("dateFrom", dateFrom)
                .param("withVendorAction", String.valueOf(withVendorAction))
                .contentType(MediaType.APPLICATION_JSON)
                .content(hids),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected CountDto getQuestionsCountForVendorDateFrom(long brandId,
                                                          String hids,
                                                          String dateFrom,
                                                          boolean withVendorAction) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question/count")
                .param("userId", String.valueOf(UID))
                .param("dateFrom", dateFrom)
                .param("withVendorAction", String.valueOf(withVendorAction))
                .contentType(MediaType.APPLICATION_JSON)
                .content(hids),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }

    protected QAPager<QuestionDto> getQuestionsForVendorDateTo(long brandId,
                                                         String hids,
                                                         int pageNum,
                                                         int pageSize,
                                                         String sortField,
                                                         boolean isAscending,
                                                         String dateTo,
                                                         boolean withVendorAction) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question")
                .param("userId", String.valueOf(UID))
                .param("sortField", sortField)
                .param("asc", String.valueOf(isAscending))
                .param("pageNum", String.valueOf(pageNum))
                .param("pageSize", String.valueOf(pageSize))
                .param("dateTo", dateTo)
                .param("withVendorAction", String.valueOf(withVendorAction))
                .contentType(MediaType.APPLICATION_JSON)
                .content(hids),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected CountDto getQuestionsCountForVendorDateTo(long brandId,
                                                        String hids,
                                                        String dateTo,
                                                        boolean withVendorAction) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question/count")
                .param("userId", String.valueOf(UID))
                .param("dateTo", dateTo)
                .param("withVendorAction", String.valueOf(withVendorAction))
                .contentType(MediaType.APPLICATION_JSON)
                .content(hids),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }

    protected QAPager<QuestionDto> getQuestionsForVendorNoDate(long brandId,
                                                         String hids,
                                                         int pageNum,
                                                         int pageSize,
                                                         String sortField,
                                                         boolean isAscending,
                                                         boolean withVendorAction) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question")
                .param("userId", String.valueOf(UID))
                .param("sortField", sortField)
                .param("asc", String.valueOf(isAscending))
                .param("pageNum", String.valueOf(pageNum))
                .param("pageSize", String.valueOf(pageSize))
                .param("withVendorAction", String.valueOf(withVendorAction))
                .contentType(MediaType.APPLICATION_JSON)
                .content(hids),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected CountDto getQuestionsCountForVendorNoDate(long brandId,
                                                        String hids,
                                                        boolean withVendorAction) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question/count")
                .param("userId", String.valueOf(UID))
                .param("withVendorAction", String.valueOf(withVendorAction))
                .contentType(MediaType.APPLICATION_JSON)
                .content(hids),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }

    private QAPager<QuestionDto> getQuestionsForVendor(long vendorId, String dateFrom, String dateTo) throws Exception {
        return getQuestionsForVendorWithBody(vendorId,
            1, 10, "DATE", true, dateFrom, dateTo, false, fileToString("/data/vendor_question_request.json"));
    }

    private CountDto getQuestionsCountForVendor(long vendorId, String dateFrom, String dateTo) throws Exception {
        return getQuestionsCountForVendorWithBody(vendorId,
            1, 10, "DATE", true, dateFrom, dateTo, false, fileToString("/data/vendor_question_request.json"));
    }

    protected QAPager<QuestionDto> getQuestionsForVendorWithBody(long brandId,
                                                                 int pageNum,
                                                                 int pageSize,
                                                                 String sortField,
                                                                 boolean isAscending,
                                                                 String dateFrom,
                                                                 String dateTo,
                                                                 boolean withVendorAction,
                                                                 String body) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question")
                .param("userId", String.valueOf(UID))
                .param("sortField", sortField)
                .param("asc", String.valueOf(isAscending))
                .param("pageNum", String.valueOf(pageNum))
                .param("pageSize", String.valueOf(pageSize))
                .param("dateFrom", dateFrom)
                .param("dateTo", dateTo)
                .param("withVendorAction", String.valueOf(withVendorAction))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected CountDto getQuestionsCountForVendorWithBody(long brandId,
                                                          int pageNum,
                                                          int pageSize,
                                                          String sortField,
                                                          boolean isAscending,
                                                          String dateFrom,
                                                          String dateTo,
                                                          boolean withVendorAction,
                                                          String body) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question/count")
                .param("userId", String.valueOf(UID))
                .param("sortField", sortField)
                .param("asc", String.valueOf(isAscending))
                .param("pageNum", String.valueOf(pageNum))
                .param("pageSize", String.valueOf(pageSize))
                .param("dateFrom", dateFrom)
                .param("dateTo", dateTo)
                .param("withVendorAction", String.valueOf(withVendorAction))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }

    protected QAPager<QuestionDto> getQuestionsForVendorNoBody(long brandId,
                                                               int pageNum,
                                                               int pageSize,
                                                               String sortField,
                                                               boolean isAscending,
                                                               String dateFrom,
                                                               String dateTo,
                                                               boolean withVendorAction) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question")
                .param("userId", String.valueOf(UID))
                .param("sortField", sortField)
                .param("asc", String.valueOf(isAscending))
                .param("pageNum", String.valueOf(pageNum))
                .param("pageSize", String.valueOf(pageSize))
                .param("dateFrom", dateFrom)
                .param("dateTo", dateTo)
                .param("withVendorAction", String.valueOf(withVendorAction))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected CountDto getQuestionsCountForVendorNoBody(long brandId,
                                                        int pageNum,
                                                        int pageSize,
                                                        String sortField,
                                                        boolean isAscending,
                                                        String dateFrom,
                                                        String dateTo,
                                                        boolean withVendorAction) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/question/count")
                .param("userId", String.valueOf(UID))
                .param("sortField", sortField)
                .param("asc", String.valueOf(isAscending))
                .param("pageNum", String.valueOf(pageNum))
                .param("pageSize", String.valueOf(pageSize))
                .param("dateFrom", dateFrom)
                .param("dateTo", dateTo)
                .param("withVendorAction", String.valueOf(withVendorAction)),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }
}
