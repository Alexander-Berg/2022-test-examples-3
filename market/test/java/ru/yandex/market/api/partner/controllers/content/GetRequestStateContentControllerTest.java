package ru.yandex.market.api.partner.controllers.content;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.ir.http.MboRobot;
import ru.yandex.market.ir.http.PartnerContentApi;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.ir.http.ProtocolMessage;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mbi.util.io.MbiFiles;

/**
 * Тесты для {@link ContentController}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = {"GetRequestStateContentControllerTest.tanker.csv"})
class GetRequestStateContentControllerTest extends FunctionalTest {
    private static final long CAMPAIGN_ID = 1000571241;
    private static final int IR_SOURCE_ID = 110774;
    private static final long IR_TICKET_ID = 395678934;
    private static final PartnerContentApi.GetTicketStateResponse GENERIC_IR_GET_TICKET_STATE_RESPONSE =
            genericIrGetTicketStateResponse();

    @Autowired
    private PartnerContentService marketProtoPartnerContentService;

    @Nonnull
    private static PartnerContentApi.GetTicketStateResponse genericIrGetTicketStateResponse() {
        return PartnerContentApi.GetTicketStateResponse.newBuilder()
                .addTicketState(PartnerContentApi.TicketState.newBuilder()
                        .setSourceId(IR_SOURCE_ID)
                        .setTicketId(IR_TICKET_ID)
                        .addShopSku("SDF325742")
                        .addShopSku("EWT73529573")
                        .addShopSku("DHSF9236429")
                        .addShopSku("SDFS4309572")
                        .addShopSku("DSGSD549079250")
                        .addTicketValidationError(ProtocolMessage.Message.newBuilder()
                                .setCode("ir.partner_content.error.invalid_param_value")
                                .setTemplate(""
                                        + "Указано не допустимое значение ({{receivedValue}})"
                                        + " параметра {{paramName}} в shop sku {{shopSKU}}")
                                .setParams(/*language=JSON*/ ""
                                        + "{"
                                        + "    \"receivedValue\": \"213\","
                                        + "    \"paramName\": \"Высота\","
                                        + "    \"paramId\": 234141,"
                                        + "    \"shopSKU\": \"SDF325742\""
                                        + "}")
                                .setDetails(ProtocolMessage.Message.Details.newBuilder()
                                        .addShopSku("SDF325742")
                                        .addParamId(234141)
                                        .build())
                                .build())
                        .addTicketValidationError(ProtocolMessage.Message.newBuilder()
                                .setCode("ir.partner_content.error.pictures.bad_picture")
                                .setTemplate(""
                                        + "Картинка {{&url}} для shop_sku {{#shopSKUs}}{{.}}, {{/shopSKUs}}."
                                        + " Обнаружены проблемы:\n"
                                        + "НЕ соответсвует товару - {{isNotRelevant}},\n"
                                        + "изображение размыто - {{isBlurred}},\n"
                                        + "изображение НЕ на белом фоне - {{isNotWhiteBackground}},\n"
                                        + "на изображении присутвует водяной знак - {{hasWatermark}},\n"
                                        + "изображение обрезано - {{isCropped}}.")
                                .setParams(/*language=JSON*/ ""
                                        + "{"
                                        + "    \"url\": \"https://i.imgur.com/Q81Gyv5.jpg\","
                                        + "    \"shopSKUs\": [\"EWT73529573\", \"DHSF9236429\"],"
                                        + "    \"isNotRelevant\": true,"
                                        + "    \"isBlurred\": false,"
                                        + "    \"isNotWhiteBackground\": true,"
                                        + "    \"hasWatermark\": true,"
                                        + "    \"isCropped\": true"
                                        + "}")
                                .setDetails(ProtocolMessage.Message.Details.newBuilder()
                                        .addShopSku("EWT73529573")
                                        .addShopSku("DHSF9236429")
                                        .setImageUrl("https://i.imgur.com/Q81Gyv5.jpg")
                                        .build())
                                .build())
                        .addTicketValidationError(ProtocolMessage.Message.newBuilder()
                                .setCode("ir.partner_content.error.inconsistent_data")
                                .setTemplate(""
                                        + "Найдены не консистентные записи sku({{#shopSKUs}}{{.}}, {{/shopSKUs}})"
                                        + " для модели {{modelName}}."
                                        + " Не консистенты наборы значений параметров"
                                        + " {{#paramNames}}{{.}}, {{/paramNames}}")
                                .setParams(/*language=JSON*/ ""
                                        + "{"
                                        + "    \"shopSKUs\": [\"SDFS4309572\", \"DSGSD549079250\"],"
                                        + "    \"modelName\": \"Roventa XL234\","
                                        + "    \"paramNames\": [\"Цвет\", \"Мощность\"],"
                                        + "    \"paramIds\": [23042436, 34934055]"
                                        + "}")
                                .setDetails(ProtocolMessage.Message.Details.newBuilder()
                                        .addShopSku("SDFS4309572")
                                        .addShopSku("DSGSD549079250")
                                        .addParamId(23042436)
                                        .addParamId(34934055)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Test
    void testGetRequestStateJson() throws IOException {
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(IR_SOURCE_ID)
                        .build());
        Mockito.when(marketProtoPartnerContentService.getTicketState(Mockito.any()))
                .thenReturn(GENERIC_IR_GET_TICKET_STATE_RESPONSE);
        String url = String.format("%s/campaigns/%d/models/requests/%d", urlBasePrefix, CAMPAIGN_ID, IR_TICKET_ID);
        String expectedResponse = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testGetRequestState.json"),
                StandardCharsets.UTF_8
        );
        MatcherAssert.assertThat(
                FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.JSON),
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonEquals(expectedResponse))
        );
        Mockito.verify(marketProtoPartnerContentService).getTicketState(ArgumentMatchers.argThat(
                request -> Collections.singletonList(IR_TICKET_ID).equals(request.getTicketIdList())
        ));
    }

    @Test
    void testGetRequestStateXml() throws IOException {
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(IR_SOURCE_ID)
                        .build());
        Mockito.when(marketProtoPartnerContentService.getTicketState(Mockito.any()))
                .thenReturn(GENERIC_IR_GET_TICKET_STATE_RESPONSE);
        String url = String.format("%s/campaigns/%d/models/requests/%d", urlBasePrefix, CAMPAIGN_ID, IR_TICKET_ID);
        String expectedResponse = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testGetRequestState.xml"),
                StandardCharsets.UTF_8
        );
        MatcherAssert.assertThat(
                FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.XML),
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.xmlEquals(expectedResponse))
        );
        Mockito.verify(marketProtoPartnerContentService).getTicketState(ArgumentMatchers.argThat(
                request -> Collections.singletonList(IR_TICKET_ID).equals(request.getTicketIdList())
        ));

    }

    @Test
    void testGetRequestStateOfOtherSourceId() throws IOException {
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(110775)
                        .build());
        Mockito.when(marketProtoPartnerContentService.getTicketState(Mockito.any()))
                .thenReturn(GENERIC_IR_GET_TICKET_STATE_RESPONSE);
        String url = String.format("%s/campaigns/%d/models/requests/%d", urlBasePrefix, CAMPAIGN_ID, IR_TICKET_ID);
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.XML)
        );

        //language=xml
        String expected = ""
                + "<response>"
                + "    <status>ERROR</status>"
                + "    <errors>"
                + "        <error code=\"NOT_FOUND\" message=\"content request not found: 395678934\"/>"
                + "    </errors>"
                + "</response>";
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND),
                        HttpClientErrorMatcher.bodyMatches(MbiMatchers.xmlEquals(expected))
                )
        );
    }
}
