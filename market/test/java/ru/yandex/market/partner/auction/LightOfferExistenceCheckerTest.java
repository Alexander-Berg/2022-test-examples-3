package ru.yandex.market.partner.auction;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.partner.auction.servantlet.AuctionServantletMockBase;

import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Тест для {LightOfferExistenceChecker}.
 *
 * @author vbudnev
 */
class LightOfferExistenceCheckerTest {

    private static final long SHOP_ID = 774;
    private static final long FEED_ID_100 = 100;
    private static final AuctionOfferId SOME_TITLE_OFFER_ID = new AuctionOfferId("someTitle");
    private static final AuctionOfferId SOME_FEEDID_OFFER_ID = new AuctionOfferId(FEED_ID_100, "someId");
    private static final OfferExistenceRequest REQUEST_BY_TITLE
            = new OfferExistenceRequest(SHOP_ID, SOME_TITLE_OFFER_ID);
    private static final OfferExistenceRequest REQUEST_BY_ID
            = new OfferExistenceRequest(SHOP_ID, SOME_FEEDID_OFFER_ID);
    @Mock
    private AsyncMarketReportService marketReportService;
    private LightOfferExistenceChecker lightOfferExistenceChecker;

    private static Stream<Arguments> testCases() {
        return Stream.of(
                of("Offer exists", "api_offerinfo_for_casio.xml", true, true),
                of("Offer exists no card", "api_offerinfo_for_casio_no_model-id.xml", true, false),
                of("Offer is missing", "api_offerinfo_empty.xml", false, false)
        );
    }

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        lightOfferExistenceChecker = new LightOfferExistenceChecker(marketReportService);
    }

    @DisplayName("Check existence when requested by feedId-offerId")
    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void test_checkExistence_byId(String description,
                                  String reportAnswerPayload,
                                  Boolean expectedExistenceCheck,
                                  Boolean expectedCardAttachment)
            throws Exception {
        mockReportAnswer(reportAnswerPayload);
        LightOfferExistenceChecker.LightOfferInfo actual = lightOfferExistenceChecker.getOfferInfo(
                REQUEST_BY_TITLE).get();
        Assert.assertEquals(expectedExistenceCheck, actual.isOfferFound());
        Assert.assertEquals(expectedCardAttachment, actual.isCardOffer());

        MarketSearchRequest capturedRed = extractMarketSearchReq();
        Assert.assertEquals(MarketSearchRequest.REGARDLESS_OF_THE_REGION, capturedRed.getRegionId().longValue());
        Assert.assertEquals(SHOP_ID, capturedRed.getShopId().longValue());
        Assert.assertEquals(SOME_TITLE_OFFER_ID.getId(), capturedRed.getQuery2());
        Assert.assertEquals(AuctionServantletMockBase.PARTNER_INTERFACE_CLIENT, capturedRed.getClient());
    }

    @DisplayName("Check existence when requested by title")
    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void test_checkExistence_byTitle(String description,
                                     String reportAnswerPayload,
                                     Boolean expectedExistenceCheck,
                                     Boolean expectedCardAttachment)
            throws Exception {
        mockReportAnswer(reportAnswerPayload);
        LightOfferExistenceChecker.LightOfferInfo offerInfo
                = lightOfferExistenceChecker.getOfferInfo(REQUEST_BY_ID).get();
        Assert.assertEquals(expectedExistenceCheck, offerInfo.isOfferFound());
        Assert.assertEquals(expectedCardAttachment, offerInfo.isCardOffer());

        MarketSearchRequest capturedRed = extractMarketSearchReq();
        Assert.assertEquals(MarketSearchRequest.REGARDLESS_OF_THE_REGION, capturedRed.getRegionId().longValue());
        Assert.assertEquals(SHOP_ID, capturedRed.getShopId().longValue());
        Assert.assertEquals(ImmutableSet.of(SOME_FEEDID_OFFER_ID), capturedRed.getOfferIds());
        Assert.assertEquals(AuctionServantletMockBase.PARTNER_INTERFACE_CLIENT, capturedRed.getClient());
    }

    private void mockReportAnswer(String fileName) throws IOException, SAXException {
        LightOfferExistenceChecker.ReportOfferExistenceParser parser
                = new LightOfferExistenceChecker.ReportOfferExistenceParser();
        parser.parseXmlStream(this.getClass().getResourceAsStream(fileName));

        doReturn(CompletableFuture.completedFuture(parser))
                .when(marketReportService).async(any(), any());
    }

    private MarketSearchRequest extractMarketSearchReq() {
        ArgumentCaptor<MarketSearchRequest> requestCaptor
                = ArgumentCaptor.forClass(MarketSearchRequest.class);
        verify(marketReportService).async(requestCaptor.capture(), any());

        return requestCaptor.getValue();
    }

}
