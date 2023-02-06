#include <market/robotics/cv/library/cpp/detector/types/detector_output.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

class TDetectorOutputTest: public TTestBase {
    UNIT_TEST_SUITE(TDetectorOutputTest);

    UNIT_TEST(DetectorOutputEmptyConstructorTest);
    UNIT_TEST(DetectorOutputConstructorTest);

    UNIT_TEST_SUITE_END();

public:
    void DetectorOutputEmptyConstructorTest() {
        TDetectorOutput output;
        UNIT_ASSERT(!output.IsValid());
    }

    void DetectorOutputConstructorTest() {
        TDetection detection({0, 0, 10, 10}, 0.67);
        TImage image;
        bool loadStatus = image.LoadImage("./test_image.jpeg");
        UNIT_ASSERT(loadStatus);
        UNIT_ASSERT(image.IsValid());

        TDetectorOutput output1(detection, image);
        UNIT_ASSERT(output1.IsValid());

        TDetectorOutput output2(std::move(detection));
        UNIT_ASSERT(!output2.IsValid());
        output2.Image = image;
        UNIT_ASSERT(output2.IsValid());
    }
};

UNIT_TEST_SUITE_REGISTRATION(TDetectorOutputTest);
