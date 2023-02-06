#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/akita/service/include/errors.h>
#include <mail/akita/service/include/auth_checker/auth_checker.h>


using namespace ::testing;

namespace akita {

static inline std::ostream& operator <<(std::ostream& stream, const AuthCode& value) {
    switch (value) {
        case AuthCode::notFinished: stream << "notFinished"; break;
        case AuthCode::valid: stream << "valid"; break;
        case AuthCode::needReset: stream << "needReset"; break;
        case AuthCode::noAuth: stream << "noAuth"; break;
        case AuthCode::noMailbox: stream << "noMailbox"; break;
        case AuthCode::internalProblem: stream << "internalProblem"; break;
        case AuthCode::noPassword: stream << "noPassword"; break;
        case AuthCode::userIsBlocked: stream << "userIsBlocked"; break;
        case AuthCode::unallowedOAuthScope: stream << "unallowedOAuthScope"; break;
        case AuthCode::userIsFrozen: stream << "userIsFrozen"; break;
        case AuthCode::wrongGuard: stream << "wrongGuard"; break;
    }
    return stream;
}

}

struct AuthCheckerTest : public ::testing::TestWithParam<bool> {};

INSTANTIATE_TEST_SUITE_P(shouldCheckAuthWithOldAndNewAttribute, AuthCheckerTest, ::testing::Values(true, false));

namespace akita::tests {

struct FakeBlackBox: public BlackBox {
    SessionResponse::Status status_;

    FakeBlackBox(SessionResponse::Status status = SessionResponse::Status_valid)
        : status_(status)
    { }

    void checkSessionSSLAsync(SessionRequestSSL, OnCheck h) const override {
        h(mail_errors::error_code(), getResponse(), nullptr);
    }

    void checkOAuthAsync(OAuthRequest, OnCheck h) const override {
        h(mail_errors::error_code(), getResponse(), nullptr);
    }

    virtual SessionResponse getResponse() const = 0;

    bool isAllowedOAuthScope(const std::string&) const override {
        return false;
    }

    static std::string getGoodSuid() {
        return "TEST_NON_EMPTY_UID_0000001";
    }

    static std::string getBadSuid() {
        return "";
    }
};

struct ResponseOAuthStatus: public FakeBlackBox {
    ResponseOAuthStatus(SessionResponse::Status status = SessionResponse::Status_valid)
        : FakeBlackBox(status)
        , mailStatus(SubscriptionMailStatus::active)
        , isSuidGood(true) {
    }

    ResponseOAuthStatus(SubscriptionMailStatus mailStatus,
                        bool isSuidGood)
            : FakeBlackBox()
            , mailStatus(mailStatus)
            , isSuidGood(isSuidGood) {
    }

    bool isAllowedOAuthScope(const std::string&) const override {
        return true;
    }

    SessionResponse getResponse() const override {
        SessionResponse result;

        result.mailLoginAllowed = true;
        result.status = status_;
        result.serviceUserId = isSuidGood ? getGoodSuid() : getBadSuid();
        result.hasPassword = true;
        result.isOAuth = true;
        result.mailStatus = mailStatus;
        result.oAuthInfo.scopes.push_back("SOME_SCOPE");

        return result;
    }

private:
    SubscriptionMailStatus mailStatus;
    bool isSuidGood;
};

struct ResponseSslStatus: public FakeBlackBox {
    using FakeBlackBox::FakeBlackBox;

    SessionResponse getResponse() const override {
        SessionResponse result;

        result.mailLoginAllowed = true;
        result.status = status_;
        result.serviceUserId = getGoodSuid();
        result.mailStatus = SubscriptionMailStatus::active;
        result.hasPassword = true;

        return result;
    }
};

AuthRequestFn requestSSL() {
    return [](const BlackBox& bb, BlackBox::OnCheck h) {
        bb.checkSessionSSLAsync(SessionRequestSSL(), h);
    };
}
AuthRequestFn requestOAuth() {
    return [](const BlackBox& bb, BlackBox::OnCheck h) {
        bb.checkOAuthAsync(OAuthRequest(), h);
    };
}

void checkAuthCodeFromBB(AuthRequestFn request, std::shared_ptr<FakeBlackBox> blackBox, AuthCode authCodeExpected) {
    AuthResultData retval;
    std::promise<mail_errors::error_code> p;
    auto f = p.get_future();
    checkAuthAsync(blackBox, request, [&p, &retval](mail_errors::error_code e, auto data) {
        auto res = std::move(p);
        if(e.category() == akita::getAuthCategory()) {
            retval = std::move(data);
        }
        res.set_value(e);
    });
    const auto result = f.get();
    EXPECT_EQ(result, authCodeExpected);
}

void checkAuthCodeFromOAuth(AuthRequestFn request, BlackBox::SessionResponse::Status status, AuthCode authCodeExpected) {
    checkAuthCodeFromBB(request, std::make_shared<ResponseOAuthStatus>(status), authCodeExpected);
}

void checkAuthCodeFromSSL(AuthRequestFn request, BlackBox::SessionResponse::Status status, AuthCode authCodeExpected) {
    checkAuthCodeFromBB(request, std::make_shared<ResponseSslStatus>(status), authCodeExpected);
}

TEST_P(AuthCheckerTest, testRequestResponseSSL) {
    checkAuthCodeFromSSL(requestSSL(), BlackBox::SessionResponse::Status_valid, AuthCode::valid);
    checkAuthCodeFromSSL(requestSSL(), BlackBox::SessionResponse::Status_needReset, AuthCode::needReset);
    checkAuthCodeFromSSL(requestSSL(), BlackBox::SessionResponse::Status_expired, AuthCode::noAuth);
    checkAuthCodeFromSSL(requestSSL(), BlackBox::SessionResponse::Status_noAuth, AuthCode::noAuth);
    checkAuthCodeFromSSL(requestSSL(), BlackBox::SessionResponse::Status_disabled, AuthCode::noAuth);
    checkAuthCodeFromSSL(requestSSL(), BlackBox::SessionResponse::Status_invalid, AuthCode::noAuth);
    checkAuthCodeFromSSL(requestSSL(), BlackBox::SessionResponse::Status_wrongGuard, AuthCode::wrongGuard);
}

TEST_P(AuthCheckerTest, testRequestResponseOAuth) {
    checkAuthCodeFromOAuth(requestOAuth(), BlackBox::SessionResponse::Status_valid, AuthCode::valid);
    checkAuthCodeFromOAuth(requestOAuth(), BlackBox::SessionResponse::Status_needReset, AuthCode::needReset);
    checkAuthCodeFromOAuth(requestOAuth(), BlackBox::SessionResponse::Status_expired, AuthCode::noAuth);
    checkAuthCodeFromOAuth(requestOAuth(), BlackBox::SessionResponse::Status_noAuth, AuthCode::noAuth);
    checkAuthCodeFromOAuth(requestOAuth(), BlackBox::SessionResponse::Status_disabled, AuthCode::noAuth);
    checkAuthCodeFromOAuth(requestOAuth(), BlackBox::SessionResponse::Status_invalid, AuthCode::noAuth);
    checkAuthCodeFromOAuth(requestOAuth(), BlackBox::SessionResponse::Status_wrongGuard, AuthCode::wrongGuard);
}

TEST_P(AuthCheckerTest, shouldResponseUserIsBlockedIfMailLoginIsNotAllowed) {
    struct MailLoginDisallowed: public FakeBlackBox {
        using FakeBlackBox::FakeBlackBox;
        SessionResponse getResponse() const override {
            SessionResponse result;
            result.mailLoginAllowed = false;
            result.status = SessionResponse::Status_valid;
            result.mailStatus = SubscriptionMailStatus::active;
            result.serviceUserId = getGoodSuid();
            return result;
        }
    };
    checkAuthCodeFromBB(requestSSL(), std::make_shared<MailLoginDisallowed>(), AuthCode::userIsBlocked);
}

TEST_P(AuthCheckerTest, shouldResponseNoPasswordIfUserIsRegularAndDoesNotHavePassword) {
    struct NoPassword: public FakeBlackBox {
        using FakeBlackBox::FakeBlackBox;
        SessionResponse getResponse() const override {
            SessionResponse result;
            result.mailLoginAllowed = true;
            result.status = SessionResponse::Status_valid;
            result.serviceUserId = getGoodSuid();
            result.hasPassword = false;
            result.isMailish = false;
            result.mailStatus = SubscriptionMailStatus::active;
            return result;
        }
    };
    checkAuthCodeFromBB(requestSSL(), std::make_shared<NoPassword>(), AuthCode::noPassword);
}

TEST_P(AuthCheckerTest, shouldResponseValidIfUserIsMailishAndDoesNotHavePassword) {
    struct Mailish: public ResponseOAuthStatus {
        using ResponseOAuthStatus::ResponseOAuthStatus;
        SessionResponse getResponse() const override {
            SessionResponse result;
            result.mailLoginAllowed = true;
            result.status = status_;
            result.serviceUserId = getGoodSuid();
            result.hasPassword = false;
            result.isOAuth = true;
            result.isMailish = true;
            result.mailStatus = SubscriptionMailStatus::active;
            result.oAuthInfo.scopes.push_back("SOME_SCOPE");
            return result;
        }
    };
    checkAuthCodeFromBB(requestOAuth(), std::make_shared<Mailish>(), AuthCode::valid);
}

TEST_P(AuthCheckerTest, shouldResponseValidIfUserIsSSOAndDoesNotHavePassword) {
    struct SSO: public FakeBlackBox {
        using FakeBlackBox::FakeBlackBox;
        SessionResponse getResponse() const override {
            SessionResponse result;
            result.mailLoginAllowed = true;
            result.status = SessionResponse::Status_valid;
            result.serviceUserId = getGoodSuid();
            result.hasPassword = false;
            result.isMailish = false;
            result.isSSO = true;
            result.mailStatus = SubscriptionMailStatus::active;
            return result;
        }
    };
    checkAuthCodeFromBB(requestSSL(), std::make_shared<SSO>(), AuthCode::valid);
}

TEST_P(AuthCheckerTest, shouldResponseUnallowedOAuthScope) {
    struct UnallowedOAuthScope: public ResponseOAuthStatus {
        using ResponseOAuthStatus::ResponseOAuthStatus;

        bool isAllowedOAuthScope(const std::string&) const override {
            return false;
        }
    };
    checkAuthCodeFromBB(requestOAuth(), std::make_shared<UnallowedOAuthScope>(), AuthCode::unallowedOAuthScope);
}

TEST_P(AuthCheckerTest, shouldResponseUserIsFrozenIfSubscriptionMailStatusIsFrozen) {
    checkAuthCodeFromBB(requestSSL(), std::make_shared<ResponseOAuthStatus>(SubscriptionMailStatus::frozen, true), AuthCode::userIsFrozen);
    checkAuthCodeFromBB(requestSSL(), std::make_shared<ResponseOAuthStatus>(SubscriptionMailStatus::frozen, false), AuthCode::userIsFrozen);
}

TEST_P(AuthCheckerTest, shouldResponseNoMailUserIfSubscriptionMailStatusNoMailUser) {
    checkAuthCodeFromBB(requestSSL(), std::make_shared<ResponseOAuthStatus>(SubscriptionMailStatus::noMailUser, true), AuthCode::noMailbox);
    checkAuthCodeFromBB(requestSSL(), std::make_shared<ResponseOAuthStatus>(SubscriptionMailStatus::noMailUser, false), AuthCode::noMailbox);
}

TEST_P(AuthCheckerTest, shouldResponseValidIfSubscriptionMailStatusActive) {
    checkAuthCodeFromBB(requestSSL(), std::make_shared<ResponseOAuthStatus>(SubscriptionMailStatus::active, true), AuthCode::valid);
    checkAuthCodeFromBB(requestSSL(), std::make_shared<ResponseOAuthStatus>(SubscriptionMailStatus::active, false), AuthCode::valid);
}

TEST_P(AuthCheckerTest, shouldCheckSuidIfSubscriptionMailStatusEmpty) {
    checkAuthCodeFromBB(requestSSL(), std::make_shared<ResponseOAuthStatus>(SubscriptionMailStatus::undefined, true), AuthCode::valid);
    checkAuthCodeFromBB(requestSSL(), std::make_shared<ResponseOAuthStatus>(SubscriptionMailStatus::undefined, false), AuthCode::noMailbox);
}

}
