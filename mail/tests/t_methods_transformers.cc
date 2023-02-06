#include <ymod_webserver/methods/transform.h>
#include <catch.hpp>

namespace ymod_webserver {

using std::string;
typedef std::map<string, string> params_type;

TEST_CASE("methods/transform/single_string_verified_as_string_with_no_errors", "")
{
    auto t = transformer(argument<string>("p"));
    auto error = t.validate(params_type{ { "p", "test" } });
    REQUIRE(!error);
}

TEST_CASE("methods/transform/single_string_verified_as_int_with_error", "")
{
    auto t = transformer(argument<int>("p"));
    auto error = t.validate(params_type{ { "p", "test" } });
    REQUIRE(static_cast<bool>(error));
}

TEST_CASE("methods/transform/empty_verified_as_int_with_error", "")
{
    auto t = transformer(argument<int>("p"));
    auto error = t.validate(params_type{});
    REQUIRE(static_cast<bool>(error));
}

TEST_CASE("methods/transform/argument_with_no_value_gives_missing_argument", "")
{
    auto t = transformer(argument<string>("p"));
    auto error = t.validate(params_type{ { "p", "" } });
    REQUIRE(static_cast<bool>(error));
    REQUIRE(error.reason == "missing argument");
    REQUIRE(error.argument_name == "p");
}

TEST_CASE("methods/transform/text_to_string_with_int_validator_with_error", "")
{
    auto t = transformer(argument<string>("p", validator(isdigit)));
    auto error = t.validate(params_type{ { "p", "test" } });
    REQUIRE(static_cast<bool>(error));
}

TEST_CASE("methods/transform/digits_to_string_with_int_validator_with_no_error", "")
{
    auto t = transformer(argument<string>("p", validator(isdigit)));
    auto error = t.validate(params_type{ { "p", "12345" } });
    REQUIRE(!error);
}

TEST_CASE("methods/transform/signed_digits_to_int_with_no_error", "")
{
    auto t = transformer(argument<int>("p"));
    auto error = t.validate(params_type{ { "p", "-1234" } });
    REQUIRE(!error);
}

TEST_CASE("methods/transform/signed_digits_to_unsigned_validate_fails", "")
{
    auto t = transformer(argument<unsigned>("p"));
    auto error = t.validate(params_type{ { "p", "-1234" } });
    REQUIRE(static_cast<bool>(error));
}

TEST_CASE("methods/transform/signed_digits_to_string_has_correct_value", "")
{
    auto t = transformer(argument<int>("p"));
    decltype(t)::results_type results;
    t.transform(params_type{ { "p", "-1234" } }, results);
    REQUIRE(std::get<0>(results) == -1234);
}

TEST_CASE("methods/transform/converter_gets_first_char_with_no_error", "")
{
    auto t = transformer(argument<char>(
        "p", default_validator<string>(), converter([](const string& s) -> char { return s[0]; })));
    auto error = t.validate(params_type{ { "p", "12345" } });
    REQUIRE(!error);
}

TEST_CASE("methods/transform/converter_gets_first_char_with_correct_result", "")
{
    auto t = transformer(argument<char>(
        "p", default_validator<string>(), converter([](const string& s) -> char { return s[0]; })));
    decltype(t)::results_type results;
    t.transform(params_type{ { "p", "12345" } }, results);
    REQUIRE(std::get<0>(results) == '1');
}

TEST_CASE("methods/transform/missing_optional_argument_with_no_error", "")
{
    auto t = transformer(optional_argument<int>("p", 5));
    auto error = t.validate(params_type{});
    REQUIRE(!error);
}

TEST_CASE("methods/transform/missing_optional_argument_has_correct_default_value", "")
{
    auto t = transformer(optional_argument<int>("p", 5));
    decltype(t)::results_type results;
    t.transform(params_type{}, results);
    REQUIRE(std::get<0>(results) == 5);
}

TEST_CASE("methods/transform/optional_argument_has_correct_not_default_value", "")
{
    auto t = transformer(optional_argument<int>("p", 5));
    decltype(t)::results_type results;
    t.transform(params_type{ { "p", "6" } }, results);
    REQUIRE(std::get<0>(results) == 6);
}

TEST_CASE("methods/transform/optional_argument_with_aliases_will_take_first_value", "")
{
    auto t = transformer(optional_argument<int>("p", { "alias1", "alias2" }, 5));
    decltype(t)::results_type results;
    t.transform(params_type{ { "alias2", "2" }, { "alias2", "1" } }, results);
    REQUIRE(std::get<0>(results) == 2);
}

TEST_CASE("methods/transform/argument_with_aliases_will_take_nonempty_value", "")
{
    auto t = transformer(argument<int>("p", { "alias1", "alias2" }));
    decltype(t)::results_type results;
    auto error = t.transform(params_type{ { "alias1", "" }, { "alias2", "2" } }, results);
    REQUIRE(std::get<0>(results) == 2);
}

TEST_CASE("methods/transform/optional_argument_will_treat_aliases_not_as_default_value", "")
{
    auto t = transformer(
        optional_argument<string>("p", { "alias1", "alias2" }, "", default_validator<string>()));
    decltype(t)::results_type results;
    t.transform(params_type{ { "alias2", "2" } }, results);
    REQUIRE(std::get<0>(results) == "2");
}

TEST_CASE("methods/transform/if_value_valid_but_converter_throws_then_cause_error", "")
{
    auto t = transformer(optional_argument<int>(
        "p", 5, default_validator<int>(), converter([](const string&) -> int {
            throw std::runtime_error("test runtime error");
        })));
    auto error = t.validate(params_type{ { "p", "123" } });
    REQUIRE(static_cast<bool>(error));
}

}