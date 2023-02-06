#include <gtest/gtest.h>
#include "../../src/processor/so_check_filter.h"
#include <furita/common/rule_helper.hpp>
#include <mail/yreflection/include/yamail/data/serialization/yajl.h>
#include <ymod_httpclient/cluster_client.h>

namespace furita {

class TSoCheckClusterClientMock : public ymod_httpclient::cluster_client {
public:
    TSoCheckClusterClientMock(int response_status, const std::string& response_body,
            boost::asio::io_service& io_service, const yhttp::cluster_client::settings& settings)
        : ymod_httpclient::cluster_client(io_service, settings)
        , response_status(response_status)
        , response_body(response_body) {
    }

    void async_run(task_context_ptr /*ctx*/, yhttp::request req, callback_type callback) override {
        last_run_request_url = req.url;
        last_run_request_body = req.body ? *req.body : "";
        yhttp::response response;
        response.status = response_status;
        response.body = response_body;
        callback(boost::system::error_code(), response);
    }

    std::string get_last_run_request_url() const {
        return last_run_request_url;
    }

    std::string get_last_run_request_body() const {
        return last_run_request_body;
    }

private:
    int response_status;
    std::string response_body;
    std::string last_run_request_url;
    std::string last_run_request_body;
};

auto get_cluster_client_mock(int response_code, const std::string& response_body) {
    boost::asio::io_service io_service;
    yhttp::cluster_client::settings settings;
    settings.nodes.emplace_back("test_host");
    return std::make_shared<TSoCheckClusterClientMock>(response_code, response_body, io_service, settings);
}

auto get_so_check_result(std::shared_ptr<TSoCheckClusterClientMock> cluster_client,
                         TContextPtr ctx = std::make_shared<TContext>(""),
                         rules::rule_ptr rule = boost::make_shared<rules::rule>(),
                         const processor::so_check::TParams& params = {}) {
    auto future_so_check_result = processor::so_check::so_check_filter(ctx, rule, cluster_client, params);
    future_so_check_result.wait();
    return future_so_check_result.get();
}

TEST(SoCheckFilter, so_check_filter_request_params) {
    auto ctx = boost::make_shared<yplatform::task_context>();

    auto rule = boost::make_shared<rules::rule>();
    rule->name = "test_rule";
    rule->actions->push_back(boost::make_shared<rules::action>("move", "1"));
    rules_helpers::ConditionOper op("1");
    rule->conditions->push_back(boost::make_shared<rules::condition>(
                                     "from", "user@domain", rules::condition::link_type::AND, op.toRuleOperType(), op.negative()));

    processor::so_check::TParams params;
    params.user_ip = "127.0.0.1";
    params.user_uid = "4002342134";
    params.form_realpath = "mail.yandex.ru";
    params.url = "test_url";

    auto cluster_client_mock = get_cluster_client_mock(200, "<spam>0</spam>");
    const auto so_check_result = get_so_check_result(cluster_client_mock, std::make_shared<TContext>(ctx->uniq_id()), rule, params);
    const auto correct_url = params.url + "?form_id=" + ctx->uniq_id() +
                                          "&id="      + ctx->uniq_id();

    EXPECT_EQ(so_check_result, processor::so_check::EResult::OK);
    EXPECT_EQ(cluster_client_mock->get_last_run_request_url(), correct_url);
    EXPECT_EQ(cluster_client_mock->get_last_run_request_body(),
              "{\"client_ip\":\"127.0.0.1\",\"form_type\":\"create_filter\",\"form_realpath\":\"mail.yandex.ru\","
              "\"form_author\":\"4002342134\",\"subject\":\"test_rule\","
              "\"form_fields\":{\"action1\":{\"type\":\"string\",\"filled_by\":\"user\",\"value\":\"move:1\"},"
              "\"condition1\":{\"type\":\"string\",\"filled_by\":\"user\","
              "\"value\":\"(hdr_from_email:\\\"user@domain\\\" OR hdr_from_display_name:\\\"user@domain\\\")\"}}}");
}

TEST(SoCheckFilter, so_check_filter_spam_response) {
    auto cluster_client_mock = get_cluster_client_mock(200, "<spam>1</spam>");
    auto so_check_result = get_so_check_result(cluster_client_mock);
    EXPECT_EQ(so_check_result, processor::so_check::EResult::SPAM);
}

TEST(SoCheckFilter, so_check_filter_error) {
    auto cluster_client_mock = get_cluster_client_mock(501, "");
    auto so_check_result = get_so_check_result(cluster_client_mock);
    EXPECT_EQ(so_check_result, processor::so_check::EResult::ERROR);
}

} // namespace furita

