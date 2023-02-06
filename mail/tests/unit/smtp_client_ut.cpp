#include "fakes/context.h"
#include "mocks/ymod_smtpclient.h"
#include "nsls_test.h"
#include "errors.h"
#include "smtp/client.h"

using namespace testing;
using namespace NNotSoLiteSrv;

namespace ymod_smtpclient {

// Used by EXPECT_CALL() arguments matchers
bool operator==(const MailFrom& lhs, const MailFrom& rhs) {
    return std::tie(lhs.email, lhs.envid) == std::tie(rhs.email, rhs.envid);
}

bool operator==(const RcptTo& lhs, const RcptTo& rhs) {
    return std::tie(lhs.email, lhs.notifyModes) == std::tie(rhs.email, rhs.notifyModes);
}

bool operator==(const AuthData& lhs, const AuthData& rhs) {
    return std::tie(lhs.login, lhs.password, lhs.token, lhs.mechanism)
        == std::tie(rhs.login, rhs.password, rhs.token, rhs.mechanism);
}

bool operator==(const Request& lhs, const Request& rhs) {
    return std::tie(lhs.address, lhs.auth, lhs.mailfrom, lhs.rcpts, *lhs.message)
        == std::tie(rhs.address, rhs.auth, rhs.mailfrom, rhs.rcpts, *rhs.message);
}

bool operator==(const Timeouts& lhs, const Timeouts& rhs) {
    return std::tie(lhs.connectAttempt, lhs.connect, lhs.command, lhs.data)
        == std::tie(rhs.connectAttempt, rhs.connect, rhs.command, rhs.data);
}

bool operator==(const Options& lhs, const Options& rhs) {
    return std::tie(lhs.timeouts, lhs.reuseSession, lhs.dotStuffing, lhs.allowRcptToErrors, lhs.useSsl)
        == std::tie(rhs.timeouts, rhs.reuseSession, rhs.dotStuffing, rhs.allowRcptToErrors, rhs.useSsl);
}

} // namespace ymod_smtpclient

struct TSmtpClientTest: public TNslsTest {
    void SetUp() override {
        Ctx = boost::make_shared<TTaskContext>();

        yplatform::ptree pt;
        pt.add<unsigned int>("attempts", 2);
        pt.add<std::string>("remote", "smtp://localhost:25");
        pt.add<bool>("use_ssl", false);
        Config = std::make_unique<NConfig::TSmtpClient>(pt);

        YSmtpClientMock = std::make_shared<StrictMock<TYmodSmtpClientMock>>();
    }

    void Test(NSmtp::TCallback check) {
        ExpectCallbackCalled(
            std::move(check),
            1,
            std::bind(NSmtp::AsyncSendMessageWithYmodSmtpClient,
                GetContext(),
                *Config,
                From,
                Rcpts,
                "message is here",
                YSmtpClientMock,
                std::placeholders::_1
            )
        );
    }

    TTaskContextPtr Ctx;
    NConfig::TSmtpClientUptr Config;
    std::shared_ptr<TYmodSmtpClientMock> YSmtpClientMock;

    ymod_smtpclient::MailFrom From{"from@ya.ru"};
    std::vector<ymod_smtpclient::RcptTo> Rcpts{{"to@ya.ru"}};

    ymod_smtpclient::Request Request{
        {ymod_smtpclient::SmtpPoint::smtp, "localhost", 25},
        {"from@ya.ru"},
        {{"to@ya.ru"}},
        boost::none,
        std::make_shared<std::string>("message is here")
    };

    ymod_smtpclient::Response Response{
        {{"to@ya.ru", {250, boost::none, "Ok"}}},
        ymod_smtpclient::OneResponse{250, boost::none, "Ok"}
    };
};

TEST_F(TSmtpClientTest, SendSuccess) {
    EXPECT_CALL(*YSmtpClientMock, asyncRun(_, Request, _, _))
        .WillOnce(InvokeArgument<3>(ymod_smtpclient::error::Success, Response));

    Test([](TErrorCode ec, const std::string& resp) {
        EXPECT_FALSE(ec);
        EXPECT_EQ(resp, "Ok");
    });
}

TEST_F(TSmtpClientTest, SendFailedWith5xx) {
    Response.session->replyCode = 541;
    Response.session->data = "user not found";
    EXPECT_CALL(*YSmtpClientMock, asyncRun(_, Request, _, _))
        .WillOnce(InvokeArgument<3>(ymod_smtpclient::error::Success, Response));

    Test([](TErrorCode ec, const std::string& resp) {
        EXPECT_EQ(ec, EError::SmtpPermanentError);
        EXPECT_EQ(resp, "user not found");
    });
}

TEST_F(TSmtpClientTest, SendFailedWithErrorCode) {
    EXPECT_CALL(*YSmtpClientMock, asyncRun(_, Request, _, _))
        .Times(2).WillRepeatedly(InvokeArgument<3>(ymod_smtpclient::error::WriteError, Response));

    Test([](TErrorCode ec, const std::string&) {
        EXPECT_EQ(ec, EError::SmtpTemporaryError);
    });
}

TEST_F(TSmtpClientTest, SendFailedWith4xxRetrySuccess) {
    auto failedResponse = Response;
    failedResponse.session->replyCode = 451;
    failedResponse.session->data = "internal error";
    EXPECT_CALL(*YSmtpClientMock, asyncRun(_, Request, _, _))
        .WillOnce(InvokeArgument<3>(ymod_smtpclient::error::Success, failedResponse))
        .WillOnce(InvokeArgument<3>(ymod_smtpclient::error::Success, Response));

    Test([](TErrorCode ec, const std::string& resp) {
        EXPECT_FALSE(ec);
        EXPECT_EQ(resp, "Ok");
    });
}
