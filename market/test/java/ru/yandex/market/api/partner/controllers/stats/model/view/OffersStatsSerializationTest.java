package ru.yandex.market.api.partner.controllers.stats.model.view;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.serialization.BaseOldSerializationTest;
import ru.yandex.market.api.partner.controllers.stats.model.OfferStats;
import ru.yandex.market.api.partner.controllers.stats.model.OfferStatsV1;
import ru.yandex.market.api.partner.controllers.stats.model.OffersStats;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.clickreport.model.TotalStats;

/**
 * @author m-bazhenov
 */
class OffersStatsSerializationTest extends BaseOldSerializationTest {

    @Test
    void shouldSerializeAllFieldsOffersStatsV1() {
        OffersStats offersStats = prepareOffersStatsV1AllFields();
        getChecker().testSerialization(
                offersStats,

                "{" +
                        "  \"fromOffer\": 1," +
                        "  \"toOffer\": 2," +
                        "  \"totalOffersCount\": 2," +
                        "  \"offerStats\": [" +
                        "    {" +
                        "      \"clicks\": 1," +
                        "      \"spending\": \"10\"," +
                        "      \"offerName\": \"test offer\"" +
                        "    }," +
                        "    {" +
                        "      \"clicks\": 0," +
                        "      \"spending\": \"10\"," +
                        "      \"offerName\": \"test offer\"" +
                        "    " + "}" +
                        "  ]" +
                        "}",

                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<offers-stats total-offers-count=\"2\" from-offer=\"1\" to-offer=\"2\">" +
                        "    <offer-stats clicks=\"1\" spending=\"10\">" +
                        "        <offer-name>test offer</offer-name>" +
                        "    </offer-stats>" +
                        "    <offer-stats clicks=\"0\" spending=\"10\">" +
                        "        <offer-name>test offer</offer-name>" +
                        "</offer-stats>" +
                        "</offers-stats>");
    }

    @Test
    void shouldSerializeAllFieldsOffersStatsV2() {
        OffersStats offersStats = prepareOffersStatsV2AllFields();
        getChecker().testSerialization(
                offersStats,

                "{" +
                        "  \"fromOffer\":1," +
                        "  \"toOffer\":2," +
                        "  \"totalOffersCount\":2," +
                        "  \"offerStats\":[" +
                        "    {" +
                        "      \"clicks\":1," +
                        "      \"spending\":\"10\"," +
                        "      \"createdOrders\":1," +
                        "      \"createdOrdersItems\":1," +
                        "      \"createdOrdersGmv\":1.00," +
                        "      \"createdOrdersCpaSpending\":1," +
                        "      \"acceptedOrders\":1," +
                        "      \"orderItems\":1," +
                        "      \"acceptedOrdersItems\":1," +
                        "      \"acceptedOrdersGmv\":1.00," +
                        "      \"cpaSpending\":1," +
                        "      \"acceptedOrdersCpaSpending\":1," +
                        "      \"offerName\":\"test offer\"," +
                        "      \"feedId\":1," +
                        "      \"offerId\":\"offer_id\"," +
                        "      \"url\":\"test offer url\"," +
                        "      \"detailedStats\":[" +
                        "        {" +
                        "          \"type\":\"mobile\"," +
                        "          \"clicks\":0," +
                        "          \"spending\":\"0.00\"," +
                        "          \"orderItems\":0," +
                        "          \"acceptedOrdersItems\":0," +
                        "          \"cpaSpending\":0," +
                        "          \"acceptedOrdersCpaSpending\":0" +
                        "        }" +
                        "      ]" +
                        "    }" +
                        "  ]" +
                        "}",

                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "" +
                        "<offers-stats total-offers-count=\"2\" from-offer=\"1\" to-offer=\"2\">" +
                        "  <offer-stats " +
                        "    clicks=\"1\"" +
                        "    spending=\"10\"" +
                        "    created-orders=\"1\"" +
                        "    created-orders-items=\"1\"" +
                        "    created-orders-gmv=\"1.00\"" +
                        "    created-orders-cpa-spending=\"1\"" +
                        "    accepted-orders=\"1\"" +
                        "    order-items=\"1\"" +
                        "    accepted-orders-items=\"1\"" +
                        "    accepted-orders-gmv=\"1.00\"" +
                        "    cpa-spending=\"1\"" +
                        "    accepted-orders-cpa-spending=\"1\"" +
                        "    feed-id=\"1\"" +
                        "    offer-id=\"offer_id\">" +
                        "    <url>test offer url</url>" +
                        "    <offer-name>test offer</offer-name>" +
                        "    <detailed-stats>" +
                        "      <stats " +
                        "        type=\"mobile\" " +
                        "        clicks=\"0\" " +
                        "        spending=\"0.00\" " +
                        "        order-items=\"0\" " +
                        "        accepted-orders-items=\"0\" " +
                        "        cpa-spending=\"0\" " +
                        "        accepted-orders-cpa-spending=\"0\"/>" +
                        "    </detailed-stats>" +
                        "  </offer-stats>" +
                        "</offers-stats>"
        );
    }


    @Test
    void shouldNotSerializeNullFields() {
        OffersStats offersStats = prepareOffersStatsV2WithNullFields();
        getChecker().testSerialization(
                offersStats,

                "{" +
                        "  \"fromOffer\":1," +
                        "  \"toOffer\":2," +
                        "  \"totalOffersCount\":2," +
                        "  \"offerStats\":[" +
                        "    {" +
                        "      \"clicks\":1," +
                        "      \"spending\":\"10\"," +
                        "      \"offerName\":\"test offer\"," +
                        "      \"feedId\":1," +
                        "      \"offerId\":\"offer_id\"" +
                        "    }" +
                        "  ]" +
                        "}",

                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<offers-stats total-offers-count=\"2\" from-offer=\"1\" to-offer=\"2\">" +
                        "  <offer-stats clicks=\"1\" spending=\"10\" feed-id=\"1\" offer-id=\"offer_id\">" +
                        "    <offer-name>test offer</offer-name>" +
                        "  </offer-stats>" +
                        "</offers-stats>"
        );
    }

    private OffersStats getBaseOffersStats() {
        OffersStats offersStats = new OffersStats();
        offersStats.setFromOffer(1);
        offersStats.setToOffer(2);
        offersStats.setTotalOffersCount(2);
        return offersStats;
    }

    private OffersStats prepareOffersStatsV1AllFields() {
        OffersStats offersStats = getBaseOffersStats();
        offersStats.setOfferStats(Arrays.asList(
                new OfferStatsV1() {{
                    setClicks(1);
                    setOfferName("test offer");
                    setSpending(BigDecimal.TEN);
                }},

                new OfferStatsV1() {{
                    setClicks(0);
                    setOfferName("test offer");
                    setSpending(BigDecimal.TEN);
                }}));
        return offersStats;
    }

    private OffersStats prepareOffersStatsV2AllFields() {
        OffersStats offersStats = getBaseOffersStats();
        offersStats.setOfferStats(Collections.singletonList(
                new OfferStats() {{
                    setOfferId(new AuctionOfferId(1L, "offer_id"));
                    setClicks(1);
                    setOfferName("test offer");
                    setSpending(BigDecimal.TEN);
                    setUrl("test offer url");
                    setAcceptedOrders(1);
                    setAcceptedOrdersItems(1);
                    setAcceptedOrdersCpaSpending(BigDecimal.ONE);
                    setAcceptedOrdersGmv(BigDecimal.ONE);
                    setCreatedOrders(1);
                    setCreatedOrdersItems(1);
                    setCreatedOrdersCpaSpending(BigDecimal.ONE);
                    setCreatedOrdersGmv(BigDecimal.ONE);
                    setDetailedStats(Collections.singletonMap("mobile", new TotalStats() {{
                        setClicks(0);
                        setSpending(BigInteger.ZERO);
                        setAcceptedOrdersItems(0);
                        setAcceptedOrdersCpaSpending(BigDecimal.ZERO);
                    }}));
                }}));
        return offersStats;
    }

    private OffersStats prepareOffersStatsV2WithNullFields() {
        OffersStats offersStats = getBaseOffersStats();
        offersStats.setOfferStats(Collections.singletonList(
                new OfferStats() {{
                    setOfferId(new AuctionOfferId(1L, "offer_id"));
                    setClicks(1);
                    setOfferName("test offer");
                    setSpending(BigDecimal.TEN);
                }}));
        return offersStats;
    }
}