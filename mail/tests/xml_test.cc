#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yamail/data/serialization/libxml.h>


namespace {

using namespace yamail::data::serialization;

TEST(XmlTest, stringWithoutMetaSymbolToXml) {
    EXPECT_EQ(toXml(std::string{"good"}, "tag").str(),
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tag>good</tag>\n");
}

TEST(XmlTest, stringWithMetaSymbolToXml) {
    EXPECT_EQ(toXml(std::string{"good&bad"}, "tag").str(),
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tag>good&amp;bad</tag>\n");
}

TEST(XmlTest, stringViewForStringWithoutMetaSymbolToXml) {
    EXPECT_EQ(toXml(std::string_view{"good"}, "tag").str(),
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tag>good</tag>\n");
}

TEST(XmlTest, stringViewForStringWithMetaSymbolToXml) {
    EXPECT_EQ(toXml(std::string_view{"good&bad"}, "tag").str(),
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tag>good&amp;bad</tag>\n");
}

}
