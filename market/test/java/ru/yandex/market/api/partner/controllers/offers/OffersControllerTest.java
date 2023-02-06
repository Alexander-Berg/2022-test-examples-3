package ru.yandex.market.api.partner.controllers.offers;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.report.ApiMarketReportService;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.report.model.json.miprime.MiprimeSearchResult;
import ru.yandex.market.common.report.parser.json.MiprimeSearchResultParser;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author otedikova
 */
public class OffersControllerTest extends FunctionalTest {
    private static final long CAMPAIGN_ID = 10774;
    @Autowired
    @Qualifier(value = "marketReportService")
    private ApiMarketReportService marketReportService;

    @BeforeEach
    public void setUp() throws IOException {
        mockReportService();
    }

    @DisplayName("Получение офферов кампании - JSON V2")
    @Test
    public void getCampaignOffersV2TestJson() {
        String response = makeRequest("v2", Format.JSON.formatName());
        String expected = StringTestUtil.getString(getClass(), "json/papi_expected_response_v2.json");
        MbiAsserts.assertJsonEquals(expected, response);
    }

    @DisplayName("Получение офферов кампании 500 error - JSON V2")
    @Test
    public void getCampaignOffersV2ErrorTestJson() {
        mockErrorInReportService();
        String response = makeRequest("v2", Format.JSON.formatName());
        String expected = StringTestUtil.getString(getClass(), "json/papi_expected_500_response.json");
        MbiAsserts.assertJsonEquals(expected, response);
    }

    @DisplayName("Получение офферов кампании - XML V2")
    @Test
    public void getCampaignOffersV2TestXml() {
        String response = makeRequest("v2", Format.XML.formatName());
        String expected = StringTestUtil.getString(getClass(), "xml/papi_expected_response_v2.xml");

        MbiAsserts.assertXmlEquals(expected, response);
    }

    @DisplayName("Получение офферов кампании 500 error - XML V2")
    @Test
    public void getCampaignOffersV2ErrorTestXml() {
        mockErrorInReportService();
        String response = makeRequest("v2", Format.XML.formatName());
        String expected = StringTestUtil.getString(getClass(), "xml/papi_expected_500_response.xml");

        MbiAsserts.assertXmlEquals(expected, response);
    }

    @DisplayName("Получение офферов кампании - JSON V1")
    @Test
    public void getCampaignOffersV1TestJson() {
        String response = makeRequest("v1", Format.JSON.formatName());
        String expected = StringTestUtil.getString(getClass(), "json/papi_expected_response_v1.json");
        MbiAsserts.assertJsonEquals(expected, response);
    }

    @DisplayName("Получение офферов кампании 500 error - JSON V1")
    @Test
    public void getCampaignOffersV1ErrorTestJson() {
        mockErrorInReportService();
        String response = makeRequest("v1", Format.JSON.formatName());
        String expected = StringTestUtil.getString(getClass(), "json/papi_expected_500_response.json");
        MbiAsserts.assertJsonEquals(expected, response);
    }

    @DisplayName("Получение офферов кампании - XML V1")
    @Test
    public void getCampaignOffersV1TestXml() {
        String response = makeRequest("v1", Format.XML.formatName());
        String expected = StringTestUtil.getString(getClass(), "xml/papi_expected_response_v1.xml");

        MbiAsserts.assertXmlEquals(expected, response);
    }

    @DisplayName("Получение офферов кампании 500 error - XML V1")
    @Test
    public void getCampaignOffersV1ErrorTestXml() {
        mockErrorInReportService();
        String response = makeRequest("v1", Format.XML.formatName());
        String expected = StringTestUtil.getString(getClass(), "xml/papi_expected_500_response.xml");

        MbiAsserts.assertXmlEquals(expected, response);
    }

    private String makeRequest(String version, String format) {
        try {
            return FunctionalTestHelper.makeRequest(url(version, CAMPAIGN_ID, format), HttpMethod.GET, String.class).getBody();
        } catch (HttpServerErrorException e) {
            return e.getResponseBodyAsString();
        }
    }

    private String url(String version, long campaignId, String format) {
        return String.format("%s/%s/campaigns/%d/offers.%s",
                urlBasePrefix, version, campaignId, format);
    }

    private void mockReportService() throws IOException {
        MiprimeSearchResultParser parser = new MiprimeSearchResultParser();
        MiprimeSearchResult searchResult = parser.parse(this.getClass().getResourceAsStream("json/miprime_report.json"));
        CompletableFuture<MiprimeSearchResult> future = CompletableFuture.completedFuture(searchResult);
        Mockito.when(marketReportService.async(any(MarketSearchRequest.class), any(MiprimeSearchResultParser.class)))
                .thenReturn(future);
    }

    private void mockErrorInReportService() {
        Mockito.when(marketReportService.async(any(MarketSearchRequest.class), any(MiprimeSearchResultParser.class)))
                .thenThrow(new IllegalStateException());
    }
}

