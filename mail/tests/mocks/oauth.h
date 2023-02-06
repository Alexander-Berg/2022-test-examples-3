#pragma once

#include <mocks/mock.h>
#include <oauth/oauth.h>

#include <yplatform/repository.h>

namespace yrpopper::mock {

using namespace yrpopper::oauth;

class OauthServiceImpl
    : public OauthService
    , public yrpopper::mock::mock
{
public:
    FutureRefreshTokenData getRefreshToken(
        yplatform::task_context_ptr /*ctx*/,
        const std::string& /*taskId*/)  override
    {
        throw std::runtime_error("not implemented");
    }

    FutureString getAccessToken(
        yplatform::task_context_ptr /*ctx*/,
        const std::string& /*server*/,
        const std::string& /*refreshToken*/) override
        {
            PromiseString prom;
            prom.set("access_token_ph");
            return prom;
        }

    std::string getOauthApplication(const std::string& /*server*/)  override{
        return "oauth-mock";
    }

    void init_mock() override
    {
        auto repo = yplatform::repository::instance_ptr();
        repo->add_service<OauthService>("oauth_module", shared_from_this());

    }
};

}
