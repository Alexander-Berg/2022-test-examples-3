#include "test_utils.h"
#include <market/idx/feeds/qparser/lib/util/time_units.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;
TEST(TimeUtils, ParseDurationPositive) {
    TStringBuilder error;
    Market::DataCamp::Duration duration;
    {
        duration.Clear();
        error.clear();
        ASSERT_TRUE(TryParseTimeUnit("11 дней", duration, error.Out));
        ASSERT_EQ(11, duration.days());
    }
    {
        duration.Clear();
        error.clear();
        ASSERT_TRUE(TryParseTimeUnit("2 дня", duration, error.Out));
        ASSERT_EQ(2, duration.days());
    }
    {
        duration.Clear();
        error.clear();
        ASSERT_TRUE(TryParseTimeUnit("21 месяц", duration, error.Out));
        ASSERT_EQ(21, duration.months());
        ASSERT_EQ(0, duration.days());
    }
    {
        duration.Clear();
        error.clear();
        ASSERT_TRUE(TryParseTimeUnit("21  месяц  ", duration, error.Out));
        ASSERT_EQ(21, duration.months());
        ASSERT_EQ(0, duration.days());
    }
    {
        duration.Clear();
        error.clear();
        ASSERT_TRUE(TryParseTimeUnit("22 года", duration, error.Out));
        ASSERT_EQ(22, duration.years());
        ASSERT_EQ(0, duration.months());
    }
}

TEST(TimeUtils, ParseDurationNegative) {
    TStringBuilder error;
    Market::DataCamp::Duration duration;
    {
        duration.Clear();
        error.clear();
        ASSERT_FALSE(TryParseTimeUnit("-11 дней", duration, error.Out));
        UNIT_ASSERT_STRING_CONTAINS(error, "Невозможно преобразовать");
        ASSERT_FALSE( duration.has_days());
    }
    {
        duration.Clear();
        error.clear();
        ASSERT_FALSE(TryParseTimeUnit("0 дней", duration, error.Out));
        UNIT_ASSERT_STRING_CONTAINS(error, "Невозможно преобразовать");
        ASSERT_FALSE( duration.has_days());
    }
    {
        duration.Clear();
        error.clear();
        ASSERT_FALSE(TryParseTimeUnit("5 днеееей", duration, error.Out));
        UNIT_ASSERT_STRING_CONTAINS(error, "Неизвестная единица");
        ASSERT_FALSE( duration.has_days());
    }
}
