#include <catch.hpp>

#include <messenger/telegram/api.h>
#include <boost/algorithm/string.hpp>

namespace botserver::messenger {

using namespace Catch::Matchers;

struct fake_http_client
{
    template <typename Cb>
    void async_run(task_context_ptr /*ctx*/, yhttp::request req, Cb cb)
    {
        requests.emplace_back(req);
        cb(error_code(), response);
    }

    yhttp::response response;
    vector<yhttp::request> requests;
};

struct t_telegram_api
{
    using api_type = telegram::api<fake_http_client>;

    string test_token = "TEST_TOKEN_FOR_TELEGRAM";
    string test_chat_id = "100500";
    string test_text = "Helloworld";
    string test_parse_mode = "HTML";
    string test_file_id = "200500";
    string test_file_path = "123:file_path_to_download";

    task_context_ptr ctx;
    shared_ptr<fake_http_client> client = make_shared<fake_http_client>();
    api_type api{ client, test_token };

    size_t requests_count()
    {
        return client->requests.size();
    }

    yhttp::request last_request()
    {
        return client->requests.back();
    }

    std::string make_api_url(std::string method)
    {
        return "/bot" + test_token + "/" + method;
    }

    std::string make_file_api_url(std::string method)
    {
        return "/file/bot" + test_token + "/" + method;
    }

    void prepare_response(int status, std::string body)
    {
        client->response = { .status = status, .body = body };
    }
};

TEST_CASE_METHOD(t_telegram_api, "send_message")
{
    api.send_message(ctx, test_chat_id, test_text, test_parse_mode);
    REQUIRE(requests_count() == 1);
    REQUIRE(last_request().url == make_api_url("sendMessage"));
    REQUIRE(
        *last_request().body ==
        "chat_id=" + test_chat_id + "&text=" + test_text + "&parse_mode=" + test_parse_mode);
}

TEST_CASE_METHOD(t_telegram_api, "get_file")
{
    api.get_file_info(ctx, test_file_id);
    REQUIRE(client->requests.size() == 1);
    REQUIRE(last_request().url == make_api_url("getFile"));
    REQUIRE(*last_request().body == "file_id=" + test_file_id);
}

TEST_CASE_METHOD(t_telegram_api, "download_file")
{
    api.download_file(ctx, test_file_path);
    REQUIRE(client->requests.size() == 1);
    REQUIRE(last_request().url == make_file_api_url(test_file_path));
    REQUIRE(!last_request().body);
}

TEST_CASE_METHOD(t_telegram_api, "bad_status")
{
    prepare_response(502, "wtf_error");
    auto res = api.send_message(ctx, test_chat_id, test_text);
    REQUIRE_THROWS_WITH(res.get(), "telegram api bad http status, code=502, body=wtf_error");
}

TEST_CASE_METHOD(t_telegram_api, "bad_response")
{
    prepare_response(200, "<html><body>i am bad response</body></html>");
    auto res = api.send_message(ctx, test_chat_id, test_text);
    REQUIRE_THROWS_WITH(res.get(), StartsWith("telegram api parse error:"));
}

}
