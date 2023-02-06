#include "utils.hpp"

#include <src/server/handlers/get_shared_contacts_count_from_list_handler.hpp>

#include <tests/unit/error_category.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace {

namespace logic = collie::logic;
namespace server = collie::server;
namespace tests = collie::tests;

using collie::error_code;
using collie::expected;
using collie::make_expected;
using collie::make_expected_from_error;
using collie::TaskContextPtr;
using logic::GetSharedContactsCountFromList;
using logic::ListContactsCounter;
using server::GetSharedContactsCountFromListHandler;
using server::ListId;
using server::Uid;
using tests::Error;
using tests::makeRequestWithUriParams;
using tests::MockStream;

struct GetSharedContactsCountFromListMock : GetSharedContactsCountFromList {
    MOCK_METHOD(expected<ListContactsCounter>, call, (const TaskContextPtr&, const logic::Uid&,
            logic::ListId listId, bool), (const));
    expected<ListContactsCounter> operator()(const TaskContextPtr& context, const logic::Uid& uid,
            logic::ListId listId, bool sharedWithEmails) const override {
        return call(context, uid, listId, sharedWithEmails);
    }
};

struct TestServerHandlersGetSharedContactsCountFromListHandler : TestWithTaskContext {
    ymod_webserver::param_map_t makeUriParams(std::string sharedWithEmailsValue) const {
        std::string sharedWithEmailsKey{"shared_with_emails"};
        return {{std::move(sharedWithEmailsKey), std::move(sharedWithEmailsValue)}};
    }

    std::string makeResultBody() const {
        return R"({"count":16})";
    }

    void prepareStreamResultDataExpectations() const {
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, std::string{}));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(makeResultBody()));
    }

    void testHandler(const TaskContextPtr& context) const {
        prepareStreamResultDataExpectations();
        EXPECT_EQ(make_expected(), handler(uid, listId, stream, context));
    }

    void testOptionalParams(std::string sharedWithEmailsValue, bool sharedWithEmails) {
        withSpawn([&](const auto& context) {
            const InSequence sequence;
            EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams(makeUriParams(
                    std::move(sharedWithEmailsValue)))));
            EXPECT_CALL(*impl, call(context, uid.value, listId.value, sharedWithEmails)).WillOnce(Return(
                    result));
            testHandler(context);
        });
    }

    const std::shared_ptr<const StrictMock<GetSharedContactsCountFromListMock>> impl {
        std::make_shared<const StrictMock<GetSharedContactsCountFromListMock>>()
    };

    const GetSharedContactsCountFromListHandler handler{impl};
    const Uid uid{"uid"};
    const ListId listId{1};
    const bool allTheShared{false};
    const ymod_webserver::request_ptr request{boost::make_shared<ymod_webserver::request>()};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
    const ListContactsCounter result{16};
};

TEST_F(TestServerHandlersGetSharedContactsCountFromListHandler,
        operator_call_must_return_error_when_impl_returns_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(request));
        const error_code error{Error::fail};
        EXPECT_CALL(*impl, call(context, uid.value, listId.value, allTheShared)).WillOnce(
                Return(make_expected_from_error<ListContactsCounter>(error)));
        EXPECT_EQ(make_expected_from_error<void>(error), handler(uid, listId, stream, context));
    });
}

TEST_F(TestServerHandlersGetSharedContactsCountFromListHandler,
        operator_call_must_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(request));
        EXPECT_CALL(*impl, call(context, uid.value, listId.value, allTheShared)).WillOnce(Return(result));
        testHandler(context);
    });
}

TEST_F(TestServerHandlersGetSharedContactsCountFromListHandler,
        operator_call_must_call_impl_operator_call_with_all_the_shared) {
    std::string sharedWithEmailsValue{"0"};
    testOptionalParams(std::move(sharedWithEmailsValue), allTheShared);
}

TEST_F(TestServerHandlersGetSharedContactsCountFromListHandler,
        operator_call_must_call_impl_operator_call_with_shared_with_emails) {
    std::string sharedWithEmailsValue{"1"};
    const auto sharedWithEmails{true};
    testOptionalParams(std::move(sharedWithEmailsValue), sharedWithEmails);
}

}
