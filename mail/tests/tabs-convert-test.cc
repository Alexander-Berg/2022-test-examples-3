#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/query/ids.h>
#include <algorithm>
#include <map>
#include <iterator>
#include <yamail/data/serialization/json_writer.h>
#include <pgg/cast.h>
#include <internal/tab/factory.h>
#include "base-convert-test.h"

namespace {

using namespace testing;

class ConvertTabTest : public tests::BaseConvertTest<macs::pg::reflection::Tab> {
protected:
    static void fill(Reflection& data) {
        data.tab = "";
        data.revision = 10;
        data.created = 1419580017;
        data.message_count = 100500;
        data.message_seen = 100000;
        data.message_size = 4294967296;
        data.fresh_count = 13;
    }

    ConvertTabTest() {
        modifyData(fill);
    }

    macs::Tab convert() const {
        return macs::pg::TabFactory().fromReflection(data()).release();
    }
};


TEST_F(ConvertTabTest, TabConverter) {
    macs::Tab t = convert();
    EXPECT_EQ(t.type(), macs::Tab::Type::unknown);
    EXPECT_EQ(t.revision(), macs::Revision(10));
    EXPECT_EQ(t.creationTime(), 1419580017);
    EXPECT_EQ(t.messagesCount(), 100500ul);
    EXPECT_EQ(t.newMessagesCount(), 500ul);
    EXPECT_EQ(t.freshMessagesCount(), 13ul);
    EXPECT_EQ(t.bytes(), 4294967296ul);
}

TEST_F(ConvertTabTest, rowWithRelevantType_set_relevant_type) {
    modifyData([] (Reflection& data) {
        data.tab = "relevant";
    });
    macs::Tab t = convert();
    EXPECT_EQ(t.type(), macs::Tab::Type::relevant);
}

TEST_F(ConvertTabTest, rowWithNewsType_set_news_type) {
    modifyData([] (Reflection& data) {
        data.tab = "news";
    });
    macs::Tab t = convert();
    EXPECT_EQ(t.type(), macs::Tab::Type::news);
}

TEST_F(ConvertTabTest, rowWithSocialType_set_social_type) {
    modifyData([] (Reflection& data) {
        data.tab = "social";
    });
    macs::Tab t = convert();
    EXPECT_EQ(t.type(), macs::Tab::Type::social);
}

} // namespace
