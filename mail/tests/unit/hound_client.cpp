#include "http_client.hpp"
#include "tvm_client.hpp"
#include "common.hpp"
#include <src/services/hound/hound_error.hpp>
#include <src/services/hound/reflection/error.hpp>
#include <src/services/hound/reflection/mimes.hpp>
#include <macs/mime_part_factory.h>
#include <http_getter/http_request.h>

BOOST_FUSION_DEFINE_STRUCT((retriever), HoundMimes,
    (boost::optional<retriever::hound::Mimes>, mimes)
    (boost::optional<retriever::hound::ErrorStruct>, error)
)

namespace retriever {
namespace hound {

bool operator ==(const RootMessagePart& lhs, const RootMessagePart& rhs) {
    return lhs.stid == rhs.stid && lhs.mimeParts == rhs.mimeParts;
}

bool operator ==(const StidHidMessagePart& lhs, const StidHidMessagePart& rhs) {
    return lhs.stidHid == rhs.stidHid && lhs.mimePart == rhs.mimePart;
}

bool operator ==(const MessageParts& lhs, const MessageParts& rhs) {
    return lhs.root == rhs.root && lhs.other == rhs.other;
}

} // namespace hound
} // namespace retriever

namespace {

std::string getFakeServiceTicket(const std::string&) {
    return "service";
}

using namespace testing;
using namespace retriever;
using namespace hound;
using namespace macs;

using Response = yhttp::response;
using Request = yhttp::request;
using Options = yhttp::options;
using Timeouts = HoundClientImpl::Timeouts;
using Config = HoundClientImpl::Config;
using yamail::data::serialization::toJson;
using http_getter::operator""_arg;

struct HoundClientTest : public Test {
    std::shared_ptr<HttpClientMock> httpClient = std::make_shared<HttpClientMock>();
    std::shared_ptr<TvmClientMock> tvmClient = std::make_shared<TvmClientMock>();
    GetServiceTicket getServiceTicket;
    const GetHttpClientMock getHttpClient;
    Config config;
    Options options;
    Request request;
    const Uid uid = "42";
    const Mid mid = "13";
    const std::string requestId = "requestId";
    const MimePart rootMimePart1 = MimePartFactory().hid("root.hid.1").release();
    const MimePart rootMimePart2 = MimePartFactory().hid("root.hid.2").release();
    const MimePart otherMimePart1 = MimePartFactory().hid("other.hid.1").release();
    const MimePart otherMimePart2 = MimePartFactory().hid("other.hid.2").release();
    const MessageParts messageParts = MessageParts {
        RootMessagePart {
            Stid("root.stid"),
            StidHidMimeParts {{rootMimePart1.hid(), rootMimePart1}, {rootMimePart2.hid(), rootMimePart2}}
        },
        StidMessageParts({
            {
                Stid("other.stid.1"),
                StidHidMessageParts {{otherMimePart1.hid(), StidHidMessagePart {"1", otherMimePart1}}}
            },
            {
                Stid("other.stid.2"),
                StidHidMessageParts {{otherMimePart2.hid(), StidHidMessagePart {"1", otherMimePart2}}}
            },
        })
    };
    Response response;
    const HoundMimes mimes = HoundMimes {
        Mimes {{mid, messageParts}},
        boost::none,
    };
    const HoundMimes retriableError = HoundMimes {
        boost::none,
        ErrorStruct {int(HoundError::internal), "", ""},
    };
    const HoundMimes notRetriableError = HoundMimes {
        boost::none,
        ErrorStruct {int(HoundError::notInitialized), "", ""},
    };

    std::unique_ptr<const HoundClient> houndClient;

    HoundClientTest() {
        using Seconds = yplatform::time_traits::seconds;

        config.location = "http://hound";
        config.timeouts.connect = Seconds(13);
        config.timeouts.total = Seconds(42);
        config.retries = 0;
        config.service_name = "hound2";

        getServiceTicket = [this](const std::string& name, const std::string&) {
            return this->tvmClient->invoke(name);
        };

        houndClient = std::make_unique<const HoundClientImpl>(getHttpClient, getServiceTicket, config);

        auto getterRequest = http_getter::get(config.location + "/mimes")
            .headers(http_getter::requestId="requestId", http_getter::serviceTicket="service")
            .getArgs("uid"_arg=uid, "mid"_arg=mid)
            .timeouts(config.timeouts)
            .make();

        request = std::move(getterRequest.request);
        options = http_getter::toClientOptions<http_getter::HttpClient>(getterRequest.options);

        response.status = 200;
        response.headers.insert({"content-type", "application/json"});
        response.body = toJson(mimes).str();

        EXPECT_CALL(*tvmClient, invoke("hound2")).WillRepeatedly(Invoke(getFakeServiceTicket));
    }
};

TEST_F(HoundClientTest, get_message_parts_then_http_client_async_run_call_back_with_connect_error_should_throw_exception) {
    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::connect_error, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_with_one_retry_then_http_client_async_run_call_back_with_connect_error_should_retry_once_and_throw_exception) {
    config.retries = 1;
    houndClient = std::make_unique<const HoundClientImpl>(getHttpClient, getServiceTicket, config);

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .Times(2).WillRepeatedly(InvokeArgument<3>(yhttp::errc::connect_error, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_with_one_retry_then_http_client_async_run_throws_exception_should_throw_exception) {
    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(Throw(std::exception()));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::exception);
    });
}

TEST_F(HoundClientTest, get_message_parts_with_http_status_500_should_throw_exception) {
    response.status = 500;

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_with_one_retry_and_http_status_500_should_retry_once_and_throw_exception) {
    config.retries = 1;
    houndClient = std::make_unique<const HoundClientImpl>(getHttpClient, getServiceTicket, config);
    response.status = 500;

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .Times(2).WillRepeatedly(InvokeArgument<3>(yhttp::errc::success, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_with_one_retry_and_http_status_400_should_throw_exception) {
    config.retries = 1;
    houndClient = std::make_unique<const HoundClientImpl>(getHttpClient, getServiceTicket, config);
    response.status = 400;

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_but_response_without_content_type_should_throw_exception) {
    response.headers.erase("content-type");

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_but_response_without_content_type_application_json_should_throw_exception) {
    response.headers.find("content-type")->second = "text/plain";

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_but_response_with_only_error_should_throw_exception) {
    response.body = toJson(retriableError).str();

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_with_one_retry_but_response_with_only_retrieable_error_should_retry_once_and_throw_exception) {
    config.retries = 1;
    houndClient = std::make_unique<const HoundClientImpl>(getHttpClient, getServiceTicket, config);
    response.body = toJson(retriableError).str();

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .Times(2).WillRepeatedly(InvokeArgument<3>(yhttp::errc::success, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_with_one_retry_but_response_with_only_not_retrieable_error_should_throw_exception) {
    response.body = toJson(notRetriableError).str();

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_but_response_without_mimes_nor_error_should_throw_exception) {
    response.body = "{}";

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

TEST_F(HoundClientTest, get_message_parts_but_response_without_message_part_for_mid_should_return_none) {
    response.body = toJson(HoundMimes {Mimes {}, boost::none}).str();

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        const OptMessageParts result = houndClient->getMessageParts(context, uid, mid);

        EXPECT_EQ(result, OptMessageParts());
    });
}

TEST_F(HoundClientTest, get_message_parts_should_return_message_parts) {
    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, request, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));


        const OptMessageParts result = houndClient->getMessageParts(context, uid, mid);

        EXPECT_EQ(result, OptMessageParts(messageParts));
    });
}

TEST_F(HoundClientTest, get_message_parts_should_throw_an_exception_in_case_of_tvm_client_error) {
    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*tvmClient, invoke("hound2")).WillOnce(Invoke([](const std::string&) -> std::string {
            throw std::runtime_error("");
        }));

        EXPECT_THROW(houndClient->getMessageParts(context, uid, mid), std::runtime_error);
    });
}

} // namespace
