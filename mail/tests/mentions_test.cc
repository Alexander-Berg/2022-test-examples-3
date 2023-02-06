#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/services/include/smtp_gate.h>

using namespace testing;

namespace sendbernar {

struct EmailRepo {
    EmailVec emails;

    EmailVec toVector() const {
        return emails;
    }
};


TEST(MentionsTest, shouldReturnTrueOnEmptyMentions) {
    EXPECT_TRUE(checkMentionsInRecipients(boost::none, EmailRepo()));
}

TEST(MentionsTest, shouldReturnTrueIfAllMentionsAreSubsetOfEmails) {
    EmailRepo repo{
        { Email("a", "ya.ru"), Email("b", "ya.ru") }
    };

    EXPECT_TRUE(checkMentionsInRecipients(boost::none, repo));
    EXPECT_TRUE(checkMentionsInRecipients(boost::make_optional<params::Mentions>({"a@ya.ru"}), repo));
    EXPECT_TRUE(checkMentionsInRecipients(boost::make_optional<params::Mentions>({"b@ya.ru"}), repo));
    EXPECT_TRUE(checkMentionsInRecipients(boost::make_optional<params::Mentions>({"a@ya.ru", "b@ya.ru"}), repo));
}

TEST(MentionsTest, shouldReturnFalseInCaseOfUnmet) {
    EmailRepo repo{
        { Email("a", "ya.ru"), Email("b", "ya.ru") }
    };

    EXPECT_FALSE(checkMentionsInRecipients(boost::make_optional<params::Mentions>({"c@ya.ru"}), repo));
    EXPECT_FALSE(checkMentionsInRecipients(boost::make_optional<params::Mentions>({"c@ya.ru"}), EmailRepo()));
    EXPECT_FALSE(checkMentionsInRecipients(boost::make_optional<params::Mentions>({"a@ya.ru", "c@ya.ru"}), EmailRepo()));
}

}
