#include "add_emails_mock.hpp"
#include "utils.hpp"

#include <src/server/handlers/abook_colabook_feed_addrdb_handler.hpp>

#include <tests/unit/error_category.hpp>
#include <tests/unit/logic/interface/types/recipients.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace {

namespace logic = collie::logic;
namespace server = collie::server;
namespace tests = collie::tests;

using namespace std::string_literals;

using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::TaskContextPtr;
using logic::ContactId;
using logic::CreatedContacts;
using logic::Recipients;
using logic::Revision;
using logic::Uid;
using server::AbookColabookFeedAddrdbHandler;
using server::Error;
using tests::AddEmailsMock;
using tests::makeRequestWithUriParams;
using tests::MockStream;

struct TestServerHandlersAbookColabookFeedAddrdbHandler : TestWithTaskContext {
    ymod_webserver::param_map_t makeUriParams() const {
        return {{"uid", "uid"}, {"to", R"("First, Second" local0@domain0.com, local1@domain1.com)"}};
    }

    ymod_webserver::param_map_t makeIncorrectUriParams() const {
        return {{"uid", "uid"}, {"to", "IncorrectAddress"}};
    }

    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
    const std::shared_ptr<const StrictMock<AddEmailsMock>> impl{std::make_shared<const StrictMock<
            AddEmailsMock>>()};
    const AbookColabookFeedAddrdbHandler handler{impl};
    const Uid uid{"uid"};
    const Recipients recipients{std::vector<std::string>{R"("First, Second" <local0@domain0.com>)",
            "local1@domain1.com"}, {}, {}};

    const CreatedContacts createdContacts{{ContactId{1}}, Revision{2}};
};

TEST_F(TestServerHandlersAbookColabookFeedAddrdbHandler,
        operator_call_must_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams(makeUriParams())));
        EXPECT_CALL(*impl, call(context, uid, recipients)).WillOnce(Return(createdContacts));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, std::string{}));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(R"({"contact_ids":[1],"revision":2})"));
        EXPECT_EQ(expected<void>{}, handler(stream, context));
    });
}

TEST_F(TestServerHandlersAbookColabookFeedAddrdbHandler,
        operator_call_must_return_error_when_uid_not_present_in_query_string) {
    withSpawn([&](const auto& context) {
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams(
                {{"to", "local0@domain0.com"}})));
        EXPECT_EQ(make_expected_from_error<void>(error_code(Error::invalidParameter)),
                handler(stream, context));
    });
}

TEST_F(TestServerHandlersAbookColabookFeedAddrdbHandler,
        operator_call_must_return_error_when_to_not_present_in_query_string) {
    withSpawn([&](const auto& context) {
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams({{uid, uid}})));
        EXPECT_EQ(make_expected_from_error<void>(error_code(Error::invalidParameter)),
                handler(stream, context));
    });
}

TEST_F(TestServerHandlersAbookColabookFeedAddrdbHandler,
        operator_call_must_return_empty_contacts_ids_for_incorrect_address_in_to) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams(makeIncorrectUriParams())));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, std::string{}));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(R"({"contact_ids":[],"revision":0})"));
        EXPECT_EQ(expected<void>{}, handler(stream, context));
    });
}

TEST_F(TestServerHandlersAbookColabookFeedAddrdbHandler,
        operator_call_must_return_error_when_impl_returns_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams(makeUriParams())));
        const error_code error{tests::Error::fail};
        EXPECT_CALL(*impl, call(context, uid, recipients)).WillOnce(Return(make_expected_from_error<
                CreatedContacts>(error)));
        EXPECT_EQ(make_expected_from_error<void>(error), handler(stream, context));
    });
}

} // namespace
