#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <butil/http/url.h>

namespace {
using namespace testing;

TEST(UrlTest, urlWithoutPort_setupPort_80) {
    http::url url("simple.com");
    ASSERT_EQ( url.port(), 80 );
}

TEST(UrlTest, httpUrlWithoutPort_setupPort_80) {
    http::url url("http://simple.com");
    ASSERT_EQ( url.port(), 80 );
}

TEST(UrlTest, httpsUrlWithoutPort_setupPort_443) {
    http::url url("https://simple.com");
    ASSERT_EQ( url.port(), 443 );
}

TEST(UrlTest, ftpUrlWithoutPort_setupPort_21) {
    http::url url("ftp://simple.com");
    ASSERT_EQ( url.port(), 21 );
}

TEST(UrlTest, httpUrlWithoutPort_protocol_returnsHttp) {
    http::url url("http://simple.com");
    ASSERT_EQ( url.protocol(), "http" );
}

TEST(UrlTest, httpsUrlWithoutPort_protocol_returnsHttps) {
    http::url url("https://simple.com");
    ASSERT_EQ( url.protocol(), "https" );
}

TEST(UrlTest, ftpUrlWithoutPort_protocol_returnsFtp) {
    http::url url("ftp://simple.com");
    ASSERT_EQ( url.protocol(), "ftp" );
}

TEST(UrlTest, unknownUrlWithoutPort_protocol_returnsEmpty) {
    http::url url("simple.com");
    ASSERT_EQ( url.protocol(), "" );
}

TEST(UrlTest, urlWithoutUri_setupUri_inSlash) {
    http::url url("http://simple.com");
    ASSERT_EQ( url.uri(), "/" );
}

TEST(UrlTest, correctUrl_setupServer_right) {
    http::url url("http://simple.com");
    ASSERT_EQ( url.server(), "simple.com" );
}

TEST(UrlTest, correctUrl_setupLogin_right) {
    http::url url("http://login:password@simple.com");
    ASSERT_EQ( url.login(), "login" );
}

TEST(UrlTest, correctUrl_setupPassword_right) {
    http::url url("http://login:password@simple.com");
    ASSERT_EQ( url.password(), "password" );
}

TEST(UrlTest, urlWithPortSpecified_setupPort_right) {
    http::url url("http://server.com:666/uri");
    ASSERT_EQ( url.port(), 666 );
}

TEST(UrlTest, urlWithPortSpecified_portSpecified_returnsTrue) {
    http::url url("http://server.com:666/uri");
    ASSERT_TRUE( url.portSpecified() );
}

TEST(UrlTest, urlWithNoPortSpecified_portSpecified_returnsTrue) {
    http::url url("http://server.com/uri");
    ASSERT_FALSE( url.portSpecified() );
}

TEST(UrlTest, urlWithSlashUriSeparator_setupUri_right) {
    http::url url("http://server.com:666/uri");
    ASSERT_EQ( url.uri(), "/uri" );
}

TEST(UrlTest, urlWithSharpUriSeparator_setupUri_right) {
    http::url url("http://server.com:666#uri");
    ASSERT_EQ( url.uri(), "#uri" );
}

TEST(UrlTest, urlWithQuestionMarkUriSeparator_setupUri_right) {
    http::url url("http://quentao.yandex-team.ru:9999?db=base-21#problems/3841");
    ASSERT_EQ( url.uri(), "?db=base-21#problems/3841" );
}

TEST(UrlTest, urlWithLoginAndEmptyPassword_setupPassword_empty) {
    http::url url("ftp://qwerty@ya.ru:80");
    ASSERT_TRUE( url.password().empty() );
}

TEST(UrlTest, WMI_749_urlWithDogAtInParameters_setupServer_correct) {
    http::url url("http://gotable.ru/reset_pass.php?submit=reset&email=il@gotable.ru"
            "&pass=c4ca4238a0b923820dcc509a6f75849b");
    ASSERT_EQ( url.server(), "gotable.ru" );
}

TEST(UrlTest, WMI_749_urlWithDogAtInParameters_setupUri_correct) {
    http::url url("http://gotable.ru/reset_pass.php?submit=reset&email=il@gotable.ru"
            "&pass=c4ca4238a0b923820dcc509a6f75849b");
    ASSERT_EQ( url.uri(), "/reset_pass.php?submit=reset&email=il@gotable.ru"
            "&pass=c4ca4238a0b923820dcc509a6f75849b" );
}

TEST(UrlTest, extract_urlPart_WithoutArguments) {
    http::url url("http://simple.com");
    ASSERT_EQ( url.urlPart(), "http://simple.com" );
}

TEST(UrlTest, extract_urlPart_WithoutArgumentsWithMethod) {
    http::url url("http://server.com:666/uri");
    ASSERT_EQ( url.urlPart(), "http://server.com:666/uri" );
}

TEST(UrlTest, extract_urlPart_WithoutArgumentsWithFragment) {
    http::url url("http://server.com:666#uri");
    ASSERT_EQ( url.urlPart(), "http://server.com:666" );
}

TEST(UrlTest, extract_urlPart_WithArguments) {
    http::url url("http://quentao.yandex-team.ru:9999?db=base-21#problems/3841");
    ASSERT_EQ( url.urlPart(), "http://quentao.yandex-team.ru:9999" );
}

TEST(UrlTest, WMI_749_urlWithDogAtInParameters_setupUrlPart_correct) {
    http::url url("http://gotable.ru/reset_pass.php?submit=reset&email=il@gotable.ru"
            "&pass=c4ca4238a0b923820dcc509a6f75849b");
    ASSERT_EQ( url.urlPart(), "http://gotable.ru/reset_pass.php" );
}

}
