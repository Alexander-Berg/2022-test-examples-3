#include <market/robotics/cv/library/cpp/tracking/filtration/kalman.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

namespace {
    constexpr TTrackingRect GetInvalidRect() noexcept {
        return TTrackingRect(10, 12, 0, 20);
    }

    constexpr TTrackingRect GetValidRect() noexcept {
        return TTrackingRect(10, 12, 20, 20);
    }

    constexpr TTrackingRect GetMovedRect(const TTrackingRect& rect,
                                         const TTrackingRect& deltaRect = TTrackingRect(1, 1, 0, 0)) noexcept {
        return TTrackingRect(
            rect.X + deltaRect.X,
            rect.Y + deltaRect.Y,
            rect.Width + deltaRect.Width,
            rect.Height + deltaRect.Height);
    }

    template <size_t N>
    void EstimateAndCompareWithReference(TKalmanTracker& tracker, const TTrackingRect (&references)[N]) {
        constexpr auto epsilon = 0.001f;
        for (const auto& reference : references) {
            const auto estimation = tracker.Estimate();

            UNIT_ASSERT(estimation.IsExists());
            UNIT_ASSERT_DOUBLES_EQUAL(reference.X, estimation.X, epsilon);
            UNIT_ASSERT_DOUBLES_EQUAL(reference.Y, estimation.Y, epsilon);
            UNIT_ASSERT_DOUBLES_EQUAL(reference.Width, estimation.Width, epsilon);
            UNIT_ASSERT_DOUBLES_EQUAL(reference.Height, estimation.Height, epsilon);
        }
    }
}

class TKalmanTest: public TTestBase {
    UNIT_TEST_SUITE(TKalmanTest);

    UNIT_TEST(ConstructorTest);
    UNIT_TEST(AddTest);
    UNIT_TEST(SimpleEstimateTest);
    UNIT_TEST(EstimationFistRectsTest);
    UNIT_TEST(LongEstimationRectsTest);
    UNIT_TEST(EstimationWithSkip);

    UNIT_TEST_SUITE_END();

public:
    void ConstructorTest() {
        UNIT_ASSERT_EXCEPTION(TKalmanTracker({GetValidRect(), GetInvalidRect()}), std::runtime_error);
        UNIT_ASSERT_EXCEPTION(TKalmanTracker({GetInvalidRect(), GetValidRect()}), std::runtime_error);
        UNIT_ASSERT_EXCEPTION(TKalmanTracker({GetInvalidRect(), GetInvalidRect()}), std::runtime_error);
        UNIT_ASSERT_NO_EXCEPTION(TKalmanTracker({GetValidRect(), GetValidRect()}));
    }

    void AddTest() {
        auto tracker = TKalmanTracker{{GetValidRect(), GetValidRect()}};

        UNIT_ASSERT_EXCEPTION(tracker.Add(GetInvalidRect()), std::runtime_error);
        UNIT_ASSERT_NO_EXCEPTION(tracker.Add(GetValidRect()));
    }

    void SimpleEstimateTest() {
        constexpr auto bbox = GetValidRect();
        constexpr auto deltaRect = TTrackingRect(1, 1, 0, 0);
        constexpr auto movedBbox = GetMovedRect(bbox, deltaRect);
        auto tracker = TKalmanTracker{{bbox, movedBbox}};
        const auto estimation = tracker.Estimate();

        UNIT_ASSERT(estimation.IsExists());
        UNIT_ASSERT_DOUBLES_EQUAL(movedBbox.X, estimation.X, deltaRect.X);
        UNIT_ASSERT_DOUBLES_EQUAL(movedBbox.Y, estimation.Y, deltaRect.Y);
        UNIT_ASSERT_DOUBLES_EQUAL(movedBbox.Width, estimation.Width, deltaRect.Width);
        UNIT_ASSERT_DOUBLES_EQUAL(movedBbox.Height, estimation.Height, deltaRect.Height);
    }

    void EstimationFistRectsTest() {
        constexpr auto detections = std::array<TTrackingRect, 2>{
            TTrackingRect{788, 372, 163, 139}, TTrackingRect{789, 367, 164, 140}};

        constexpr TTrackingRect references[] = {{789.961f, 364.296f, 163.910f, 139.910f},
                                                {790.926f, 361.402f, 163.910f, 139.910f},
                                                {791.891f, 358.508f, 163.910f, 139.910f},
                                                {792.855f, 355.614f, 163.910f, 139.910f},
                                                {793.820f, 352.720f, 163.910f, 139.910f}};

        auto tracker = TKalmanTracker{detections};

        EstimateAndCompareWithReference(tracker, references);
    }

    void LongEstimationRectsTest() {
        constexpr auto detections = std::array<TTrackingRect, 6>{
            TTrackingRect{788, 372, 163, 139}, TTrackingRect{789, 367, 164, 140}, TTrackingRect{789, 370, 165, 140},
            TTrackingRect{790, 371, 167, 139}, TTrackingRect{791, 371, 168, 138}, TTrackingRect{792, 369, 171, 141}};

        constexpr TTrackingRect references[] = {{795.500f, 369.379f, 167.659f, 139.651f},
                                                {797.504f, 368.993f, 167.659f, 139.651f},
                                                {799.509f, 368.607f, 167.659f, 139.651f},
                                                {801.513f, 368.221f, 167.659f, 139.651f},
                                                {803.517f, 367.835f, 167.659f, 139.651f}};

        auto tracker = TKalmanTracker{{detections[0], detections[1]}};

        for (auto idx = 2ul; idx < detections.size(); ++idx)
            tracker.Add(detections[idx]);

        EstimateAndCompareWithReference(tracker, references);
    }

    void EstimationWithSkip() {
        // life applied test, detector each 5 steps

        constexpr auto detections = std::array<TTrackingRect, 2>{
            TTrackingRect{788, 372, 163, 139}, TTrackingRect{789, 367, 164, 140}};

        constexpr size_t skipCount = 3;
        constexpr auto detectionPostSkip = TTrackingRect{792, 369, 171, 141};

        constexpr TTrackingRect references[] = {{795.152f, 369.720f, 167.931f, 140.528f},
                                                {796.779f, 370.257f, 167.931f, 140.528f},
                                                {798.406f, 370.794f, 167.931f, 140.528f},
                                                {800.033f, 371.331f, 167.931f, 140.528f},
                                                {801.660f, 371.868f, 167.931f, 140.528f}};

        auto tracker = TKalmanTracker{detections};

        for (size_t idx = 0; idx < skipCount; ++idx)
            tracker.Estimate();

        tracker.Add(detectionPostSkip);

        EstimateAndCompareWithReference(tracker, references);
    }
};

UNIT_TEST_SUITE_REGISTRATION(TKalmanTest);
