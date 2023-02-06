#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/spaniel/service/include/proxy_helpers.h>
#include <mail/http_getter/client/mock/mock.h>
#include <yplatform/reactor.h>


using namespace ::testing;

namespace spaniel::tests {

const std::string SUCCESS_RESPONSE_BODY = R"(
    {
        "messageId" : "<1460761631260748@iva4-5cef92ade846.qloud-c.yandex.net>",
        "limited" : [
        
        ]
    }
)";

struct SendbernarTest: public Test {
    std::shared_ptr<yplatform::reactor> reactor;
    http_getter::TypedEndpoint endpoint;
    Request requestType;
    RemoteServiceError defaultError;
    http_getter::ResponseSequencePtr responses;
    http_getter::TypedClientPtr getter;
    HttpArguments args;
    http::headers headers;
    std::string body;

    void SetUp() override {
        reactor = std::make_shared<yplatform::reactor>();
        reactor->init(1, 1);

        args.add("uid", "12345");
        args.add("to", "sendshare.test@yandex.ru");

        body = "body";

        requestType = Request::sendbernar;

        defaultError = RemoteServiceError::proxy;

        responses = std::make_shared<StrictMock<http_getter::ResponseSequence>>();
        getter = http_getter::createTypedDummy(responses);
    }

    template<class Fn>
    void spawn(Fn fn) {
        boost::asio::spawn(*reactor->io(), fn);
        reactor->io()->run();
    }

    yamail::expected<SendShareResult> sendShare(boost::asio::yield_context yield) {
        return doProxyPostRequest(args, headers, body, getter, endpoint, requestType, defaultError, yield);
    }
};

TEST_F(SendbernarTest, shouldNotRetryWhenResponseStatusIs5XX) {
    EXPECT_CALL(*responses, get())
        .WillOnce(Return(yhttp::response {.status=500, .body=""}));
    ;

    spawn([=] (boost::asio::yield_context yield) {
        sendShare(yield);
    });
}

TEST_F(SendbernarTest, shouldSaveResponseBodyAndResponseCode) {
    EXPECT_CALL(*responses, get())
        .WillOnce(Return(yhttp::response {.status=200, .body=SUCCESS_RESPONSE_BODY}))
        .WillOnce(Return(yhttp::response {.status=499, .body=""}))
        .WillOnce(Return(yhttp::response {.status=500, .body=""}))
        ;
    ;

    spawn([=] (boost::asio::yield_context yield) {
        auto result = sendShare(yield);
        EXPECT_EQ(result->code, 200);
        EXPECT_EQ(result->body, SUCCESS_RESPONSE_BODY);

        result = sendShare(yield);
        EXPECT_EQ(result->code, 499);
        EXPECT_EQ(result->body, "");

        result = sendShare(yield);
        EXPECT_EQ(result->code, 500);
        EXPECT_EQ(result->body, "");
    });
}

}
