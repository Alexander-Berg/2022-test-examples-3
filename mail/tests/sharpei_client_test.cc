#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/bind.hpp>
#include <sharpei_client/sharpei_client.h>
#include <sharpei_client/reflection.h>
#include <mail/sharpei_client/src/cached_sharpei_client.h>
#include <yamail/data/deserialization/json_reader.h>

std::ostream& operator<<(std::ostream& o, const sharpei::client::http::Address& addr) {
    return o << addr.host << ":" << addr.port;
}
std::ostream& operator<<(std::ostream& o, sharpei::client::ErrorCode error) {
    return o << "error_code " << error.value() << ": " << error.message();
}

namespace sharpei {
namespace client {

using namespace testing;
using namespace http;
using namespace std::chrono_literals;

class MockHttpClient: public HttpClient {
public:
    MOCK_METHOD(void, aget, (const Address&,
        Timeout,
        const std::string&,
        const Arguments&,
        const Headers&,
        ResponseHandler,
        bool,
        const std::string&), (const, override));

    MOCK_METHOD(void, apost, (const Address&,
        Timeout,
        const std::string&,
        const Arguments&,
        const Headers&,
        const std::string&,
        ResponseHandler,
        bool,
        const std::string&), (const, override));
};

class AsyncHandler {
public:
    MOCK_METHOD(void, handle, (const ErrorCode&, Shard), (const));
};

class AsyncMapHandler {
public:
    MOCK_METHOD(void, handle, (const ErrorCode&, MapShard), (const));
};

class MockSharpeiClient: public SharpeiClient {
public:
    MOCK_METHOD(void, asyncGetConnInfo, (const ResolveParams&, AsyncHandler), (const, override));
    MOCK_METHOD(void, asyncGetDeletedConnInfo, (const ResolveParams&, AsyncHandler), (const, override));
    MOCK_METHOD(void, asyncGetOrgConnInfo, (const ResolveParams&, AsyncHandler), (const, override));
    MOCK_METHOD(void, asyncStat, (AsyncMapHandler), (const, override));
    MOCK_METHOD(void, asyncStatById, (const Shard::Id&, AsyncHandler), (const, override));
};

class SharpeiClientTest: public Test {
public:
    SharpeiClientTest()
        : httpClient_(new MockHttpClient)
    {
        settings_.retries = 2;
        settings_.sharpeiAddress.host = "sharpei.yandex.ru";
        settings_.sharpeiAddress.port = 5555;

        const RequestInfo requestInfo{"request-id", "connection-id", "client-type", "user-ip", "uniq-id"};
        sharpeiClient_ = createSharpeiClient(httpClient_, settings_, requestInfo);
    }
protected:
    std::shared_ptr<MockHttpClient> httpClient_;
    Settings settings_;
    SharpeiClientPtr sharpeiClient_;
    AsyncHandler asyncHandler_;
    AsyncMapHandler asyncMapHandler_;
};

namespace sys = boost::system;

const std::string conninfoResponseStr =
"{"
"   \"databases\" : ["
"      {"
"         \"address\" : {"
"            \"host\" : \"pgload03g.mail.yandex.net\","
"            \"dataCenter\" : \"iva\","
"            \"dbname\" : \"maildb\","
"            \"port\" : \"6432\""
"         },"
"         \"role\" : \"master\","
"         \"state\" : {"
"            \"lag\" : 1"
"         },"
"         \"status\" : \"alive\""
"      },"
"      {"
"         \"address\" : {"
"            \"host\" : \"pgload02g.mail.yandex.net\","
"            \"dataCenter\" : \"sas\","
"            \"dbname\" : \"dbmail\","
"            \"port\" : \"1234\""
"         },"
"         \"state\" : {"
"            \"lag\" : 0"
"         },"
"         \"role\" : \"replica\","
"         \"status\" : \"dead\""
"      }"
"   ],"
"   \"id\" : \"2\","
"   \"name\" : \"second\""
"}";

const std::string statResponseStr =
"{"
"   \"2\": {"
"      \"databases\" : ["
"         {"
"            \"address\" : {"
"               \"host\" : \"pgload03g.mail.yandex.net\","
"               \"dataCenter\" : \"iva\","
"               \"dbname\" : \"maildb\","
"               \"port\" : 6432"
"            },"
"            \"state\" : {"
"                \"lag\" : 1"
"            },"
"            \"role\" : \"master\","
"            \"status\" : \"alive\""
"         },"
"         {"
"            \"address\" : {"
"               \"host\" : \"pgload02g.mail.yandex.net\","
"               \"dataCenter\" : \"sas\","
"               \"dbname\" : \"dbmail\","
"               \"port\" : 1234"
"            },"
"            \"state\" : {"
"                \"lag\" : 0"
"            },"
"            \"role\" : \"replica\","
"            \"status\" : \"dead\""
"         }"
"      ],"
"      \"id\" : 2,"
"      \"name\" : \"second\""
"   }"
"}";


const Shard parsedShard = Shard{"2", "second", {
    Shard::Database{
        Shard::Database::Address{"pgload03g.mail.yandex.net", 6432u, "maildb", "iva"},
        "master", "alive", Shard::Database::State{1ul}
    },
    Shard::Database{
        Shard::Database::Address{"pgload02g.mail.yandex.net", 1234u, "dbmail", "sas"},
        "replica", "dead", Shard::Database::State{0ul}
    }
}};
const MapShard parsedMapShard = MapShard{{"2", parsedShard}};

using boost::fusion::operators::operator==;

TEST(ParseShardJsonTest, parsed_responseStr_should_be_equal_to_parsedResponse) {
    using namespace yamail::data::deserialization;
    ASSERT_TRUE(fromJson<Shard>(conninfoResponseStr) == parsedShard);
}

TEST(ParseStatJsonTest, parsed_statResponseStr_should_be_equal_to_parsedResponse) {
    using namespace yamail::data::deserialization;
    ASSERT_TRUE(fromJson<MapShard>(statResponseStr) == parsedMapShard);
}

TEST_F(SharpeiClientTest,
        asyncGetConnInfo_httpReturnsGoodResponse_sharpeiClientReturnsShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetConnInfo_httpReturnsGoodResponseOnSecondRetry_sharpeiClientReturnsShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{500, ""}))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetConnInfo_httpReturns500MoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/conninfo", _, _, _, _, _))
        .Times(2).WillRepeatedly(InvokeArgument<5>(sys::error_code{}, Response{500, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::SharpeiError), _));
    sharpeiClient_->getConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetConnInfo_httpReturns404_sharpeiClientInvokesHandlerWithErrorUidNotFound) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{404, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::UidNotFound), _));
    sharpeiClient_->getConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetConnInfo_httpReturnsNot2xxAndNot5xxAndNot404_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{403, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::HttpCode), _));
    sharpeiClient_->getConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetConnInfo_httpThrowsExceptionMoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/conninfo", _, _, _, _, _))
        .Times(2).WillRepeatedly(Throw(std::runtime_error("http error")));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::Exception), _));
    sharpeiClient_->getConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetConnInfo_httpThrowsExceptionAndReturnsGoodResponse_sharpeiClientReturnsShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/conninfo", _, _, _, _, _))
        .WillOnce(Throw(std::runtime_error("http error")))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetConnInfo_httpPassesErrorMoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    ErrorCode error(666, getErrorCategory());
    EXPECT_CALL(*httpClient_, aget(_, _, "/conninfo", _, _, _, _, _))
        .Times(2).WillRepeatedly(InvokeArgument<5>(sys::error_code{666, getErrorCategory()}, Response()));
    EXPECT_CALL(asyncHandler_, handle(Eq(error), _));
    sharpeiClient_->getConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetConnInfo_httpPassesErrorAndReturnsGoodResponse_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{666, getErrorCategory()}, Response()))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetDeletedConnInfo_httpReturnsGoodResponse_sharpeiClientReturnsShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/deleted_conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getDeletedConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetDeletedConnInfo_httpReturnsGoodResponseOnSecondRetry_sharpeiClientReturnsShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/deleted_conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{500, ""}))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getDeletedConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetDeletedConnInfo_httpReturns500MoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/deleted_conninfo", _, _, _, _, _))
        .Times(2).WillRepeatedly(InvokeArgument<5>(sys::error_code{}, Response{500, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::SharpeiError), _));
    sharpeiClient_->getDeletedConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetDeletedConnInfo_httpReturns404_sharpeiClientInvokesHandlerWithErrorUidNotFound) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/deleted_conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{404, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::UidNotFound), _));
    sharpeiClient_->getDeletedConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetDeletedConnInfo_httpReturnsNot2xxAndNot5xxAndNot404_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/deleted_conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{403, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::HttpCode), _));
    sharpeiClient_->getDeletedConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetDeletedConnInfo_httpThrowsExceptionMoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/deleted_conninfo", _, _, _, _, _))
        .Times(2).WillRepeatedly(Throw(std::runtime_error("http error")));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::Exception), _));
    sharpeiClient_->getDeletedConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetDeletedConnInfo_httpThrowsExceptionAndReturnsGoodResponse_sharpeiClientReturnsShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/deleted_conninfo", _, _, _, _, _))
        .WillOnce(Throw(std::runtime_error("http error")))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getDeletedConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetDeletedConnInfo_httpPassesErrorMoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    ErrorCode error(666, getErrorCategory());
    EXPECT_CALL(*httpClient_, aget(_, _, "/deleted_conninfo", _, _, _, _, _))
        .Times(2).WillRepeatedly(InvokeArgument<5>(sys::error_code{666, getErrorCategory()}, Response()));
    EXPECT_CALL(asyncHandler_, handle(Eq(error), _));
    sharpeiClient_->getDeletedConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetDeletedConnInfo_httpPassesErrorAndReturnsGoodResponse_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/deleted_conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{666, getErrorCategory()}, Response()))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getDeletedConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetOrgConnInfo_httpReturnsGoodResponse_sharpeiClientReturnsShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/org_conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getOrgConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetOrgConnInfo_httpReturnsGoodResponseOnSecondRetry_sharpeiClientReturnsShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/org_conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{500, ""}))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getOrgConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetOrgConnInfo_httpReturns500MoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/org_conninfo", _, _, _, _, _))
        .Times(2).WillRepeatedly(InvokeArgument<5>(sys::error_code{}, Response{500, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::SharpeiError), _));
    sharpeiClient_->getOrgConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetOrgConnInfo_httpReturns404_sharpeiClientInvokesHandlerWithErrorUidNotFound) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/org_conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{404, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::UidNotFound), _));
    sharpeiClient_->getOrgConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetOrgConnInfo_httpReturnsNot2xxAndNot5xxAndNot404_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/org_conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{403, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::HttpCode), _));
    sharpeiClient_->getOrgConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetOrgConnInfo_httpThrowsExceptionMoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/org_conninfo", _, _, _, _, _))
        .Times(2).WillRepeatedly(Throw(std::runtime_error("http error")));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::Exception), _));
    sharpeiClient_->getOrgConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetOrgConnInfo_httpThrowsExceptionAndReturnsGoodResponse_sharpeiClientReturnsShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/org_conninfo", _, _, _, _, _))
        .WillOnce(Throw(std::runtime_error("http error")))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getOrgConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetOrgConnInfo_httpPassesErrorMoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    ErrorCode error(666, getErrorCategory());
    EXPECT_CALL(*httpClient_, aget(_, _, "/org_conninfo", _, _, _, _, _))
        .Times(2).WillRepeatedly(InvokeArgument<5>(sys::error_code{666, getErrorCategory()}, Response()));
    EXPECT_CALL(asyncHandler_, handle(Eq(error), _));
    sharpeiClient_->getOrgConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncGetOrgConnInfo_httpPassesErrorAndReturnsGoodResponse_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/org_conninfo", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{666, getErrorCategory()}, Response()))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, conninfoResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->getOrgConnInfo(ResolveParams("1"), boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncStat_httpReturnsGoodResponse_sharpeiClientReturnsMapShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, statResponseStr}));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    sharpeiClient_->stat(boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncStat_httpReturnsGoodResponseOnSecondRetry_sharpeiClientReturnsMapShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{500, ""}))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, statResponseStr}));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    sharpeiClient_->stat(boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncStat_httpReturns500MoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", _, _, _, _, _))
        .Times(2).WillRepeatedly(InvokeArgument<5>(sys::error_code{}, Response{500, ""}));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode(Errors::SharpeiError), _));
    sharpeiClient_->stat(boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncStat_httpThrowsExceptionMoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", _, _, _, _, _))
        .Times(2).WillRepeatedly(Throw(std::runtime_error("http error")));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode(Errors::Exception), _));
    sharpeiClient_->stat(boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncStat_httpThrowsExceptionAndReturnsGoodResponse_sharpeiClientReturnsMapShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", _, _, _, _, _))
        .WillOnce(Throw(std::runtime_error("http error")))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, statResponseStr}));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    sharpeiClient_->stat(boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncStat_httpPassesErrorMoreThanRetries_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", _, _, _, _, _))
        .Times(2).WillRepeatedly(InvokeArgument<5>(sys::error_code{666, getErrorCategory()}, Response()));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode(666, getErrorCategory()), _));
    sharpeiClient_->stat(boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncStat_httpPassesErrorAndReturnsGoodResponse_sharpeiClientReturnsMapShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", _, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{666, getErrorCategory()}, Response()))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, statResponseStr}));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    sharpeiClient_->stat(boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(SharpeiClientTest, asyncStatAndShardById_httpReturnsGoodResponse_sharpeiClientReturnsShard) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", Arguments{{"shard_id", std::vector<std::string>{"2"}}}, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, statResponseStr}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    sharpeiClient_->statById("2", boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest,
        asyncStatAndShardById_httpReturnsGoodResponseWithoutRequestedShard_sharpeiClientInvokesHandlerWithError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", Arguments{{"shard_id", std::vector<std::string>{"3"}}}, _, _, _, _))
        .WillOnce(InvokeArgument<5>(sys::error_code{}, Response{200, "{}"}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::ShardNotFound), _));
    sharpeiClient_->statById("3", boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest, asyncStatAndShardById_httpReturns500_sharpeiClientReturnsSharpeiError) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", Arguments{{"shard_id", std::vector<std::string>{"2"}}}, _, _, _, _))
        .WillRepeatedly(InvokeArgument<5>(sys::error_code{}, Response{500, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::SharpeiError), _));
    sharpeiClient_->statById("2", boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(SharpeiClientTest, asyncStatAndShardById_httpReturns404_sharpeiClientReturnsShardNotFound) {
    EXPECT_CALL(*httpClient_, aget(_, _, "/v3/stat", Arguments{{"shard_id", std::vector<std::string>{"2"}}}, _, _, _, _))
        .WillRepeatedly(InvokeArgument<5>(sys::error_code{}, Response{404, ""}));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode(Errors::ShardNotFound), _));
    sharpeiClient_->statById("2", boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

struct NowMock {
    MOCK_METHOD(std::chrono::steady_clock::time_point, getTimePoint, (), (const));
};

using NowPtr = std::shared_ptr<NowMock>;

class CachedSharpeiClientTest: public Test {
public:
    CachedSharpeiClientTest()
            : now_(std::make_shared<NowMock>())
            , time_point_(std::chrono::steady_clock::time_point::min())
            , component_(new MockSharpeiClient)
            , cachedClient_(makeCached(component_))
    {
        auto get_now = [now=now_]() {
            return now->getTimePoint();
        };
        cachedTtlClient_ = std::make_shared<CachedSharpeiClient<decltype(get_now)>>(component_, 1s, get_now);
    }

protected:
    NowPtr now_;
    const std::chrono::steady_clock::time_point time_point_;
    std::shared_ptr<MockSharpeiClient> component_;
    SharpeiClientPtr cachedClient_;
    SharpeiClientPtr cachedTtlClient_;
    AsyncHandler asyncHandler_;
    AsyncMapHandler asyncMapHandler_;
};

TEST_F(CachedSharpeiClientTest,
        asyncGetConnInfo_componentReturnsGoodResponse_useTtlCache) {
    EXPECT_CALL(*now_, getTimePoint()).WillOnce(Return(time_point_));
    EXPECT_CALL(*component_, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getConnInfo(ResolveParams("1"),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
    EXPECT_CALL(*now_, getTimePoint()).WillOnce(Return(time_point_));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getConnInfo(ResolveParams("1", Mode::WriteOnly, false),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetConnInfo_componentReturnsGoodResponse_invalidateTtlCache) {
    EXPECT_CALL(*now_, getTimePoint()).WillOnce(Return(time_point_));
    EXPECT_CALL(*component_, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getConnInfo(ResolveParams("1"),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
    EXPECT_CALL(*now_, getTimePoint())
            .WillOnce(Return(time_point_ + 2s))
            .WillOnce(Return(time_point_ + 3s));
    EXPECT_CALL(*component_, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getConnInfo(ResolveParams("1", Mode::WriteOnly, false),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetConnInfo_componentReturnsGoodResponse_cachedClientCallsComponentOnlyOnce) {
    EXPECT_CALL(*component_, asyncGetConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getConnInfo(ResolveParams("1", Mode::WriteOnly, false),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetConnInfo_forceTrue_ignoreCache) {
    EXPECT_CALL(*component_, asyncGetConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));

    EXPECT_CALL(*component_, asyncGetConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getConnInfo(ResolveParams("1", Mode::WriteOnly, true),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetConnInfo_firstError_setNoCache) {
    ErrorCode error = makeErrorCode(Errors::UidNotFound, "");
    EXPECT_CALL(*component_, asyncGetConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(error, Shard()));
    EXPECT_CALL(asyncHandler_, handle(error, Shard()));
    cachedClient_->getConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));

    EXPECT_CALL(*component_, asyncGetConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getConnInfo(ResolveParams("1", Mode::WriteOnly, false),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetConnInfo_anotherUid_cacheDoesNotHelp) {
    EXPECT_CALL(*component_, asyncGetConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));

    EXPECT_CALL(*component_, asyncGetConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getConnInfo(ResolveParams("2", Mode::WriteOnly, false),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetStat_componentReturnsGoodResponse_useTtlCache) {
    EXPECT_CALL(*now_, getTimePoint()).WillOnce(Return(time_point_));
    EXPECT_CALL(*component_, asyncStat(_))
            .WillOnce(InvokeArgument<0>(ErrorCode{}, parsedMapShard));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    cachedTtlClient_->stat(
            boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
    EXPECT_CALL(*now_, getTimePoint()).WillOnce(Return(time_point_));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    cachedTtlClient_->stat(
            boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetStat_componentReturnsGoodResponse_invalidateTtlCache) {
    EXPECT_CALL(*now_, getTimePoint()).WillOnce(Return(time_point_));
    EXPECT_CALL(*component_, asyncStat(_))
            .WillOnce(InvokeArgument<0>(ErrorCode{}, parsedMapShard));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    cachedTtlClient_->stat(
            boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
    EXPECT_CALL(*now_, getTimePoint())
            .WillOnce(Return(time_point_ + 1s))
            .WillOnce(Return(time_point_ + 2s));
    EXPECT_CALL(*component_, asyncStat(_))
            .WillOnce(InvokeArgument<0>(ErrorCode{}, parsedMapShard));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    cachedTtlClient_->stat(
            boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetStat_componentReturnsGoodResponse_cachedClientCallsComponentOnlyOnce) {
    EXPECT_CALL(*component_, asyncStat(_))
        .WillOnce(InvokeArgument<0>(ErrorCode{}, parsedMapShard));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    cachedClient_->stat(
        boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    cachedClient_->stat(
        boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetStat_error_setNoCache) {
    ErrorCode error = makeErrorCode(Errors::HttpCode, "");
    EXPECT_CALL(*component_, asyncStat(_))
        .WillOnce(InvokeArgument<0>(error, parsedMapShard));
    EXPECT_CALL(asyncMapHandler_, handle(error, parsedMapShard));
    cachedClient_->stat(
        boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));

    EXPECT_CALL(*component_, asyncStat(_))
        .WillOnce(InvokeArgument<0>(ErrorCode{}, parsedMapShard));
    EXPECT_CALL(asyncMapHandler_, handle(ErrorCode{}, parsedMapShard));
    cachedClient_->stat(
        boost::bind(&AsyncMapHandler::handle, boost::ref(asyncMapHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetDeletedConnInfo_componentReturnsGoodResponse_invalidateTtlCache) {
    EXPECT_CALL(*now_, getTimePoint()).WillOnce(Return(time_point_));
    EXPECT_CALL(*component_, asyncGetDeletedConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getDeletedConnInfo(ResolveParams("1"),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
    EXPECT_CALL(*now_, getTimePoint())
            .WillOnce(Return(time_point_ + 1s))
            .WillOnce(Return(time_point_ + 2s));
    EXPECT_CALL(*component_, asyncGetDeletedConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getDeletedConnInfo(ResolveParams("1", Mode::WriteOnly, false),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetDeletedConnInfo_componentReturnsGoodResponse_useTtlCache) {
    EXPECT_CALL(*now_, getTimePoint()).WillOnce(Return(time_point_));
    EXPECT_CALL(*component_, asyncGetDeletedConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getDeletedConnInfo(ResolveParams("1"),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
    EXPECT_CALL(*now_, getTimePoint())
            .WillOnce(Return(time_point_));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getDeletedConnInfo(ResolveParams("1", Mode::WriteOnly, false),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetDeletedConnInfo_componentReturnsGoodResponse_cachedClientCallsComponentOnlyOnce) {
    EXPECT_CALL(*component_, asyncGetDeletedConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getDeletedConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getDeletedConnInfo(ResolveParams("1", Mode::WriteOnly, false),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetDeletedConnInfo_forceTrue_ignoreCache) {
    EXPECT_CALL(*component_, asyncGetDeletedConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getDeletedConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));

    EXPECT_CALL(*component_, asyncGetDeletedConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getDeletedConnInfo(ResolveParams("1", Mode::WriteOnly, true),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetDeletedConnInfo_firstError_setNoCache) {
    ErrorCode error = makeErrorCode(Errors::UidNotFound, "");
    EXPECT_CALL(*component_, asyncGetDeletedConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(error, Shard()));
    EXPECT_CALL(asyncHandler_, handle(error, Shard()));
    cachedClient_->getDeletedConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));

    EXPECT_CALL(*component_, asyncGetDeletedConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getDeletedConnInfo(ResolveParams("1", Mode::WriteOnly, false),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetDeletedConnInfo_anotherUid_cacheDoesNotHelp) {
    EXPECT_CALL(*component_, asyncGetDeletedConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getDeletedConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));

    EXPECT_CALL(*component_, asyncGetDeletedConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getDeletedConnInfo(ResolveParams("2", Mode::WriteOnly, false),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetOrgConnInfo_componentReturnsGoodResponse_invalidateTtlCache) {
    EXPECT_CALL(*now_, getTimePoint()).WillOnce(Return(time_point_));
    EXPECT_CALL(*component_, asyncGetOrgConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getOrgConnInfo(ResolveParams("1"),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
    EXPECT_CALL(*now_, getTimePoint())
            .WillOnce(Return(time_point_ + 1s))
            .WillOnce(Return(time_point_ + 2s));
    EXPECT_CALL(*component_, asyncGetOrgConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getOrgConnInfo(ResolveParams("1", Mode::WriteOnly, false),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetOrgConnInfo_componentReturnsGoodResponse_useTtlCache) {
    EXPECT_CALL(*now_, getTimePoint()).WillOnce(Return(time_point_));
    EXPECT_CALL(*component_, asyncGetOrgConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getOrgConnInfo(ResolveParams("1"),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
    EXPECT_CALL(*now_, getTimePoint())
            .WillOnce(Return(time_point_));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedTtlClient_->getOrgConnInfo(ResolveParams("1", Mode::WriteOnly, false),
            boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetOrgConnInfo_componentReturnsGoodResponse_cachedClientCallsComponentOnlyOnce) {
    EXPECT_CALL(*component_, asyncGetOrgConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getOrgConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getOrgConnInfo(ResolveParams("1", Mode::WriteOnly, false),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetOrgConnInfo_forceTrue_ignoreCache) {
    EXPECT_CALL(*component_, asyncGetOrgConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getOrgConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));

    EXPECT_CALL(*component_, asyncGetOrgConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getOrgConnInfo(ResolveParams("1", Mode::WriteOnly, true),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetOrgConnInfo_firstError_setNoCache) {
    ErrorCode error = makeErrorCode(Errors::UidNotFound, "");
    EXPECT_CALL(*component_, asyncGetOrgConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(error, Shard()));
    EXPECT_CALL(asyncHandler_, handle(error, Shard()));
    cachedClient_->getOrgConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));

    EXPECT_CALL(*component_, asyncGetOrgConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getOrgConnInfo(ResolveParams("1", Mode::WriteOnly, false),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

TEST_F(CachedSharpeiClientTest,
        asyncGetOrgConnInfo_anotherUid_cacheDoesNotHelp) {
    EXPECT_CALL(*component_, asyncGetOrgConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getOrgConnInfo(ResolveParams("1"),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));

    EXPECT_CALL(*component_, asyncGetOrgConnInfo(_, _))
        .WillOnce(InvokeArgument<1>(ErrorCode{}, parsedShard));
    EXPECT_CALL(asyncHandler_, handle(ErrorCode{}, parsedShard));
    cachedClient_->getOrgConnInfo(ResolveParams("2", Mode::WriteOnly, false),
        boost::bind(&AsyncHandler::handle, boost::ref(asyncHandler_), _1, _2));
}

} // namespace
} // namespace
