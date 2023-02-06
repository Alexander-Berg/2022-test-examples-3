#include <gtest/gtest.h>
#include <macs/folder.h>

namespace {
    TEST(FoldersNoexceptMoveTest, folderIsNoexceptMoveable) {
        static_assert(
            std::is_nothrow_move_constructible<macs::Folder>::value,
            "macs::Folder should be noexcept move constructible"
        );
    }
}
