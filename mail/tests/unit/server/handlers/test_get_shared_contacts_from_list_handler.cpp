#include "utils.hpp"

#include <src/logic/interface/types/email_id.hpp>
#include <src/server/handlers/get_shared_contacts_from_list_handler.hpp>

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
using logic::ContactId;
using logic::EmailId;
using logic::ExistingContacts;
using logic::GetSharedContactsFromList;
using logic::Revision;
using logic::TagId;
using server::GetSharedContactsFromListHandler;
using server::ContactIds;
using server::ListId;
using server::Uid;
using tests::Error;
using tests::makeRequestWithUriParams;
using tests::MockStream;

struct GetSharedContactsFromListMock : GetSharedContactsFromList {
    MOCK_METHOD(expected<ExistingContacts>, call, (
            const TaskContextPtr&,
            const logic::Uid&,
            const std::vector<ContactId>&,
            logic::ListId,
            const std::optional<std::string_view>&,
            const std::optional<std::string_view>&,
            bool), (const));

    expected<ExistingContacts> operator ()(
            const TaskContextPtr& context,
            const logic::Uid& uid,
            const std::vector<ContactId>& contactIds,
            logic::ListId listId,
            const std::optional<std::string_view>& offset,
            const std::optional<std::string_view>& limit,
            bool sharedWithEmails) const override {
        return call(context, uid, contactIds, listId, offset, limit, sharedWithEmails);
    }
};

struct TestServerHandlersGetSharedContactsFromListHandler : TestWithTaskContext {
    ymod_webserver::param_map_t makeUriParams(std::string sharedWithEmailsValue) const {
        std::string offsetKey{"offset"};
        std::string limitKey{"limit"};
        std::string sharedWithEmailsKey{"shared_with_emails"};
        return {{std::move(offsetKey), std::string{*offset}}, {std::move(limitKey), std::string{*limit}},
                {std::move(sharedWithEmailsKey), std::move(sharedWithEmailsValue)}};
    }

    std::string makeResultBody() const {
        return R"({"contacts":[{"contact_id":1,"list_id":1,"revision":3,"vcard":{},"tag_ids":[1,2],)"
                R"("uri":"URI","emails":[{"id":1,"value":"one@ya.ru","tags":[1,2]},)"
                R"({"id":2,"value":"two@ya.ru","tags":[1,2]}]}]})";
    }

    void prepareStreamResultDataExpectations() const {
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, std::string{}));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(makeResultBody()));
    }

    void testHandler(const TaskContextPtr& context) const {
        prepareStreamResultDataExpectations();
        EXPECT_EQ(make_expected(), handler(uid, listId, contactIds, stream, context));
    }

    void testOptionalParams(std::string sharedWithEmailsValue, bool sharedWithEmails) {
        withSpawn([&](const auto& context) {
            const InSequence sequence;
            EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams(makeUriParams(
                    std::move(sharedWithEmailsValue)))));
            EXPECT_CALL(*impl, call(context, uid.value, contactIds.value, listId.value, offset, limit,
                    sharedWithEmails)).WillOnce(Return(result));
            testHandler(context);
        });
    }

    const std::shared_ptr<const StrictMock<GetSharedContactsFromListMock>> impl{
            std::make_shared<const StrictMock<GetSharedContactsFromListMock>>()};
    const GetSharedContactsFromListHandler handler{impl};
    const Uid uid{"42"};
    const ListId listId{2};
    const ContactIds contactIds{{1}};
    const std::optional<std::string_view> offset{"2"};
    const std::optional<std::string_view> limit{"1"};
    const bool allTheShared{false};
    const ymod_webserver::request_ptr request{boost::make_shared<ymod_webserver::request>()};
    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
    const Revision revision{3};
    const std::vector<TagId> tagIds{1,2};
    const std::string uri{"URI"};
    const EmailId emailId{1};
    ExistingContacts result{{{contactIds.value[0], listId.value - 1, revision, {}, tagIds, uri,
        {{emailId, "one@ya.ru", tagIds}, {emailId + 1, "two@ya.ru", tagIds}}}}};
};

TEST_F(TestServerHandlersGetSharedContactsFromListHandler,
        operator_call_must_return_error_when_impl_returns_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(request));
        const error_code error{Error::fail};
        EXPECT_CALL(*impl, call(context, uid.value, contactIds.value, listId.value, std::optional<
                std::string_view>{}, std::optional<std::string_view>{}, allTheShared)).WillOnce(
                        Return(make_expected_from_error<ExistingContacts>(error)));
        EXPECT_EQ(make_expected_from_error<void>(error), handler(uid, listId, contactIds, stream, context));
    });
}

TEST_F(TestServerHandlersGetSharedContactsFromListHandler,
        operator_call_must_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(request));
        EXPECT_CALL(*impl, call(context, uid.value, contactIds.value, listId.value, std::optional<
                std::string_view>{}, std::optional<std::string_view>{}, allTheShared)).WillOnce(
                        Return(result));
        testHandler(context);
    });
}

TEST_F(TestServerHandlersGetSharedContactsFromListHandler,
        operator_call_must_call_impl_operator_call_with_optional_params_and_with_all_the_shared) {
    std::string sharedWithEmailsValue{"0"};
    testOptionalParams(std::move(sharedWithEmailsValue), allTheShared);
}

TEST_F(TestServerHandlersGetSharedContactsFromListHandler,
        operator_call_must_call_impl_operator_call_with_optional_params_and_with_shared_with_emails) {
    std::string sharedWithEmailsValue{"1"};
    const auto sharedWithEmails{true};
    testOptionalParams(std::move(sharedWithEmailsValue), sharedWithEmails);
}

}
