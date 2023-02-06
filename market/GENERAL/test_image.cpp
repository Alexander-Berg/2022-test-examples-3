#include <market/robotics/cv/library/cpp/types/image.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

class TImageTest: public TTestBase {
    UNIT_TEST_SUITE(TImageTest);

    UNIT_TEST(ImageCommonTest);

    UNIT_TEST_SUITE_END();

public:
    void ImageCommonTest() {
        TImage image;
        bool loadStatus = image.LoadImage("./test_image.jpeg");
        UNIT_ASSERT(loadStatus);
        UNIT_ASSERT(image.IsValid());
        UNIT_ASSERT(!image.IsNull());
        UNIT_ASSERT(image.GetHeight() == 1920);
        UNIT_ASSERT(image.GetWidth() == 1080);
        UNIT_ASSERT(image.GetBox().IsValid());
    }
};

UNIT_TEST_SUITE_REGISTRATION(TImageTest);
