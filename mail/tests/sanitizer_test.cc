#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/core/include/configuration.h>
#include <mail/sendbernar/composer/include/sanitizer.h>
#include <mail/sendbernar/composer/tests/mock/mail_getter.h>
#include <mail/http_getter/client/mock/mock.h>

namespace sendbernar::tests {

using namespace testing;
using namespace http_getter;

struct SanitizerTest: public ::testing::TestWithParam<
        std::tuple<
              std::string
            , mail_getter::SanitizerMarkupEntry>
        > {

    std::string uid() const {
        return "uid";
    }

    std::string requestId() const {
        return "requestId";
    }

    std::string host() const {
        return "host";
    }

    SanitizerConfiguration sanitizerConfig;
};

INSTANTIATE_TEST_SUITE_P(shouldFindElementsWithDifferentClasses, SanitizerTest, ::testing::Values(
      std::make_tuple("\r\n"
                      "--FOO\r\nContent-Disposition: form-data; name=\"markup.json\"\r\n"
                      "Content-Type: application/json; charset=utf-8\r\n"
                      "Content-Transfer-Encoding: 8bit\r\n"
                      "\r\n"
                      "[{\"type\":1,\"position\":[5,22,10,16]}]\r\n"
                      "--FOO\r\n"
                      "Content-Disposition: form-data; name=\"sanitized.html\"\r\n"
                      "Content-Type: text/html; charset=utf-8\r\n"
                      "Content-Transfer-Encoding: binary\r\n"
                      "\r\n"
                      "<img src=\"http://localhost\" /><h1>h1</h1>\r\n"
                      "--FOO--\r\n",
                      mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Image,
                                                        boost::none, mail_getter::MarkupPosition{5, 22, 10, 16}})
    , std::make_tuple("\r\n"
                      "--FOO\r\n"
                      "Content-Disposition: form-data; name=\"markup.json\"\r\n"
                      "Content-Type: application/json; charset=utf-8\r\n"
                      "Content-Transfer-Encoding: 8bit\r\n"
                      "\r\n"
                      "[{\"type\":3,\"position\":[3,29,9,22]}]\r\n"
                      "--FOO\r\n"
                      "Content-Disposition: form-data; name=\"sanitized.html\"\r\n"
                      "Content-Type: text/html; charset=utf-8\r\n"
                      "Content-Transfer-Encoding: binary\r\n"
                      "\r\n"
                      "<a href=\"https://mail.yandex.ru\">mail.yandex.ru</a>\r\n"
                      "--FOO--\r\n",
                      mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Link,
                                                        boost::none, mail_getter::MarkupPosition{3, 29, 9, 22}})
    , std::make_tuple("\r\n"
                      "--FOO\r\n"
                      "Content-Disposition: form-data; name=\"markup.json\"\r\n"
                      "Content-Type: application/json; charset=utf-8\r\n"
                      "Content-Transfer-Encoding: 8bit\r\n"
                      "\r\n"
                      "[{\"type\":2,\"position\":[5,18,10,12]}]\r\n"
                      "--FOO\r\n"
                      "Content-Disposition: form-data; name=\"sanitized.html\"\r\n"
                      "Content-Type: text/html; charset=utf-8\r\n"
                      "Content-Transfer-Encoding: binary\r\n"
                      "\r\n"
                      "<img src=\"cid:test_cid\" />\r\n"
                      "--FOO--\r\n",
                      mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Cid,
                                                        boost::none, mail_getter::MarkupPosition{5, 18, 10, 12}})
    , std::make_tuple("\r\n"
                      "--FOO\r\n"
                      "Content-Disposition: form-data; name=\"markup.json\"\r\n"
                      "Content-Type: application/json; charset=utf-8\r\n"
                      "Content-Transfer-Encoding: 8bit\r\n"
                      "\r\n"
                      "[{\"type\":4,\"classValue\":\"test_class\",\"position\":[5,67,10,61]}]\r\n"
                      "--FOO\r\n"
                      "Content-Disposition: form-data; name=\"sanitized.html\"\r\n"
                      "Content-Type: text/html; charset=utf-8\r\n"
                      "Content-Transfer-Encoding: binary\r\n"
                      "\r\n"
                      "<img src=\"https://static.yandex-team.ru/pic.jpg?yandex_class=test_class\" />\r\n"
                      "--FOO--\r\n",
                      mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                                        boost::make_optional<std::string>("test_class"),
                                                        mail_getter::MarkupPosition{5, 67, 10, 61}})
));

TEST_P(SanitizerTest, shouldFindElementsWithDifferentClasses) {
    const auto [resp, element] = GetParam();

    const auto http = createTypedDummy(yhttp::response{
        .status=200,
        .headers=ymod_httpclient::headers_dict{
            {"content-type", "multipart/mixed; boundary=FOO"}
        },
        .body=resp,
    });

    mail_getter::SanitizerParsedResponse r = *makeSanitizer(uid(), requestId(), sanitizerConfig, http)->sanitize("", "");

    EXPECT_THAT(r.markup, UnorderedElementsAre(element));
}

TEST_F(SanitizerTest, shouldNotFindElementsInCaseOfEmptyHtml) {
    std::string resp = "\r\n"
                       "--FOO\r\n"
                       "Content-Disposition: form-data; name=\"markup.json\"\r\n"
                       "Content-Type: application/json; charset=utf-8\r\n"
                       "Content-Transfer-Encoding: 8bit\r\n"
                       "\r\n"
                       "null\r\n"
                       "--FOO\r\n"
                       "Content-Disposition: form-data; name=\"sanitized.html\"\r\n"
                       "Content-Type: text/html; charset=utf-8\r\n"
                       "Content-Transfer-Encoding: binary\r\n"
                       "\r\n"
                       "asdf\r\n"
                       "--FOO--\r\n";

    const auto http = createTypedDummy(yhttp::response{
        .status=200,
        .headers=ymod_httpclient::headers_dict{
            {"content-type", "multipart/mixed; boundary=FOO"}
        },
        .body=resp,
    });

    mail_getter::SanitizerParsedResponse r = *makeSanitizer(uid(), requestId(), sanitizerConfig, http)->sanitize("", "");

    EXPECT_TRUE(r.markup.empty());
}

TEST_F(SanitizerTest, shouldReturnEmptyResponseOnNon200SanitizerResponse) {
    {
        const auto http = createTypedDummy(yhttp::response{.status=500});
        EXPECT_EQ(makeSanitizer(uid(), requestId(), sanitizerConfig, http)->sanitize("", ""), std::nullopt);
    }

    {
        const auto http = createTypedDummy(yhttp::response{.status=400});
        EXPECT_EQ(makeSanitizer(uid(), requestId(), sanitizerConfig, http)->sanitize("", ""), std::nullopt);
    }
}

}
