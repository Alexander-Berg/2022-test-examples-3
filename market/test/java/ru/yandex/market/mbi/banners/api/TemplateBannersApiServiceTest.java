package ru.yandex.market.mbi.banners.api;

import java.util.Collections;
import java.util.function.Function;

import okhttp3.RequestBody;
import okio.Buffer;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import retrofit2.Response;
import retrofit2.mock.Calls;

import ru.yandex.market.common.bunker.BunkerService;
import ru.yandex.market.common.bunker.BunkerWritingApi;
import ru.yandex.market.common.bunker.loader.BunkerLoader;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.banners.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * Функциональные тесты на {@link TemplateBannersApiService}
 */
class TemplateBannersApiServiceTest extends FunctionalTest {

    public static final String NODE_NAME = "/market-mbi/template-banners/sample-banner-dropoff-off";

    @Autowired
    private BunkerLoader bunkerLoader;

    @Autowired
    private BunkerWritingApi bunkerWritingApi;

    @Test
    @DisplayName("Проверка успешной генерации баннера")
    void testSuccessGenerateBanner() throws Exception {
        generateBanner(
                "TemplateBannersService.successGenerateBanner.request.json",
                "TemplateBannersService.successGenerateBanner.bunkerNode.before.json",
                "TemplateBannersService.successGenerateBanner.bunkerNode.after.json"
        );
    }

    @Test
    @DisplayName("Проверка перезаписи уже существующего баннера")
    void testOverwriteBanner() throws Exception {
        generateBanner(
                "TemplateBannersService.overwriteBanner.request.json",
                "TemplateBannersService.overwriteBanner.bunkerNode.before.json",
                "TemplateBannersService.overwriteBanner.bunkerNode.after.json"
        );
    }

    @Test
    @DisplayName("Проверка успешного удаления баннера")
    void testRemoveBanner() throws Exception {
        deleteBanner(
            "TemplateBannersService.deleteBanner.request.json",
            "TemplateBannersService.successRemoveBanner.bunkerNode.before.json",
            "TemplateBannersService.successRemoveBanner.bunkerNode.after.json"
        );
    }

    @Test
    @DisplayName("Проверка успешного удаления партнера из баннера")
    void testRemovePartnerFromBanner() throws Exception {
        deletePartnerFromBanner(
                "TemplateBannersService.deletePartnerFromBanner.request.json",
                "TemplateBannersService.successRemovePartnerFromBanner.bunkerNode.before.json",
                "TemplateBannersService.successRemovePartnerFromBanner.bunkerNode.after.json"
        );
    }

    @Test
    @DisplayName("Проверка, что нельзя перезаписать шаблон баннера")
    void testFailedAttemptToOverwriteTemplate() {
        assertThatThrownBy(() ->
                generateBanner(
                        "TemplateBannersService.failedAttemptToOverwriteTemplate.request.json",
                        "TemplateBannersService.failedAttemptToOverwriteTemplate.bunkerNode.before.json",
                        null
                ))
                .isInstanceOf(HttpClientErrorException.BadRequest.class)
                .satisfies(e -> jsonAssertFile(
                        "TemplateBannersService.failedAttemptToOverwriteTemplate.response.json",
                        ((HttpClientErrorException.BadRequest) e).getResponseBodyAsString())
                );
    }

    @Test
    @DisplayName("Проверка, что нельзя создать баннер без указания templateId")
    void testGenerateBannerWithEmptyTemplateId() {
        assertThatThrownBy(() ->
                generateBanner(
                        "TemplateBannersService.generateBannerWithEmptyTemplateId.request.json",
                        null,
                        null
                ))
                .isInstanceOf(HttpClientErrorException.BadRequest.class)
                .satisfies(e -> jsonAssert("{\n" +
                                "    \"message\": \"templateId must not be null or empty\",\n" +
                                "    \"code\": \"ILLEGAL_ARGUMENTS_ERROR\"\n" +
                                "}\n",
                        ((HttpClientErrorException.BadRequest) e).getResponseBodyAsString())
                );
    }

    @Test
    @DisplayName("Проверка, что нельзя создать баннер без указания bannerId")
    void testGenerateBannerWithEmptyBannerId() {
        assertThatThrownBy(() ->
                generateBanner(
                        "TemplateBannersService.generateBannerWithEmptyBannerId.request.json",
                        null,
                        null
                ))
                .isInstanceOf(HttpClientErrorException.BadRequest.class)
                .satisfies(e -> jsonAssert("{\n" +
                                "    \"message\": \"bannerId must not be null or empty\",\n" +
                                "    \"code\": \"ILLEGAL_ARGUMENTS_ERROR\"\n" +
                                "}\n",
                        ((HttpClientErrorException.BadRequest) e).getResponseBodyAsString())
                );
    }

    private void doRequest(
        String requestFile,
        String bunkerNodeBeforeFile,
        String bunkerNodeAfterFile,
        Function<String, HttpStatus> post
    ) throws Exception {
        if (bunkerNodeBeforeFile != null) {
            when(bunkerLoader.getNodeStream(NODE_NAME, BunkerService.Version.LATEST))
                    .thenReturn(getClass().getResourceAsStream(bunkerNodeBeforeFile));
        }

        String request = getString(getClass(), requestFile);

        ArgumentCaptor<RequestBody> nodeCaptor = ArgumentCaptor.forClass(RequestBody.class);
        when(bunkerWritingApi.store(any(),
                any(),
                nodeCaptor.capture(),
                any())
        ).thenReturn(Calls.response(Response.success(null)));

        HttpStatus responseStatus = post.apply(request);
        assertThat(responseStatus).isEqualTo(HttpStatus.OK);

        Buffer buffer = new Buffer();
        nodeCaptor.getValue().writeTo(buffer);
        String actual = buffer.readUtf8();
        jsonAssertFile(bunkerNodeAfterFile, actual);
    }

    private void generateBanner(String requestFile, String bunkerNodeBeforeFile, String bunkerNodeAfterFile) throws Exception {
        doRequest(requestFile, bunkerNodeBeforeFile, bunkerNodeAfterFile, this::postGenerateBanner);
    }

    private void deleteBanner(String requestFile, String bunkerNodeBeforeFile, String bunkerNodeAfterFile) throws Exception {
        doRequest(requestFile, bunkerNodeBeforeFile, bunkerNodeAfterFile, this::postDeleteBanners);
    }

    private void deletePartnerFromBanner(String requestFile, String bunkerNodeBeforeFile, String bunkerNodeAfterFile) throws Exception {
        doRequest(requestFile, bunkerNodeBeforeFile, bunkerNodeAfterFile, this::postDeletePartnerFromBanners);
    }

    private void jsonAssert(String expected, String actual) {
        try {
            JSONAssert.assertEquals(
                    expected,
                    actual,
                    false
            );
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
    }

    private void jsonAssertFile(String expectedFile, String actual) {
        jsonAssert(getString(getClass(), expectedFile), actual);
    }

    private HttpStatus postGenerateBanner(String request) {
        return doPost("/generateBanner", request);
    }

    private HttpStatus postDeleteBanners(String request) {
        return doPost("/deleteBanners", request);
    }

    private HttpStatus postDeletePartnerFromBanners(String request) {
        return doPost("/deleteBannerPartners", request);
    }

    private HttpStatus doPost(String endpoint, String request) {
        ResponseEntity<String> response = FunctionalTestHelper.post(baseUrl + endpoint,
            new HttpEntity<>(request, jsonHeaders()));
        return response.getStatusCode();
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return httpHeaders;
    }
}
