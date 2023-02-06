#include <gtest/gtest.h>
#include <macs/folder.h>
#include <boost/range/join.hpp>
#include "throw-wmi-helper.h"

namespace {

using namespace ::testing;
using namespace macs;

using PathValue = std::vector<Folder::Name>;

TEST(FolderPathTest, construct_should_succeed) {
    EXPECT_NO_THROW(Folder::Path path(PathValue{"a"}));
}

TEST(FolderPathTest, operator_eq_should_return_true_for_equal) {
    const Folder::Path lhs(PathValue{"a"});
    const Folder::Path rhs(PathValue{"a"});
    EXPECT_TRUE(lhs == rhs);
}

TEST(FolderPathTest, operator_eq_should_return_false_for_not_equal) {
    const Folder::Path lhs(PathValue{"lhs"});
    const Folder::Path rhs(PathValue{"rhs"});
    EXPECT_FALSE(lhs == rhs);
}

TEST(FolderPathTest, construct_should_normalize_names) {
    const Folder::Path path(PathValue{" pa ", "  th  "});
    EXPECT_EQ(path, Folder::Path(PathValue{"pa", "th"}));
}

TEST(FolderPathTest, construct_from_inputIterators_should_succeed) {
    const PathValue val{"p", "a", "t", "h"};
    const Folder::Path path(val.begin() + 1, val.end());
    EXPECT_EQ(path, Folder::Path(PathValue{"a", "t", "h"}));
}

TEST(FolderPathTest, construct_from_range_should_succeed) {
    const PathValue left{"p", "a"};
    const PathValue right{"t", "h"};
    const Folder::Path path(boost::range::join(left, right));
    EXPECT_EQ(path, Folder::Path(PathValue{"p", "a", "t", "h"}));
}

TEST(FolderPathTest, toString_should_return_escaped_full_folder_name_with_separators) {
    const Folder::Path path(PathValue{"p", "a", "t", "h|s"});
    EXPECT_EQ(path.toString(), "p|a|t|h||s");
}

} // namespace
