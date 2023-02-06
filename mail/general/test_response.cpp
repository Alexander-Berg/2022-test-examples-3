#include <web/types/reflection/response.h>
#include <web/response.h>

#include <mocks/http_stream.h>

#include <gtest/gtest.h>

namespace {

using namespace testing;

using NMdb::TResolvedFolder;
using NMdb::NWeb::EHttpCode;
using NMdb::NWeb::Respond;
using NMdb::NWeb::TSaveResponse;
using NMdb::NWeb::TSaveResponseRcpt;
using NMdb::NWeb::TErrorResponse;

TEST(TTestRespond, for_save_response_must_call_stream_methods_with_relevant_arguments) {
    const auto httpStream = boost::make_shared<StrictMock<THttpStreamMock>>();
    const auto httpCode = EHttpCode::ok;

    TSaveResponseRcpt successNode;
    successNode.Uid = "uid0";
    successNode.Status = "ok";
    successNode.Mid = "mid0";
    successNode.ImapId = "imap_id0";
    successNode.Tid = "tid0";
    successNode.Duplicate = false;
    successNode.Folder = TResolvedFolder{"fid0", "name0", "type0", 123};
    successNode.Labels = {
        {"lid0", "symbol0"},
        {"lid1", "symbol1"}
    };

    TSaveResponseRcpt permErrorNode;
    permErrorNode.Uid = "uid1";
    permErrorNode.Status = "perm error";
    permErrorNode.Description = "perm error happened";

    TSaveResponseRcpt tempErrorNode;
    tempErrorNode.Uid = "uid2";
    tempErrorNode.Status = "temp error";
    tempErrorNode.Description = "temp error happened";

    TSaveResponse response;
    response.Rcpts = {
        {"0", std::move(successNode)},
        {"1", std::move(permErrorNode)},
        {"2", std::move(tempErrorNode)}
    };

    const InSequence sequence;
    EXPECT_CALL(*httpStream, set_code(httpCode, std::string{}));
    EXPECT_CALL(*httpStream, set_content_type("application/json"));
    EXPECT_CALL(*httpStream, result_body(
        R"({"rcpts":[{"id":"0","rcpt":{"uid":"uid0","status":"ok","mid":"mid0","imap_id":"imap_id0",)"
        R"("tid":"tid0","duplicate":false,"folder":{"fid":"fid0","name":"name0","type":"type0",)"
        R"("type_code":123},"labels":[{"lid":"lid0","symbol":"symbol0"},{"lid":"lid1","symbol":"symbol1"}]}},)"
        R"({"id":"1","rcpt":{"uid":"uid1","status":"perm error","description":"perm error happened"}},)"
        R"({"id":"2","rcpt":{"uid":"uid2","status":"temp error","description":"temp error happened"}}]})"
    ));
    Respond(httpStream, httpCode, response);
}

TEST(TTestRespond, for_error_response_must_call_stream_methods_with_relevant_arguments) {
    const auto httpStream = boost::make_shared<StrictMock<THttpStreamMock>>();
    const auto httpCode = EHttpCode::bad_request;

    TErrorResponse response {"bad request", "bad request message"};

    const InSequence sequence;
    EXPECT_CALL(*httpStream, set_code(httpCode, std::string{}));
    EXPECT_CALL(*httpStream, set_content_type("application/json"));
    EXPECT_CALL(*httpStream, result_body(
        R"({"error":"bad request","message":"bad request message"})"
    ));
    Respond(httpStream, httpCode, response);
}

}
