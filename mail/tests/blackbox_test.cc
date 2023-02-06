#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/blackbox.h>

namespace {

TEST(retryable, should_return_true_with_status_500) {
    ymod_httpclient::response r;
    r.status = 500;
    EXPECT_TRUE(mbox_oper::blackbox::retryable(r));
}

TEST(retryable, should_return_true_with_status_503) {
    ymod_httpclient::response r;
    r.status = 503;
    EXPECT_TRUE(mbox_oper::blackbox::retryable(r));
}

TEST(retryable, should_return_false_with_status_400) {
    ymod_httpclient::response r;
    r.status = 400;
    EXPECT_FALSE(mbox_oper::blackbox::retryable(r));
}

TEST(retryable, should_return_false_with_status_200) {
    ymod_httpclient::response r;
    r.status = 200;
    EXPECT_FALSE(mbox_oper::blackbox::retryable(r));
}

TEST(succeeded, should_return_true_with_status_200) {
    ymod_httpclient::response r;
    r.status = 200;
    EXPECT_TRUE(mbox_oper::blackbox::succeeded(r));
}

TEST(succeeded, should_return_false_with_status_400) {
    ymod_httpclient::response r;
    r.status = 400;
    EXPECT_FALSE(mbox_oper::blackbox::succeeded(r));
}

TEST(succeeded, should_return_false_with_status_500) {
    ymod_httpclient::response r;
    r.status = 500;
    EXPECT_FALSE(mbox_oper::blackbox::succeeded(r));
}

TEST(failed, should_return_true_with_error_initialized) {
    mbox_oper::blackbox::reflection::Response r;
    r.error = "error message";
    EXPECT_TRUE(mbox_oper::blackbox::failed(r));
}

TEST(failed, should_return_false_with_error_not_initialized) {
    mbox_oper::blackbox::reflection::Response r;
    EXPECT_FALSE(mbox_oper::blackbox::failed(r));
}

TEST(errorMessage, should_return_error_with_error_initialized) {
    mbox_oper::blackbox::reflection::Response r;
    r.error = "error message";
    EXPECT_EQ(mbox_oper::blackbox::errorMessage(r), "error message");
}

TEST(errorMessage, should_throw_system_error_with_error_not_initialized) {
    mbox_oper::blackbox::reflection::Response r;
    EXPECT_THROW(mbox_oper::blackbox::errorMessage(r), boost::system::system_error);
}

TEST(user, should_throw_system_error_with_users_not_initialized) {
    mbox_oper::blackbox::reflection::Response r;
    EXPECT_THROW(mbox_oper::blackbox::user(r), boost::system::system_error);
}

TEST(user, should_throw_system_error_with_users_initialized_and_empty) {
    mbox_oper::blackbox::reflection::Response r;
    r.users = std::vector<mbox_oper::blackbox::reflection::User>{};
    EXPECT_THROW(mbox_oper::blackbox::user(r), boost::system::system_error);
}

TEST(user, should_throw_system_error_with_users_initialized_and_size_greater_then_1) {
    mbox_oper::blackbox::reflection::Response r;
    r.users = std::vector<mbox_oper::blackbox::reflection::User>{
        mbox_oper::blackbox::reflection::User{}, mbox_oper::blackbox::reflection::User{}};
    EXPECT_THROW(mbox_oper::blackbox::user(r), boost::system::system_error);
}

TEST(user, should_return_first_element_of_users_initialized_and_size_1) {
    mbox_oper::blackbox::reflection::Response r;
    r.users = std::vector<mbox_oper::blackbox::reflection::User>{mbox_oper::blackbox::reflection::User{}};
    EXPECT_EQ(std::addressof(mbox_oper::blackbox::user(r)), std::addressof(r.users->front()));
}

TEST(exists, should_return_true_if_user_uid_value_initialized) {
    mbox_oper::blackbox::reflection::User user;
    user.uid.value = "1245630";
    EXPECT_TRUE(mbox_oper::blackbox::exists(user));
}

TEST(exists, should_return_false_if_user_uid_value_not_initialized) {
    mbox_oper::blackbox::reflection::User user;
    EXPECT_FALSE(mbox_oper::blackbox::exists(user));
}

TEST(aliases, should_throw_system_error_with_aliases_not_initialized) {
    mbox_oper::blackbox::reflection::User user;
    EXPECT_THROW(mbox_oper::blackbox::aliases(user), boost::system::system_error);
}

TEST(aliases, should_return_reference_to_aliases_of_user_with_aliases_initialized) {
    mbox_oper::blackbox::reflection::User user;
    user.aliases = mbox_oper::blackbox::reflection::Aliases{};
    EXPECT_EQ(std::addressof(mbox_oper::blackbox::aliases(user)), std::addressof(*user.aliases));
}

TEST(isMailish, should_return_true_if_aliases_initialized_contain_key_12) {
    mbox_oper::blackbox::reflection::User user;
    user.aliases = mbox_oper::blackbox::reflection::Aliases{{"12", "something"}};
    EXPECT_TRUE(mbox_oper::blackbox::isMailish(user));
}

TEST(isMailish, should_return_false_if_aliases_aliases_initialized_do_not_contain_key_12) {
    mbox_oper::blackbox::reflection::User user;
    user.aliases = mbox_oper::blackbox::reflection::Aliases{};
    EXPECT_FALSE(mbox_oper::blackbox::isMailish(user));
}

TEST(isMailish, should_throw_system_error_with_aliases_not_initialized) {
    mbox_oper::blackbox::reflection::User user;
    EXPECT_THROW(mbox_oper::blackbox::isMailish(user), boost::system::system_error);
}

TEST(fromReflection, should_throw_system_error_with_aliases_not_initialized) {
    mbox_oper::blackbox::reflection::User user;
    EXPECT_THROW(mbox_oper::blackbox::fromReflection(user), boost::system::system_error);
}

TEST(fromReflection, should_return_empty_fields_if_not_set_in_reflection) {
    mbox_oper::blackbox::reflection::User user;
    user.aliases = mbox_oper::blackbox::reflection::Aliases{{"12", "something"}};
    auto res = mbox_oper::blackbox::fromReflection(user);
    EXPECT_TRUE(res.isMailish);
    EXPECT_TRUE(res.uid.empty());
    EXPECT_TRUE(res.login.empty());
    EXPECT_TRUE(res.karma.empty());
    EXPECT_TRUE(res.karmaStatus.empty());
}

TEST(fromReflection, should_return_fields_from_reflection) {
    mbox_oper::blackbox::reflection::User user;
    user.aliases = mbox_oper::blackbox::reflection::Aliases{{"12", "something"}};
    user.uid = mbox_oper::blackbox::reflection::StringValue{{"666"}};
    user.login = "login";
    user.karma = mbox_oper::blackbox::reflection::StringValue{{"karma"}};
    user.karma_status = mbox_oper::blackbox::reflection::StringValue{{"karma_status"}};

    auto res = mbox_oper::blackbox::fromReflection(user);
    EXPECT_TRUE(res.isMailish);
    EXPECT_EQ(res.uid, "666");
    EXPECT_EQ(res.login, "login");
    EXPECT_EQ(res.karma, "karma");
    EXPECT_EQ(res.karmaStatus, "karma_status");
}

}


namespace {

struct NetworkMock {
    using Response = std::variant<yhttp::response, boost::system::error_code>;
    MOCK_METHOD(Response, response, (http_getter::Request), ());

};


using namespace ::testing;


struct UserIsMailishOp : public Test {
    StrictMock<NetworkMock> network;

    http_getter::TypedClientPtr httpClient;
    http_getter::TypedEndpoint endpoint = http_getter::TypedEndpoint::fromData("/blackbox", "blackbox", nullptr);

    boost::asio::io_context context;

    static std::string uid() { return "666"; }
    static std::string ip() { return "127.1.1.1"; }

    static HttpArguments getArgs() {
        HttpArguments args;
        args.add("aliases", "12");
        args.add("format", "json");
        args.add("method", "userinfo");
        args.add("uid", uid());
        args.add("userip", ip());
        return args;
    }

    void SetUp() override {
        using namespace http_getter;

        auto factory = [&](TypedHttpClientPtr, RequestStatsPtr, std::string) -> TypedRun {
            return [&network=this->network](Request req, Handler handler, TypedToken token) {
                auto resp = network.response(std::move(req));
                if (std::holds_alternative<boost::system::error_code>(resp)) {
                    handler.error(std::get<boost::system::error_code>(resp));
                }
                else {
                    handler.success(std::get<yhttp::response>(resp));
                }
                token(boost::system::error_code{});
            };
        };
        httpClient = std::make_shared<TypedClient>(nullptr, http::headers{}, factory, nullptr);
    }

    template<class Fn>
    void spawn(Fn&& fn) {
        boost::asio::spawn(context, std::forward<Fn>(fn));
        context.run();
    }

    mbox_oper::blackbox::User perform(boost::asio::yield_context yield, boost::system::error_code& err) {
        mbox_oper::blackbox::UserInfoOp<logdog::none_t> op{httpClient, endpoint, {}};
        err = mbox_oper::blackbox::error::ok;
        try {
            return op.perform(uid(), ip(), yield);
        } catch (const ::boost::system::system_error& e) {
            err = e.code();
        } catch (...) {
            throw;
        }
        return {};
    }

    Matcher<http_getter::Request> urlMatcher() {
        auto yhttpUrl = Field(&yhttp::request::url, Eq(endpoint.method() + http::combineArgs({""}, getArgs())));
        return Field(&http_getter::Request::request, yhttpUrl);
    }
};


TEST_F(UserIsMailishOp, should_provide_error_if_not_succeeded_and_not_retryable_http_response_status) {
    EXPECT_CALL(network, response(urlMatcher())).WillOnce(Return(yhttp::response{.status=321}));
    spawn([&] (boost::asio::yield_context yield) {
        boost::system::error_code err;
        perform(yield, err);
        EXPECT_EQ(err, mbox_oper::blackbox::error::nonRetryableStatus);
    });
}

TEST_F(UserIsMailishOp, should_provide_error_badResponse_in_case_of_invalid_json) {
    const std::string body = R"JSON({"error1":"retry"})JSON";
    EXPECT_CALL(network, response(urlMatcher())).WillOnce(Return(yhttp::response{.status=200, .body=body}));
    spawn([&] (boost::asio::yield_context yield) {
        boost::system::error_code err;
        perform(yield, err);
        EXPECT_EQ(err, mbox_oper::blackbox::error::badResponse);
    });
}

TEST_F(UserIsMailishOp, should_provide_error_retriesExceeded_in_case_of_exception) {
    const std::string body = R"JSON({"exception":{"value":"", "id":1}})JSON";
    EXPECT_CALL(network, response(urlMatcher())).WillOnce(Return(yhttp::response{.status=200, .body=body}));
    spawn([&] (boost::asio::yield_context yield) {
        boost::system::error_code err;
        perform(yield, err);
        EXPECT_EQ(err, mbox_oper::blackbox::error::retriesExceeded);
    });
}

TEST_F(UserIsMailishOp, should_provide_error_retriesExceeded_if_all_tries_was_not_succeeded) {
    const std::string body = R"JSON({"error":"retry"})JSON";
    EXPECT_CALL(network, response(urlMatcher())).WillOnce(Return(yhttp::response{.status=200, .body=body}));
    spawn([&] (boost::asio::yield_context yield) {
        boost::system::error_code err;
        perform(yield, err);
        EXPECT_EQ(err, mbox_oper::blackbox::error::retriesExceeded);
    });
}

TEST_F(UserIsMailishOp, should_provide_error_retriesExceeded_in_case_of_db_exception) {
    const std::string body = R"JSON({"exception":{"value":"", "id":10}})JSON";
    EXPECT_CALL(network, response(urlMatcher())).WillOnce(Return(yhttp::response{.status=200, .body=body}));
    spawn([&] (boost::asio::yield_context yield) {
        boost::system::error_code err;
        perform(yield, err);
        EXPECT_EQ(err, mbox_oper::blackbox::error::retriesExceeded);
    });
}

TEST_F(UserIsMailishOp, should_provide_error_userDoesNotExist_if_user_does_not_exist) {
    const std::string body = R"JSON({"users":[{"login":"does not exist", "uid": {}}]})JSON";
    EXPECT_CALL(network, response(urlMatcher())).WillOnce(Return(yhttp::response{.status=200, .body=body}));
    spawn([&] (boost::asio::yield_context yield) {
        boost::system::error_code err;
        perform(yield, err);
        EXPECT_EQ(err, mbox_oper::blackbox::error::userDoesNotExist);
    });
}

TEST_F(UserIsMailishOp, should_return_true_if_user_is_mailish) {
    const std::string body = R"JSON({"users":[ {"login":"some_user", "uid": {"value":"1"}, "aliases":{ "12": "12" } } ]})JSON";
    EXPECT_CALL(network, response(urlMatcher())).WillOnce(Return(yhttp::response{.status=200, .body=body}));
    spawn([&] (boost::asio::yield_context yield) {
        boost::system::error_code err;
        EXPECT_TRUE(perform(yield, err).isMailish);
        EXPECT_EQ(err, mbox_oper::blackbox::error::ok);
    });
}

TEST_F(UserIsMailishOp, should_return_false_if_user_is_not_mailish) {
    const std::string body = R"JSON({"users":[ {"login":"some_user", "uid": {"value":"1"}, "aliases":{} } ]})JSON";
    EXPECT_CALL(network, response(urlMatcher())).WillOnce(Return(yhttp::response{.status=200, .body=body}));
    spawn([&] (boost::asio::yield_context yield) {
        boost::system::error_code err;
        EXPECT_FALSE(perform(yield, err).isMailish);
        EXPECT_EQ(err, mbox_oper::blackbox::error::ok);
    });
}

}
