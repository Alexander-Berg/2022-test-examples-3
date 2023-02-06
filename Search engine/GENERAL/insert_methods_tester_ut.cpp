#include "insert_methods_tester.h"

#include <search/web/util/grouping/grouping.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TInsertMethodsTesterTest) {
    Y_UNIT_TEST(RuleWorks) {
        TUgcInsertMethodsTester tester;
        tester.TestWithFunction([](IRearrangeRuleContext&, IMetaRearrangeContext::TRearrangeParams& rearrangeParams) {
            UNIT_ASSERT(!InitGrouping(rearrangeParams, NSearchGrouping::NName::e.data()).IsDummy());
            UNIT_ASSERT(rearrangeParams.Current);
            UNIT_ASSERT(IsDeepMainGrouping(rearrangeParams.Current));
            UNIT_ASSERT_STRINGS_EQUAL(rearrangeParams.Current->first.Attr, NSearchGrouping::NName::d);
            UNIT_ASSERT(rearrangeParams.Current->second);
        });
    }
}
