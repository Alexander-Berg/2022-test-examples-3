#include <extsearch/geo/kernel/xml_writer/xml.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NXmlWr;

namespace {
    struct TI18nString {
        TString Language;
        TString String;
    };

    TDocument CreateDocument(TStringBuf root) {
        TDocument doc{root};
        doc.Root().SetAttr("xmlns", "http://example.com/1.x");
        return doc;
    }
} // namespace

namespace NXmlWr {
    template <>
    struct TAddCustomTextTraits<TI18nString> {
        static TNode Add(TNode& node, const TString& name, const TI18nString& value) {
            TNode child = node.AddText(name, value.String);
            if (!value.Language.empty()) {
                child.SetAttr("lang", value.Language);
            }
            return child;
        }
    };
} // namespace NXmlWr

Y_UNIT_TEST_SUITE(TXmlWriterTest) {
    Y_UNIT_TEST(TestBasic) {
        NXmlWr::TDocument doc{"ymaps"};
        doc.Root().SetAttr("xmlns", "http://maps.yandex.ru/ymaps/1.x");
        doc.Root().AddText("hello", "world");
        doc.Root().AddOptionalText("empty", "");

        UNIT_ASSERT_STRINGS_EQUAL(doc.AsString(),
                                  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                  "<ymaps xmlns=\"http://maps.yandex.ru/ymaps/1.x\">"
                                  "<hello>world</hello>"
                                  "</ymaps>\n");

        UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(),
                                  "<ymaps xmlns=\"http://maps.yandex.ru/ymaps/1.x\">"
                                  "<hello>world</hello></ymaps>");
    }

    Y_UNIT_TEST(TestCustomItem) {
        NXmlWr::TDocument doc{"Names"};
        doc.Root().AddCustomText("name", TI18nString{"en", "hello"});
        doc.Root().AddCustomText("name", TI18nString{"", "fake"});
        doc.Root().AddCustomText("name", TI18nString{"ru", "привет"});

        UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(),
                                  "<Names>"
                                  "<name lang=\"en\">hello</name>"
                                  "<name>fake</name>"
                                  "<name lang=\"ru\">привет</name>"
                                  "</Names>");
    }

    Y_UNIT_TEST(TestEscaping) {
        NXmlWr::TDocument doc{"test"};
        doc.Root().AddText("message", "<>'\"&\t\x01\x02\x03\xFF\xC2"
                                      "abacaba");
        doc.Root().SetAttr("attr", "\"&amp;><'");
        UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(),
                                  "<test attr=\"&quot;&amp;amp;&gt;&lt;'\">"
                                  "<message>&lt;&gt;'\"&amp;\t\x01\x02\x03\xFF\xC2"
                                  "abacaba</message>"
                                  "</test>");
    }

    Y_UNIT_TEST(TestAttrs) {
        NXmlWr::TDocument doc{"test"};
        auto root = doc.Root();
        root.SetAttr("first", 1);
        root.SetAttr("second", "");
        root.SetOptionalAttr("third", "");
        root.SetAttr("fourth", "4");

        UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(),
                                  "<test first=\"1\" second=\"\" fourth=\"4\"/>");
    }

    Y_UNIT_TEST(TestOutOfOrderBuild) {
        NXmlWr::TDocument doc{"test"};
        auto root = doc.Root();
        auto first = root.AddElement("first");
        auto second = root.AddElement("second");
        second.AddText("aba", "caba");
        auto sub = first.AddElement("sub");
        first.AddText("foo", "bar");
        sub.SetAttr("haha", "DA");

        UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(),
                                  "<test>"
                                  "<first><sub haha=\"DA\"/><foo>bar</foo></first>"
                                  "<second><aba>caba</aba></second>"
                                  "</test>");
    }

    Y_UNIT_TEST(TestRemove) {
        {
            NXmlWr::TDocument doc{"test"};
            auto root = doc.Root();
            auto a1 = root.AddElement("a1");
            auto a2 = root.AddElement("a2");
            auto a3 = root.AddElement("a3");

            UNIT_ASSERT_EXCEPTION(root.Remove(), yexception);
            UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<test><a1/><a2/><a3/></test>");
            a1.Remove();
            UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<test><a2/><a3/></test>");
            a3.Remove();
            UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<test><a2/></test>");
            UNIT_ASSERT_EXCEPTION(root.Remove(), yexception);
            a2.Remove();
            UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<test/>");
        }
        {
            NXmlWr::TDocument doc{"test"};
            auto root = doc.Root();
            auto a1 = root.AddElement("a1");
            auto a2 = root.AddElement("a2");
            auto a3 = root.AddElement("a3");

            a3.RemoveIfEmpty();
            UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<test><a1/><a2/></test>");
            a2.RemoveIfEmpty();
            UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<test><a1/></test>");
            a1.RemoveIfEmpty();
            UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<test/>");
        }
        {
            NXmlWr::TDocument doc{"test"};
            auto root = doc.Root();
            auto a1 = root.AddElement("a1");
            auto b1 = a1.AddElement("b1");
            b1.AddElement("c1");
            root.AddElement("a2");
            a1.AddElement("b2");

            UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<test><a1><b1><c1/></b1><b2/></a1><a2/></test>");
            a1.RemoveIfEmpty();
            b1.RemoveIfEmpty();
            UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<test><a1><b1><c1/></b1><b2/></a1><a2/></test>");
            b1.Remove();
            UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<test><a1><b2/></a1><a2/></test>");
        }
    }

    Y_UNIT_TEST(TestReturningFromFunc) {
        auto doc = CreateDocument("ymaps");
        UNIT_ASSERT_STRINGS_EQUAL(doc.Root().AsString(), "<ymaps xmlns=\"http://example.com/1.x\"/>");
    }
}
