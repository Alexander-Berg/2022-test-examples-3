#include <crypta/lib/native/graphite/utils.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta;

Y_UNIT_TEST_SUITE(CanonizeGraphiteMetricNode) {
    Y_UNIT_TEST(Basic) {
        UNIT_ASSERT_STRINGS_EQUAL("a_b_c", CanonizeGraphiteMetricNode("a.b.c"));
    }

    Y_UNIT_TEST(Empty) {
        UNIT_ASSERT_STRINGS_EQUAL("", CanonizeGraphiteMetricNode(""));
    }

    Y_UNIT_TEST(NoDots) {
        UNIT_ASSERT_STRINGS_EQUAL("abc", CanonizeGraphiteMetricNode("abc"));
    }
}

Y_UNIT_TEST_SUITE(MakeGraphiteMetric) {
    Y_UNIT_TEST(Basic) {
        UNIT_ASSERT_STRINGS_EQUAL("a.b.c", MakeGraphiteMetric("a", "b", "c"));
    }

    Y_UNIT_TEST(SingleNode) {
        UNIT_ASSERT_STRINGS_EQUAL("abc", MakeGraphiteMetric("abc"));
    }

    Y_UNIT_TEST(OhterTypes) {
        UNIT_ASSERT_STRINGS_EQUAL("1.2.53.c", MakeGraphiteMetric(1, 2.53, "c"));
    }
}
