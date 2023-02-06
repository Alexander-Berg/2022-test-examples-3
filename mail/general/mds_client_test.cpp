#include "mds_client_mock.h"

#include <mail/nwsmtp/src/header_storage.h>
#include <mail/nwsmtp/src/mds/client_impl.h>
#include <mail/nwsmtp/src/mds/error_code.h>
#include <mail/nwsmtp/src/utils.h>
#include <mail/nwsmtp/ut/test_with_spawn.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/asio.hpp>

namespace {

using namespace testing;
using namespace NTesting;
using namespace NNwSmtp;
using namespace NNwSmtp::NMds;

struct TTestMdsClient: TTestWithSpawn {
    const std::shared_ptr<StrictMock<TMdsClientMock>> MdsModuleClient =
        std::make_shared<StrictMock<TMdsClientMock>>();
    const std::shared_ptr<TMdsClient> MdsClient = std::make_shared<TMdsClient>(MdsModuleClient, Io);

    TContextPtr Context = boost::make_shared<TContext>("","","","");
    NNwSmtp::TBuffer MessageWithHeaders = NUtil::MakeSegment("Header-1: 1\r\nHeader-2: 2\r\n\r\nBody");
    NNwSmtp::TBuffer MessageWithoutHeaders = NUtil::MakeSegment("Body");
    NNwSmtp::TBuffer AddedHeaders = NUtil::MakeSegment("AddedHeader-1: 1\r\n");

    template <typename THandler>
    std::string Put(
        THandler handler,
        TPutRequest request
    ) {
        boost::asio::async_completion<
            THandler,
            void(TErrorCode, std::string)
        > init(handler);
        MdsClient->Put(Context, std::move(request), init.completion_handler);
        return init.result.get();
    }

    template <typename THandler>
    std::string Get(
        THandler handler,
        std::string stid
    ) {
        boost::asio::async_completion<
            THandler,
            void(TErrorCode, std::string)
        > init(handler);
        MdsClient->Get(Context, std::move(stid), init.completion_handler);
        return init.result.get();
    }

    TPutRequest MakePutRequest(const NNwSmtp::TBuffer& message, const std::string& uid, bool isSpam) {
        auto [headers, body] = ParseMessage(message);
        return {uid, isSpam, headers, AddedHeaders, body, {}};
    }
};

TEST_F(TTestMdsClient, for_put_request_ended_with_error_should_return_error) {
    WithSpawn([this](boost::asio::yield_context yield) {
        auto putRequest = MakePutRequest(MessageWithHeaders, "1", true);

        EXPECT_CALL(*MdsModuleClient, Put(_, "mail:1", "AddedHeader-1: 1\r\nHeader-1: 1\r\nHeader-2: 2\r\n\r\nBody", ::NMds::NS_SPAM, _))
            .WillOnce(InvokeArgument<4>(ymod_httpclient::http_error::code::ssl_error, std::string {}));

        TErrorCode ec;
        auto result = Put(yield[ec], putRequest);
        ASSERT_TRUE(ec);
        EXPECT_EQ(ec, ymod_httpclient::http_error::code::ssl_error);
    });
}

TEST_F(TTestMdsClient, for_failed_make_message_should_return_error) {
    WithSpawn([this](boost::asio::yield_context yield) {
        auto putRequest = MakePutRequest(MessageWithoutHeaders, "1", true);

        TErrorCode ec;
        auto result = Put(yield[ec], putRequest);
        ASSERT_TRUE(ec);
        EXPECT_EQ(ec, EError::EC_MAKE_MESSAGE);
    });
}

TEST_F(TTestMdsClient, for_successful_put_request_should_return_stid) {
    WithSpawn([this](boost::asio::yield_context yield) {
        auto putRequest = MakePutRequest(MessageWithHeaders, "1", false);

        EXPECT_CALL(*MdsModuleClient, Put(_, "mail:1", "AddedHeader-1: 1\r\nHeader-1: 1\r\nHeader-2: 2\r\n\r\nBody", ::NMds::NS_MAIL, _))
            .WillOnce(InvokeArgument<4>(TErrorCode{}, std::string{"stid"}));

        TErrorCode ec;
        auto result = Put(yield[ec], putRequest);
        ASSERT_FALSE(ec);
        EXPECT_EQ(result, "stid");
    });
}

TEST_F(TTestMdsClient, for_get_request_ended_with_error_should_return_error) {
    WithSpawn([this](boost::asio::yield_context yield) {
        EXPECT_CALL(*MdsModuleClient, Get(_, "stid", _))
            .WillOnce(InvokeArgument<2>(ymod_httpclient::http_error::code::ssl_error, std::string{}));

        TErrorCode ec;
        auto result = Get(yield[ec], "stid");
        ASSERT_TRUE(ec);
        EXPECT_EQ(ec, ymod_httpclient::http_error::code::ssl_error);
    });
}

TEST_F(TTestMdsClient, for_successful_get_request_should_return_message) {
    WithSpawn([this](boost::asio::yield_context yield) {
        EXPECT_CALL(*MdsModuleClient, Get(_, "stid", _))
            .WillOnce(InvokeArgument<2>(TErrorCode{}, std::string{"message"}));

        TErrorCode ec;
        auto result = Get(yield[ec], "stid");
        ASSERT_FALSE(ec);
        EXPECT_EQ(result, "message");
    });
}

} // namespace anonymous
