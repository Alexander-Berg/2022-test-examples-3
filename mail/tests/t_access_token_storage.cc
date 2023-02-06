#include <yxiva_mobile/access_token_storage.h>
#include <catch.hpp>

using namespace yxiva;
using namespace yxiva::mobile;
using access_token_handler = std::function<void(const access_token&)>;

struct request
{
    string sid;
    string secret_key;
    access_token_handler handler;
};

struct result
{
    error::code ec;
    string token;
};

using request_vector = std::vector<request>;
using result_vector = std::vector<result>;

struct test_get_access_token_op
{
    void operator()(
        const string& sid,
        const string& secret_key,
        const access_token_handler& handler)
    {
        requests->push_back(request{ sid, secret_key, handler });
    }

    request_vector* requests;
};

struct T_access_token_storage
{
    T_access_token_storage()
        : requests()
        , pstorage(std::make_shared<access_token_storage<test_get_access_token_op>>(
              test_get_access_token_op{ &requests },
              "",
              ""))
        , storage(*pstorage)
    {
    }

    void setup()
    {
        storage.reset(SID, KEY);
    }

    void get_next()
    {
        auto requests_before_call = requests.size();
        got_results = 0;
        storage.async_get([&](error::code ec, const string& token) {
            results.push_back(result{ ec, token });
            got_results++;
        });
        requests_sent = requests.size() - requests_before_call;
    }

    access_token make_token(const string& token)
    {
        access_token result;
        result.value = token;
        result.expires_at = result.refresh_at = std::time(nullptr) + 1000;
        return result;
    }

    access_token make_token(error::code ec, const string& token)
    {
        access_token result;
        result.ec = ec;
        result.value = token;
        result.expires_at = result.refresh_at = std::time(nullptr) + 1000;
        return result;
    }

    request_vector requests;
    result_vector results;
    std::shared_ptr<access_token_storage<test_get_access_token_op>> pstorage;
    access_token_storage<test_get_access_token_op>& storage;
    const string SID = "sid-tst";
    const string KEY = "secret-tst";
    const string TOKEN = "good-token";
    size_t requests_sent = 0;
    size_t got_results = 0;
};

TEST_CASE_METHOD(T_access_token_storage, "access_token_storage/empty_credentials", "")
{
    get_next();
    REQUIRE(requests_sent == 0);
    REQUIRE(got_results == 1);
    REQUIRE(results.back().ec == error::code::invalid_cert);
}

TEST_CASE_METHOD(T_access_token_storage, "access_token_storage/send_req_at_first_async_get", "")
{
    setup();
    get_next();
    REQUIRE(requests_sent == 1);
    REQUIRE(got_results == 0);
}

TEST_CASE_METHOD(
    T_access_token_storage,
    "access_token_storage/returns_good_token_if_valid_answer",
    "")
{
    setup();
    get_next();
    REQUIRE(requests_sent == 1);
    requests.back().handler(make_token(TOKEN));
    REQUIRE(got_results == 1);
    REQUIRE(!results.back().ec);
    REQUIRE(results.back().token == TOKEN);
}

TEST_CASE_METHOD(
    T_access_token_storage,
    "access_token_storage/returns_error_token_if_cloud_error",
    "")
{
    setup();
    get_next();
    REQUIRE(requests_sent == 1);
    requests.back().handler(make_token(error::code::cloud_error, TOKEN));
    REQUIRE(got_results == 1);
    REQUIRE(results.back().ec == error::code::cloud_error);
    REQUIRE(results.back().token == TOKEN);
}

TEST_CASE_METHOD(
    T_access_token_storage,
    "access_token_storage/send_req_at_first_async_get_only",
    "")
{
    setup();
    get_next();
    REQUIRE(requests_sent == 1);
    requests.back().handler(make_token(TOKEN));
    get_next();
    REQUIRE(requests_sent == 0);
}

TEST_CASE_METHOD(
    T_access_token_storage,
    "access_token_storage/send_req_if_sid_or_key_were_changed",
    "")
{
    setup();
    get_next();
    REQUIRE(requests_sent == 1);
    requests.back().handler(make_token(TOKEN));
    storage.reset(SID + "1", KEY);
    get_next();
    REQUIRE(requests_sent == 1);
    requests.back().handler(make_token(TOKEN));
    storage.reset(SID + "1", KEY + "1");
    get_next();
    REQUIRE(requests_sent == 1);
    requests.back().handler(make_token(TOKEN));
}

TEST_CASE_METHOD(
    T_access_token_storage,
    "access_token_storage/send_req_if_refresh_time_but_call_handler",
    "")
{
    setup();
    get_next();
    REQUIRE(requests_sent == 1);
    auto answer = make_token(TOKEN);
    answer.refresh_at = 0;
    requests.back().handler(answer);
    get_next();
    REQUIRE(requests_sent == 1);
    REQUIRE(got_results == 1);
}

TEST_CASE_METHOD(T_access_token_storage, "access_token_storage/send_req_if_expired", "")
{
    setup();
    get_next();
    REQUIRE(requests_sent == 1);
    auto answer = make_token(TOKEN);
    answer.expires_at = 0;
    requests.back().handler(answer);
    get_next();
    REQUIRE(requests_sent == 1);
    REQUIRE(got_results == 0);
}

TEST_CASE_METHOD(T_access_token_storage, "access_token_storage/reqs_are_queued", "")
{
    setup();
    get_next();
    get_next();
    get_next();
    REQUIRE(requests.size() == 1);
    REQUIRE(results.size() == 0);
}

TEST_CASE_METHOD(T_access_token_storage, "access_token_storage/reqs_are_executed_on_response", "")
{
    setup();
    get_next();
    get_next();
    get_next();
    REQUIRE(requests.size() == 1);
    requests.back().handler(make_token(TOKEN));
    REQUIRE(results.size() == 3);
}
