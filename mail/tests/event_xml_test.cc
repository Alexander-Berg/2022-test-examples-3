#include <stdexcept>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/event_xml.h>

namespace msg_body {

using namespace testing;

typedef Test EventXmlTest;

TEST(EventXmlTest, eventXml_simpleParagraph_noTransform) {
    const std::string content = "<p>Hello</p>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<p>Hello</p>", res.content_);
}

TEST(EventXmlTest, eventXml_invalidXml_throwJsException) {
    const std::string content = "<p>Tom & Jerry</p>";
    ASSERT_THROW(eventXmlTransform(content, false, EmbedInfos()), std::runtime_error);
}

TEST(EventXmlTest, eventXml_simpleParagraph_emptyCids) {
    const std::string content = "<p>Hello</p>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_TRUE(res.cids_.empty());
}

TEST(EventXmlTest, eventXml_imgWithCid_convertToInlineImage) {
    const std::string content = "<img src=\"cid:avatar-123456\" alt=\"Аватар\" border=\"0\" />";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<inline-image border=\"0\" alt=\"Аватар\">avatar-123456</inline-image>", res.content_);
}

TEST(EventXmlTest, eventXml_imgWithCid_extractCid) {
    const std::string content = "<img src=\"cid:avatar-123456\" alt=\"Аватар\" border=\"0\" />";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    Cids cids;
    cids.insert("avatar-123456");
    ASSERT_TRUE(cids == res.cids_);
}

TEST(EventXmlTest, eventXml_phishingUrl_redirectUrl) {
    const std::string content = "<p><a class=\"c\" href=\"http://fasebook.com/index.php?param=Qyh83d\">Click me!</a></p>";
    const EventXmlResult res = eventXmlTransform(content, true, EmbedInfos());
    ASSERT_EQ("<p><a class=\"c\" href=\"http://mail.yandex.ru/r?url=http%3A%2F%2Ffasebook.com%2Findex.php%3Fparam%3DQyh83d\">Click me!</a></p>",
        res.content_);
}

TEST(EventXmlTest, eventXml_spanWithoutClass_noChange) {
    const std::string content = "<span>Hello</span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<span>Hello</span>", res.content_);
}

TEST(EventXmlTest, eventXml_spanUnknownClass_noChange) {
    const std::string content = "<span class=\"some-class\">Hello</span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<span class=\"some-class\">Hello</span>", res.content_);
}

TEST(EventXmlTest, eventXml_spanWmiLink_convertToA) {
    const std::string content = "<span class=\"wmi-link\">http://www.yandex.ru</span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<a href=\"http://www.yandex.ru\">http://www.yandex.ru</a>", res.content_);
}

TEST(EventXmlTest, eventXml_spanWmiLinkWithShowAttr_textFromShowAttr) {
    const std::string content = "<span class=\"wmi-link\" show=\"yandex\">http://www.yandex.ru</span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<a href=\"http://www.yandex.ru\">yandex</a>", res.content_);
}

TEST(EventXmlTest, eventXml_spanWmiLinkPhishing_redirectLink) {
    const std::string content = "<span class=\"wmi-link\">http://www.yandex.ru</span>";
    const EventXmlResult res = eventXmlTransform(content, true, EmbedInfos());
    ASSERT_EQ("<a href=\"http://mail.yandex.ru/r?url=http%3A%2F%2Fwww.yandex.ru\">http://www.yandex.ru</a>", res.content_);
}

TEST(EventXmlTest, eventXml_spanWmiLinkWithCgiParams_escapeParams) {
    const std::string content = "<span class=\"wmi-link\">http://www.yandex.ru/search?text=blabla&amp;lang=ru</span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<a href=\"http://www.yandex.ru/search?text=blabla&amp;lang=ru\">http://www.yandex.ru/search?text=blabla&amp;lang=ru</a>", res.content_);
}

TEST(EventXmlTest, eventXml_spanWmiLinkWithCgiParamsWithShow_escapeParams) {
    const std::string content = "<span class=\"wmi-link\" show=\"https://skydrive.live.com/?cid=c8ad44874742a74d&amp;id=C8AD44874742A74D%21141#cid=C8AD44874742A74D&amp;id=C8AD44874742A74D%21141\">http://mail.yandex.ru/re.jsx?h=a,Zxc1DZae0XEBH_kPfaw71w&amp;l=aHR0cHM6Ly9za3lkcml2ZS5saXZlLmNvbS8_Y2lkPWM4YWQ0NDg3NDc0MmE3NGQmaWQ9QzhBRDQ0ODc0NzQyQTc0RCUyMTE0MSNjaWQ9QzhBRDQ0ODc0NzQyQTc0RCZpZD1DOEFENDQ4NzQ3NDJBNzREJTIxMTQx</span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<a href=\"http://mail.yandex.ru/re.jsx?h=a,Zxc1DZae0XEBH_kPfaw71w&amp;l=aHR0cHM6Ly9za3lkcml2ZS5saXZlLmNvbS8_Y2lkPWM4YWQ0NDg3NDc0MmE3NGQmaWQ9QzhBRDQ0ODc0NzQyQTc0RCUyMTE0MSNjaWQ9QzhBRDQ0ODc0NzQyQTc0RCZpZD1DOEFENDQ4NzQ3NDJBNzREJTIxMTQx\">https://skydrive.live.com/?cid=c8ad44874742a74d&amp;id=C8AD44874742A74D%21141#cid=C8AD44874742A74D&amp;id=C8AD44874742A74D%21141</a>", res.content_);
}

TEST(EventXmlTest, eventXml_spanWmiLinkWithBadLink_fixBadLink) {
    const std::string content = "<span class=\"wmi-link\">www.yandex.ru</span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<a href=\"http://www.yandex.ru\">www.yandex.ru</a>", res.content_);
}

TEST(EventXmlTest, eventXml_spanWmiLinkInsideA_simplyAppendText) {
    const std::string content = "<a href=\"http://mail.yandex.ru\"><span class=\"wmi-link\">www.yandex.ru</span></a>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<a href=\"http://mail.yandex.ru\">www.yandex.ru</a>", res.content_);
}

TEST(EventXmlTest, eventXml_spanInsideSpanWmiLink_createAtagInsideSpan) {
    const std::string content = "<span class=\"wmi-link\">http://yandex.ru<span>/search?text=blabla</span></span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<span><a href=\"http://yandex.ru/search?text=blabla\">http://yandex.ru/search?text=blabla</a></span>", res.content_);
}

TEST(EventXmlTest, eventXml_spanWmiMailto_convertToA) {
    const std::string content = "<span class=\"wmi-mailto\">test@yandex.ru</span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<a href=\"mailto:test@yandex.ru\">test@yandex.ru</a>", res.content_);
}

TEST(EventXmlTest, eventXml_spanWmiVideoLinkNoTitle_noChange) {
    const std::string content = "<span class=\"wmi-video-link\">Hello</span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<span class=\"wmi-video-link\">Hello</span>", res.content_);
}

TEST(EventXmlTest, eventXml_spanWmiVideoLinkExistingVideoLink_stringifyVideoLinkAndInsertAsAttr) {
    const std::string content = "<span class=\"wmi-video-link\" title=\"0\">Hello</span>";
    const EmbedInfo link = {0, "http://www.youtube.com/v/onjt2L2lDEY", "vars", 200, 200, "http://youtube.com", "YouTube"};
    EmbedInfos links;
    links.push_back(link);
    const EventXmlResult res = eventXmlTransform(content, false, links);
    ASSERT_EQ("<span params=\"flashvars=vars&amp;height=200&amp;hosting_name=YouTube&amp;player_url=http%3A%2F%2Fwww.youtube.com%2Fv%2Fonjt2L2lDEY&amp;width=200\" class=\"wmi-video-link\">Hello</span>", res.content_);
}

TEST(EventXmlTest, eventXml_httpCapitilize_noFixUrl) {
    const std::string content = "<span class=\"wmi-link\" show=\"Http://ya.ru\">Http://ya.ru</span>";
    const EventXmlResult res = eventXmlTransform(content, false, EmbedInfos());
    ASSERT_EQ("<a href=\"Http://ya.ru\">Http://ya.ru</a>", res.content_);
}

} // namespace msg_body
