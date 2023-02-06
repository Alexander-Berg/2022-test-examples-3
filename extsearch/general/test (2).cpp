#include <extsearch/geo/saas/common/protos/company.pb.h>
#include <extsearch/geo/saas/common/mms/gen_types.h>
#include <extsearch/geo/saas/common/mms/proto2mms.h>

#include <extsearch/geo/saas/common/ut/test_2d.h>
#include <extsearch/geo/saas/common/ut/test_2d.pb.h>
#include <extsearch/geo/saas/common/ut/test_2d.proto2mms.h>

#include <google/protobuf/descriptor.pb.h>

#include <library/cpp/testing/unittest/registar.h>

template <class T>
TString SerializeMms(const T& value) {
    TStringStream stringStream;
    NMms::SafeWrite(stringStream, value);
    return stringStream.Str();
}

template <class T>
void AssertEqualMms(const T& a, const T& b) {
    UNIT_ASSERT_EQUAL(SerializeMms(a), SerializeMms(b));
}

template <class T>
void AssertNotEqualMms(const T& a, const T& b) {
    UNIT_ASSERT_UNEQUAL(SerializeMms(a), SerializeMms(b));
}

using namespace NGeosearch::NMmsTypes::NBusiness;

Y_UNIT_TEST_SUITE(CodegenSuite) {
    Y_UNIT_TEST(Vector_2D) {
        NTesting::NCpp::TDocEmbeddings<NMms::TStandalone> obj, obj2;
        obj.Data.resize(3);
        obj.Data[0].push_back(12);
        obj.Data[0].push_back(34);
        obj.Data[1].push_back(56);
        NTesting::NProtos::TDocEmbeddings objProto;
        NTesting::NCodegen::Copy_MmsToProto_TDocEmbeddings(objProto, obj);
        NTesting::NCodegen::Copy_ProtoToMms_TDocEmbeddings(obj2, objProto);
        UNIT_ASSERT_EQUAL(obj.Data, obj2.Data);
    }
    Y_UNIT_TEST(TCompanyImpl_Test) {
        TCompany<NMms::TStandalone> obj, objCopy, objCopy2;

        obj.Ids.push_back("id4");
        obj.Ids.push_back("id3");

        obj.Name.Add(12, "Horns & Hooves LLC");
        obj.Name.Add(34, "ООО Рога и Копыта");

        obj.Shortname.Add(12, "Horns&Hooves");
        obj.Shortname.Add(34, "Рога и Копыта");

        obj.Synonyms.push_back("HH");
        obj.Synonyms.push_back("Horns and Hooves");

        obj.Urls.push_back("horns-and-hooves.com");
        obj.Urls.push_back("horns-hooves.com");

        obj.Emails.push_back("sales@horns-and-hooves.com");
        obj.Emails.push_back("support@horns-and-hooves.com");
        obj.Emails.push_back("feedback@horns-and-hooves.com");

        obj.Rubrics.push_back(345);
        obj.Rubrics.push_back(711);

        obj.ChainId = 23;

        obj.Providers.push_back("prov2");
        obj.Providers.push_back("prov3");

        obj.Owners.push_back(47);
        obj.Owners.push_back(88);

        obj.Address.push_back(98);
        obj.Address.push_back(11);

        obj.AddressLine.Add(12, "1600 Amphitheatre Parkway, Mountain View, CA 94043");
        obj.AddressLine.Add(34, "Москва, ул. Балчуг, 7");

        obj.Kind = NGeosearch::NCommon::EKind::Bridge;
        obj.Precision = NGeosearch::NMmsTypes::NBusiness::EPrecision::Near;

        obj.GeoIds.push_back(629);
        obj.CityGeoId = 628;

        obj.Point.X = 12.34;
        obj.Point.Y = 49.27;
        obj.Span.GetLowerCorner().X = 10;
        obj.Span.GetLowerCorner().Y = 40;
        obj.Span.GetUpperCorner().X = 20;
        obj.Span.GetUpperCorner().Y = 50;

        obj.Hours.Intervals.push_back(TTimeInterval{10, 20});
        obj.Hours.Intervals.push_back(TTimeInterval{30, 40});
        obj.Hours.TimezoneOffset = 47;

        obj.Phones.resize(1);
        obj.Phones[0].Formatted = "+7 999 234-47-77 ext. 23";
        obj.Phones[0].Country = "Russia";
        obj.Phones[0].Prefix = "RU";
        obj.Phones[0].Number = "+79992344777";
        obj.Phones[0].Ext = "23";
        obj.Phones[0].Info.Add(12, "something");
        obj.Phones[0].Info.Add(23, "something else");
        obj.Phones[0].Type = EPhoneType::PhoneFax;

        obj.Links.resize(2);
        obj.Links[0].Type = "type";
        obj.Links[0].Aref = "areff";
        obj.Links[0].Url = "http://example.com";
        obj.Links[1].Type = "type2";
        obj.Links[1].Aref = "arefff";
        obj.Links[1].Url = "http://example2.com";

        obj.Features.resize(1);
        obj.Features[0].Id = "456";
        obj.Features[0].Values.push_back("val1");
        obj.Features[0].Values.push_back("value2");
        obj.Features[0].Aref = "reference";

        obj.FeatureData = "feature_data";

        obj.FastFeaturesGroups.push_back("fast");
        obj.FastFeaturesGroups.push_back("quick");
        obj.FastFeaturesGroups.push_back("slow");

        obj.Timestamp = 111;

        obj.Rating.ConstructInPlace();
        obj.Rating->Score = 0.7f;
        obj.Rating->Ratings = 31;
        obj.Rating->Reviews = 12;

        obj.SnippetRubric = 733;
        obj.Flags = 0x103;
        obj.MainRubricCount = 200;

        NGeosearch::NProtos::TCompany objProto;
        NGeosearch::NCodegen::Copy_MmsToProto_TCompany(objProto, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TCompany(objCopy, objProto);
        AssertEqualMms(obj, objCopy);

        NGeosearch::NProtos::TCompany objProto2;
        NGeosearch::NCodegen::Copy_MmsToProto_TCompany(objProto2, obj);
        NGeosearch::NCodegen::Copy_MmsToProto_TCompany(objProto2, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TCompany(objCopy2, objProto2);
        NGeosearch::NCodegen::Copy_ProtoToMms_TCompany(objCopy2, objProto2);
        AssertEqualMms(obj, objCopy2);
    };

    Y_UNIT_TEST(TCompanyImpl_Test_Fail) {
        TCompany<NMms::TStandalone> obj, objCopy;

        NGeosearch::NProtos::TCompany objProto;
        NGeosearch::NCodegen::Copy_MmsToProto_TCompany(objProto, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TCompany(objCopy, objProto);

        objCopy.CityGeoId = 23;

        AssertNotEqualMms(obj, objCopy);
    };

    Y_UNIT_TEST(TCompanyImpl_CopyMaybeTest) {
        TCompany<NMms::TStandalone> obj1;
        obj1.ShowedRating = 5.0f;

        NGeosearch::NProtos::TCompany objProto;
        UNIT_ASSERT(!objProto.HasShowedRating());
        NGeosearch::NCodegen::Copy_MmsToProto_TCompany(objProto, obj1);
        UNIT_ASSERT(objProto.HasShowedRating());
        UNIT_ASSERT_EQUAL(objProto.GetShowedRating(), 5.0f);

        TCompany<NMms::TStandalone> obj2;
        // obj2.ShowedRating is not set
        NGeosearch::NCodegen::Copy_MmsToProto_TCompany(objProto, obj2);
        UNIT_ASSERT(!objProto.HasShowedRating());
    }

    Y_UNIT_TEST(TAddressFragments_Test) {
        TAddressFragments<NMms::TStandalone> obj, objCopy;

        obj.Items.resize(2);
        obj.Items[0].Kind = 567;
        obj.Items[0].Names.emplace(12, "one name");
        obj.Items[0].Names.emplace(34, "another name");
        obj.Items[0].CountryCode = "RU";
        obj.Items[0].PostalCode = "postal_code";
        obj.Items[1].Kind = 319;
        obj.Items[1].Names.emplace(56, "oneee name");
        obj.Items[1].Names.emplace(78, "anotherrr name");
        obj.Items[1].CountryCode = "EN";
        obj.Items[1].PostalCode = "code";

        NGeosearch::NProtos::TAddressFragments objProto;
        NGeosearch::NCodegen::Copy_MmsToProto_TAddressFragments(objProto, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TAddressFragments(objCopy, objProto);
        AssertEqualMms(obj, objCopy);
    };

    Y_UNIT_TEST(TChainNames_Test) {
        TChainNames<NMms::TStandalone> obj, objCopy;

        obj.Items.resize(2);
        obj.Items[0].Locale = "EN";
        obj.Items[0].Value = "en_value";
        obj.Items[1].Locale = "RU";
        obj.Items[1].Value = "val_ru";

        NGeosearch::NProtos::TChainNames objProto;
        NGeosearch::NCodegen::Copy_MmsToProto_TChainNames(objProto, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TChainNames(objCopy, objProto);
        AssertEqualMms(obj, objCopy);
    };

    Y_UNIT_TEST(TChainImpl_Test) {
        TChainImpl<NMms::TStandalone> obj, objCopy;

        obj.Id = 389;
        obj.Name.Add(67, "Apple");
        obj.Name.Add(78, "Orange");
        obj.Rubrics.push_back(77);
        obj.Rubrics.push_back(929);
        obj.Phones.resize(1);
        obj.Phones[0].Formatted = "+7 999 234-47-77 ext. 23";
        obj.Phones[0].Country = "Russia";
        obj.Phones[0].Prefix = "RU";
        obj.Phones[0].Number = "+79992344777";
        obj.Phones[0].Ext = "23";
        obj.Phones[0].Info.Add(12, "something");
        obj.Phones[0].Info.Add(23, "something else");
        obj.Phones[0].Type = EPhoneType::Phone;
        obj.Urls.push_back("http://ya.ru");
        obj.Urls.push_back("http://url.com");
        obj.Emails.push_back("a@host.com");
        obj.Emails.push_back("b@host2.com");
        obj.Owners.resize(2);
        obj.Owners[0].GeoId = 61;
        obj.Owners[0].OwnerId = 16;
        obj.Owners[1].GeoId = 27;
        obj.Owners[1].OwnerId = 31;
        obj.Children = 1;

        NGeosearch::NProtos::TChainImpl objProto;
        NGeosearch::NCodegen::Copy_MmsToProto_TChainImpl(objProto, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TChainImpl(objCopy, objProto);
        AssertEqualMms(obj, objCopy);
    };

    Y_UNIT_TEST(TFeatures_Test) {
        TFeatures<NMms::TStandalone> obj, objCopy;

        obj.Items.resize(1);
        obj.Items[0].Id = "222";
        obj.Items[0].Type = NGeosearch::NMmsTypes::NBusiness::EFeatureType::Enum;
        obj.Items[0].Name.Add(81, "feature_name");
        obj.Items[0].IsNameHidden = true;
        obj.Items[0].EnumValues["enum_key"].Add(12, "enum_12");
        obj.Items[0].EnumValues["enum_key"].Add(34, "enum_2");
        obj.Items[0].EnumValues["enum_key2"].Add(12, "enum_56");
        obj.Items[0].EnumValues["enum_key2"].Add(34, "enum_88");
        obj.Items[0].FastFeaturesGroup = "my_group";
        obj.Items[0].DisplayMode = NGeosearch::NMmsTypes::NBusiness::EFeatureDisplayMode::AlwaysShow;

        NGeosearch::NProtos::TFeatures objProto;
        NGeosearch::NCodegen::Copy_MmsToProto_TFeatures(objProto, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TFeatures(objCopy, objProto);
        AssertEqualMms(obj, objCopy);
    };

    Y_UNIT_TEST(TFactorsImpl_Test) {
        TFactors<NMms::TStandalone> obj, objCopy;

        obj.CompanyId = "234324";
        obj.X = 45;
        obj.Y = 7;
        obj.Reserved1 = 34;
        obj.DynamicBooking = 6;
        obj.Foursquare = 56;
        obj.FeatureCount = 9;
        obj.SiblingCount = 6;
        obj.Flags = 0x12;
        obj.PhotoCount = 23;
        obj.Precision = EPrecision::Number;
        obj.HostRank = 22;
        obj.SearchYaBar = 54;
        obj.SearchYaBarCoreHost = 88;
        obj.SearchOwnerClicksPCTR = 19;
        obj.OrgShows = 1.2f;
        obj.OrgClicks = 0.9f;
        obj.OrgPCTR = 0.333f;
        obj.OrgShowsOrg1Query = 0.23f;
        obj.OrgClicksOrg1Query = 0.37f;
        obj.OrgPCTROrg1Query = 0.999f;
        obj.OrgShowsRubricQuery = 0.711f;
        obj.OrgClicksRubricQuery = 0.51f;
        obj.OrgPCTRRubricQuery = 0.66f;
        obj.OrgShowsFixed = 0.82f;
        obj.OrgDeepClicks = 0.116f;
        obj.OrgUrlShows = 0.002f;
        obj.OrgUrlDeepClicks = 0.0233f;
        obj.OrgRubricShows = 0.877f;
        obj.OrgRubricShortClicks = 0.714f;
        obj.OrgRubricSuperLongClicks = 0.189f;
        obj.OrgRubricDeepClicks = 0.48f;
        obj.OrgRegionSuperLongClicks = 0.5551f;
        obj.OrgRegionDeepClicks = 12.3f;
        obj.GoodReviewRatio = 1.91f;
        obj.FreshReviewRatio = 0.728f;
        obj.GoodFreshReviewRatio = 0.82f;
        obj.ExpFactors.push_back(0.1f);
        obj.ExpFactors.push_back(0.39f);

        NGeosearch::NProtos::TFactors objProto;
        NGeosearch::NCodegen::Copy_MmsToProto_TFactors(objProto, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TFactors(objCopy, objProto);
        AssertEqualMms(obj, objCopy);
    };

    Y_UNIT_TEST(TProviders_Test) {
        TProviders<NMms::TStandalone> obj, objCopy;

        obj.Items.resize(1);
        obj.Items[0].Id = "provider_name";
        obj.Items[0].Name.Add(31, "31_name");
        obj.Items[0].Name.Add(43, "nname_43");
        obj.Items[0].Uri.Add(32, "http://host32.com");
        obj.Items[0].Uri.Add(322, "http://host322.something.net");

        NGeosearch::NProtos::TProviders objProto;
        NGeosearch::NCodegen::Copy_MmsToProto_TProviders(objProto, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TProviders(objCopy, objProto);
        AssertEqualMms(obj, objCopy);
    };

    Y_UNIT_TEST(TRubrics_Test) {
        TRubrics<NMms::TStandalone> obj, objCopy;

        obj.Items.resize(1);
        obj.Items[0].Id = 78991;
        obj.Items[0].ParentId = 39921;
        obj.Items[0].AppleLevel = 16;
        obj.Items[0].RubricClass = "shop";
        obj.Items[0].Name.Add(723, "shop_723");
        obj.Items[0].Name.Add(722, "722shop");
        obj.Items[0].Filters.resize(1);
        obj.Items[0].Filters[0].Type = NGeosearch::NMmsTypes::NBusiness::EFilterType::Feature;
        obj.Items[0].Filters[0].Id = "filter_di";
        obj.Items[0].SnippetFeatures.push_back("first_feature");
        obj.Items[0].SnippetFeatures.push_back("second_feature");
        obj.Items[0].AppleRubrics.push_back("app_ruubric_frist");
        obj.Items[0].AppleRubrics.push_back("apple_second_r");
        obj.Items[0].SerpData = NGeosearch::NMmsTypes::NBusiness::ESerpDataType::Link;

        NGeosearch::NProtos::TRubrics objProto;
        NGeosearch::NCodegen::Copy_MmsToProto_TRubrics(objProto, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TRubrics(objCopy, objProto);
        AssertEqualMms(obj, objCopy);
    };

    Y_UNIT_TEST(TRubricExes_Test) {
        TRubricExes<NMms::TStandalone> obj, objCopy;

        obj.Items.resize(1);
        obj.Items[0].Id = 78991;
        obj.Items[0].Names.resize(2);
        obj.Items[0].Names[0].Locale = "ch";
        obj.Items[0].Names[0].Value = "value_ch";
        obj.Items[0].Names[1].Locale = "de";
        obj.Items[0].Names[1].Value = "deval";
        obj.Items[0].Keywords.resize(2);
        obj.Items[0].Keywords[0].Locale = "ch";
        obj.Items[0].Keywords[0].Value = "kw_ch";
        obj.Items[0].Keywords[1].Locale = "de";
        obj.Items[0].Keywords[1].Value = "kwde";

        NGeosearch::NProtos::TRubricExes objProto;
        NGeosearch::NCodegen::Copy_MmsToProto_TRubricExes(objProto, obj);
        NGeosearch::NCodegen::Copy_ProtoToMms_TRubricExes(objCopy, objProto);
        AssertEqualMms(obj, objCopy);
    };

    Y_UNIT_TEST(EPhoneType_Test) {
        using EPhoneType = NGeosearch::NProtos::EPhoneType;

        const auto getText = [](EPhoneType phoneType) {
            const auto* enumDescriptor = NGeosearch::NProtos::EPhoneType_descriptor();
            const auto* enumValueDescriptor = enumDescriptor->FindValueByNumber(phoneType);
            return enumValueDescriptor->options().GetExtension(NBroto::text);
        };

        UNIT_ASSERT_EQUAL(getText(EPhoneType::Phone), "phone");
        UNIT_ASSERT_EQUAL(getText(EPhoneType::Fax), "fax");
        UNIT_ASSERT_EQUAL(getText(EPhoneType::PhoneFax), "phone_fax");
    }
};
