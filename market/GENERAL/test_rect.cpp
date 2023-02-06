#include <market/robotics/cv/library/cpp/tracking/filtration/rect.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

class TTrackingRectTest: public TTestBase {
    UNIT_TEST_SUITE(TTrackingRectTest);

    UNIT_TEST(ConstructorTest);
    UNIT_TEST(RectExistsTest);

    UNIT_TEST_SUITE_END();

public:
    void ConstructorTest() {
        constexpr auto rect = TTrackingRect(0, 1, 2, 3);

        UNIT_ASSERT_EQUAL(0, rect.X);
        UNIT_ASSERT_EQUAL(1, rect.Y);
        UNIT_ASSERT_EQUAL(2, rect.Width);
        UNIT_ASSERT_EQUAL(3, rect.Height);
    }

    void RectExistsTest() {
        for (const auto value : {0.f, -100.0f}) {
            InvalidPositiveValueTest(value);
        }

        for (const auto value : {
                 std::numeric_limits<float>::quiet_NaN(), std::numeric_limits<float>::signaling_NaN(),
                 std::numeric_limits<float>::infinity(), -std::numeric_limits<float>::infinity()}) {
            InvalidValueTest(value);
        }

        UNIT_ASSERT(TTrackingRect(1, 1, 1, 1).IsExists());
    }

private:
    void InvalidPositiveValueTest(const float value) {
        UNIT_ASSERT(!TTrackingRect(1, 1, value, 1).IsExists());
        UNIT_ASSERT(!TTrackingRect(1, 1, 1, value).IsExists());
        UNIT_ASSERT(!TTrackingRect(1, 1, value, value).IsExists());
    }

    void InvalidValueTest(const float value) {
        InvalidPositiveValueTest(value);

        UNIT_ASSERT(!TTrackingRect(value, 1, 1, 1).IsExists());
        UNIT_ASSERT(!TTrackingRect(1, value, 1, 1).IsExists());
        UNIT_ASSERT(!TTrackingRect(value, value, 1, 1).IsExists());
        UNIT_ASSERT(!TTrackingRect(value, value, value, value).IsExists());
    }
};

UNIT_TEST_SUITE_REGISTRATION(TTrackingRectTest);
