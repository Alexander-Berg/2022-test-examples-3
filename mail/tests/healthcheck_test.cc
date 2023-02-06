#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/webmail/ymod_healthcheck/include/healthcheck.h>

using namespace ::testing;


namespace ymod_healthcheck {

TEST(StatusTest, shouldCheckThatDeadIsOnlyDead) {
    EXPECT_FALSE(Healthcheck::alive().isDead());
    EXPECT_TRUE (Healthcheck::alive().isAlive());
    EXPECT_FALSE(Healthcheck::alive().isReady());
}

TEST(StatusTest, shouldCheckThatAliveIsOnlyAlive) {
    EXPECT_FALSE(Healthcheck::alive().isDead());
    EXPECT_TRUE (Healthcheck::alive().isAlive());
    EXPECT_FALSE(Healthcheck::alive().isReady());
}

TEST(StatusTest, shouldCheckThatReadyIsReadyAndAlsoAlive) {
    EXPECT_FALSE(Healthcheck::ready().isDead());
    EXPECT_TRUE (Healthcheck::ready().isAlive());
    EXPECT_TRUE (Healthcheck::ready().isReady());
}

TEST(ReflectionTest, shouldWritePtreeWithTwoValues) {
    yplatform::ptree tree = Healthcheck::alive();

    EXPECT_TRUE(tree.get_optional<bool>("healthcheck.alive"));
    EXPECT_TRUE(tree.get_optional<bool>("healthcheck.ready"));
}

using Method = bool (Healthcheck::*)() const;

struct FoldTest: public TestWithParam<std::tuple<Healthcheck, Healthcheck, Method>> { };

INSTANTIATE_TEST_SUITE_P(foldRules, FoldTest, Values(
    std::make_tuple(Healthcheck::dead(),  Healthcheck::dead(),  &Healthcheck::isDead),
    std::make_tuple(Healthcheck::dead(),  Healthcheck::alive(), &Healthcheck::isDead),
    std::make_tuple(Healthcheck::dead(),  Healthcheck::ready(), &Healthcheck::isDead),

    std::make_tuple(Healthcheck::alive(), Healthcheck::dead(),  &Healthcheck::isDead),
    std::make_tuple(Healthcheck::alive(), Healthcheck::alive(), &Healthcheck::isAlive),
    std::make_tuple(Healthcheck::alive(), Healthcheck::ready(), &Healthcheck::isAlive),

    std::make_tuple(Healthcheck::ready(), Healthcheck::dead(),  &Healthcheck::isDead),
    std::make_tuple(Healthcheck::ready(), Healthcheck::alive(), &Healthcheck::isAlive),
    std::make_tuple(Healthcheck::ready(), Healthcheck::ready(), &Healthcheck::isReady)
));

TEST_P(FoldTest, shouldFoldValues) {
    const auto& [init, right, method] = GetParam();

    const Healthcheck res = Healthcheck::fold(init, static_cast<yplatform::ptree>(right));
    EXPECT_TRUE(((&res)->*method)());
}

TEST_F(FoldTest, shouldNotFoldInCaseOfMissingReflection) {
    EXPECT_TRUE(Healthcheck::fold(Healthcheck::ready(), yplatform::ptree()).isReady());
}

TEST_F(FoldTest, shouldFoldToDeadInCaseOfMalformedReflection) {
    yplatform::ptree malformed;
    malformed.put("healthcheck.alive", "123");
    malformed.put("healthcheck.ready", "123");

    EXPECT_TRUE(Healthcheck::fold(Healthcheck::ready(), malformed).isDead());
}

TEST_F(FoldTest, shouldFoldToDeadInCaseOfPartialReflection) {
    yplatform::ptree malformedAlive;
    malformedAlive.put("healthcheck.alive", "true");

    yplatform::ptree malformedReady;
    malformedReady.put("healthcheck.ready", "true");

    EXPECT_TRUE(Healthcheck::fold(Healthcheck::ready(), malformedAlive).isDead());
    EXPECT_TRUE(Healthcheck::fold(Healthcheck::ready(), malformedReady).isDead());
}


TEST_F(FoldTest, shouldFoldToDeadInCaseOfImpossibleState) {
    yplatform::ptree malformed;
    malformed.put("healthcheck.alive", "false");
    malformed.put("healthcheck.ready", "true");

    EXPECT_TRUE(Healthcheck::fold(Healthcheck::ready(), malformed).isDead());
}
}
