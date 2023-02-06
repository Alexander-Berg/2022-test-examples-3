#include <gtest/gtest.h>
#include "../../src/api/util.h"

TEST(AddressSyntax, check_address) {
    EXPECT_TRUE(furita::check_address("example@ya.ru"));
}
