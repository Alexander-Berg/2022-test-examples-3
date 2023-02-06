#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <algorithm>
#include <map>
#include <iterator>
#include <pgg/cast.h>
#include <internal/label/factory.h>
#include "base-convert-test.h"

namespace macs {
std::ostream & operator << ( std::ostream & s, const Label::Type & v ) {
    s << v.title();
    return s;
}

std::ostream & operator << ( std::ostream & s, const Label::Symbol & v ) {
    s << v.title();
    return s;
}

} // namespace macs


namespace {

using namespace testing;

class ConvertLabelTest : public tests::BaseConvertTest<macs::pg::reflection::Label> {
protected:
    static void fill(Reflection& data) {
        data.lid = 13;
        data.name = "NaMe";
        data.type = "user";
        data.color = "green";
        data.created = 1419580017;
        data.revision = 10;
        data.message_count = 42;
    }

    ConvertLabelTest() {
        modifyData(fill);
    }

    macs::Label convert() const {
        return macs::pg::LabelFactory().fromReflection(data()).product();
    }
};

TEST_F(ConvertLabelTest, LabelConverter) {
    const auto l = convert();
    EXPECT_EQ(l.lid(), "13");
    EXPECT_EQ(l.name(), "NaMe");
    EXPECT_EQ(l.type(), macs::Label::Type::user);
    EXPECT_EQ(l.symbolicName(), macs::Label::Symbol::none);
    EXPECT_EQ(l.color(), "green");
    EXPECT_EQ(l.creationTime(), "1419580017");
    EXPECT_EQ(l.revision(), macs::Revision(10));
    EXPECT_EQ(l.messagesCount(), 42ul);
    EXPECT_FALSE(l.isSystem());
}

TEST_F(ConvertLabelTest, rowWithImapType_set_imap_type) {
    modifyData([] (Reflection& data) {
        data.type = "imap";
    });
    const auto l = convert();
    EXPECT_EQ(l.type(), macs::Label::Type::imap);
}

TEST_F(ConvertLabelTest, rowWithDomainType_set_social_type) {
    modifyData([] (Reflection& data) {
        data.type = "domain";
    });
    const auto l = convert();
    EXPECT_EQ(l.type(), macs::Label::Type::social);
}

TEST_F(ConvertLabelTest, rowWithRpopType_set_rpop_type) {
    modifyData([] (Reflection& data) {
        data.type = "rpop";
    });
    const auto l = convert();
    EXPECT_EQ(l.type(), macs::Label::Type::rpop);
}

TEST_F(ConvertLabelTest, rowWithTypeType_set_spam_defence_type) {
    modifyData([] (Reflection& data) {
        data.type = "type";
    });
    const auto l = convert();
    EXPECT_EQ(l.type(), macs::Label::Type::spamDefense);
}

TEST_F(ConvertLabelTest, rowWithUnkwnonType_throws_exception) {
    modifyData([] (Reflection& data) {
        data.type = "OMG";
    });
    EXPECT_THROW(convert(), std::out_of_range);
}

TEST_F(ConvertLabelTest, rowWithSystemType_set_system_type) {
    modifyData([] (Reflection& data) {
        data.type = "system";
    });
    const auto l = convert();
    EXPECT_EQ(l.type(), macs::Label::Type::system);
}

TEST_F(ConvertLabelTest, rowWithSystemTypeAndPriorityHighName_set_important_symbol) {
    modifyData([] (Reflection& data) {
        data.type = "system";
        data.name = "priority_high";
    });
    const auto l = convert();
    EXPECT_EQ(l.type(), macs::Label::Type::system);
    EXPECT_EQ(l.symbolicName(), macs::Label::Symbol::important_label);
}

TEST_F(ConvertLabelTest, rowWithSystemTypeAndDraftName_set_draft_symbol) {
    modifyData([] (Reflection& data) {
        data.type = "system";
        data.name = "draft";
    });
    const auto l = convert();
    EXPECT_EQ(l.type(), macs::Label::Type::system);
    EXPECT_EQ(l.symbolicName(), macs::Label::Symbol::draft_label);
}

TEST_F(ConvertLabelTest, rowWithSystemTypeAndForwardedName_set_forwarded_symbol) {
    modifyData([] (Reflection& data) {
        data.type = "system";
        data.name = "forwarded";
    });
    const auto l = convert();
    EXPECT_EQ(l.type(), macs::Label::Type::system);
    EXPECT_EQ(l.symbolicName(), macs::Label::Symbol::forwarded_label);
}

TEST_F(ConvertLabelTest, rowWithSystemTypeAndPinnedName_set_pinned_symbol) {
    modifyData([] (Reflection& data) {
        data.type = "system";
        data.name = "pinned";
    });
    const auto l = convert();
    EXPECT_EQ(l.type(), macs::Label::Type::system);
    EXPECT_EQ(l.symbolicName(), macs::Label::Symbol::pinned_label);
}

TEST_F(ConvertLabelTest, rowWithUserTypeAndPriorityHighName__dont_set_important_symbol) {
    modifyData([] (Reflection& data) {
        data.type = "user";
        data.name = "priority_high";
    });
    const auto l = convert();
    EXPECT_EQ(l.type(), macs::Label::Type::user);
    EXPECT_EQ(l.name(), "priority_high");
    EXPECT_EQ(l.symbolicName(), macs::Label::Symbol::none);
}

} // namespace
