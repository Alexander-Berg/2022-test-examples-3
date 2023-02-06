#include <mdb/build_envelope.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

static bool operator==(const Email& left, const Email& right) {
    return left.displayName() == right.displayName() 
        && left.local() == right.local()
        && left.domain() == right.domain();
}

namespace {

using namespace ::testing;

using NMdb::BuildEnvelope;

struct TTestBuildEnvelopeHelper : public Test {
    void TestParsedTo(macs::Emails inputEmails, macs::Emails shouldBeOutputEmails) {
        NMdb::THeaders headers;
        for (const auto& email : inputEmails) {
            if (!headers.To.empty()) { 
                headers.To += ", ";
            }
            headers.To += email.displayName() + "<" + email.local() + "@" + email.domain() + ">";
        }
        macs::Envelope envelope = BuildEnvelope(NMdb::TMessageBase{}, {}, {}, {}, headers, {}, nullptr);

        EXPECT_TRUE(envelope.parsedTo());
        const auto& parsedTo = *envelope.parsedTo();
        EXPECT_EQ(parsedTo.size(), shouldBeOutputEmails.size());
        for (size_t i = 0; i < parsedTo.size(); ++i) {
            EXPECT_EQ(parsedTo[i], shouldBeOutputEmails[i]);
        }
    }
};

TEST_F(TTestBuildEnvelopeHelper, for_to_header_assign_parsedTo) {
    macs::Emails emails;
    emails.emplace_back("foobar", "test.yandex.net", "Foo bar");
    emails.emplace_back("bazbaz", "test.yandex.ru", "Baz baz");

    TestParsedTo(emails, emails);
}

TEST_F(TTestBuildEnvelopeHelper, for_to_header_and_very_long_display_name_assign_parsedTo_and_name_truncated) {
    macs::Emails emails, shouldBeOutputEmails;
    std::string veryLongDisplayName;
    for (size_t i = 0; i < 4096; ++i) {
        veryLongDisplayName += "foo";
    }
    emails.emplace_back("foobar", "test.yandex.net", veryLongDisplayName);
    shouldBeOutputEmails.emplace_back("foobar", "test.yandex.net", veryLongDisplayName.substr(0, 1024));
    emails.emplace_back("bazbaz", "test.yandex.ru", "Baz baz");
    shouldBeOutputEmails.emplace_back("bazbaz", "test.yandex.ru", "Baz baz");

    TestParsedTo(emails, shouldBeOutputEmails);
}

}
