#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/weighted_random_get.h>

namespace {

using namespace testing;
using namespace sharpei;

struct GetRandomByWeightTest : Test {
    std::minstd_rand generator {0};
};

TEST_F(GetRandomByWeightTest, for_empty_range_should_return_end) {
    const std::vector<std::pair<std::string, int>> range;
    EXPECT_EQ(getRandomByWeight(range, generator), range.end());
}

TEST_F(GetRandomByWeightTest, for_range_with_one_element_should_return_begin) {
    const std::vector<std::pair<std::string, int>> range {{"a", 1}};
    EXPECT_EQ(getRandomByWeight(range, generator), range.begin());
}

TEST_F(GetRandomByWeightTest, for_range_with_one_zero_weight_element_should_return_end) {
    const std::vector<std::pair<std::string, int>> range {{"a", 0}};
    EXPECT_EQ(getRandomByWeight(range, generator), range.end());
}

TEST_F(GetRandomByWeightTest, for_range_with_many_elements_after_many_iterations_element_sampling_fraction_should_be_near_theoretical_probability) {
    const std::size_t iterations = 100000;
    const std::vector<std::pair<std::string, int>> range({{"a", 0}, {"b", 1}, {"c", 2}, {"d", 3}, {"e", 4}});
    const auto weight_sum = std::accumulate(range.begin(), range.end(), 0, [] (auto r, const auto& v) { return r + v.second; });
    std::vector<std::size_t> counter(range.size());
    for (std::size_t i = 0; i < iterations; ++i) {
        ++counter[getRandomByWeight(range, generator) - range.begin()];
    }

    for (std::size_t i = 0; i < range.size(); ++i) {
        EXPECT_NEAR(double(range[i].second) / double(weight_sum), double(counter[i]) / double(iterations), 1e-2);
    }
}

} // namespace
