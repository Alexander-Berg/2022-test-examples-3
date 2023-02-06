package ru.yandex.market.util.report;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import ru.yandex.market.common.report.model.CurrencyConvertRequest;
import ru.yandex.market.common.report.model.CurrencyConvertRequestUrlBuilder;
import ru.yandex.market.common.report.model.CurrencyConvertResult;
import ru.yandex.market.util.TestSerializationService;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@Component
public class CurrencyConvertConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyConvertConfigurer.class);

    private final WireMockServer reportMock;
    private final TestSerializationService testSerializationService;
    private final CurrencyConvertRequestUrlBuilder urlBuilder;

    public CurrencyConvertConfigurer(WireMockServer reportMock,
                                     TestSerializationService testSerializationService,
                                     CurrencyConvertRequestUrlBuilder urlBuilder) {
        this.reportMock = reportMock;
        this.testSerializationService = testSerializationService;
        this.urlBuilder = urlBuilder;
    }

    public void mockCurrencyConvert(CurrencyConvertRequest currencyConvertRequest,
                                    CurrencyConvertResult currencyConvertResult) {
        String url = urlBuilder.build(currencyConvertRequest) + "&client=checkout&co-from=shopadmin-stub";

        logger.debug("Mocking request: {}", url);

        UriComponents components = UriComponentsBuilder.fromUriString(url).build();

        MappingBuilder builder = get(urlPathEqualTo(components.getPath()));
        components.getQueryParams().forEach((key, values) -> {
            values.forEach(value -> {
                builder.withQueryParam(key, equalTo(value));
            });
        });


        reportMock.stubFor(builder
                    .willReturn(ResponseDefinitionBuilder.responseDefinition()
                    .withBody(testSerializationService.serializeJson(currencyConvertResult))));
    }
}
