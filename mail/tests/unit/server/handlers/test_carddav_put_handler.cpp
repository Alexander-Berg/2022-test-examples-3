#include "utils.hpp"

#include <src/server/handlers/carddav_put_handler.hpp>

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace {

namespace logic = collie::logic;

using collie::expected;
using collie::server::CarddavPutHandler;
using collie::server::Etag;
using collie::server::Uid;
using collie::server::Uri;
using collie::TaskContextPtr;
using collie::tests::makeRequestWithRawBody;
using collie::tests::MockStream;
using logic::CarddavPut;
using logic::CarddavPutResult;

struct CarddavPutMock : CarddavPut {
    MOCK_METHOD(expected<CarddavPutResult>, call, (const TaskContextPtr&, const logic::Uid&,
            const std::string&, const std::string&, std::string), (const));

    expected<CarddavPutResult> operator()(const TaskContextPtr& context, const logic::Uid& uid,
            const std::string& uri, const std::string& etag, std::string vcard) const override {
        return call(context, uid, uri, etag, std::move(vcard));
    }
};

class TestServerHandlersCarddavPutHandler : public TestWithTaskContext {
protected:
    std::string getVcard() const {
        return "BEGIN:VCARD\n"
                "VERSION:3.0\n"
                "UID:YAAB-671844354-1\n"
                "BDAY:2019-04-19\n"
                "EMAIL:server@domain.ru\n"
                "FN:Server\n"
                "N:;Server;;;\n"
                "TEL:9876543210\n"
                "END:VCARD\n";
    }

    std::string getCarddavPutResponseBody(int status, const std::optional<std::string>& etag,
            const std::optional<std::string>& description) {
        std::string body {
            R"(<?xml version="1.0" encoding="UTF-8"?>)"
            "\n"
            "<put-response>"
            "<status>" + std::to_string(status) + "</status>"
        };

        const std::string replacedString{R"(")"};
        const std::string replacingString{"&quot;"};
        if (etag) {
            body += "<etag>" + boost::algorithm::replace_all_copy(
                    *etag, replacedString, replacingString) + "</etag>";
        }

        if (description) {
            body += "<description>" + boost::algorithm::replace_all_copy(
                    *description, replacedString, replacingString) + "</description>";
        }

        body += "</put-response>\n";
        return body;
    }

    const std::shared_ptr<const StrictMock<CarddavPutMock>> impl{
            std::make_shared<const StrictMock<CarddavPutMock>>()};
    const CarddavPutHandler handler{impl};
    const Uid queryUid{"uid"};
    const Uri queryUri{"a.vcf"};
    const Etag queryEtag{"%2A"};
    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
    yplatform::zerocopy::streambuf buffer;
};

TEST_F(TestServerHandlersCarddavPutHandler,
        operator_call_must_call_impl_operator_call_which_succeeds_and_write_result_to_stream) {
    withSpawn([this](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody(getVcard(), buffer)));

        const int statusOk{200};
        std::optional<std::string> etag{R"(")" + queryEtag.value + R"(")"};
        const std::string body{getCarddavPutResponseBody(statusOk, etag, {})};
        const CarddavPutResult result{statusOk, std::move(etag), {}};
        EXPECT_CALL(*impl, call(context, queryUid.value, queryUri.value, queryEtag.value, getVcard())).
                WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, set_content_type("application/xml"));
        EXPECT_CALL(*stream, result_body(body));
        EXPECT_EQ(expected<void>{}, handler(queryUid, queryUri, queryEtag, stream, context));
    });
}

TEST_F(TestServerHandlersCarddavPutHandler,
        operator_call_must_call_impl_operator_call_which_fails_and_write_result_to_stream) {
    withSpawn([this](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody(getVcard(), buffer)));

        const int statusConflict{409};
        std::optional<std::string> description{R"(etag mismatch, contact etag ("69-64" != "68-63"))"};
        const std::string body{getCarddavPutResponseBody(statusConflict, {}, description)};
        const CarddavPutResult result{statusConflict, {}, std::move(description)};
        EXPECT_CALL(*impl, call(context, queryUid.value, queryUri.value, queryEtag.value, getVcard())).
                WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, set_content_type("application/xml"));
        EXPECT_CALL(*stream, result_body(body));
        EXPECT_EQ(expected<void>{}, handler(queryUid, queryUri, queryEtag, stream, context));
    });
}

} // namespace
