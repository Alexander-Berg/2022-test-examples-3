package ru.yandex.market.pers.qa.controller.api.external;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.client.model.SortField;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.mock.SaasMocks;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Sort;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.UpdateModelVendorIdService;
import ru.yandex.market.util.ExecUtils;
import ru.yandex.market.util.FormatUtils;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 04.04.2019
 */
public class PartnerShopControllerQuestionTest extends QAControllerTest {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .withZone(ZoneId.systemDefault());

    private static final long SHOP_ID = 3458276452L;
    private static final long SHOP_ID_OTHER = 239859235L;

    private static final int FIRST_PAGE = 1;
    private static final int SECOND_PAGE = 2;

    @Autowired
    private UpdateModelVendorIdService modelVendorIdService;

    @Autowired
    private SaasMocks saasMocks;

    @Autowired
    private QuestionService questionService;

    @Test
    public void testShopQuestionsByShopModels() throws Exception {
        List<Long> questionIds = LongStream.of(buildQuestions(20)).boxed().collect(Collectors.toList());
        List<Pair<Long, ModState>> rejectedQuestions = Arrays.stream(ModState.values())
            .filter(modState -> !modState.isPublished())
            .map(modState -> Pair.of(questionIds.get(modState.getValue()), modState))
            .collect(Collectors.toList());
        questionService.updateModStates(rejectedQuestions);
        long deletedQuestionId = questionIds.get(questionIds.size() - 1);
        questionService.deleteQuestion(deletedQuestionId, ControllerTest.UID);

        saasMocks.mockShopQuestionIdsForPartner(SHOP_ID, questionIds);

        QAPager<QuestionDto> questionDtoQAPager = getPartnerQuestionsByModels(SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            questionIds.size(),
            null,
            x -> x
        );

        Assertions.assertEquals(20 - rejectedQuestions.size() - 1, questionDtoQAPager.getPager().getCount());
        Assertions.assertFalse(questionDtoQAPager.getData().stream()
            .map(QuestionDto::getId)
            .anyMatch(id -> id == deletedQuestionId
                || rejectedQuestions.stream().anyMatch(idWithModState -> idWithModState.getLeft().equals(id))));
    }

    @Test
    public void testEmptySaasResponse() throws Exception {
        List<Long> questionIds = LongStream.of(buildQuestions(20)).boxed().collect(Collectors.toList());
        List<Pair<Long, ModState>> rejectedQuestions = Arrays.stream(ModState.values())
            .filter(modState -> !modState.isPublished())
            .map(modState -> Pair.of(questionIds.get(modState.getValue()), modState))
            .collect(Collectors.toList());
        questionService.updateModStates(rejectedQuestions);
        long deletedQuestionId = questionIds.get(questionIds.size() - 1);
        questionService.deleteQuestion(deletedQuestionId, ControllerTest.UID);

        saasMocks.mockShopQuestionIdsForPartnerEmptyResponse(SHOP_ID);

        QAPager<QuestionDto> questionDtoQAPager = getPartnerQuestionsByModels(SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            null,
            x -> x
        );

        Assertions.assertTrue(questionDtoQAPager.getData().isEmpty());
        Assertions.assertEquals(0L, questionDtoQAPager.getPager().getCount());
    }

    @Test
    void testBasicCall() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        Set<Long> expected = getExpected(sourceQuestions, 0, DEF_PAGE_SIZE);

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            null,
            x -> x
        );

        assertEquals(DEF_PAGE_SIZE, questions.getData().size());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testCallNoHids() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            Collections.emptyList(),
            x -> x
        );

        assertEquals(0, questions.getData().size());
    }

    @Test
    void testCallMultipleHids() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        List<Integer> hids = Arrays.asList(2, 5, 8, 13, 15, 23, 42);
        Set<Long> expected = getExpected(sourceQuestions, hids);

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x
        );

        assertEquals(hids.size(), questions.getData().size());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testCallBrand() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        List<Integer> hids = Arrays.asList(2, 5, 8, 13, 15, 23, 42);

        long brandId = 42341;
        modelVendorIdService.saveModelVendor(2L, brandId);
        modelVendorIdService.saveModelVendor(3L, brandId);
        modelVendorIdService.saveModelVendor(5L, brandId + 1);

        Set<Long> expected = new HashSet<>(Arrays.asList(
            sourceQuestions[2]
        ));

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            Collections.singletonList(brandId),
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x);

        assertEquals(expected.size(), questions.getData().size());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testCallBrandMultiple() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        List<Integer> hids = Arrays.asList(2, 5, 8, 13, 15, 23, 42);

        long brandId = 42341;
        modelVendorIdService.saveModelVendor(2L, brandId);
        modelVendorIdService.saveModelVendor(3L, brandId);
        modelVendorIdService.saveModelVendor(5L, brandId + 1);
        modelVendorIdService.saveModelVendor(15L, brandId + 2);

        Set<Long> expected = new HashSet<>(Arrays.asList(
            sourceQuestions[2],
            sourceQuestions[15]
        ));

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            Lists.newArrayList(brandId, brandId + 2),
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x);

        assertEquals(expected.size(), questions.getData().size());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testCallModel() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        List<Integer> hids = Arrays.asList(2, 5, 8, 13, 15, 23, 42);

        long modelId = 13;

        Set<Long> expected = new HashSet<>(Arrays.asList(
            sourceQuestions[13]
        ));

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            modelId,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x
        );

        assertEquals(expected.size(), questions.getData().size());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testCallWithAnswers() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        List<Integer> hids = Arrays.asList(2, 5, 8, 13, 15, 23, 42);

        createShopAnswer(SHOP_ID, sourceQuestions[2]);
        createShopAnswer(SHOP_ID_OTHER, sourceQuestions[13]);

        Set<Long> expected = new HashSet<>(Arrays.asList(
            sourceQuestions[2],
            sourceQuestions[13]
        ));

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x.param("withAnswers", "true")
        );

        assertEquals(expected.size(), questions.getData().size());
        assertEquals(expected.size(), questions.getPager().getCount());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testCallWithOwnAnswers() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        List<Integer> hids = Arrays.asList(2, 5, 8, 13, 15, 23, 42);

        createShopAnswer(SHOP_ID, sourceQuestions[2]);
        createAnswer(sourceQuestions[8]);
        createShopAnswer(SHOP_ID_OTHER, sourceQuestions[13]);
        createShopAnswer(SHOP_ID, sourceQuestions[23]);

        Set<Long> expected = new HashSet<>(Arrays.asList(
            sourceQuestions[2],
            sourceQuestions[23]
        ));

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x.param("withOwnAnswers", "true")
        );

        assertEquals(expected.size(), questions.getData().size());
        assertEquals(expected.size(), questions.getPager().getCount());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testCallWithRivalAnswers() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        List<Integer> hids = Arrays.asList(2, 5, 8, 13, 15, 23, 42);

        createShopAnswer(SHOP_ID, sourceQuestions[2]);
        createAnswer(sourceQuestions[8]);
        createShopAnswer(SHOP_ID_OTHER, sourceQuestions[13]);
        createShopAnswer(SHOP_ID_OTHER, sourceQuestions[42]);

        Set<Long> expected = new HashSet<>(Arrays.asList(
            sourceQuestions[13],
            sourceQuestions[42]
        ));

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x.param("withRivalAnswers", "true")
        );

        assertEquals(expected.size(), questions.getData().size());
        assertEquals(expected.size(), questions.getPager().getCount());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testCallWithNeedToAnswer() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        List<Integer> hids = Arrays.asList(2, 5, 8, 13, 15, 23, 42);

        createShopAnswer(SHOP_ID, sourceQuestions[2]);
        createAnswer(sourceQuestions[8]);
        createShopAnswer(SHOP_ID_OTHER, sourceQuestions[13]);
        createShopAnswer(SHOP_ID_OTHER, sourceQuestions[42]);

        Set<Long> expected = new HashSet<>(Arrays.asList(
            sourceQuestions[5],
            sourceQuestions[8],
            sourceQuestions[15],
            sourceQuestions[23]
        ));

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x.param("withOwnAnswers", "false")
                .param("withRivalAnswers", "false")
        );

        assertEquals(expected.size(), questions.getData().size());
        assertEquals(expected.size(), questions.getPager().getCount());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testCallWithoutAnswers() throws Exception {
        long[] sourceQuestions = buildQuestions(100);
        List<Integer> hids = Arrays.asList(2, 5, 8, 13, 15, 23, 42);

        createShopAnswer(SHOP_ID, sourceQuestions[2]);
        createShopAnswer(SHOP_ID_OTHER, sourceQuestions[13]);

        Set<Long> expected = new HashSet<>(Arrays.asList(
            sourceQuestions[5],
            sourceQuestions[8],
            sourceQuestions[15],
            sourceQuestions[23],
            sourceQuestions[42]
        ));

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x.param("withAnswers", "false")
        );

        assertEquals(expected.size(), questions.getData().size());
        assertEquals(expected.size(), questions.getPager().getCount());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testCallWithDateFilter() throws Exception {
        final Instant now = Instant.now();
        final Instant from = now.minus(1, DAYS);
        final Instant to = now.plus(1, DAYS);

        final String dateFrom = DATE_TIME_FORMATTER.format(from);
        final String dateTo = DATE_TIME_FORMATTER.format(to);

        long[] source = {
            createQuestionTimed(MODEL_ID, CATEGORY_HID, -2, TimeUnit.DAYS),
            createQuestionTimed(MODEL_ID + 1, CATEGORY_HID, -1, TimeUnit.HOURS),
            createQuestionTimed(MODEL_ID + 2, CATEGORY_HID, 1, TimeUnit.MINUTES),
            createQuestionTimed(MODEL_ID + 3, CATEGORY_HID, 15, TimeUnit.HOURS),
            createQuestionTimed(MODEL_ID + 4, CATEGORY_HID, 2, TimeUnit.DAYS),
        };

        List<Long> hids = Collections.singletonList(CATEGORY_HID);

        Set<Long> expected = new HashSet<>(
            Arrays.asList(
                source[1],
                source[2],
                source[3]
            )
        );

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x.param("dateFrom", dateFrom)
                .param("dateTo", dateTo)
        );

        assertEquals(expected.size(), questions.getData().size());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @Test
    void testSortAscId() throws Exception {
        int pageSize = 4;

        long[] sourceQuestions = buildQuestions(100);

        // first page
        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            pageSize,
            Sort.asc(SortField.ID),
            null,
            x -> x
        );

        long[] expected = {
            sourceQuestions[0],
            sourceQuestions[1],
            sourceQuestions[2],
            sourceQuestions[3],
        };
        assertEquals(expected.length, questions.getData().size());
        assertArrayEquals(expected, getDataIdsArray(questions));

        // second page
        questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            SECOND_PAGE,
            pageSize,
            Sort.asc(SortField.ID),
            null,
            x -> x
        );

        expected = new long[]{
            sourceQuestions[4],
            sourceQuestions[5],
            sourceQuestions[6],
            sourceQuestions[7],
        };
        assertEquals(expected.length, questions.getData().size());
        assertArrayEquals(expected, getDataIdsArray(questions));
    }

    @Test
    void testSortDescId() throws Exception {
        int pageSize = 4;

        int countTotal = 100;
        long[] sourceQuestions = buildQuestions(countTotal);

        // first page
        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            FIRST_PAGE,
            pageSize,
            Sort.desc(SortField.ID),
            null,
            x -> x
        );

        long[] expected = {
            sourceQuestions[countTotal - 1],
            sourceQuestions[countTotal - 2],
            sourceQuestions[countTotal - 3],
            sourceQuestions[countTotal - 4],
        };
        assertEquals(expected.length, questions.getData().size());
        assertArrayEquals(expected, getDataIdsArray(questions));

        // second page
        questions = getPartnerQuestions(
            SHOP_ID,
            null,
            null,
            SECOND_PAGE,
            pageSize,
            Sort.desc(SortField.ID),
            null,
            x -> x
        );

        expected = new long[]{
            sourceQuestions[countTotal - 5],
            sourceQuestions[countTotal - 6],
            sourceQuestions[countTotal - 7],
            sourceQuestions[countTotal - 8],
        };
        assertEquals(expected.length, questions.getData().size());
        assertArrayEquals(expected, getDataIdsArray(questions));
    }

    @Test
    void testEverything() throws Exception {
        final Instant now = Instant.now();
        final Instant from = now.minus(1, DAYS);
        final Instant to = now.plus(1, DAYS);

        final String dateFrom = DATE_TIME_FORMATTER.format(from);
        final String dateTo = DATE_TIME_FORMATTER.format(to);

        long[] sourceQuestions = buildQuestions(100);
        List<Integer> hids = Arrays.asList(2, 5, 8, 13, 15, 23, 42);

        // valid by hids
        alterTime(sourceQuestions[2], -10, TimeUnit.DAYS);
        alterTime(sourceQuestions[5], -2, TimeUnit.DAYS);
        alterTime(sourceQuestions[8], -1, TimeUnit.HOURS);
        alterTime(sourceQuestions[13], 1, TimeUnit.MINUTES);
        alterTime(sourceQuestions[15], 15, TimeUnit.HOURS);
        alterTime(sourceQuestions[23], 2, TimeUnit.DAYS);
        alterTime(sourceQuestions[42], 1, TimeUnit.SECONDS);

        // other
        alterTime(sourceQuestions[1], -1, TimeUnit.HOURS);
        alterTime(sourceQuestions[44], 1, TimeUnit.MINUTES);

        // answers
        createAnswer(sourceQuestions[13]);
        createShopAnswer(SHOP_ID, sourceQuestions[8]);

        //brands
        long vendorId = 222L;
        modelVendorIdService.saveModelVendor(15L, vendorId);

        Set<Long> expected = new HashSet<>(
            Arrays.asList(
                sourceQuestions[15]
            )
        );

        QAPager<QuestionDto> questions = getPartnerQuestions(
            SHOP_ID,
            15L,
            Collections.singletonList(vendorId),
            FIRST_PAGE,
            DEF_PAGE_SIZE,
            Sort.asc(SortField.ID),
            hids,
            x -> x.param("dateFrom", dateFrom)
                .param("dateTo", dateTo)
                .param("withAnswers", "false")
        );

        assertEquals(expected.size(), questions.getData().size());
        assertTrue(expected.containsAll(getDataIds(questions)));
    }

    @NotNull
    private List<Long> getDataIds(QAPager<QuestionDto> questions) {
        return questions.getData().stream()
            .map(QuestionDto::getId)
            .collect(Collectors.toList());
    }

    @NotNull
    private long[] getDataIdsArray(QAPager<QuestionDto> questions) {
        return getDataIds(questions).stream()
            .mapToLong(x -> x)
            .toArray();
    }

    private Set<Long> getExpected(long[] source, int from, int to) {
        return Arrays.stream(Arrays.copyOfRange(source, from, to))
            .boxed()
            .collect(Collectors.toSet());
    }

    private Set<Long> getExpected(long[] source, List<Integer> hids) {
        return hids.stream()
            .map(x -> source[x])
            .collect(Collectors.toSet());
    }

    private void createShopAnswer(long shopId, long questionId) throws Exception {
        long answerId = createAnswer(questionId);

        //TODO create with special partner api
        qaJdbcTemplate.update(
            "update qa.answer " +
                "set shop_id = ? " +
                "where id = ?",
            shopId,
            answerId
        );
    }

    private long[] buildQuestions(long count) {
        return LongStream.range(0, count)
            .map(x -> {
                    try {
                        return createModelQuestionHid(x, x);
                    } catch (Exception e) {
                        throw ExecUtils.silentError(e);
                    }
                }
            )
            .toArray();
    }

    protected QAPager<QuestionDto> getPartnerQuestions(long shopId,
                                                       Long modelId,
                                                       List<Long> brandId,
                                                       int pageNum,
                                                       int pageSize,
                                                       Sort sort,
                                                       List<? extends Number> hids,
                                                       Function<MockHttpServletRequestBuilder,
                                                           MockHttpServletRequestBuilder> fun
    ) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            fun.apply(post("/partner/shop/" + shopId + "/questions"))
                .param("modelId", modelId != null ? String.valueOf(modelId) : null)
                .param("brandId", brandId != null && brandId.size() == 1 ? String.valueOf(brandId.get(0)) : null)
                .param("userId", String.valueOf(UID))
                .param("sortField", sort.getField().name())
                .param("asc", String.valueOf(sort.isAscending()))
                .param("pageNum", String.valueOf(pageNum))
                .param("pageSize", String.valueOf(pageSize))

                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(hidsBody(hids, brandId)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    protected QAPager<QuestionDto> getPartnerQuestionsByModels(long shopId,
                                                               Long modelId,
                                                               List<Long> brandId,
                                                               int pageNum,
                                                               int pageSize,
                                                               List<? extends Number> hids,
                                                               Function<MockHttpServletRequestBuilder,
                                                                   MockHttpServletRequestBuilder> fun
    ) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            fun.apply(post("/partner/shop/" + shopId + "/questions/by/models"))
                .param("modelId", modelId != null ? String.valueOf(modelId) : null)
                .param("brandId", brandId != null && brandId.size() == 1 ? String.valueOf(brandId.get(0)) : null)
                .param("userId", String.valueOf(UID))
                .param("pageNum", String.valueOf(pageNum))
                .param("pageSize", String.valueOf(pageSize))

                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(hidsBody(hids, brandId)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QuestionDto>>() {
        });
    }

    /**
     * this is only test that tests QaRuntimeException resolves to 500 response
     */
    @Test
    public void getPartnerQuestionsByModelsGets5xx() throws Exception {
        saasMocks.mockShopQuestionIdsForPartner500();
        invokeAndRetrieveResponse(
            post("/partner/shop/1/questions/by/models")
                .param("userId", String.valueOf(UID))
                .param("pageNum", String.valueOf(FIRST_PAGE))
                .param("pageSize", "1")

                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(hidsBody(null, null)),
            status().is5xxServerError());
    }

    private String hidsBody(List<? extends Number> hids, List<Long> brands) {
        Map<String, Object> data = new HashMap<>();
        if (hids != null) {
            data.put("hids", hids);
        }
        if (brands != null && brands.size() > 1) {
            data.put("brands", brands);
        }

        return FormatUtils.toJson(data);
    }

    private long createQuestionTimed(Long model, long hid, long delta, TimeUnit timeUnit) throws Exception {
        long id = createModelQuestionHid(model, hid);
        alterTime(id, delta, timeUnit);
        return id;
    }

    private void alterTime(long questionId, long delta, TimeUnit timeUnit) {
        String sign = delta > 0 ? "+" : "-";
        long deltaSec = timeUnit.toSeconds(Math.abs(delta));

        qaJdbcTemplate.update(
            "update qa.question \n" +
                "set cr_time = now() " + sign + " interval '" + deltaSec + "' second \n" +
                "where id = ?", questionId);
    }

}
