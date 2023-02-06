package ru.yandex.market.vendor.controllers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.common.util.UrlParameterMultimap;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.opinions.ModelOpinionModel;
import ru.yandex.vendor.questions.MarketEntity;
import ru.yandex.vendor.questions.MarketUser;
import ru.yandex.vendor.questions.ModelAnswersWithPager;
import ru.yandex.vendor.questions.ModelQuestion;
import ru.yandex.vendor.questions.ModelQuestionsWithPager;
import ru.yandex.vendor.questions.QuestionAnswer;
import ru.yandex.vendor.questions.QuestionVotes;
import ru.yandex.vendor.questions.QuestionsPager;
import ru.yandex.vendor.questions.pers.GetQuestionsCountResponse;
import ru.yandex.vendor.security.Role;
import ru.yandex.vendor.util.IRestClient;
import ru.yandex.vendor.util.NettyRestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/QuestionsControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/QuestionsControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class QuestionsControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    private NettyRestClient persQaRestClient;
    @Autowired
    private WireMockServer blackboxMock;
    @Autowired
    private WireMockServer reportMock;
    @Autowired
    private NettyRestClient ugcDaemonRestClient;

    /**
     * Проверяем, что ответ от репорта пришел и обработался корректно ответ с тремя hyperId
     */
    @Test
    void testGetQuestionsForThreeHyperIds() {

        List<ModelQuestion> question = mockQuestion(300, 3);
        doAnswer(invocation -> {
            IRestClient.Request request = invocation.getArgument(0);
            long questionId = Long.parseLong(request.getEndpoint().substring(request.getEndpoint().lastIndexOf('/')+1));
            if (question.get(0).getId() == questionId) {
                return mockAnswer(3011);
            } else if (question.get(1).getId() == questionId) {
                return mockAnswer(3012);
            } else if (question.get(2).getId() == questionId) {
                return mockAnswer(3013);
            } else {
                return null;
            }
        }).when(persQaRestClient).getForObject(any());

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetQuestionsForThreeHyperIds/blackbox_response.json"))));

        doAnswer(invocation -> getInputStreamResource("/testGetQuestionsForThreeHyperIds/ugc_daemon_response.json"))
                .when(ugcDaemonRestClient).getForObject(any(), any(), any());

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/questions?uid=100500");
        String expected = getStringResource("/testGetQuestionsForThreeHyperIds/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Проверяем, что ответ от репорта пришел и обработался корректно ответ с двумя hyperId
     */
    @Test
    void testGetQuestionsForTwoHyperIds() {

        List<ModelQuestion> question = mockQuestion(300, 2);
        doAnswer(invocation -> {
            IRestClient.Request request = invocation.getArgument(0);
            long questionId = Long.parseLong(request.getEndpoint().substring(request.getEndpoint().lastIndexOf('/')+1));
            if (question.get(0).getId() == questionId) {
                return mockAnswer(3011);
            } else if (question.get(1).getId() == questionId) {
                return mockAnswer(3012);
            } else {
                return null;
            }
        }).when(persQaRestClient).getForObject(any());

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetQuestionsForTwoHyperIds/blackbox_response.json"))));

        doAnswer(
                invocation -> getInputStreamResource("/testGetQuestionsForTwoHyperIds/ugc_daemon_response.json"))
                .when(ugcDaemonRestClient).getForObject(any(), any(), any());


        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/questions?uid=100500");
        String expected = getStringResource("/testGetQuestionsForTwoHyperIds/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Проверяем, что ответ от репорта пришел и обработался корректно ответ с одним hyperId
     */
    @Test
    void testGetQuestionsForOneHyperId() {
        List<ModelQuestion> question = mockQuestion(300, 1);
        doAnswer(invocation -> {
            IRestClient.Request request = invocation.getArgument(0);
            long questionId = Long.parseLong(request.getEndpoint().substring(request.getEndpoint().lastIndexOf('/')+1));
            if (question.get(0).getId() == questionId) {
                return mockAnswer(3012);
            } else {
                return null;
            }
        }).when(persQaRestClient).getForObject(any());

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetQuestionsForOneHyperId/blackbox_response.json"))));

        doAnswer(invocation -> getInputStreamResource("/testGetQuestionsForOneHyperId/ugc_daemon_response.json"))
                .when(ugcDaemonRestClient).getForObject(any(), any(), any());

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/questions?uid=100500");
        String expected = getStringResource("/testGetQuestionsForOneHyperId/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Проверяем, что ответ от смежных систем правильно парсится и выдается ответ
     */
    @Test
    void testGetQuestions() {
        List<ModelQuestion> question = mockQuestion(300, 1);
        doAnswer(invocation -> {
            IRestClient.Request request = invocation.getArgument(0);
            long questionId = Long.parseLong(request.getEndpoint().substring(request.getEndpoint().lastIndexOf('/')+1));
            if (question.get(0).getId() == questionId) {
                return mockAnswer(3012);
            } else {
                return null;
            }
        }).when(persQaRestClient).getForObject(any());

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetQuestions/blackbox_response.json"))));

        doAnswer(invocation -> getInputStreamResource("/testGetQuestions/ugc_daemon_response.json"))
                .when(ugcDaemonRestClient).getForObject(any(), any(), any());

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", equalTo("modelinfo"))
                .willReturn(aResponse().withBody(getStringResource("/testGetQuestions/report.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", equalTo("model_skus_info"))
                .willReturn(aResponse().withBody(getStringResource("/testGetQuestions/report_blue.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/questions?uid=100500");
        String expected = getStringResource("/testGetQuestions/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Проверяем доступ в ресурс {@code GET /vendors/[vendorId]/questions} {@link QuestionsController#getVendorProductsQuestions(long, Long, LocalDateTime, LocalDateTime, Boolean, String, String, Integer, Integer, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "model_charge_free_user",
            "brand_zone_user",
            "manager_ro_user",
            "manager_user",
            "model_bid_user",
            "questions_charge_free_user",
            "recommended_shops_user",
            "support_user"
    })
    void testGetQuestionsJavaSecOk(String roleName) {
        List<ModelQuestion> question = mockQuestion(300, 1);
        doAnswer(invocation -> {
            IRestClient.Request request = invocation.getArgument(0);
            long questionId = Long.parseLong(request.getEndpoint().substring(request.getEndpoint().lastIndexOf('/')+1));
            if (question.get(0).getId() == questionId) {
                return mockAnswer(3012);
            } else {
                return null;
            }
        }).when(persQaRestClient).getForObject(any());

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetQuestionsJavaSecOk/blackbox_response.json"))));

        doAnswer(
                invocation -> {
                    UrlParameterMultimap params = invocation.getArgument(1);
                    if ("3012".equals(params.get("answerId").get(0))) {
                        return getInputStreamResource("/testGetQuestionsJavaSecOk/ugc_daemon_response_3012.json");
                    } else {
                        return null;
                    }
                })
                .when(ugcDaemonRestClient)
                .getForObject(any(), any(), any());

        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500L, 1000L);

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", equalTo("modelinfo"))
                .willReturn(aResponse().withBody(getStringResource("/testGetQuestionsJavaSecOk/report.json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1000/questions?uid=100500");
        String expected = getStringResource("/testGetQuestionsJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testParseListOfCommentsForJson() {
        doAnswer(
                invocation -> {
                    UrlParameterMultimap params = invocation.getArgument(1);
                    if ("root-5-0-3011".equals(params.get("root_id").get(0))) {
                        return getInputStreamResource("/testGetQuestionsForThreeHyperIds/ugc_daemon_response_3011.json");
                    } else if ("root-5-0-3012".equals(params.get("root_id").get(0))) {
                        return getInputStreamResource("/testGetQuestionsForThreeHyperIds/ugc_daemon_response_3012.json");
                    } else if ("root-5-0-3013".equals(params.get("root_id").get(0))) {
                        return getInputStreamResource("/testGetQuestionsForThreeHyperIds/ugc_daemon_response_3013.json");
                    } else {
                        return null;
                    }
                })
                .when(ugcDaemonRestClient)
                .getForObject(any(), any(), any());
    }

    /**
     * Проверяем отсутствие доступа в ресурс {@code GET /vendors/[vendorId]/questions} {@link QuestionsController#getVendorProductsQuestions(long, Long, LocalDateTime, LocalDateTime, Boolean, String, String, Integer, Integer, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "balance_client_user",
            "moderator_user",
            "feedback_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testGetQuestionsJavaSecForbidden(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1000L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1000/questions?uid=100500")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет доступ в ресурс {@code GET /vendors/[vendorId]/questions/count} {@link QuestionsController#getVendorProductsQuestionsCount(long, LocalDateTime, LocalDateTime, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "model_charge_free_user",
            "brand_zone_user",
            "manager_ro_user",
            "manager_user",
            "model_bid_user",
            "questions_charge_free_user",
            "recommended_shops_user",
            "support_user"
    })
    void testGetQuestionsCountJavaSecOk(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1000L);
        doAnswer(invocation -> {
            GetQuestionsCountResponse response = new GetQuestionsCountResponse();
            response.setCount(5);
            return response;
        }).when(persQaRestClient).postForObject(any(), any());

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1000/questions/count?uid=100500");
        String expected = getStringResource("/testGetQuestionsCountJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Проверяем отсутствие доступа в ресурс {@code GET /vendors/[vendorId]/questions/count} {@link QuestionsController#getVendorProductsQuestionsCount(long, LocalDateTime, LocalDateTime, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "balance_client_user",
            "moderator_user",
            "feedback_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testGetQuestionsCountJavaSecForbidden(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1000L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1000/questions/count?uid=100500")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    private ModelAnswersWithPager mockAnswer(long answerId) {
        ModelQuestionsWithPager modelQuestionsWithPager = new ModelQuestionsWithPager();
        QuestionsPager pager = new QuestionsPager();
        pager.setCount(1);
        pager.setPageNum(1);
        pager.setPageSize(1);
        pager.setTotalPageCount(1);
        modelQuestionsWithPager.setPager(pager);

        ModelAnswersWithPager answers = new ModelAnswersWithPager();
        answers.setPager(pager);

        List<QuestionAnswer> questionAnswerList = new ArrayList<>();


        questionAnswerList.add(createQuestionAnswer(answerId));
        answers.setData(questionAnswerList);
        return answers;
    }

    private QuestionAnswer createQuestionAnswer(long answerId) {

        QuestionAnswer answer = new QuestionAnswer();
        answer.setText("answer text");
        answer.setId(answerId);
        answer.setCreated(LocalDateTime.of(2018, 10, 25, 19, 10, 30));

        MarketUser marketUser = new MarketUser();
        marketUser.setUid(606060);
        answer.setUser(marketUser);

        return answer;
    }

    private List<ModelQuestion> mockQuestion(long questionId, int count) {
        ModelQuestionsWithPager modelQuestionsWithPager = new ModelQuestionsWithPager();


        QuestionsPager pager = new QuestionsPager();
        pager.setCount(1);
        pager.setPageNum(1);
        pager.setPageSize(1);
        pager.setTotalPageCount(1);
        modelQuestionsWithPager.setPager(pager);

        List<ModelQuestion> modelQuestionList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            modelQuestionList.add(createModelQuestion(questionId + i, questionId + 10 + i,
                    questionId + 20 + i, 10787832 + i));
        }
        modelQuestionsWithPager.setData(modelQuestionList);

        doAnswer(invocation -> modelQuestionsWithPager).when(persQaRestClient).postForObject(any(), any());

        return modelQuestionList;
    }

    private ModelQuestion createModelQuestion(long questionId, long modelId, long userId, long markEntityId) {

        ModelQuestion question = new ModelQuestion();

        ModelOpinionModel model = new ModelOpinionModel();
        model.setId(modelId);
        model.setTitle("model title");

        MarketUser questionUser = new MarketUser();
        questionUser.setUid(userId);

        question.setText("question text? " + questionId);
        question.setId(questionId);
        question.setCreated(LocalDateTime.of(2018, 10, 25, 18, 30, 30));
        question.setModel(model);
        question.setUser(questionUser);
        question.setAnswersCount(1);

        MarketEntity marketEntity = new MarketEntity();
        marketEntity.setEntity("SomeProduct");
        marketEntity.setId(markEntityId);
        question.setProduct(marketEntity);

        QuestionVotes questionVotes = new QuestionVotes();
        questionVotes.setDislikes(2);
        questionVotes.setLikes(10);
        questionVotes.setUserVote(55);
        question.setVotes(questionVotes);

        return question;
    }

}
