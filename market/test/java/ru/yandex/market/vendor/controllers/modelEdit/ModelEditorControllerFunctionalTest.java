package ru.yandex.market.vendor.controllers.modelEdit;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ir.http.AutoGenerationApi;
import ru.yandex.market.ir.http.AutoGenerationService;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.brand.BrandInfoService;
import ru.yandex.vendor.mock.AutoGenerationServiceResolver;
import ru.yandex.vendor.util.NettyRestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link ModelEditorController}.
 */
@Disabled
@DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorControllerFunctionalTest/before.csv")
class ModelEditorControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private final NettyRestClient httpExporterRestClient;
    private final WireMockServer reportMock;
    private final AutoGenerationService autoGenService;
    private final AutoGenerationServiceResolver autoGenServiceResolver;
    private final BrandInfoService brandInfoService;

    @Autowired
    public ModelEditorControllerFunctionalTest(NettyRestClient httpExporterRestClient,
                                               WireMockServer reportMock,
                                               AutoGenerationService autoGenService,
                                               AutoGenerationServiceResolver autoGenServiceResolver,
                                               BrandInfoService brandInfoService) {
        this.httpExporterRestClient = httpExporterRestClient;
        this.reportMock = reportMock;
        this.autoGenService = autoGenService;
        this.autoGenServiceResolver = autoGenServiceResolver;
        this.brandInfoService = brandInfoService;
    }

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
                    String method = invocation.getArgument(0);
                    if ("/categoryParameters/GetParameters".equals(method)) {
                        return getInputStreamResource("/http_exporter_category_parameters_response.json");
                    } else if ("/category-parameters-form".equals(method)) {
                        return getInputStreamResource("/http_exporter_category_parameters_form_response.json");
                    } else {
                        return null;
                    }
                })
                .when(httpExporterRestClient).getForObject(any(),any(), eq(Resource.class));


        when(autoGenServiceResolver.resolve()).thenReturn(autoGenService);
        when(autoGenService.getModelIsEditable(Mockito.any()))
                .thenReturn(AutoGenerationApi.GetModelIsEditableResponse.newBuilder().setModelIsEditable(true).build());
        when(autoGenService.getModelWithSku(Mockito.any()))
                .thenReturn(AutoGenerationApi.GetModelWithSkuResponse.newBuilder().build());

        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/report_response.json"))));

        brandInfoService.brandById(110401L);
    }

    /**
     * Тест проверяет, что вендорские цвета SKU, впервые переданные в заявке на создание/редактирование
     * модели (не имеющие айдишника в КВ) будут автоматически созданы при подготовке заявки и привязаны к ней
     * (SKU не существует в БД)
     */
    @Disabled
    @Test
    void testUnsavedSkuVendorColorShouldBeSavedDuringPostForNewSku() {
        String body = getStringResource("/testUnsavedSkuVendorColorShouldBeSavedDuringPostForNewSku/request.json");
        String postModelEditRequestResponse = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", body);
        String postModelEditRequestExpected = getStringResource("/testUnsavedSkuVendorColorShouldBeSavedDuringPostForNewSku/post_request_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, postModelEditRequestResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String getColorsResponse = FunctionalTestHelper.get(baseUrl + "/vendors/401/modelEdit/params/colors?uid=100500&categoryId=4954975");
        String getColorsExpected = getStringResource("/testUnsavedSkuVendorColorShouldBeSavedDuringPostForNewSku/get_colors_expected.json");
        JsonAssert.assertJsonEquals(getColorsExpected, getColorsResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String getRequestByIdResponse = FunctionalTestHelper.get(baseUrl + "/vendors/401/modelEdit/requests/1?uid=100500");
        String getRequestByIdExpected = getStringResource("/testUnsavedSkuVendorColorShouldBeSavedDuringPostForNewSku/get_requests_id_expected.json");
        JsonAssert.assertJsonEquals(getRequestByIdExpected, getRequestByIdResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что вендорские цвета SKU, впервые переданные в заявке на создание/редактирование
     * модели (не имеющие айдишника в КВ) будут автоматически созданы при подготовке заявки и привязаны к ней
     * (SKU существует в БД)
     */
    @Disabled
    @Test
    @DbUnitDataSet(after = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorControllerFunctionalTest/testUnsavedSkuVendorColorShouldBeSavedDuringPostForExistentSku/after.csv")
    void testUnsavedSkuVendorColorShouldBeSavedDuringPostForExistentSku() {
        String postSkuRequestBody = getStringResource("/testUnsavedSkuVendorColorShouldBeSavedDuringPostForExistentSku/post_sku_request.json");
        String postModelEditSkuResponse = FunctionalTestHelper.put(baseUrl + "/vendors/401/modelEdit/params/sku/100256653449?uid=100500", postSkuRequestBody);
        String postModelEditSkuExpected = getStringResource("/testUnsavedSkuVendorColorShouldBeSavedDuringPostForExistentSku/post_sku_expected.json");
        JsonAssert.assertJsonEquals(postModelEditSkuExpected, postModelEditSkuResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String body = getStringResource("/testUnsavedSkuVendorColorShouldBeSavedDuringPostForExistentSku/request.json");
        String postModelEditRequestResponse = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", body);
        String postModelEditRequestExpected = getStringResource("/testUnsavedSkuVendorColorShouldBeSavedDuringPostForExistentSku/post_request_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, postModelEditRequestResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String getColorsResponse = FunctionalTestHelper.get(baseUrl + "/vendors/401/modelEdit/params/colors?uid=100500&categoryId=4954975");
        String getColorsExpected = getStringResource("/testUnsavedSkuVendorColorShouldBeSavedDuringPostForExistentSku/get_colors_expected.json");
        JsonAssert.assertJsonEquals(getColorsExpected, getColorsResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String getRequestByIdResponse = FunctionalTestHelper.get(baseUrl + "/vendors/401/modelEdit/requests/1?uid=100500");
        String getRequestByIdExpected = getStringResource("/testUnsavedSkuVendorColorShouldBeSavedDuringPostForExistentSku/get_requests_id_expected.json");
        JsonAssert.assertJsonEquals(getRequestByIdExpected, getRequestByIdResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что если при создании заявки передать флаг isDraft, будет создана заявка со статусом DRAFT, и что
     * она доступна только по requestId (не видна в общем списке заявок)
     */
    @Test
    void testDraftModelEditorRequestCreation() {
        String body = getStringResource("/testDraftModelEditorRequestCreation/request.json");
        String postModelEditRequestResponse = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", body);
        String postModelEditRequestExpected = getStringResource("/testDraftModelEditorRequestCreation/post_request_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, postModelEditRequestResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String getRequestByIdResponse = FunctionalTestHelper.get(baseUrl + "/vendors/401/modelEdit/requests/1?uid=100500");
        String getRequestByIdExpected = getStringResource("/testDraftModelEditorRequestCreation/get_requests_id_expected.json");
        JsonAssert.assertJsonEquals(getRequestByIdExpected, getRequestByIdResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String getRequestsResponse = FunctionalTestHelper.get(baseUrl + "/vendors/401/modelEdit/requests?uid=100500");
        String getRequestsExpected = getStringResource("/testDraftModelEditorRequestCreation/get_requests_expected.json");
        JsonAssert.assertJsonEquals(getRequestsExpected, getRequestsResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что при создании SKU, она привязывается к заявке-черновику по её requestId
     */
    @Test
    void testSkuLinkToDraftModelEditorRequest() {
        String body = getStringResource("/testSkuLinkToDraftModelEditorRequest/request.json");
        String postModelEditRequestResponse = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", body);
        String postModelEditRequestExpected = getStringResource("/testSkuLinkToDraftModelEditorRequest/post_request_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, postModelEditRequestResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String postSkuRequestBody = getStringResource("/testSkuLinkToDraftModelEditorRequest/post_sku_request.json");
        String postModelEditSkuResponse = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/params/sku?uid=100500", postSkuRequestBody);
        String postModelEditSkuExpected = getStringResource("/testSkuLinkToDraftModelEditorRequest/post_sku_expected.json");
        JsonAssert.assertJsonEquals(postModelEditSkuExpected, postModelEditSkuResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String getSkuByIdResponse = FunctionalTestHelper.get(baseUrl + "/vendors/401/modelEdit/params/sku/1?uid=100500");
        String getSkuByIdExpected = getStringResource("/testSkuLinkToDraftModelEditorRequest/get_params_sku_id_expected.json");
        JsonAssert.assertJsonEquals(getSkuByIdExpected, getSkuByIdResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String getRequestByIdResponse = FunctionalTestHelper.get(baseUrl + "/vendors/401/modelEdit/requests/1?uid=100500");
        String getRequestByIdExpected = getStringResource("/testSkuLinkToDraftModelEditorRequest/get_requests_id_expected.json");
        JsonAssert.assertJsonEquals(getRequestByIdExpected, getRequestByIdResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что при создании SKU с привязкой к несуществующей заявке, получаем ошибку
     */
    @Test
    void testSkuLinkToDraftModelEditorRequestWithWrongRequestId() {
        String postSkuRequestBody = getStringResource("/testSkuLinkToDraftModelEditorRequestWithWrongRequestId/post_sku_request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/params/sku?uid=100500", postSkuRequestBody)
        );
        String postModelEditSkuExpected = getStringResource("/testSkuLinkToDraftModelEditorRequestWithWrongRequestId/post_sku_expected.json");
        JsonAssert.assertJsonEquals(postModelEditSkuExpected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }


    /**
     * Тест проверяет, что при создании SKU, привязанной к заявке-черновику, её можно получить в списке SKU по requestId заявки
     */
    @Test
    void testSkuLinkedDraftRequestCanBeObtainedByRequestId() {
        String postModelEditRequestBody = getStringResource("/testSkuLinkedDraftRequestCanBeObtainedByRequestId/post_request_request.json");

        String postModelEditRequestResponse_1 = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", postModelEditRequestBody);
        String postModelEditRequestExpected_1 = getStringResource("/testSkuLinkedDraftRequestCanBeObtainedByRequestId/post_request_expected_1.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected_1, postModelEditRequestResponse_1, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String postModelEditRequestResponse_2 = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", postModelEditRequestBody);
        String postModelEditRequestExpected_2 = getStringResource("/testSkuLinkedDraftRequestCanBeObtainedByRequestId/post_request_expected_2.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected_2, postModelEditRequestResponse_2, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String postSkuRequestBody = getStringResource("/testSkuLinkedDraftRequestCanBeObtainedByRequestId/post_sku_request.json");
        String postModelEditSkuResponse = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/params/sku?uid=100500", postSkuRequestBody);
        String postModelEditSkuExpected = getStringResource("/testSkuLinkedDraftRequestCanBeObtainedByRequestId/post_sku_expected.json");
        JsonAssert.assertJsonEquals(postModelEditSkuExpected, postModelEditSkuResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String getRequestByIdResponse_1 = FunctionalTestHelper.get(baseUrl + "/vendors/401/modelEdit/params/sku?uid=100500&requestId=1");
        String getRequestByIdExpected_1 = getStringResource("/testSkuLinkedDraftRequestCanBeObtainedByRequestId/get_skus_by_request_id_expected_1.json");
        JsonAssert.assertJsonEquals(getRequestByIdExpected_1, getRequestByIdResponse_1, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String getRequestByIdResponse_2 = FunctionalTestHelper.get(baseUrl + "/vendors/401/modelEdit/params/sku?uid=100500&requestId=2");
        String getRequestByIdExpected_2 = getStringResource("/testSkuLinkedDraftRequestCanBeObtainedByRequestId/get_skus_by_request_id_expected_2.json");
        JsonAssert.assertJsonEquals(getRequestByIdExpected_2, getRequestByIdResponse_2, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testDraftCreationRequestWithoutTitleLeadsToBadParam() {
        String body = getStringResource("/testDraftCreationRequestWithoutTitleLeadsToBadParam/post_request_request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", body)
        );
        String postModelEditRequestExpected = getStringResource("/testDraftCreationRequestWithoutTitleLeadsToBadParam/post_request_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testDraftCreationRequestWithoutCategoryLeadsToBadParam() {
        String body = getStringResource("/testDraftCreationRequestWithoutCategoryLeadsToBadParam/post_request_request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", body)
        );
        String postModelEditRequestExpected = getStringResource("/testDraftCreationRequestWithoutCategoryLeadsToBadParam/post_request_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testDraftCreationRequestWithoutCategoryIdLeadsToBadParam() {
        String body = getStringResource("/testDraftCreationRequestWithoutCategoryIdLeadsToBadParam/post_request_request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", body)
        );
        String postModelEditRequestExpected = getStringResource("/testDraftCreationRequestWithoutCategoryIdLeadsToBadParam/post_request_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testDraftCreationRequestWithoutBrandIdLeadsToBadParam() {
        String body = getStringResource("/testDraftCreationRequestWithoutBrandIdLeadsToBadParam/post_request_request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", body)
        );
        String postModelEditRequestExpected = getStringResource("/testDraftCreationRequestWithoutBrandIdLeadsToBadParam/post_request_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testDraftCreationRequestWithWrongBrandIdLeadsToBadParam() {
        String body = getStringResource("/testDraftCreationRequestWithWrongBrandIdLeadsToBadParam/post_request_request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", body)
        );
        String postModelEditRequestExpected = getStringResource("/testDraftCreationRequestWithWrongBrandIdLeadsToBadParam/post_request_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorControllerFunctionalTest/testDeleteRequest/before.csv",
            after = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorControllerFunctionalTest/testDeleteRequest/after.csv")
    @Test
    void testDeleteRequest() {
        String response = FunctionalTestHelper.delete(baseUrl + "/vendors/401/modelEdit/requests/135686?uid=67282295");
        String expected = getStringResource("/testDeleteRequest/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }


    /**
     * Валидация плохих значений баркодов
     */
    @ParameterizedTest
    @ValueSource(strings = {"2055323605093", "12345", "1055323c05093"})
    void testBadBarcodeValidation(String barcodeValue) {
        String postModelEditRequestBody = getStringResource("/testBadBarcodeValidation/post_model_request.json");

        String postModelEditRequestResponse = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", postModelEditRequestBody);
        String postModelEditRequestExpected = getStringResource("/testBadBarcodeValidation/post_model_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, postModelEditRequestResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String body = getStringResource("/testBadBarcodeValidation/request.json")
                .replaceAll("BARCODE_VALUE", barcodeValue);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/params/sku?uid=100500", body)
        );
        String postModelEditSkuExpected = getStringResource("/testBadBarcodeValidation/expected.json");
        JsonAssert.assertJsonEquals(postModelEditSkuExpected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Валидация хороших значений баркодов
     */
    @ParameterizedTest
    @ValueSource(strings = {"205532360509", "0234567891011", "1234567891011"})
    void testGoodBarcodeValidation(String barcodeValue) {
        String postModelEditRequestBody = getStringResource("/testGoodBarcodeValidation/model_request.json");

        String postModelEditRequestResponse = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/requests?uid=100500", postModelEditRequestBody);
        String postModelEditRequestExpected = getStringResource("/testGoodBarcodeValidation/post_model_expected.json");
        JsonAssert.assertJsonEquals(postModelEditRequestExpected, postModelEditRequestResponse, JsonAssert.when(IGNORING_ARRAY_ORDER));

        String body = getStringResource("/testGoodBarcodeValidation/request.json")
                .replaceAll("BARCODE_VALUE", barcodeValue);
        String postModelEditSkuActual = FunctionalTestHelper.post(baseUrl + "/vendors/401/modelEdit/params/sku?uid=100500", body);
        String postModelEditSkuExpected = getStringResource("/testGoodBarcodeValidation/expected.json");
        System.out.println(postModelEditSkuActual);
        JsonAssert.assertJsonEquals(postModelEditSkuExpected, postModelEditSkuActual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }
}
