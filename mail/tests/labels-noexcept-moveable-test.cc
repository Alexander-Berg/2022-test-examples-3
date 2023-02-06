#include <gtest/gtest.h>
#include <macs/label.h>

namespace {
    TEST(LabelsNoexceptMoveTest, labelIsNoexceptMoveable) {
        static_assert(
            std::is_nothrow_move_constructible<macs::Label>::value,
            "macs::Label should be noexcept move constructible"
        );
    }
}
