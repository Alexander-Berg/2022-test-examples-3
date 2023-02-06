package ru.yandex.market.vendor.controllers;

import java.time.LocalDateTime;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.common.util.UrlParameterMultimap;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.model.opinions.ModelOpinionCommentView;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.security.Role;
import ru.yandex.vendor.util.IRestClient;
import ru.yandex.vendor.util.NettyRestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/VendorOpinionsControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/VendorOpinionsControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class VendorOpinionsControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    @Autowired
    private WireMockServer blackboxMock;

    @Autowired
    private WireMockServer reportMock;

    @Autowired
    @Qualifier("ugcDaemonRestClient")
    private NettyRestClient ugcDaemonRestClient;

    @Autowired
    @Qualifier("persGradeRestClient")
    private NettyRestClient persGradeRestClient;

    /**
     * Тест на проверку ответа ресурса {@code GET /vendors/[vendor_id]/opinions/count}
     * {@link VendorOpinionsController#getVendorProductsOpinionsCount(long, long)}
     * Запрос без параметров поиска
     */
    @Test
    void testGetOpinions() {

        doAnswer(invocation -> getInputStreamResource("/testGetOpinions/ugcDaemon_response.json"))
                .when(ugcDaemonRestClient)
                .getForObject(any());
        doAnswer(invocation -> getInputStreamResource("/testGetOpinions/persGrade_response.json"))
                .when(persGradeRestClient)
                .getForObject(any());
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinions/report_response.json"))));
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinions/blackbox_response.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/opinions?uid=100500");
        String expected = getStringResource("/testGetOpinions/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Проверка заполнения модели для синих")
    void testGetOpinionsFillSkuPlace() {

        doAnswer(invocation -> getInputStreamResource("/testGetOpinions/ugcDaemon_response.json"))
                .when(ugcDaemonRestClient)
                .getForObject(any());
        doAnswer(invocation -> getInputStreamResource("/testGetOpinions/persGrade_response.json"))
                .when(persGradeRestClient)
                .getForObject(any());
        reportMock.stubFor(get(urlEqualTo("/?place=modelinfo&rids=0&bsformat=2&all-gl-mbo-params=true&hyperid=1724548318&hyperid=1724548319"))
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinionsFillSkuPlace/report_response_white.json"))));
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinionsFillSkuPlace/report_response_blue.json"))));
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinions/blackbox_response.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/opinions?uid=100500");
        String expected = getStringResource("/testGetOpinionsFillSkuPlace/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Проверка статуса CanEdit")
    void testGetOpinionsWithCanEditTrue() {
        doAnswer(invocation -> getInputStreamResource("/testGetOpinionsWithCanEditTrue/ugcDaemon_response.json"))
                .when(ugcDaemonRestClient)
                .getForObject(any());
        doAnswer(invocation -> getInputStreamResource("/testGetOpinionsWithCanEditTrue/persGrade_response.json"))
                .when(persGradeRestClient)
                .getForObject(any());
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetOpinionsWithCanEditTrue/report_response.json")
                )));
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetOpinionsWithCanEditTrue/blackbox_response.json")
                )));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/opinions?uid=100500");
        String expected = getStringResource("/testGetOpinionsWithCanEditTrue/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку ответа ресурса {@code GET /vendors/[vendor_id]/opinions/count}
     * {@link VendorOpinionsController#getVendorProductsOpinionsCount(long, long)}
     * Запрос по model_id
     */
    @Test
    void testGetOpinionsByModelId() {

        doAnswer(invocation -> {
            IRestClient.Request request = invocation.getArgument(0);
            if ("10713122".equals(request.getParams().get("modelId").get(0))) {
                return getInputStreamResource("/testGetOpinionsByModelId/persDrade_response.json");
            } else {
                return null;
            }
        }).when(persGradeRestClient).getForObject(any());
        doAnswer(invocation -> {
            UrlParameterMultimap params = invocation.getArgument(1);
            if ("59726298".equals(params.get("gradeId").get(0))) {
                return getInputStreamResource("/testGetOpinionsByModelId/ugcDaemon_response.json");
            } else {
                return null;
            }
        }).when(ugcDaemonRestClient).getForObject(any(), any(), eq(Resource.class));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("hyperid", equalTo("10713122"))
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinionsByModelId/report.json"))));
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinionsByModelId/blackBox_response.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/opinions?uid=100500&modelId=10713122");
        String expected = getStringResource("/testGetOpinionsByModelId/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку ответа ресурса {@code GET /vendors/[vendor_id]/opinions/count}
     * {@link VendorOpinionsController#getVendorProductsOpinionsCount(long, long)}
     * Запрос по периоду
     */
    @Test
    void testGetOpinionsByDates() {

        doAnswer(invocation -> {
            IRestClient.Request request = invocation.getArgument(0);
            if ("10.11.2017".equals(request.getParams().get("dateFrom").get(0)) && "10.11.2018".equals(request.getParams().get(
                    "dateTo").get(0))) {
                return getInputStreamResource("/testGetOpinionsByDates/persGrade_response.json");
            } else {
                return null;
            }
        }).when(persGradeRestClient).getForObject(any());

        doAnswer(invocation -> getInputStreamResource("/testGetOpinionsByDates/ugcDaemon_response.json")
        ).when(ugcDaemonRestClient).getForObject(any());

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinionsByDates/blackBox_response.json"))));

        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinionsByDates/report.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/opinions?uid=100500&from=2017-11-10T18:04" +
                ":21Z&to=2018-11-10T18:04:21Z");
        String expected = getStringResource("/testGetOpinionsByDates/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку ответа ресурса {@code GET /vendors/[vendor_id]/opinions/count}
     * {@link VendorOpinionsController#getVendorProductsOpinionsCount(long, long)}
     * Запрос по оценке (grade)
     */
    @Test
    void testGetOpinionsByGrade() {

        doAnswer(invocation -> {
            IRestClient.Request request = invocation.getArgument(0);
            if ("1".equals(request.getParams().get("gr0").get(0))) {
                return getInputStreamResource("/testGetOpinionsByGrade/persGrade_response.json");
            } else {
                return null;
            }
        }).when(persGradeRestClient).getForObject(any());

        doAnswer(invocation -> getInputStreamResource("/testGetOpinionsByGrade/ugcDaemon_response.json"))
                .when(ugcDaemonRestClient).getForObject(any());

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinionsByGrade/blackBox_response.json"))));

        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinionsByGrade/report.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/opinions?uid=100500&grade=1");
        String expected = getStringResource("/testGetOpinionsByGrade/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку доступа в ресурс {@code GET /vendors/[vendor_id]/opinions}
     * {@link VendorOpinionsController#getVendorProductsOpinions(long, Long, LocalDateTime, LocalDateTime, Integer, Boolean, Boolean, String, String, Integer, Integer, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "model_charge_free_user",
            "brand_zone_user",
            "manager_ro_user",
            "manager_user",
            "model_bid_user",
            "feedback_charge_free_user",
            "recommended_shops_user",
            "support_user",
            "balance_client_user"

    })
    void testGetOpinionsJavaSecOk(String roleName) {
        if ("balance_client_user".equals(roleName)) {
            initBalanceServiceByClientId(550);
        } else {
            setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1000L);
        }

        doAnswer(invocation -> getInputStreamResource("/testGetOpinionsJavaSecOk/ugcDaemon_response.json"))
                .when(ugcDaemonRestClient)
                .getForObject(any(), any(), any());
        doAnswer(invocation -> getInputStreamResource("/testGetOpinionsJavaSecOk/persGrade_response.json"))
                .when(persGradeRestClient)
                .getForObject(any());
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinionsJavaSecOk/report_response.json"))));
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetOpinionsJavaSecOk/blackbox_response.json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1000/opinions?uid=100500");
        String expected = getStringResource("/testGetOpinionsJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку отсутсвия доступа в ресурс {@code GET /vendors/[vendor_id]/opinions}
     * {@link VendorOpinionsController#getVendorProductsOpinions(long, Long, LocalDateTime, LocalDateTime, Integer, Boolean, Boolean, String, String, Integer, Integer, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "moderator_user",
            "questions_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testGetOpinionsJavaSecForbidden(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1000L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1000/opinions?uid=100500")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(),
                JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку доступа пользователя другой карточки в ресурс {@code GET /vendors/[vendor_id]/opinions}
     * {@link VendorOpinionsController#getVendorProductsOpinions(long, Long, LocalDateTime, LocalDateTime, Integer, Boolean, Boolean, String, String, Integer, Integer, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "model_charge_free_user",
            "brand_zone_user",
            "model_bid_user",
            "feedback_charge_free_user",
            "recommended_shops_user",
            "balance_client_user"
    })
    void testGetOpinionsForAnotherCardUser(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1001L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1000/opinions?uid=100500")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(),
                JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку ресурса {@code GET /vendors/[vendor_id]/opinions/count}
     * {@link VendorOpinionsController#getVendorProductsOpinionsCount(long, long)}
     * Получение количества отзывов для вендора
     */
    @Test
    void testGetOpinionsCount() {
        doAnswer(invocation -> getInputStreamResource("/testGetOpinionsCount/persGrade_response.json"))
                .when(persGradeRestClient).getForObject(any());

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1000/opinions/count?uid=100500");
        String expected = getStringResource("/testGetOpinionsCount/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку доступа в ресурс {@code GET /vendors/[vendor_id]/opinions/count}
     * {@link VendorOpinionsController#getVendorProductsOpinionsCount(long, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "model_charge_free_user",
            "brand_zone_user",
            "manager_ro_user",
            "manager_user",
            "model_bid_user",
            "feedback_charge_free_user",
            "recommended_shops_user",
            "support_user",
            "balance_client_user"
    })
    void testGetOpinionsCountJavaSecOk(String roleName) {
        if ("balance_client_user".equals(roleName)) {
            initBalanceServiceByClientId(550);
        } else {
            setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1000L);
        }
        doAnswer(invocation -> getInputStreamResource("/testGetOpinionsCount/persGrade_response.json"))
                .when(persGradeRestClient).getForObject(any());

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1000/opinions/count?uid=100500");
        String expected = getStringResource("/testJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку отсутствия доступа в ресурс {@code GET /vendors/[vendor_id]/opinions/count}
     * {@link VendorOpinionsController#getVendorProductsOpinionsCount(long, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "moderator_user",
            "questions_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testGetOpinionsCountJavaSecForbidden(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1000L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1000/opinions/count?uid=100500")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(),
                JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку доступа пользователя другой карточки в ресурс {@code GET /vendors/[vendor_id]/opinions/count}
     * {@link VendorOpinionsController#getVendorProductsOpinionsCount(long, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "model_charge_free_user",
            "brand_zone_user",
            "model_bid_user",
            "feedback_charge_free_user",
            "recommended_shops_user",
            "balance_client_user"
    })
    void testGetOpinionsCountForAnotherCardUser(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1001L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1000/opinions/count?uid=100500")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(),
                JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет успешное добваление комментария вендором
     *
     * @code POST /vendors/{vendorId}/opinions/{opinionId}/comment
     * {@link VendorOpinionsController#postOpinionCommentFromVendor(long, long, Long, long, ModelOpinionCommentView)}
     */
    @Test
    void testPostOpinionCommentFromVendor() {
        doAnswer(invocation -> getInputStreamResource("/testPostOpinionCommentFromVendor/marketUtils_response.json"))
                .when(ugcDaemonRestClient)
                .postForObject(any(), any());

        String body = getStringResource("/testPostOpinionCommentFromVendor/request.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/1000/opinions/59942725/comment?uid=100500",
                body);
        String expected = getStringResource("/testPostOpinionCommentFromVendor/expected.json");

        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет доступ в ресурс POST /vendors/{vendorId}/opinions/{opinionId}/comment
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "feedback_charge_free_user",
            "manager_user",
            "support_user"
    })
    void testPostOpinionsCommentFromVendorJavaSecOk(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1000L);
        String body = getStringResource("/testPostOpinionCommentFromVendor/request.json");
        doAnswer(invocation -> getInputStreamResource("/testPostOpinionCommentFromVendor/marketUtils_response.json"))
                .when(ugcDaemonRestClient)
                .postForObject(any(), any());

        String response = FunctionalTestHelper.postWithAuth(baseUrl + "/vendors/1000/opinions/59942725/comment?uid" +
                "=100500", body);
        String expected = getStringResource("/testJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет отсутсвие доступа в ресурс POST /vendors/{vendorId}/opinions/{opinionId}/comment
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "model_charge_free_user",
            "brand_zone_user",
            "model_bid_user",
            "recommended_shops_user",
            "balance_client_user",
            "moderator_user",
            "questions_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testPostOpinionsCommentFromVendorJavaSecForbidden(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1001L);
        String body = getStringResource("/testPostOpinionCommentFromVendor/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.postWithAuth(baseUrl + "/vendors/1000/opinions/59942725/comment?uid=100500"
                        , body)
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(),
                JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет успешное редактирование комментария вендором
     *
     * @code PUT /vendors/{vendorId}/{opinionId}/comment/{commentId}
     * {@link VendorOpinionsController#putOpinionCommentFromVendor(long, long, long, Long, long, ModelOpinionCommentView)}
     */
    @Test
    void testPutOpinionsCommentFromVendor() {

        doAnswer(invocation -> getInputStreamResource("/testPutOpinionsCommentFromVendor/ugcDaemon_response_get.json"))
                .when(ugcDaemonRestClient)
                .getForObject(any());
        doAnswer(invocation -> ResponseEntity.ok(getInputStreamResource("/testPutOpinionsCommentFromVendor/ugcDaemon_response.json")))
                .when(ugcDaemonRestClient)
                .patchForResponse(any(), any());

        String body = getStringResource("/testPutOpinionsCommentFromVendor/request.json");
        String response = FunctionalTestHelper.put(baseUrl + "/vendors/1000/opinions/59745315/comment/100066146?uid" +
                "=100500", body);
        String expected = getStringResource("/testPutOpinionsCommentFromVendor/expected.json");

        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет доступ в ручку PUT /vendors/{vendorId}/{opinionId}/comment/{commentId}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "feedback_charge_free_user",
            "manager_user",
            "support_user"
    })
    void testPutOpinionsCommentFromVendorJavaSecOk(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1000L);
        String body = getStringResource("/testPutOpinionsCommentFromVendor/request.json");

        doAnswer(invocation -> getInputStreamResource("/testPutOpinionsCommentFromVendor/ugcDaemon_response_get.json"))
                .when(ugcDaemonRestClient)
                .getForObject(any());
        doAnswer(invocation -> ResponseEntity.ok(getInputStreamResource("/testPutOpinionsCommentFromVendor/ugcDaemon_response.json")))
                .when(ugcDaemonRestClient)
                .patchForResponse(any(), any());

        String response = FunctionalTestHelper.putWithAuth(baseUrl + "/vendors/1000/opinions/59745315/comment" +
                "/100066146?uid=100500", body);
        String expected = getStringResource("/testPutOpinionsCommentFromVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет отсутсвие доступа в ресурс PUT /vendors/{vendorId}/{opinionId}/comment/{commentId}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "model_charge_free_user",
            "brand_zone_user",
            "model_bid_user",
            "recommended_shops_user",
            "balance_client_user",
            "moderator_user",
            "questions_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testPutOpinionsCommentFromVendorJavaForbidden(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 1001L);
        String body = getStringResource("/testPutOpinionsCommentFromVendor/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.putWithAuth(baseUrl + "/vendors/1000/opinions/59745315/comment/100066146" +
                        "?uid=100500", body)
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(),
                JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /*
     * @sbakanova: тест пока закомментировала - нужно решение в тикете VNDMARKET-3203 и соотвествующее исправление в
     * тесте
     * Проверяет, что от пришедший ответ с 400 ошибкой от marketUtilsClient
     * обрабатывается и не кидается в ответ 500
     * @code POST /vendors/{vendorId}/opinions/{opinionId}/comment
     */
    /*@Test
    void testPostOpinionCommentFromVendorWithError400() throws Exception {
        String body = getStringResource("/testPostOpinionCommentFromVendorWithError400/request.json");

        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(marketUtilsClient)
                .postBrandModelOpinionComment(anyLong(), anyLong(), any());

        String response = FunctionalTestHelper.post(baseUrl + "/vendors/1000/opinions/12345678901234/comment?uid=0",
        body);
        String expected = getStringResource("/testPostOpinionCommentFromVendorWithError400/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }*/

    /**
     * Тест проверяет успешное удаление комментария вендором
     *
     * @code DELETE /vendors/{vendorId}/{opinionId}/comment/{commentId}
     * {@link VendorOpinionsController#deleteOpinionCommentFromVendor(long, long, long, Long, long)}
     */
    @Test
    void testDeleteOpinionsCommentFromVendor() {

        doAnswer(invocation -> getInputStreamResource("/testDeleteOpinionsCommentFromVendor/ugcDaemon_response_get.json"))
                .when(ugcDaemonRestClient)
                .getForObject(any());
        doAnswer(invocation -> ResponseEntity.ok(getInputStreamResource("/testDeleteOpinionsCommentFromVendor/ugcDaemon_response.json")))
                .when(ugcDaemonRestClient)
                .deleteForResponse(any(), any());

        String response = FunctionalTestHelper.delete(
                baseUrl + "/vendors/1000/opinions/60145423/comment/100082201?uid=100500");
        String expected = getStringResource("/testDeleteOpinionsCommentFromVendor/expected.json");

        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

}
