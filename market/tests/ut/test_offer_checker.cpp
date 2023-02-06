#include <market/library/trees/category_tree.h>
#include <market/idx/offers/lib/checkers/offer_checker.h>
#include <market/idx/offers/lib/iworkers/OfferCtx.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <util/charset/utf8.h>

#include <unordered_map>
#include <utility>


TEST(CreateOfferChecker, UnknownRules) {
    const TString rules = R"({"other_filter": []})";
    auto checker = NMarket::NIdx::CreateOfferChecker(rules);
    ASSERT_EQ(checker->ChecksCount(), 0);
}


TEST(CreateOfferChecker, ParseRules) {
    const TString rules = R"({
        "model": [{"model_id": 1, "exclude": []},
                  {"model_id": 2, "exclude": [{"shop_id": 1}, {"shop_id": 2}]}],
        "shop": [{"shop_id": 1, "include": [{"model_id": 1}, {"vendor_id": 2}]},
                 {"shop_id": 2, "include": [{"vendor_id": 1}]},
                 {"shop_id": 3, "include": [{"url": "http://example.com/1"},
                                            {"url": "https://example.com/2"},
                                            {"url": "example.com/3"}]},
                 {"shop_id": 4, "include": [{"shop_word": "test"}]}],
        "other_filter": []
    })";

    auto checker = NMarket::NIdx::CreateOfferChecker(rules);
    ASSERT_EQ(checker->ChecksCount(), 6);
}


class TTestOfferFrame : public TGlRecord {
public:
    TTestOfferFrame(int shopId, int modelId, std::uint64_t vendorId = 0,
                    const TString& offerTitle = "",
                    const TString& offerUrl = "") {

        this->set_shop_id(shopId);
        this->set_model_id(modelId);
        this->set_vendor_id(vendorId);
        this->set_title(offerTitle);
        this->set_url(offerUrl);
    }

    TTestOfferFrame(TTestOfferFrame&& other) {
        this->set_shop_id(other.shop_id());
        this->set_model_id(other.model_id());
        this->set_vendor_id(other.vendor_id());
        this->set_title(other.title());
        this->set_url(other.url());
    }
};


class TOfferCheckerTest : public ::testing::Test {
public:
    TVector<TTestOfferFrame> FilterOffers(TVector<TTestOfferFrame>&& offers) {
        TVector<TTestOfferFrame> filtered;
        for (auto& offer : offers) {
            if (Checker_->CheckOffer(offer)) {
                filtered.push_back(std::move(offer));
            }
        }
        return filtered;
    }

protected:
    virtual void SetUp() {
        const TString rules = R"({
            "model": [{"model_id": 10, "exclude": []},
                      {"model_id": 20, "exclude": [{"shop_id": 10},
                                                  {"shop_id": 20}]}],
            "shop": [{"shop_id": 1, "include": [{"model_id": 1}]},
                     {"shop_id": 2, "include": [{"vendor_id": 1}]},
                     {"shop_id": 3, "include": [{"url": "http://example.com/1"},
                                                {"url": "https://example.com/2"},
                                                {"url": "example.com/3"}]},
                     {"shop_id": 4, "include": [{"stop_word": "IPhone"},
                                                {"stop_word": "сеалекс"}]},
                     {"shop_id": 5, "include": [{"model_id": 3}, {"vendor_id": 4}]}]
        })";
        Checker_ = NMarket::NIdx::CreateOfferChecker(rules);
    }

private:
    THolder<NMarket::NIdx::TOfferChecker> Checker_;
};


TEST_F(TOfferCheckerTest, ModelEmptyExclude) {
    TVector<TTestOfferFrame> offers;
    // TTestOfferFrame(feed_id, model_id)
    offers.push_back(TTestOfferFrame(10, 10));
    offers.push_back(TTestOfferFrame(20, 10));
    offers.push_back(TTestOfferFrame(10, 20));

    TVector<TTestOfferFrame> filtered = FilterOffers(std::move(offers));
    EXPECT_EQ(filtered.size(), 1);

    auto it = std::find_if(
            filtered.begin(), filtered.end(),
            [](const TTestOfferFrame& offer) { return offer.model_id() == 10; });
    EXPECT_EQ(it, filtered.end());
}


TEST_F(TOfferCheckerTest, ModelNonEmptyExclude) {
    TVector<TTestOfferFrame> offers;
    // TTestOfferFrame(feed_id, model_id)
    offers.push_back(TTestOfferFrame(20, 20));
    offers.push_back(TTestOfferFrame(30, 20));
    offers.push_back(TTestOfferFrame(40, 20));
    offers.push_back(TTestOfferFrame(10, 30));
    offers.push_back(TTestOfferFrame(20, 30));

    TVector<TTestOfferFrame> filtered = FilterOffers(std::move(offers));
    EXPECT_EQ(filtered.size(), 3);

    auto it1 = std::find_if(
            filtered.begin(), filtered.end(),
            [](const TTestOfferFrame& offer) {
                return offer.model_id() == 20 &&
                       (offer.shop_id()!= 10 && offer.shop_id()!= 20);
            });
    EXPECT_EQ(it1, filtered.end());

    auto it2 = std::find_if(
            filtered.begin(), filtered.end(),
            [](const TTestOfferFrame& offer) {
                return offer.model_id() == 20 &&
                       (offer.shop_id()== 10 || offer.shop_id()== 20);
            });
    EXPECT_NE(it2, filtered.end());
}


TEST_F(TOfferCheckerTest, ShopModelId) {
    TVector<TTestOfferFrame> offers;
    // TTestOfferFrame(feedId, modelId, vendorId, title, url)
    offers.push_back(TTestOfferFrame(1, 1, 1, "title", "http://example.com"));
    offers.push_back(TTestOfferFrame(1, 2, 1, "title", "http://example.com"));
    offers.push_back(TTestOfferFrame(888, 1, 2, "title", "http://example.com"));

    TVector<TTestOfferFrame> filtered = FilterOffers(std::move(offers));
    EXPECT_EQ(filtered.size(), 2);

    auto it = std::find_if(
            filtered.begin(), filtered.end(),
            [](const TTestOfferFrame& offer) {
                return offer.shop_id()== 1 && offer.model_id() == 1;
            });
    EXPECT_EQ(it, filtered.end());
}


TEST_F(TOfferCheckerTest, ShopVendorId) {
    TVector<TTestOfferFrame> offers;
    // TTestOfferFrame(feedId, modelId, vendorId, title, url)
    offers.push_back(TTestOfferFrame(2, 1, 1, "title", "http://example.com"));
    offers.push_back(TTestOfferFrame(2, 1, 2, "title", "http://example.com"));
    offers.push_back(TTestOfferFrame(888, 1, 1, "title", "http://example.com"));

    TVector<TTestOfferFrame> filtered = FilterOffers(std::move(offers));
    EXPECT_EQ(filtered.size(), 2);

    auto it = std::find_if(
            filtered.begin(), filtered.end(),
            [](const TTestOfferFrame& offer) {
                return offer.shop_id()== 2 && offer.vendor_id() == 1;
            });
    EXPECT_EQ(it, filtered.end());
}


TEST_F(TOfferCheckerTest, ShopUrl) {
    TVector<TTestOfferFrame> offers;
    // TTestOfferFrame(feedId, modelId, vendorId, title, url)
    offers.push_back(TTestOfferFrame(3, 1, 1, "title", "http://example.com/1"));
    offers.push_back(TTestOfferFrame(3, 1, 1, "title", "example.com/1"));
    offers.push_back(TTestOfferFrame(3, 1, 1, "title", "https://example.com/1"));
    offers.push_back(TTestOfferFrame(3, 1, 1, "title", "https://example.com/2"));
    offers.push_back(TTestOfferFrame(3, 1, 1, "title", "https://example.com/3"));
    offers.push_back(TTestOfferFrame(3, 1, 1, "title", "https://example.com/888"));
    offers.push_back(TTestOfferFrame(888, 1, 1, "title", "http://example.com/1"));

    TVector<TTestOfferFrame> filtered = FilterOffers(std::move(offers));
    EXPECT_EQ(filtered.size(), 2);

    auto it = std::find_if(
            filtered.begin(), filtered.end(),
            [](const TTestOfferFrame& offer) {
                return offer.shop_id()== 3 &&
                       (offer.url().find("example.com/1") != std::string::npos ||
                        offer.url().find("example.com/2") != std::string::npos ||
                        offer.url().find("example.com/3") != std::string::npos);
            });
    EXPECT_EQ(it, filtered.end());
}


TEST_F(TOfferCheckerTest, ShopStopWord) {
    TVector<TTestOfferFrame> offers;
    // TTestOfferFrame(feedId, modelId, vendorId, title, url)
    offers.push_back(TTestOfferFrame(4, 1, 1, "Apple IPhone 6s", "http://example.com/1"));
    offers.push_back(TTestOfferFrame(4, 1, 1, "iPhOnE 5s", "http://example.com/1"));
    offers.push_back(TTestOfferFrame(4, 1, 1, "another phone", "http://example.com/1"));
    offers.push_back(TTestOfferFrame(5, 1, 1, "new IPhone", "http://example.com/1"));

    TVector<TTestOfferFrame> filtered = FilterOffers(std::move(offers));
    EXPECT_EQ(filtered.size(), 2);

    auto it = std::find_if(
            filtered.begin(), filtered.end(),
            [](const TTestOfferFrame& offer) {
                TString title = offer.title();
                std::transform(title.begin(), title.begin() + title.size(), title.begin(), ::toupper);
                return offer.shop_id()== 4 && title.find("IPHONE") != std::string::npos;
            });
    EXPECT_EQ(it, filtered.end());
}


TEST_F(TOfferCheckerTest, ShopStopWordRU) {
    TVector<TTestOfferFrame> offers;
    // TTestOfferFrame(feedId, modelId, vendorId, title, url)
    offers.push_back(TTestOfferFrame(4, 1, 1, "сеалекс 1", "http://exmaple.com/2"));
    offers.push_back(TTestOfferFrame(4, 1, 1, "Сеалекс 2", "http://exmaple.com/2"));
    offers.push_back(TTestOfferFrame(4, 1, 1, "СЕАЛЕКС 3", "http://exmaple.com/2"));
    offers.push_back(TTestOfferFrame(5, 1, 1, "Cеалекс 4", "http://exmaple.com/2"));
    offers.push_back(TTestOfferFrame(4, 1, 1, "Жень шень", "http://exmaple.com/2"));

    TVector<TTestOfferFrame> filtered = FilterOffers(std::move(offers));
    EXPECT_EQ(filtered.size(), 2);

    auto it = std::find_if(
            filtered.begin(), filtered.end(),
            [](const TTestOfferFrame& offer) {
                TString title = ToLowerUTF8(offer.title());
                return offer.shop_id()== 4 && title.find("сеалекс") != std::string::npos;
            });
    EXPECT_EQ(it, filtered.end());
}


TEST_F(TOfferCheckerTest, ShopModelIdVendorId) {
    TVector<TTestOfferFrame> offers;
    // TTestOfferFrame(feedId, modelId, vendorId, title, url)
    offers.push_back(TTestOfferFrame(5, 3, 4, "title", "http://example.com"));
    offers.push_back(TTestOfferFrame(5, 3, 3, "title", "http://example.com"));
    offers.push_back(TTestOfferFrame(5, 4, 4, "title", "http://example.com"));
    offers.push_back(TTestOfferFrame(5, 1, 2, "title", "http://example.com"));
    offers.push_back(TTestOfferFrame(888, 3, 4, "title", "http://example.com"));

    TVector<TTestOfferFrame> filtered = FilterOffers(std::move(offers));
    EXPECT_EQ(filtered.size(), 2);

    auto it = std::find_if(
            filtered.begin(), filtered.end(),
            [](const TTestOfferFrame& offer) {
                return offer.shop_id()== 5 &&
                       (offer.model_id() == 3 || offer.vendor_id() == 4);
            });
    EXPECT_EQ(it, filtered.end());
}
