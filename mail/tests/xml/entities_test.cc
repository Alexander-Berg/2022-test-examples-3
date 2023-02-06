#include <gtest/gtest.h>

#include <butil/xml/entities.h>

namespace {

TEST(XmlEntitiesTest, decodeXmlEntities_withEntities_returnsStringWithGlyphs)
{
    std::string str = "&lt;&amp;&gt;=&apos;NO!&apos;!=&quot;YES&quot;";
    ASSERT_EQ(decodeXmlEntities(str), "<&>=\'NO!\'!=\"YES\"" );
}

TEST(XmlEntitiesTest, decodeXmlEntities_emptyString_returnEmptyString) {
    std::string str = "";
    ASSERT_EQ("", decodeXmlEntities(str));
}

TEST(XmlEntitiesTest, decodeXmlEntities_withDoubleEscapedAmpersand_decodeAmpersandOnlyOnce) {
    std::string str = "&lt;tag attr=first&amp;amp;second&gt;";
    ASSERT_EQ("<tag attr=first&amp;second>", decodeXmlEntities(str));
}

TEST(XmlEntitiesTest, decodeXmlEntities_entityWithoutSemicolon_doNotChange) {
    std::string str = "&lttag attr=first&amp;second&gt";
    ASSERT_EQ("&lttag attr=first&second&gt", decodeXmlEntities(str));
}

TEST(XmlEntitiesTest, encodeXmlEntities_emptyString_returnEmptyString) {
    std::string str = "";
    ASSERT_EQ("", encodeXmlEntities(str));
}

TEST(XmlEntitiesTest, encodeXmlEntities_withGlyphs_returnStringWithEntities) {
    std::string str = "<&>=\'NO!\'!=\"YES\"";
    ASSERT_EQ("&lt;&amp;&gt;=&apos;NO!&apos;!=&quot;YES&quot;", encodeXmlEntities(str));
}

TEST(XmlEntitiesTest, encodeXmlEntities_withEntities_doubleEscapeAmpersand) {
    std::string str = "first&second&amp;third";
    ASSERT_EQ("first&amp;second&amp;amp;third", encodeXmlEntities(str));
}

TEST(XmlEntitiesTest, encodeXmlEntities_withGlyphAtEnd_returnStringWithEntities) {
    std::string str = "first&second&";
    ASSERT_EQ("first&amp;second&amp;", encodeXmlEntities(str));
}

}
