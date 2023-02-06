#include <utility>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/v2/tabs/method.h>
#include "../helpers.h"

namespace {

using namespace hound::server::handlers::v2::tabs;
using namespace hound::testing;
using namespace ::testing;
using macs::TabFactory;

struct MailboxMock {
    MOCK_METHOD(macs::TabSet, getAllTabs, (), (const));
    MOCK_METHOD(bool, canReadTabs, (), (const));
};

struct TestMailbox {
    MailboxMock& mailbox;

    macs::TabSet tabs() const {
        return mailbox.getAllTabs();
    }

    bool canReadTabs() const {
        return mailbox.canReadTabs();
    }
};

struct GetMailboxMock {
    MOCK_METHOD(TestMailbox, call, (macs::Uid uid), (const));
};

struct MailboxGetter {
    GetMailboxMock& mock;
    auto operator()(macs::Uid uid) const { return mock.call(std::move(uid)); }
};

struct TabsMethod : Test {
    MailboxMock mailbox;
    GetMailboxMock getMailbox;
    Method<MailboxGetter> method{MailboxGetter{getMailbox}};
};

TEST_F(TabsMethod, should_return_invalid_argument_for_empty_uid) {
    const auto res = method(Request{""});
    EXPECT_EQ(res.error(), error_code{error::invalidArgument});
}

TEST_F(TabsMethod, should_return_empty_tabs_for_uid_with_disabled_tabs) {
    macs::TabsMap tabs;
    tabs[macs::Tab::Type::relevant] = TabFactory().type(macs::Tab::Type::relevant).release();
    tabs[macs::Tab::Type::news] = TabFactory().type(macs::Tab::Type::news).release();

    EXPECT_CALL(getMailbox, call("uid")).WillOnce(Return(TestMailbox{mailbox}));
    EXPECT_CALL(mailbox, canReadTabs()).WillOnce(Return(false));

    const auto res = method(Request{"uid"});
    const std::vector<Tab> actual = res.value().tabs;
    EXPECT_THAT(actual, IsEmpty());
}

TEST_F(TabsMethod, should_return_tabs_for_correct_uid) {
    macs::TabsMap tabs;
    tabs[macs::Tab::Type::relevant] = TabFactory().type(macs::Tab::Type::relevant).release();
    tabs[macs::Tab::Type::news] = TabFactory().type(macs::Tab::Type::news).release();

    EXPECT_CALL(getMailbox, call("uid")).WillOnce(Return(TestMailbox{mailbox}));
    EXPECT_CALL(mailbox, canReadTabs()).WillOnce(Return(true));
    EXPECT_CALL(mailbox, getAllTabs()).WillOnce(Return(macs::TabSet(tabs)));

    const auto res = method(Request{"uid"});
    const std::vector<Tab> actual = res.value().tabs;
    EXPECT_THAT(map(actual, tabTypeExtractor), Contains("relevant"));
    EXPECT_THAT(map(actual, tabTypeExtractor), Contains("news"));

}

} // namespace

