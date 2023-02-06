#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/v2/threads_by_tab/method.h>
#include "../helpers.h"
#include "../messages_mailbox_mock.h"

namespace {

using namespace hound::server::handlers::v2::threads_by_tab;
using namespace hound::testing;
using namespace ::testing;
using macs::TabFactory;

struct ThreadsByTabMethod : Test {
    using Mailbox = StrictMock<MailboxMock>;
    using Params = Method<Mailbox>::QueryParams;
    using Response = Method<Mailbox>::Response;
    Method<Mailbox> method;
};

TEST_F(ThreadsByTabMethod, should_call_query_without_interval_if_not_set_and_returns_envelopes) {
    Params params;
    params.tab = macs::TabFactory().type(macs::Tab::Type::relevant).release();
    params.count = 100;
    params.first = 10;
    params.timestampRange = std::nullopt;

    macs::Envelope envelope = macs::EnvelopeFactory().mid("mid").threadId("tid").release();

    InSequence seq;
    EXPECT_CALL(*(method.mailbox.query_.mock), inTab(params.tab.type())).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), from(params.first)).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), count(params.count)).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), groupByThreads()).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), get()).WillOnce(Return(std::vector<macs::Envelope>{envelope}));
    EXPECT_CALL(method.mailbox, threadLabels(std::vector<std::string>{"tid"})).WillOnce(Return(macs::ThreadLabelsList{}));

    const auto res = method(params);
    const auto actual = res.value().threads_by_folder.envelopes;
    EXPECT_THAT(map(actual, midExtractor), Contains("mid"));
}

TEST_F(ThreadsByTabMethod, should_call_query_with_interval_if_set_and_returns_envelopes) {
    Params params;
    params.tab = macs::TabFactory().type(macs::Tab::Type::relevant).release();
    params.count = 100;
    params.first = 10;
    params.timestampRange = std::make_optional(std::make_pair(0, 0));

    macs::Envelope envelope = macs::EnvelopeFactory().mid("mid").threadId("tid").release();

    InSequence seq;
    EXPECT_CALL(*(method.mailbox.query_.mock), inTab(params.tab.type())).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), from(params.first)).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), count(params.count)).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), groupByThreads()).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), withinInterval(*(params.timestampRange))).Times(1);
    EXPECT_CALL(*(method.mailbox.query_.mock), get()).WillOnce(Return(std::vector<macs::Envelope>{envelope}));
    EXPECT_CALL(method.mailbox, threadLabels(std::vector<std::string>{"tid"})).WillOnce(Return(macs::ThreadLabelsList{}));

    const auto res = method(params);
    const auto actual = res.value().threads_by_folder.envelopes;
    EXPECT_THAT(map(actual, midExtractor), Contains("mid"));
}

} // namespace
