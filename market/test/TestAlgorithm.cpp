#include "Algorithm.h"

#include <library/cpp/testing/unittest/gtest.h>


TEST(TestAlgorithm, Split)
{
    TVector<TString> strings;
    StringCollector f(strings);
    split("", ':', f);
    ASSERT_TRUE(strings.size() == 1);
    ASSERT_EQ(strings[0], "");

    strings.clear();
    split(":", ':', f);
    ASSERT_TRUE(strings.size() == 2);
    ASSERT_EQ(strings[0], "");
    ASSERT_EQ(strings[1], "");

    strings.clear();
    split("a:bcd:", ':', f);
    ASSERT_TRUE(strings.size() == 3);
    ASSERT_EQ(strings[0], "a");
    ASSERT_EQ(strings[1], "bcd");
    ASSERT_EQ(strings[2], "");
};
