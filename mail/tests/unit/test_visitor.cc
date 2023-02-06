#include <gtest/gtest.h>

#include "helper_context.h"
#include <internal/server/handlers/helpers.h>

using namespace testing;

BOOST_FUSION_DEFINE_STRUCT((york)(tests), Params,
                           (std::string, a)
                           (std::string, b) )

BOOST_FUSION_DEFINE_STRUCT((york)(tests), ParamsWithOptional,
                           (boost::optional<std::string>, a))

BOOST_FUSION_DEFINE_STRUCT((york)(tests), ParamsWithSequence,
                           (std::vector<std::string>, a))

BOOST_FUSION_DEFINE_STRUCT((york)(tests), ParamsWithBool,
                           (bool, a))

BOOST_FUSION_DEFINE_STRUCT((york)(tests), ParamsWithOptBool,
                           (boost::optional<bool>, a))

BOOST_FUSION_DEFINE_STRUCT((york)(tests), ParamsWithInteger,
                           (int, a))

BOOST_FUSION_DEFINE_STRUCT((york)(tests), ParamsWithOptInteger,
                           (boost::optional<int>, a))

namespace york {
namespace tests {

struct GetArgsTest: public Test {
    StrictMock<ContextMock> ctx;
};

TEST_F(GetArgsTest, emptyArgs_givesFalseAndResponses400) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::none));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    Params params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, false);
}

TEST_F(GetArgsTest, haveOneOfTwoArgs_givesFalseAndResponses400) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("1")));
    EXPECT_CALL(ctx, getOptionalArg("b")).WillOnce(Return(boost::none));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    Params params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, false);
}

TEST_F(GetArgsTest, haveBothArgs_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("1")));
    EXPECT_CALL(ctx, getOptionalArg("b")).WillOnce(Return(boost::optional<std::string>("2")));

    Params params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, "1");
    ASSERT_EQ(params.b, "2");
}

TEST_F(GetArgsTest, haveOptionalArg_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("1")));

    ParamsWithOptional params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, boost::optional<std::string>("1"));
}

TEST_F(GetArgsTest, haveNotOptionalArg_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>()));

    ParamsWithOptional params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, boost::none);
}

TEST_F(GetArgsTest, haveJsonArrayArg_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>(R"json(["1", "2"])json")));

    ParamsWithSequence params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, std::vector<std::string>({"1", "2"}));
}

TEST_F(GetArgsTest, requiredBoolEmpty_givesFalseAndResponses400) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::none));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    ParamsWithBool params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, false);
}

TEST_F(GetArgsTest, requiredBoolYes_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("yes")));

    ParamsWithBool params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, true);
}

TEST_F(GetArgsTest, requiredBoolNo_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("no")));

    ParamsWithBool params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, false);
}

TEST_F(GetArgsTest, requiredBoolUnknown_givesFalseAndResponses400) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("asdf")));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    ParamsWithBool params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, false);
}

TEST_F(GetArgsTest, optionalBoolEmpty_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::none));

    ParamsWithOptBool params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, boost::none);
}

TEST_F(GetArgsTest, optionalBoolYes_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("yes")));

    ParamsWithOptBool params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, boost::optional<bool>(true));
}

TEST_F(GetArgsTest, optionalBoolNo_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("no")));

    ParamsWithOptBool params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, boost::optional<bool>(false));
}

TEST_F(GetArgsTest, optionalBoolUnknown_givesFalseAndResponses400) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("asdf")));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    ParamsWithOptBool params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, false);
}

TEST_F(GetArgsTest, intCorrect_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("42")));

    ParamsWithInteger params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, 42);
}

TEST_F(GetArgsTest, intNotCorrect_givesFalseAndResponses400) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("qwerty")));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    ParamsWithInteger params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, false);
}

TEST_F(GetArgsTest, optionalIntCorrect_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("42")));

    ParamsWithOptInteger params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, boost::optional<int>(42));
}

TEST_F(GetArgsTest, optionalIntNotCorrect_givesFalseAndResponses400) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::optional<std::string>("qwerty")));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    ParamsWithOptInteger params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, false);
}

TEST_F(GetArgsTest, optionalIntEmpty_givesTrueAndFillsArgs) {
    EXPECT_CALL(ctx, getOptionalArg("a")).WillOnce(Return(boost::none));

    ParamsWithOptInteger params;
    const auto res = server::handlers::getArgs(ctx, params);
    ASSERT_EQ(res, true);
    ASSERT_EQ(params.a, boost::none);
}

} //namespace tests
} //namespace york
