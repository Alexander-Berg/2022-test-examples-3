#include <crypta/graph/rtmr/lib/common/browserinfo.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TestBrowserInfo) {
    using namespace NCrypta::NGraph;

    void Test(const TString& browserInfo, const TString& key, const TString& expected) {
        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo, key), expected);
    }

    Y_UNIT_TEST(empty) {
        const TString emptyBrowserInfo{};

        Test(emptyBrowserInfo, {}, {});
        Test(emptyBrowserInfo, TString{"aa"}, {});
    }

    Y_UNIT_TEST(notEmpty) {
        const TString browserInfo1{"a:aaa:b:aaa:t:c:c:d:e"};
        const TString browserInfo2{"a:aaa:b:aaa:c:c:d:d:e:f"};

        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo1, TString{"a"}), TString{"aaa"});
        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo1, TString{"b"}), TString{"aaa"});
        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo1, TString{"t"}), TString{"c:c:d:e"});
        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo1, TString{"c"}), TString{});
        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo1, TString{"d"}), TString{});

        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo2, TString{"a"}), TString{"aaa"});
        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo2, TString{"b"}), TString{"aaa"});
        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo2, TString{"c"}), TString{"c"});
        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo2, TString{"e"}), TString{"f"});
        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo2, TString{"aaa"}), TString{});
        UNIT_ASSERT_EQUAL(NBrowserInfo::GetValue(browserInfo2, TString{"t"}), TString{});
    }
}
