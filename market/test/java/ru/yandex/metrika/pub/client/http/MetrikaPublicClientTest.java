package ru.yandex.metrika.pub.client.http;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.Header;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.metrika.pub.client.api.MetrikaPublicClient;
import ru.yandex.metrika.pub.client.model.MetrikaActionField;
import ru.yandex.metrika.pub.client.model.MetrikaEcomProduct;
import ru.yandex.metrika.pub.client.model.MetrikaEcommerce;
import ru.yandex.metrika.pub.client.model.MetrikaPurchase;
import ru.yandex.metrika.pub.client.model.MetrikaRoot;
import ru.yandex.metrika.pub.client.model.SiteInfo;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@MockServerSettings(ports = 12233)
public class MetrikaPublicClientTest extends AbstractMetrikaPublicMockServerTest {

    private static final long COUNTER_ID = 1L;
    private static final String BROWSER_INFO = "test_browser_info";
    private static final String PAGE_URL = "test_page_url";

    @Autowired
    private MetrikaPublicClient metrikaPublicClient;

    MetrikaPublicClientTest(MockServerClient server) {
        super(server);
    }

    @Test
    @DisplayName("Запрос успешно выполнен")
    void getPublic_correctData_success() {

        List<Header> headers = List.of(
                new Header("Cookie", "yandexuid=test_uid"),
                new Header("X-Real-IP", "127.0.0.1"),
                new Header("X-Forwarded-For", "127.0.0.1")
        );

        server
                .when(request()
                        .withMethod("GET")
                        .withPath("/watch/" + COUNTER_ID)
                        .withQueryStringParameter("browser-info", BROWSER_INFO)
                        .withQueryStringParameter("page-url", PAGE_URL)
                        .withQueryStringParameter("site-info",
                                loadFile("json/getPublic_correctData.json")
                                        .replaceAll("\\s+", ""))
                        .withHeaders(headers)
                )
                .respond(response()
                        .withStatusCode(200)
                );

        Assertions.assertThatNoException().isThrownBy(() -> metrikaPublicClient.getPublic(
                        COUNTER_ID,
                        BROWSER_INFO,
                        PAGE_URL,
                        getSiteInfo(),
                        "test_uid",
                        "127.0.0.1"
                )
        );
    }



    @Test
    @DisplayName("Запрос возвращает неверный ответ и завершается исключением")
    void getPublic_errorResponse_exception() {

        List<Header> headers = List.of(
                new Header("Cookie", "yandexuid=test_uid"),
                new Header("X-Real-IP", "127.0.0.1"),
                new Header("X-Forwarded-For", "127.0.0.1")
        );

        server
                .when(request()
                        .withMethod("GET")
                        .withPath("/watch/" + COUNTER_ID)
                        .withQueryStringParameter("browser-info", BROWSER_INFO)
                        .withQueryStringParameter("page-url", PAGE_URL)
                        .withQueryStringParameter("site-info",
                                loadFile("json/getPublic_correctData.json")
                                        .replaceAll("\\s+", ""))
                        .withHeaders(headers)
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        Assertions.assertThatThrownBy(() -> metrikaPublicClient.getPublic(
                                COUNTER_ID,
                                BROWSER_INFO,
                                PAGE_URL,
                                getSiteInfo(),
                                "test_uid",
                                "127.0.0.1"
                        )
                )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MetrikaPublic response Internal Server Error error: error");
    }


    private SiteInfo getSiteInfo() {
        SiteInfo siteInfo = new SiteInfo();
        siteInfo.setYm(getMetrikaRoot());
        return siteInfo;
    }

    private MetrikaRoot getMetrikaRoot() {
        MetrikaRoot metrikaRoot = new MetrikaRoot();
        metrikaRoot.setEcommerce(List.of(getMetrikaEcommerce()));
        return metrikaRoot;
    }

    private MetrikaEcommerce getMetrikaEcommerce() {
        MetrikaEcommerce metrikaEcommerce = new MetrikaEcommerce();
        metrikaEcommerce.setCurrencyCode("RUB");
        metrikaEcommerce.setPurchase(getMetrikaPurchase());
        return metrikaEcommerce;
    }

    private MetrikaPurchase getMetrikaPurchase() {
        MetrikaPurchase metrikaPurchase = new MetrikaPurchase();
        metrikaPurchase.setActionField(getMetrikaActionField());
        metrikaPurchase.setProducts(List.of(getMetrikaEcomProduct()));
        return metrikaPurchase;
    }

    private MetrikaActionField getMetrikaActionField() {
        MetrikaActionField metrikaActionField = new MetrikaActionField();
        metrikaActionField.setId("test_id");
        return metrikaActionField;
    }

    private MetrikaEcomProduct getMetrikaEcomProduct() {
        MetrikaEcomProduct metrikaEcomProduct = new MetrikaEcomProduct();
        metrikaEcomProduct.setId("ecom_product_id");
        metrikaEcomProduct.setName("test_name");
        metrikaEcomProduct.setBrand("test_brand");
        metrikaEcomProduct.setCategory("test_category");
        metrikaEcomProduct.setPrice(1.5);
        metrikaEcomProduct.setQuantity(10);
        metrikaEcomProduct.setVariant("test_variant");
        metrikaEcomProduct.setSupplierId(1);
        return metrikaEcomProduct;
    }
}
