#include <market/robotics/cv/library/cpp/detector/types/detection.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

class TDetectionTest: public TTestBase {
    UNIT_TEST_SUITE(TDetectionTest);

    UNIT_TEST(DetectionClassEmptyConstructorTest);
    UNIT_TEST(DetectionClassConstructorTest);

    UNIT_TEST_SUITE_END();

public:
    void DetectionClassEmptyConstructorTest() {
        TDetection detection;
        UNIT_ASSERT(!detection.IsValid());
    }

    void DetectionClassConstructorTest() {
        NBBoxDetection::TBBox box(0, 0, 10, 10);
        TDetection detection(box, 0.67);
        UNIT_ASSERT(detection.IsValid());
    }
};

UNIT_TEST_SUITE_REGISTRATION(TDetectionTest);
