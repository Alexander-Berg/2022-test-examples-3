#include <market/robotics/cv/library/cpp/detector/plt_detector.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

class TPLTDetectorTest: public TTestBase {
    UNIT_TEST_SUITE(TPLTDetectorTest);

    UNIT_TEST(DetectorCreationTest);
    UNIT_TEST(DetectorDetectTest);

    UNIT_TEST_SUITE_END();

public:
    void DetectorCreationTest() {
        // reading config and initializing network
        auto detector = TPLTDetector::Create("./");
        UNIT_ASSERT(detector != nullptr);
    }

    void DetectorDetectTest() {
        // create detector, read image, detect labels on image
        auto detector = TPLTDetector::Create("./");
        TImage image;
        image.LoadImage("./test_image.jpeg");
        UNIT_ASSERT(image.IsValid());
        auto result = detector->Detect(image);
        UNIT_ASSERT(result.IsOk());
        UNIT_ASSERT(result.GetValue().size() == 1);
        UNIT_ASSERT(result.GetValue()[0].IsValid());
    }
};

UNIT_TEST_SUITE_REGISTRATION(TPLTDetectorTest);
