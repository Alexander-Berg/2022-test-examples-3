#include "utils.hpp"

#include <src/server/handlers/abook_search_contacts_handler.hpp>

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace {

using collie::expected;
using collie::logic::GetAbookFormatContacts;
using collie::logic::Uid;
using collie::services::abook::Tid;
using collie::services::abook::Contact;
using collie::services::abook::ContactEmail;
using collie::services::abook::SearchContactsGroupedResult;
using collie::services::abook::SearchContactsResult;
using collie::services::abook::SearchContactsUngroupedResult;
using collie::server::AbookSearchContactsHandler;
using collie::TaskContextPtr;
using collie::tests::makeRequestWithUriParams;
using collie::tests::MockStream;

struct GetAbookFormatContactsMock : public GetAbookFormatContacts {
    MOCK_METHOD(expected<SearchContactsResult>, call, (
        const TaskContextPtr&,
        const Uid& uid,
        const std::optional<std::string_view>&,
        const std::optional<std::string_view>&), (const));

    expected<SearchContactsResult> operator ()(
        const TaskContextPtr& context,
        const Uid& uid,
        const std::optional<std::string_view>& mixin,
        const std::optional<std::string_view>& group) const override
    {
        return call(context, uid, mixin, group);
    }
};

struct TestServerHandlersAbookSearchContactsHandler : TestWithTaskContext {
    SearchContactsResult makeSearchContactsGroupedResult() const {
        Contact contact;
        contact.cid = 123;
        contact.tag = std::vector<Tid>{};
        contact.name = {"collie", {}, "tests"};
        const auto emailId{1};
        contact.email = {{emailId, "collietest@yandex.ru", {}}};
        SearchContactsGroupedResult result;
        result.contact = {contact};
        result.count = 1;
        return result;
    }

    SearchContactsResult makeSearchContactsUngroupedResult() const {
        ContactEmail contactEmail;
        contactEmail.cid = 123;
        contactEmail.id = 1;
        contactEmail.tags = std::vector<Tid>{};
        contactEmail.name = {"collie", {}, "tests"};
        contactEmail.email = "collietest@yandex.ru";
        SearchContactsUngroupedResult result;
        result.contact = {contactEmail};
        result.count = 1;
        return result;
    }

    const std::shared_ptr<const StrictMock<GetAbookFormatContactsMock>> impl {std::make_shared<const StrictMock<GetAbookFormatContactsMock>>()};
    const AbookSearchContactsHandler handler {impl};
    const Uid uid {"uid"};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
};

TEST_F(TestServerHandlersAbookSearchContactsHandler,
        operator_call_with_mixin_and_group_must_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const auto request{makeRequestWithUriParams({{"uid", uid}, {"mixin", "yt"}, {"group", "yes"}})};
        EXPECT_CALL(*stream, request()).WillOnce(Return(request));
        EXPECT_CALL(*impl, call(context, uid, {"yt"}, {"yes"})).WillOnce(Return(
                makeSearchContactsGroupedResult()));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, "")).WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
        EXPECT_CALL(*stream, result_body(
                R"({"contact":[{"cid":123,"tag":[],"name":{"first":"collie","last":"tests"},)"
                R"("email":[{"id":1,"value":"collietest@yandex.ru"}]}],"count":1})")
        ).WillOnce(Return());
        EXPECT_EQ(handler(stream, context), expected<void>());
    });
}


TEST_F(TestServerHandlersAbookSearchContactsHandler,
        operator_call_without_mixin_and_group_must_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const auto request{makeRequestWithUriParams({{"uid", uid}})};
        EXPECT_CALL(*stream, request()).WillOnce(Return(request));
        EXPECT_CALL(*impl, call(context, uid, std::optional<std::string_view>{}, std::optional<
                std::string_view>{})).WillOnce(Return(makeSearchContactsUngroupedResult()));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, "")).WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
        EXPECT_CALL(*stream, result_body(
                R"({"contact":[{"cid":123,"id":1,"tags":[],"name":{"first":"collie","last":"tests"},)"
                R"("email":"collietest@yandex.ru"}],"count":1})")
        ).WillOnce(Return());
        EXPECT_EQ(handler(stream, context), expected<void>());
    });
}

} // namespace
