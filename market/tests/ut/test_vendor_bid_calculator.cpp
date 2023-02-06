#include <market/idx/offers/lib/iworkers/OfferCtx.h>
#include <market/idx/offers/lib/iworkers/vendor_bid_calculator.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/tests_data.h>


using TVendorRecord = NMarket::TVendorPropsRecord;

class TTestOffer : public TGlRecord {
public:
    TTestOffer(int vendorId, int categoryId, int shopId)  {

        this->set_vendor_id(vendorId);
        this->set_category_id(categoryId);
        this->set_shop_id(shopId);
    }

    TTestOffer(TTestOffer&& other) {
        this->set_vendor_id(other.vendor_id());
        this->set_category_id(other.category_id());
        this->set_shop_id(other.shop_id());
    }
};


class TTestVendorBidCalculator {
public:
    static TTestVendorBidCalculator& Instance() {
        static TTestVendorBidCalculator calculator;
        return calculator;
    }

    TVendorRecord CreateVendorBidInfo(TGlRecord *glRecord) const {
        TVendorBidCalculator calculator;
        calculator.Load(SRC_("data/vendor_bids.csv"));
        return calculator.CreateVendorBidInfo(glRecord);
    }

private:
    TTestVendorBidCalculator() {
        NCategoryTreeHelper::InitCategoryTree(SRC_("data/tovar-tree.pb"));
    }
};


TEST(VendorBidCalculator, TestBids) {
    TTestVendorBidCalculator& calculator = TTestVendorBidCalculator::Instance();

    {
        TTestOffer offer = TTestOffer(1, 91302, 777);
        const TVendorRecord record = calculator.CreateVendorBidInfo(&offer);
        EXPECT_EQ(record.VendorBid, 300);
        EXPECT_EQ(record.IsRecommended, 1);
        EXPECT_EQ(record.VendorDataSource, 192383);
    }

    {
        TTestOffer offer = TTestOffer(2, 91007, 777);
        const TVendorRecord record = calculator.CreateVendorBidInfo(&offer);
        EXPECT_EQ(record.VendorBid, 50);
        EXPECT_EQ(record.IsRecommended, 0);
        EXPECT_EQ(record.VendorDataSource, 192383);
    }

    {
        TTestOffer offer = TTestOffer(1, 91007, 888);
        const TVendorRecord record = calculator.CreateVendorBidInfo(&offer);
        EXPECT_EQ(record.VendorBid, 700);
        EXPECT_EQ(record.IsRecommended, 0);
        EXPECT_EQ(record.VendorDataSource, 192383);
    }
}


TEST(VendorBidCalculator, TestBidsPropagation) {
    TTestVendorBidCalculator& calculator = TTestVendorBidCalculator::Instance();

    // bid propagation from intermediate category
    {
        TTestOffer offer = TTestOffer(1, 91007, 777);
        const TVendorRecord record = calculator.CreateVendorBidInfo(&offer);
        EXPECT_EQ(record.VendorBid, 200);
        EXPECT_EQ(record.IsRecommended, 1);
        EXPECT_EQ(record.VendorDataSource, 192383);
    }

    // bid propagation from "All goods"
    {
        TTestOffer offer = TTestOffer(1, 91325, 777);
        const TVendorRecord record = calculator.CreateVendorBidInfo(&offer);
        EXPECT_EQ(record.VendorBid, 100);
        EXPECT_EQ(record.IsRecommended, 0);
        EXPECT_EQ(record.VendorDataSource, 192383);
    }
}


TEST(VendorBidCalculator, NullBids) {
    TTestVendorBidCalculator& calculator = TTestVendorBidCalculator::Instance();

    // vendor_id = 3 is not set in vendor_bids.csv
    {
        TTestOffer offer = TTestOffer(3, 91007, 777);
        const TVendorRecord record = calculator.CreateVendorBidInfo(&offer);
        EXPECT_EQ(record.VendorBid, 0);
        EXPECT_EQ(record.IsRecommended, 0);
        EXPECT_EQ(record.VendorDataSource, 0);
    }

    // shop_id = 123 is not set in vendor_bids.csv
    {
        TTestOffer offer = TTestOffer(1, 91325, 123);
        const TVendorRecord record = calculator.CreateVendorBidInfo(&offer);
        EXPECT_EQ(record.VendorBid, 0);
        EXPECT_EQ(record.IsRecommended, 0);
        EXPECT_EQ(record.VendorDataSource, 0);
    }
}
