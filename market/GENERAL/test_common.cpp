#include <market/robotics/cv/library/cpp/detector/common.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

class TCommonDetectorTest: public TTestBase {
    UNIT_TEST_SUITE(TCommonDetectorTest);

    UNIT_TEST(NmsTest);
    UNIT_TEST(BboxIntersectionTest);

    UNIT_TEST_SUITE_END();

public:
    void NmsTest() {
        TVector<TDetection> boxes = {
            TDetection({0, 0, 10, 10}, 0.5), // Base rectangle
            TDetection({0, 0, 5, 5}, 0.5),   // 5x5 = 25 - should stay
            TDetection({1, 1, 9, 9}, 0.5),   // 9x9 = 81 - should leave
            TDetection({9, 9, 1, 1}, 0.5)    // 1x1 = 1 - should stay
        };
        TVector<size_t> correctBoxesIndexes = {0, 1, 3};
        TVector<TDetection> output1 = Nms(boxes, 0.6, 5);
        UNIT_ASSERT(output1.size() == correctBoxesIndexes.size());
        float delta = 1.0;
        for (size_t idx = 0; idx < correctBoxesIndexes.size(); idx++) {
            auto trueBox = boxes.at(correctBoxesIndexes.at(idx)).Box;
            auto nmsBox = output1.at(idx).Box;
            UNIT_ASSERT_DOUBLES_EQUAL(trueBox.Top, nmsBox.Top, delta);
            UNIT_ASSERT_DOUBLES_EQUAL(trueBox.Left, nmsBox.Left, delta);
            UNIT_ASSERT_DOUBLES_EQUAL(trueBox.Width, nmsBox.Width, delta);
            UNIT_ASSERT_DOUBLES_EQUAL(trueBox.Height, nmsBox.Height, delta);
        }
        TVector<TDetection> output2 = Nms(boxes, 0.9, 5);
        UNIT_ASSERT(output2.size() == boxes.size());
        TVector<TDetection> output3 = Nms(boxes, 0.9, 1);
        UNIT_ASSERT(output3.size() == 1);
        auto trueBox0 = boxes.at(correctBoxesIndexes.at(0)).Box;
        auto nmsBox0 = output3.at(0).Box;
        UNIT_ASSERT_DOUBLES_EQUAL(trueBox0.Top, nmsBox0.Top, delta);
        UNIT_ASSERT_DOUBLES_EQUAL(trueBox0.Left, nmsBox0.Left, delta);
        UNIT_ASSERT_DOUBLES_EQUAL(trueBox0.Width, nmsBox0.Width, delta);
        UNIT_ASSERT_DOUBLES_EQUAL(trueBox0.Height, nmsBox0.Height, delta);
    }

    void BboxIntersectionTest() {
        //        NBBoxDetection::TBBox GetBboxIntersection(const NBBoxDetection::TBBox& lhs, const NBBoxDetection::TBBox& other);
        auto bbox = GetBboxIntersection({0, 0, 10, 10}, {9, 9, 20, 20});
        UNIT_ASSERT(bbox.Top = 9);
        UNIT_ASSERT(bbox.Left = 9);
        UNIT_ASSERT(bbox.Width = 1);
        UNIT_ASSERT(bbox.Height = 1);
    }

    void RescaleResultBackTest() {
        TDetectorOutput out1({{0, 0, 10, 10}, 0.5});
        TDetectorOutput out2({{10, 10, 20, 20}, 0.5});
        TVector<TDetectorOutput> inputs = {out1, out2};
        TVector<TDetectorOutput> outputs = {out1, out2};
        TImage image;
        image.LoadImage("./test_image.jpeg");
        UNIT_ASSERT(image.IsValid());
        float scaleX = 2.0;
        float scaleY = 2.0;
        RescaleResultBack(image, scaleX, scaleY, outputs);
        UNIT_ASSERT(outputs.size() == inputs.size());
        float delta = 0.01;
        for (size_t i = 0; i < outputs.size(); ++i) {
            auto out = outputs.at(i);
            auto in = inputs.at(i);
            UNIT_ASSERT(out.IsValid());
            UNIT_ASSERT_DOUBLES_EQUAL(in.Detection.Box.Top * scaleX, out.Detection.Box.Top, delta);
            UNIT_ASSERT_DOUBLES_EQUAL(in.Detection.Box.Left * scaleY, out.Detection.Box.Left, delta);
            UNIT_ASSERT_DOUBLES_EQUAL(in.Detection.Box.Width * scaleX, out.Detection.Box.Width, delta);
            UNIT_ASSERT_DOUBLES_EQUAL(in.Detection.Box.Height * scaleY, out.Detection.Box.Height, delta);
        }
    }
};

UNIT_TEST_SUITE_REGISTRATION(TCommonDetectorTest);
