package ru.yandex.market.checkout.checkouter.offer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.ShowUrlsParam;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Optional.ofNullable;

public class AbstractOfferCategorizeTestBase extends AbstractWebTestBase {

    protected void mockReport(long regionId, List<FoundOffer> offers) {
        mockReport(reportMock, regionId, offers);
    }

    protected void mockReport(WireMockServer reportMock, long regionId, List<FoundOffer> offers) {
        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.OFFER_INFO.getId()))
                .withQueryParam("rids", equalTo((String.valueOf(regionId))))
                .withQueryParam("regset", equalTo("1"))
                .withQueryParam("numdoc", equalTo(String.valueOf(offers.size())))
                .withQueryParam("cpa-category-filter", equalTo("0"))
                .withQueryParam("pp", equalTo("18"))
                .withQueryParam("show-urls", equalTo(ShowUrlsParam.DECRYPTED.getId()));

        toWareMd5(offers).forEach(wareId -> builder.withQueryParam("offerid", equalTo(wareId)));
        ReportGeneratorParameters parameters = new ReportGeneratorParameters(offers);
        for (FoundOffer offer : offers) {
            parameters.overrideItemInfo(
                    offer.getFeedOfferId()
            ).getFulfilment().fulfilment = offer.isFulfillment();
        }

        String response = reportConfigurer.generateResponse(parameters, MarketReportPlace.OFFER_INFO);

        reportMock.stubFor(builder
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(response)
                )
        );
    }

    @Nonnull
    protected FoundOffer createOffer(String wareMd5, Integer warehouseId, long shopId) {
        FoundOffer offerOne = new FoundOffer();
        offerOne.setShopOfferId(UUID.randomUUID().toString());
        offerOne.setWareMd5(wareMd5);
        offerOne.setFulfillment(true);
        offerOne.setWarehouseId(warehouseId);
        ofNullable(warehouseId)
                .map(Integer::longValue)
                .ifPresent(offerOne::setFulfillmentWarehouseId);
        offerOne.setShopId(shopId);
        return offerOne;
    }

    @Nonnull
    protected FoundOffer createOffer(String wareMd5, int warehouseId, long fulfilmentWarehouseId, long shopId) {
        FoundOffer offerOne = createOffer(wareMd5, warehouseId, shopId);
        offerOne.setFulfillmentWarehouseId(fulfilmentWarehouseId);
        return offerOne;
    }

    @Nonnull
    protected FoundOffer createDigitalOffer(String wareMd5, long shopId) {
        FoundOffer offerOne = createOffer(wareMd5, null, shopId);
        offerOne.setDownloadable(true);
        return offerOne;
    }


    protected List<String> toWareMd5(Collection<FoundOffer> offers) {
        return offers.stream().map(FoundOffer::getWareMd5).collect(Collectors.toList());
    }
}
