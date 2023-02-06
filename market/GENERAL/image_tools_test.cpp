#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/logger/global/global.h>
#include <util/generic/string.h>
#include <cv/library/imgcore/simple_image/image_ops.h>

#include <market/robotics/cv/library/cpp/util/image_procesing.h>
#include <market/robotics/cv/library/cpp/util/test_utils.h>

class TCVToolsTest: public TTestBase {
    UNIT_TEST_SUITE(TCVToolsTest);

    UNIT_TEST(RGBToGrayscaleTest);

    UNIT_TEST_SUITE_END();

public:
    void RGBToGrayscaleTest() {
        TString filepath = GetPathToFile("opencv_example.jpeg");
        NWarehouseSDK::TImage image;
        bool isOpened = image.LoadImage(filepath);
        if (isOpened) {
            TStackAllocator<TSystemPageAllocator> allocator;
            TStackUnrollBarrier barrier(allocator);
            NWarehouseSDK::TGrayscaleImage grayImage;
            NWarehouseSDK::ConvertRGB2Gray(image, grayImage, allocator);
            UNIT_ASSERT(image.GetImage().cols == grayImage.GetWidth());
            UNIT_ASSERT(image.GetImage().rows == grayImage.GetHeight());
        }
    }
};

UNIT_TEST_SUITE_REGISTRATION(TCVToolsTest);
