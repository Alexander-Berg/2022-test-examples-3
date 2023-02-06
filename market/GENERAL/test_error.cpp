#include "helper.h"
#include <market/robotics/cv/library/cpp/types/error.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

class TErrorTest: public TTestBase {
    UNIT_TEST_SUITE(TErrorTest);

    UNIT_TEST(ErrorTraitsIsOKTest);
    UNIT_TEST(ErrorTraitsToStringTest);

    UNIT_TEST_SUITE_END();

public:
    void ErrorTraitsIsOKTest() {
        UNIT_ASSERT(TErrorTraitsForTests::IsOk(EErrorExample::Ok));
        UNIT_ASSERT(!TErrorTraitsForTests::IsOk(EErrorExample::InternalError));
    }

    void ErrorTraitsToStringTest() {
        UNIT_ASSERT(TErrorTraitsForTests::ToString(EErrorExample::Ok) == "Ok");
        UNIT_ASSERT(TErrorTraitsForTests::ToString(EErrorExample::InternalError) == "Unknown error");
    }
};

UNIT_TEST_SUITE_REGISTRATION(TErrorTest);
