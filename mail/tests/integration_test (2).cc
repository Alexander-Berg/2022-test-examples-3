#include <sharpei_client/integration.h>

#include <gtest/gtest.h>

namespace  {

using namespace testing;

using sharpei::client::integration::parseSharpeiSettings;
using sharpei::client::integration::ptree;

struct ParseSharpeiSettings : Test {
    ptree data;

    ParseSharpeiSettings() {
        data.add("host", "sharpei");
        data.add("port", "9999");
        data.add("retries", "42");
        data.add("timeout_ms", "100500");
        data.add("keep_alive", "true");
    }
};

TEST_F(ParseSharpeiSettings, for_empty_ptree_should_throw_exception) {
    EXPECT_THROW(parseSharpeiSettings(ptree()), std::runtime_error);
}

TEST_F(ParseSharpeiSettings, for_filled_ptree_should_return_settings) {
    const auto result = parseSharpeiSettings(data);

    EXPECT_EQ(result.sharpeiAddress.host, "sharpei");
    EXPECT_EQ(result.sharpeiAddress.port, 9999u);
    EXPECT_EQ(result.retries, 42u);
    EXPECT_EQ(result.timeout, std::chrono::milliseconds(100500));
    EXPECT_EQ(result.keepAlive, true);
    EXPECT_EQ(result.httpClient.has_value(), false);
}

TEST_F(ParseSharpeiSettings, for_keep_alive_false_should_return_set_false) {
    data.put("keep_alive", "false");

    const auto result = parseSharpeiSettings(data);

    EXPECT_EQ(result.keepAlive, false);
}

TEST_F(ParseSharpeiSettings, for_keep_alive_no_should_return_set_false) {
    data.put("keep_alive", "no");

    const auto result = parseSharpeiSettings(data);

    EXPECT_EQ(result.keepAlive, false);
}

TEST_F(ParseSharpeiSettings, for_keep_alive_yes_should_return_set_true) {
    data.put("keep_alive", "yes");

    const auto result = parseSharpeiSettings(data);

    EXPECT_EQ(result.keepAlive, true);
}

TEST_F(ParseSharpeiSettings, for_keep_alive_0_should_return_set_false) {
    data.put("keep_alive", "0");

    const auto result = parseSharpeiSettings(data);

    EXPECT_EQ(result.keepAlive, false);
}

TEST_F(ParseSharpeiSettings, for_keep_alive_1_should_return_set_true) {
    data.put("keep_alive", "1");

    const auto result = parseSharpeiSettings(data);

    EXPECT_EQ(result.keepAlive, true);
}

TEST_F(ParseSharpeiSettings, for_keep_alive_f_should_return_set_false) {
    data.put("keep_alive", "f");

    const auto result = parseSharpeiSettings(data);

    EXPECT_EQ(result.keepAlive, false);
}

TEST_F(ParseSharpeiSettings, for_keep_alive_t_should_return_set_true) {
    data.put("keep_alive", "t");

    const auto result = parseSharpeiSettings(data);

    EXPECT_EQ(result.keepAlive, true);
}

TEST_F(ParseSharpeiSettings, for_filled_http_client_should_return_settings_only_for_it) {
    const std::string httpClientPath = "http_client";
    const std::string_view clientName = "ymod_http_cluster";

    ptree httpClient;
    httpClient.add("cluster_client_module", clientName);
    data.add_child(httpClientPath, httpClient);

    const auto result = parseSharpeiSettings(data, nullptr);
    EXPECT_EQ(result.httpClient.has_value(), true);
    EXPECT_EQ(result.httpClient->clusterClientModuleName, clientName);

    EXPECT_EQ(result.sharpeiAddress.host, "");
    EXPECT_EQ(result.sharpeiAddress.port, 0u);
    EXPECT_EQ(result.retries, 0u);
    EXPECT_EQ(result.timeout, decltype(result.timeout){0});
    EXPECT_EQ(result.keepAlive, false);
}

} // namespace
