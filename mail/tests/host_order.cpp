#include <gtest/gtest.h>
#include <nwsmtp/host_chooser.h>

using namespace testing;
using namespace NNwSmtp;

TEST(host_order, test_primary_preferred) {
    std::string primary = "primary";
    std::string secondary = "secondary";
    unsigned attemptsPerHost = 2;

    using Chooser = HostChooser<PRIMARY_PREFERRED>;

    EXPECT_EQ(Chooser::choose(primary, secondary, attemptsPerHost, 0), "primary");
    EXPECT_EQ(Chooser::choose(primary, secondary, attemptsPerHost, 1), "primary");
    EXPECT_EQ(Chooser::choose(primary, secondary, attemptsPerHost, 2), "secondary");
    EXPECT_EQ(Chooser::choose(primary, secondary, attemptsPerHost, 3), "secondary");
}

TEST(host_order, test_alternating) {
    std::string primary = "primary";
    std::string secondary = "secondary";
    unsigned attemptsPerHost = 3;

    using Chooser = HostChooser<ALTERNATING>;

    EXPECT_EQ(Chooser::choose(primary, secondary, attemptsPerHost, 0), "primary");
    EXPECT_EQ(Chooser::choose(primary, secondary, attemptsPerHost, 1), "secondary");
    EXPECT_EQ(Chooser::choose(primary, secondary, attemptsPerHost, 2), "primary");
    EXPECT_EQ(Chooser::choose(primary, secondary, attemptsPerHost, 3), "secondary");
    EXPECT_EQ(Chooser::choose(primary, secondary, attemptsPerHost, 4), "primary");
    EXPECT_EQ(Chooser::choose(primary, secondary, attemptsPerHost, 5), "secondary");
}

TEST(host_order, test_exception) {
    std::string primary = "primary";
    std::string secondary = "secondary";

    try {
        HostChooser<ALTERNATING>::choose(primary, secondary, 3, 10);
        FAIL();
    } catch (const std::exception& e) {
        ASSERT_STREQ("too many attempts", e.what());
    }

    try {
        HostChooser<PRIMARY_PREFERRED>::choose(primary, secondary, 3, 10);
        FAIL();
    } catch (const std::exception& e) {
        ASSERT_STREQ("too many attempts", e.what());
    }
}

