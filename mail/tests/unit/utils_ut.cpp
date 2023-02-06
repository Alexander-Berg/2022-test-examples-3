#include <mail/notsolitesrv/src/util/headers.h>
#include <mail/notsolitesrv/src/util/md5.h>
#include <mail/notsolitesrv/src/util/message.h>
#include <mail/notsolitesrv/src/util/optional.h>
#include <mail/notsolitesrv/src/util/string.h>
#include <mail/notsolitesrv/src/types/time.h>

#include <mimeparser/rfc2822date.h>
#include <util/generic/yexception.h>

#include <gtest/gtest.h>

#include <boost/optional/optional_io.hpp>

#include <limits>

namespace {

using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NUtil;

std::string ExtractLoginTest(const std::string& email, bool isOk = true) {
    std::string login;
    EXPECT_EQ(ExtractLogin(email, login), isOk);
    return login;
}

TEST(NUtilString, ExtractLogin) {
    EXPECT_EQ(ExtractLoginTest("user@example.com"), "user@example.com");
    EXPECT_EQ(ExtractLoginTest("user+plus_hack@example.com"), "user@example.com");
    EXPECT_EQ(ExtractLoginTest("'user.with|extra+symbols+#@example.com"),
        "'user.with|extra@example.com");
    EXPECT_EQ(ExtractLoginTest("пользователь@яндекс.рф"), "пользователь@яндекс.рф");
    EXPECT_EQ(ExtractLoginTest("\"user\"@example.com"), "user@example.com");
}

TEST(NUtilString, ExtractLoginFailed) {
    EXPECT_EQ(ExtractLoginTest("invalid\"user@example.com", false), "");
    EXPECT_EQ(ExtractLoginTest("", false), "");
}

TEST(NUtilString, DeBackslash) {
    EXPECT_EQ(DeBackslash("test"), "test");
    EXPECT_EQ(DeBackslash("\\test"), "test");
    EXPECT_EQ(DeBackslash("te\\\\st"), "te\\st");
}

TEST(NUtilString, StripBadChars) {
    EXPECT_EQ(StripBadChars("normal string"), "normal string");
    EXPECT_EQ(StripBadChars({"normal string with \0", 20}), "normal string with ");
    EXPECT_EQ(StripBadChars({"ill-formed\0 string with \0", 25}), "ill-formed");
    EXPECT_EQ(StripBadChars({"\0should be empty", 16}), "");
}

TEST(NUtilString, SplitEmail) {
    std::string login, domain;

    EXPECT_TRUE(SplitEmail("login@domain", login, domain));
    EXPECT_EQ(login, "login");
    EXPECT_EQ(domain, "domain");

    EXPECT_TRUE(SplitEmail("login@", login, domain));
    EXPECT_EQ(login, "login");
    EXPECT_TRUE(domain.empty());

    EXPECT_TRUE(SplitEmail("@domain", login, domain));
    EXPECT_TRUE(login.empty());
    EXPECT_EQ(domain, "domain");

    EXPECT_FALSE(SplitEmail("login_domain", login, domain));
}

TEST(NUtilString, JoinBy) {
    struct T {
        int Int;
        T(int i): Int(i) {}
        std::string Get() const { return std::to_string(Int * 2); }
    };
    std::vector<T> data{1, 2, 3, 4, 5, 6, 7};
    std::vector<T> empty;

    EXPECT_EQ(JoinBy(data, ", ", std::mem_fn(&T::Int)), "1, 2, 3, 4, 5, 6, 7");
    EXPECT_EQ(JoinBy(std::next(data.begin()), data.end(), ", ", std::mem_fn(&T::Get)), "4, 6, 8, 10, 12, 14");

    EXPECT_EQ(JoinBy(empty, ", ", std::mem_fn(&T::Int)), "");
    EXPECT_EQ(JoinBy(empty.begin(), empty.end(), ", ", std::mem_fn(&T::Get)), "");
}

TEST(NUtilHeaders, MakeRfc2822Date) {
    EXPECT_EQ(MakeRfc2822Date(1522938309), "Thu, 05 Apr 2018 17:25:09 +0300");
}

TEST(NUtilHeaders, MimeParserRfc2822DateParser) {
    rfc2822::rfc2822date mpDate("Thu, 13 Feb 2014 12:50:50 +0400 (MSK)");
    EXPECT_EQ(mpDate.unixtime(), 1392281450);
}


TEST(NUtilHeaders, MakeXYForwardValue) {
    EXPECT_EQ(MakeXYForwardValue("1234567890"), "d8ea24a26056e08c2f0267bf70eda7c4");
    EXPECT_EQ(MakeXYForwardValue(""), "18f05aeddc212b523b40818fa2b87b33");
}

TEST(NUtilHeaders, DecodeHeaderRfc2047Simple) {
    EXPECT_EQ(DecodeHeaderRfc2047("simple string", "utf-8"), "simple string");
    EXPECT_EQ(DecodeHeaderRfc2047("просто строчка", "utf-8"), "просто строчка");
}

TEST(NUtilHeaders, DecodeHeaderRfc2047SingleBase64) {
    EXPECT_EQ(DecodeHeaderRfc2047("=?utf-8?B?c2ltcGxlIHN0cmluZw==?=", "utf-8"), "simple string");
    EXPECT_EQ(DecodeHeaderRfc2047("=?cp1251?B?c2ltcGxlIHN0cmluZw==?=", "utf-8"), "simple string");
    EXPECT_EQ(DecodeHeaderRfc2047("=?utf-8?B?0L/RgNC+0YHRgtC+INGB0YLRgNC+0YfQutCw?=", "utf-8"),
        "просто строчка");
    EXPECT_EQ(DecodeHeaderRfc2047("=?cp1251?B?7/Du8fLuIPHy8O736uA=?=", "utf-8"), "просто строчка");
}

TEST(NUtilHeaders, DecodeHeaderRfc2047_SingleBase64Fallback) {
    EXPECT_EQ(DecodeHeaderRfc2047("=?cp1?B?0L/RgNC+0YHRgtC+INGB0YLRgNC+0YfQutCw?=", "utf-8"),
        "просто строчка");
}

TEST(NUtilHeaders, DecodeHeaderRfc2047SingleQP) {
    EXPECT_EQ(DecodeHeaderRfc2047("=?utf-8?Q?simple=20string?=", "utf-8"), "simple string");
    EXPECT_EQ(DecodeHeaderRfc2047("=?utf-8?Q?=D1=81=D1=82=D1=80=D0=BE=D1=87=D0=BA=D0=B0?=", "utf-8"),
        "строчка");
    EXPECT_EQ(DecodeHeaderRfc2047("=?cp1251?Q?=EF=F0=EE=F1=F2=EE=20=F1=F2=F0=EE=F7=EA=E0?=", "utf-8"),
        "просто строчка");
}

TEST(NUtilHeaders, DecodeHeaderRfc2047SingleQPFallback) {
    EXPECT_EQ(DecodeHeaderRfc2047("=?cp1?Q?=D1=81=D1=82=D1=80=D0=BE=D1=87=D0=BA=D0=B0?=", "utf-8"),
        "строчка");
}

TEST(NUtilHeaders, DecodeHeaderRfc2047MultiWords) {
    EXPECT_EQ(DecodeHeaderRfc2047("=?utf-8?Q?simple=20?= =?koi8-r?Q?=D3=D4=D2=CF=DE=CB=C1?=", "utf-8"),
        "simple строчка");
    EXPECT_EQ(DecodeHeaderRfc2047("=?utf-8?Q?simple=20?= =?windows-1251?B?8fLw7vfq4A==?=", "utf-8"),
        "simple строчка");
    EXPECT_EQ(DecodeHeaderRfc2047("=?utf-8?Q?simple=20?= =?lindows-2049?B?8fLw7vfq4A==?=", "utf-8"),
        "simple строчка");
}

TEST(NUtilHeaders, ParseCorrectMessageId) {
    std::string out;
    EXPECT_TRUE(ParseMessageId("<message@domain>", out));
    EXPECT_EQ("<message@domain>", out);

    EXPECT_TRUE(ParseMessageId("   <message@domain>  ", out));
    EXPECT_EQ("<message@domain>", out);
}

TEST(NUtilHeaders, ParseIncorrectMessageId) {
    std::string out;
    EXPECT_FALSE(ParseMessageId("message@domain>", out));
    EXPECT_EQ("", out);

    EXPECT_FALSE(ParseMessageId("   <thereisnoatsign>  ", out));
    EXPECT_EQ("", out);

    EXPECT_FALSE(ParseMessageId("   <incorrectЁsymbolsЁhere@d\"omain$$>  ", out));
    EXPECT_EQ("", out);
}

const TEnvelope ENVELOPE{
    "lhlo_domain",          // Lhlo
    "127.0.0.1",            // RemoteIp
    "localhost",            // RemoteHost
    "devnull@ya.ru",        // MailFrom
    "localhost.localdomain" // Hostname
};

TEST(NUtilHeaders, MakeReceivedHeaderWithRecipient) {
    auto [name, value] = MakeReceivedHeader("sessId", ENVELOPE, "user@example.com");
    auto dateSz = MakeRfc2822Date(time(nullptr)).size();
    std::string expectedValue = "from lhlo_domain (localhost [127.0.0.1])\r\n"
        "\tby localhost.localdomain with LMTP id sessId\r\n"
        "\tfor <user@example.com>; ";

    // do not compare date part to prevent flapping
    value.resize(value.size() - dateSz);
    EXPECT_EQ(name, "Received");
    EXPECT_EQ(value, expectedValue);
}

TEST(NUtilHeaders, MakeReceivedHeaderWithoutRecipient) {
    auto [name, value] = MakeReceivedHeader("sessId", ENVELOPE, "");
    auto dateSz = MakeRfc2822Date(time(nullptr)).size();
    std::string expectedValue = "from lhlo_domain (localhost [127.0.0.1])\r\n"
        "\tby localhost.localdomain with LMTP id sessId;\r\n"
        "\t";

    // do not compare date part to prevent flapping
    value.resize(value.size() - dateSz);
    EXPECT_EQ(name, "Received");
    EXPECT_EQ(value, expectedValue);
}

TEST(NUtilHeaders, MakeReceivedHeaderWithoutValidEnvelope) {
    auto env = ENVELOPE;
    env.Lhlo.clear();
    EXPECT_THROW(MakeReceivedHeader("sessId", env, ""), yexception);

    env = ENVELOPE;
    env.RemoteHost.clear();
    EXPECT_THROW(MakeReceivedHeader("sessId", env, ""), yexception);

    env = ENVELOPE;
    env.RemoteIp.clear();
    EXPECT_THROW(MakeReceivedHeader("sessId", env, ""), yexception);
}

TEST(NUtilHeaders, MakeReceivedHeaderWithoutHostname) {
    auto env = ENVELOPE;
    env.Hostname.clear();
    EXPECT_NO_THROW(MakeReceivedHeader("sessId", env, ""));
}

TEST(NUtilHeaders, EncodeRfc2047Default) {
    EXPECT_EQ(EncodeRfc2047("Hello"), "=?utf-8?B?SGVsbG8=?=");
}

TEST(NUtilHeaders, EncodeRfc2047Codecs) {
    EXPECT_EQ(EncodeRfc2047("Привет", ECodec::QuotedPrintable), "=?utf-8?Q?=D0=9F=D1=80=D0=B8=D0=B2=D0=B5=D1=82?=");
}

TEST(NUtilHeaders, EncodeRfc2047Charset) {
    EXPECT_EQ(EncodeRfc2047("Привет", ECodec::QuotedPrintable, "cp1251"), "=?cp1251?Q?=D0=9F=D1=80=D0=B8=D0=B2=D0=B5=D1=82?=");
}

TEST(NUtilMd5, Empty) {
    std::string expected{"d41d8cd98f00b204e9800998ecf8427e"};
    EXPECT_EQ(expected, Md5HexDigest(std::string{""}));
}

TEST(NUtilMd5, NonEmpty) {
    EXPECT_EQ(
        "9e107d9d372bb6826bd81d3542a419d6",
        Md5HexDigest("The quick brown fox jumps over the lazy dog"));
    EXPECT_EQ(
        "500ab6613c6db7fbd30c62f5ff573d0f",
        Md5HexDigest("Test vector from febooti.com"));
}

TEST(NUtilArcadia, OperatorOutForTStringBuf) {
    const char* cstr = "1234567890";
    TStringBuf strbuf(cstr, 3);
    EXPECT_EQ(strbuf, "123");
    std::ostringstream os;
    os << strbuf;
    EXPECT_EQ(os.str(), "123");
}

TEST(NUtilMessage, FilenameSanitized) {
    EXPECT_EQ(FilenameSanitized("some.doc", 9), "some.doc");
    EXPECT_EQ(FilenameSanitized("длинное имя", 9), "длин");
    EXPECT_EQ(FilenameSanitized("длинное имя.docx", 9), "дл.docx");
    EXPECT_EQ(FilenameSanitized("длинное имя. Без расширения", 9), "длин");
}

TEST(NUtilMessage, CalculateAttachmentSizeBase64) {
    EXPECT_EQ(CalculateAttachmentSize("base64", "AAAA"), 3u);
    EXPECT_EQ(CalculateAttachmentSize("base64", "AAA="), 2u);
    EXPECT_EQ(CalculateAttachmentSize("base64", "AA=="), 1u);

    // malformed base64 without padding, size should be at least 3
    EXPECT_GE(CalculateAttachmentSize("base64", "AAAAA"), 3u);
}

TEST(NUtilMessage, CalculateAttachmentSizeBrokenBase64ShouldReturnsSomeReasonableSize) {
    EXPECT_LE(CalculateAttachmentSize("base64", "!\r\n=\r\n\r\n"), 9u);
}

TEST(NUtilMessage, CalculateAttachmentSizeQP) {
    EXPECT_EQ(
        CalculateAttachmentSize("quoted-printable", "It=20is=20ju=\r\nst=20a=20test"),
        17u);
}

TEST(NUtilMessage, CalculateAttachmentSizeUnknown) {
    EXPECT_EQ(CalculateAttachmentSize("", "1234567890"), 10u);
}

TEST(TestConvertOptional, for_empty_boost_optional_must_return_empty_std_optional) {
    EXPECT_FALSE(ConvertOptional(boost::optional<int>{boost::none}));
}

TEST(TestConvertOptional, for_nonempty_boost_optional_must_return_nonempty_std_optional) {
    EXPECT_EQ(std::make_optional(3), ConvertOptional(boost::make_optional(3)));
}

TEST(TestConvertOptional, for_empty_std_optional_must_return_empty_boost_optional) {
    EXPECT_FALSE(ConvertOptional(std::optional<int>{std::nullopt}));
}

TEST(TestConvertOptional, for_nonempty_std_optional_must_return_nonempty_boost_optional) {
    EXPECT_EQ(boost::make_optional(3), ConvertOptional(std::make_optional(3)));
}

TEST(NTypesTime, ToStringDuration) {
    using namespace NTimeTraits;
    using namespace std::chrono_literals;

    EXPECT_EQ(NTimeTraits::ToString(Now() - (Now() - 10ms)), "0.010");
    EXPECT_EQ(NTimeTraits::ToString(Now() - (Now() + 10ms)), "-0.010");

    EXPECT_EQ(NTimeTraits::ToString(Now() - (Now() - 10s)), "10.000");
    EXPECT_EQ(NTimeTraits::ToString(Now() - (Now() + 10s)), "-10.000");

    EXPECT_EQ(NTimeTraits::ToString(Now() - (Now() - 10100ms)), "10.100");
    EXPECT_EQ(NTimeTraits::ToString(Now() - (Now() + 10100ms)), "-10.100");
}

}
