
#include <gtest/gtest.h>
#include <mail_getter/mime_type.h>

TEST(MimeType, MimeType_TextHtml_returnTypeSubtypeTextHtml) {
    std::string str = "text/html";
    MimeType mime(str);
    ASSERT_EQ("text", mime.type());
    ASSERT_EQ("html", mime.subtype());
}

TEST(MimeType, MimeType_TextHtmlAndWhiteSpaces_returnTypeSubtypeTextHtml) {
    std::string str = " text/html ";
    MimeType mime(str);
    ASSERT_EQ("text", mime.type());
    ASSERT_EQ("html", mime.subtype());
}

TEST(MimeType, MimeType_TextHtmlAndCharsetDelimitedSemicolon_returnTextHtmlAndCharsetParam) {
    std::string str = "text/html; charset=utf-8";
    MimeType mime(str);
    ASSERT_EQ("text", mime.type());
    ASSERT_EQ("html", mime.subtype());
    ASSERT_EQ("utf-8", mime.param("charset"));
}

TEST(MimeType, MimeType_TextHtmlAndCharsetCompact_returnTextHtmlAndCharsetParam) {
    std::string str = "text/html;charset=utf-8";
    MimeType mime(str);
    ASSERT_EQ("text", mime.type());
    ASSERT_EQ("html", mime.subtype());
    ASSERT_EQ("utf-8", mime.param("charset"));
}

TEST(MimeType, MimeType_CharsetWithWhiteSpaces_returnCharsetParam) {
    std::string str = "text/plain; charset = koi8-r; ";
    MimeType mime(str);
    ASSERT_EQ("koi8-r", mime.param("charset"));
}

TEST(MimeType, MimeType_TextPlainAndCharsetFormatDelimitedSemicolon_returnTextHtmlAndCharsetFormatParams) {
    std::string str = "text/plain; charset=koi8-r; format=flowed";
    MimeType mime(str);
    ASSERT_EQ("text", mime.type());
    ASSERT_EQ("plain", mime.subtype());
    ASSERT_EQ("koi8-r", mime.param("charset"));
    ASSERT_EQ("flowed", mime.param("format"));
}


TEST(MimeType, MimeType_CharsetFormatCompact_returnCharsetFormatParams) {
    std::string str = "text/plain;charset=koi8-r;format=flowed;";
    MimeType mime(str);
    ASSERT_EQ("koi8-r", mime.param("charset"));
    ASSERT_EQ("flowed", mime.param("format"));
}


TEST(MimeType, MimeType_CharsetFormatLoose_returnAndCharsetFormatParams) {
    std::string str = " text/plain ;  charset =  koi8-r \n; \r\n format  =  flowed\n;  ";
    MimeType mime(str);
    ASSERT_EQ("koi8-r", mime.param("charset"));
    ASSERT_EQ("flowed", mime.param("format"));
}

TEST(MimeType, MimeType_ErrorParam_ignoreError) {
    std::string str = "text/plain; =utf-8; format=flowed";
    MimeType mime(str);
    ASSERT_EQ("text", mime.type());
    ASSERT_EQ("plain", mime.subtype());
    ASSERT_EQ("flowed", mime.param("format"));
}
