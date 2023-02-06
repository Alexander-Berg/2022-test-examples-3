#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "nsls_test.h"
#include "fakes/context.h"
#include "mocks/http_client.h"
#include "blackwhitelist/client.h"
#include <ymod_httpclient/errors.h>

using namespace testing;
using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NBlackWhiteList;

struct TBWListClientTest: public TNslsTest {
    void SetUp() override {
        Ctx = GetContext();
        Http = std::make_shared<THttpClientMock>();
    }

    void TestLoadLists(const std::string& uid, TCallback cb) {
        return LoadListsWithHttpClient(Ctx, uid, Http, std::move(cb));
    }

    TContextPtr Ctx;
    std::shared_ptr<THttpClientMock> Http;
};

TEST_F(TBWListClientTest, SimpleList) {
    NHttp::TResponse Response{
        200,
        {},
        R"({"blacklist": ["one@domain"], "whitelist": ["two@domain"]})",
        "Ok"};

    EXPECT_CALL(*Http, Get("bwlist?uid=uid", _))
        .WillOnce(InvokeArgument<1>(EError::Ok, Response));

    ExpectCallbackCalled(
        [](TErrorCode ec, TListPtr bwlist) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(bwlist->ToString(NBlackWhiteList::EType::Black), "one@domain");
            EXPECT_EQ(bwlist->ToString(NBlackWhiteList::EType::White), "two@domain");
        },
        1,
        &TBWListClientTest::TestLoadLists, this, "uid");
}

TEST_F(TBWListClientTest, EmptyList) {
    NHttp::TResponse Response{
        200,
        {},
        R"({"blacklist":[],"whitelist":[]})",
        "Ok"};

    EXPECT_CALL(*Http, Get("bwlist?uid=uid", _))
        .WillOnce(InvokeArgument<1>(EError::Ok, Response));

    ExpectCallbackCalled(
        [](TErrorCode ec, TListPtr bwlist) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(bwlist->ToString(NBlackWhiteList::EType::Black), "");
            EXPECT_EQ(bwlist->ToString(NBlackWhiteList::EType::White), "");
        },
        1,
        &TBWListClientTest::TestLoadLists, this, "uid");
}

TEST_F(TBWListClientTest, InternalServerError) {
    NHttp::TResponse Response{
        500,
        {},
        "",
        "Internal Server Error"};

    EXPECT_CALL(*Http, Get("bwlist?uid=uid", _))
        .WillOnce(InvokeArgument<1>(EError::Ok, Response));

    ExpectCallbackCalled(
        [](TErrorCode ec, TListPtr bwlist) {
            EXPECT_EQ(ec, EError::BWListError);
            EXPECT_EQ(bwlist->ToString(), "Blacklist: [], Whitelist: []");
        },
        1,
        &TBWListClientTest::TestLoadLists, this, "uid");
}

TEST_F(TBWListClientTest, Garbage) {
    NHttp::TResponse Response{
        200,
        {},
        "hello, world!",
        "Ok"};

    EXPECT_CALL(*Http, Get("bwlist?uid=uid", _))
        .WillOnce(InvokeArgument<1>(EError::Ok, Response));

    ExpectCallbackCalled(
        [](TErrorCode ec, TListPtr bwlist) {
            EXPECT_EQ(ec, EError::BWListError);
            EXPECT_EQ(bwlist->ToString(), "Blacklist: [], Whitelist: []");
        },
        1,
        &TBWListClientTest::TestLoadLists, this, "uid");
}
