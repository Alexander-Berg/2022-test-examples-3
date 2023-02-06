#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/vdirect/secure_vdirect.h>

namespace {
using namespace testing;

typedef SecureVdirect::Params Params;

struct HashProviderMock : public vdirect::HashProvider {
    MOCK_METHOD(std::string, hash, (const std::string & url) , (const, override));
};

struct VdirectBaseMock : public SecureVdirect
{
    VdirectBaseMock(const Params & params, vdirect::HashProviderPtr hashProvider)
    : SecureVdirect(params, std::move(hashProvider)) {}
    MOCK_METHOD(std::string, process, (const std::string & url) , (const, override));
    MOCK_METHOD(std::string, unwrapUrl, (const std::string & url) , (const, override));
    MOCK_METHOD(std::string, getUnwrapPattern, () , (const, override));
};

struct AuthVdirectTest : public Test {
    enum { http, https };

    AuthVdirectTest() :
        provider(new HashProviderMock),
        vdirect(Params(http, "yandex.ru", "r.jsx"),  vdirect::HashProviderPtr(provider)){
        storage.addKey("a", "abrakadabra", true);
    }

    vdirect::KeysStorage storage;
    HashProviderMock * provider;
    SecureVdirect vdirect;
};

TEST_F( AuthVdirectTest, processA_withHttpUrl_returnsSubstitutedUrl ) {
    VdirectBaseMock vdirect(Params(http, "", ""),
            vdirect::HashProviderPtr(new HashProviderMock));
    const std::string src = "<a href=\"https://some.url.com?param=1&bar=foo\">blah</a>";
    EXPECT_CALL( vdirect, process("https://some.url.com?param=1&bar=foo") ).WillOnce(Return("ZZZZ"));
    ASSERT_EQ(vdirect.processA(src), "<a href=\"ZZZZ\">blah</a>");
}

TEST_F( AuthVdirectTest, unwrap_withSeveralWrappedHttpLink_returnsHttpLink ) {
    VdirectBaseMock vdirect(Params(http, "", ""),
                vdirect::HashProviderPtr(new HashProviderMock));
    std::string url = "<a href=\"https://vdirect.com/url/D2TDnP1eDEA_-mJyG7F6QQ,777\">blah</a>"
            "Some text<a href=\"https://vdirect.com/url/D2TDnP1eDEA_-mJyG7F6QQ,777\">blah2</a>";
    EXPECT_CALL( vdirect, getUnwrapPattern() ).WillOnce(Return("https://vdirect.com/url"));
    EXPECT_CALL( vdirect, unwrapUrl("/D2TDnP1eDEA_-mJyG7F6QQ,777") ).Times(2)
            .WillRepeatedly(Return("http://booo.com"));
    vdirect.unwrap(url);
    ASSERT_EQ(url, "<a href=\"http://booo.com\">blah</a>"
            "Some text<a href=\"http://booo.com\">blah2</a>");
}


TEST_F( AuthVdirectTest, process_withUrl_provideEncodedLink ) {
    EXPECT_CALL( *provider, hash("aHR0cDovL3NvbWUudXJsLmNvbT9wYXJhbT0xJmJhcj1mb28"
                                ) ).WillOnce(Return(""));
    vdirect.process("http://some.url.com?param=1&bar=foo");
}

TEST_F( AuthVdirectTest, httpConfiguredProcess_withUrl_returnsHttpWrappedUrl ) {
    EXPECT_CALL( *provider, hash(_) ).WillOnce(Return("a,GOODHASH"));
    ASSERT_EQ(vdirect.process("http://some.url.com?param=1&bar=foo"),
            "http://mail.yandex.ru/r.jsx?h=a,GOODHASH&amp;l="
            "aHR0cDovL3NvbWUudXJsLmNvbT9wYXJhbT0xJmJhcj1mb28");
}

TEST_F( AuthVdirectTest, httpsConfiguredProcess_withHttpUrl_returnsHttpsWrappedUrl ) {
    std::unique_ptr<HashProviderMock> provider(new HashProviderMock);
    EXPECT_CALL( *provider, hash(_) ).WillOnce(Return("a,GOODHASH"));

    SecureVdirect vdirectHttps(Params(https, "yandex.ru", "r.jsx"),
            vdirect::HashProviderPtr(provider.release()));
    ASSERT_EQ(vdirectHttps.process("http://some.url.com"),
            "https://mail.yandex.ru/r.jsx?h=a,GOODHASH&amp;l="
            "aHR0cDovL3NvbWUudXJsLmNvbS8");
}

TEST_F( AuthVdirectTest, process_withUnknownUrl_returnsSpecifiedUrl ) {
    ASSERT_EQ(vdirect.process("some.url.com"), "some.url.com");
}

TEST_F( AuthVdirectTest, process_withYandexRedirectUrl_returnsSpecifiedUrl ) {
    const std::string& redirectedUrl = "http://mail.yandex.ru/r?url=http%3A%2F%2Fany.url";
    ASSERT_EQ(vdirect.process(redirectedUrl), redirectedUrl);
}

TEST_F( AuthVdirectTest, process_withCirllycDomainHttpUrl_providePunycodedUrl ) {
    EXPECT_CALL( *provider, hash(
            "aHR0cDovL3huLS0tLTdzYjFhY2J5YzVhLnhuLS1rMWFqaS54bi0tcDFhaT9wYXJhbT0xJmJhcj1mb28")
            ).WillOnce(Return(""));
    vdirect.process("http://какой-то.урл.рф?param=1&bar=foo");
}

TEST_F( AuthVdirectTest, process_withEntitiesInUri_provideSameEncodedAsWithGlyph ) {
    EXPECT_CALL( *provider, hash(
            "aHR0cDovL3huLS0tLTdzYjFhY2J5YzVhLnhuLS1rMWFqaS54bi0tcDFhaT9wYXJhbT0xJmJhcj1mb28")
            ).WillOnce(Return(""));
    vdirect.process("http://какой-то.урл.рф?param=1&amp;bar=foo");
}

TEST_F( AuthVdirectTest, unwrapUrl_withWrappedUrl_returnsUnwrappedUrl ) {
    const std::string url = "?h=a,hOtNB9GrbkUWlstPeyiXqA&amp;"
    "l=aHR0cDovL3NvbWUudXJsLmNvbS8";
    ASSERT_EQ(vdirect.unwrapUrl(url), "http://some.url.com/");
}

}
