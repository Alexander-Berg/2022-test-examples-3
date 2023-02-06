#include "helper.h"
#include <market/robotics/cv/library/cpp/types/result_value.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

class TResultValueTest: public TTestBase {
    UNIT_TEST_SUITE(TResultValueTest);

    UNIT_TEST(ResultValueEmptyConstructorTest);
    UNIT_TEST(ResultErrorConstructorsTest);

    UNIT_TEST_SUITE_END();

public:
    void ResultValueEmptyConstructorTest() {
        TResultValue<EErrorExample, int> result;
        UNIT_ASSERT(result.IsOk());
        UNIT_ASSERT(result.What() == "Ok");
    }

    void ResultErrorConstructorsTest() {
        TResultValue<EErrorExample, int> result1(EErrorExample::InternalError);
        UNIT_ASSERT(!result1.IsOk());
        UNIT_ASSERT_EXCEPTION(result1.GetValue(), yexception);

        TResultValue<EErrorExample, double> result2(EErrorExample::Ok, 0.1234);
        UNIT_ASSERT(result2.IsOk());
        UNIT_ASSERT(result2.GetValue() == 0.1234);
    }
};

UNIT_TEST_SUITE_REGISTRATION(TResultValueTest);
