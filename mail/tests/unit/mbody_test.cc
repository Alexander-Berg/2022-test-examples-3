#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/spaniel/service/include/proxy_helpers.h>
#include <mail/http_getter/client/mock/mock.h>
#include <yplatform/reactor.h>


using namespace ::testing;

namespace spaniel::tests {

const std::string SUCCESS_RESPONSE_BODY = R"(
    {
        "bodies": [
            {
                "isAttach": false,
                "hid": "1",
                "transformerResult": {
                    "textTransformerResult": {
                        "hid": "1",
                        "isMain": true,
                        "typeInfo": {
                            "contentType": {
                                "type": "text",
                                "subtype": "html",
                                "params": {
                                    
                                }
                            },
                            "name": "no_name",
                            "nameUriEncoded": "",
                            "fileName": "",
                            "fileExtension": "",
                            "dispositionValue": "attachment",
                            "contentId": "",
                            "contentLocation": "",
                            "dispositionFilename": "",
                            "partClassInfo": {
                                "partClass_": "doc",
                                "isPreviewSupported_": false,
                                "doesBrowserSupport_": false
                            },
                            "length": 55
                        },
                        "isRaw": false,
                        "content": "<div>Hola!</div>\r\n",
                        "lang": "mis",
                        "isTrimmed": false,
                        "divLimitExceeded": false,
                        "contentTransformersResult": {
                            "hid": "1",
                            "uudecodeChunks": [
                                
                            ],
                            "isPhishing": false,
                            "videoLinks": [
                                
                            ]
                        },
                        "afterTykva": false
                    }
                }
            }
        ],
        "attachments": [
            
        ],
        "signatures": [
            
        ],
        "calendars": [
            
        ],
        "passbooks": [
            
        ],
        "info": {
            "stid": "320.mail:4014115614.E69075:311834263849987983554570979756",
            "references": "",
            "inReplyTo": "",
            "messageId": "<14598521549007901@myt6-2fee75662a4f.qloud-c.yandex.net>",
            "filterId": "",
            "personalSpam": "",
            "spam": "1",
            "deliveredTo": "",
            "listUnsubscribe": "",
            "flightDirection": "",
            "liveMail": "",
            "senderDomain": "yandex.ru",
            "signedBy": "yandex.ru",
            "dkimStatus": "pass",
            "collectedRpopId": "",
            "encrypted": true,
            "isSpam": false,
            "dateResult": {
                "timestamp": 1549007901000,
                "userTimestamp": 1549007901000
            },
            "addressesResult": [
                {
                    "direction": "to",
                    "name": "mbodytestdkim",
                    "email": "mbodytestdkim@yandex.ru"
                },
                {
                    "direction": "from",
                    "name": "Def-???-autotests Def-???????-autotests",
                    "email": "mbodytestdkim@yandex.ru"
                }
            ],
            "noReplyResult": {
                "notification": ""
            },
            "dkim": {
                "status": 1,
                "headeri": "@yandex.ru"
            }
        }
    }
)";

struct MbodyTest: public Test {
    std::shared_ptr<yplatform::reactor> reactor;
    http_getter::TypedEndpoint endpoint;
    Request requestType;
    RemoteServiceError defaultError;
    http_getter::ResponseSequencePtr responses;
    http_getter::TypedClientPtr getter;
    HttpArguments args;
    http::headers headers;

    void SetUp() override {
        reactor = std::make_shared<yplatform::reactor>();
        reactor->init(1, 1);

        args.add("uid", "123");
        args.add("mid", "456");

        requestType = Request::mbody;

        defaultError = RemoteServiceError::proxy;

        responses = std::make_shared<StrictMock<http_getter::ResponseSequence>>();
        getter = http_getter::createTypedDummy(responses);
    }

    template<class Fn>
    void spawn(Fn fn) {
        boost::asio::spawn(*reactor->io(), fn);
        reactor->io()->run();
    }

    yamail::expected<SendShareResult> message(boost::asio::yield_context yield) {
        return doProxyGetRequest(args, headers, getter, endpoint, requestType, defaultError, yield);
    }
};

TEST_F(MbodyTest, shouldNotRetryWhenResponseStatusIs5XX) {
    EXPECT_CALL(*responses, get())
        .WillOnce(Return(yhttp::response {.status=500, .body=""}));
    ;

    spawn([=] (boost::asio::yield_context yield) {
        message(yield);
    });
}

TEST_F(MbodyTest, shouldSaveResponseBodyAndResponseCode) {
    EXPECT_CALL(*responses, get())
        .WillOnce(Return(yhttp::response {.status=200, .body=SUCCESS_RESPONSE_BODY}))
        .WillOnce(Return(yhttp::response {.status=499, .body=""}))
        .WillOnce(Return(yhttp::response {.status=500, .body=""}))
        ;
    ;

    spawn([=] (boost::asio::yield_context yield) {
        auto result = message(yield);
        EXPECT_EQ(result->code, 200);
        EXPECT_EQ(result->body, SUCCESS_RESPONSE_BODY);

        result = message(yield);
        EXPECT_EQ(result->code, 499);
        EXPECT_EQ(result->body, "");

        result = message(yield);
        EXPECT_EQ(result->code, 500);
        EXPECT_EQ(result->body, "");
    });
}

}
