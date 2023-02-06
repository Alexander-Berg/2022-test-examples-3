#include <mail/notsolitesrv/tests/unit/fakes/context.h>
#include <mail/notsolitesrv/tests/unit/mocks/msettings_client.h>

#include <mail/notsolitesrv/src/new_emails/autoreplies.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

BOOST_FUSION_ADAPT_STRUCT(NNotSoLiteSrv::NMSettings::TParamsRequest,
    Uid,
    Params
)

using namespace testing;
using namespace NNotSoLiteSrv;

namespace NNotSoLiteSrv::NMSettings {

using boost::fusion::operators::operator==;
}
using NNotSoLiteSrv::NNewEmails::TAutoRepliesDataProvider;
using TMSettingsClientMock = NNotSoLiteSrv::NMSettings::TMSettingsClientMock;
using TParamsRequest = NNotSoLiteSrv::NMSettings::TParamsRequest;
using TParamsResponse = NNotSoLiteSrv::NMSettings::TParamsResponse;
using TSettingsGetter = TAutoRepliesDataProvider::TSettingsGetter;

struct TSettingsGetterTest : public Test {
    TSettingsGetterTest()
        : Ctx(GetContext())
        , MSettingsMock(std::make_shared<StrictMock<TMSettingsClientMock>>())
    {
    }

    void RequestSettings(const std::string& uid, auto&& callback) {
        auto getter = std::make_shared<TSettingsGetter>(Ctx, uid, MSettingsMock, std::move(callback));
        yplatform::spawn(IoContext.get_executor(), getter);
        IoContext.run();
    }

    TContextPtr Ctx;
    boost::asio::io_context IoContext;
    std::shared_ptr<StrictMock<TMSettingsClientMock>> MSettingsMock;
};

TEST_F(TSettingsGetterTest, for_correct_response_should_run_callback) {
    auto cb = [](TErrorCode ec, std::string fromName){
        EXPECT_EQ(ec, EError::Ok);
        EXPECT_EQ(fromName, "Display Name");
    };
    InSequence seq;
    EXPECT_CALL(*MSettingsMock, GetProfile(_, TParamsRequest{.Uid = 42U, .Params = {"from_name"}}, _))
        .WillOnce(InvokeArgument<2>(EError::Ok, TParamsResponse{.FromName = "Display Name"}));

    RequestSettings("42", std::move(cb));
}

TEST_F(TSettingsGetterTest, for_error_should_return_error) {
    auto cb = [](TErrorCode ec, std::string fromName){
        EXPECT_EQ(ec, EError::HttpRetriesExceeded);
        EXPECT_THAT(fromName, IsEmpty());
    };
    InSequence seq;
    EXPECT_CALL(*MSettingsMock, GetProfile(_, TParamsRequest{.Uid = 42U, .Params = {"from_name"}}, _))
        .WillOnce(InvokeArgument<2>(EError::HttpRetriesExceeded, std::nullopt));

    RequestSettings("42", std::move(cb));
}
