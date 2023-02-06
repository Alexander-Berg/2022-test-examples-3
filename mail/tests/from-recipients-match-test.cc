
#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/envelope/from_recipients_matcher.h>
#include <vector>

namespace
{

using namespace testing;
using RcptReflection = macs::pg::Recipient::Reflection;
using FromRecipientsMatcher = macs::pg::FromRecipientsMatcher;

struct FromRecipientsMatcherTest : public Test {

    RcptReflection getReflection(const std::string & type, const std::string & name,
            const std::string & email) const {
        return RcptReflection(type, name, email);
    }
};

TEST_F(FromRecipientsMatcherTest, match_withOneRecipinentWithSameNameAndAddress_returnsTrue) {
    std::vector<RcptReflection> reflections = {
        getReflection("from", "Name", "example@ya.ru")
    };
    FromRecipientsMatcher matcher("Name <example@ya.ru>");

    EXPECT_TRUE( matcher.match(reflections) );
}

TEST_F(FromRecipientsMatcherTest, match_withOneRecipinentWithDifferentDisplayName_returnsTrue) {
    std::vector<RcptReflection> reflections = {
        getReflection("from", "Different", "example@ya.ru")
    };
    FromRecipientsMatcher matcher("Name <example@ya.ru>");

    EXPECT_TRUE( matcher.match(reflections) );
}

TEST_F(FromRecipientsMatcherTest, match_withOneRecipinentWithDifferentAddress_returnsFalse) {
    std::vector<RcptReflection> reflections = {
        getReflection("from", "Name", "different@ya.ru")
    };
    FromRecipientsMatcher matcher("Name <example@ya.ru>");

    EXPECT_FALSE( matcher.match(reflections) );
}

TEST_F(FromRecipientsMatcherTest, match_withOneRecipinentWithDifferentType_returnsFalse) {
    std::vector<RcptReflection> reflections = {
        getReflection("to", "Name", "example@ya.ru")
    };
    FromRecipientsMatcher matcher("Name <example@ya.ru>");

    EXPECT_FALSE( matcher.match(reflections) );
}

TEST_F(FromRecipientsMatcherTest,
        match_withManyRecipientsAndOneFromWithSameNameAndAddress_returnsTrue) {
    std::vector<RcptReflection> reflections = {
        getReflection("to", "NameTo", "example-to@ya.ru"),
        getReflection("cc", "NameCc", "example-cc@ya.ru"),
        getReflection("from", "NameFrom", "example-from@ya.ru"),
        getReflection("to", "NameTo2", "example-to2@ya.ru")
    };
    FromRecipientsMatcher matcher("NameFrom <example-from@ya.ru>");

    EXPECT_TRUE( matcher.match(reflections) );
}

TEST_F(FromRecipientsMatcherTest,
        match_withManyRecipientsAndOneFromWithDifferentNameAndAddress_returnsFalse) {
    std::vector<RcptReflection> reflections = {
        getReflection("to", "NameTo", "example-to@ya.ru"),
        getReflection("cc", "NameCc", "example-cc@ya.ru"),
        getReflection("from", "Different", "different@ya.ru"),
        getReflection("to", "NameTo2", "example-to2@ya.ru")
    };
    FromRecipientsMatcher matcher("NameFrom <example-from@ya.ru>");

    EXPECT_FALSE( matcher.match(reflections) );
}

TEST_F(FromRecipientsMatcherTest, match_withManyRecipientsAndManySameFromsInSameOrder_returnsTrue) {
    std::vector<RcptReflection> reflections = {
        getReflection("to", "NameTo", "example-to@ya.ru"),
        getReflection("from", "NameFrom1", "example-from-1@ya.ru"),
        getReflection("cc", "NameCc", "example-cc@ya.ru"),
        getReflection("from", "", "example-from-2@ya.ru"),
        getReflection("to", "NameTo2", "example-to2@ya.ru"),
        getReflection("from", "NameFrom3", "example-from-3@ya.ru")
    };
    FromRecipientsMatcher matcher("NameFrom1 <example-from-1@ya.ru>;"
        " example-from-2@ya.ru; NameFrom3 <example-from-3@ya.ru>");

    EXPECT_TRUE( matcher.match(reflections) );
}

TEST_F(FromRecipientsMatcherTest,
        match_withManyRecipientsAndManyFromsInDifferentOrder_returnsFalse) {
    std::vector<RcptReflection> reflections = {
        getReflection("to", "NameTo", "example-to@ya.ru"),
        getReflection("from", "", "example-from-2@ya.ru"),
        getReflection("cc", "NameCc", "example-cc@ya.ru"),
        getReflection("from", "NameFrom3", "example-from-3@ya.ru"),
        getReflection("to", "NameTo2", "example-to2@ya.ru"),
        getReflection("from", "NameFrom1", "example-from-1@ya.ru")
    };
    FromRecipientsMatcher matcher("NameFrom1 <example-from-1@ya.ru>;"
        " example-from-2@ya.ru; NameFrom3 <example-from-3@ya.ru>");

    EXPECT_FALSE( matcher.match(reflections) );
}

} //namespace

