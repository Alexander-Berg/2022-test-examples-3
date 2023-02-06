package ru.yandex.market.shopadminstub.beans;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.apache.commons.io.IOUtils;
import ru.yandex.market.common.report.model.FeedOfferId;

import java.io.IOException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public abstract class WireMockHelper {
    /**
     * Мокаем ответ place=mainreport
     *
     * @param reportMock
     * @param feedOfferId
     * @param regionId
     * @param resource
     */
    public static void mockReport(WireMockServer reportMock, FeedOfferId feedOfferId, long regionId, URL resource) throws IOException {
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo("offerinfo"))
                .withQueryParam("pp", equalTo("18"))
                .withQueryParam("feed_shoffer_id", equalTo(feedOfferId.getFeedId() + "-" + feedOfferId.getId()))
                .withQueryParam("rids", equalTo(String.valueOf(regionId)))
                .withQueryParam("regset", equalTo("1"))
                .withQueryParam("regional-delivery", equalTo("1"))
                .withQueryParam("geo", absent())
                .willReturn(new ResponseDefinitionBuilder().withBody(IOUtils.toByteArray(resource))));
    }

    public static void mockReportGeo(WireMockServer reportMock, long shopId, URL resource) throws IOException {
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo("geo"))
                .withQueryParam("fesh", equalTo(String.valueOf(shopId)))
                .willReturn(new ResponseDefinitionBuilder().withBody(IOUtils.toByteArray(resource))));
    }
}
