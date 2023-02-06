#include <market/report/library/dynamic_filter/cutoff_filter.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/json/json_reader.h>
#include <util/stream/str.h>
#include <util/generic/is_in.h>

Y_UNIT_TEST_SUITE(LibReportBookNowCutoffFilterTest) {

    Y_UNIT_TEST(Empty) {
        auto filter = NMarketReport::NBookNow::CreateCutOffFilter("[]", "[]");
        UNIT_ASSERT(!filter->IsShopDisabled(1));
    }

    Y_UNIT_TEST(BadJsonStructure) {
        UNIT_ASSERT_EXCEPTION(NMarketReport::NBookNow::CreateCutOffFilter("[1, 2, 3", "[]"), NJson::TJsonException);
    }

    Y_UNIT_TEST(Shops) {
        auto filter = NMarketReport::NBookNow::CreateCutOffFilter("[1, 2, 7856]", "[]");
        UNIT_ASSERT(filter->IsShopDisabled(1));
        UNIT_ASSERT(filter->IsShopDisabled(2));
        UNIT_ASSERT(filter->IsShopDisabled(7856));
        UNIT_ASSERT(!filter->IsShopDisabled(10));
    }

    Y_UNIT_TEST(Offers) {
        const char *disabledOutletsStr = R"zzz(
                [
                   ["158928685cb2e819166861ea0c5ad4a3", 5, 8, 13],
                   ["1e4d494d3bbe0264cfb49130c2294183", 3, 51, 120, 7745]
                ]
            )zzz";
        auto filter = NMarketReport::NBookNow::CreateCutOffFilter("[]", disabledOutletsStr);
        auto nonexistent = filter->GetOutletsWhereOfferIsDisabled(Market::parseHexMd5("26361281364590d8fa0620395fad37eb"));
        UNIT_ASSERT(nonexistent.empty());
        auto one = filter->GetOutletsWhereOfferIsDisabled(Market::parseHexMd5("158928685cb2e819166861ea0c5ad4a3"));
        UNIT_ASSERT(IsIn(one, 5));
        UNIT_ASSERT(IsIn(one, 8));
        UNIT_ASSERT(IsIn(one, 13));
        UNIT_ASSERT(!IsIn(one, 21));
    }

}
