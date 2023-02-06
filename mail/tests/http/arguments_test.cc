
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <butil/http/arguments.h>

namespace {
using namespace testing;
using namespace http;

TEST(ArgsTest, combineArgs_urlAndArgs_combineThem) {
    const std::string url = "http://yandex.ru/search?text=hello&xml=da";
    HttpArguments args;
    args.add("uid", "123");
    args.add("mdb", "pg");
    ASSERT_EQ("?text=hello&xml=da&mdb=pg&uid=123", combineArgs(url, args));
}

TEST(ArgsTest, combineArgs_urlWithoutArgsAndArgs_onlyArgs) {
    const std::string url = "http://yandex.ru/search";
    HttpArguments args;
    args.add("uid", "123");
    args.add("mdb", "pg");
    ASSERT_EQ("?mdb=pg&uid=123", combineArgs(url, args));
}

TEST(ArgsTest, combineArgs_urlAndEmptyArgs_onlyFromUrl) {
    const std::string url = "http://yandex.ru/search?text=hello&xml=da";
    const HttpArguments args;
    ASSERT_EQ("?text=hello&xml=da", combineArgs(url, args));
}

TEST(ArgsTest, combineArgs_urlWithoutArgsAndEmptyArgs_emptyString) {
    const std::string url = "http://yandex.ru/search";
    const HttpArguments args;
    ASSERT_EQ("", combineArgs(url, args));
}

TEST(ArgsTest, combineArgs_urlWithoutArgsAndEmptyArgsAndNotAskQuestion_emptyString) {
    const std::string url = "http://yandex.ru/search";
    const HttpArguments args;
    ASSERT_EQ("", combineArgs(url, args, false));
}

TEST(ArgsTest, combineArgs_urlAndArgsDoNotAskQuestion_combineThemNoQuestion) {
    const std::string url = "http://yandex.ru/search?text=hello&xml=da";
    HttpArguments args;
    args.add("uid", "123");
    args.add("mdb", "pg");
    ASSERT_EQ("text=hello&xml=da&mdb=pg&uid=123", combineArgs(url, args, false));
}

TEST(FlatTest, shouldMakeSimpleMap) {
    HttpArguments args;
    args.add("uid", "123");
    args.add("mdb", "pg");

    EXPECT_THAT(args.flatten(), UnorderedElementsAre(std::make_pair("uid", "123"), std::make_pair("mdb", "pg")));
}

TEST(FlatTest, shouldTransformEmptyParam) {
    HttpArguments args;
    args.add("uid", "123");
    args.add("mdb", "");

    EXPECT_THAT(args.flatten(), UnorderedElementsAre(std::make_pair("uid", "123"), std::make_pair("mdb", "")));
}

TEST(FlatTest, shouldParseOnlyQueryFromUrl) {
    HttpArguments args;
    args.fromUrl("http://localhost/get_data?arg1=val1&arg2=val2");

    EXPECT_THAT(args.flatten(), UnorderedElementsAre(std::make_pair("arg1", "val1"), std::make_pair("arg2", "val2")));
}

TEST(FlatTest, shouldThrowAnExceptionInCaseOfManyArgumentsWithSameName) {
    HttpArguments args;
    args.add("uid", "123");
    args.add("uid", "456");

    EXPECT_THROW(args.flatten(), std::runtime_error);
}

TEST(FlatTest, shouldReturnEmptyMapOnEmptyArguments) {
    HttpArguments args;

    EXPECT_TRUE(args.flatten().empty());
}

} // namespace
