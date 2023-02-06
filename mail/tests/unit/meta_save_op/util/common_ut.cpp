#include <mail/notsolitesrv/src/meta_save_op/util/common.h>

#include <mail/notsolitesrv/tests/unit/util/email_address.h>

#include <gtest/gtest.h>

namespace {

using namespace NNotSoLiteSrv::NMetaSaveOp;

using NNotSoLiteSrv::TUid;

TEST(TestFiltersInUse, for_filters_not_in_use_must_return_false) {
    TParams params;
    params.use_filters = false;
    TRecipientMap recipients{{"DeliveryId", {{}, std::move(params)}}};
    EXPECT_FALSE(FiltersInUse(recipients));
}

TEST(TestFiltersInUse, for_filters_in_use_must_return_true) {
    TRecipientMap recipients{{"DeliveryId", {}}};
    EXPECT_TRUE(FiltersInUse(recipients));
}

TEST(TestToString, must_return_concatenated_email_address) {
    EXPECT_EQ("Local@Domain", ToString({"Local", "Domain", {}}));
}

TEST(TestFindRecipientByUid, for_recipient_unavailable_must_return_empty_optional) {
    TUser user;
    user.uid = 0;
    TRecipientMap recipients{{"DeliveryId", {std::move(user), {}}}};
    EXPECT_FALSE(FindRecipientByUid(recipients, TUid{1}));
}

TEST(TestFindRecipientByUid, for_recipient_available_must_return_recipient) {
    TRecipientMap recipients;
    TUser user;
    user.uid = 0;
    recipients["DeliveryId0"] = {user, {}};
    user.uid = 1;
    recipients["DeliveryId1"] = {user, {}};
    const auto recipient = FindRecipientByUid(recipients, user.uid);
    ASSERT_TRUE(recipient);
    EXPECT_EQ(user.uid, recipient->get().user.uid);
}

TEST(TestMakeOptionalRange, for_empty_range_must_return_empty_optional) {
    EXPECT_FALSE(MakeOptionalRange(std::vector<int>{}));
}

TEST(TestMakeOptionalRange, for_nonempty_range_must_return_optional_range) {
    const std::vector<int> range{0, 1, 2};
    const auto optionalRange = MakeOptionalRange(range);
    ASSERT_TRUE(optionalRange);
    EXPECT_EQ(range, *optionalRange);
}

}
