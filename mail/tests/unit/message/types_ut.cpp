#include <gtest/gtest.h>

#include "message/types.h"

using namespace testing;
using namespace NNotSoLiteSrv;

TEST(TMessageTypes, People) {
    EXPECT_EQ(ConvertSoLabelToType("people"), "4");
}

TEST(TMessageTypes, AreCaseSensitive) {
    EXPECT_EQ(ConvertSoLabelToType("PeOpLe"), "");
}

TEST(TMessageTypes, Unknown) {
    EXPECT_EQ(ConvertSoLabelToType("foobarbaz"), "");
}

TEST(TToString, MustConvertSpamResolutionType) {
    EXPECT_EQ("Global", ToString(ESpamResolutionType::Global));
    EXPECT_EQ("Personal by UID", ToString(ESpamResolutionType::PersonalByUid));
}
