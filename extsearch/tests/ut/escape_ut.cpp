#include <extsearch/geo/kernel/xml_writer/escape.h>

#include <library/cpp/testing/unittest/registar.h>

namespace {
    TString EscapeText(TStringBuf s) {
        TStringStream stream;
        NXmlWr::WriteXmlEscapedText(stream, s);
        return stream.Str();
    }

    TString EscapeAttr(TStringBuf s) {
        TStringStream stream;
        NXmlWr::WriteXmlEscapedAttr(stream, s);
        return stream.Str();
    }
} // namespace

Y_UNIT_TEST_SUITE(TXmlEscapeTest) {
    Y_UNIT_TEST(NoEscaping) {
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText(""), "");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText("aba caba"), "aba caba");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText(" "), " ");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText("\""), "\"");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText("'as is'"), "'as is'");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText("привет"), "привет");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText(TStringBuf("\x00\x01\x02"sv)), TStringBuf("\x00\x01\x02"sv));
    }

    Y_UNIT_TEST(AllEscaped) {
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText("<"), "&lt;");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText(">"), "&gt;");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText("&"), "&amp;");

        UNIT_ASSERT_STRINGS_EQUAL(EscapeAttr("<"), "&lt;");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeAttr(">"), "&gt;");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeAttr("&"), "&amp;");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeAttr("\""), "&quot;");

        UNIT_ASSERT_STRINGS_EQUAL(EscapeText("<&>&"), "&lt;&amp;&gt;&amp;");
    }

    Y_UNIT_TEST(EscapedParts) {
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText("&amp;"), "&amp;amp;");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeText("<![CDATA[]]]]><![CDATA[>]]>"), "&lt;![CDATA[]]]]&gt;&lt;![CDATA[&gt;]]&gt;");
        UNIT_ASSERT_STRINGS_EQUAL(EscapeAttr("It's a \"test\" & <TEST>"), "It's a &quot;test&quot; &amp; &lt;TEST&gt;");
    }
}
