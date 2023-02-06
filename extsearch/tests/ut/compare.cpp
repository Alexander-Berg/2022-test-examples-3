#include <extsearch/geo/kernel/xml_writer/tests/lib/example.h>
#include <extsearch/geo/kernel/xml_writer/wrapper/libxml.h>
#include <extsearch/geo/kernel/xml_writer/xml.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(CompareToLibXmlTest) {
    Y_UNIT_TEST(NodeFormattingOff) {
        TString libxmlResult;
        {
            NXml::TDocument doc{"ymaps", NXml::TDocument::RootName};
            FillBsuDocument(NXmlWr::TLibXmlNode{doc.Root()});
            libxmlResult = doc.Root().ToString();
        }
        TString myResult;
        {
            NXmlWr::TDocument doc{"ymaps"};
            FillBsuDocument(doc.Root());
            myResult = doc.Root().AsString();
        }
        UNIT_ASSERT_VALUES_EQUAL(libxmlResult, myResult);
    }

    Y_UNIT_TEST(DocFormattingOn) {
        TString libxmlResult;
        {
            NXml::TDocument doc{"ymaps", NXml::TDocument::RootName};
            FillBsuDocument(NXmlWr::TLibXmlNode{doc.Root()});
            TStringStream sstream;
            doc.Save(sstream, {}, true);
            libxmlResult = sstream.Str();
        }
        TString myResult;
        {
            NXmlWr::TDocument doc{"ymaps"};
            FillBsuDocument(doc.Root());
            myResult = doc.AsString(NXmlWr::TOpts{}.SetShouldFormat(true));
        }
        UNIT_ASSERT_VALUES_EQUAL(libxmlResult, myResult);
    }
}
