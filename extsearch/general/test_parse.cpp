#include "builder.h"
#include "storage.h"
#include "test_common.h"
#include "xal.h"

#include <extsearch/geo/kernel/localeutils/constants.h>

#include <library/cpp/on_disk/mms/cast.h>
#include <library/cpp/resource/resource.h>

#include <util/stream/str.h>

using namespace NGeosearch;
using namespace NGeosearch::NAddress;

namespace {
    struct TAddressSeq {
        struct TAddressDescription {
            TAddressDescription(TAddress a, TString f)
                : Address(a)
                , Formatted(f)
            {
            }

            TAddress Address;
            TString Formatted;
            TString ExpectedXAL;
        };

        size_t size() const {
            return Data.size();
        }
        TAddressDescription& operator[](size_t i) {
            return Data[i];
        }
        const TAddressDescription& operator[](size_t i) const {
            return Data[i];
        }

        TVector<TAddressDescription> Data;
        TStringStream MmsData;
        const TStorage<NMms::TMmapped>* Storage;
    };

    struct TTestData {
        TTestData(const TString& basename);

        THolder<NXml::TDocument> InputXalDocument;
        THolder<NXml::TDocument> InputAddressDocument;
        THolder<NXml::TDocument> ExpectedDocument;

        THolder<NXml::TConstNodes> InputXalNodes;
        THolder<NXml::TConstNodes> InputAddressNodes;
        THolder<NXml::TConstNodes> ExpectedNodes;

        TAddressSeq Addresses;
        TAddressSeq Xals;
    };

    void LoadXAL(const TString& fn, THolder<NXml::TDocument>& doc, THolder<NXml::TConstNodes>& nodes) {
        doc.Reset(new NXml::TDocument(NResource::Find(fn), NXml::TDocument::String));
        nodes.Reset(new NXml::TConstNodes(doc->Root().Nodes("/root/xal:AddressDetails", false, XAL_NS)));
    }

    void LoadAddress(const TString& fn, THolder<NXml::TDocument>& doc, THolder<NXml::TConstNodes>& nodes) {
        doc.Reset(new NXml::TDocument(NResource::Find(fn), NXml::TDocument::String));
        nodes.Reset(new NXml::TConstNodes(doc->Root().Nodes("/root/Address")));
    }

    template <typename ParseFunction>
    TAddressSeq Parse(const NXml::TConstNodes& nodes, const TString& xpath,
                      const ParseFunction& parseFunction) {
        TStorageBuilder builder;

        for (size_t i = 0; i < nodes.Size(); ++i) {
            parseFunction(builder, nodes[i]);
        }
        TAddressSeq result;
        NMms::Write(result.MmsData, builder.Build());
        result.Storage = &NMms::SafeCast<TStorage<NMms::TMmapped>>(result.MmsData.Str());

        for (size_t i = 0; i < nodes.Size(); ++i) {
            const NXml::TConstNode node = nodes[i];
            const auto formatted = node.Node(xpath, true, XAL_NS).Value(TString());
            result.Data.push_back(TAddressSeq::TAddressDescription(TAddress{*result.Storage, parseFunction(builder, node)}, formatted));
        }

        return result;
    }

    TTestData::TTestData(const TString& basename) {
        const auto sourceXmlXal = "/address/" + basename + ".xal.xml";
        const auto sourceXmlAddress = "/address/" + basename + ".input.xml";
        const auto expectedXml = "/address/" + basename + ".expected.xml";

        LoadXAL(sourceXmlXal, InputXalDocument, InputXalNodes);
        LoadAddress(sourceXmlAddress, InputAddressDocument, InputAddressNodes);

        THolder<NXml::TDocument> doc;
        THolder<NXml::TConstNodes> nodes;
        LoadXAL(expectedXml, doc, nodes);

        Y_ENSURE(nodes->size() == InputXalNodes->size(),
                 "objects count differ: " + sourceXmlXal + " vs. " + expectedXml);
        Y_ENSURE(nodes->size() == InputAddressNodes->size(),
                 "objects count differ: " + sourceXmlAddress + " vs. " + expectedXml);

        Xals = Parse(*InputXalNodes, ".//xal:AddressLine",
                     [](TStorageBuilder& builder, const NXml::TConstNode node) { return builder.ParseXAL(node); });

        Addresses = Parse(*InputAddressNodes, "formatted",
                          [](TStorageBuilder& builder, const NXml::TConstNode node) { return builder.ParseAddress(node); });
        Y_ENSURE(nodes->size() == Addresses.size(), "failed to parse: '" << sourceXmlAddress << "'");

        for (size_t i = 0; i < nodes->size(); ++i) {
            TString s = (*nodes)[i].ToString();
            Xals[i].ExpectedXAL = Addresses[i].ExpectedXAL = s;
        }
    }

    TString Premise(const TString& name) {
        TString result;
        TStringOutput ss(result);
        ss << "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
           << "<Country><Locality><Premise>"
           << "<PremiseName>" << name << "</PremiseName>"
           << "</Premise></Locality></Country>"
           << "</AddressDetails>";
        return result;
    }

    void TestLanguages(const TAddressSeq& addresses) {
        const auto multiLangAddress = addresses[0].Address;
        const auto unknownLangAddress = addresses[1].Address;
        const auto turkishLangAddress = addresses[2].Address;
        const auto ruenAddress = addresses[3].Address;
        const auto reallifeAddress = addresses[4].Address;

        ASSERT_EQUAL_XAL(Address2String(multiLangAddress, {}), Premise("EN"));
        ASSERT_EQUAL_XAL(Address2String(multiLangAddress, NLocaleUtils::RU), Premise("RU"));
        ASSERT_EQUAL_XAL(Address2String(multiLangAddress, NLocaleUtils::UK), Premise("UK"));
        ASSERT_EQUAL_XAL(Address2String(multiLangAddress, NLocaleUtils::TR), Premise("TR"));

        ASSERT_EQUAL_XAL(Address2String(unknownLangAddress, {}), Premise("!!"));
        ASSERT_EQUAL_XAL(Address2String(unknownLangAddress, NLocaleUtils::RU), Premise("!!"));

        ASSERT_EQUAL_XAL(Address2String(turkishLangAddress, {}), Premise("TR"));
        ASSERT_EQUAL_XAL(Address2String(turkishLangAddress, NLocaleUtils::RU), Premise("TR"));
        ASSERT_EQUAL_XAL(Address2String(turkishLangAddress, NLocaleUtils::TR), Premise("TR"));

        ASSERT_EQUAL_XAL(Address2String(ruenAddress, NLocaleUtils::RU), Premise("RU"));
        ASSERT_EQUAL_XAL(Address2String(ruenAddress, NLocaleUtils::EN), Premise("EN"));
        ASSERT_EQUAL_XAL(Address2String(ruenAddress, NLocaleUtils::UK), Premise("RU"));
        ASSERT_EQUAL_XAL(Address2String(ruenAddress, NLocaleUtils::TR), Premise("EN"));

        ASSERT_EQUAL_XAL(Address2String(reallifeAddress, NLocaleUtils::UK, "", "Николаев, ул. Дзержинского, 19"),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country><AddressLine>Николаев, ул. Дзержинского, 19</AddressLine>"
                         "<CountryNameCode>UA</CountryNameCode><CountryName>Україна</CountryName>"
                         "<AdministrativeArea><AdministrativeAreaName>Николаевская область</AdministrativeAreaName>"
                         "<Locality><LocalityName>город Николаев</LocalityName>"
                         "<Thoroughfare><ThoroughfareName>улица Дзержинского</ThoroughfareName>"
                         "<Premise><PremiseNumber>19</PremiseNumber></Premise></Thoroughfare></Locality>"
                         "</AdministrativeArea></Country></AddressDetails>");

        ASSERT_EQUAL_XAL(Address2String(multiLangAddress, NLocaleUtils::UK),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country><Locality><Premise>"
                         "<PremiseName>UK</PremiseName>"
                         "</Premise></Locality></Country>"
                         "</AddressDetails>");
    }

    void TestStructure(const TAddressSeq& addresses) {
        for (size_t i = 0; i < addresses.size(); ++i) {
            ASSERT_EQUAL_XAL(Address2String(addresses[i].Address, NLocaleUtils::RU, {}, addresses[i].Formatted),
                             addresses[i].ExpectedXAL);
        }
    }
} // namespace

Y_UNIT_TEST_SUITE(TParseTest) {
    Y_UNIT_TEST(TestStructure) {
        TTestData data("structure");

        TestStructure(data.Xals);
        TestStructure(data.Addresses);
    }

    Y_UNIT_TEST(TestLanguages) {
        TTestData data("languages");

        TestLanguages(data.Xals);
        TestLanguages(data.Addresses);
    }
}
