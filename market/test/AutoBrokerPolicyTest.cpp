#include <market/report/library/relevance/money/auto_broker_policy/old_cpm.h>
#include <market/report/library/relevance/money/hybrid_auction_calculator.h>
#include <market/report/library/money/auto_broker/auto_broker.h>
#include <market/report/library/internals/internals.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NAutoBroker;
using namespace NMarketReport;

/**
 * Tests described in https://st.yandex-team.ru/MARKETOUT-8558
 **/

namespace {
    void initDocPropsForRecFromValues(DocProps& doc, int cbid, int min_bid, float ctr, float ctr_matrix_net,
                                      EDeliveryType delivery, NQBid::TFee fee, uint64_t price, float conv
    ) {
        doc.ctr = ctr;
        doc.ctr_matrix_net = ctr_matrix_net;
        doc.shop_bid = cbid;
        doc.vendor_bid = 0;
        doc.min_bid = min_bid;
        doc.delivery = delivery;
        doc.fee = fee;
        doc.is_cpa = (fee > 0);
        doc.price = TFixedPointNumber{ static_cast<double>(price) / 100 };
        doc.conv = conv;
        doc.doc_id = 42;
    }

    click_price_t GetCpmMinCost(const DocProps& forDoc, const DocProps& orig, const IAutoBrokerPolicy& abPolicy) {
        return abPolicy.CalculateCost(forDoc, orig, EBrokeredField::Bid);
    }
}

const float DEFAULT_CONV_TEST = 0.00002;
TEST(AutoBrokerPolicy, BaseCalcBid) {
    {
        DocProps orig;
        DocProps forCalc;
        initDocPropsForRecFromValues(orig, 20, 10, 0.025, 0.015, EDeliveryType::Priority, 200, 10000, DEFAULT_CONV_TEST);
        initDocPropsForRecFromValues(forCalc, MAX_BID, 10, 0.025, 0.015, EDeliveryType::Priority, 200, 10000, DEFAULT_CONV_TEST);
        {
            TAutoBrokerOldCpmPolicy abPolicy;
            unsigned bid = GetCpmMinCost(forCalc, orig, abPolicy);
            EXPECT_EQ(bid, 21);
        }
    }
}
TEST(AutoBrokerPolicy, FeeCalcBid) {
    {
        DocProps orig;
        DocProps forCalc;
        initDocPropsForRecFromValues(orig, 20, 10, 0.025, 0.025, EDeliveryType::Priority, 200, 10000, DEFAULT_CONV_TEST);
        initDocPropsForRecFromValues(forCalc, MAX_BID, 10, 0.025, 0.025, EDeliveryType::Priority, 300, 10000, DEFAULT_CONV_TEST);
        {
            TAutoBrokerOldCpmPolicy abPolicy;
            unsigned bid = GetCpmMinCost(forCalc, orig, abPolicy);
            EXPECT_EQ(bid, 21);
        }
    }
    {
        DocProps orig;
        DocProps forCalc;
        initDocPropsForRecFromValues(orig, 20, 10, 0.025, 0.025, EDeliveryType::Priority, 200, 10000, DEFAULT_CONV_TEST);
        initDocPropsForRecFromValues(forCalc, MAX_BID, 10, 0.025, 0.025, EDeliveryType::Priority, 3000, 10000, DEFAULT_CONV_TEST);
        {
            TAutoBrokerOldCpmPolicy abPolicy;
            unsigned bid = GetCpmMinCost(forCalc, orig, abPolicy);
            EXPECT_EQ(bid, 21);
        }
    }
}
TEST(AutoBrokerPolicy, PriceCalcBid) {
    {
        DocProps orig;
        DocProps forCalc;
        initDocPropsForRecFromValues(orig, 20, 10, 0.025, 0.025, EDeliveryType::Priority, 200, 10000, DEFAULT_CONV_TEST);
        initDocPropsForRecFromValues(forCalc, MAX_BID, 10, 0.025, 0.025, EDeliveryType::Priority, 200, 20000, DEFAULT_CONV_TEST);
        {
            TAutoBrokerOldCpmPolicy abPolicy;
            unsigned bid = GetCpmMinCost(forCalc, orig, abPolicy);
            EXPECT_EQ(bid, 21);
        }
    }
    {
        DocProps orig;
        DocProps forCalc;
        initDocPropsForRecFromValues(orig, 20, 10, 0.025, 0.025, EDeliveryType::Priority, 200, 10000, DEFAULT_CONV_TEST);
        initDocPropsForRecFromValues(forCalc, MAX_BID, 10, 0.025, 0.025, EDeliveryType::Priority, 200, 100000, DEFAULT_CONV_TEST);
        {
            TAutoBrokerOldCpmPolicy abPolicy;
            unsigned bid = GetCpmMinCost(forCalc, orig, abPolicy);
            EXPECT_EQ(bid, 21);
        }
    }
}
TEST(AutoBrokerPolicy, CtrCalcBid) {
    {
        DocProps orig;
        DocProps forCalc;
        initDocPropsForRecFromValues(orig, 100, 10, 0.02, 0.02, EDeliveryType::Priority, 100, 10000, DEFAULT_CONV_TEST);
        initDocPropsForRecFromValues(forCalc, MAX_BID, 10, 0.08, 0.04, EDeliveryType::Priority, 0, 10000, DEFAULT_CONV_TEST);
        {
            TAutoBrokerOldCpmPolicy abPolicy;
            unsigned bid = GetCpmMinCost(forCalc, orig, abPolicy);
            EXPECT_EQ(bid, 26);
        }
    }
}
TEST(AutoBrokerPolicy, MinCalcBid) {
    {
        DocProps orig;
        DocProps forCalc;
        initDocPropsForRecFromValues(orig, 11, 10, 0.02, 0.02, EDeliveryType::Priority, 100, 10000, DEFAULT_CONV_TEST);
        initDocPropsForRecFromValues(forCalc, MAX_BID, 10, 0.02, 0.02, EDeliveryType::Priority, 1200, 10000, DEFAULT_CONV_TEST);
        {
            TAutoBrokerOldCpmPolicy abPolicy;
            unsigned bid = GetCpmMinCost(forCalc, orig, abPolicy);
            EXPECT_EQ(bid, 12);
        }
    }
    {
        DocProps orig;
        DocProps forCalc;
        initDocPropsForRecFromValues(orig, 20, 10, 0.025, 0.025, EDeliveryType::Priority, 200, 10000, DEFAULT_CONV_TEST);
        initDocPropsForRecFromValues(forCalc, MAX_BID, 10, 0.25, 0.25, EDeliveryType::Priority, 200, 1000000, DEFAULT_CONV_TEST);
        {
            TAutoBrokerOldCpmPolicy abPolicy;
            unsigned bid = GetCpmMinCost(forCalc, orig, abPolicy);
            EXPECT_EQ(bid, 10);
        }
    }
}
TEST(AutoBrokerPolicy, MaxCalcBid) {
    {
        DocProps orig;
        DocProps forCalc;
        initDocPropsForRecFromValues(orig, 500, 10, 1, 1, EDeliveryType::Priority, 100, 10000, DEFAULT_CONV_TEST);
        initDocPropsForRecFromValues(forCalc, MAX_BID, 10, 0.001, 0.001, EDeliveryType::Priority, 1200, 10000, DEFAULT_CONV_TEST);
        {
            TAutoBrokerOldCpmPolicy abPolicy;
            unsigned bid = GetCpmMinCost(forCalc, orig, abPolicy);
            EXPECT_EQ(bid, MAX_BID);
        }
    }
    {
        DocProps orig;
        DocProps forCalc;
        initDocPropsForRecFromValues(orig, 20, 10, 1, 1, EDeliveryType::Priority, 100, 10000, DEFAULT_CONV_TEST);
        initDocPropsForRecFromValues(forCalc, 150, 10, 0.1, 0.1, EDeliveryType::Priority, 1200, 10000, DEFAULT_CONV_TEST);
        {
            TAutoBrokerOldCpmPolicy abPolicy;
            unsigned bid = GetCpmMinCost(forCalc, orig, abPolicy);
            EXPECT_EQ(bid, 150);
        }
    }
}
TEST(HybridAuctionCalc, AmnestyBid) {
    EXPECT_EQ(51, NHybridAuction::UpperBoundBid(0, 100, 50.0, [](unsigned bid){return float(bid);}));
    EXPECT_EQ(51, NHybridAuction::UpperBoundBid(40, 140, 50.0, [](unsigned bid){return float(bid);}));
    EXPECT_EQ(41, NHybridAuction::UpperBoundBid(40, 140, 40.0, [](unsigned bid){return float(bid);}));
    EXPECT_EQ(40, NHybridAuction::UpperBoundBid(40, 140, 35.0, [](unsigned bid){return float(bid);}));
    EXPECT_EQ(100, NHybridAuction::UpperBoundBid(0, 100, 100.0, [](unsigned bid){return float(bid);}));
    EXPECT_EQ(100, NHybridAuction::UpperBoundBid(0, 100, 99.0, [](unsigned bid){return float(bid);}));
    EXPECT_EQ(11, NHybridAuction::UpperBoundBid(0, 100, 100.0, [](unsigned bid){return float(bid*bid);}));
}
