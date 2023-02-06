#include <market/cataloger/src/utils/util.h>
#include <market/cataloger/src/utils/algorithm.h>

#include <library/cpp/testing/unittest/gtest.h>


TEST(TestUtil, Strip)
{
    // empty
    ASSERT_EQ(strip(""), "");
    ASSERT_EQ(strip(" "), "");
    ASSERT_EQ(strip("  "), "");
    // big
    ASSERT_EQ(strip("hello world"), "hello world");
    ASSERT_EQ(strip(" hello world"), "hello world");
    ASSERT_EQ(strip("hello world "), "hello world");
    ASSERT_EQ(strip(" hello world  "), "hello world");
}

std::string findParamValue(std::vector<TParamValuePair>& params, const std::string& name)
{
    for (const auto& param : params)
    {
        if (param.first == name)
            return param.second;
    }
    return "";
}

TEST(TestUtil, SplitUrlToParams)
{
    std::vector<TParamValuePair> params;
    std::string url = "market.yandex.ru/guru.xml?CMD=-RR%3D0%2C0%2C0%2C0-PF%3D2136322283~EQ~sel~x2145182455-VIS%3D8070-CAT_ID%3D11158823-EXC%3D1-PG%3D10&hid=8475557";
    SplitUrlToParams(url, params);
    ASSERT_EQ(findParamValue(params, "hid"), "8475557");
    url = "market.yandex.ru/catalog/91491/list?gfilter=2142557977%3A-5029107&exc=1&regprice=9&how=dpop";
    SplitUrlToParams(url, params);
    ASSERT_EQ(findParamValue(params, "hid"), "91491");
    ASSERT_EQ(findParamValue(params, "gfilter"), "2142557977%3A-5029107");
    ASSERT_EQ(findParamValue(params, "how"), "dpop");
    ASSERT_EQ(findParamValue(params, "abc"), "");
    SplitUrlToParams("market.yandex.ru/catalog/91491/", params);
    ASSERT_EQ(findParamValue(params, "hid"), "91491");
    SplitUrlToParams("market.yandex.ru/catalog/", params);
    ASSERT_TRUE(params.empty());
}

void CheckFindFirstCross(
    const NSorted::TSortedVector<uint32_t>& vectorA,
    const NSorted::TSortedVector<uint32_t>& vectorB,
    const TMaybe<uint32_t>& expectedValue
) {
    ASSERT_EQ(FindFirstCross<uint32_t>(vectorA, vectorB), expectedValue);
    ASSERT_EQ(FindFirstCross<uint32_t>(vectorB, vectorA), expectedValue);
}

TEST(TestUtil, FindFirstCross) {
    // нет пересечений
    CheckFindFirstCross({}, {}, Nothing());
    CheckFindFirstCross({1, 5, 10}, {}, Nothing());
    CheckFindFirstCross({1, 5, 10}, {2, 6, 7}, Nothing());
    // первый с первым
    CheckFindFirstCross({10, 15}, {10, 20}, 10);
    // первый с последним
    CheckFindFirstCross({10, 15}, {8, 9, 10}, 10);
    // последний с последним
    CheckFindFirstCross({1, 5, 10}, {8, 9, 10}, 10);
    // случайные места
    CheckFindFirstCross({10, 20, 30, 40}, {1, 15, 25, 30, 35, 40}, 30);
}
