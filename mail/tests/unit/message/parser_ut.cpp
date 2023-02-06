#include "fakes/context.h"

#include <mail/notsolitesrv/src/message/parser/message_handler.h>
#include <mail/notsolitesrv/src/message/parser/rfc2231.h>
#include <mail/notsolitesrv/src/message/parser.h>

#include <library/cpp/resource/resource.h>
#include <util/memory/blob.h>

#include <gtest/gtest.h>

#include <algorithm>
#include <fstream>
#include <string>
#include <vector>

using namespace testing;
using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NDetail;

class TMessageParserTest: public Test {
public:
    void ReadEml(const std::string& name) {
        auto data = NResource::Find(std::string("messages/") + name);
        MessageData.assign(data.data(), data.size());
        RootPart = ParseMessage(MessageData, GetContext());
        ASSERT_TRUE(RootPart);
    }

protected:
    std::string MessageData;
    TPartPtr RootPart;
};

TEST(ParseMessage, ErrorWhenEmptyBodyWithHeaders) {
    EXPECT_THROW(ParseMessage("Subject: test\r\nX-Yandex-Hint: hint\r\n", GetContext()), std::runtime_error);
}

TEST(ParseMessage, ErrorWhenEmptyBodyWithHeader) {
    EXPECT_THROW(ParseMessage("X-Yandex-Hint: hint\r\n", GetContext()), std::runtime_error);
}

TEST(ParseMessage, ErrorWhenEmptyBodyWithoutHeaders) {
    EXPECT_THROW(ParseMessage("\r\n", GetContext()), std::runtime_error);
}

TEST(ParseMessage, ErrorWhenEmptyBodyAndEmptyHeader) {
    EXPECT_THROW(ParseMessage("", GetContext()), std::runtime_error);
}

TEST_F(TMessageParserTest, SimpleRootPart) {
    ReadEml("simple.eml");
    EXPECT_TRUE(RootPart->GetFrom());
    EXPECT_TRUE(RootPart->GetTo());
    EXPECT_FALSE(RootPart->GetCc());
    EXPECT_EQ(RootPart->GetFrom()->size(), 1ul);
    EXPECT_EQ(RootPart->GetTo()->size(), 1ul);

    const auto& from = RootPart->GetFrom()->front();
    EXPECT_EQ(from.first, "Ugly B");
    EXPECT_EQ(from.second, "b@b.ru");

    const auto& to = RootPart->GetTo()->front();
    EXPECT_TRUE(to.first.empty());
    EXPECT_EQ(to.second, "a@a.ru");

    ASSERT_EQ(std::distance(RootPart->begin(), RootPart->end()), 1l);
    EXPECT_EQ(RootPart->GetOffset(), 99ul);
    EXPECT_EQ(RootPart->GetLength(), 11ul);
    EXPECT_EQ(RootPart->GetHid(), "1");
    EXPECT_EQ(RootPart->GetContentType(), "text");
    EXPECT_EQ(RootPart->GetContentSubtype(), "plain");
    EXPECT_EQ(RootPart->GetEncoding(), "7bit");
    EXPECT_EQ(RootPart->GetCharset(), "US-ASCII");
    EXPECT_TRUE(RootPart->GetContentDisposition().empty());
}

TEST_F(TMessageParserTest, SimpleRootPartWithUtf8DisplayNames) {
    ReadEml("simple-utf.eml");
    EXPECT_TRUE(RootPart->GetFrom());
    EXPECT_TRUE(RootPart->GetTo());
    EXPECT_FALSE(RootPart->GetCc());
    EXPECT_EQ(RootPart->GetFrom()->size(), 1ul);
    EXPECT_EQ(RootPart->GetTo()->size(), 1ul);

    const auto& from = RootPart->GetFrom()->front();
    EXPECT_EQ(from.first, "Йа креведко");
    EXPECT_EQ(from.second, "b@b.ru");

    const auto& to = RootPart->GetTo()->front();
    EXPECT_EQ(to.first, "Превед");
    EXPECT_EQ(to.second, "a@a.ru");

    ASSERT_EQ(std::distance(RootPart->begin(), RootPart->end()), 1l);
    EXPECT_EQ(RootPart->GetOffset(), 116ul);
    EXPECT_EQ(RootPart->GetLength(), 10ul);
    EXPECT_EQ(RootPart->GetHid(), "1");
    EXPECT_EQ(RootPart->GetContentType(), "text");
    EXPECT_EQ(RootPart->GetContentSubtype(), "plain");
    EXPECT_EQ(RootPart->GetEncoding(), "7bit");
    EXPECT_EQ(RootPart->GetCharset(), "US-ASCII");
}

TEST_F(TMessageParserTest, SimpleRootPartWithMultilineFromAddress) {
    ReadEml("simple-multiline.eml");
    EXPECT_TRUE(RootPart->GetFrom());
    EXPECT_TRUE(RootPart->GetTo());
    EXPECT_FALSE(RootPart->GetCc());
    EXPECT_EQ(RootPart->GetFrom()->size(), 1ul);
    EXPECT_EQ(RootPart->GetTo()->size(), 1ul);

    const auto& from = RootPart->GetFrom()->front();
    EXPECT_EQ(from.first, "Ugly Multiline B");
    EXPECT_EQ(from.second, "b@b.ru");

    ASSERT_EQ(std::distance(RootPart->begin(), RootPart->end()), 1l);
    EXPECT_EQ(RootPart->GetOffset(), 61ul);
    EXPECT_EQ(RootPart->GetLength(), 10ul);
    EXPECT_EQ(RootPart->GetHid(), "1");
    EXPECT_EQ(RootPart->GetContentType(), "text");
    EXPECT_EQ(RootPart->GetContentSubtype(), "plain");
    EXPECT_EQ(RootPart->GetEncoding(), "7bit");
    EXPECT_EQ(RootPart->GetCharset(), "US-ASCII");
}

TEST_F(TMessageParserTest, HeadersNamesValuesAndOrderStaysUnchanged) {
    ReadEml("simple-multiline.eml");
    std::vector<std::pair<std::string, std::string>> originalHeaders{
        {"To",      "a@a.ru"},
        {"from",    "\"Ugly\n Multiline B\" <b@b.ru>"},
        {"Subject", "test"}
    };
    size_t i = 0;
    for (const auto& header: RootPart->GetHeaders()) {
        ASSERT_LT(i, originalHeaders.size());
        const auto& originalHeader = originalHeaders[i++];
        EXPECT_EQ(header.first, originalHeader.first);
        EXPECT_EQ(header.second, originalHeader.second);
    }
    EXPECT_EQ(i, originalHeaders.size());
}

struct TPartCheck {
    std::string Id;
    std::string CType;
    std::string CSubtype;
    std::string Boundary;
    std::string Name;
    std::string Filename;
    std::string ContentDisposition;
    size_t Offset;
    size_t Length;
};

TEST_F(TMessageParserTest, BoundaryIsAPrefixOfAnother) {
    ReadEml("boundary.eml");
    std::vector<TPartCheck> checks{
        { "1",     "multipart", "related",     "----=_NextPart_3",     "", "", "", 981,  1263 },
        { "1.1",   "multipart", "alternative", "----=_NextPart_3_alt", "", "", "", 1070, 1151 },
        { "1.1.1", "text",      "plain",       "",                     "", "", "", 1168, 80 },
        { "1.1.2", "text",      "html",        "",                     "", "", "", 1346, 850 }
    };
    size_t i = 0;
    for (const auto& part: *RootPart) {
        ASSERT_LT(i, checks.size());
        const auto& check = checks.at(i);

        EXPECT_EQ(part.GetHid(), check.Id);
        EXPECT_EQ(part.GetContentType(), check.CType);
        EXPECT_EQ(part.GetContentSubtype(), check.CSubtype);
        EXPECT_EQ(part.GetBoundary(), check.Boundary);
        EXPECT_EQ(part.GetName(), check.Name);
        EXPECT_EQ(part.GetFilename(), check.Filename);
        EXPECT_EQ(part.GetContentDisposition(), check.ContentDisposition);
        EXPECT_EQ(part.GetOffset(), check.Offset);
        EXPECT_EQ(part.GetLength(), check.Length);

        i++;
    }
}

TEST_F(TMessageParserTest, InlineRootPart) {
    ReadEml("inline.eml");
    std::vector<TPartCheck> checks{
        { "1",         "multipart", "mixed",       "bound",                "",        "", "", 740,  2668 },
        { "1.1",       "text",      "plain",       "",                     "",        "", "", 821,  16 },
        { "1.2",       "text",      "html",        "",                     "",        "", "", 923,  22 },
        { "1.3",       "message",   "rfc822",      "",                     "",        "", "", 1016, 2242 },
        { "1.3.1",     "multipart", "related",     "----=_NextPart_3",     "",        "", "", 1997, 1262 },
        { "1.3.1.1",   "multipart", "alternative", "----=_NextPart_3_alt", "",        "", "", 2086, 1151 },
        { "1.3.1.1.1", "text",      "plain",       "",                     "",        "", "", 2184, 80 },
        { "1.3.1.1.2", "text",      "html",        "",                     "",        "", "", 2362, 850 },
        { "1.4",       "message",   "rfc822",      "",                     "message", "", "", 3343, 54 },
        { "1.4.1",     "text",      "plain",       "",                     "",        "", "", 3392, 6 }
    };
    size_t i = 0;
    for (const auto& part: *RootPart) {
        ASSERT_LT(i, checks.size());
        const auto& check = checks.at(i);

        EXPECT_EQ(part.GetHid(), check.Id);
        EXPECT_EQ(part.GetContentType(), check.CType);
        EXPECT_EQ(part.GetContentSubtype(), check.CSubtype);
        EXPECT_EQ(part.GetBoundary(), check.Boundary);
        EXPECT_EQ(part.GetName(), check.Name);
        EXPECT_EQ(part.GetFilename(), check.Filename);
        EXPECT_EQ(part.GetContentDisposition(), check.ContentDisposition);
        EXPECT_EQ(part.GetOffset(), check.Offset);
        EXPECT_EQ(part.GetLength(), check.Length);

        i++;
    }
}

TEST_F(TMessageParserTest, Rfc2231FilenameEncoding) {
    ReadEml("rfc2231.eml");
    ASSERT_EQ(std::distance(RootPart->begin(), RootPart->end()), 2l);
    auto part = std::next(RootPart->begin());
    EXPECT_EQ(part->GetName(), "Тестовое.docx");
    EXPECT_EQ(part->GetFilename(), "Тестовое.docx");
}

TEST_F(TMessageParserTest, BrokenRfc2231FilenameEncoding) {
    ReadEml("broken_rfc2231.eml");
    ASSERT_EQ(std::distance(RootPart->begin(), RootPart->end()), 2l);
    auto part = std::next(RootPart->begin());
    EXPECT_EQ(part->GetName(), "Тестовое.docx");
    EXPECT_EQ(part->GetFilename(), "Тестовое.docx");
}

TEST_F(TMessageParserTest, IllFormedFromHeader) {
    ReadEml("daria-24681.eml");
    const auto& from = RootPart->GetFrom();
    ASSERT_TRUE(from);
    EXPECT_EQ(from->size(), 1ul);
    EXPECT_EQ(from->front().first, "Display Name");
    EXPECT_EQ(from->front().second, "login@domain");
}

TEST_F(TMessageParserTest, ContentDispositionAttachment) {
    ReadEml("att_all.eml");
    EXPECT_EQ(RootPart->GetContentDisposition(), "");
    for (const auto& part: RootPart->GetChildren()) {
        if (part->GetContentType() != "text") {
            EXPECT_EQ(part->GetContentDisposition(), "attachment");
        }
    }
}

TEST_F(TMessageParserTest, ContentDispositionInline) {
    ReadEml("mproto2462.eml");
    EXPECT_EQ(RootPart->GetContentDisposition(), "");
    // hid=1.2 has Content-Disposition: inline; ...
    EXPECT_EQ(RootPart->GetChildren().back()->GetContentDisposition(), "inline");
}

TEST_F(TMessageParserTest, TooManyInlineMessages) {
    ReadEml("too_many_inline.eml");
    auto it = RootPart->begin();

    // find last part
    while (std::distance(it, RootPart->end()) > 1) {
        ++it;
    }

    // test that we do not parse innermost text/plain part due to inline limit
    EXPECT_NE(it->GetContentType(), "text");
    EXPECT_NE(it->GetContentSubtype(), "plain");
    EXPECT_NE(std::string(TString{it->GetBody()}), "Here we are!\n");
}

TEST_F(TMessageParserTest, TooManyPartsThrowsException) {
    auto data = NResource::Find("messages/too_many_inline.eml");
    std::string msg{data.data(), data.size()};
    EXPECT_THROW(ParseMessage(msg, GetContext({{"max_parts", "10"}})), std::runtime_error);
}

TEST_F(TMessageParserTest, IncorrectContentType) {
    ReadEml("incorrect-c-type.eml");
    std::vector<TPartCheck> checks{
        { "1",     "multipart",   "mixed",        "", "", "", "", 0, 0 },
        { "1.1",   "application", "vnd.ms-excel", "", "", "", "", 0, 0 },
        { "1.2",   "application", "vnd.ms-excel", "", "", "", "", 0, 0 },
        { "1.3",   "multipart",   "alternative",  "", "", "", "", 0, 0 },
        { "1.3.1", "text",        "plain",        "", "", "", "", 0, 0 },
        { "1.3.2", "text",        "html",         "", "", "", "", 0, 0 }
    };
    size_t i = 0;
    for (const auto& part: *RootPart) {
        ASSERT_LT(i, checks.size());
        const auto& check = checks.at(i);

        EXPECT_EQ(part.GetHid(), check.Id);
        EXPECT_EQ(part.GetContentType(), check.CType);
        EXPECT_EQ(part.GetContentSubtype(), check.CSubtype);

        i++;
    }
}

TEST_F(TMessageParserTest, IncorrectAddress) {
    ReadEml("incorrect-addr.eml");
    ASSERT_TRUE(RootPart->GetFrom());
    ASSERT_EQ(RootPart->GetFrom()->size(), 1ul);

    // test for bug2bug compatibility with fastsrv
    EXPECT_TRUE(RootPart->GetFrom()->front().first.empty());
    EXPECT_EQ(RootPart->GetFrom()->front().second, "//Host_Name/admin/imhttp://Host_Name/admin/img/email.gifg/email.gif>");
}

namespace mulca_mime {

bool operator==(const DecodedString& lhs, const DecodedString& rhs) {
    return std::tie(lhs.charset, lhs.language, lhs.contents) == std::tie(rhs.charset, rhs.language, rhs.contents);
}

std::ostream& operator<<(std::ostream& os, const DecodedString& v) {
    os << "(" << v.charset << "/" << v.language << ") " << v.contents;
    return os;
}

} // namespace mulca_mime

TEST(ParseRfc2231Value, EmptyStringIsInvalid) {
    mulca_mime::DecodedString res;
    EXPECT_FALSE(ParseRfc2231Value("", res));
    EXPECT_EQ(res, mulca_mime::DecodedString("", "", "", 0));
}

TEST(ParseRfc2231Value, ValueWithCharsetAndLang) {
    mulca_mime::DecodedString res;
    ASSERT_TRUE(ParseRfc2231Value("cp1251'ru'%ef%f0%e8%e2%e5%f2", res));
    EXPECT_EQ(res, mulca_mime::DecodedString("cp1251", "ru", "%ef%f0%e8%e2%e5%f2", 0));
}

TEST(ParseRfc2231Value, ValueWithCharsetAndEmptyLang) {
    mulca_mime::DecodedString res;
    ASSERT_TRUE(ParseRfc2231Value("cp1251''%ef%f0%e8%e2%e5%f2", res));
    EXPECT_EQ(res, mulca_mime::DecodedString("cp1251", "", "%ef%f0%e8%e2%e5%f2", 0));
}

TEST(ParseRfc2231Value, ValueWithEmptyCharsetIsInvalid) {
    mulca_mime::DecodedString res;
    ASSERT_FALSE(ParseRfc2231Value("'ru'%ef%f0%e8%e2%e5%f2", res));
}

TEST(ParseRfc2231Value, InvalidChars) {
    mulca_mime::DecodedString res;
    ASSERT_FALSE(ParseRfc2231Value("utf-8'ru'><", res));
}

TEST(ParseRfc2231Value, UnescapedUtfIsInvalid) {
    mulca_mime::DecodedString res;
    ASSERT_FALSE(ParseRfc2231Value("utf-8'ru'testтест", res));
}

TEST(HandleContentId, Empty) {
    EXPECT_EQ(HandleContentId(""), "");
}

TEST(HandleContentId, OnlySpaces) {
    EXPECT_EQ(HandleContentId("    "), "");
}

TEST(HandleContentId, OnlySpacesAndOpenAngleBrace) {
    EXPECT_EQ(HandleContentId("    <"), "");
}

TEST(HandleContentId, StripBracesAndTrailers) {
    EXPECT_EQ(HandleContentId("  <content_id> trailer"), "content_id");
}

TEST(HandleContentId, StopOnSpace) {
    EXPECT_EQ(HandleContentId("<first second>"), "first");
}

TEST(HandlePossiblyIncorrectRfc2231Value, ValidRfc2231Value) {
    std::string res;
    ASSERT_TRUE(HandlePossiblyIncorrectRfc2231Value("cp1251'ru'%ef%f0%e8%e2%e5%f2", res));
    EXPECT_EQ(res, "привет");
}

TEST(HandlePossiblyIncorrectRfc2231Value, ValidRfc2231ValueWithWrongCharset) {
    std::string res;
    ASSERT_TRUE(HandlePossiblyIncorrectRfc2231Value("utf8'ru'%EF%f0%E8%e2%E5%f2%2C%20world", res));
    EXPECT_EQ(res, "привет, world");
}
