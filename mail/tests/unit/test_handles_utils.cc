#include "test_with_task_context.h"

#include <internal/http/detail/request.h>
#include <internal/http/detail/handlers/utils.h>
#include <internal/common/error_code.h>
#include <internal/common/types_reflection.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace macs::settings {

static bool operator ==(const TextTraits& lhs, const TextTraits& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const Signature& lhs, const Signature& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

}

namespace settings {

static bool operator ==(const Email& lhs, const Email& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const MapOptions& lhs, const MapOptions& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

}


namespace {

using namespace testing;
using namespace settings;
using namespace settings::http::detail::handlers;
using namespace settings::http::detail::format;

using Headers = ymod_webserver::header_map_t;
using Params = ymod_webserver::param_map_t;
using Request = ymod_webserver::request;
using RequestPtr = settings::http::detail::RequestPtr;
using Type = settings::http::detail::format::Type;


TEST(TestHandlerUtils, for_get_existing_header_should_return_its_content) {
    Request request;
    request.headers = {{"answer_to_life_the_universe_and_everything", "42"}};
    EXPECT_EQ(
        getHeader(
            request,
            "answer_to_life_the_universe_and_everything",
            "43"
        ),
        "42"
    );
}

TEST(TestHandlerUtils, for_get_nonexistent_header_should_return_default_value) {
    Request request;
    request.headers = Headers {};
    EXPECT_EQ(getHeader(request, "be_or_not_to_be", "bee"), "bee");
}

TEST(TestHandlerUtils, for_get_valid_uid_from_request_parameters_should_return_it) {
    RequestPtr request = boost::make_shared<Request>();
    request->url.params = {{"uid", "228"}};
    EXPECT_EQ(getUid(request), "228");
}

TEST(TestHandlerUtils, for_get_nonexistent_uid_parameter_from_request_should_return_empty_string) {
    RequestPtr request = boost::make_shared<Request>();
    EXPECT_EQ(getUid(request), "");
}

TEST(TestHandlerUtils, for_get_valid_format_from_request_parameters_should_return_it) {
    auto result = getFormat({{"format", "json"}});
    ASSERT_TRUE(result);
    EXPECT_EQ(result.value(), Type::Json);
}

TEST(TestHandlerUtils, for_get_invalid_format_from_request_parameters_should_throw_exception) {
    auto result = getFormat({{"format", "yml"}});
    ASSERT_FALSE(result);
    EXPECT_EQ(result.error(), Error::invalidParameterError);
}

TEST(TestHandlerUtils, for_get_nonexistent_format_parameter_from_request_should_return_json) {
    auto result = getFormat({});
    ASSERT_TRUE(result);
    EXPECT_EQ(result.value(), Type::Json);
}

TEST(TestHandlerUtils, for_get_existing_header_should_return_optional_with_its_content) {
    RequestPtr request = boost::make_shared<Request>();
    request->headers = {{"answer_to_life_the_universe_and_everything", "42"}};
    EXPECT_EQ(
        getOptionalHeader(
            request,
            "answer_to_life_the_universe_and_everything"
        ),
        std::optional("42")
    );
}

TEST(TestHandlerUtils, for_get_nonexistent_header_should_return_nullopt) {
    RequestPtr request = boost::make_shared<Request>();
    request->headers = Headers {};
    EXPECT_EQ(getOptionalHeader(request, "be_or_not_to_be"), std::nullopt);
}

TEST(TestHandlerUtils, for_not_valid_settings_name_header_should_return_false) {
    EXPECT_FALSE(isValidParamName("_name"));
}

TEST(TestHandlerUtils, for_valid_settings_name_header_should_return_true) {
    EXPECT_TRUE(isValidParamName("name"));
}

struct TestValidSettings: TestWithParam<std::tuple<std::string, bool>> {
};

TEST_P(TestValidSettings, test_validation_url_parameters_settings_name) {
    EXPECT_EQ(
        isValidSettings(std::get<0>(GetParam())),
        std::get<1>(GetParam())
    );
}

INSTANTIATE_TEST_SUITE_P(
    test_validation_url_parameters_settings_name,
    TestValidSettings,
    Values(
        std::make_tuple(
            "uid",
            false
        ),
        std::make_tuple(
            "suid",
            false
        ),
        std::make_tuple(
            "mdb",
            false
        ),
        std::make_tuple(
            "ask_validator",
            false
        ),
        std::make_tuple(
            "format",
            false
        ),
        std::make_tuple(
            "db_role",
            false
        ),
        std::make_tuple(
            "settings_list",
            false
        ),
        std::make_tuple(
            "name",
            true
        )
    )
);

TEST(TestGetSettings, for_settings_in_request_body_should_return_MapOptions) {
    const std::string body = R"({"signs":[{"associated_emails":[], "is_default":false,)"
        R"("text":"<div>-- </div><div>meow</div>"}],)"
        R"("single_settings":{"enable_social_notification":"on", "localize_imap":""}})";
    MapOptions  settings;
    Signature signature;
    signature.text = "<div>-- </div><div>meow</div>";
    settings.signs = {signature};
    settings.single_settings = SettingsMap {{"localize_imap", ""}, {"enable_social_notification", "on"}};
    const auto result = getSettingsFromBody(body);
    ASSERT_TRUE(result);
    EXPECT_EQ(result.value(), settings);
}

TEST(TestGetSettings, for_empty_settings_in_request_body_should_throw_exception) {
    std::string body = R"({})";
    EXPECT_THROW(getSettingsFromBody(body), std::exception);
}

TEST(TestGetSettings, for_wrong_parameters_name_should_return_error) {
    const auto result = getSettingsFromParams({{"_wrong_parameter's_name", ""}});
    ASSERT_FALSE(result);
    EXPECT_EQ(result.error(), Error::noSuchNode);
}

TEST(TestGetSettings, for_settings_in_url_parameters_should_return_MapOptions) {
    MapOptions  settings;
    settings.single_settings = SettingsMap {{"from_name", "hello kitty"}};
    const auto result = getSettingsFromParams({{"uid", "42"}, {"from_name", "hello kitty"}});
    ASSERT_TRUE(result);
    EXPECT_EQ(result.value(), settings);
}

TEST(TestGetSettings, for_settings_in_json_data_url_parameter_should_return_MapOptions) {
    MapOptions  settings;
    settings.single_settings = SettingsMap {{"localize_imap", ""}, {"enable_social_notification", "on"}};
    const auto result = getSettingsFromParams(
        {{"json_data", R"({"single_settings":{"enable_social_notification":"on", "localize_imap":""}})"}}
    );
    ASSERT_TRUE(result);
    EXPECT_EQ(result.value(), settings);
}

TEST(TestGetSettings, for_wrong_parameters_name_in_json_data_url_parameter_should_return_error) {
    MapOptions  settings;
    settings.single_settings = SettingsMap {{"localize_imap", ""}, {"enable_social_notification", "on"}};
    const auto result = getSettingsFromParams(
        {{"json_data", R"({"single_settings":{"_enable_social_notification":"on", "localize_imap":""}})"}}
    );
    ASSERT_FALSE(result);
    EXPECT_EQ(result.error(), Error::noSuchNode);
}

TEST(TestGetSettings, for_settings_in_sings_url_parameter_should_return_MapOptions) {
    MapOptions  settings;
    Signature signature;
    signature.text = "<div>-- </div><div>meow</div>";
    settings.signs = {signature};
    const auto result = getSettingsFromParams(
        {{"signs", R"([{"associated_emails":[], "is_default":false,)"
        R"("text":"<div>-- </div><div>meow</div>"}])"}}
    );
    ASSERT_TRUE(result);
    EXPECT_EQ(result.value(), settings);
}

struct TestGetParameters : public TestWithTaskContext {};

TEST_F(TestGetParameters, for_no_uid_in_url_parameters_should_return_error) {
    withSpawn([](const auto& context) {
        const auto result = getUid(context, {});
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::invalidParameterError);
    });
}

TEST_F(TestGetParameters, for_not_valid_uid_in_url_parameters_should_return_error) {
    withSpawn([](const auto& context) {
        const auto result = getUid(context, {{"uid", "uid"}});
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::invalidParameterError);
    });
}

TEST_F(TestGetParameters, for_zero_uid_in_url_parameters_should_return_error) {
    withSpawn([](const auto& context) {
        const auto result = getUid(context, {{"uid", "0"}});
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::invalidParameterError);
    });
}

TEST_F(TestGetParameters, for_uid_in_url_parameters_should_return_uid_in_context) {
    withSpawn([](const auto& context) {
        const auto result = getUid(context, {{"uid", "42"}});
        ASSERT_TRUE(result);
        EXPECT_EQ(context->uid(), "42");
    });
}

TEST_F(TestGetParameters, for_no_settings_list_in_url_parameters_should_return_error) {
    withSpawn([](const auto& context) {
        const auto result = getSettingsListFromParams(context, {});
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::invalidParameterError);
    });
}

TEST_F(TestGetParameters, for_empty_settings_list_in_url_parameters_should_return_error) {
    withSpawn([](const auto& context) {
        const auto result = getSettingsListFromParams(context, {{"settings_list", ""}});
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::invalidParameterError);
    });
}

TEST_F(TestGetParameters, for_settings_list_in_url_parameters_should_return_settings_list_in_context) {
    withSpawn([](const auto& context) {
        const auto result = getSettingsListFromParams(context, {{"settings_list", "one\rtwo"}});
        ASSERT_TRUE(result);
        EXPECT_THAT(context->settingsList(), ContainerEq(std::vector<std::string> {"one", "two"}));
    });
}

TEST_F(TestGetParameters, for_no_ask_validator_in_url_parameters_should_return_false_ask_validator_in_context) {
    withSpawn([](const auto& context) {
        const auto result = getAskValidator(context, {});
        ASSERT_TRUE(result);
        EXPECT_FALSE(context->askValidator());
    });
}

TEST_F(TestGetParameters, for_not_y_ask_validator_in_url_parameters_should_return_false_ask_validator_in_context) {
    withSpawn([](const auto& context) {
        const auto result = getAskValidator(context, {{"ask_validator", "228"}});
        ASSERT_TRUE(result);
        EXPECT_FALSE(context->askValidator());
    });
}

TEST_F(TestGetParameters, for_y_ask_validator_in_url_parameters_should_return_true_ask_validator_in_context) {
    withSpawn([](const auto& context) {
        const auto result = getAskValidator(context, {{"ask_validator", "y"}});
        ASSERT_TRUE(result);
        EXPECT_TRUE(context->askValidator());
    });
}

TEST_F(TestGetParameters, for_no_db_role_in_url_parameters_should_return_master_db_role_in_context) {
    withSpawn([](const auto& context) {
        const auto result = getDatabaseRole(context, {});
        ASSERT_TRUE(result);
        EXPECT_EQ(context->databaseRole(), DatabaseRole::Master);
    });
}

TEST_F(TestGetParameters, for_master_db_role_in_url_parameters_should_return_master_db_role_in_context) {
    withSpawn([](const auto& context) {
        const auto result = getDatabaseRole(context, {{"db_role", "master"}});
        ASSERT_TRUE(result);
        EXPECT_EQ(context->databaseRole(), DatabaseRole::Master);
    });
}

TEST_F(TestGetParameters, for_replica_db_role_in_url_parameters_should_return_replica_db_role_in_context) {
    withSpawn([](const auto& context) {
        const auto result = getDatabaseRole(context, {{"db_role", "replica"}});
        ASSERT_TRUE(result);
        EXPECT_EQ(context->databaseRole(), DatabaseRole::Replica);
    });
}

TEST_F(TestGetParameters, for_not_replica_or_master_db_role_in_url_parameters_should_return_error) {
    withSpawn([](const auto& context) {
        const auto result = getDatabaseRole(context, {{"db_role", "tutu"}});
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::invalidParameterError);
    });
}

TEST_F(TestGetParameters, for_no_format_in_url_parameters_should_return_json) {
    const auto result = getFormat({});
    ASSERT_TRUE(result);
    EXPECT_EQ(result.value(), Type::Json);
}

TEST_F(TestGetParameters, for_json_format_in_url_parameters_should_return_json) {
    const auto result = getFormat({{"format", "json"}});
    ASSERT_TRUE(result);
    EXPECT_EQ(result.value(), Type::Json);
}

TEST_F(TestGetParameters, for_no_json_format_in_url_parameters_should_return_error) {
    const auto result = getFormat({{"format", "not_json"}});
    ASSERT_FALSE(result);
    EXPECT_EQ(result.error(), Error::invalidParameterError);
}

TEST_F(TestGetParameters, for_settings_list_in_request_body_should_return_settings_list_in_context) {
    withSpawn([](const auto& context) {
        const auto result = getSettingsListFromBody(context, R"(["one", "two"])");
        ASSERT_TRUE(result);
        EXPECT_THAT(context->settingsList(), ContainerEq(std::vector<std::string> {"one", "two"}));
    });
}

TEST_F(TestGetParameters, for_empty_settings_list_in_request_body_should_return_error) {
    withSpawn([](const auto& context) {
        const auto result = getSettingsListFromBody(context, R"([])");
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::invalidParameterError);
    });
}

TEST_F(TestGetParameters, for_wrong_settings_list_in_request_body_should_return_error) {
    withSpawn([](const auto& context) {
        const auto result = getSettingsListFromBody(context, R"([)");
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::noSuchNode);
    });
}

}
