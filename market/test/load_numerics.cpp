#include <market/report/library/user_data/user_data.h>
#include <market/report/library/cookies/cookies.h>

#include <market/report/library/cgi/params.h>
#include <market/report/library/factors/utils/utils.h>
#include <market/report/library/query_parsing/query_parsing.h>

#include <map>

#include <library/cpp/testing/unittest/gtest.h>

using TMultiMapType = std::multimap<TString, TString, std::less<>>;
using NMarketReport::NFactors::TInplaceVector;

class TMultiMapProvider : public UserData::TProvider {
public:
    TMultiMapProvider(const TMultiMapType& map)
    : Map(map)
    {}

    int FormFieldCount(TStringBuf k) override {
        return Map.count(k);
    }

    virtual const char* FormField(TStringBuf k, int n) override {
        auto range = Map.equal_range(k);
        if (n >= std::distance(range.first, range.second)) {
            return nullptr;
        }
        std::advance(range.first, n);
        return range.first->second.c_str();
    }

    virtual void FormFieldInsert(TStringBuf k, TStringBuf v) override {
        Y_UNUSED(k);
        Y_UNUSED(v);
        Y_FAIL("Not implemented");
    }

    virtual void FormFieldRemove(TStringBuf k, int n) override {
        Y_UNUSED(k);
        Y_UNUSED(n);
        Y_FAIL("Not implemented");
    }

    virtual const char* GetRawRequest() const override {
        Y_FAIL("Not implemented");
    };

    virtual TString GetUnsortedRawRequest() const override {
        Y_FAIL("Not implemented");
    }
private:
    TMultiMapType Map;
};

template<typename M, typename T>
void InsertIntoMap(M& map, const T& elem) {
    map.insert(elem);
}


template<typename M, typename T, typename... Ts>
void InsertIntoMap(M& map, const T& elem, Ts... ts) {
    map.insert(elem);
    InsertIntoMap(map, ts...);
}

TEST(LoadListOfParamsTest, OldSplitting) {
    Cookies cookies;
    TMultiMapType map;
    InsertIntoMap(map,
                std::make_pair("hyperid", "1"),
                std::make_pair("hyperid", "2"),
                std::make_pair("hyperid", "3"));
    TMultiMapProvider provider(map);
    UserData userData(cookies, provider, "", "", "", "", "", false, "", false);

    auto numerics = NMarketReport::LoadListOfParams<int>(userData, "hyperid");
    std::vector<int> expected = TInplaceVector<int>::New(1)(2)(3);
    EXPECT_EQ(expected, numerics);
}

TEST(LoadListOfParamsTest, NewSplitting) {
    Cookies cookies;
    TMultiMapType map;
    InsertIntoMap(map, std::make_pair("hyperid", "1,2,3"));
    TMultiMapProvider provider(map);
    UserData userData(cookies, provider, "", "", "", "", "", false, "", false);

    auto numerics = NMarketReport::LoadListOfParams<int>(userData, "hyperid");
    std::vector<int> expected = TInplaceVector<int>::New(1)(2)(3);
    EXPECT_EQ(expected, numerics);
}

TEST(LoadListOfParamsTest, MixSplitting) {
    Cookies cookies;
    TMultiMapType map;
    InsertIntoMap(map,
                std::make_pair("hyperid", "1,2"),
                std::make_pair("hyperid", "3"));
    TMultiMapProvider provider(map);
    UserData userData(cookies, provider, "", "", "", "", "", false, "", false);

    auto numerics = NMarketReport::LoadListOfParams<int>(userData, "hyperid");
    std::vector<int> expected = TInplaceVector<int>::New(1)(2)(3);
    EXPECT_EQ(expected, numerics);
}

TEST(LoadListOfParamsTest, StringType) {
    Cookies cookies;
    TMultiMapType map;
    InsertIntoMap(map,
                std::make_pair("string", "1,2"),
                std::make_pair("string", "3"));
    TMultiMapProvider provider(map);
    UserData userData(cookies, provider, "", "", "", "", "", false, "", false);

    auto numerics = NMarketReport::LoadListOfParams<TString>(userData, "string");
    std::vector<TString> expected = TInplaceVector<TString>::New("1")("2")("3");
    EXPECT_EQ(expected, numerics);

}
