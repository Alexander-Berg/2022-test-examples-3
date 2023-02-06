#include <gtest/gtest.h>

#include "nsls_test.h"
#include "fakes/context.h"
#include "mocks/bb_client.h"
#include "mocks/bwlist_client.h"
#include <mail/notsolitesrv/src/user/processor.h>

using namespace testing;
using namespace testing::internal;
using namespace NNotSoLiteSrv;
namespace ph = std::placeholders;


struct TUserProcessorTest: public TNslsTest {
    void SetUp() override {
        Ctx = GetContext();
    }

    auto GetUserGetter() const {
        return
            [this](TContextPtr, const std::string& e, bool m, NUser::TUser& u, NBlackbox::TCallback c) {
                return BBClient.GetUser(e, m, u, c);
            };
    }
    auto GetListsLoader() const {
        return
            [this](TContextPtr, const std::string& u, NBlackWhiteList::TCallback c) {
                return BWListClient.LoadLists(u, c);
            };
    }

    TContextPtr Ctx;
    StrictMock<TBBClientMock> BBClient;
    StrictMock<TBWListClientMock> BWListClient;
};

void SetUserInfo(NUser::TUser& ui) {
    ui.Status = NUser::ELoadStatus::Found;
    ui.Uid = "1234";
}

TEST_F(TUserProcessorTest, UserOkEmptyListOk) {
    EXPECT_CALL(
        BBClient, GetUser("a@a.ru", false, _, _)
    ).WillOnce(DoAll(
        WithArg<2>(Invoke(SetUserInfo)),
        InvokeArgument<3>(EError::Ok)
    ));
    EXPECT_CALL(
        BWListClient, LoadLists("1234", _)
    ).WillOnce(InvokeArgument<1>(EError::Ok, std::make_shared<NBlackWhiteList::TList>()));

    NUser::TUser userInfo;
    ExpectCallbackCalled(
        [&userInfo](TErrorCode ec) {
            EXPECT_FALSE(ec);
            EXPECT_FALSE(userInfo.DeliveryResult.ErrorCode);
            EXPECT_EQ(userInfo.Status, NUser::ELoadStatus::Found);
        },
        1,
        &NUser::ProcessUser,
            Ctx,
            GetUserGetter(),
            GetListsLoader(),
            "a@a.ru",
            false,
            false,
            userInfo);
}

TEST_F(TUserProcessorTest, UserOkListsSkipped) {
    EXPECT_CALL(
        BBClient, GetUser("a@a.ru", false, _, _)
    ).WillOnce(DoAll(
        WithArg<2>(Invoke(SetUserInfo)),
        InvokeArgument<3>(EError::Ok)
    ));
    EXPECT_CALL(
        BWListClient, LoadLists("1234", _)
    ).Times(0);

    NUser::TUser userInfo;
    ExpectCallbackCalled(
        [&userInfo](TErrorCode ec) {
            EXPECT_FALSE(ec);
            EXPECT_FALSE(userInfo.DeliveryResult.ErrorCode);
            EXPECT_EQ(userInfo.Status, NUser::ELoadStatus::Found);
        },
        1,
        &NUser::ProcessUser,
            Ctx,
            GetUserGetter(),
            GetListsLoader(),
            "a@a.ru",
            false,
            true,
            userInfo);
}

TEST_F(TUserProcessorTest, MailishUserOkListSkipped) {
    EXPECT_CALL(
        BBClient, GetUser("a@a.ru", true, _, _)
    ).WillOnce(DoAll(
        WithArg<2>(Invoke(SetUserInfo)),
        InvokeArgument<3>(EError::Ok)
    ));
    EXPECT_CALL(
        BWListClient, LoadLists(_, _)
    ).Times(0);

    NUser::TUser userInfo;
    userInfo.Uid = "1234";
    ExpectCallbackCalled(
        [&userInfo](TErrorCode ec) {
            EXPECT_FALSE(ec);
            EXPECT_FALSE(userInfo.DeliveryResult.ErrorCode);
            EXPECT_EQ(userInfo.Status, NUser::ELoadStatus::Found);
        },
        1,
        &NUser::ProcessUser,
            Ctx,
            GetUserGetter(),
            GetListsLoader(),
            "a@a.ru",
            true,
            false,
            userInfo);
}

TEST_F(TUserProcessorTest, UserFailed) {
    EXPECT_CALL(
        BBClient, GetUser("a@a.ru", false, _, _)
    ).WillOnce(InvokeArgument<3>(EError::DeliveryInternal));
    EXPECT_CALL(
        BWListClient, LoadLists("1234", _)
    ).Times(0);

    NUser::TUser userInfo;
    userInfo.Uid = "1234";
    ExpectCallbackCalled(
        [&userInfo](TErrorCode ec) {
            EXPECT_TRUE(ec);
            EXPECT_TRUE(userInfo.DeliveryResult.ErrorCode);
            EXPECT_EQ(userInfo.Status, NUser::ELoadStatus::Unknown);
        },
        1,
        &NUser::ProcessUser,
            Ctx,
            GetUserGetter(),
            GetListsLoader(),
            "a@a.ru",
            false,
            false,
            userInfo);
}

TEST_F(TUserProcessorTest, UserLoadedButNotFound) {
    EXPECT_CALL(
        BBClient, GetUser("a@a.ru", false, _, _)
    ).WillOnce(DoAll(
        WithArg<2>(Invoke(
            [](NUser::TUser& ui) { ui.Status = NUser::ELoadStatus::Loaded; }
        )),
        InvokeArgument<3>(EError::Ok)
    ));
    EXPECT_CALL(
        BWListClient, LoadLists("1234", _)
    ).Times(0);

    NUser::TUser userInfo;
    userInfo.Uid = "1234";
    ExpectCallbackCalled(
        [&userInfo](TErrorCode ec) {
            EXPECT_EQ(ec, EError::UserNotFound);
            EXPECT_EQ(userInfo.DeliveryResult.ErrorCode, EError::UserNotFound);
            EXPECT_EQ(userInfo.Status, NUser::ELoadStatus::Loaded);
        },
        1,
        &NUser::ProcessUser,
            Ctx,
            GetUserGetter(),
            GetListsLoader(),
            "a@a.ru",
            false,
            false,
            userInfo);
}

TEST_F(TUserProcessorTest, UserOkBWListFailed) {
    EXPECT_CALL(
        BBClient, GetUser("a@a.ru", false, _, _)
    ).WillOnce(DoAll(
        WithArg<2>(Invoke(SetUserInfo)),
        InvokeArgument<3>(EError::Ok)
    ));
    EXPECT_CALL(
        BWListClient, LoadLists("1234", _)
    ).WillOnce(InvokeArgument<1>(EError::DeliveryInternal, NBlackWhiteList::TListPtr()));

    NUser::TUser userInfo;
    userInfo.Uid = "1234";
    ExpectCallbackCalled(
        [&userInfo](TErrorCode ec) {
            EXPECT_TRUE(ec);
            EXPECT_TRUE(userInfo.DeliveryResult.ErrorCode);
            EXPECT_EQ(userInfo.Status, NUser::ELoadStatus::Found);
        },
        1,
        &NUser::ProcessUser,
            Ctx,
            GetUserGetter(),
            GetListsLoader(),
            "a@a.ru",
            false,
            false,
            userInfo);
}
