#include <message_processor/parser.h>
#include <catch.hpp>

namespace botserver::message_processor {

using args_map = map<string, string>;

auto make_message(string text)
{
    auto message = make_shared<gate_message>();
    message->text = text;
    return message;
}

struct t_parser
{
    parser parser;
};

TEST_CASE_METHOD(t_parser, "start")
{
    auto command = parser(make_message("/start"));
    REQUIRE(command.name == command_name::start);
    REQUIRE(command.args == args_map{});
}

TEST_CASE_METHOD(t_parser, "debug")
{
    auto command = parser(make_message("/debug"));
    REQUIRE(command.name == command_name::debug);
    REQUIRE(command.args == args_map{});
}

TEST_CASE_METHOD(t_parser, "forward")
{
    auto command = parser(make_message("Hello my dear bot"));
    REQUIRE(command.name == command_name::forward);
    REQUIRE(command.args == args_map{});
}

TEST_CASE_METHOD(t_parser, "email/no_args")
{
    auto command = parser(make_message("/email"));
    REQUIRE(command.name == command_name::email);
    REQUIRE(command.args == args_map{ { "email", "" } });
}

TEST_CASE_METHOD(t_parser, "email/extra_args")
{
    auto command = parser(make_message("/email yapoptest@yandex.ru 123"));
    REQUIRE(command.name == command_name::email);
    REQUIRE(command.args == args_map{ { "email", "yapoptest@yandex.ru" } });
}

TEST_CASE_METHOD(t_parser, "email")
{
    auto command = parser(make_message("/email yapoptest@yandex.ru"));
    REQUIRE(command.name == command_name::email);
    REQUIRE(command.args == args_map{ { "email", "yapoptest@yandex.ru" } });
}

TEST_CASE_METHOD(t_parser, "code/no_args")
{
    auto command = parser(make_message("/code"));
    REQUIRE(command.name == command_name::code);
    REQUIRE(command.args == args_map{ { "code", "" } });
}

TEST_CASE_METHOD(t_parser, "code/extra_args")
{
    auto command = parser(make_message("/code 1234 1234"));
    REQUIRE(command.name == command_name::code);
    REQUIRE(command.args == args_map{ { "code", "1234" } });
}

TEST_CASE_METHOD(t_parser, "code")
{
    auto command = parser(make_message("/code 1234"));
    REQUIRE(command.name == command_name::code);
    REQUIRE(command.args == args_map{ { "code", "1234" } });
}

}