package ru.yandex.market.pers.qa.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pers.qa.client.model.SortField;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.AnswerFilter;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.QuestionFilter;
import ru.yandex.market.pers.qa.model.ResultLimit;
import ru.yandex.market.pers.qa.model.Sort;
import ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.PhotoService;
import ru.yandex.market.pers.qa.service.QuestionProductService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.saas.SaasQueryService;
import ru.yandex.market.pers.qa.utils.CommonUtils;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;
import ru.yandex.market.util.ExecUtils;

import static ru.yandex.market.pers.qa.client.model.QuestionType.CATEGORY;
import static ru.yandex.market.pers.qa.client.model.QuestionType.MODEL;
import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.AUTHOR_ID;
import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.CATEGORY_ID;
import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.ENTITY_TYPE;
import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.INTEREST_ID;
import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.MODEL_ID;
import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.QUESTION_ID;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.07.2019
 */
public class SaasMocks {
    private static final String BLOCK_TEMPLATE_WITHOUT_COMMA = "{\n" +
        "            \"Key\": \"%s\",\n" +
        "            \"Value\": \"%s\"\n" +
        "          }\n";

    private static final String BLOCK_TEMPLATE = BLOCK_TEMPLATE_WITHOUT_COMMA + ",\n";

    @Autowired
    private QuestionService questionService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private QuestionProductService questionProductService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    @Qualifier("saasHttpClient")
    protected HttpClient saasHttpClientMock;

    public HttpClient getSaasHttpClientMock() {
        return saasHttpClientMock;
    }

    public void mockShopQuestionIdsForPartnerEmptyResponse(long shopId) {
        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            200,
            mockSaasKvSEmptyResponse(),
            HttpClientMockUtils.withQueryParam("text=shop" + shopId));
    }

    public void mockShopQuestionIdsForPartner(long shopId, List<Long> questionIds) {
        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            200,
            mockSaasKvShopQuestionForPartner(shopId, questionIds),
            HttpClientMockUtils.withQueryParam("text=shop" + shopId));
    }

    public void mockShopQuestionIdsForPartner500() {
        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            500,
            mockSaasKvSEmptyResponse(),
            HttpClientMockUtils.withQueryParam("text=shop" + 1));
    }

    @NotNull
    private org.mockito.stubbing.Answer<InputStream> mockSaasKvShopQuestionForPartner(long shopId, List<Long> questionIds) {
        return argument -> {
            String response = loadTemplate("/saas/saas_kv_result_template.json")
                .replaceAll("URL_PLACEHOLDER", "shop" + shopId)
                .replaceAll("PROPERTIES_PLACEHOLDER", questionIds.isEmpty() ? "" : questionIds.stream()
                    .map(questionId -> String.format(BLOCK_TEMPLATE_WITHOUT_COMMA, "question_id", questionId))
                    .collect(Collectors.joining(","))
                );
            return new ByteArrayInputStream(response.getBytes());
        };
    }

    @NotNull
    private org.mockito.stubbing.Answer<InputStream> mockSaasKvSEmptyResponse() {
        return argument -> {
            String response = loadTemplate("/saas/saas_kv_empty.json");
            return new ByteArrayInputStream(response.getBytes());
        };
    }

    public void mockModelCountCallExcludeUid(long authorId, long modelId, List<Long> idList) {
        ArgumentMatcher<HttpUriRequest> entityFilter = HttpClientMockUtils.and(
            SaasSearchMockUtils.withSearchAttribute(MODEL_ID.getName(),
                String.valueOf(modelId)),
            SaasSearchMockUtils.withSearchExcludeAttribute(AUTHOR_ID.getName(),
                String.valueOf(authorId))
        );

        mockModelCountCall(idList, entityFilter);
    }

    public void mockModelCountCall(long modelId, List<Long> idList) {
        ArgumentMatcher<HttpUriRequest> entityFilter = SaasSearchMockUtils
            .withSearchAttribute(MODEL_ID.getName(), String.valueOf(modelId));

        mockModelCountCall(idList, entityFilter);
    }

    private void mockModelCountCall(List<Long> idList, ArgumentMatcher<HttpUriRequest> entityFilter) {
        mockModelPageCall(1, 1, idList, HttpStatus.SC_OK, entityFilter);
    }

    private void mockCategoryCountCallWithModel(long hid, List<Long> idList) {
        mockCategoryCountCall(idList, getCategoryEntityFilter(hid, false));
    }

    private void mockCategoryCountCallLite(long hid, List<Long> idList) {
        mockCategoryCountCall(idList, getCategoryEntityFilter(hid, true));
    }

    private void mockCategoryCountCall(List<Long> idList, ArgumentMatcher<HttpUriRequest> entityFilter) {
        mockCategoryPageCall(1, 1, idList, HttpStatus.SC_OK, entityFilter);
    }

    public void mockCategoryCallWithModel(long hid, long pageSize, List<Long> idList) {
        mockCategoryCall(pageSize, idList, getCategoryEntityFilter(hid, false));
    }

    public void mockCategoryCallLite(long hid, long pageSize, List<Long> idList) {
        mockCategoryCall(pageSize, idList, getCategoryEntityFilter(hid, true));
    }

    public void mockAnswerCountCall(long questionId, List<Long> idList) {
        ArgumentMatcher<HttpUriRequest> entityFilter = SaasSearchMockUtils
            .withSearchAttribute(QUESTION_ID.getName(), String.valueOf(questionId));

        mockAnswerCountCall(idList, entityFilter);
    }

    private void mockAnswerCountCall(List<Long> idList, ArgumentMatcher<HttpUriRequest> entityFilter) {
        mockAnswersPageCall(1, 1, idList, HttpStatus.SC_OK, entityFilter);
    }

    public void mockAnswerCall(long questionId, long pageSize, List<Long> idList) {
        ArgumentMatcher<HttpUriRequest> entityFilter = SaasSearchMockUtils
            .withSearchAttribute(QUESTION_ID.getName(), String.valueOf(questionId));

        mockAnswerCall(pageSize, idList, entityFilter);
    }

    public void mockAnswerCallExcludeUid(long questionId, long authorId, long pageSize, List<Long> idList) {
        ArgumentMatcher<HttpUriRequest> entityFilter = HttpClientMockUtils.and(
            SaasSearchMockUtils.withSearchAttribute(QUESTION_ID.getName(),
                String.valueOf(questionId)),
            SaasSearchMockUtils.withSearchExcludeAttribute(AUTHOR_ID.getName(),
                String.valueOf(authorId))
        );

        mockAnswerCall(pageSize, idList, entityFilter);
    }

    public void mockAnswerCallByUid(long authorId, long pageSize, List<Long> idList) {
        ArgumentMatcher<HttpUriRequest> entityFilter = HttpClientMockUtils.and(
                SaasSearchMockUtils.withSearchAttribute(AUTHOR_ID.getName(), String.valueOf(authorId))
        );

        mockAnswerCall(pageSize, idList, entityFilter);
    }

    public void mockModelCall(long modelId, long pageSize, List<Long> idList) {
        ArgumentMatcher<HttpUriRequest> entityFilter = SaasSearchMockUtils
            .withSearchAttribute(MODEL_ID.getName(), String.valueOf(modelId));

        mockModelCountCall(idList, entityFilter);

        long lastPage = idList.size() / pageSize + 1;
        for (long page = 1; page <= lastPage + 1; ++page) {
            mockModelPageCall(
                page,
                pageSize,
                idList,
                HttpStatus.SC_OK,
                entityFilter);
        }
    }

    public void mockAuthorQuestionCall(long authorId, long pageSize, List<Long> idList) {
        ArgumentMatcher<HttpUriRequest> entityFilter = HttpClientMockUtils.and(
                HttpClientMockUtils.withQueryParam("how", "create_dt"),
                SaasSearchMockUtils.withSearchAttribute(ENTITY_TYPE.getName(),
                    String.valueOf(MODEL.getValue()), String.valueOf(CATEGORY.getValue())),
                SaasSearchMockUtils.withSearchAttribute(AUTHOR_ID.getName(), String.valueOf(authorId))
        );

        mockModelCountCall(idList, entityFilter);

        long lastPage = idList.size() / pageSize + 1;
        for (long page = 1; page <= lastPage + 1; ++page) {
            mockModelPageCall(
                    page,
                    pageSize,
                    idList,
                    HttpStatus.SC_OK,
                    entityFilter);
        }
    }

    private void mockModelPageCall(long page,
                                   long pageSize,
                                   List<Long> idList,
                                   int httpStatus,
                                   ArgumentMatcher<HttpUriRequest> entityFilter) {

        int count = idList.size();
        List<Long> idSubList = ResultLimit.page(page, pageSize).sublist(idList);

        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            httpStatus,
            mockQuestionListSaasResponse(count, idSubList),
            HttpClientMockUtils.and(
                entityFilter,
                SaasSearchMockUtils.pageFilter(page, pageSize)
            ));
    }

    public void mockQuestionBulkCall(List<Long> idList) {
        mockQuestionBulkCall(idList, Collections.emptyList());
    }


    public void mockQuestionBulkCall(List<Long> idList, List<Long> withoutIds) {
        List<Long> publicIds = idList.stream()
            .filter(x -> questionService.getQuestionsCount(new QuestionFilter().id(x)) > 0)
            .collect(Collectors.toList());
        publicIds.removeAll(withoutIds);
        int count = publicIds.size();

        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            HttpStatus.SC_OK,
            mockQuestionListSaasResponse(count, publicIds),
            HttpClientMockUtils.and(
                SaasSearchMockUtils.withSearchAttribute("s_id", CommonUtils.toStrArray(idList)),
                SaasSearchMockUtils.pageFilter(1, idList.size())
            ));
    }

    public void mockModelAgitationCall(long userId, List<Long> modelIds, long pageSize) {
        List<Long> questionIds = questionService.getQuestionsForAgitationForTests(MODEL, userId, modelIds, pageSize);

        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            HttpStatus.SC_OK,
            mockQuestionListSaasResponse(questionIds.size(), questionIds),
            HttpClientMockUtils.and(
                SaasSearchMockUtils.withSearchAttribute("s_model_id", CommonUtils.toStrArray(modelIds)),
                SaasSearchMockUtils.withSearchExcludeAttribute(AUTHOR_ID.getName(), String.valueOf(userId)),
                HttpClientMockUtils.withQueryParam("g",
                    String.format("2.s_model_id.%s.%s.....rlv.0.count",
                        modelIds.size(),
                        SaasQueryService.calculateAgitationLimitPerGroup(pageSize, modelIds.size())))
            ));
    }

    private void mockCategoryCall(long pageSize,
                                  List<Long> idList,
                                  ArgumentMatcher<HttpUriRequest> entityFilter) {
        mockCategoryCountCall(idList, entityFilter);

        long lastPage = idList.size() / pageSize + 1;
        for (long page = 1; page <= lastPage; ++page) {
            mockCategoryPageCall(page, pageSize, idList, HttpStatus.SC_OK, entityFilter);
        }
    }

    public void mockCategoryPageCall(long page,
                                     long pageSize,
                                     List<Long> idList,
                                     int httpStatus,
                                     ArgumentMatcher<HttpUriRequest> entityFilter) {
        int count = idList.size();
        List<Long> idSubList = ResultLimit.page(page, pageSize).sublist(idList);

        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            httpStatus,
            mockQuestionListSaasResponse(count, idSubList),
            HttpClientMockUtils.and(
                entityFilter,
                SaasSearchMockUtils.pageFilter(page, pageSize)
            ));
    }

    private void mockAnswerCall(long pageSize,
                                List<Long> idList,
                                ArgumentMatcher<HttpUriRequest> entityFilter) {
        mockAnswerCountCall(idList, entityFilter);

        long lastPage = idList.size() / pageSize + 1;
        for (long page = 1; page <= lastPage + 1; ++page) {
            mockAnswersPageCall(
                page,
                pageSize,
                idList,
                HttpStatus.SC_OK,
                entityFilter);
        }
    }

    private void mockAnswersPageCall(long page,
                                     long pageSize,
                                     List<Long> idList,
                                     int httpStatus,
                                     ArgumentMatcher<HttpUriRequest> entityFilter) {

        int count = idList.size();
        List<Long> idSubList = ResultLimit.page(page, pageSize).sublist(idList);

        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            httpStatus,
            mockAnswerSaasResponse(count, idSubList),
            HttpClientMockUtils.and(
                entityFilter,
                SaasSearchMockUtils.pageFilter(page, pageSize)
            ));
    }

    public static ArgumentMatcher<HttpUriRequest> getCategoryEntityFilter(long hid, boolean isLight) {
        return isLight
            ? SaasSearchMockUtils.entityFilter(CATEGORY, hid)
            : SaasSearchMockUtils.withSearchAttribute(CATEGORY_ID.getName(), String.valueOf(hid));
    }

    public void mockBestAnswersSaasCall(long pageSize,
                                        List<Long> idList,
                                        int answersLimit) {
        long lastPage = idList.size() / pageSize + 1;

        for (long page = 1; page <= lastPage + 1; ++page) {
            List<Long> idSubList = ResultLimit.page(page, pageSize).sublist(idList);
            String[] idSubArray = idSubList.stream().map(String::valueOf).toArray(String[]::new);

            if (idSubList.isEmpty()) {
                continue;
            }

            HttpClientMockUtils.mockResponse(
                saasHttpClientMock,
                HttpStatus.SC_OK,
                mockBestAnswersSaasResponse(idList.size(), idSubList, answersLimit),
                HttpClientMockUtils.and(
                    SaasSearchMockUtils.withSearchAttribute(QUESTION_ID.getName(), idSubArray),
                    HttpClientMockUtils.withQueryParam("g", "2.s_question_id.[1-9].[1-9].....rlv.0.count")
                ));
        }
    }

    @NotNull
    public org.mockito.stubbing.Answer<InputStream> mockQuestionListSaasResponse(int count, List<Long> questionIds) {
        return argument -> {
            String response = loadTemplate("/saas/saas_result_template.json")
                .replaceAll("\"insert_count_here\"", String.valueOf(count))
                .replaceAll("\"insert_groups_here\"", questionIds.isEmpty() ? "" : questionIds.stream()
                    .map(this::mockSaasQuestionDocument)
                    .collect(Collectors.joining(","))
                );
            return new ByteArrayInputStream(response.getBytes());
        };
    }

    @NotNull
    private String mockSaasQuestionDocument(long id) {
        Question question = questionService.getQuestionByIdInternal(id);
        long answersCount = answerService.getAnswersCount(new AnswerFilter().questionId(id));

        String skuId = "";
        if (question.getDetails() != null && question.getDetails().get(Question.SKU_KEY) != null) {
            skuId = ",\n{\"Key\": \"sku\",\"Value\": \"" + question.getDetails().get(Question.SKU_KEY) + "\"}";
        }

        String PHOTO_JSON_TEMPLATE =
            ",\n{\"Key\": \"photos\", \"Value\": \"%s:%s:%s:%d\"}";
        String photos = photoService.getPhotos(QaEntityType.QUESTION, List.of(id))
            .getOrDefault(id, Collections.emptyList())
            .stream()
            .map(photo -> String.format(PHOTO_JSON_TEMPLATE, photo.getNamespace(), photo.getGroupId(),
                photo.getImageName(), photo.getOrderNumber()))
            .collect(Collectors.joining("\n"));

        String PRODUCT_IDS_JSON_TEMPLATE =
                ",\n{\"Key\": \"question_products\", \"Value\": \"%s:%d\"}";
        List<Long> productIdsList = questionProductService.getProductIds(List.of(id)).getOrDefault(id,
                Collections.emptyList());
        String productIds = IntStream.range(0, productIdsList.size()).boxed()
                .map(i -> String.format(PRODUCT_IDS_JSON_TEMPLATE, productIdsList.get(i), i))
                .collect(Collectors.joining("\n"));

        SaasQaSearchAttribute entityAttribute;
        switch (question.getQuestionType()){
            case MODEL: entityAttribute = MODEL_ID; break;
            case CATEGORY: entityAttribute = CATEGORY_ID; break;
            case INTEREST: entityAttribute = INTEREST_ID; break;
            default:throw new RuntimeException("Unexpected question type");
        }

        return loadTemplate("/saas/saas_question_group.json")
            .replaceAll("insert_id_here", String.valueOf(id))
            .replaceAll("insert_user_id_here", question.getUserId())
            .replaceAll("insert_text_here", question.getText())
            .replaceAll("insert_ans_cnt_here", String.valueOf(answersCount))
            .replaceAll("insert_etype_here", entityAttribute.getName())
            .replaceAll("insert_etype_id_here", String.valueOf(question.getQuestionType().getValue()))
            .replaceAll("insert_eid_here", question.getEntityId())
            .replaceAll("insert_cr_time_here", String.valueOf(question.getTimestamp().toEpochMilli()))
            .replaceAll("insert_photos_here", photos)
            .replaceAll("insert_product_ids_here", productIds)
            .replaceAll("insert_sku_id_here", skuId);

    }

    @NotNull
    private org.mockito.stubbing.Answer<InputStream> mockAnswerSaasResponse(int count, List<Long> idSubList) {
        return argument -> {
            String response = loadTemplate("/saas/saas_result_template.json")
                .replaceAll("\"insert_count_here\"", String.valueOf(count))
                .replaceAll("\"insert_groups_here\"", idSubList.isEmpty() ? ""
                    : idSubList.stream()
                    .map(this::mockSaasAnswer)
                    .collect(Collectors.joining(","))
                );
            return new ByteArrayInputStream(response.getBytes());
        };
    }

    @NotNull
    private org.mockito.stubbing.Answer<InputStream> mockBestAnswersSaasResponse(int count,
                                                                                 List<Long> idSubList,
                                                                                 int answersLimit) {
        return argument -> {
            String response = loadTemplate("/saas/saas_result_template.json")
                .replaceAll("\"insert_count_here\"", String.valueOf(count))
                .replaceAll("\"insert_groups_here\"", idSubList.isEmpty() ? ""
                    : idSubList.stream()
                    .map(id -> mockSaasBestAnswerGroup(id, answersLimit))
                    .collect(Collectors.joining(","))
                );
            return new ByteArrayInputStream(response.getBytes());
        };
    }

    @NotNull
    private String mockSaasAnswer(long id) {
        Answer answer = answerService.getAnswerByIdInternal(id);

        return loadTemplate("/saas/saas_answer_group.json")
            .replaceAll("insert_docs_here", buildAnswerDoc(answer));
    }

    @NotNull
    private String mockSaasBestAnswerGroup(long id, int answersLimit) {
        AnswerFilter filter = new AnswerFilter()
            .questionId(id)
            .sort(Sort.desc(SortField.ID))
            .resultLimit(ResultLimit.limited(answersLimit));
        List<Answer> answers = answerService.getAnswers(filter);
        long count = answerService.getAnswersCount(filter);

        return loadTemplate("/saas/saas_answer_group_by_question.json")
            .replaceAll("insert_question_id", String.valueOf(id))
            .replaceAll("insert_answers_count", String.valueOf(count))
            .replaceAll("insert_docs_here",
                answers.stream()
                    .map(this::buildAnswerDoc)
                    .collect(Collectors.joining(","))
            );
    }

    private String buildAnswerDoc(Answer answer) {
        String authorBlock = "";
        if (answer.getBrandId() != null) {
            authorBlock = String.format(BLOCK_TEMPLATE, "brand_id", answer.getBrandId());
        } else if (answer.getShopId() != null) {
            authorBlock = String.format(BLOCK_TEMPLATE, "shop_id", answer.getShopId());
        }

        return loadTemplate("/saas/saas_answer_doc.json")
            .replaceAll("insert_id_here", String.valueOf(answer.getId()))
            .replaceAll("insert_text_here", answer.getText())
            .replaceAll("insert_user_id_here", answer.getUserId())
            .replaceAll("insert_question_id_here", String.valueOf(answer.getQuestionId()))
            .replaceAll("insert_vendor_fl_here", answer.getBrandId() != null ? "1" : "0")
            .replaceAll("insert_shop_fl_here", answer.getShopId() != null ? "1" : "0")
            .replaceAll("insert_author_block_here", authorBlock);
    }

    private String loadTemplate(String path) {
        try {
            return IOUtils.toString(getTemplate(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw ExecUtils.silentError(e);
        }
    }

    private InputStream getTemplate(String path) {
        return getClass().getResourceAsStream(path);
    }

}
