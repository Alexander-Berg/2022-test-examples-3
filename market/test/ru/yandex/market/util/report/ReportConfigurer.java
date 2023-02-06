package ru.yandex.market.util.report;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.util.report.CommonReportResponseGenerator;
import ru.yandex.market.checkout.util.report.generators.geo.GeoGeneratorParameters;
import ru.yandex.market.checkout.util.report.generators.offerinfo.OfferInfoGeneratorParameters;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@Component
public class ReportConfigurer {

    private final WireMockServer reportMock;
    private final CommonReportResponseGenerator<OfferInfoGeneratorParameters> offerInfoResponseGenerator;
    private final CommonReportResponseGenerator<GeoGeneratorParameters> geoResponseGenerator;

    public ReportConfigurer(WireMockServer reportMock,
                            CommonReportResponseGenerator<OfferInfoGeneratorParameters> offerInfoResponseGenerator,
                            CommonReportResponseGenerator<GeoGeneratorParameters> geoResponseGenerator) {
        this.reportMock = reportMock;
        this.offerInfoResponseGenerator = offerInfoResponseGenerator;
        this.geoResponseGenerator = geoResponseGenerator;
    }

    public void mockReport(ReportParameters reportParameters) throws IOException {
        MappingBuilder defaultRegionBuilder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo("offerinfo"))
                .withQueryParam("pp", equalTo("18"))
                .withQueryParam("rids", equalTo("0"))
                .withQueryParam("geo", absent());

        reportParameters.getCartRequest().getItems().keySet().stream()
                .map(OfferItemKey::getFeedOfferPart)
                .distinct()
                .forEach(feedOfferId -> {
                    defaultRegionBuilder.withQueryParam("feed_shoffer_id",
                            equalTo(feedOfferId.getFeedId() + "-" + feedOfferId.getId()));
                });

        byte[] bodyArray = offerInfoResponseGenerator
                .generate(MarketReportPlace.OFFER_INFO, reportParameters)
                .getBytes(Charset.defaultCharset());

        reportMock.stubFor(defaultRegionBuilder
                .willReturn(new ResponseDefinitionBuilder().withBody(bodyArray)));

        MappingBuilder deliveryRegionBuilder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo("offerinfo"))
                .withQueryParam("pp", equalTo("18"))
                .withQueryParam("rids", equalTo(String.valueOf(reportParameters.getCartRequest().getRegionId())))
                .withQueryParam("geo", absent());

        reportParameters.getCartRequest().getItems().keySet().stream()
                .map(OfferItemKey::getFeedOfferPart)
                .distinct()
                .forEach(key -> {
                    deliveryRegionBuilder.withQueryParam("feed_shoffer_id",
                            equalTo(key.getFeedId() + "-" + key.getId()));
                });

        reportMock.stubFor(deliveryRegionBuilder
                // TODO: Заменить на генерацию
                .willReturn(new ResponseDefinitionBuilder().withBody(bodyArray)));
    }

    public void mockGeo(ReportGeoParameters reportGeoParameters) throws IOException {

        for (ReportGeoParameters.ReportGeoParametersEntry rgpe : reportGeoParameters.getEntries()) {
            byte[] body = geoResponseGenerator.generate(MarketReportPlace.GEO, rgpe)
                    .getBytes(StandardCharsets.UTF_8);

            reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                    .withQueryParam("place", equalTo("geo"))
                    .withQueryParam("offerid", equalTo(rgpe.getWareMd5()))
                    .withQueryParam("fesh", equalTo(String.valueOf(reportGeoParameters.getShopId())))
                    .withQueryParam("numdoc", equalTo("1000"))
                    // TODO: Заменить на генерацию
                    .willReturn(new ResponseDefinitionBuilder().withBody(body)));
        }
    }

    public List<ServeEvent> getReportEvents() {
        return reportMock.getAllServeEvents();
    }
}
