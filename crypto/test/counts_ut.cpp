#include <library/cpp/testing/unittest/registar.h>
#include <crypta/graph/engine/score/native/lib/counts.h>

Y_UNIT_TEST_SUITE(TUnitCountsTest) {
    Y_UNIT_TEST(HistogramScoreTest) {
        NCrypta::NGraphEngine::THistogramCountsScore::Builder histBuilder{};
        histBuilder.LessOrEqualAs(10, 1.);
        histBuilder.AndTheRestAs(0.);
        histBuilder.AndPenalizeEmpty();
        auto hist = histBuilder.Build();

        UNIT_ASSERT_EQUAL(1., hist.GetScore(5));
        UNIT_ASSERT_EQUAL(1., hist.GetScore(10));
        UNIT_ASSERT_EQUAL(0., hist.GetScore(11));
        UNIT_ASSERT_EQUAL(-1., hist.GetScore(0));
    }

    Y_UNIT_TEST(UniformIncreasingDecreasingTest) {
        NCrypta::NGraphEngine::THistogramCountsScore::Builder histBuilder{};
        histBuilder.UniformIncreasingRange(0, 5, 0.7);
        histBuilder.UniformDecreasingRange(5, 8, 0.3);
        histBuilder.AndTheRestAs(0);
        NCrypta::NGraphEngine::THistogramCountsScore hist = histBuilder.Build();

        UNIT_ASSERT_DOUBLES_EQUAL(0.0466, hist.GetScore(1), 0.01);
        UNIT_ASSERT_DOUBLES_EQUAL(0.0933, hist.GetScore(2), 0.01);
        UNIT_ASSERT_DOUBLES_EQUAL(0.1399, hist.GetScore(3), 0.01);
        UNIT_ASSERT_DOUBLES_EQUAL(0.1866, hist.GetScore(4), 0.01);
        UNIT_ASSERT_DOUBLES_EQUAL(0.2333, hist.GetScore(5), 0.01);
        // decreases here!
        UNIT_ASSERT_DOUBLES_EQUAL(0.15, hist.GetScore(6), 0.01);
        UNIT_ASSERT_DOUBLES_EQUAL(0.0999, hist.GetScore(7), 0.01);
        UNIT_ASSERT_DOUBLES_EQUAL(0.0499, hist.GetScore(8), 0.01);
        UNIT_ASSERT_DOUBLES_EQUAL(0, hist.GetScore(9), 0.01);
    }
}
