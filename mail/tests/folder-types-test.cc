#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/folder.h>
#include <macs/test_environment/rands.h>

namespace macs {
    inline std::ostream & operator << (std::ostream & s, const Folder::Type& type) {
        return s << "type {" << type.code() << ", " << type.title() << "}";
    }
}

namespace {
    using namespace ::testing;
    using namespace macs;

    TEST(FolderTypesTest, getByTitle_withEmptyString_returnsTypeUser) {
        ASSERT_EQ(Folder::Type::user, Folder::Type::getByTitle(""));
    }

    TEST(FolderTypesTest, getByTitle_withNonExistingTitle_returnsTypeUser) {
        ASSERT_EQ(Folder::Type::user, Folder::Type::getByTitle(Rands::getString(1, 64)));
    }

    TEST(FolderTypesTest, getByTitle_withExistingTitle_returnsCorrectType) {
        ASSERT_EQ(Folder::Type::system, Folder::Type::getByTitle("system"));
    }

    TEST(FolderTypesTest, getByCode_withNonExistingCode_returnsTypeUser) {
        ASSERT_EQ(Folder::Type::user, Folder::Type::getByCode(666));
    }
}
