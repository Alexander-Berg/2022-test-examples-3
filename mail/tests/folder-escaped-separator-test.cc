#include <gtest/gtest.h>
#include <macs/folder.h>
#include <macs/folder_factory.h>

namespace {

using namespace ::testing;
using namespace macs;

using Nodes = std::vector<std::string>;

TEST(FolderEscapedSeparatorTest, escapeFolderName_should_escape_separator_symbol) {
    EXPECT_EQ(escapeFolderName("folder/name//with/separators", "/"), "folder//name////with//separators");
}

TEST(FolderFactoryNameTest, FolderFactory_name_should_not_escape_separator_in_folder_name) {
    const auto folder = macs::FolderFactory().name("folder|name||with|separators").product();
    EXPECT_EQ(folder.name(), "folder|name||with|separators");
}

struct FolderEscapedSeparatorTest
        : public ::testing::TestWithParam<std::pair<std::string, Nodes>> {
};

INSTANTIATE_TEST_SUITE_P(parse_and_join_should_work_equally, FolderEscapedSeparatorTest, ::testing::Values(
    std::make_pair(std::string(""), Nodes{}),
    std::make_pair(std::string("name"), Nodes{"name"}),
    std::make_pair(std::string("name|"), Nodes{"name", ""}),
    std::make_pair(std::string("|name"), Nodes{"", "name"}),
    std::make_pair(std::string("name||"), Nodes{"name|"}),
    std::make_pair(std::string("||name"), Nodes{"|name"}),
    std::make_pair(std::string("name|||"), Nodes{"name|", ""}),
    std::make_pair(std::string("|||name"), Nodes{"|", "name"}),
    std::make_pair(std::string("path|name"), Nodes{"path", "name"}),
    std::make_pair(std::string("path||name"), Nodes{"path|name"}),
    std::make_pair(std::string("path|||name"), Nodes{"path|", "name"}),
    std::make_pair(std::string("p|a|t|h|name"), Nodes{"p", "a", "t", "h", "name"}),
    std::make_pair(std::string("p||a|t||h|name"), Nodes{"p|a", "t|h", "name"})
));

TEST_P(FolderEscapedSeparatorTest, parse_and_join_should_work_equally) {
    const auto [path, nodes] = GetParam();
    EXPECT_EQ(parseFolderPath(path, "|"), nodes);
    EXPECT_EQ(joinFolderPath(nodes, "|"), path);
}

} // namespace
