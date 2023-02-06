#include <rendering/replace_params.h>
#include <catch.hpp>

namespace fan::rendering {

map<string, string> PARAMS = { { "recipient", "yapoptest02@yandex.ru" }, { "secret", "123" } };

string TEMPLATE =
    "To: %recipient%\r\nFrom: "
    "yapoptest@yandex.ru\r\n\r\nHello!\r\nhttps://sender.yandex.ru/unsubscribe/%secret%";
string EML = "To: yapoptest02@yandex.ru\r\nFrom: "
             "yapoptest@yandex.ru\r\n\r\nHello!\r\nhttps://sender.yandex.ru/unsubscribe/123";

string TEMPLATE_BASE64 =
    "Content-Transfer-Encoding: base64\r\nTo: %recipient%\r\nFrom: "
    "yapoptest@yandex."
    "ru\r\n\r\nSGVsbG8hDQpodHRwczovL3NlbmRlci55YW5kZXgucnUvdW5zdWJzY3JpYmUvJXNlY3JldCU=";
string TEMPLATE_CORRUPTED_BASE64 = "Content-Transfer-Encoding: base64\r\nTo: %recipient%\r\nFrom: "
                                   "yapoptest@yandex."
                                   "ru\r\n\r\n######################=";
string EML_BASE64 =
    "Content-Transfer-Encoding: base64\r\nTo: yapoptest02@yandex.ru\r\nFrom: "
    "yapoptest@yandex.ru\r\n\r\nSGVsbG8hDQpodHRwczovL3NlbmRlci55YW5kZXgucnUvdW5zdWJzY3JpYmUvMTIz";

string TEMPLATE_MULTIPART_BASE64 =
    "Content-Type: multipart/mixed; boundary=\"xxx\"\r\nTo: "
    "%recipient%\r\n\r\n--xxx\r\nContent-Type: "
    "text/plain\r\n\r\nHello!\r\nhttps://sender.yandex.ru/unsubscribe/"
    "%secret%\r\n--xxx\r\nnContent-Type: "
    "text/plain\r\nContent-Transfer-Encoding: "
    "base64\r\n\r\nSGVsbG8hDQpodHRwczovL3NlbmRlci55YW5kZXgucnUvdW5zdWJzY3JpYmUvJXNlY3JldCU=\r\n--"
    "xxx--";
string EML_MULTIPART_BASE64 =
    "Content-Type: multipart/mixed; boundary=\"xxx\"\r\nTo: "
    "yapoptest02@yandex.ru\r\n\r\n--xxx\r\nContent-Type: "
    "text/plain\r\n\r\nHello!\r\nhttps://sender.yandex.ru/unsubscribe/"
    "123\r\n--xxx\r\nnContent-Type: "
    "text/plain\r\nContent-Transfer-Encoding: "
    "base64\r\n\r\nSGVsbG8hDQpodHRwczovL3NlbmRlci55YW5kZXgucnUvdW5zdWJzY3JpYmUvMTIz\r\n--xxx--";
string EML_MULTIPART_BASE64_WITH_NOT_REPLACED_SECRET =
    "Content-Type: multipart/mixed; boundary=\"xxx\"\r\nTo: "
    "yapoptest02@yandex.ru\r\n\r\n--xxx\r\nContent-Type: "
    "text/plain\r\n\r\nHello!\r\nhttps://sender.yandex.ru/unsubscribe/"
    "%secret%\r\n--xxx\r\nnContent-Type: "
    "text/plain\r\nContent-Transfer-Encoding: "
    "base64\r\n\r\nSGVsbG8hDQpodHRwczovL3NlbmRlci55YW5kZXgucnUvdW5zdWJzY3JpYmUvJXNlY3JldCU=\r\n--"
    "xxx--";

string REAL_TEMPLATE =
    "Content-Type: multipart/mixed; boundary=\"===============8031628399123967481==\"\r\n"
    "MIME-Version: 1.0\r\n"
    "Date: Fri, 12 Mar 2021 15:39:08 +0300\r\n"
    "Message-ID: <20210312153908.541335.35051.2571.21@kharybin-dev.sas.yp-c.yandex.net>\r\n"
    "List-ID: <sendr-2-dev.yandex.ru>\r\n"
    "Precedence: bulk\r\n"
    "X-Mailru-Msgtype: <sendr-2-dev>\r\n"
    "X-Auto-Response-Suppress: All\r\n"
    "X-Yandex-Hint: bGFiZWw9U3lzdE1ldGthU086dHJ1c3RfNQo=\r\n"
    "Reply-To: yapoptest@yandex.ru\r\n"
    "X-Mailer: YandexSender/0.1\r\n"
    "Subject: test2\r\n"
    "From: yapoptest <yapoptest@yandex-team.ru>\r\n"
    "To: %recipient%\r\n"
    "List-Unsubscribe: https://sender.yandex.ru/unsubscribe/%secret%\r\n"
    "X-Sendr-Id: %secret%\r\n"
    "\r\n"
    "This is a multi-part message in MIME format.\r\n"
    "\r\n"
    "--===============8031628399123967481==\r\n"
    "Content-Type: multipart/related;\r\n"
    " boundary=\"===============5523104519510471048==\"\r\n"
    "MIME-Version: 1.0\r\n"
    "\r\n"
    "--===============5523104519510471048==\r\n"
    "Content-Type: multipart/alternative;\r\n"
    " boundary=\"===============3015547874013356061==\"\r\n"
    "MIME-Version: 1.0\r\n"
    "\r\n"
    "--===============3015547874013356061==\r\n"
    "Content-Type: text/html; charset=\"utf-8\"\r\n"
    "MIME-Version: 1.0\r\n"
    "Content-Transfer-Encoding: base64\r\n"
    "\r\n"
    "PGh0bWw+PGhlYWQ+PC9oZWFkPjxib2R5PtCf0YDQuNCy0LXRgiwgITxicj48YnI+0JrQsNC6INC0\r\n"
    "0LXQu9CwPzxicj48YnI+0JLQvtGCINGB0YHRi9C70LrQsCDQvdCwINC+0YLQv9C40YHQutGDOjxi\r\n"
    "cj5odHRwczovL3NlbmRlci55YW5kZXgucnUvdW5zdWJzY3JpYmUvJXNlY3JldCU8YnI+\r\n"
    "\r\n"
    "--===============3015547874013356061==--\r\n"
    "\r\n"
    "--===============5523104519510471048==--\r\n"
    "\r\n"
    "--===============8031628399123967481==--\r\n";
string REAL_EML =
    "Content-Type: multipart/mixed; boundary=\"===============8031628399123967481==\"\r\n"
    "MIME-Version: 1.0\r\n"
    "Date: Fri, 12 Mar 2021 15:39:08 +0300\r\n"
    "Message-ID: <20210312153908.541335.35051.2571.21@kharybin-dev.sas.yp-c.yandex.net>\r\n"
    "List-ID: <sendr-2-dev.yandex.ru>\r\n"
    "Precedence: bulk\r\n"
    "X-Mailru-Msgtype: <sendr-2-dev>\r\n"
    "X-Auto-Response-Suppress: All\r\n"
    "X-Yandex-Hint: bGFiZWw9U3lzdE1ldGthU086dHJ1c3RfNQo=\r\n"
    "Reply-To: yapoptest@yandex.ru\r\n"
    "X-Mailer: YandexSender/0.1\r\n"
    "Subject: test2\r\n"
    "From: yapoptest <yapoptest@yandex-team.ru>\r\n"
    "To: yapoptest02@yandex.ru\r\n"
    "List-Unsubscribe: https://sender.yandex.ru/unsubscribe/123\r\n"
    "X-Sendr-Id: 123\r\n"
    "\r\n"
    "This is a multi-part message in MIME format.\r\n"
    "\r\n"
    "--===============8031628399123967481==\r\n"
    "Content-Type: multipart/related;\r\n"
    " boundary=\"===============5523104519510471048==\"\r\n"
    "MIME-Version: 1.0\r\n"
    "\r\n"
    "--===============5523104519510471048==\r\n"
    "Content-Type: multipart/alternative;\r\n"
    " boundary=\"===============3015547874013356061==\"\r\n"
    "MIME-Version: 1.0\r\n"
    "\r\n"
    "--===============3015547874013356061==\r\n"
    "Content-Type: text/html; charset=\"utf-8\"\r\n"
    "MIME-Version: 1.0\r\n"
    "Content-Transfer-Encoding: base64\r\n"
    "\r\n"
    "PGh0bWw+PGhlYWQ+PC9oZWFkPjxib2R5PtCf0YDQuNCy0LXRgiwgITxicj48YnI+0JrQsNC6INC0\r\n"
    "0LXQu9CwPzxicj48YnI+0JLQvtGCINGB0YHRi9C70LrQsCDQvdCwINC+0YLQv9C40YHQutGDOjxi\r\n"
    "cj5odHRwczovL3NlbmRlci55YW5kZXgucnUvdW5zdWJzY3JpYmUvMTIzPGJyPg==\r\n"
    "\r\n"
    "--===============3015547874013356061==--\r\n"
    "\r\n"
    "--===============5523104519510471048==--\r\n"
    "\r\n"
    "--===============8031628399123967481==--\r\n";

TEST_CASE("replace_params/empty_params")
{
    REQUIRE(*replace_params(TEMPLATE, {}) == TEMPLATE);
}

TEST_CASE("replace_params/replace_in_text")
{
    REQUIRE(*replace_params(TEMPLATE, PARAMS) == EML);
}

TEST_CASE("replace_params/corrupted_base64")
{
    REQUIRE_THROWS(*replace_params(TEMPLATE_CORRUPTED_BASE64, {}));
}

TEST_CASE("replace_params/replace_in_base64")
{
    REQUIRE(*replace_params(TEMPLATE_BASE64, PARAMS) == EML_BASE64);
}

TEST_CASE("replace_params/replace_in_multipart")
{
    REQUIRE(*replace_params(TEMPLATE_MULTIPART_BASE64, PARAMS) == EML_MULTIPART_BASE64);
}

TEST_CASE("replace_params/missing_param")
{
    map<string, string> params_copy = PARAMS;
    params_copy.erase("secret");
    REQUIRE(
        *replace_params(TEMPLATE_MULTIPART_BASE64, params_copy) ==
        EML_MULTIPART_BASE64_WITH_NOT_REPLACED_SECRET);
}

TEST_CASE("replace_params/extra_param")
{
    map<string, string> params_copy = PARAMS;
    params_copy.emplace("extra_param", "extra_value");
    REQUIRE(*replace_params(TEMPLATE_MULTIPART_BASE64, params_copy) == EML_MULTIPART_BASE64);
}

TEST_CASE("replace_params/real_eml")
{
    REQUIRE(*replace_params(REAL_TEMPLATE, PARAMS) == REAL_EML);
}

}
