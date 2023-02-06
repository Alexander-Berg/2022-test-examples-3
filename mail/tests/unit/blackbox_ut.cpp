#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "nsls_test.h"
#include "fakes/context.h"
#include "mocks/http_client.h"
#include "blackbox/client.h"
#include <ymod_httpclient/errors.h>
#include <boost/algorithm/string/join.hpp>
#include <boost/format.hpp>
#include <boost/range/adaptors.hpp>
#include <functional>

using namespace testing;
using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NBlackbox;

struct TBBClientTest: public TNslsTest {
    void SetUp() override {
        Ctx = GetContext();
        Http = std::make_shared<StrictMock<THttpClientMock>>();
    }

    using TCheckFunc = std::function<void(TErrorCode)>;
    virtual void Test(TCheckFunc check, bool emptyUid [[maybe_unused]] = false) {
        using namespace boost::adaptors;
        auto usersRng = Users | transformed([](auto& u) { return std::ref(u); });
        TUsers users(usersRng.begin(), usersRng.end());
        ExpectCallbackCalled(
            std::move(check), 1,
            GetUsersWithHttpClient,
            Ctx,
            std::move(users),
            Http
        );
    }

    virtual std::string GetUrl() const {
        std::string url{"bb?"
            "method=userinfo&"
            "uid="};

        using namespace boost::adaptors;
        url.append(boost::join(Users | transformed(std::mem_fn(&NUser::TUser::Uid)), "%2c"));
        url.append("&userip=127.0.0.1&"
            "emails=getdefault&"
            "dbfields="
                "account_info.fio.uid%2c"
                "account_info.country.uid%2c"
                "account_info.lang.uid%2c"
                "account_info.reg_date.uid%2c"
                "userphones.confirmed.uid&"
            "attributes=1031&"
            "format=json");
        return url;
    }

    void AddUser(const std::string& uid) {
        NUser::TUser u;
        u.Uid = uid;
        Users.emplace_back(u);
    }

    std::string GetResultString(const std::map<std::string, std::string>& types) {
        std::string ret;
        for (const auto& [uid, type]: types) {
            if (!ret.empty()) {
                ret.append(",");
            }
            ret.append((boost::format(ResponseTypes.at(type)) % uid).str());
        }
        return R"({"users":[)" + ret + "]}";
    }

    TContextPtr Ctx;
    std::shared_ptr<THttpClientMock> Http;
    std::vector<NUser::TUser> Users;

    const std::map<std::string, std::string> ResponseTypes {
        {
            "ok",
            "{"
                R"("id":"%1%",)"
                R"("uid":{"value":"%1%","lite":false,"hosted":false},)"
                R"("login":"sto-at-auto-ru",)"
                R"("have_password":true,)"
                R"("have_hint":false,)"
                R"("aliases":{},)"
                R"("karma":{"value":0},)"
                R"("karma_status":{"value":0},)"
                R"("dbfields":{)"
                    R"("account_info.country.uid":"ru",)"
                    R"("account_info.reg_date.uid":"2016-10-17 19:27:02",)"
                    R"("subscription.born_date.2":"2016-10-17 19:27:02",)"
                    R"("subscription.login.-":"sto-at-auto-ru",)"
                    R"("subscription.login.2":"sto-at-auto-ru",)"
                    R"("subscription.login_rule.-":"1",)"
                    R"("subscription.suid.-":"%1%1",)"
                    R"("subscription.suid.1000":"",)"
                    R"("account_info.country.uid":"ru",)"
                    R"("userphones.confirmed.uid":"")"
                "},"
                R"("attributes":{"1031":"org_id"},)"
                R"("address-list":[{)"
                    R"("born-date": "2011-04-03 01:05:29",)"
                    R"("native": true,)"
                    R"("unsafe": false,)"
                    R"("silent": false,)"
                    R"("rpop": false,)"
                    R"("default": true,)"
                    R"("validated": true,)"
                    R"("address": "uid-%1%@yandex.ru")"
                "}]"
            "}"
        },
        {
            "empty_attributes",
            "{"
                R"("id":"%1%",)"
                R"("uid":{"value":"%1%"},)"
                R"("karma":{"value":0},)"
                R"("karma_status":{"value":0},)"
                R"("dbfields":{)"
                    R"("account_info.reg_date.uid":"2016-10-17 19:27:02",)"
                    R"("subscription.login_rule.-":"1")"
                "},"
                R"("attributes":{},)"
                R"("address-list":[{)"
                    R"("default": true,)"
                    R"("address": "uid-%1%@yandex.ru")"
                "}]"
            "}"
        },
        {
            "notfound",
            "{"
                R"("id":"%1%","uid":{},"karma":{"value":0},"karma_status":{"value":0})"
            "}"
        },
        {
            "garbage",
            "Hello, %1%!"
        }
    };
};

struct TSmtpTag {};
struct TMailishTag {};

template <typename TTag>
struct TTypedBBClientTest: public TBBClientTest {
    void Test(TCheckFunc check, bool emptyMailish = false) override {
        bool isMailish = std::is_same<TTag, TMailishTag>::value;
        if (isMailish && !emptyMailish) {
            UserInfo.Uid = "1120000000044345";
        } else if (!isMailish && emptyMailish) {
            return;
        }
        ExpectCallbackCalled(
            std::move(check), 1,
            std::bind(
                GetUserWithHttpClient,
                Ctx,
                "email@domain",
                isMailish,
                std::ref(UserInfo),
                Http,
                std::placeholders::_1)
        );
    }

    std::string GetUrl() const override {
        if (std::is_same<TTag, TSmtpTag>::value) {
            return Url;
        } else {
            return UrlMailish;
        }
    }

    NUser::TUser UserInfo;

    NHttp::TResponse ResultOk{
        200,
        {},
        "{"
            R"("users":[)"
                "{"
                    R"("id":"1120000000044345",)"
                    R"("uid":{"value":"1120000000044345","lite":false,"hosted":false},)"
                    R"("login":"sto-at-auto-ru",)"
                    R"("have_password":true,)"
                    R"("have_hint":false,)"
                    R"("aliases":{},)"
                    R"("karma":{"value":0},)"
                    R"("karma_status":{"value":0},)"
                    R"("dbfields":{)"
                        R"("account_info.country.uid":"ru",)"
                        R"("account_info.reg_date.uid":"2016-10-17 19:27:02",)"
                        R"("subscription.born_date.2":"2016-10-17 19:27:02",)"
                        R"("subscription.login.-":"sto-at-auto-ru",)"
                        R"("subscription.login.2":"sto-at-auto-ru",)"
                        R"("subscription.login_rule.-":"1",)"
                        R"("subscription.suid.-":"1120000000172346",)"
                        R"("subscription.suid.1000":"",)"
                        R"("account_info.country.uid":"ru",)"
                        R"("userphones.confirmed.uid":"")"
                    "},"
                    R"("attributes":{"1031":"org_id"},)"
                    R"("address-list":[)"
                        "{"
                            R"("born-date": "2011-04-03 01:05:29",)"
                            R"("native": true,)"
                            R"("unsafe": false,)"
                            R"("silent": false,)"
                            R"("rpop": false,)"
                            R"("default": true,)"
                            R"("validated": true,)"
                            R"("address": "antipovsky.alexey@yandex.ru")"
                        "}"
                    "]"
                "}"
            "]"
        "}",
        "Ok"};

    NHttp::TResponse ResultNotFound{
        200,
        {},
        R"({"users":[{"id":"","uid":{},"karma":{"value":0},"karma_status":{"value":0}}]})",
        "Ok"};

    NHttp::TResponse ResultGarbage{
        200,
        {},
        "hello, world!",
        "Ok"};

    std::string Url{"bb?"
        "method=userinfo&"
        "login=email%40domain&"
        "sid=smtp&"
        "userip=127.0.0.1&"
        "emails=getdefault&"
        "dbfields="
            "subscription.login.-%2c"
            "subscription.login_rule.-%2c"
            "subscription.suid.-%2c"
            "account_info.fio.uid%2c"
            "account_info.country.uid%2c"
            "account_info.lang.uid%2c"
            "account_info.reg_date.uid%2c"
            "userphones.confirmed.uid&"
        "attributes=1031&"
        "format=json"};

    std::string UrlMailish{"bb?"
        "method=userinfo&"
        "uid=1120000000044345&"
        "userip=127.0.0.1&"
        "emails=getdefault&"
        "dbfields="
            "account_info.fio.uid%2c"
            "account_info.country.uid%2c"
            "account_info.lang.uid%2c"
            "account_info.reg_date.uid%2c"
            "userphones.confirmed.uid&"
        "attributes=1031&"
        "format=json"};
};

using TTags = Types<TSmtpTag, TMailishTag>;
TYPED_TEST_SUITE(TTypedBBClientTest, TTags);

TYPED_TEST(TTypedBBClientTest, Success) {
    EXPECT_CALL(*this->Http, Get(this->GetUrl(), _))
        .WillOnce(InvokeArgument<1>(EError::Ok, this->ResultOk));

    this->Test([this](auto ec) {
        EXPECT_FALSE(ec);
        EXPECT_EQ(this->UserInfo.Status, NUser::ELoadStatus::Found);
        EXPECT_EQ(this->UserInfo.Uid, "1120000000044345");
    });
}

TYPED_TEST(TTypedBBClientTest, ServerError) {
    EXPECT_CALL(*this->Http, Get(this->GetUrl(), _))
        .WillOnce(InvokeArgument<1>(yhttp::errc::server_status_error, NHttp::TResponse()));

    this->Test([this](auto ec) {
        EXPECT_EQ(ec, yhttp::errc::server_status_error);
        EXPECT_EQ(this->UserInfo.Status, NUser::ELoadStatus::Unknown);
    });
}

TYPED_TEST(TTypedBBClientTest, NotFound) {
    EXPECT_CALL(*this->Http, Get(this->GetUrl(), _))
        .WillOnce(InvokeArgument<1>(EError::Ok, this->ResultNotFound));

    this->Test([this](auto ec) {
        EXPECT_EQ(ec, EError::UserNotFound);
        EXPECT_EQ(this->UserInfo.Status, NUser::ELoadStatus::Loaded);

        if (std::is_same<TypeParam, TMailishTag>::value) {
            EXPECT_EQ(this->UserInfo.Uid, "1120000000044345");
        } else {
            EXPECT_TRUE(this->UserInfo.Uid.empty());
        }
    });
}

TYPED_TEST(TTypedBBClientTest, Garbage) {
    EXPECT_CALL(*this->Http, Get(this->GetUrl(), _))
        .WillOnce(InvokeArgument<1>(EError::Ok, this->ResultGarbage));

    this->Test([this](auto ec) {
        EXPECT_EQ(ec, EError::UserInvalid);
        EXPECT_EQ(this->UserInfo.Status, NUser::ELoadStatus::Loaded);
    });
}

TYPED_TEST(TTypedBBClientTest, DontBotherBBOnWrongMailish) {
    EXPECT_CALL(*this->Http, Get(_, _)).Times(0);

    this->Test([](auto ec) {
        EXPECT_EQ(ec, EError::UserInvalid);
    }, true);
}

TYPED_TEST(TTypedBBClientTest, GetDefaultEmail) {
    EXPECT_CALL(*this->Http, Get(this->GetUrl(), _))
        .WillOnce(InvokeArgument<1>(EError::Ok, this->ResultOk));

    this->Test([this](auto ec) {
        EXPECT_FALSE(ec);
        EXPECT_EQ(this->UserInfo.Status, NUser::ELoadStatus::Found);
        EXPECT_EQ(this->UserInfo.DefaultEmail, "antipovsky.alexey@yandex.ru");
    });
}

TEST_F(TBBClientTest, GetUsersWithNoneUidsIsOk) {
    EXPECT_CALL(*this->Http, Get(this->GetUrl(), _))
        .WillOnce(InvokeArgument<1>(EError::Ok, NHttp::TResponse{200, {}, GetResultString({}), "Ok"}));

    Test([](auto ec) {
        ASSERT_FALSE(ec);
    });
}

TEST_F(TBBClientTest, GetUsersWithOneUidIsOk) {
    AddUser("1");
    EXPECT_CALL(*this->Http, Get(this->GetUrl(), _))
        .WillOnce(InvokeArgument<1>(EError::Ok, NHttp::TResponse{200, {}, GetResultString({{"1", "empty_attributes"}}), "Ok"}));

    Test([this](auto ec) {
        ASSERT_FALSE(ec);
        const auto& user{Users.front()};
        EXPECT_EQ(user.Status, NUser::ELoadStatus::Found);
        EXPECT_EQ(user.DefaultEmail, "uid-1@yandex.ru");
        EXPECT_FALSE(user.OrgId);
    });
}

TEST_F(TBBClientTest, GetUsersWithTwoUidsIsOk) {
    AddUser("1");
    AddUser("2");
    EXPECT_CALL(*this->Http, Get(this->GetUrl(), _))
        .WillOnce(InvokeArgument<1>(EError::Ok, NHttp::TResponse{200, {}, GetResultString({{"1", "ok"}, {"2", "ok"}}), "Ok"}));

    Test([this](auto ec) {
        ASSERT_FALSE(ec);
        for (const auto& u: Users) {
            EXPECT_EQ(u.Status, NUser::ELoadStatus::Found);
            EXPECT_EQ(u.DefaultEmail, "uid-" + u.Uid + "@yandex.ru");
            EXPECT_EQ(u.OrgId.value_or(TOrgId{}), "org_id");
        }
    });
}

TEST_F(TBBClientTest, GetUsersWithOneNotFound) {
    AddUser("1");
    AddUser("2");
    EXPECT_CALL(*this->Http, Get(this->GetUrl(), _))
        .WillOnce(InvokeArgument<1>(EError::Ok, NHttp::TResponse{200, {}, GetResultString({{"1", "ok"}, {"2", "notfound"}}), "Ok"}));

    Test([this](auto ec) {
        ASSERT_FALSE(ec);
        EXPECT_EQ(Users[0].Status, NUser::ELoadStatus::Found);
        EXPECT_EQ(Users[0].DefaultEmail, "uid-1@yandex.ru");

        EXPECT_EQ(Users[1].DeliveryResult.ErrorCode, EError::UserNotFound);
        EXPECT_EQ(Users[1].Status, NUser::ELoadStatus::Loaded);
        EXPECT_EQ(Users[1].DefaultEmail, "");
    });
}

TEST_F(TBBClientTest, GetUsersWithOneGarbageIsFail) {
    AddUser("1");
    AddUser("2");
    EXPECT_CALL(*this->Http, Get(this->GetUrl(), _))
        .WillOnce(InvokeArgument<1>(EError::Ok, NHttp::TResponse{200, {}, GetResultString({{"1", "ok"}, {"2", "garbage"}}), "Ok"}));

    Test([this](auto ec) {
        ASSERT_EQ(ec, EError::UserInvalid);
        EXPECT_EQ(Users[0].Status, NUser::ELoadStatus::Loaded);
        EXPECT_EQ(Users[1].Status, NUser::ELoadStatus::Loaded);
    });
}
