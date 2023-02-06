package ru.yandex.market.vendor.controllers.modelEdit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ir.http.AutoGenerationService;
import ru.yandex.market.ir.http.MboRobot;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.ir.http.ProtocolMessage;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.documents.S3Connection;
import ru.yandex.vendor.mock.AutoGenerationServiceResolver;
import ru.yandex.vendor.util.VendorFiles;

/**
 * Функциональные тесты на {@link ShopFeedsController}.
 */
@Disabled
class ShopFeedsControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private static final int MBO_PROCESS_REQUEST_ID = 235235;
    private static final int MBO_SOURCE_ID = 5467423;
    private static final long SHOP_ID = 774L;
    private static final long USER_ID = 1000L;

    @Autowired
    private PartnerContentService partnerContentService;

    @Autowired
    private AutoGenerationServiceResolver autoGenServiceResolver;

    @Autowired
    private S3Connection s3Connection;

    @Test
    @DisplayName("Получение списка фидов магазина")
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/getFeeds/before.csv"
    )
    void getFeeds() throws IOException {
        String urlTemplate = baseUrl + "/shops/{shopId}/modelEdit/feeds?uid={uid}&page={page}&pageSize={pageSize}";
        String response = FunctionalTestHelper.get(urlTemplate, SHOP_ID, USER_ID, 2, 3);
        String resourceName = "ShopFeedsControllerFunctionalTest/getFeeds/response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение списка фидов магазина")
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/getFeeds/before.csv"
    )
    void getFeedsWithDefaultArguments() throws IOException {
        String urlTemplate = baseUrl + "/shops/{shopId}/modelEdit/feeds?uid={uid}";
        String response = FunctionalTestHelper.get(urlTemplate, SHOP_ID, USER_ID);
        String resourceName = "ShopFeedsControllerFunctionalTest/getFeedsWithDefaultArguments/response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, response);
    }

    @ParameterizedTest
    @DisplayName("Получение списка фидов магазина с указанием страницы {0}")
    @ValueSource(ints = {1, 2, 10, 100, 1000, 10000})
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/getFeeds/before.csv"
    )
    void getFeedsWithDifferentPage(int pageNumber) throws IOException {
        String urlTemplate = baseUrl + "/shops/{shopId}/modelEdit/feeds?uid={uid}&page={page}";
        FunctionalTestHelper.get(urlTemplate, SHOP_ID, USER_ID, pageNumber);
    }

    @ParameterizedTest
    @DisplayName("Получение списка фидов магазина с размером страницы {0}")
    @ValueSource(ints = {1, 2, 10, 100, 999, 1000})
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/getFeeds/before.csv"
    )
    void getFeedsWithDifferentPageSize(int pageSize) throws IOException {
        String urlTemplate = baseUrl + "/shops/{shopId}/modelEdit/feeds?uid={uid}&pageSize={pageSize}";
        FunctionalTestHelper.get(urlTemplate, SHOP_ID, USER_ID, pageSize);
    }

    @ParameterizedTest
    @DisplayName("Получение списка фидов магазина cо слишком большим размером страницы: {0}")
    @ValueSource(ints = {1001, 1002, 2000})
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/getFeeds/before.csv"
    )
    void getFeedsWithTooLargePageSize(int pageSize) throws IOException {
        String urlTemplate = baseUrl + "/shops/{shopId}/modelEdit/feeds?uid={uid}&page={page}&pageSize={pageSize}";
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class, () -> {
            FunctionalTestHelper.get(urlTemplate, SHOP_ID, USER_ID, 2, pageSize);
        });
        String resourceName = "ShopFeedsControllerFunctionalTest/getFeedsWithTooLargePageSize/response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @ParameterizedTest
    @DisplayName("Получение списка фидов магазина c указание страницы с номером {0}")
    @ValueSource(ints = {0, -1, -3})
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/getFeeds/before.csv"
    )
    void getFeedsWithNegativePage(int pageNumber) throws IOException {
        String urlTemplate = baseUrl + "/shops/{shopId}/modelEdit/feeds?uid={uid}&page={page}&pageSize={pageSize}";
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class, () -> {
            FunctionalTestHelper.get(urlTemplate, SHOP_ID, USER_ID, pageNumber, 10);
        });
        String resourceName = "ShopFeedsControllerFunctionalTest/getFeedsWithNegativePage/response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @ParameterizedTest
    @DisplayName("Получение списка фидов магазина c размером страницы {0}")
    @ValueSource(ints = {0, -1, -3})
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/getFeeds/before.csv"
    )
    void getFeedsWithNegativePageSize(int pageSize) throws IOException {
        String urlTemplate = baseUrl + "/shops/{shopId}/modelEdit/feeds?uid={uid}&page={page}&pageSize={pageSize}";
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class, () -> {
            FunctionalTestHelper.get(urlTemplate, SHOP_ID, USER_ID, 2, pageSize);
        });
        String resourceName = "ShopFeedsControllerFunctionalTest/getFeedsWithNegativePageSize/response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Добавление фида")
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/addFeed/before.csv",
            after = "ShopFeedsControllerFunctionalTest/addFeed/after.csv"
    )
    void addFeed() throws IOException {
        String feedsUrl = UriComponentsBuilder.fromUriString(baseUrl + "/shops/{shopId}/modelEdit/feeds")
                .queryParam("uid", USER_ID)
                .buildAndExpand(SHOP_ID)
                .toUriString();
        Mockito.when(partnerContentService.addFile(Mockito.any()))
                .thenReturn(
                        PartnerContent.ProcessRequest.newBuilder()
                                .setProcessRequestId(MBO_PROCESS_REQUEST_ID)
                                .build()
                );
        String response = FunctionalTestHelper.postMultipartData(feedsUrl, mockMultipartFile());
        Mockito.verify(partnerContentService).addFile(Mockito.argThat(
                request -> request.hasIsDynamic()
                        && !request.getIsDynamic()
                        && request.getSourceId() == MBO_SOURCE_ID
                        && request.getUrl().equals("https://vendors-public.s3.mdst.yandex.net/template-feed/template-feed-1.xls")
        ));
        String resourceName = "ShopFeedsControllerFunctionalTest/addFeed/response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Добавление и скачивание фида")
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/addFeedAndDownloadFile/before.csv"
    )
    void addFeedAndDownloadFile() throws IOException {
        AmazonS3 amazonS3 = s3Connection.getS3();
        Mockito.when(partnerContentService.addFile(Mockito.any()))
                .thenReturn(
                        PartnerContent.ProcessRequest.newBuilder()
                                .setProcessRequestId(MBO_PROCESS_REQUEST_ID)
                                .build()
                );
        S3Object s3Object = Mockito.mock(S3Object.class);
        Mockito.when(amazonS3.getObject(Mockito.any(String.class), Mockito.any(String.class)))
                .thenReturn(s3Object);
        Mockito.when(s3Object.getObjectContent())
                .thenAnswer(invocation -> new S3ObjectInputStream(new ByteArrayInputStream("qwerty".getBytes()), null));
        String feedsUrl = UriComponentsBuilder.fromUriString(baseUrl + "/shops/{shopId}/modelEdit/feeds")
                .queryParam("uid", USER_ID)
                .buildAndExpand(SHOP_ID)
                .toUriString();
        String addResponse = FunctionalTestHelper.postMultipartData(feedsUrl, mockMultipartFile());
        String resourceName = "ShopFeedsControllerFunctionalTest/addFeedAndDownloadFile/addResponse.json";
        String expectedAddResponse = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expectedAddResponse, addResponse);

        ArgumentCaptor<String> bucketNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> s3KeyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(amazonS3).putObject(bucketNameCaptor.capture(), s3KeyCaptor.capture(), Mockito.any(File.class));

        String bucketName = bucketNameCaptor.getValue();
        String s3Key = s3KeyCaptor.getValue();

        String feedUrl =
                UriComponentsBuilder.fromUriString(baseUrl + "/shops/{shopId}/modelEdit/feeds/{feedId}/download")
                        .queryParam("uid", USER_ID)
                        .buildAndExpand(SHOP_ID, 1)
                        .toUriString();
        ResponseEntity<byte[]> response = FunctionalTestHelper.getAsEntity(feedUrl, byte[].class);
        Mockito.verify(amazonS3).getObject(Mockito.eq(bucketName), Mockito.eq(s3Key));
        Assertions.assertEquals("application/vnd.ms-excel", response.getHeaders().getContentType().toString());
        Assertions.assertEquals("xls_simplified_10101.xls", response.getHeaders().getContentDisposition().getFilename());
        Assertions.assertArrayEquals("qwerty".getBytes(), response.getBody());
    }

    @Disabled
    @Test
    @DisplayName("Добавление фида для партнёра у которого ещё нет Source'а в MBO")
    @DbUnitDataSet(
            after = "ShopFeedsControllerFunctionalTest/addFeedWithoutExistingSource/after.csv"
    )
    void addFeedWithoutExistingSource() throws IOException {
        String feedsUrl = UriComponentsBuilder.fromUriString(baseUrl + "/shops/{shopId}/modelEdit/feeds")
                .queryParam("uid", USER_ID)
                .buildAndExpand(SHOP_ID)
                .toUriString();
        AutoGenerationService autoGenerationService = Mockito.mock(AutoGenerationService.class);
        Mockito.when(autoGenServiceResolver.resolve()).thenReturn(autoGenerationService);
        Mockito.when(autoGenerationService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder().setSourceId(MBO_SOURCE_ID).build());
        Mockito.when(partnerContentService.addFile(Mockito.any()))
                .thenReturn(
                        PartnerContent.ProcessRequest.newBuilder()
                                .setProcessRequestId(MBO_PROCESS_REQUEST_ID)
                                .build()
                );
        String response = FunctionalTestHelper.postMultipartData(feedsUrl, mockMultipartFile());
        Mockito.verify(autoGenerationService).addSource(Mockito.argThat(
                request -> request.getAuthorUid() == USER_ID
                        && request.getSourceName().equals("CV_2_" + SHOP_ID)
                        && request.getShopId() == SHOP_ID
                        && request.getUrl().equals("https://partner.market.yandex.ru/" + SHOP_ID)
        ));
        Mockito.verify(partnerContentService).addFile(Mockito.argThat(
                request -> request.hasIsDynamic()
                        && !request.getIsDynamic()
                        && request.getSourceId() == MBO_SOURCE_ID
                        && request.getUrl().equals("https://vendors-public.s3.mdst.yandex.net/template-feed/template-feed-1.xls")
        ));
        String resourceName = "ShopFeedsControllerFunctionalTest/addFeedWithoutExistingSource/response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Добавление фида с GOOD CONTENT")
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/addFeed/before.csv",
            after = "ShopFeedsControllerFunctionalTest/addFeed/after.csv"
    )
    void addFeedTrueGoodContent() throws IOException {
        String response = postFile(true);
        Mockito.verify(partnerContentService).addFile(Mockito.argThat(
                request -> request.hasIsDynamic()
                        && !request.getIsDynamic()
                        && request.getSourceId() == MBO_SOURCE_ID
                        && request.getFileContentType() == PartnerContent.FileContentType.GOOD_XLS
                        && request.getUrl().equals("https://vendors-public.s3.mdst.yandex.net/template-feed/template-feed-1.xls")
        ));
        String resourceName = "ShopFeedsControllerFunctionalTest/addFeed/response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Добавление фида с не GOOD CONTENT")
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/addFeed/before.csv",
            after = "ShopFeedsControllerFunctionalTest/addFeed/after.csv"
    )
    void addFeedFalseGoodContent() throws IOException {
        String response = postFile(false);
        Mockito.verify(partnerContentService).addFile(Mockito.argThat(
                request -> request.hasIsDynamic()
                        && !request.getIsDynamic()
                        && request.getSourceId() == MBO_SOURCE_ID
                        && request.getFileContentType() == PartnerContent.FileContentType.BETTER_XLS
                        && request.getUrl().equals("https://vendors-public.s3.mdst.yandex.net/template-feed/template-feed-1.xls")
        ));
        String resourceName = "ShopFeedsControllerFunctionalTest/addFeed/response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, response);
    }

    private String postFile(boolean goodContent) {
        String feedsUrl = UriComponentsBuilder.fromUriString(baseUrl + "/shops/{shopId}/modelEdit/feeds")
                .queryParam("uid", USER_ID)
                .queryParam("good_content", goodContent)
                .buildAndExpand(SHOP_ID)
                .toUriString();
        Mockito.when(partnerContentService.addFile(Mockito.any()))
                .thenReturn(
                        PartnerContent.ProcessRequest.newBuilder()
                                .setProcessRequestId(MBO_PROCESS_REQUEST_ID)
                                .build()
                );
        return FunctionalTestHelper.postMultipartData(feedsUrl, mockMultipartFile());
    }

    @Test
    @DisplayName("Добавление фида без GOOD CONTENT")
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/addFeed/before.csv",
            after = "ShopFeedsControllerFunctionalTest/addFeed/after.csv"
    )
    void addFeedWithoutGoodContent() throws IOException {
        String feedsUrl = UriComponentsBuilder.fromUriString(baseUrl + "/shops/{shopId}/modelEdit/feeds")
                .queryParam("uid", USER_ID)
                .buildAndExpand(SHOP_ID)
                .toUriString();
        Mockito.when(partnerContentService.addFile(Mockito.any()))
                .thenReturn(
                        PartnerContent.ProcessRequest.newBuilder()
                                .setProcessRequestId(MBO_PROCESS_REQUEST_ID)
                                .build()
                );
        String response = FunctionalTestHelper.postMultipartData(feedsUrl, mockMultipartFile());
        Mockito.verify(partnerContentService).addFile(Mockito.argThat(
                request -> request.hasIsDynamic()
                        && !request.getIsDynamic()
                        && request.getSourceId() == MBO_SOURCE_ID
                        && request.getFileContentType() == PartnerContent.FileContentType.BETTER_XLS
                        && request.getUrl().equals("https://vendors-public.s3.mdst.yandex.net/template-feed/template-feed-1.xls")
        ));
        String resourceName = "ShopFeedsControllerFunctionalTest/addFeed/response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, response);
    }

    @ParameterizedTest
    @DisplayName("Получение деталей по процессу обработки фида")
    @DbUnitDataSet(
            before = "ShopFeedsControllerFunctionalTest/getFeeds/before.csv"
    )
    @MethodSource("getFileInfoResponses")
    void getFeedDetails(String filePrefix, PartnerContent.FileInfoResponse getFileInfoResponse) throws IOException {
        Mockito.when(partnerContentService.getFileInfo(Mockito.any())).thenReturn(getFileInfoResponse);
        String urlTemplate = baseUrl + "/shops/{shopId}/modelEdit/feeds/{feedId}?uid={uid}";
        String response = FunctionalTestHelper.get(urlTemplate, SHOP_ID, 16, USER_ID);
        String resourceName = "ShopFeedsControllerFunctionalTest/getFeedDetails/" + filePrefix + ".response.json";
        String expected = VendorFiles.readText(this.getClass(), resourceName, StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, response);
    }

    private static MultipartFile mockMultipartFile() {
        return new MockMultipartFile(
                "file",
                "xls_simplified_10101.xls",
                "application/vnd.ms-excel",
                "qwerty".getBytes()
        );
    }

    private static Stream<Arguments> getFileInfoResponses() {
        return Stream.of(
                Arguments.of("getFileInfoResponse.Error1", mockErrorGetFileInfoResponse1()),
                Arguments.of("getFileInfoResponse.Error2", mockErrorGetFileInfoResponse2()),
                Arguments.of("getFileInfoResponse.Success1", mockSuccessGetFileInfoResponse1())
        );
    }

    @SuppressWarnings({
            "checkstyle:magicNumber",
            "checkstyle:lineLength"
    })
    private static PartnerContent.FileInfoResponse mockErrorGetFileInfoResponse1() {
        PartnerContent.FileInfoResponse.Builder builder = PartnerContent.FileInfoResponse.newBuilder();
        builder.setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED);
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.sku.not_exist_sku")
                .setParams("{\"shopSKU\":\"7890\"}")
                .setTemplate("Shop_sku {{shopSKU}} не существует."));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.sku.wrong_shop_sku")
                .setParams("{\"shopSKU\":\"7890\", \"maxSkuLength\": 80, \"validSymbols\": \"[A-Za-z0-9_А-ЯЁа-яё]\"}")
                .setTemplate("Переданное shop_sku {{shopSKU}} имеет неправильный формат. " +
                        "Должно иметь {{maxSkuLength}} длину и может содержать только {{validSymbols}} символы."));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.excel_invalid_file_format")
                .setParams("{\"message\":\"\u042D\u0442\u043E \u043D\u0435 Excel\"}")
                .setTemplate("\u041D\u0435\u043F\u0440\u0430\u0432\u0438\u043B\u044C\u043D\u044B\u0439 \u0444\u043E\u0440\u043C\u0430\u0442 \u0444\u0430\u0439\u043B\u0430: {{message}}"));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.params.invalid_param_value")
                .setParams("{\"correctValue\":\"Adidas\",\"shopSKUs\":[\"1234\", \"1235\"],\"receivedValue\":\"Abibas\",\"paramName\":\"\u041F\u0440\u043E\u0438\u0437\u0432\u043E\u0434\u0438\u0442\u0435\u043B\u044C\"}")
                .setTemplate("\u0412 shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}} \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440 {{paramName}} \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D \u043D\u0435 \u0432\u0435\u0440\u043D\u043E. \u041F\u043E\u043B\u0443\u0447\u0435\u043D\u043D\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435: {{receivedValue}}, \u0432\u0435\u0440\u043D\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435: {{correctValue}}."));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.params.not_filled_value")
                .setParams("{\"correctValue\":\"\u0441\u0435\u0440\u043E\u0431\u0443\u0440\u043E\u043C\u0430\u043B\u0438\u043D\u043E\u0432\u044B\u0439\",\"shopSKUs\":[\"2345\", \"2346\"],\"paramName\":\"\u0426\u0432\u0435\u0442 \u0442\u043E\u0432\u0430\u0440\u0430\"}")
                .setTemplate("\u0412 shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}} \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440 {{paramName}} \u043D\u0435 \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D, \u0430 \u0434\u043E\u043B\u0436\u0435\u043D \u0431\u044B\u0442\u044C \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D. \u0412\u0435\u0440\u043D\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435: {{correctValue}}."));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.pictures.bad_picture")
                .setParams("{\"hasWatermark\":true,\"isBlurred\":true,\"shopSKUs\":[\"3456\", \"3457\"],\"isCropped\":true,\"isNotWhiteBackground\":true,\"isNotRelevant\":true,\"url\":\"https://avatars.mds.yandex.net/get-mpic/1220464/img_id6559446408753393621.jpeg/9hq\"}")
                .setTemplate("\u041A\u0430\u0440\u0442\u0438\u043D\u043A\u0430 {{url}} \u0434\u043B\u044F shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}}. \u041E\u0431\u043D\u0430\u0440\u0443\u0436\u0435\u043D\u044B \u043F\u0440\u043E\u0431\u043B\u0435\u043C\u044B:\n\u041D\u0415 \u0441\u043E\u043E\u0442\u0432\u0435\u0442\u0441\u0432\u0443\u0435\u0442 \u0442\u043E\u0432\u0430\u0440\u0443 - {{isNotRelevant}},\n\u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u0440\u0430\u0437\u043C\u044B\u0442\u043E - {{isBlurred}},\n\u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u041D\u0415 \u043D\u0430 \u0431\u0435\u043B\u043E\u043C \u0444\u043E\u043D\u0435 - {{isNotWhiteBackground}},\n\u043D\u0430 \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0438 \u043F\u0440\u0438\u0441\u0443\u0442\u0432\u0443\u0435\u0442 \u0432\u043E\u0434\u044F\u043D\u043E\u0439 \u0437\u043D\u0430\u043A - {{hasWatermark}},\n\u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u043E\u0431\u0440\u0435\u0437\u0430\u043D\u043E - {{isCropped}}."));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.logs.wrong_category")
                .setParams("{\"shopSKUs\":[\"4567\", \"4568\"],\"partnerModelName\":\"\u0413\u0440\u0430\u0432\u0438\u0446\u0430\u043F\u043F\u0430\"}")
                .setTemplate("\u041C\u043E\u0434\u0435\u043B\u044C {{partnerModelName}} \u0432 \u043D\u0435\u0432\u0435\u0440\u043D\u043E\u0439 \u043A\u0430\u0442\u0435\u0433\u043E\u0440\u0438\u0438. \u0417\u0430\u0442\u0440\u0430\u0433\u0438\u0432\u0430\u0435\u0442 shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}}."));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.logs.market_model_exist")
                .setParams("{\"shopSKUs\":[\"5678\", \"5679\"],\"marketModelId\":12345678,\"partnerModelName\":\"\u0421\u0442\u0440\u0430\u043D\u043D\u0430\u044F \u0430\u0431\u0441\u0442\u0440\u0430\u043A\u0438\u0446\u0438\u044F\",\"marketModelName\":\"\u041A\u043E\u043D\u043A\u0440\u0435\u0442\u043D\u0430\u044F \u0430\u0431\u0441\u0442\u0440\u0430\u043A\u0438\u0446\u0438\u044F\"}")
                .setTemplate("\u041C\u043E\u0434\u0435\u043B\u044C {{partnerModelName}} \u0441 shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}} \u0443\u0436\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442 \u043D\u0430 \u043C\u0430\u0440\u043A\u0435\u0442\u0435 (model_id = {{marketModelId}}) \u0438 \u0438\u043C\u0435\u0435\u0442 \u0438\u043D\u043E\u0435 \u043D\u0430\u0437\u0432\u0430\u043D\u0438\u0435: \"{{marketModelName}}\""));


        PartnerContent.BucketProcessInfo.Builder bucketBuilder = PartnerContent.BucketProcessInfo.newBuilder();
        bucketBuilder.setCategoryId(12233);
        bucketBuilder.setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.INVALID);
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.absent_mandatory_parameter")
                .setParams("{\"shopSKU\":\"6789\",\"rowIndex\":10,\"paramName\":\"\u041D\u0430\u043B\u0438\u0447\u0438\u0435 \u0447\u0435\u0433\u043E-\u0442\u043E\"}")
                .setTemplate("\u041D\u0435 \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D \u043E\u0431\u044F\u0437\u0430\u0442\u0435\u043B\u044C\u043D\u044B\u0439 \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0435\u0440 {{paramName}} \u0434\u043B\u044F shop sku {{shopSKU}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.empty_model_name")
                .setParams("{\"shopSKU\":\"7890\",\"rowIndex\":11}")
                .setTemplate("\u041D\u0435 \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D\u043E \u043D\u0430\u0437\u0432\u0430\u043D\u0438\u0435 \u043C\u043E\u0434\u0435\u043B\u0438 \u0434\u043B\u044F shop sku {{shopSKU}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.empty_pictures")
                .setParams("{\"shopSKU\":\"7890\",\"rowIndex\":12}")
                .setTemplate("\u041D\u0435 \u0443\u043A\u0430\u0437\u0430\u043D\u044B \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F \u0434\u043B\u044F shop sku {{shopSKU}}"));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.invalid_picture")
                .setParams("{\"notAvailable\":true,\"invalidSize\":true,\"noWhiteBackground\":true,\"invalidFormat\":true,\"shopSKUs\":[\"3456\", \"3457\"],\"url\":\"https://avatars.mds.yandex.net/get-mpic/1220464/img_id6559446408753393621.jpeg/9hq\"}")
                .setTemplate("С изображением {{url}}, представленным в {{#shopSKUs}}{{.}}, {{/shopSKUs}}, обнаружены проблемы:\n" +
                        "изображение недоступно - {{notAvailable}},\n" +
                        "изображение имеет не верный размер - {{invalidSize}},\n" +
                        "изображение НЕ на белом фоне - {{noWhiteBackground}},\n" +
                        "изображение имеет не верный формат - {{invalidFormat}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.invalid_param_value")
                .setParams("{\"shopSKU\":\"8901\",\"receivedValue\":\"\u0447\u0435\u0440\u043D\u044F\u0432\u044B\u0439\",\"rowIndex\":14,\"paramName\":\"\u0446\u0432\u0435\u0442\"}")
                .setTemplate("\u0423\u043A\u0430\u0437\u0430\u043D\u043E \u043D\u0435 \u0434\u043E\u043F\u0443\u0441\u0442\u0438\u043C\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435 ({{receivedValue}}) \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440\u0430 {{paramName}} \u0432 shop sku {{shopSKU}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.invalid_numeric_param_value")
                .setParams("{\"minValue\":1.0,\"shopSKU\":\"9012\",\"maxValue\":100.0,\"receivedValue\":\"9999\",\"rowIndex\":15,\"paramName\":\"\u0434\u0438\u0430\u0433\u0430\u043D\u0430\u043B\u044C\"}")
                .setTemplate("\u0423\u043A\u0430\u0437\u0430\u043D\u043E \u043D\u0435 \u0434\u043E\u043F\u0443\u0441\u0442\u0438\u043C\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435 ({{receivedValue}}) \u0447\u0438\u0441\u043B\u043E\u0432\u043E\u0433\u043E \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440\u0430 {{paramName}} \u0432 shop sku {{shopSKU}}. \u0417\u043D\u0430\u0447\u0435\u043D\u0438\u0435 \u0434\u043E\u043B\u0436\u043D\u043E \u0431\u044B\u0442\u044C{{#minValue}} \u0431\u043E\u043B\u044C\u0448\u0435 \u0438\u043B\u0438 \u0440\u0430\u0432\u043D\u043E {{minValue}}{{/minValue}}{{#maxValue}} \u043C\u0435\u043D\u044C\u0448\u0435 \u0438\u043B\u0438 \u0440\u0430\u0432\u043D\u043E {{maxValue}}{{/maxValue}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.empty_sku_defining_data")
                .setParams("{\"paramNames\":[\"\u0446\u0432\u0435\u0442 \u0442\u043E\u0432\u0430\u0440\u0430\", \"Вес товара\"],\"modelName\":\"\u041C\u043E\u0434\u0435\u043B\u044C\u043A\u0430 A100 CB\",\"shopSKUs\":[\"0123\",\"01234\"],\"rowIndexes\":[16,17]}")
                .setTemplate("Найдены записи sku({{#shopSKUs}}{{.}}, {{/shopSKUs}}) без определяющих параметров " +
                        "для модели {{modelName}}. Должен быть заполнен хотя бы один параметр из " +
                        "{{#paramNames}}{{.}}, {{/paramNames}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.duplicate_sku_data")
                .setParams("{\"paramNames\":[\"\u0446\u0432\u0435\u0442 \u0442\u043E\u0432\u0430\u0440\u0430\", \"Вес товара\"],\"modelName\":\"\u041C\u043E\u0434\u0435\u043B\u044C\u043A\u0430 A100 CB\",\"shopSKUs\":[\"0123\",\"01234\"],\"rowIndexes\":[16,17]}")
                .setTemplate("\u041D\u0430\u0439\u0434\u0435\u043D\u044B \u0434\u0443\u0431\u043B\u0438\u0440\u0443\u044E\u0449\u0438\u0435\u0441\u044F \u0437\u0430\u043F\u0438\u0441\u0438 sku({{#shopSKUs}}{{.}}, {{/shopSKUs}}) \u0434\u043B\u044F \u043C\u043E\u0434\u0435\u043B\u0438 {{modelName}}. \u0421\u043E\u0432\u043F\u0430\u0434\u0430\u044E\u0442 \u043D\u0430\u0431\u043E\u0440\u044B \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0439 \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440\u043E\u0432 {{#paramNames}}{{.}}, {{/paramNames}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.inconsistent_data")
                .setParams("{\"paramNames\":[\"\u0414\u0438\u0430\u0433\u043E\u043D\u0430\u043B\u044C \u044D\u043A\u0440\u0430\u043D\u0430\", \"Разрешение экрана\"],\"modelName\":\"\u041C\u043E\u0434\u0435\u043B\u044C\u043A\u0430 A200 ZX\",\"shopSKUs\":[\"01235\",\"01236\"],\"rowIndexes\":[18,19]}")
                .setTemplate("\u041D\u0430\u0439\u0434\u0435\u043D\u044B \u043D\u0435 \u043A\u043E\u043D\u0441\u0438\u0441\u0442\u0435\u043D\u0442\u043D\u044B\u0435 \u0437\u0430\u043F\u0438\u0441\u0438 sku({{#shopSKUs}}{{.}}, {{/shopSKUs}}) \u0434\u043B\u044F \u043C\u043E\u0434\u0435\u043B\u0438 {{modelName}}. \u041D\u0435 \u043A\u043E\u043D\u0441\u0438\u0441\u0442\u0435\u043D\u0442\u044B \u043D\u0430\u0431\u043E\u0440\u044B \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0439 \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440\u043E\u0432 {{#paramNames}}{{.}}, {{/paramNames}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.incorrect_param_name")
                .setParams("{\"paramName\":\"\u0418\u043B\u044E\u0441\u0442\u0440\u0430\u0446\u0438\u044F\"}")
                .setTemplate("\u0423\u043A\u0430\u0437\u0430\u043D \u043D\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u044E\u0449\u0438\u0439 \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0435\u0440 {{paramName}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.empty_shop_sku")
                .setParams("{\"rowIndex\":123}")
                .setTemplate("\u041D\u0435 \u0443\u043A\u0430\u0437\u0430\u043D shop sku \u0432 \u0441\u0442\u0440\u043E\u043A\u0435 {{rowIndex}}"));
        bucketBuilder.setBucketProcessStatistics(PartnerContent.BucketProcessStatistics.newBuilder().setModelCreated(100).setModelAlreadyExists(1000).setSkuAlreadyExists(0).setModelAlreadyExists(100));
        builder.addBucketProcessInfo(bucketBuilder);
        return builder.build();
    }

    @SuppressWarnings({
            "checkstyle:magicNumber",
            "checkstyle:lineLength"
    })
    private static PartnerContent.FileInfoResponse mockErrorGetFileInfoResponse2() {
        PartnerContent.FileInfoResponse.Builder builder = PartnerContent.FileInfoResponse.newBuilder();
        builder.setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED);

        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.some_new_unknown_and_never_intended_error")
                .setParams("{\"shopSKU\":\"ABBABBA123\"}")
                .setTemplate("Эта ошибка для shop sku {{shopSKU}} только для тестов фронта и никогда, никогда не должна прийти от IR'а"));

        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.excel_invalid_file_format")
                .setParams("{\"message\":\"\u042D\u0442\u043E \u043D\u0435 Excel\"}")
                .setTemplate("\u041D\u0435\u043F\u0440\u0430\u0432\u0438\u043B\u044C\u043D\u044B\u0439 \u0444\u043E\u0440\u043C\u0430\u0442 \u0444\u0430\u0439\u043B\u0430: {{message}}"));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.params.invalid_param_value")
                .setParams("{\"correctValue\":\"Adidas\",\"shopSKUs\":[\"1234\", \"1235\"],\"receivedValue\":\"Abibas\",\"paramName\":\"\u041F\u0440\u043E\u0438\u0437\u0432\u043E\u0434\u0438\u0442\u0435\u043B\u044C\"}")
                .setTemplate("\u0412 shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}} \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440 {{paramName}} \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D \u043D\u0435 \u0432\u0435\u0440\u043D\u043E. \u041F\u043E\u043B\u0443\u0447\u0435\u043D\u043D\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435: {{receivedValue}}, \u0432\u0435\u0440\u043D\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435: {{correctValue}}."));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.params.not_filled_value")
                .setParams("{\"correctValue\":\"\u0441\u0435\u0440\u043E\u0431\u0443\u0440\u043E\u043C\u0430\u043B\u0438\u043D\u043E\u0432\u044B\u0439\",\"shopSKUs\":[\"2345\", \"2346\"],\"paramName\":\"\u0426\u0432\u0435\u0442 \u0442\u043E\u0432\u0430\u0440\u0430\"}")
                .setTemplate("\u0412 shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}} \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440 {{paramName}} \u043D\u0435 \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D, \u0430 \u0434\u043E\u043B\u0436\u0435\u043D \u0431\u044B\u0442\u044C \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D. \u0412\u0435\u0440\u043D\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435: {{correctValue}}."));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.pictures.bad_picture")
                .setParams("{\"hasWatermark\":true,\"isBlurred\":true,\"shopSKUs\":[\"3456\", \"3457\"],\"isCropped\":true,\"isNotWhiteBackground\":true,\"isNotRelevant\":true,\"url\":\"https://avatars.mds.yandex.net/get-mpic/1220464/img_id6559446408753393621.jpeg/9hq\"}")
                .setTemplate("\u041A\u0430\u0440\u0442\u0438\u043D\u043A\u0430 {{url}} \u0434\u043B\u044F shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}}. \u041E\u0431\u043D\u0430\u0440\u0443\u0436\u0435\u043D\u044B \u043F\u0440\u043E\u0431\u043B\u0435\u043C\u044B:\n\u041D\u0415 \u0441\u043E\u043E\u0442\u0432\u0435\u0442\u0441\u0432\u0443\u0435\u0442 \u0442\u043E\u0432\u0430\u0440\u0443 - {{isNotRelevant}},\n\u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u0440\u0430\u0437\u043C\u044B\u0442\u043E - {{isBlurred}},\n\u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u041D\u0415 \u043D\u0430 \u0431\u0435\u043B\u043E\u043C \u0444\u043E\u043D\u0435 - {{isNotWhiteBackground}},\n\u043D\u0430 \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0438 \u043F\u0440\u0438\u0441\u0443\u0442\u0432\u0443\u0435\u0442 \u0432\u043E\u0434\u044F\u043D\u043E\u0439 \u0437\u043D\u0430\u043A - {{hasWatermark}},\n\u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u043E\u0431\u0440\u0435\u0437\u0430\u043D\u043E - {{isCropped}}."));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.logs.wrong_category")
                .setParams("{\"shopSKUs\":[\"4567\", \"4568\"],\"partnerModelName\":\"\u0413\u0440\u0430\u0432\u0438\u0446\u0430\u043F\u043F\u0430\"}")
                .setTemplate("\u041C\u043E\u0434\u0435\u043B\u044C {{partnerModelName}} \u0432 \u043D\u0435\u0432\u0435\u0440\u043D\u043E\u0439 \u043A\u0430\u0442\u0435\u0433\u043E\u0440\u0438\u0438. \u0417\u0430\u0442\u0440\u0430\u0433\u0438\u0432\u0430\u0435\u0442 shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}}."));
        builder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.logs.market_model_exist")
                .setParams("{\"shopSKUs\":[\"5678\", \"5679\"],\"marketModelId\":12345678,\"partnerModelName\":\"\u0421\u0442\u0440\u0430\u043D\u043D\u0430\u044F \u0430\u0431\u0441\u0442\u0440\u0430\u043A\u0438\u0446\u0438\u044F\",\"marketModelName\":\"\u041A\u043E\u043D\u043A\u0440\u0435\u0442\u043D\u0430\u044F \u0430\u0431\u0441\u0442\u0440\u0430\u043A\u0438\u0446\u0438\u044F\"}")
                .setTemplate("\u041C\u043E\u0434\u0435\u043B\u044C {{partnerModelName}} \u0441 shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}} \u0443\u0436\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442 \u043D\u0430 \u043C\u0430\u0440\u043A\u0435\u0442\u0435 (model_id = {{marketModelId}}) \u0438 \u0438\u043C\u0435\u0435\u0442 \u0438\u043D\u043E\u0435 \u043D\u0430\u0437\u0432\u0430\u043D\u0438\u0435: \"{{marketModelName}}\""));

        PartnerContent.BucketProcessInfo.Builder bucketBuilder = PartnerContent.BucketProcessInfo.newBuilder();
        bucketBuilder.setCategoryId(12233);
        bucketBuilder.setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.INVALID);
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.absent_mandatory_parameter")
                .setParams("{\"shopSKU\":\"6789\",\"rowIndex\":10,\"paramName\":\"\u041D\u0430\u043B\u0438\u0447\u0438\u0435 \u0447\u0435\u0433\u043E-\u0442\u043E\"}")
                .setTemplate("\u041D\u0435 \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D \u043E\u0431\u044F\u0437\u0430\u0442\u0435\u043B\u044C\u043D\u044B\u0439 \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0435\u0440 {{paramName}} \u0434\u043B\u044F shop sku {{shopSKU}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.empty_model_name")
                .setParams("{\"shopSKU\":\"7890\",\"rowIndex\":11}")
                .setTemplate("\u041D\u0435 \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D\u043E \u043D\u0430\u0437\u0432\u0430\u043D\u0438\u0435 \u043C\u043E\u0434\u0435\u043B\u0438 \u0434\u043B\u044F shop sku {{shopSKU}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.empty_pictures")
                .setParams("{\"shopSKU\":\"7890\",\"rowIndex\":12}")
                .setTemplate("\u041D\u0435 \u0443\u043A\u0430\u0437\u0430\u043D\u044B \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F \u0434\u043B\u044F shop sku {{shopSKU}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.invalid_param_value")
                .setParams("{\"shopSKU\":\"8901\",\"receivedValue\":\"\u0447\u0435\u0440\u043D\u044F\u0432\u044B\u0439\",\"rowIndex\":14,\"paramName\":\"\u0446\u0432\u0435\u0442\"}")
                .setTemplate("\u0423\u043A\u0430\u0437\u0430\u043D\u043E \u043D\u0435 \u0434\u043E\u043F\u0443\u0441\u0442\u0438\u043C\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435 ({{receivedValue}}) \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440\u0430 {{paramName}} \u0432 shop sku {{shopSKU}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.invalid_numeric_param_value")
                .setParams("{\"minValue\":1.0,\"shopSKU\":\"9012\",\"maxValue\":100.0,\"receivedValue\":\"9999\",\"rowIndex\":15,\"paramName\":\"\u0434\u0438\u0430\u0433\u0430\u043D\u0430\u043B\u044C\"}")
                .setTemplate("\u0423\u043A\u0430\u0437\u0430\u043D\u043E \u043D\u0435 \u0434\u043E\u043F\u0443\u0441\u0442\u0438\u043C\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435 ({{receivedValue}}) \u0447\u0438\u0441\u043B\u043E\u0432\u043E\u0433\u043E \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440\u0430 {{paramName}} \u0432 shop sku {{shopSKU}}. \u0417\u043D\u0430\u0447\u0435\u043D\u0438\u0435 \u0434\u043E\u043B\u0436\u043D\u043E \u0431\u044B\u0442\u044C{{#minValue}} \u0431\u043E\u043B\u044C\u0448\u0435 \u0438\u043B\u0438 \u0440\u0430\u0432\u043D\u043E {{minValue}}{{/minValue}}{{#maxValue}} \u043C\u0435\u043D\u044C\u0448\u0435 \u0438\u043B\u0438 \u0440\u0430\u0432\u043D\u043E {{maxValue}}{{/maxValue}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.duplicate_sku_data")
                .setParams("{\"paramNames\":[\"\u0446\u0432\u0435\u0442 \u0442\u043E\u0432\u0430\u0440\u0430\", \"Вес товара\"],\"modelName\":\"\u041C\u043E\u0434\u0435\u043B\u044C\u043A\u0430 A100 CB\",\"shopSKUs\":[\"0123\",\"01234\"],\"rowIndexes\":[16,17]}")
                .setTemplate("\u041D\u0430\u0439\u0434\u0435\u043D\u044B \u0434\u0443\u0431\u043B\u0438\u0440\u0443\u044E\u0449\u0438\u0435\u0441\u044F \u0437\u0430\u043F\u0438\u0441\u0438 sku({{#shopSKUs}}{{.}}, {{/shopSKUs}}) \u0434\u043B\u044F \u043C\u043E\u0434\u0435\u043B\u0438 {{modelName}}. \u0421\u043E\u0432\u043F\u0430\u0434\u0430\u044E\u0442 \u043D\u0430\u0431\u043E\u0440\u044B \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0439 \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440\u043E\u0432 {{#paramNames}}{{.}}, {{/paramNames}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.inconsistent_data")
                .setParams("{\"paramNames\":[\"\u0414\u0438\u0430\u0433\u043E\u043D\u0430\u043B\u044C \u044D\u043A\u0440\u0430\u043D\u0430\", \"Разрешение экрана\"],\"modelName\":\"\u041C\u043E\u0434\u0435\u043B\u044C\u043A\u0430 A200 ZX\",\"shopSKUs\":[\"01235\",\"01236\"],\"rowIndexes\":[18,19]}")
                .setTemplate("\u041D\u0430\u0439\u0434\u0435\u043D\u044B \u043D\u0435 \u043A\u043E\u043D\u0441\u0438\u0441\u0442\u0435\u043D\u0442\u043D\u044B\u0435 \u0437\u0430\u043F\u0438\u0441\u0438 sku({{#shopSKUs}}{{.}}, {{/shopSKUs}}) \u0434\u043B\u044F \u043C\u043E\u0434\u0435\u043B\u0438 {{modelName}}. \u041D\u0435 \u043A\u043E\u043D\u0441\u0438\u0441\u0442\u0435\u043D\u0442\u044B \u043D\u0430\u0431\u043E\u0440\u044B \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0439 \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440\u043E\u0432 {{#paramNames}}{{.}}, {{/paramNames}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.incorrect_param_name")
                .setParams("{\"paramName\":\"\u0418\u043B\u044E\u0441\u0442\u0440\u0430\u0446\u0438\u044F\"}")
                .setTemplate("\u0423\u043A\u0430\u0437\u0430\u043D \u043D\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u044E\u0449\u0438\u0439 \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0435\u0440 {{paramName}}"));
        bucketBuilder.addValidationError(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.error.empty_shop_sku")
                .setParams("{\"rowIndex\":123}")
                .setTemplate("\u041D\u0435 \u0443\u043A\u0430\u0437\u0430\u043D shop sku \u0432 \u0441\u0442\u0440\u043E\u043A\u0435 {{rowIndex}}"));

        bucketBuilder.addDuplicateWarn(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.warn.model_already_exist")
                .setParams("{\"modelName\":\"Моделька A200 ZX\",\"shopSKUs\":[\"01235\",\"01236\"], \"marketModelId\": 12412541}")
                .setTemplate("Модель {{modelName}} с shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}} уже представлена на маркете с ID={{marketModelId}}")
                .build());

        bucketBuilder.setBucketProcessStatistics(PartnerContent.BucketProcessStatistics.newBuilder().setModelCreated(100).setModelAlreadyExists(1000).setSkuAlreadyExists(0).setModelAlreadyExists(100));
        builder.addBucketProcessInfo(bucketBuilder);
        return builder.build();
    }


    @SuppressWarnings({
            "checkstyle:magicNumber",
            "checkstyle:lineLength"
    })
    private static PartnerContent.FileInfoResponse mockSuccessGetFileInfoResponse1() {
        PartnerContent.FileInfoResponse.Builder builder = PartnerContent.FileInfoResponse.newBuilder();
        builder.setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED);

        PartnerContent.BucketProcessInfo.Builder bucketBuilder = PartnerContent.BucketProcessInfo.newBuilder();
        bucketBuilder.setCategoryId(12233);
        bucketBuilder.setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.FINISHED);

        bucketBuilder.addDuplicateWarn(ProtocolMessage.Message.newBuilder()
                .setCode("ir.partner_content.warn.model_already_exist")
                .setParams("{\"modelName\":\"Моделька A200 ZX\",\"shopSKUs\":[\"01235\",\"01236\"], \"marketModelId\": 12412541}")
                .setTemplate("Модель {{modelName}} с shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}} уже представлена на маркете с ID={{marketModelId}}")
                .build());

        bucketBuilder.setBucketProcessStatistics(PartnerContent.BucketProcessStatistics.newBuilder().setModelCreated(100).setModelAlreadyExists(1000).setSkuAlreadyExists(0).setModelAlreadyExists(100));
        builder.addBucketProcessInfo(bucketBuilder);
        return builder.build();
    }
}
