package ru.yandex.market.replenishment.autoorder.service.ax;

import java.time.LocalDateTime;
import java.util.List;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.config.security.tvm.TvmService;
import ru.yandex.market.replenishment.autoorder.dto.AxCreatePurchasePriceDto;
import ru.yandex.market.replenishment.autoorder.dto.AxaptaPriceSpecType;
import ru.yandex.market.replenishment.autoorder.dto.AxaptaPurchasePriceItem;
import ru.yandex.market.replenishment.autoorder.model.PriceSpecificationStatus;
import ru.yandex.market.replenishment.autoorder.service.AxHandlerService;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentConstants;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AxHandlerServiceTest extends FunctionalTest {

    private AxHandlerService axHandlerService;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Before
    public void setUp() {
        initStubs();
        EnvironmentService environmentService = mock(EnvironmentService.class);
        when(environmentService.getBooleanWithDefault(
            EnvironmentConstants.PRICE_SPEC_AXAPTA_HANDLERS_ENABLED, false)
        ).thenReturn(true);
        axHandlerService = new AxHandlerService(123, mock(TvmService.class), "http://localhost:8080/",
            new RestTemplate(new HttpComponentsClientHttpRequestFactory()), environmentService);
    }

    @Test
    public void testRequestBody_sendAddPriceSpecRequest() {
        AxCreatePurchasePriceDto dto = new AxCreatePurchasePriceDto("rsId", "agreement", AxaptaPriceSpecType.TENDER,
            1L, LocalDateTime.of(2021, 6, 24, 18, 0, 0),
            LocalDateTime.of(2021, 6, 25, 18, 0, 0),
            List.of(new AxaptaPurchasePriceItem("ssku", 2.0, 1L)));
        axHandlerService.sendAddPriceSpecRequest(dto);
        List<LoggedRequest> requests = findAll(postRequestedFor(urlEqualTo("/create-purch-price")));
        Assertions.assertEquals(requests.get(0).getBodyAsString(), "{\"rs_id\":\"rsId\",\"agreement\":\"agreement\"," +
            "\"spec_type\":\"TENDER\",\"tender_num\":1,\"date_from\":\"2021-06-24 18:00:00.000000\"," +
            "\"date_to\":\"2021-06-25 18:00:00.000000\",\"lines\":[{\"vat_rate\":20,\"ssku\":\"ssku\"," +
            "\"price\":2.0,\"currency\":\"RUB\",\"quantity_to\":1}]}");
    }

    @Test
    public void testRequestBody_changePurchPriceStatus() {
        axHandlerService.changePurchPriceStatus("priceSpecificationAxaptaSplitId", PriceSpecificationStatus.OK);
        List<LoggedRequest> requests = findAll(postRequestedFor(urlEqualTo("/change_purch_price_status")));
        Assertions.assertEquals(requests.get(0).getBodyAsString(), "{\"id\":\"priceSpecificationAxaptaSplitId\"," +
            "\"status\":\"SignedByVendor\"}");
    }

    private void initStubs() {
        stubFor(post(urlEqualTo("/create-purch-price"))
            .willReturn(okJson("{ \"isOk\": true }")));
        stubFor(post(urlEqualTo("/change_purch_price_status"))
            .willReturn(okJson("{ \"isOk\": true }")));
    }
}
