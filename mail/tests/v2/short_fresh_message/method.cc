#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/v2/short_fresh_message/method.h>
#include "../helpers.h"
#include "../messages_mailbox_mock.h"

#include <macs/tab_factory.h>
#include <macs/label_factory.h>

namespace {

using namespace hound::server::handlers::v2::short_fresh_message;
using namespace hound::testing;
using namespace ::testing;
using macs::TabFactory;

struct ShortFreshMessageMethod : Test {
    using Mailbox = StrictMock<MailboxMock>;
    using Params = Method<Mailbox>::QueryParams;
    using Response = Method<Mailbox>::Response;
    Method<Mailbox> method;
};

TEST_F(ShortFreshMessageMethod, should_return_empty_result_if_relevant_not_found) {
    InSequence seq;
    EXPECT_CALL(method.mailbox, tabs()).WillOnce(Return(macs::TabSet{}));

    const auto res = method(Params{});
    const auto actual = res.value().messages;
    EXPECT_THAT(actual, IsEmpty());
}

TEST_F(ShortFreshMessageMethod, should_return_empty_result_if_relevant_has_no_fresh) {
    macs::TabsMap tabs;
    tabs[macs::Tab::Type::relevant] = macs::TabFactory()
            .type(macs::Tab::Type::relevant)
            .freshMessagesCount(0)
            .newMessagesCount(10)
            .release();

    InSequence seq;
    EXPECT_CALL(method.mailbox, tabs()).WillOnce(Return(macs::TabSet{tabs}));

    const auto res = method(Params{});
    const auto actual = res.value().messages;
    EXPECT_THAT(actual, IsEmpty());
}

TEST_F(ShortFreshMessageMethod, should_return_empty_result_if_relevant_has_no_new) {
    macs::TabsMap tabs;
    tabs[macs::Tab::Type::relevant] = macs::TabFactory()
            .type(macs::Tab::Type::relevant)
            .freshMessagesCount(10)
            .newMessagesCount(0)
            .release();

    InSequence seq;
    EXPECT_CALL(method.mailbox, tabs()).WillOnce(Return(macs::TabSet{tabs}));

    const auto res = method(Params{});
    const auto actual = res.value().messages;
    EXPECT_THAT(actual, IsEmpty());
}

TEST_F(ShortFreshMessageMethod, should_return_message_if_relevant_has_fresh) {
    macs::TabsMap tabs;
    tabs[macs::Tab::Type::relevant] = macs::TabFactory()
            .type(macs::Tab::Type::relevant)
            .freshMessagesCount(10)
            .newMessagesCount(10)
            .release();

    macs::LabelSet labels;
    labels["seen"] = macs::LabelFactory().lid("seen")
            .symbol(macs::Label::Symbol::seen_label).product();

    macs::Envelope envelope = macs::EnvelopeFactory().mid("mid").release();

    EXPECT_CALL(method.mailbox, labels()).WillOnce(Return(labels));

    InSequence seq;
    EXPECT_CALL(method.mailbox, tabs()).WillOnce(Return(macs::TabSet{tabs}));
    EXPECT_CALL(*(method.mailbox.query_.mock), inTab(macs::Tab::Type(macs::Tab::Type::relevant))).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), withoutLabel("seen")).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), from(0)).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), count(1)).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), get()).WillOnce(Return(std::vector<macs::Envelope>{envelope}));

    const auto res = method(Params{});
    const auto actual = res.value().messages;
    EXPECT_THAT(map(actual, shortMidExtractor), Contains("mid"));
}

} // namespace
