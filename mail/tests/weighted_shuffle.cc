#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/weighted_shuffle.h>

namespace {

using namespace testing;
using namespace sharpei;

using Range = std::vector<std::pair<std::string, int>>;
using DoubleRange = std::vector<std::pair<std::string, double>>;

struct WeightedShuffleTest : Test {
    std::minstd_rand generator {0};
};

TEST_F(WeightedShuffleTest, for_empty_range_should_return_same) {
    Range range;
    EXPECT_EQ(weightedShuffle(range.begin(), range.end(), generator), std::vector<std::size_t>());
}

TEST_F(WeightedShuffleTest, for_range_with_one_element_should_return_same) {
    Range range({{"a", 1}});
    EXPECT_EQ(weightedShuffle(range.begin(), range.end(), generator), std::vector<std::size_t>({0}));
}

TEST_F(WeightedShuffleTest, for_range_with_one_zero_weight_element_should_return_same) {
    Range range({{"a", 0}});
    EXPECT_EQ(weightedShuffle(range.begin(), range.end(), generator), std::vector<std::size_t>({0}));
}

TEST_F(WeightedShuffleTest, for_range_with_one_negative_weight_element_should_return_same) {
    Range range({{"a", -1}});
    EXPECT_EQ(weightedShuffle(range.begin(), range.end(), generator), std::vector<std::size_t>({0}));
}

TEST_F(WeightedShuffleTest, for_range_with_many_elements_after_many_iterations_sampling_fraction_of_element_at_first_position_should_be_near_theoretical_probability) {
    const std::size_t iterations = 100000;
    Range range({{"a", 1}, {"b", 2}, {"c", 3}, {"d", 4}, {"d", 5}});
    std::sort(range.begin(), range.end(), [] (const auto& lhs, const auto& rhs) { return lhs.second > rhs.second; });
    const auto weight_sum = std::accumulate(range.begin(), range.end(), 0, [] (auto r, const auto& v) { return r + v.second; });
    std::vector<std::size_t> counter(range.size());
    for (std::size_t i = 0; i < iterations; ++i) {
        const auto result = weightedShuffle(range.begin(), range.end(), generator);
        ++counter[result.front()];
    }
    for (std::size_t element = 0; element < range.size(); ++element) {
        EXPECT_NEAR(double(range[element].second) / double(weight_sum), double(counter[element]) / double(iterations), 1e-2);
    }
}

TEST_F(WeightedShuffleTest, element_with_zero_weight_should_be_always_after_elements_with_greater_weight) {
    const std::size_t iterations = 100000;
    Range range({{"a", 0}, {"b", 1}, {"c", 2}});
    std::vector<std::size_t> counter(range.size());
    for (std::size_t i = 0; i < iterations; ++i) {
        const auto result = weightedShuffle(range.begin(), range.end(), generator);
        ++counter[result.back()];
    }
    EXPECT_EQ(counter.front(), iterations);
}

TEST_F(WeightedShuffleTest, element_with_negative_weight_should_be_always_after_elements_with_greater_weight) {
    const std::size_t iterations = 100000;
    Range range({{"a", -1}, {"b", 0}, {"c", 1}});
    std::vector<std::size_t> counter(range.size());
    for (std::size_t i = 0; i < iterations; ++i) {
        const auto result = weightedShuffle(range.begin(), range.end(), generator);
        ++counter[result.back()];
    }
    EXPECT_EQ(counter.front(), iterations);
}

TEST_F(WeightedShuffleTest, element_with_zero_weight_should_be_always_after_elements_with_greater_less_than_one_weight) {
    const std::size_t iterations = 100000;
    DoubleRange range({{"a", 0}, {"b", 0.1}, {"c", 0.2}});
    std::vector<std::size_t> counter(range.size());
    for (std::size_t i = 0; i < iterations; ++i) {
        const auto result = weightedShuffle(range.begin(), range.end(), generator);
        ++counter[result.back()];
    }
    EXPECT_EQ(counter.front(), iterations);
}

TEST_F(WeightedShuffleTest, element_with_negative_less_than_one_weight_should_be_always_after_elements_with_greater_weight) {
    const std::size_t iterations = 100000;
    DoubleRange range({{"a", -0.1}, {"b", 0}, {"c", 0.1}});
    std::vector<std::size_t> counter(range.size());
    for (std::size_t i = 0; i < iterations; ++i) {
        const auto result = weightedShuffle(range.begin(), range.end(), generator);
        ++counter[result.back()];
    }
    EXPECT_EQ(counter.front(), iterations);
}

} // namespace
