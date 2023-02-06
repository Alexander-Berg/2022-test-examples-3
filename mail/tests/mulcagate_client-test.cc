#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <boost/bind.hpp>
#include <mail_getter/mulcagate/mulcagate_client.h>
#include "logging_mock.h"

namespace mail_getter {
namespace mulcagate {
namespace http {

class HttpClientMock : public HttpClient {
public:
    MOCK_METHOD(void, aget, (const BaseParams&, ResponseHandler), (const, override));
    MOCK_METHOD(void, apost, (const BaseParams&, const std::string&, ResponseHandler), (const, override));
};

using HttpClientMockPtr = std::shared_ptr<HttpClientMock>;
using Seconds = std::chrono::seconds;

bool operator ==(const BaseParams& first, const BaseParams& second) {
    return first.host == second.host && first.port == second.port &&
           first.path == second.path && first.timeout == second.timeout &&
           first.connectTimeout == second.connectTimeout &&
           first.keepAlive == second.keepAlive &&
           first.tvmTicket.uid() == second.tvmTicket.uid() &&
           first.tvmTicket.ticket() == second.tvmTicket.ticket() &&
           first.requestId == second.requestId &&
           first.headers.format() == second.headers.format() &&
           first.args.format() == second.args.format();
}

std::ostream& operator<<(std::ostream& o, const BaseParams& params) {
    return o << params.host << ":" << params.port << params.path << "?" << params.args.format()
            << "; connectTimeout=" << params.connectTimeout.count()
            << "Ms, timeout=" << params.timeout.count() << "Ms, requestId=" << params.requestId
            << ", keepAlive=" << params.keepAlive
            << ", tvmTicket=" << params.tvmTicket.uid() << "/" << params.tvmTicket.ticket()
            << ", headers=" << params.headers.format();
}

} // namespace http

class AsyncHandlerMock {
public:
    MOCK_METHOD(void, handle, (error_code, std::string), (const));
};

} // namespace mulcagate
} // namesopace mail_getter

namespace {

using namespace ::testing;
using namespace ::mail_getter;
using namespace ::mail_getter::mulcagate;
using namespace ::mail_getter::mulcagate::http;
using Milliseconds = std::chrono::milliseconds;

struct MulcagateClientTest : public Test {
    SettingsPtr settings;
    HttpClientMockPtr httpMock;
    logging::LogMockPtr logger;
    MulcagateClientPtr client;
    AsyncHandlerMock asyncHandler;

    MulcagateClientTest()
            : settings(std::make_shared<Settings>()),
              httpMock(std::make_shared<HttpClientMock>()),
              logger(std::make_shared<logging::LogMock>()),
              client(std::make_shared<MulcagateClient>(settings, httpMock, logger,
                      tvm::Ticket().setUid("uid").setTicket("ticket"),
                      "test_request_id")) {
        settings->retries = 2;
        settings->storageNameSpace = "storageNameSpace1";
        settings->service = "service1";
        settings->host = "mulcagate.ru";
        settings->port = 1111;
        settings->connectTimeoutMs = 10;
        settings->getTimeoutMs = 100;
        settings->putTimeoutMs = 200;
        settings->keepAlive = false;
    }
};

BaseParams defaultRequestParams() {
    tvm::Ticket tvmTicket(tvm::Ticket().setUid("uid").setTicket("ticket"));
    return {"mulcagate.ru", 1111, "", Milliseconds(10), Milliseconds(100), false,
            {{}}, {{}}, tvmTicket, "test_request_id"};
}


BaseParams getWholeRequestParams() {
    auto requestParams =  defaultRequestParams();
    requestParams.path = "/gate/get/1.1.1";
    requestParams.args = {{ {"service", {"service1"}} }};
    return requestParams;
}

TEST_F(MulcagateClientTest, getWhole_withKeepAliveSettings_clientCallsAgetWithKeepAlive) {
    auto requestParams = getWholeRequestParams();
    settings->keepAlive = true;
    requestParams.keepAlive = true;

    EXPECT_CALL(*httpMock, aget(requestParams, _)).WillOnce(
            InvokeArgument<1>(error_code{}, Response{200, "whole_message"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "whole_message"));

    client->getWhole("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getWhole_withRetries_httpReturns200_clientInvokesHandlerWithRequestedData) {
    auto requestParams = getWholeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _)).WillOnce(
            InvokeArgument<1>(error_code{}, Response{200, "whole_message"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "whole_message"));

    client->getWhole("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getWhole_withRetries_httpReturns500_then_httpReturns200_clientInvokesHandlerWithRequestedData) {
    auto requestParams = getWholeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{500, ""}))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{200, "whole_message"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "whole_message"));

    client->getWhole("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getWhole_withRetries_httpReturnsError_then_httpReturns200_clientInvokesHandlerWithRequestedData) {
    auto requestParams = getWholeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{Errors::exception}, Response{}))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{200, "whole_message"}));
    EXPECT_CALL(*logger, notice("mulcagate", "retrying request to mulcagate due to error: exception was caught"));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "whole_message"));

    client->getWhole("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getWhole_withRetries_httpReturns500MoreThanRetries_clientInvokesHandlerWithInternalError) {
    auto requestParams = getWholeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .Times(2).WillRepeatedly(InvokeArgument<1>(error_code{}, Response{500, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::internal}, _));

    client->getWhole("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getWhole_withRetries_httpReturnsErrorMoreThanRetries_clientInvokesHandlerWithThisError) {
    auto requestParams = getWholeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .Times(2).WillRepeatedly(InvokeArgument<1>(error_code{Errors::exception}, Response{}));
    EXPECT_CALL(*logger, notice("mulcagate", "retrying request to mulcagate due to error: exception was caught")).Times(2);
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::exception}, _));

    client->getWhole("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getWhole_withRetries_httpReturns404_clientInvokesHandlerWithDataNotFoundError) {
    auto requestParams = getWholeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{404, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::dataNotFound}, _));

    client->getWhole("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getWhole_withRetries_httpReturnsNot2xxAndNot5xxAndNot404_clientInvokesHandlerWithHttpCodeError) {
    auto requestParams = getWholeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{309, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::httpCode}, _));

    client->getWhole("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getWhole_withRetries_httpThrowsException_clientInvokesHandlerWithError) {
    auto requestParams = getWholeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(Throw(std::runtime_error("http error")));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::exception}, _));

    client->getWhole("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}


BaseParams getByRangeRequestParams() {
    auto requestParams =  defaultRequestParams();
    requestParams.path = "/gate/get/1.1.1";
    requestParams.args = {{ {"service", {"service1"}} }};
    requestParams.headers = {{ {"Range", {"bytes=200-300"}} }};
    return requestParams;
}

TEST_F(MulcagateClientTest, getByRange_withKeepAliveSettings_clientCallsAgetWithKeepAlive) {
    auto requestParams = getByRangeRequestParams();
    settings->keepAlive = true;
    requestParams.keepAlive = true;

    EXPECT_CALL(*httpMock, aget(requestParams, _)).WillOnce(
            InvokeArgument<1>(error_code{}, Response{206, "message_range"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "message_range"));

    client->getByRange("1.1.1", {200, 300}, boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getByRange_withRetries_httpReturns206_clientInvokesHandlerWithRequestedData) {
    auto requestParams = getByRangeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _)).WillOnce(
            InvokeArgument<1>(error_code{}, Response{206, "message_range"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "message_range"));

    client->getByRange("1.1.1", {200, 300}, boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getByRange_withRetries_httpReturns500_then_httpReturns206_clientInvokesHandlerWithRequestedData) {
    auto requestParams = getByRangeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{500, ""}))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{206, "message_range"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "message_range"));

    client->getByRange("1.1.1", {200, 300}, boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getByRange_withRetries_httpReturnsError_then_httpReturns206_clientInvokesHandlerWithRequestedData) {
    auto requestParams = getByRangeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{Errors::exception}, Response{}))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{206, "message_range"}));
    EXPECT_CALL(*logger, notice("mulcagate", "retrying request to mulcagate due to error: exception was caught"));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "message_range"));

    client->getByRange("1.1.1", {200, 300}, boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getByRange_withRetries_httpReturns500MoreThanRetries_clientInvokesHandlerWithInternalError) {
    auto requestParams = getByRangeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .Times(2).WillRepeatedly(InvokeArgument<1>(error_code{}, Response{500, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::internal}, _));

    client->getByRange("1.1.1", {200, 300}, boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getByRange_withRetries_httpReturnsErrorMoreThanRetries_clientInvokesHandlerWithThisError) {
    auto requestParams = getByRangeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .Times(2).WillRepeatedly(InvokeArgument<1>(error_code{Errors::exception}, Response{}));
    EXPECT_CALL(*logger, notice("mulcagate", "retrying request to mulcagate due to error: exception was caught")).Times(2);
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::exception}, _));

    client->getByRange("1.1.1", {200, 300}, boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getByRange_withRetries_httpReturns404_clientInvokesHandlerWithDataNotFoundError) {
    auto requestParams = getByRangeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{404, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::dataNotFound}, _));

    client->getByRange("1.1.1", {200, 300}, boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getByRange_withRetries_httpReturns416_clientInvokesHandlerWithBadRangeError) {
    auto requestParams = getByRangeRequestParams();
    requestParams.headers = {{ {"Range", {"bytes=200-150"}} }};

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{416, "ERROR_BAD_RANGE"}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::badRange}, _));

    client->getByRange("1.1.1", {200, 150}, boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getByRange_withRetries_httpReturnsNot2xxAndNot5xxAndNot404AndNot416_clientInvokesHandlerWithHttpCodeError) {
    auto requestParams = getByRangeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{309, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::httpCode}, _));

    client->getByRange("1.1.1", {200, 300}, boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getByRange_withRetries_httpThrowsException_clientInvokesHandlerWithError) {
    auto requestParams = getByRangeRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(Throw(std::runtime_error("http error")));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::exception}, _));

    client->getByRange("1.1.1", {200, 300}, boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}


BaseParams getXmlRequestParams() {
    auto requestParams =  defaultRequestParams();
    requestParams.path = "/gate/get/1.1.1";
    requestParams.args = {{ {"gettype", {"xml"}}, {"service", {"service1"}} }};
    return requestParams;
}

TEST_F(MulcagateClientTest, getXml_withKeepAliveSettings_clientCallsAgetWithKeepAlive) {
    auto requestParams = getXmlRequestParams();
    settings->keepAlive = true;
    requestParams.keepAlive = true;

    EXPECT_CALL(*httpMock, aget(requestParams, _)).WillOnce(
            InvokeArgument<1>(error_code{}, Response{200, "xml_data"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "xml_data"));

    client->getXml("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getXml_withRetries_httpReturns200_clientInvokesHandlerWithRequestedData) {
    auto requestParams = getXmlRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _)).WillOnce(
            InvokeArgument<1>(error_code{}, Response{200, "xml_data"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "xml_data"));

    client->getXml("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getXml_withRetries_httpReturns500_then_httpReturns200_clientInvokesHandlerWithRequestedData) {
    auto requestParams = getXmlRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{500, ""}))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{200, "xml_data"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "xml_data"));

    client->getXml("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getXml_withRetries_httpReturnsError_then_httpReturns200_clientInvokesHandlerWithRequestedData) {
    auto requestParams = getXmlRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{Errors::exception}, Response{}))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{200, "xml_data"}));
    EXPECT_CALL(*logger, notice("mulcagate", "retrying request to mulcagate due to error: exception was caught"));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "xml_data"));

    client->getXml("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getXml_withRetries_httpReturns500MoreThanRetries_clientInvokesHandlerWithInternalError) {
    auto requestParams = getXmlRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .Times(2).WillRepeatedly(InvokeArgument<1>(error_code{}, Response{500, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::internal}, _));

    client->getXml("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getXml_withRetries_httpReturnsErrorMoreThanRetries_clientInvokesHandlerWithThisError) {
    auto requestParams = getXmlRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .Times(2).WillRepeatedly(InvokeArgument<1>(error_code{Errors::exception}, Response{}));
    EXPECT_CALL(*logger, notice("mulcagate", "retrying request to mulcagate due to error: exception was caught")).Times(2);
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::exception}, _));

    client->getXml("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getXml_withRetries_httpReturns404_clientInvokesHandlerWithDataNotFoundError) {
    auto requestParams = getXmlRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{404, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::dataNotFound}, _));

    client->getXml("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getXml_withRetries_httpReturnsNot2xxAndNot5xxAndNot404_clientInvokesHandlerWithHttpCodeError) {
    auto requestParams = getXmlRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(InvokeArgument<1>(error_code{}, Response{309, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::httpCode}, _));

    client->getXml("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, getXml_withRetries_httpThrowsException_clientInvokesHandlerWithError) {
    auto requestParams = getXmlRequestParams();

    EXPECT_CALL(*httpMock, aget(requestParams, _))
            .WillOnce(Throw(std::runtime_error("http error")));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::exception}, _));

    client->getXml("1.1.1", boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}


BaseParams putDataRequestParams() {
    auto requestParams =  defaultRequestParams();
    requestParams.path = "/gate/put/tmp";
    requestParams.args = {{ {"elliptics", {"1"}}, {"expire", {"300s"}},
            {"service", {"service1"}}, {"ns", {"storageNameSpace1"}} }};
    requestParams.timeout = Milliseconds(200);
    return requestParams;
}

TEST_F(MulcagateClientTest, putData_withKeepAliveSettings_clientCallsApostWithKeepAlive) {
    auto requestParams = putDataRequestParams();
    settings->keepAlive = true;
    requestParams.keepAlive = true;

    EXPECT_CALL(*httpMock, apost(requestParams, std::string("attach_data"), _)).WillOnce(
            InvokeArgument<2>(error_code{}, Response{200, "tmp_stid"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "tmp_stid"));

    client->putData("tmp", "attach_data", Seconds(300),
            boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, putData_withRetries_httpReturns200_clientInvokesHandlerWithRequestedData) {
    auto requestParams = putDataRequestParams();

    EXPECT_CALL(*httpMock, apost(requestParams, std::string("attach_data"), _)).WillOnce(
            InvokeArgument<2>(error_code{}, Response{200, "tmp_stid"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "tmp_stid"));

    client->putData("tmp", "attach_data", Seconds(300),
            boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, putData_withRetries_httpReturns500_then_httpReturns200_clientInvokesHandlerWithRequestedData) {
    auto requestParams = putDataRequestParams();

    EXPECT_CALL(*httpMock, apost(requestParams, "attach_data", _))
            .WillOnce(InvokeArgument<2>(error_code{}, Response{500, ""}))
            .WillOnce(InvokeArgument<2>(error_code{}, Response{200, "tmp_stid"}));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "tmp_stid"));

    client->putData("tmp", "attach_data", Seconds(300),
            boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, putData_withRetries_httpReturnsError_then_httpReturns200_clientInvokesHandlerWithRequestedData) {
    auto requestParams = putDataRequestParams();

    EXPECT_CALL(*httpMock, apost(requestParams, "attach_data", _))
            .WillOnce(InvokeArgument<2>(error_code{Errors::exception}, Response{}))
            .WillOnce(InvokeArgument<2>(error_code{}, Response{200, "tmp_stid"}));
    EXPECT_CALL(*logger, notice("mulcagate", "retrying request to mulcagate due to error: exception was caught"));
    EXPECT_CALL(asyncHandler, handle(error_code{}, "tmp_stid"));

    client->putData("tmp", "attach_data", Seconds(300),
            boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, putData_withRetries_httpReturns500MoreThanRetries_clientInvokesHandlerWithInternalError) {
    auto requestParams = putDataRequestParams();

    EXPECT_CALL(*httpMock, apost(requestParams, "attach_data", _))
            .Times(2).WillRepeatedly(InvokeArgument<2>(error_code{}, Response{500, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::internal}, _));

    client->putData("tmp", "attach_data", Seconds(300),
            boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, putData_withRetries_httpReturnsErrorMoreThanRetries_clientInvokesHandlerWithThisError) {
    auto requestParams = putDataRequestParams();

    EXPECT_CALL(*httpMock, apost(requestParams, "attach_data", _))
            .Times(2).WillRepeatedly(InvokeArgument<2>(error_code{Errors::exception}, Response{}));
    EXPECT_CALL(*logger, notice("mulcagate", "retrying request to mulcagate due to error: exception was caught")).Times(2);
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::exception}, _));

    client->putData("tmp", "attach_data", Seconds(300),
            boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, putData_withRetries_httpReturns404_clientInvokesHandlerWithDataNotFoundError) {
    auto requestParams = putDataRequestParams();

    EXPECT_CALL(*httpMock, apost(requestParams, "attach_data", _))
            .WillOnce(InvokeArgument<2>(error_code{}, Response{404, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::dataNotFound}, _));

    client->putData("tmp", "attach_data", Seconds(300),
            boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, putData_withRetries_httpReturnsNot2xxAndNot5xxAndNot404_clientInvokesHandlerWithHttpCodeError) {
    auto requestParams = putDataRequestParams();

    EXPECT_CALL(*httpMock, apost(requestParams, "attach_data", _))
            .WillOnce(InvokeArgument<2>(error_code{}, Response{309, ""}));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::httpCode}, _));

    client->putData("tmp", "attach_data", Seconds(300),
            boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

TEST_F(MulcagateClientTest, putData_withRetries_httpThrowsException_clientInvokesHandlerWithError) {
    auto requestParams = putDataRequestParams();

    EXPECT_CALL(*httpMock, apost(requestParams, "attach_data", _))
            .WillOnce(Throw(std::runtime_error("http error")));
    EXPECT_CALL(asyncHandler, handle(error_code{Errors::exception}, _));

    client->putData("tmp", "attach_data", Seconds(300),
            boost::bind(&AsyncHandlerMock::handle, boost::ref(asyncHandler), _1, _2));
}

}
