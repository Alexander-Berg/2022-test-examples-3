#include <gtest/gtest.h>

#include <internal/update_user_info_params.h>

namespace {

using namespace ::testing;

struct UpdateUserInfo : Test {
    mbox_oper::MailboxOperParams params;
    mbox_oper::blackbox::User userInfo;

    void SetUp() override {
        params.login = "p_login";

        userInfo.login = "u_login";
        userInfo.karma = "u_karma";
        userInfo.karmaStatus = "u_karma_status";
    }
};

TEST_F(UpdateUserInfo, should_use_info_only_from_params_on_params_only_strategy) {
    mbox_oper::MailboxOperParams result = params;
    mbox_oper::updateUserInfo(result, userInfo, mbox_oper::UserInfoStrategy::paramsOnly);

    EXPECT_EQ(result.login, "p_login");
    EXPECT_EQ(result.karma, "");
    EXPECT_EQ(result.karmaStatus, "");
}

TEST_F(UpdateUserInfo, should_use_info_only_from_blackbox_on_blackbox_only_strategy) {
    mbox_oper::MailboxOperParams result = params;
    mbox_oper::updateUserInfo(result, userInfo, mbox_oper::UserInfoStrategy::blackboxOnly);

    EXPECT_EQ(result.login, "u_login");
    EXPECT_EQ(result.karma, "u_karma");
    EXPECT_EQ(result.karmaStatus, "u_karma_status");
}

TEST_F(UpdateUserInfo, should_use_info_from_params_first_on_params_then_blackbox_strategy) {
    mbox_oper::MailboxOperParams result = params;
    mbox_oper::updateUserInfo(result, userInfo, mbox_oper::UserInfoStrategy::paramsThenBlackbox);

    EXPECT_EQ(result.login, "p_login");
    EXPECT_EQ(result.karma, "u_karma");
    EXPECT_EQ(result.karmaStatus, "u_karma_status");
}

TEST_F(UpdateUserInfo, should_use_info_from_blackbox_first_on_blackbox_then_params_strategy) {
    mbox_oper::MailboxOperParams result = params;
    mbox_oper::updateUserInfo(result, userInfo, mbox_oper::UserInfoStrategy::blackboxThenParams);

    EXPECT_EQ(result.login, "u_login");
    EXPECT_EQ(result.karma, "u_karma");
    EXPECT_EQ(result.karmaStatus, "u_karma_status");
}

}
