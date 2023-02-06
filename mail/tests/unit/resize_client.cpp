#include "http_client.hpp"
#include "tvm_client.hpp"
#include "common.hpp"
#include <src/services/resize/resize_client_impl.hpp>
#include <src/services/resize/resize_params_writer.hpp>
#include <http_getter/http_request.h>

namespace retriever {

bool operator ==(const ResizeClient::Image& lhs, const ResizeClient::Image& rhs) {
    return lhs.content == rhs.content && lhs.contentType == rhs.contentType
        && lhs.contentDisposition == rhs.contentDisposition;
}

std::ostream& operator <<(std::ostream& stream, const ResizeClient::Image& value) {
    return stream << "Image {" << value.contentDisposition << ", " << value.contentType << ", " << value.content << "}";
}

} // namespace retriever

namespace {

std::string getFakeServiceTicket(const std::string&) {
    return "service";
}

using namespace testing;
using namespace retriever;

using Response = yhttp::response;
using Request = yhttp::request;
using Timeouts = ymod_httpclient::timeouts;
using Config = ResizeClientImpl::Config;
using GenurlParams = ResizeClient::GenurlParams;
using Image = ResizeClient::Image;
using yamail::data::serialization::toJson;

struct ResizeClientTest : public Test {
    std::shared_ptr<StrictMock<ClusterClientMock>> clusterClient = std::make_shared<StrictMock<ClusterClientMock>>();
    std::shared_ptr<StrictMock<HttpClientMock>> httpClient = std::make_shared<StrictMock<HttpClientMock>>();
    std::shared_ptr<TvmClientMock> tvmClient = std::make_shared<TvmClientMock>();
    GetServiceTicket getServiceTicket;
    const GetClusterClientMock getClusterClient;
    const GetHttpClientMock getHttpClient;
    Config config;
    ymod_httpclient::cluster_call::options genurlOptions;
    yhttp::options getOptions;
    Request genurlRequest;
    Request getRequest;
    const std::string requestId = "requestId";
    GenurlParams params;
    Response genurlResponse;
    Response getResponse;
    std::unique_ptr<const ResizeClient> resizeClient;
    Image image;

    ResizeClientTest() {
        using yamail::data::reflection::applyVisitor;
        using yamail::data::reflection::namedItemTag;
        using Seconds = yplatform::time_traits::seconds;

        config.get.timeouts.connect = Seconds(130);
        config.get.timeouts.total = Seconds(420);
        config.get.retries = 0;

        getServiceTicket = [this](const std::string& name, const std::string&) {
            return tvmClient->invoke(name);
        };

        resizeClient = std::make_unique<const ResizeClientImpl>(getClusterClient, getHttpClient, getServiceTicket, config);

        params.url = "image_url";
        params.width = 42;
        params.height = 13;
        params.crop = true;
        params.noautoorient = true;

        std::ostringstream requestUrl;
        requestUrl << "/genurl?";
        ResizeParamsWriter writer(requestUrl);
        applyVisitor(params, writer, namedItemTag(""));

        auto getterGenurlRequest = http_getter::get(requestUrl.str())
            .headers(http_getter::requestId="requestId", http_getter::serviceTicket="service")
            .make();

        auto getterGetRequest = http_getter::get("url")
            .headers(http_getter::requestId="requestId", http_getter::serviceTicket="service")
            .timeouts(config.get.timeouts.total, config.get.timeouts.connect)
            .make();

        genurlRequest = getterGenurlRequest.request;
        getRequest = getterGetRequest.request;

        genurlOptions = http_getter::toClientOptions<http_getter::ClusterClient>(getterGenurlRequest.options);

        genurlResponse.status = 200;
        genurlResponse.headers.insert({"content-type", "text/plain"});
        genurlResponse.body = "resize_url";

        getOptions = http_getter::toClientOptions<http_getter::HttpClient>(getterGetRequest.options);

        getResponse.status = 200;
        getResponse.body = "content";

        image.content = "content";

        EXPECT_CALL(*tvmClient, invoke("mulcagate")).WillRepeatedly(Invoke(getFakeServiceTicket));
    }
};

TEST_F(ResizeClientTest, genurl_but_http_client_async_run_call_back_with_connect_error_should_throw_exception) {
    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getClusterClient.impl, call()).WillOnce(Return(clusterClient));
        EXPECT_CALL(*clusterClient, async_run(_, genurlRequest, genurlOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::connect_error, genurlResponse));

        EXPECT_THROW(resizeClient->genurl(context, params), std::runtime_error);
    });
}

TEST_F(ResizeClientTest, genurl_with_one_retry_but_http_client_async_run_throws_exception_should_throw_exception) {
    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getClusterClient.impl, call()).WillOnce(Return(clusterClient));
        EXPECT_CALL(*clusterClient, async_run(_, genurlRequest, genurlOptions, _))
            .WillOnce(Throw(std::exception()));

        EXPECT_THROW(resizeClient->genurl(context, params), std::exception);
    });
}

TEST_F(ResizeClientTest, genurl_with_http_status_500_should_throw_exception) {
    genurlResponse.status = 500;

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getClusterClient.impl, call()).WillOnce(Return(clusterClient));
        EXPECT_CALL(*clusterClient, async_run(_, genurlRequest, genurlOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, genurlResponse));

        EXPECT_THROW(resizeClient->genurl(context, params), std::runtime_error);
    });
}

TEST_F(ResizeClientTest, genurl_with_http_status_400_should_throw_exception) {
    genurlResponse.status = 400;

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getClusterClient.impl, call()).WillOnce(Return(clusterClient));
        EXPECT_CALL(*clusterClient, async_run(_, genurlRequest, genurlOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, genurlResponse));

        EXPECT_THROW(resizeClient->genurl(context, params), std::runtime_error);
    });
}

TEST_F(ResizeClientTest, genurl_but_response_without_content_type_should_throw_exception) {
    genurlResponse.headers.erase("content-type");

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getClusterClient.impl, call()).WillOnce(Return(clusterClient));
        EXPECT_CALL(*clusterClient, async_run(_, genurlRequest, genurlOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, genurlResponse));

        EXPECT_THROW(resizeClient->genurl(context, params), std::runtime_error);
    });
}

TEST_F(ResizeClientTest, genurl_but_response_without_content_type_text_plain_should_throw_exception) {
    genurlResponse.headers.find("content-type")->second = "application/json";

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getClusterClient.impl, call()).WillOnce(Return(clusterClient));
        EXPECT_CALL(*clusterClient, async_run(_, genurlRequest, genurlOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, genurlResponse));

        EXPECT_THROW(resizeClient->genurl(context, params), std::runtime_error);
    });
}

TEST_F(ResizeClientTest, genurl_should_return_resize_url) {
    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getClusterClient.impl, call()).WillOnce(Return(clusterClient));
        EXPECT_CALL(*clusterClient, async_run(_, genurlRequest, genurlOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, genurlResponse));

        EXPECT_EQ(resizeClient->genurl(context, params), "resize_url");
    });
}

TEST_F(ResizeClientTest, get_should_return_image_with_content) {
    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, getRequest, getOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, getResponse));

        EXPECT_EQ(resizeClient->get(context, "url"), boost::optional<Image>(image));
    });
}

TEST_F(ResizeClientTest, get_should_return_image_with_content_and_content_type) {
    getResponse.headers.insert({"content-type", "image/jpeg"});

    image.contentType = "image/jpeg";

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, getRequest, getOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, getResponse));

        EXPECT_EQ(resizeClient->get(context, "url"), boost::optional<Image>(image));
    });
}

TEST_F(ResizeClientTest, get_should_return_image_with_content_and_content_disposition) {
    getResponse.headers.insert({"content-disposition", "attachment"});

    image.contentDisposition = "attachment";

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, getRequest, getOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, getResponse));

        EXPECT_EQ(resizeClient->get(context, "url"), boost::optional<Image>(image));
    });
}

TEST_F(ResizeClientTest, get_should_return_image_with_content_and_content_type_and_content_disposition) {
    getResponse.headers.insert({"content-type", "image/jpeg"});
    getResponse.headers.insert({"content-disposition", "attachment"});

    image.contentType = "image/jpeg";
    image.contentDisposition = "attachment";

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, getRequest, getOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, getResponse));

        EXPECT_EQ(resizeClient->get(context, "url"), boost::optional<Image>(image));
    });
}

TEST_F(ResizeClientTest, get_for_response_with_status_not_200_should_return_none) {
    getResponse.status = 500;

    withIoService(requestId, [&] (auto context) {
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(_, getRequest, getOptions, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, getResponse));

        EXPECT_EQ(resizeClient->get(context, "url"), boost::none);
    });
}

} // namespace
