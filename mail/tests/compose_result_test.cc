#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/core/include/cached_compose_result.h>

namespace sendbernar {

using namespace testing;

TEST(CachedComposeResultTest, shouldUpdateOnlyInSuccessState) {
    CachedComposeResult result;

    EXPECT_EQ(result.value_, ComposeResult::EMPTY);

    result.updateIfNotAlreadyAnError(ComposeResult::DONE);
    EXPECT_EQ(result.value_, ComposeResult::DONE);

    result.updateIfNotAlreadyAnError(ComposeResult::TO_INVALID);
    EXPECT_EQ(result.value_, ComposeResult::TO_INVALID);

    result.updateIfNotAlreadyAnError(ComposeResult::BCC_INVALID);
    EXPECT_EQ(result.value_, ComposeResult::TO_INVALID);
}

}
