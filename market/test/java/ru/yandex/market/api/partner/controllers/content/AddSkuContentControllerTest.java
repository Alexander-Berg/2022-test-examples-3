package ru.yandex.market.api.partner.controllers.content;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.ir.http.MboRobot;
import ru.yandex.market.ir.http.PartnerContentApi;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mbi.util.io.MbiFiles;

/**
 * Тесты для {@link ContentController}.
 */
@ParametersAreNonnullByDefault
class AddSkuContentControllerTest extends FunctionalTest {
    private static final long CATEGORY_ID = 34579;
    private static final long CAMPAIGN_ID = 1000571241;
    private static final int IR_SOURCE_ID = 110774;
    private static final long MODEL_ID = 2893472;

    @Autowired
    private PartnerContentService marketProtoPartnerContentService;

    @Nonnull
    private static PartnerContentApi.AddSkuTicketRequest testAddSkuExpectedToIrRequest() {
        return PartnerContentApi.AddSkuTicketRequest.newBuilder()
                .setSourceId(IR_SOURCE_ID)
                .addTicket(PartnerContentApi.AddSkuTicket.newBuilder()
                        .setCategoryId(CATEGORY_ID)
                        .setModelId(MODEL_ID)
                        .addSku(PartnerContentApi.Sku.newBuilder()
                                .setShopSku("iphone-x-red")
                                .addImageUrl("https://i.imgur.com/zO9NFOT.jpg")
                                .addImageUrl("https://i.imgur.com/W5BHeOB.jpg")
                                .addParameter(PartnerContentApi.ParameterValue.newBuilder()
                                        .setParamId(2384623)
                                        .setType(PartnerContentApi.ParameterType.BOOL)
                                        .setBoolValue(true)
                                        .build())
                                .addParameter(PartnerContentApi.ParameterValue.newBuilder()
                                        .setParamId(2304750)
                                        .setType(PartnerContentApi.ParameterType.ENUM)
                                        .setEnumOptionId(239529)
                                        .build())
                                .build())
                        .addSku(PartnerContentApi.Sku.newBuilder()
                                .setShopSku("iphone-x-blue")
                                .addImageUrl("https://i.imgur.com/le3qfFy.jpg")
                                .addImageUrl("https://i.imgur.com/Q81Gyv5.jpg")
                                .addParameter(PartnerContentApi.ParameterValue.newBuilder()
                                        .setParamId(2384623)
                                        .setType(PartnerContentApi.ParameterType.BOOL)
                                        .setBoolValue(false)
                                        .build())
                                .addParameter(PartnerContentApi.ParameterValue.newBuilder()
                                        .setParamId(2304750)
                                        .setType(PartnerContentApi.ParameterType.ENUM)
                                        .setEnumOptionId(283756)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Test
    void testAddSkuJson() throws IOException {
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(IR_SOURCE_ID)
                        .build());
        Mockito.when(marketProtoPartnerContentService.createAddSkuTicket(Mockito.any()))
                .thenReturn(PartnerContentApi.CreateTicketResponse.newBuilder()
                        .addResult(PartnerContentApi.CreateTicketResult.newBuilder()
                                .setStatus(PartnerContentApi.CreateTicketResult.Status.TICKET_CREATED)
                                .setTicketId(23598)
                                .build())
                        .build());
        String url = String.format("%s/campaigns/%d/categories/%d/models/%d/skus/requests",
                urlBasePrefix, CAMPAIGN_ID, CATEGORY_ID, MODEL_ID);
        String toPapiRequest = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testAddSku.json"),
                StandardCharsets.UTF_8
        );
        ResponseEntity<String> responseEntity =
                FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.JSON, toPapiRequest);
        // language=json
        String expectedResponse = ""
                + "{\n"
                + "    \"status\":\"OK\",\n"
                + "    \"result\":{\n"
                + "        \"id\":23598,\n"
                + "        \"status\":\"PROCESSING\",\n"
                + "        \"type\":\"ADD_SKU\"\n"
                + "    }\n"
                + "}";
        MatcherAssert.assertThat(
                responseEntity,
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonEquals(expectedResponse))
        );
        ArgumentCaptor<PartnerContentApi.AddSkuTicketRequest> captor =
                ArgumentCaptor.forClass(PartnerContentApi.AddSkuTicketRequest.class);
        Mockito.verify(marketProtoPartnerContentService).createAddSkuTicket(captor.capture());
        MatcherAssert.assertThat(captor.getValue(), Matchers.is(testAddSkuExpectedToIrRequest()));
    }

    @Test
    void testAddSkuXml() throws IOException {
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(IR_SOURCE_ID)
                        .build());
        Mockito.when(marketProtoPartnerContentService.createAddSkuTicket(Mockito.any()))
                .thenReturn(PartnerContentApi.CreateTicketResponse.newBuilder()
                        .addResult(PartnerContentApi.CreateTicketResult.newBuilder()
                                .setStatus(PartnerContentApi.CreateTicketResult.Status.TICKET_CREATED)
                                .setTicketId(23598)
                                .build())
                        .build());
        String url = String.format("%s/campaigns/%d/categories/%d/models/%d/skus/requests",
                urlBasePrefix, CAMPAIGN_ID, CATEGORY_ID, MODEL_ID);
        String toPapiRequest = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testAddSku.xml"),
                StandardCharsets.UTF_8
        );
        ResponseEntity<String> responseEntity =
                FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.XML, toPapiRequest);
        // language=xml
        String expectedResponse = ""
                + "<response>"
                + "    <status>OK</status>"
                + "    <result id=\"23598\" status=\"PROCESSING\" type=\"ADD_SKU\" />"
                + "</response>";
        MatcherAssert.assertThat(
                responseEntity,
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.xmlEquals(expectedResponse))
        );
        ArgumentCaptor<PartnerContentApi.AddSkuTicketRequest> captor =
                ArgumentCaptor.forClass(PartnerContentApi.AddSkuTicketRequest.class);
        Mockito.verify(marketProtoPartnerContentService).createAddSkuTicket(captor.capture());
        MatcherAssert.assertThat(captor.getValue(), Matchers.is(testAddSkuExpectedToIrRequest()));
    }

    @Test
    void testAddSkuValidationsJson() throws IOException {
        String url = String.format("%s/campaigns/%d/categories/%d/models/%d/skus/requests",
                urlBasePrefix, CAMPAIGN_ID, CATEGORY_ID, MODEL_ID);
        String toPapiRequest = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testAddSkuValidations.request.json"),
                StandardCharsets.UTF_8
        );
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.JSON, toPapiRequest)
        );

        String expectedFromPapiResponse = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testAddSkuValidations.response.json"),
                StandardCharsets.UTF_8
        );

        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(MbiMatchers.jsonEquals(expectedFromPapiResponse))
                )
        );
        Mockito.verifyZeroInteractions(marketProtoPartnerContentService);
    }
}
