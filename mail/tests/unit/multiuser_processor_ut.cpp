#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "nsls_test.h"
#include "fakes/context.h"
#include "mocks/user_processor.h"
#include "multiuser_processor.h"

using namespace testing;
using namespace testing::internal;
using namespace NNotSoLiteSrv;

struct TMultiUserTest: public TNslsTest {
    void SetUp() override {
        Ctx = GetContext();
    }

    auto GetUserProcessor() const {
        return
            [this](TContextPtr, NBlackbox::TUserGetter, NBlackWhiteList::TListsLoader,
                const std::string& e, bool m, bool s, NUser::TUser& u, NUser::TCallback cb)
            {
                return Up.ProcessUser(e, m, s, u, cb);
            };
    }

    TContextPtr Ctx;
    StrictMock<TUserProcessorMock> Up;

    NUser::TStorage UserStorage;
};

TEST_F(TMultiUserTest, OneRcptOk) {
    UserStorage.AddUser("to@a.ru", true, true);

    EXPECT_CALL(
        Up,
        ProcessUser("to@a.ru", false, false, _, _)
    ).WillOnce(InvokeArgument<4>(EError::Ok));

    ExpectCallbackCalled(
        [](TErrorCode ec) {
            EXPECT_FALSE(ec);
        },
        1,
        ProcessAllUsers,
            Ctx,
            GetUserProcessor(),
            UserStorage
    );
}

TEST_F(TMultiUserTest, TwoRcptsOneUnique) {
    UserStorage.AddUser("to@a.ru", true, true);
    UserStorage.AddUser("to+tag@a.ru", true, true); // Assumed to+tag@ => to@

    EXPECT_CALL(
        Up,
        ProcessUser("to@a.ru", false, false, _, _)
    ).WillOnce(InvokeArgument<4>(EError::Ok));
    EXPECT_CALL(
        Up,
        ProcessUser("to+tag@a.ru", false, false, _, _)
    ).WillOnce(InvokeArgument<4>(EError::Ok));

    ExpectCallbackCalled(
        [](TErrorCode ec) {
            EXPECT_FALSE(ec);
        },
        1,
        ProcessAllUsers,
            Ctx,
            GetUserProcessor(),
            UserStorage
    );
}

TEST_F(TMultiUserTest, ThreeRcptsTwoUniqueOneFailed) {
    UserStorage.AddUser("to@a.ru", true, true);
    UserStorage.AddUser("to+tag@a.ru", true, true);
    UserStorage.AddUser("another@a.ru", true, true);

    EXPECT_CALL(
        Up,
        ProcessUser("another@a.ru", false, false, _, _)
    ).WillOnce(InvokeArgument<4>(EError::DeliveryInternal));
    EXPECT_CALL(
        Up,
        ProcessUser("to@a.ru", false, false, _, _)
    ).WillOnce(InvokeArgument<4>(EError::Ok));
    EXPECT_CALL(
        Up,
        ProcessUser("to+tag@a.ru", false, false, _, _)
    ).WillOnce(InvokeArgument<4>(EError::Ok));

    ExpectCallbackCalled(
        [](TErrorCode ec) {
            EXPECT_FALSE(ec);
        },
        1,
        ProcessAllUsers,
            Ctx,
            GetUserProcessor(),
            UserStorage
    );
}

TEST_F(TMultiUserTest, NoUsers) {
    EXPECT_CALL(
        Up,
        ProcessUser(_, _, _, _, _)
    ).Times(0);

    ExpectCallbackCalled(
        [](TErrorCode ec) {
            EXPECT_EQ(ec, EError::DeliveryNoRecipients);
        },
        1,
        ProcessAllUsers,
            Ctx,
            GetUserProcessor(),
            UserStorage
    );
}

TEST_F(TMultiUserTest, AllFailed) {
    UserStorage.AddUser("to@a.ru", true, true);
    UserStorage.AddUser("to+tag@a.ru", true, true);
    UserStorage.AddUser("another@a.ru", true, true);

    EXPECT_CALL(
        Up,
        ProcessUser("another@a.ru", false, false, _, _)
    ).WillOnce(InvokeArgument<4>(EError::DeliveryInternal));
    EXPECT_CALL(
        Up,
        ProcessUser("to@a.ru", false, false, _, _)
    ).WillOnce(InvokeArgument<4>(EError::DeliveryInternal));
    EXPECT_CALL(
        Up,
        ProcessUser("to+tag@a.ru", false, false, _, _)
    ).WillOnce(InvokeArgument<4>(EError::DeliveryInternal));

    ExpectCallbackCalled(
        [](TErrorCode ec) {
            EXPECT_FALSE(ec);
        },
        1,
        ProcessAllUsers,
            Ctx,
            GetUserProcessor(),
            UserStorage
    );
}
