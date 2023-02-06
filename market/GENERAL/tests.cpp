#include <library/cpp/testing/unittest/registar.h>
#include <market/dynamic_pricing_parsing/cpp_lib/urls.h>

Y_UNIT_TEST_SUITE(Urls) {
    Y_UNIT_TEST(Ozon) {
        // clean urls
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/context/detail/id/1/"), "https://www.ozon.ru/context/detail/id/1/");
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/product/1/"), "https://www.ozon.ru/context/detail/id/1/");
        // invalid urls
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/context/detail/ids/1/"), "");
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/category/1/"), "");
        // without www
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/context/detail/id/1/"), "https://www.ozon.ru/context/detail/id/1/");
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/product/1/"), "https://www.ozon.ru/context/detail/id/1/");
        // without schema
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("www.ozon.ru/context/detail/id/1/"), "https://www.ozon.ru/context/detail/id/1/");
        // trash in path
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/context/detail/id/1/12/48/"), "https://www.ozon.ru/context/detail/id/1/");
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/context/detail/id/asdasdsa-1/12/48/"), "https://www.ozon.ru/context/detail/id/1/");
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/product/1/12/48/"), "https://www.ozon.ru/context/detail/id/1/");
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/product/asdasdsa-1/12/48/"), "https://www.ozon.ru/context/detail/id/1/");
        // trash in query
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/context/detail/id/1/?fhasdjfadslfdsaflkj"), "https://www.ozon.ru/context/detail/id/1/");
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.ozon.ru/context/detail/id/1?afskdlafldashfkjsdafh"), "https://www.ozon.ru/context/detail/id/1/");
        // all in one
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("ozon.ru/context/detail/id/1/12?fhasdjfadslfdsaflkj"), "https://www.ozon.ru/context/detail/id/1/");
    }

    Y_UNIT_TEST(OzonOnelikMe) {
        // normal
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://ozon.onelink.me/SNMZ?af_web_dp=https%253A%252F%252Fwww.ozon.ru%252Fproduct%252Fmerries-podguzniki-trusiki-xl-12-22-kg-38-sht-4826189%252F"), "https://www.ozon.ru/context/detail/id/4826189/");
        // without needed field
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://ozon.onelink.me/SNMZ?af_web=https%253A%252F%252Fwww.ozon.ru%252Fproduct%252Fmerries-podguzniki-trusiki-xl-12-22-kg-38-sht-4826189%252F"), "");
    }

    Y_UNIT_TEST(Wb) {
        // clean
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://www.wildberries.ru/catalog/1/detail.aspx"), "https://www.wildberries.ru/catalog/1/detail.aspx");
        // without some prfixes
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("wildberries.ru/catalog/1/detail.aspx"), "https://www.wildberries.ru/catalog/1/detail.aspx");
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("www.wildberries.ru/catalog/1/detail.aspx"), "https://www.wildberries.ru/catalog/1/detail.aspx");
        // by prefix
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://by.wildberries.ru/catalog/1/detail.aspx"), "https://www.wildberries.ru/catalog/1/detail.aspx");
        // with query
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("https://by.wildberries.ru/catalog/1/detail.aspx?fdhaslkjfdsajlkfsa"), "https://www.wildberries.ru/catalog/1/detail.aspx");
    }

    Y_UNIT_TEST(Other) {
        // empty
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl(""), "");
        // without schema
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("yt.yandex-team.ru"), "http://yt.yandex-team.ru");
        // clean
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("http://yt.yandex-team.ru/hahn/navigation"), "http://yt.yandex-team.ru/hahn/navigation");
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("http://yt.yandex-team.ru/hahn/navigation/"), "http://yt.yandex-team.ru/hahn/navigation/");
        // with query
        UNIT_ASSERT_VALUES_EQUAL(NMarketDP::NormalizeUrl("http://yt.yandex-team.ru/hahn/navigation?path=//drop/it/please"), "http://yt.yandex-team.ru/hahn/navigation");
    }
}
