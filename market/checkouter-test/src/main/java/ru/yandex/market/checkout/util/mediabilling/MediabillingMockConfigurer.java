package ru.yandex.market.checkout.util.mediabilling;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

/**
 * @author ugoryntsev
 */
@TestComponent
public class MediabillingMockConfigurer {

    private static final Logger log = LoggerFactory.getLogger(MediabillingMockConfigurer.class);

    private static final String STATION_PRODUCTS_URI = "/market/products/station-product";
    private static final String REFUND_URI = "/market/stations/refund";
    private static final String INVOICE_CREATION_URI = "/market/stations/checkout";
    private static final String ORDER_STATUS_URI = "/market/stations/info";

    private static final String MB_ERROR_HEADER = "X-MediaBilling-Error-Code";

    private static final String STATION_PRODUCT_NOT_AVAILABLE = STATION_PRODUCTS_URI + "_not_available";

    @Autowired
    private WireMockServer mediabillingMock;

    private static String getStringBodyFromFile(String fileName) throws IOException {
        return getStringBodyFromFile(fileName, Collections.emptyMap());
    }

    private static String getStringBodyFromFile(String fileName, Map<String, Object> vars) throws
            IOException {
        final String[] template = {IOUtils.toString(
                MediabillingMockConfigurer.class.getResourceAsStream(fileName),
                Charset.defaultCharset())};
        vars.forEach((key, value) -> template[0] = template[0].replace(key, Objects.toString(value)));
        return template[0];
    }

    public void mockWholeMediabilling() {
        try {
            mockStationProducts();
            mockNotAvailableStationProduct();
            mockNoProductForDevice();
            mockRefund();
            mockInvoiceCreation();
            mockOrderStatus();
            mockNotAvailableByScore();
        } catch (Exception e) {
            log.error("Error during Mediabilling mock setup:", e);
        }
    }

    public void mockStationProducts() throws IOException {
        MappingBuilder builder = get(urlPathMatching(STATION_PRODUCTS_URI))
                .withName(STATION_PRODUCTS_URI)
                .willReturn(ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(getStringBodyFromFile("stationProductsResponse.json"))
                );
        mediabillingMock.stubFor(builder);
    }

    public void mockNotAvailableStationProduct() {
        MappingBuilder builder = get(urlPathMatching(STATION_PRODUCTS_URI))
                .withName(STATION_PRODUCT_NOT_AVAILABLE)
                .withQueryParam("__uid", new EqualToPattern("42"))
                .willReturn(badRequest()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withHeader(MB_ERROR_HEADER, "no_available_product")
                );
        mediabillingMock.stubFor(builder);
    }

    public void mockNotAvailableByScore() {
        MappingBuilder builder = get(urlPathMatching(STATION_PRODUCTS_URI))
                .withName(STATION_PRODUCT_NOT_AVAILABLE)
                .withQueryParam("__uid", new EqualToPattern("43"))
                .willReturn(badRequest()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withHeader(MB_ERROR_HEADER, "unapproved_score")
                );
        mediabillingMock.stubFor(builder);
    }

    public void mockNoProductForDevice() {
        MappingBuilder builder = get(urlPathMatching(STATION_PRODUCTS_URI))
                .withName(STATION_PRODUCT_NOT_AVAILABLE)
                .withQueryParam("sku", new EqualToPattern("not_exist"))
                .willReturn(badRequest()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withHeader(MB_ERROR_HEADER, "no_product_for_device")
                );
        mediabillingMock.stubFor(builder);
    }

    public void mockRefund() throws IOException {
        MappingBuilder builder = post(urlPathMatching(REFUND_URI))
                .withName(REFUND_URI)
                .withQueryParam("paymentId", new RegexPattern("\\d+"))
                .willReturn(ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(getStringBodyFromFile("refundResponse.json"))
                );
        mediabillingMock.stubFor(builder);
    }

    public void mockInvoiceCreation() throws IOException {
        MappingBuilder builder = post(urlPathMatching(INVOICE_CREATION_URI))
                .withName(INVOICE_CREATION_URI)
                .willReturn(ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(getStringBodyFromFile("invoiceResponse.json"))
                );
        mediabillingMock.stubFor(builder);
    }

    public void mockOrderStatus() throws IOException {
        MappingBuilder builder = get(urlPathMatching(ORDER_STATUS_URI))
                .withName(ORDER_STATUS_URI)
                .willReturn(ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(getStringBodyFromFile("orderStatus.json"))
                );
        mediabillingMock.stubFor(builder);
    }

    public void mockRefundedOrderStatus() throws IOException {
        MappingBuilder builder = get(urlPathMatching(ORDER_STATUS_URI))
                .withName(ORDER_STATUS_URI)
                .willReturn(ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(getStringBodyFromFile("refundedStatus.json"))
                );
        mediabillingMock.stubFor(builder);
    }
}
