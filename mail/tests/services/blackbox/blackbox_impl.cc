#include "../test_with_context.h"
#include "../../mocks.h"

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/services/blackbox/blackbox_impl.h>

namespace sharpei::services::blackbox {

static bool operator ==(const HostedDomain& lhs, const HostedDomain& rhs) {
    return lhs.domid == rhs.domid;
}

} // namespace sharpei::services::blackbox

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::services;
using namespace sharpei::services::blackbox;
using namespace sharpei::tests;

BlackboxConfig makeBlackboxConfig() {
    BlackboxConfig result;
    result.http.options = makeClusterClientOptions();
    return result;
}

struct TestServicesBlackboxImpl : TestWithContext {
    BlackboxConfig config = makeBlackboxConfig();
    const std::shared_ptr<StrictMock<ClusterClientMock>> httpClient = std::make_shared<StrictMock<ClusterClientMock>>();
    const DomainId domainId {42};
    const std::string domainAddress {"ru.ya"};
    const HttpRequest getHostedDomainsByDomainIdRequest = HttpRequest::GET(
        "?method=hosted_domains&format=json&domain_id=42",
        "X-Request-Id: request_id\r\n"
    );
    const HttpRequest getHostedDomainsByAddressRequest = HttpRequest::GET(
        "?method=hosted_domains&format=json&domain=ru.ya",
        "X-Request-Id: request_id\r\n"
    );
    const std::string getHostedDomainsResponseBody {
        R"({
            "hosted_domains" : [
                {
                    "domid": "42",
                    "mx": "0",
                    "default_uid": "0",
                    "ena": "1",
                    "options": "",
                    "master_domain": "",
                    "admin": "10",
                    "domain": "ru.ya",
                    "born_date": "2009-04-05 15:13:11"
                }
            ]
        })"
    };
    HttpResponse getHostedDomainsResponse {200, {}, getHostedDomainsResponseBody, ""};
};

TEST_F(TestServicesBlackboxImpl, get_hosted_domains_by_domain_id_should_call_http_client_async_run) {
    const BlackboxImpl impl(config, httpClient);

    withContext([&] (const auto& context) {
        EXPECT_CALL(*httpClient, async_run(_, getHostedDomainsByDomainIdRequest, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, getHostedDomainsResponse));

        const auto result = impl.getHostedDomains(domainId, context);

        ASSERT_TRUE(result) << result.error().full_message();
        EXPECT_THAT(result.value(), ElementsAre(HostedDomain {domainId}));
    });
}

TEST_F(TestServicesBlackboxImpl, on_error_get_hosted_domains_by_domain_id_should_return_error) {
    const BlackboxImpl impl(config, httpClient);

    withContext([&] (const auto& context) {
        EXPECT_CALL(*httpClient, async_run(_, getHostedDomainsByDomainIdRequest, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::connect_error, getHostedDomainsResponse));

        const auto result = impl.getHostedDomains(domainId, context);

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), ExplainedError(yhttp::errc::connect_error)) << result.error().full_message();
    });
}
 
TEST_F(TestServicesBlackboxImpl, on_invalid_response_get_hosted_domains_by_domain_id_should_return_error) {
    getHostedDomainsResponse.body = "";
 
    const BlackboxImpl impl(config, httpClient);
 
    withContext([&] (const auto& context) {
        EXPECT_CALL(*httpClient, async_run(_, getHostedDomainsByDomainIdRequest, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, getHostedDomainsResponse));
 
        const auto result = impl.getHostedDomains(domainId, context);
 
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), ExplainedError(Error::blackBoxParseError)) << result.error().full_message();
    });
}
 
TEST_F(TestServicesBlackboxImpl, get_hosted_domains_by_domain_id_should_return_error_if_http_status_5xx) {
    getHostedDomainsResponse.status = 500;
 
    const BlackboxImpl impl(config, httpClient);
 
    withContext([&] (const auto& context) {
        Sequence s;
 
        EXPECT_CALL(*httpClient, async_run(_, getHostedDomainsByDomainIdRequest, config.http.options, _))
            .InSequence(s).WillOnce(InvokeArgument<3>(yhttp::errc::success, getHostedDomainsResponse));
 
        const auto result = impl.getHostedDomains(domainId, context);
 
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), ExplainedError(Error::blackBoxHttpError)) << result.error().full_message();
    });
}

TEST_F(TestServicesBlackboxImpl, get_hosted_domains_by_address_should_call_http_client_async_run) {
    const BlackboxImpl impl(config, httpClient);

    withContext([&] (const auto& context) {
        EXPECT_CALL(*httpClient, async_run(_, getHostedDomainsByAddressRequest, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, getHostedDomainsResponse));

        const auto result = impl.getHostedDomains(domainAddress, context);

        ASSERT_TRUE(result) << result.error().full_message();
        EXPECT_THAT(result.value(), ElementsAre(HostedDomain {domainId}));
    });
}

} // namespace
