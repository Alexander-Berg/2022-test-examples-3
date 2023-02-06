#include <gtest/gtest.h>
#include <mail/unistat/cpp/include/meters/common.h>

using namespace ::testing;
using namespace ::unistat;

TEST(withSigoptSuffix, shouldReturnOnlyUnderscoreForEmptyValueAndSuffix) {
    EXPECT_EQ(withSigoptSuffix("", ""), "_");
}

TEST(withSigoptSuffix, shouldAddUnderscoreForPrefixWithoutIt) {
    EXPECT_EQ(withSigoptSuffix("prefix", ""), "prefix_");
}

TEST(withSigoptSuffix, shouldNotAddUnderscoreForPrefixWithIt) {
    EXPECT_EQ(withSigoptSuffix("prefix_", ""), "prefix_");
}

TEST(withSigoptSuffix, shouldAddUnderscoreForSuffixWithoutIt) {
    EXPECT_EQ(withSigoptSuffix("", "suffix"), "_suffix");
}

TEST(withSigoptSuffix, shouldNotAddUnderscoreForSuffixWithIt) {
    EXPECT_EQ(withSigoptSuffix("", "_suffix"), "_suffix");
}

TEST(withSigoptSuffix, shouldAddUnderscoreBetweenPrefixAndSuffixWithoutIt) {
    EXPECT_EQ(withSigoptSuffix("prefix", "suffix"), "prefix_suffix");
}

TEST(withSigoptSuffix, shouldNotAddUnderscoreBetweenPrefixAndSuffixIfAnyHasIt) {
    EXPECT_EQ(withSigoptSuffix("prefix_", "suffix"), "prefix_suffix");
    EXPECT_EQ(withSigoptSuffix("prefix", "_suffix"), "prefix_suffix");
}


TEST(buildSignalName, shouldReturnEmptyStringForEmptyNameAndEmptyEndpoint) {
    EXPECT_EQ(buildSignalName("", ""), "");
}

TEST(buildSignalName, shouldReturnNameIfEndpointEmpty) {
    EXPECT_EQ(buildSignalName("name", ""), "name");
}

TEST(buildSignalName, shouldReturnNameUnderscoreEndpointIfNameAndEndpointNotEmpty) {
    EXPECT_EQ(buildSignalName("name", "endpoint"), "name_endpoint");
}

TEST(buildSignalName, shouldReturnUnderscoreEndpointIfNameEmpty) {
    EXPECT_EQ(buildSignalName("", "endpoint"), "_endpoint");
}


TEST(normalizeName, shouldConvertToLowerCase) {
    EXPECT_EQ(normalizeName("ABACABA"), "abacaba");
}

TEST(normalizeName, shouldReplaceIllformedSymbolsToUnderscore) {
    EXPECT_EQ(normalizeName("a=b a'c'a b:a"), "a_b_a_c_a_b_a");
}

TEST(normalizeName, shouldReplaceAdjacentIllformedSymbolsToExactlyOneUnderscore) {
    EXPECT_EQ(normalizeName("b::a"), "b_a");
}

TEST(normalizeName, shouldReturnOnlyLast128Symbols) {
    const std::string expected(128, '!');
    const std::string in = "42" + expected;
    EXPECT_EQ(normalizeName(in), expected);
}


TEST(CountByHttpStatus, shouldHasZeroValueJustAfterInit) {
    CountByHttpStatus meter("endpoint", "prefix");
    EXPECT_TRUE(meter.get().empty());
}

struct CountByHttpStatusWithVariousStatuses : public ::testing::TestWithParam<long> {};

INSTANTIATE_TEST_SUITE_P(shouldCountPassedStatusOnly, CountByHttpStatusWithVariousStatuses, Values(
        100, 200, 499, 505
));

TEST_P(CountByHttpStatusWithVariousStatuses, shouldCountPassedStatusOnly) {
    CountByHttpStatus meter("endpoint", "prefix");

    const long status = GetParam();
    meter.update(status);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{{"prefix_endpoint_" + std::to_string(status) + "_summ", 1}}));
}

TEST(CountByHttpStatus, shouldCountDifferentStatusesAtTheSameTime) {
    CountByHttpStatus meter("endpoint", "prefix");

    meter.update(200);
    meter.update(499);
    meter.update(504);

    const std::vector<std::tuple<std::string, std::size_t>> expected {
        {"prefix_endpoint_200_summ", 1},
        {"prefix_endpoint_499_summ", 1},
        {"prefix_endpoint_504_summ", 1}
    };

    EXPECT_EQ(meter.get(), expected);
}


TEST(CountSubstring, shouldHasZeroValueJustAfterInit) {
    CountSubstring meter("pgg::database::fallback::rules::Replica");
    EXPECT_EQ(meter.get(), (NamedValue<std::size_t>{"pgg_database_fallback_rules_replica_summ", 0}));
}

struct CountSubstringUpdateTest : public TestWithParam<std::tuple<std::string, std::string>> {
    inline static const std::string suitable = "ololo\tpgg::database::fallback::rules::Replica\nkek";
    inline static const std::string unsuitable = "ololo\tpgg::database::callback::rules::Replica\nkek";
};

TEST_P(CountSubstringUpdateTest, testUpdateWithDifferentStrings) {
    CountSubstring meter("pgg::database::fallback::rules::Replica");
    meter.update({{"message", std::get<0>(GetParam())}, {"error_code.message", std::get<1>(GetParam())}});
    const bool expected = (std::get<0>(GetParam()) == suitable) || (std::get<1>(GetParam()) == suitable);
    EXPECT_EQ(meter.get(), (NamedValue<std::size_t>{"pgg_database_fallback_rules_replica_summ", expected}));
}

INSTANTIATE_TEST_SUITE_P(, CountSubstringUpdateTest, Combine(
        Values("", CountSubstringUpdateTest::suitable, CountSubstringUpdateTest::unsuitable),
        Values("", CountSubstringUpdateTest::suitable, CountSubstringUpdateTest::unsuitable)));

TEST(Hist, shouldHasZeroValueJustAfterInit) {
    Hist<double> meter({0., 10., 100.}, "hist_name");
    EXPECT_EQ(meter.get(), (NamedHist<double>{"hist_name_hgram", {{0, 0}, {10, 0}, {100, 0}, {200, 0}}}));
}

TEST(Hist, shouldNotIncrementForValueLesserThanLeftBound) {
    Hist<double> meter({0., 10., 100.}, "hist_name");
    meter.update(-10);
    EXPECT_EQ(meter.get(), (NamedHist<double>{"hist_name_hgram", {{0, 0}, {10, 0}, {100, 0}, {200, 0}}}));
}

TEST(Hist, bucketBoundsShouldBeHalfOpenInterval) {
    Hist<double> meter({0., 10., 100.}, "hist_name");
    meter.update(10);
    meter.update(100);
    EXPECT_EQ(meter.get(), (NamedHist<double>{"hist_name_hgram", {{0, 0}, {10, 1}, {100, 1}, {200, 0}}}));
}

struct HistWithVariousValues : public TestWithParam<std::pair<double, NamedHist<double>>> {};

INSTANTIATE_TEST_SUITE_P(differentValuesShoundBePlacedInAppropriateBucket, HistWithVariousValues, Values(
        std::pair<double, NamedHist<double>>{0, NamedHist<double>{"hist_name_hgram", {{0, 1}, {10, 0}, {100, 0}, {200, 0}}}},
        std::pair<double, NamedHist<double>>{1, NamedHist<double>{"hist_name_hgram", {{0, 1}, {10, 0}, {100, 0}, {200, 0}}}},
        std::pair<double, NamedHist<double>>{10, NamedHist<double>{"hist_name_hgram", {{0, 0}, {10, 1}, {100, 0}, {200, 0}}}},
        std::pair<double, NamedHist<double>>{50, NamedHist<double>{"hist_name_hgram", {{0, 0}, {10, 1}, {100, 0}, {200, 0}}}},
        std::pair<double, NamedHist<double>>{100, NamedHist<double>{"hist_name_hgram", {{0, 0}, {10, 0}, {100, 1}, {200, 0}}}},
        std::pair<double, NamedHist<double>>{1000, NamedHist<double>{"hist_name_hgram", {{0, 0}, {10, 0}, {100, 1}, {200, 0}}}}
));

TEST_P(HistWithVariousValues, differentValuesShoundBePlacedInAppropriateBucket) {
    Hist<double> meter({0., 10., 100.}, "hist_name");
    const auto params = GetParam();
    meter.update(params.first);
    EXPECT_EQ(meter.get(), params.second);
}

TEST(Hist, shouldThrowExceptionOnConstructionWithUnsortedBuckets) {
    EXPECT_THROW(Hist<std::size_t> meter({0, 101, 100}, "hist_name"), std::invalid_argument);
}

TEST(Hist, shouldThrowExceptionOnConstructionWithNotUniqueBucketBounds) {
    EXPECT_THROW(Hist<std::size_t> meter({0, 100, 100}, "hist_name"), std::invalid_argument);
}

TEST(Hist, shouldThrowExceptionOnConstructionWithEmptyVectorOfBucketsBounds) {
    EXPECT_THROW(Hist<std::size_t> meter({}, "hist_name"), std::invalid_argument);
}
