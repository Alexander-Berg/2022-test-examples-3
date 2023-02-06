#include "helper.h"
#include <market/robotics/cv/library/cpp/types/result.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

class TResultTest: public TTestBase {
    UNIT_TEST_SUITE(TResultTest);

    UNIT_TEST(ResultEmptyConstructorTest);
    UNIT_TEST(ResultErrorConstructorTest);

    UNIT_TEST_SUITE_END();

public:
    void ResultEmptyConstructorTest() {
        TResult<EErrorExample> result;
        UNIT_ASSERT(result.IsOk());
        UNIT_ASSERT(result.What() == "Ok");
    }

    void ResultErrorConstructorTest() {
        TResult<EErrorExample> result(EErrorExample::InternalError);
        UNIT_ASSERT(!result.IsOk());
    }
};

UNIT_TEST_SUITE_REGISTRATION(TResultTest);
