#include "utils.hpp"

#include <tests/unit/server/error_code_operators.hpp>
#include <tests/unit/generic_operators.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/handlers/get_contacts_handler.hpp>
#include <src/logic/interface/types/reflection/existing_contact.hpp>

namespace collie::logic {

using collie::tests::operator ==;
using collie::tests::operator <<;

} // namespace collie::logic

namespace {

using namespace testing;
using namespace std::string_literals;

namespace logic = collie::logic;

using collie::EmailWithTags;
using collie::expected;
using collie::TaskContextPtr;
using collie::logic::ContactId;
using collie::logic::GetContacts;
using collie::logic::ExistingContact;
using collie::logic::ExistingContacts;
using collie::logic::ListId;
using collie::logic::Revision;
using collie::logic::Vcard;
using collie::server::Error;
using collie::server::ContactIds;
using collie::server::GetContactsHandler;
using collie::server::Uid;
using collie::tests::MockStream;
using collie::error_code;
using collie::services::abook::Tid;

struct GetContactsMock : GetContacts {
    MOCK_METHOD(expected<ExistingContacts>, call, (
        const TaskContextPtr&,
        const logic::Uid&,
        const std::vector<ContactId>&,
        const std::optional<std::string_view>&,
        const std::optional<std::string_view>&,
        const std::optional<std::string_view>&,
        bool,
        bool), (const));

    expected<ExistingContacts> operator()(
            const TaskContextPtr& context,
            const logic::Uid& uid,
            const std::vector<ContactId>& contactIds,
            const std::optional<std::string_view>& mixin,
            const std::optional<std::string_view>& offset,
            const std::optional<std::string_view>& limit,
            bool onlyShared,
            bool sharedWithEmails) const override {
        return call(context, uid, contactIds, mixin, offset, limit, onlyShared, sharedWithEmails);
    }
};

struct TestServerHandlersGetContactsHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<GetContactsMock>> impl {std::make_shared<const StrictMock<GetContactsMock>>()};
    const GetContactsHandler handler {impl};
    const Uid uid {"uid"};
    const std::optional<std::string_view> mixin {"yt"};
    const std::optional<std::string_view> offset {"2"};
    const std::optional<std::string_view> limit {"5"};
    bool onlyShared {true};
    bool sharedWithEmails {true};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
    const std::vector<ContactId> contactIds {ContactId {13}};
    ExistingContacts result;

    TestServerHandlersGetContactsHandler() {
        const Vcard vcard;
        std::vector<Tid> tids{1,2};
        result.contacts.push_back(ExistingContact{
            ContactId{13},
            ListId{0},
            Revision{42},
            vcard,
            {},
            "uri"s,
            {EmailWithTags{1, "one@ya.ru", tids}, EmailWithTags{2, "two@ya.ru", tids}}});
    }
};

TEST_F(TestServerHandlersGetContactsHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        const auto request = boost::make_shared<ymod_webserver::request>();
        EXPECT_CALL(*stream, request()).WillOnce(Return(request));
        EXPECT_CALL(*impl, call(
            context,
            uid.value,
            contactIds,
            std::optional<std::string_view>(),
            std::optional<std::string_view>(),
            std::optional<std::string_view>(),
            false,
            false)).WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, "")).WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"contacts":[{"contact_id":13,"list_id":0,"revision":42,)"
                R"("vcard":{},"tag_ids":[],"uri":"uri",)"
                R"("emails":[{"id":1,"value":"one@ya.ru","tags":[1,2]},{"id":2,"value":"two@ya.ru","tags":[1,2]}]}]})")
        ).WillOnce(Return());
        EXPECT_EQ(handler(uid, ContactIds {{13}}, stream, context), expected<void>());
    });
}

TEST_F(TestServerHandlersGetContactsHandler, operator_function_call_should_call_impl_operator_function_call_with_optional_params) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        const auto request = boost::make_shared<ymod_webserver::request>();
        request->url.params.insert({"mixin", std::string(*mixin)});
        request->url.params.insert({"offset", std::string(*offset)});
        request->url.params.insert({"limit", std::string(*limit)});
        request->url.params.insert({"only_shared", "1"});
        EXPECT_CALL(*stream, request()).WillOnce(Return(request));
        EXPECT_CALL(*impl, call( context, uid.value, contactIds, mixin, offset, limit, onlyShared, false)).WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, "")).WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"contacts":[{"contact_id":13,"list_id":0,"revision":42,)"
                R"("vcard":{},"tag_ids":[],"uri":"uri",)"
                R"("emails":[{"id":1,"value":"one@ya.ru","tags":[1,2]},{"id":2,"value":"two@ya.ru","tags":[1,2]}]}]})")
        ).WillOnce(Return());
        EXPECT_EQ(handler(uid, ContactIds {{13}}, stream, context), expected<void>());
    });
}

} // namespace
