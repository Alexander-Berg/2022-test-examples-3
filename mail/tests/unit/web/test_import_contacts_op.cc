#include <web/handlers/import_contacts_op.h>
#include <with_spawn.h>
#include <web/common_mocks.h>
#include <yplatform/zerocopy/streambuf.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace testing;
using namespace sheltie;
using namespace sheltie::web;
using namespace sheltie::tests;

struct TestImportContactsOp : public TestWithYieldCtx {
    WebContextMockPtr webCtx = std::make_shared<WebContextMock>();
    boost::shared_ptr<StrictMock<MockStream>> stream =boost::make_shared<StrictMock<MockStream>>();
    RequestLogger logger = getLogger("-", "-");
    std::string uid = "123";
    std::string importedContacts = R"([{"vcard": {"names": [{"first": "Elon"}], "emails": [{"email": "elon@musk.molodec"}], "telephone_numbers": [{"telephone_number": "8800"}], "vcard_uids": ["heh"]}}, {"vcard": {"names": [{"first": "Ivan"}], "emails": [{"email": "vanya@pochty.molodec"}], "telephone_numbers": [{"telephone_number": "8888"}], "vcard_uids": ["hah"]}}])";
    std::string existingContacts = R"({"contacts": [{"contact_id": 1, "list_id": 2, "revision": 3, "tag_ids": [4, 5], "vcard": {"names": [{"first": "Elon"}], "emails": [{"email": "elon@musk.molodec"}], "telephone_numbers": [{"telephone_number": "8800"}], "vcard_uids": ["heh"]}, "emails": []}, {"contact_id": 2, "list_id": 3, "revision": 4, "tag_ids": [5, 6], "vcard": {"names": [{"first": "Musk"}], "emails": [{"email": "musk@Elon.molodec"}], "telephone_numbers": [{"telephone_number": "8800"}], "vcard_uids": ["heh"]}, "emails": []}]})";
};

TEST_F(TestImportContactsOp, should_response_internal_server_error_when_error_occured_in_python_module) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        EXPECT_CALL(*(webCtx->pythonModule), importContacts(_, _, _)).WillOnce(Invoke([](auto, auto, YieldCtx yieldCtx) {
            *yieldCtx.ec_ = ymod_httpclient::http_error::unknown_error;
            return "";
        }));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, ""));
        EXPECT_CALL(*stream, result_body("import contacts error: unknown_error"));
        ImportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestImportContactsOp, should_response_empty_contacts_if_python_module_returned_empty_import_contacts) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        std::string contactResponse = "{}";
        std::string resultResponse = "{\"status\":\"Ok\",\"rec_cnt\":0,\"rec_skipped\":0}";
        EXPECT_CALL(*(webCtx->pythonModule), importContacts(_, _, _)).WillOnce(Return(contactResponse));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, result_body(resultResponse));
        ImportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestImportContactsOp, should_response_internal_server_error_when_error_occured_to_get_contacts_in_http_client) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        std::string contactResponse = R"([{"vcard": {}}])";
        EXPECT_CALL(*(webCtx->pythonModule), importContacts(_, _, _)).WillOnce(Return(contactResponse));
        EXPECT_CALL(*(webCtx->collieClient), async_run(_, _, _)).WillOnce(Invoke([](auto, auto, YieldCtx yieldCtx) {
            *yieldCtx.ec_ = ymod_httpclient::http_error::request_timeout;
            return yhttp::response();
        }));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, ""));
        EXPECT_CALL(*stream, result_body("import contacts error: request_timeout"));
        ImportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestImportContactsOp, should_response_internal_server_error_to_get_contacts_from_http_client) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        std::string contactResponse = importedContacts;
        EXPECT_CALL(*(webCtx->pythonModule), importContacts(_, _, _)).WillOnce(Return(contactResponse));
        yhttp::response response;
        response.status = 500;
        response.reason = "Internal Server Error";
        EXPECT_CALL(*(webCtx->collieClient), async_run(_, _, _)).WillOnce(Return(response));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, ""));
        EXPECT_CALL(*stream, result_body("import contacts error: Internal Server Error"));
        ImportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestImportContactsOp, should_response_ok_with_empty_new_contacts) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        std::string contactResponse = R"([{"vcard": {"names": [{"first": "Elon"}], "emails": [{"email": "elon@musk.molodec"}], "telephone_numbers": [{"telephone_number": "8800"}], "vcard_uids": ["heh"]}}])";
        EXPECT_CALL(*(webCtx->pythonModule), importContacts(_, _, _)).WillOnce(Return(contactResponse));
        yhttp::response response;
        response.status = 200;
        response.body = existingContacts;
        EXPECT_CALL(*(webCtx->collieClient), async_run(_, _, _)).WillOnce(Return(response));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, result_body("{\"status\":\"Ok\",\"rec_cnt\":0,\"rec_skipped\":1}"));
        ImportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestImportContactsOp, should_response_internal_server_error_when_error_occured_to_create_contacts_in_http_client) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        std::string contactResponse = importedContacts;
        EXPECT_CALL(*(webCtx->pythonModule), importContacts(_, _, _)).WillOnce(Return(contactResponse));
        yhttp::response response;
        response.status = 200;
        response.body = existingContacts;
        EXPECT_CALL(*(webCtx->collieClient), async_run(_, _, _))
            .WillOnce(Return(response))
            .WillOnce(Invoke([](auto, auto, YieldCtx yieldCtx) {
                *yieldCtx.ec_ = ymod_httpclient::http_error::request_timeout;
                return yhttp::response();
            }));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, ""));
        EXPECT_CALL(*stream, result_body("import contacts error: request_timeout"));
        ImportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestImportContactsOp, should_response_internal_server_error_to_create_contacts_from_http_client) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        std::string contactResponse = importedContacts;
        EXPECT_CALL(*(webCtx->pythonModule), importContacts(_, _, _)).WillOnce(Return(contactResponse));
        yhttp::response getResponse;
        getResponse.status = 200;
        getResponse.body = existingContacts;
        yhttp::response createResponse;
        createResponse.status = 500;
        createResponse.reason = "Internal Server Error";
        EXPECT_CALL(*(webCtx->collieClient), async_run(_, _, _))
            .WillOnce(Return(getResponse))
            .WillOnce(Return(createResponse));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, ""));
        EXPECT_CALL(*stream, result_body("import contacts error: Internal Server Error"));
        ImportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestImportContactsOp, should_response_ok_with_one_new_contacts) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        std::string contactResponse = importedContacts;
        EXPECT_CALL(*(webCtx->pythonModule), importContacts(_, _, _)).WillOnce(Return(contactResponse));
        yhttp::response getResponse;
        getResponse.status = 200;
        getResponse.body = existingContacts;
        yhttp::response createResponse;
        createResponse.status = 200;
        createResponse.body = R"({"contact_ids": [1], "revision": 1})";
        EXPECT_CALL(*(webCtx->collieClient), async_run(_, _, _))
            .WillOnce(Return(getResponse))
            .WillOnce(Return(createResponse));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, result_body("{\"status\":\"Ok\",\"rec_cnt\":1,\"rec_skipped\":1}"));
        ImportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

}