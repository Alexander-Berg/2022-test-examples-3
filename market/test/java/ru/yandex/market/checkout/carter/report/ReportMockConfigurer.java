package ru.yandex.market.checkout.carter.report;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import ru.yandex.market.checkout.checkouter.util.StringListUtils;
import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class ReportMockConfigurer {

    private final WireMockServer reportMock;
    private final CarterReportResponseGenerator carterReportResponseGenerator = new CarterReportResponseGenerator();

    public ReportMockConfigurer(WireMockServer reportMock) {
        this.reportMock = reportMock;
    }

    public void mockReportOk() {
        mockReportOk(new ReportGeneratorParameters());
    }

    public void mockReportOk(ReportGeneratorParameters reportGeneratorParameters) {
        mockSkuOffers(reportGeneratorParameters);
    }

    public void mockSkuOffers(ReportGeneratorParameters reportGeneratorParameters) {
        MappingBuilder mappingBuilder = WireMock.get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.SKU_OFFERS.getId()));

        Set<Long> priceDropMsku = reportGeneratorParameters.getMskuToOfferMap().entries().stream()
                .filter(entry -> entry.getValue().isPriceDropEnabled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());

        if (!priceDropMsku.isEmpty()) {
            mappingBuilder.withQueryParam("promo-by-cart-mskus",
                    equalTo(StringListUtils.toString(priceDropMsku, ",", String::valueOf)));
        }

        reportMock.stubFor(mappingBuilder
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withBody(carterReportResponseGenerator.generate(
                                MarketReportPlace.SKU_OFFERS,
                                reportGeneratorParameters))));
    }

    public void verifyReportMockNotCalled() {
        reportMock.verify(0, anyRequestedFor(anyUrl()));
    }

    public void verifyReportColorCalls(Color color, int callCount) {
        reportMock.verify(callCount, anyRequestedFor(urlPathEqualTo("/yandsearch"))
                .withQueryParam("rgb", color == null ? absent() : equalTo(color.getValue())));
    }

    public void resetMock() {
        reportMock.resetAll();
    }
}
