#include "utils.hpp"

#include <src/server/handlers/get_contacts_with_tag_handler.hpp>

#include <tests/unit/error_category.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace {

namespace logic = collie::logic;
namespace server = collie::server;
namespace tests = collie::tests;

using collie::EmailWithTags;
using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::TaskContextPtr;
using logic::GetContactsWithTag;
using logic::ExistingContacts;
using logic::Vcard;
using server::GetContactsWithTagHandler;
using server::TagId;
using server::Uid;
using tests::Error;
using tests::makeRequestWithUriParams;
using tests::MockStream;

struct GetContactsWithTagMock : GetContactsWithTag {
    MOCK_METHOD(expected<ExistingContacts>, call, (
        const TaskContextPtr&,
        const logic::Uid&,
        const logic::TagId,
        const std::optional<std::string_view>&,
        const std::optional<std::string_view>&), (const));

    expected<ExistingContacts> operator()(
            const TaskContextPtr& context,
            const logic::Uid& uid,
            logic::TagId tagId,
            const std::optional<std::string_view>& offset,
            const std::optional<std::string_view>& limit) const override {
        return call(context, uid, tagId, offset, limit);
    }
};

struct TestServerHandlersGetContactsWithTagHandler : TestWithTaskContext {
    ymod_webserver::param_map_t makeUriParams() const {
        using namespace std::string_literals;
        return {{"offset"s, std::string{*offset}}, {"limit"s, std::string{*limit}}};
    }

    ExistingContacts makeExistingContacts() const {
        ExistingContacts result;
        Vcard vcard;
        vcard.names = {{{"First", {}, {}, {}, {}}}};
        std::vector<std::int64_t> tids{1,2};
        result.contacts = {{
            1, 2, 3, std::move(vcard), {4, 5}, "uri",
            {EmailWithTags{1, "one@ya.ru", tids}, EmailWithTags{2, "two@ya.ru", tids}}}};
        return result;
    }

    std::string makeResultBody() const {
        return R"({"contacts":[{"contact_id":1,"list_id":2,"revision":3,)"
                R"("vcard":{"names":[{"first":"First"}]},"tag_ids":[4,5],"uri":"uri",)"
                R"("emails":[{"id":1,"value":"one@ya.ru","tags":[1,2]},{"id":2,"value":"two@ya.ru","tags":[1,2]}]}]})";
    }

    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
    const std::shared_ptr<const StrictMock<GetContactsWithTagMock>> impl{std::make_shared<const StrictMock<
            GetContactsWithTagMock>>()};
    const GetContactsWithTagHandler handler{impl};
    const Uid uid{"uid"};
    const TagId tagId{1};
    const std::optional<std::string_view> offset{"20"};
    const std::optional<std::string_view> limit{"10"};
};

TEST_F(TestServerHandlersGetContactsWithTagHandler,
        operator_call_must_call_impl_operator_call_with_no_opt_params_and_write_result_to_stream) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams({})));
        EXPECT_CALL(*impl, call(context, uid.value, tagId.value, std::optional<std::string_view>{},
                std::optional<std::string_view>{})).WillOnce(Return(makeExistingContacts()));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, std::string{}));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(makeResultBody()));
        EXPECT_EQ(expected<void>{}, handler(uid, tagId, stream, context));
    });
}

TEST_F(TestServerHandlersGetContactsWithTagHandler,
        operator_call_must_call_impl_operator_call_with_opt_params_and_write_result_to_stream) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams(makeUriParams())));
        EXPECT_CALL(*impl, call(context, uid.value, tagId.value, offset, limit)).WillOnce(Return(
                makeExistingContacts()));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, std::string{}));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(makeResultBody()));
        EXPECT_EQ(expected<void>{}, handler(uid, tagId, stream, context));
    });
}

TEST_F(TestServerHandlersGetContactsWithTagHandler, operator_call_must_return_error_when_impl_returns_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams(makeUriParams())));
        const error_code error{Error::fail};
        EXPECT_CALL(*impl, call(context, uid.value, tagId.value, offset, limit)).WillOnce(Return(
                make_expected_from_error<ExistingContacts>(error)));
        EXPECT_EQ(make_expected_from_error<void>(error), handler(uid, tagId, stream, context));
    });
}

} // namespace
