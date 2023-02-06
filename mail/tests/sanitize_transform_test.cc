#include <stdexcept>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/sanitize_transform.h>

namespace msg_body {

using namespace testing;
using namespace mail_getter;

typedef Test SanitizeTransformTest;

class MockInlineSpoofer: public InlineSpoofer {
public:
    MOCK_METHOD(std::string, getUrl, (const CidPartInfo&), (const, override));
};

CidParts getCidParts() {
    CidParts cidParts;
    CidPartInfo cidPart;
    cidPart.hid = "1.1.2";
    cidPart.mid = "2160000000004476215";
    cidPart.stid = "1000007.1120000000330824.170573770323993403125548517005";
    cidPart.name = "avatar.jpg";
    cidParts["avatar-123456"] = cidPart;
    return cidParts;
}

SanitizerParsedResponse getSanitizeData() {
    SanitizerParsedResponse sanitizeData;
    sanitizeData.html = "<img src=\"cid:avatar-123456\" alt=\"Аватар\" border=\"0\" />";
    SanitizerMarkupEntry entry;
    entry.type = SanitizerMarkupType_Cid;
    entry.position.attributeStart = 5;
    entry.position.attributeLength = 23;
    entry.position.dataStart = 10;
    entry.position.dataLength = 17;
    sanitizeData.markup.push_back(entry);
    return sanitizeData;
}

TEST(SanitizeTransformTest, eventXml_simpleParagraph_emptyCids) {
    CidParts cidParts;
    SanitizerParsedResponse sanitizeData;
    sanitizeData.html = "<p>Hello</p>";
    MockInlineSpoofer mockSpoofer;
    SanitizeTransformParams params;
    params.SetCidParts(cidParts);
    auto res = sanitizeTransform(mockSpoofer, sanitizeData, params);
    ASSERT_TRUE(res.cids.empty());
}

TEST(SanitizeTransformTest, eventXml_imgWithCid_extractCid) {
    CidParts cidParts = getCidParts();
    SanitizerParsedResponse sanitizeData = getSanitizeData();
    MockInlineSpoofer mockSpoofer;
    EXPECT_CALL(mockSpoofer, getUrl(_)).WillOnce(Return("../message_part"));
    SanitizeTransformParams params;
    params.SetCidParts(cidParts);
    auto res = sanitizeTransform(mockSpoofer, sanitizeData, params);
    Cids cids;
    cids.insert("avatar-123456");
    ASSERT_TRUE(cids == res.cids);
}

TEST(SanitizeTransformTest, eventXml_imgWithCid_ignoreExtractCid_skipCid) {
    SanitizerParsedResponse sanitizeData = getSanitizeData();
    MockInlineSpoofer mockSpoofer;
    SanitizeTransformParams params;
    auto res = sanitizeTransform(mockSpoofer, sanitizeData, params);
    ASSERT_TRUE(res.cids.empty());
}

TEST(SanitizeTransformTest, sanitizeTransform_spooferReturnUrl_replaceUrlWithSpoofedUrl) {
    CidParts cidParts = getCidParts();
    SanitizerParsedResponse sanitizeData = getSanitizeData();
    MockInlineSpoofer mockSpoofer;
    EXPECT_CALL(mockSpoofer, getUrl(_)).WillOnce(Return("webattach.mail.yandex.net/spoofed"));
    SanitizeTransformParams params;
    params.SetCidParts(cidParts);
    auto res = sanitizeTransform(mockSpoofer, sanitizeData, params);
    EXPECT_EQ("<img src=\"webattach.mail.yandex.net/spoofed\" alt=\"Аватар\" border=\"0\" />",
        res.content);
}

TEST(SanitizeTransformTest, sanitizeTransform_unsafeLinkInContent_wrapUnsafeLink) {
    MockInlineSpoofer mockSpoofer;
    CidParts cidParts;
    SanitizerParsedResponse sanitizeData;
    sanitizeData.html = R"(<a href="http://some.host.ru/path?param1=value1">http://some.host.ru/path</a>)";
    sanitizeData.markup.push_back(mail_getter::SanitizerMarkupEntry{
        mail_getter::SanitizerMarkupType_UnsafeLink,
        boost::none,
        {3, 45, 9, 38}
    });

    const std::string expectedResult = R"(<a href="http://mail.yandex.ru/r?url=http%3a%2f%2fsome.host.ru%2fpath%3fparam1%3dvalue1">http://some.host.ru/path</a>)";

    SanitizeTransformParams params;
    params.SetCidParts(cidParts);
    auto res = sanitizeTransform(mockSpoofer, sanitizeData, params);

    EXPECT_EQ(res.content, expectedResult);
}

} // namespace msg_body
