
#include <gtest/gtest.h>
#include <butil/xml/routines.h>

namespace {

TEST(XmlTest, xmlOpenTag_noAttrs_tagWithAngles) {
    ASSERT_EQ("<tag>", xmlOpenTag("tag"));
}

TEST(XmlTest, xmlOpenTag_attrs_putAttrs) {
    XmlAttrs attrs;
    attrs["name1"] = "value1";
    attrs["name2"] = "value2";
    ASSERT_EQ("<tag name2=\"value2\" name1=\"value1\">", xmlOpenTag("tag", attrs));
}

TEST(XmlTest, xmlOpenTag_attrsWithXmlEntities_escapeXmlEntities) {
    XmlAttrs attrs;
    attrs["name"] = "<va&lu'e>";
    ASSERT_EQ("<tag name=\"&lt;va&amp;lu&apos;e&gt;\">", xmlOpenTag("tag", attrs));
}

TEST(XmlTest, xmlWrap_emptyContent_noClosingTag) {
    ASSERT_EQ("<tag/>", xmlWrap("tag", ""));
}

TEST(XmlTest, xmlWrap_emptyContentWithAttrs_noClosingTagWithAttrs) {
    XmlAttrs attrs;
    attrs["name"] = "value";
    ASSERT_EQ("<tag name=\"value\"/>", xmlWrap("tag", "", attrs));
}

TEST(XmlTest, xmlWrap_nonEmptyContent_withClosingTag) {
    ASSERT_EQ("<tag>content</tag>", xmlWrap("tag", "content"));
}

TEST(XmlTest, xmlWrap_nonEmptyContentWithAttrs_withClosingTagAndAttrs) {
    XmlAttrs attrs;
    attrs["name1"] = "value1";
    attrs["name2"] = "value2";
    ASSERT_EQ("<tag name2=\"value2\" name1=\"value1\">content</tag>", xmlWrap("tag", "content", attrs));
}

TEST(XmlTest, xmlWrap_attrsWithXmlEntities_escapeXmlEntities) {
    XmlAttrs attrs;
    attrs["name"] = "<va&lu'e>";
    ASSERT_EQ("<tag name=\"&lt;va&amp;lu&apos;e&gt;\">content</tag>", xmlWrap("tag", "content", attrs));
}

TEST(XmlTest, getXmlEncoding_simpleXmlWithKoi_returnKoi) {
    const std::string str = "<?xml encoding=\"koi8-r\"?><root></root>";
    ASSERT_EQ("koi8-r", butil::getXmlEncoding(str));
}

TEST(XmlTest, getXmlEncoding_noEncoding_returnUtf8) {
    const std::string str = "<?xml version=\"1.0\"?><root></root>";
    ASSERT_EQ("utf-8", butil::getXmlEncoding(str));
}

TEST(XmlTest, getXmlEncoding_errataEncodin_returnUtf8) {
    const std::string str = "<?xml encodin=\"cp1251\"?><root></root>";
    ASSERT_EQ("utf-8", butil::getXmlEncoding(str));
}

TEST(XmlTest, getXmlEncoding_versionAndEncoding_returnEncoding) {
    const std::string str = "<?xml version=\"1.0\" encoding=\"cp1251\"?><root></root>";
    ASSERT_EQ("cp1251", butil::getXmlEncoding(str));
}

TEST(XmlTest, getXmlEncoding_encodingInsideTag_returnUtf8) {
    const std::string str = "<?xml version=\"1.0\" ?><root encoding=\"koi8-r\"></root>";
    ASSERT_EQ("utf-8", butil::getXmlEncoding(str));
}

TEST(XmlTest, getXmlEncoding_noXmlHeader_returnUtf8) {
    const std::string str = "<root></root>";
    ASSERT_EQ("utf-8", butil::getXmlEncoding(str));
}

TEST(XmlTest, getXmlEncoding_emptyEncoding_returnUtf8) {
    const std::string str = "<?xml encoding=\"\"?><root></root>";
    ASSERT_EQ("utf-8", butil::getXmlEncoding(str));
}

TEST(XmlTest, getXmlEncoding_invalidXml_returnUtf8) {
    const std::string str = "&some>$# !<strange?> <?string encoding=\"koi8-r\"";
    ASSERT_EQ("utf-8", butil::getXmlEncoding(str));
}

TEST(XmlTest, getXmlEncoding_invalidXmlWithHeader_returnEncoding) {
    const std::string str = "<?xml encoding=\"cp1251\"?>abracadabra";
    ASSERT_EQ("cp1251", butil::getXmlEncoding(str));
}

}
