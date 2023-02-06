package ru.yandex.market.api.partner.controllers.price;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.price.model.OfferPriceDTO;
import ru.yandex.market.api.partner.controllers.price.model.OfferPriceListDTO;
import ru.yandex.market.api.partner.controllers.price.model.PriceDTO;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;
import ru.yandex.market.mbi.jaxb.jackson.XmlNamingStrategy;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.util.io.MbiFiles;

@ParametersAreNonnullByDefault
class OfferPriceControllerUnhappyPathTest extends AbstractOfferPriceControllerTest {

    @Test
    void nonExistentFeedTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479663\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error code=\"INVALID_FEED_ID\"" +
                                "               message=\"Invalid feedId: 479663: offerId = offer2\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void updatedAtShouldNotBeProvidedInRequestTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\" updated-at=\"2017-12-31T23:59:58+03:00\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error code=\"BAD_REQUEST\"" +
                                "               message=\"updatedAt is not allowed in request, but found for:" +
                                " feedId = 479633, offerId = offer2\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void anotherShopFeedTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"473953\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error code=\"INVALID_FEED_ID\"" +
                                "               message=\"Invalid feedId: 473953: offerId = offer2\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void duplicateOfferTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error code=\"DUPLICATE_OFFER\"" +
                                "               message=\"Duplicate offer in request: feedId = 479633, offerId = " +
                                "offer1\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void missingPriceOfferTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"Missing price element: feedId = 479633, offerId = offer1\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }


    @Test
    void bothDeleteAndPriceOfferTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer2\" delete=\"true\" >" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"Price element shouldn't be present on delete:" +
                                " feedId = 479633, offerId = offer2\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void negativePriceTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"-112.95\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"Non positive price value:" +
                                " feedId = 479633, offerId = offer1\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void zeroPrice() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        patchOfferPricesURI(CAMPAIGN_ID, "xml"),
                        HttpMethod.POST,
                        Format.XML,
                        //language=xml
                        "<offer-prices>\n" +
                                "    <offers>\n" +
                                "        <offer id=\"offer1\">\n" +
                                "            <feed id=\"479633\" />\n" +
                                "            <price value=\"0\" currency-id=\"RUR\" />\n" +
                                "        </offer>\n" +
                                "    </offers>\n" +
                                "</offer-prices>\n"
                )
        );


        MbiAsserts.assertXmlEquals(
                //language=xml
                "<response>" +
                        "    <status>ERROR</status>" +
                        "    <errors>" +
                        "        <error" +
                        "            code=\"BAD_REQUEST\"" +
                        "            message=\"Non positive price value:" +
                        " feedId = 479633, offerId = offer1\"/>" +
                        "    </errors>" +
                        "</response>",
                exception.getResponseBodyAsString()
        );
    }

    @Test
    void longOfferId() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        patchOfferPricesURI(CAMPAIGN_ID, "xml"),
                        HttpMethod.POST,
                        Format.XML,
                        //language=xml
                        "<offer-prices>\n" +
                                "    <offers>\n" +
                                "        <offer id=\"offerOfferOfferOfferOfferOfferOfferOfferOfferOffer" +
                                "OfferOfferOfferOfferOfferOfferOfferOfferOfferOffer\">\n" +
                                "            <feed id=\"479633\" />\n" +
                                "            <price value=\"10\" currency-id=\"RUR\" />\n" +
                                "        </offer>\n" +
                                "    </offers>\n" +
                                "</offer-prices>\n"
                )
        );


        MbiAsserts.assertXmlEquals(
                //language=xml
                "<response>" +
                        "    <status>ERROR</status>" +
                        "    <errors>" +
                        "        <error" +
                        "            code=\"INVALID_OFFER_ID\"" +
                        "            message=\"Offer id is too long: id = " +
                        "offerOfferOfferOfferOfferOfferOfferOfferOfferOfferOfferOfferOfferOfferOfferOfferOfferOfferOfferOffer" +
                        " length = 100 maxLength = 80\"/>" +
                        "    </errors>" +
                        "</response>",
                exception.getResponseBodyAsString()
        );
    }

    @Test
    void tooPrecisePriceTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.95013271\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"Too many digits after decimal point in price value:" +
                                " feedId = 479633, offerId = offer1\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void priceTooBigXml() throws Exception {
        String bodyString1 = "" +
                "<offer-prices>" +
                "    <offers>" +
                "        <offer id=\"offer1\">" +
                "            <feed id=\"479633\" />" +
                "            <price value=\"922337203685.4775808\" currency-id=\"RUR\" />" +
                "        </offer>" +
                "    </offers>" +
                "</offer-prices>";
        String bodyString2 = "" +
                "<offer-prices>" +
                "    <offers>" +
                "        <offer id=\"offer1\">" +
                "            <feed id=\"479633\" />" +
                "            <price value=\"922337203686.4775807\" currency-id=\"RUR\" />" +
                "        </offer>" +
                "    </offers>" +
                "</offer-prices>";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            for (String bodyString : new String[]{bodyString1, bodyString2}) {
                StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
                HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                    System.out.println(bodyString);
                    MbiAsserts.assertXmlEquals("" +
                                    "<response>" +
                                    "    <status>ERROR</status>" +
                                    "    <errors>" +
                                    "        <error" +
                                    "            code=\"BAD_REQUEST\"" +
                                    "            message=\"Price value too big: feedId = 479633, offerId = offer1\"/>" +
                                    "    </errors>" +
                                    "</response>",
                            bodyString
                    );
                    Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
                }
            }
        }
    }

    @Test
    void missingPriceValueTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.95\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"Missing price value: feedId = 479633, offerId = offer2\" />" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void blankPriceValueTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.95\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"Missing price value: feedId = 479633, offerId = offer2\" />" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void nonNumericPriceValueTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.95\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"superPrice\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"bad request\" />" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void missingCurrencyIdTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.95\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"115.30\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"currencyId is missing: feedId = 479633, offerId = offer2\" />" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void blankCurrencyIdTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.95\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"115.30\" currency-id=\"\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"currencyId is missing: feedId = 479633, offerId = offer2\" />" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void nonRurCurrencyIdTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.95\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"115.30\" currency-id=\"USD\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"Only 'RUR' currencyId is currently supported:" +
                                " feedId = 479633, offerId = offer2\" />" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void negativeDiscountTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.95\" discount-base=\"99.99\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"discountBase is less then value:" +
                                " feedId = 479633, offerId = offer1\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void zeroDiscountAndPriceTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"0\" discount-base=\"0\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals(""
                                + "<response>"
                                + "     <status>ERROR</status>"
                                + "     <errors>"
                                + "         <error "
                                + "             code=\"BAD_REQUEST\" "
                                + "             message=\"Non positive price value: feedId = 479633, offerId = " +
                                "offer1\"/>"
                                + "         <error "
                                + "             code=\"BAD_REQUEST\" "
                                + "             message=\"Non positive price discountBase: feedId = 479633, offerId =" +
                                " offer1\"/>"
                                + "     </errors>"
                                + "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void requestTooBig() throws IOException {
        ObjectMapper xmlMapper = new ApiObjectMapperFactory().createXmlMapper(new XmlNamingStrategy());
        OfferPriceListDTO list = new OfferPriceListDTO();
        for (int i = 0; i < OfferPriceListDTO.MAX_REQUEST_SIZE + 1; i++) {
            OfferPriceDTO offerPrice = new OfferPriceDTO();
            offerPrice.setFeedId(479633L);
            offerPrice.setId("offer" + i);
            PriceDTO price = new PriceDTO();
            price.setValue(BigDecimal.valueOf(100000099, 2));
            price.setDiscountBase(BigDecimal.valueOf(200000099, 2));
            price.setCurrencyId("RUR");
            offerPrice.setPrice(price);
            list.getOfferPrices().add(offerPrice);
        }
        StringWriter stream = new StringWriter();
        xmlMapper.writeValue(stream, list);
        String bodyString = stream.toString();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error code=\"REQUEST_LIMIT_EXCEEDED\"" +
                                "               message=\"Request too big:" +
                                " maximum of 2000 offers are allowed in single request:" +
                                " got 2001\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void missingFeedIdTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"INVALID_FEED_ID\"" +
                                "            message=\"feedId is missing: offerId = offer2\"/>" +
                                "        </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void missingOfferIdTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer>" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"INVALID_OFFER_ID\"" +
                                "            message=\"Missing offer id: feedId = 479633\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void blankOfferIdTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"INVALID_OFFER_ID\"" +
                                "            message=\"Missing offer id: feedId = 479633\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void vatCurrentlyUnsupportedOnWhiteMarkerTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.95\" currency-id=\"RUR\" vat=\"2\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"Vats currently unsupported:" +
                                " feedId = 479633, offerId = offer1\" />" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void unrecognizedVatOnBlueMarkerTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer market-sku=\"1\">" +
                    "            <feed id=\"661661\" />" +
                    "            <price value=\"112.95\" currency-id=\"RUR\" vat=\"15\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error" +
                                "            code=\"BAD_REQUEST\"" +
                                "            message=\"Unrecognized vat: 15:" +
                                " feedId = 661661, marketSku = 1\" />" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void marketSkuNotAllowedOnWhiteMarkerTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer market-sku=\"1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.95\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>ERROR</status>" +
                                "    <errors>" +
                                "        <error code=\"INVALID_OFFER_ID\"" +
                                "               message=\"Missing offer id: feedId = 479633\"/>" +
                                "        <error code=\"INVALID_OFFER_ID\"" +
                                "               message=\"MarketSku found for offer, but only id expected: " +
                                "feedId = 479633\"/>" +
                                "    </errors>" +
                                "</response>",
                        bodyString
                );
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void illegalDiscountValues() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"1.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"4.99\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"95.01\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer4\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"99.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer5\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"99010.00\" discount-base=\"100000.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals(
                        "<response>"
                                + "    <status>ERROR</status>"
                                + "    <errors>"
                                + "        <error code=\"BAD_REQUEST\" message=\"illegal discount value: 99.00 RUR, " +
                                "99.00%: feedId = 479633, offerId = offer1\"/>"
                                + "        <error code=\"BAD_REQUEST\" message=\"illegal discount value: 95.01 RUR, " +
                                "95.01%: feedId = 479633, offerId = offer2\"/>"
                                + "        <error code=\"BAD_REQUEST\" message=\"illegal discount value: 4.99 RUR, 4" +
                                ".99%: feedId = 479633, offerId = offer3\"/>"
                                + "        <error code=\"BAD_REQUEST\" message=\"illegal discount value: 1.00 RUR, 1" +
                                ".00%: feedId = 479633, offerId = offer4\"/>"
                                + "        <error code=\"BAD_REQUEST\" message=\"illegal discount value: 990.00 RUR, " +
                                "0.99%: feedId = 479633, offerId = offer5\"/>"
                                + "    </errors>"
                                + "</response>",
                        bodyString);
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }
        }
    }
}
