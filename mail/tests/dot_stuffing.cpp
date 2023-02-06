#include <gtest/gtest.h>
#include <dot_stuffer.h>
#include <string>

using namespace ymod_smtpclient;
using namespace testing;

TEST(DotStuffer, WithoutPeriods) {
    std::string message = "first line\nsecond line\nthird line";
    std::string transformed;

    DotStuffer dotStuffer;
    dotStuffer.apply(message.begin(), message.end(), transformed);

    EXPECT_EQ(transformed, message);
}

TEST(DotStuffer, WithPeriodsInsideLine) {
    std::string message = "first .. line .\n second...line.\nthird .line\n";
    std::string transformed;

    DotStuffer dotStuffer;
    dotStuffer.apply(message.begin(), message.end(), transformed);

    EXPECT_EQ(transformed, message);
}

TEST(DotStuffer, WithPeriodsInTheBeginOfTheLine) {
    std::string message = "\n\n. one period, expected two\n.\n.\n";
    message += ".. two periods, expected three\n";
    message += "without periods\n";
    message += "... three periods, expected four";

    std::string expected = "\n\n.. one period, expected two\n..\n..\n";
    expected += "... two periods, expected three\n";
    expected += "without periods\n";
    expected += ".... three periods, expected four";

    std::string transformed;

    DotStuffer dotStuffer;
    dotStuffer.apply(message.begin(), message.end(), transformed);

    EXPECT_EQ(transformed, expected);
}

TEST(DotStuffer, CheckApplyMoreThenOneTime) {
    std::string message = ". one period, expected two.\n";

    DotStuffer dotStuffer;
    std::string transformed;

    dotStuffer.apply(message.begin(), message.end(), transformed);

    message = ". this line starts with one period \n..";
    dotStuffer.apply(message.begin(), message.end(), transformed);

    message = ". three periods, expect";
    dotStuffer.apply(message.begin(), message.end(), transformed);

    message = "ed four!\n";
    dotStuffer.apply(message.begin(), message.end(), transformed);

    std::string expected = ".. one period, expected two.\n";
    expected += ".. this line starts with one period \n";
    expected += ".... three periods, expected four!\n";

    EXPECT_EQ(transformed, expected);
}
