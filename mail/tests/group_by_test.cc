#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mailbox_oper/group_by.h>


namespace mbox_oper {
using namespace testing;

TEST(GroupByTest, shouldReturnEmpty) {
    EXPECT_TRUE(groupBy(std::vector<int>(), [](int) { return 0; }).empty());
}

std::vector<std::pair<int, int>> elements = {
    std::make_pair(1, 1), std::make_pair(1, 2),
                          std::make_pair(2, 2),
                                                std::make_pair(3, 3)
};

TEST(GroupByTest, shouldGroupByKey) {
    const auto grouped = groupBy(elements, [](const auto& el) { return el.first; });

    EXPECT_EQ(grouped.size(), 3ul);

    EXPECT_THAT(grouped.at(1), UnorderedElementsAre(std::make_pair(1, 1), std::make_pair(1, 2)));
    EXPECT_THAT(grouped.at(2), UnorderedElementsAre(std::make_pair(2, 2)));
    EXPECT_THAT(grouped.at(3), UnorderedElementsAre(std::make_pair(3, 3)));
}

TEST(GroupByTest, shouldGroupByValue) {
    const auto grouped = groupBy(elements, [](const auto& el) { return el.second; });

    EXPECT_EQ(grouped.size(), 3ul);

    EXPECT_THAT(grouped.at(1), UnorderedElementsAre(std::make_pair(1, 1)));
    EXPECT_THAT(grouped.at(2), UnorderedElementsAre(std::make_pair(1, 2), std::make_pair(2, 2)));
    EXPECT_THAT(grouped.at(3), UnorderedElementsAre(std::make_pair(3, 3)));
}

}
