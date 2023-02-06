#include "test_common.h"

#include "xal.h"
#include "print_plain.h"

#include <extsearch/geo/kernel/localeutils/constants.h>
#include <extsearch/geo/kernel/xml_writer/xml.h>

#include <library/cpp/resource/resource.h>

#include <util/stream/str.h>
#include <util/stream/file.h>
#include <util/string/split.h>

namespace {
    using namespace NGeosearch;
    using namespace NGeosearch::NAddress;

    // void SaveProtos(IOutputStream& out, const TVector<NPa::Address>& as) {
    //     for (const auto& a : as) {
    //         out << SerializeAddress(a) << "\n";
    //     }
    // }

    void TestProtoParsing(const TString& name) {
        NXml::TDocument doc(NResource::Find("/address/" + name + ".xal.xml"), NXml::TDocument::String);

        NXml::TNamespacesForXPath ns;
        ns.push_back(NXml::TNamespaceForXPath{"xal", "urn:oasis:names:tc:ciq:xsdschema:xAL:2.0"});

        TVector<NPa::Address> parsedProtos;
        const NXml::TConstNodes nodes = doc.Root().Nodes("/root/xal:AddressDetails", false, ns);
        for (size_t i = 0; i < nodes.Size(); ++i) {
            parsedProtos.push_back(ProtoFromXAL(NLocaleUtils::RU, nodes[i]));
        }

        const TString proto = NResource::Find("/address/" + name + ".expected.proto.txt");
        TStringInput fin(proto);
        const auto expectedProtos = ReadChunks(fin);
        // SaveProtos(filename + ".new", expectedProtos); // uncomment to generate expected proto files
        Y_ENSURE(parsedProtos.size() == expectedProtos.size(), "size mismatch:"
                                                                   << " parsed = "
                                                                   << parsedProtos.size()
                                                                   << ", expected = "
                                                                   << expectedProtos.size());

        for (size_t i = 0; i < parsedProtos.size(); ++i) {
            TVector<TString> actualProtoItems, expectedProtoItems;
            Split(SerializeAddress(parsedProtos[i]), "\n", actualProtoItems);
            Split(expectedProtos[i], "\n", expectedProtoItems);
            ASSERT_STRING_VECTORS_EQUAL(actualProtoItems, expectedProtoItems);
        }
    }
} // namespace

Y_UNIT_TEST_SUITE(TXALAndPlainTest) {
    Y_UNIT_TEST(TestXALToProtoStructure) {
        TestProtoParsing("structure");
    }
    Y_UNIT_TEST(TestXALToProtoLanguages) {
        TestProtoParsing("languages");
    }
    Y_UNIT_TEST(TestPrintPlainAddresses) {
        NXml::TDocument doc(NResource::Find("/address/structure.xal.xml"), NXml::TDocument::String);

        NXml::TNamespacesForXPath ns;
        ns.push_back(NXml::TNamespaceForXPath{"xal", "urn:oasis:names:tc:ciq:xsdschema:xAL:2.0"});
        ns.push_back(NXml::TNamespaceForXPath{"plain", "http://maps.yandex.ru/address/1.x"});

        const NXml::TConstNodes nodes = doc.Root().Nodes("/root/xal:AddressDetails", false, ns);
        NXmlWr::TDocument tmpDoc{"Addresses"};
        for (size_t i = 0; i < nodes.Size(); ++i) {
            auto proto = ProtoFromXAL(NLocaleUtils::RU, nodes[i]);
            if (!proto.has_postal_code()) {
                proto.set_postal_code("");
            }
            PrintPlainAddress(tmpDoc.Root(), proto);
        }

        ///* Canonization */
        // TFileOutput expectedFile(YOUR_FILENAME_HERE);
        // tmpDoc.SaveToStream(expectedFile);

        NXml::TDocument expectedPlainAddresses(NResource::Find("/address/plain.expected.xml"), NXml::TDocument::String);
        const NXml::TConstNodes expectedNodes = expectedPlainAddresses.Root().Nodes("/Addresses/plain:Address", false, ns);

        // Save to string and parse again with libxml
        NXml::TDocument actualPlainAddresses = NXml::TDocument{tmpDoc.AsString(), NXml::TDocument::String};
        const NXml::TConstNodes actualNodes = actualPlainAddresses.Root().Nodes("/Addresses/plain:Address", false, ns);
        Y_ENSURE(expectedNodes.size() == actualNodes.size(), "size mismatch:"
                                                                 << " actual = "
                                                                 << actualNodes.size()
                                                                 << ", expected = "
                                                                 << expectedNodes.size());

        for (size_t i = 0; i < expectedNodes.size(); ++i) {
            UNIT_ASSERT_VALUES_EQUAL_C(
                Node2String(actualNodes[i]), Node2String(expectedNodes[i]),
                "elements with index " << i << " are different");
        }
    }
}
