#define BOOST_TEST_MODULE rfc2047_test
#include <tests_common.h>

#include <mimeparser/rfc2047.h>

BOOST_AUTO_TEST_SUITE(rfc2047_test)

BOOST_AUTO_TEST_CASE(encoded_word_with_invalid_end)
{
    const std::string encoded = "\"Карлова Юлия А=?D0=BB=D0=B5=D0=BA=D1=81=D0=B0=D0=BD=D0=B4=D1=80=D0=BE=D0=B2=D0=BD=D0=B0\"=20?";
    std::string charset;
    BOOST_REQUIRE(mulca_mime::decode_rfc2047(encoded, charset) == encoded);
    BOOST_REQUIRE(charset.empty());
}

BOOST_AUTO_TEST_CASE(utf8_q_encoded_word)
{
    const std::string encoded = "=?UTF-8?Q?\"=D0=98=D0=B2=D0=B0=D0=BD=D0=BE=D0=B2=D0=B0 =D0=9D=D0=B0=D1=82=D0=B0=D0=BB=D0=B8=D1=8F\"=20?=<some@ya.ru>";
    std::string charset;
    BOOST_REQUIRE(mulca_mime::decode_rfc2047(encoded, charset) == "\"Иванова Наталия\" <some@ya.ru>");
    BOOST_REQUIRE(charset == "UTF-8");
}

BOOST_AUTO_TEST_CASE(utf8_b_encoded_word)
{
    const std::string encoded = "=?utf-8?B?eWFCYnMgKNGA0LDRgdGB0YvQu9C60LAp?= <bbs@yandex-team.ru>";
    std::string charset;
    BOOST_REQUIRE(mulca_mime::decode_rfc2047(encoded, charset) == "yaBbs (рассылка) <bbs@yandex-team.ru>");
    BOOST_REQUIRE(charset == "utf-8");
}

BOOST_AUTO_TEST_SUITE_END()
