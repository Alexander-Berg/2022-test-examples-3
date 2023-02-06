#include <market/idx/delivery/lib/options/options.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NDelivery;


TEST(TestOption, ValidateOptionCost) {
    ASSERT_TRUE(ValidateOptionCost(100));
    ASSERT_TRUE(ValidateOptionCost(0));
    ASSERT_FALSE(ValidateOptionCost(-10));
}

TEST(TestOption, ValidateOptionDays) {
    ASSERT_TRUE(ValidateOptionDays(10, 10));
    ASSERT_TRUE(ValidateOptionDays(9, 10));
    ASSERT_FALSE(ValidateOptionDays(10, 9));
}

TEST(TestOption, ValidateOptionDaysRange) {
    ASSERT_TRUE(ValidateOptionDaysRange(1, 1));
    ASSERT_TRUE(ValidateOptionDaysRange(1, 2));
    ASSERT_FALSE(ValidateOptionDaysRange(1, 10));
}

TEST(TestOption, ValidateProto) {
    {
        // empty proto
        TDeliveryOption opt;
        ASSERT_TRUE(ValidateOption(opt, 31) == EOptionValidationStatus::ERR_COST_VALIDATION);
    }
    {
        // good delivery option
        TDeliveryOption opt;
        opt.SetCost(10);
        opt.SetDaysMin(2);
        opt.SetDaysMax(3);
        ASSERT_TRUE(ValidateOption(opt, 31) == EOptionValidationStatus::OK);
    }
    {
        // cost < 0
        TDeliveryOption opt;
        opt.SetCost(-10);
        ASSERT_TRUE(ValidateOption(opt, 31) == EOptionValidationStatus::ERR_COST_VALIDATION);
    }
    {
        // daysMin > daysMax
        TDeliveryOption opt;
        opt.SetCost(10);
        opt.SetDaysMin(12);
        opt.SetDaysMax(10);
        ASSERT_TRUE(ValidateOption(opt, 31) == EOptionValidationStatus::ERR_MIN_MAX_DAYS_CONFLICT);
    }
    {
        // daysMax - daysMin > MaxDaysRange
        TDeliveryOption opt;
        opt.SetCost(10);
        opt.SetDaysMin(10);
        opt.SetDaysMax(20);
        ASSERT_TRUE(ValidateOption(opt, 31) == EOptionValidationStatus::WARN_TOO_LARGE_RANGE);
    }
    {
        // daysMax > DaysUnknownThreshold
        TDeliveryOption opt;
        opt.SetCost(10);
        opt.SetDaysMin(30);
        opt.SetDaysMax(32);
        ASSERT_TRUE(ValidateOption(opt, 31) == EOptionValidationStatus::OK);
        ASSERT_TRUE(opt.GetDaysMin() == DaysUnknown);
        ASSERT_TRUE(opt.GetDaysMax() == DaysUnknown);
    }
}
