#include "test_common.h"

#include "builder.h"
#include "storage.h"

#include <extsearch/geo/kernel/localeutils/constants.h>
#include <extsearch/geo/kernel/localeutils/localecodes.h>

#include <library/cpp/on_disk/mms/cast.h>

#include <util/generic/algorithm.h>
#include <util/stream/str.h>
#include <util/string/split.h>
#include <util/string/strip.h>

using namespace NGeosearch;
using namespace NGeosearch::NAddress;
using NCommon::EKind;
using TMAF = TMutableAddressFragment;
using TLocalizedString = std::pair<NLocaleUtils::TPackedLocale, TString>;
using TNameSeq = TVector<std::pair<NLocaleUtils::TPackedLocale, TString>>;

namespace {
    bool HasLocale(const TAddress& address, NLocaleUtils::TPackedLocale locale) {
        const auto& ls = address.GetLocales();
        return Find(ls.begin(), ls.end(), locale) != ls.end();
    }

    void CheckAreas(const TAddress& address, const TString& province, const TString& area) {
        const auto s = Address2String(address, NLocaleUtils::RU);
        UNIT_ASSERT_EQUAL(GetTag("AdministrativeAreaName", s), province);
        UNIT_ASSERT_EQUAL(GetTag("SubAdministrativeAreaName", s), area);
    }

    class TTestStorage {
    public:
        using TMmsStorage = TStorage<NMms::TMmapped>;

        TTestStorage();
        TAddress GetAddress(const TString& keys, int excludeMask);
        TAddress GetAddress(const TString& keys);
        const TMmsStorage& GetStorage();

        void Add(const TString& key, const TMAF& maf);
        void AddMulti(const TString& key, EKind kind, const TNameSeq& names, const TString& data = "");

        void Add(const TString& key, EKind kind, const TString& name, NLocaleUtils::TPackedLocale locale, const TString& data = "") {
            const TNameSeq names(1, {locale, name});
            AddMulti(key, kind, names, data);
        }

    private:
        TStorageBuilder StorageBuilder_;
        THashMap<TString, TAddressFragmentId> Idmap_;
        TStringStream MmsData_;
        const TMmsStorage* Storage_;
    };

    TTestStorage::TTestStorage()
        : Storage_(nullptr)
    {
        const auto ru = NLocaleUtils::RU;
        const auto en = NLocaleUtils::EN;

        // several predefined keys for frequent toponyms
        Add("ru", EKind::Country, "????????????", ru, "RU");
        Add("gb", EKind::Country, "Great Britain", en, "GB");
        Add("it", EKind::Country, "????????????", ru, "IT");
        Add("cfo", EKind::Province, "??????", ru);
        Add("mo", EKind::Province, "???????????????????? ??????????????", ru);
        Add("MSK", EKind::Province, "????????????", ru);
        Add("msk", EKind::Locality, "????????????", ru);
        Add("SVO", EKind::Airport, "???????????????? ??????????????????????", ru);
    }

    TAddress TTestStorage::GetAddress(const TString& keys) {
        TAddressFragmentIds ids;
        for (const auto& key : StringSplitter(keys).Split(',')) {
            ids.push_back(Idmap_.at(StripString(key.Token())));
        }
        return TAddress(GetStorage(), ids);
    }

    const TTestStorage::TMmsStorage& TTestStorage::GetStorage() {
        if (!Storage_) {
            NMms::Write(MmsData_, StorageBuilder_.Build());
            Storage_ = &NMms::SafeCast<TMmsStorage>(MmsData_.Str());
        }
        return *Storage_;
    }

    void TTestStorage::Add(const TString& key, const TMAF& maf) {
        Idmap_[key] = StorageBuilder_.AddAddressFragment(maf);
    }

    void TTestStorage::AddMulti(const TString& key, EKind kind, const TNameSeq& names, const TString& countryCode) {
        TMAF maf(kind);
        maf.SetCountryCode(countryCode);
        for (const std::pair<NLocaleUtils::TPackedLocale, TString>& pair : names) {
            maf.SetName(pair.first, pair.second);
        }
        Add(key, maf);
    }

} // namespace

Y_UNIT_TEST_SUITE(TAddtessTest) {
    Y_UNIT_TEST(TestAddressCopyable) {
        TTestStorage ts;

        TAddress a1 = ts.GetAddress("ru");
        TAddress a2 = ts.GetAddress("gb");
        a1 = a2;
    }

    Y_UNIT_TEST(TestUtils) {
        const auto ru = NLocaleUtils::RU;
        const auto en = NLocaleUtils::EN;

        TTestStorage ts;

        ts.AddMulti("ru", EKind::Country, {{ru, "????????????"}, {en, "Russia"}}, "RU");
        ts.AddMulti("msk", EKind::Locality, {{ru, "?????????? ????????????"}, {en, "Moscow city"}});
        ts.Add("zel", EKind::Area, "???????????????????????????? ???????????????????????????????? ??????????", ru);
        ts.AddMulti("leo", EKind::Street, {{ru, "?????????? ???????? ????????????????"}, {en, "Leo Tolstoy street"}});

        const TAddress leo_tolstoy = ts.GetAddress("ru, msk, leo");
        const TAddress zelenograd = ts.GetAddress("ru, MSK, zel");

        UNIT_ASSERT_EQUAL(leo_tolstoy.GetCountryCode(), "RU");

        UNIT_ASSERT_EQUAL(leo_tolstoy.GetName(en), "Leo Tolstoy street");
        UNIT_ASSERT_EQUAL(leo_tolstoy.GetName(ru), "?????????? ???????? ????????????????");

        UNIT_ASSERT_EQUAL(leo_tolstoy.GetFullpath(en), "Russia, Moscow city, Leo Tolstoy street");
        UNIT_ASSERT_EQUAL(leo_tolstoy.GetFullpath(ru), "????????????, ?????????? ????????????, ?????????? ???????? ????????????????");

        UNIT_ASSERT_EQUAL(leo_tolstoy.GetDescription(en), "Moscow city, Russia");
        UNIT_ASSERT_EQUAL(leo_tolstoy.GetDescription(ru), "?????????? ????????????, ????????????");

        UNIT_ASSERT_EQUAL(leo_tolstoy.GetAddressLine(en), "Moscow city, Leo Tolstoy street");
        UNIT_ASSERT_EQUAL(leo_tolstoy.GetAddressLine(ru), "?????????? ????????????, ?????????? ???????? ????????????????");

        UNIT_ASSERT_EQUAL(leo_tolstoy.GetAddressComponentName(ru, EKind::Country), "????????????");
        UNIT_ASSERT_EQUAL(leo_tolstoy.GetAddressComponentName(en, EKind::Country), "Russia");
        UNIT_ASSERT_EQUAL(leo_tolstoy.GetAddressComponentName(ru, EKind::Unknown), "");
        UNIT_ASSERT_EQUAL(leo_tolstoy.GetAddressComponentName(ru, EKind::House), "");
        UNIT_ASSERT_EQUAL(leo_tolstoy.GetAddressComponentName(ru, EKind::House, EKind::Street), "?????????? ???????? ????????????????");

        ASSERT_EQUAL_XAL(Address2String(leo_tolstoy, en, "", "Moscow, Leo Tolstoy st"),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country>"
                         "<AddressLine>Moscow, Leo Tolstoy st</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>Russia</CountryName>"
                         "<Locality>"
                         "<LocalityName>Moscow city</LocalityName>"
                         "<Thoroughfare>"
                         "<ThoroughfareName>Leo Tolstoy street</ThoroughfareName>"
                         "</Thoroughfare></Locality></Country></AddressDetails>");

        ASSERT_EQUAL_XAL(Address2String(leo_tolstoy, ru, "16"),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country>"
                         "<AddressLine>?????????? ????????????, ?????????? ???????? ????????????????, 16</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>????????????</CountryName>"
                         "<Locality><LocalityName>?????????? ????????????</LocalityName>"
                         "<Thoroughfare><ThoroughfareName>?????????? ???????? ????????????????</ThoroughfareName>"
                         "<Premise><PremiseNumber>16</PremiseNumber></Premise></Thoroughfare></Locality></Country></AddressDetails>");

        ASSERT_EQUAL_XAL(Address2String(zelenograd, ru, "??1411"),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country>"
                         "<AddressLine>????????????, ???????????????????????????? ???????????????????????????????? ??????????, ??1411</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>????????????</CountryName>"
                         "<AdministrativeArea>"
                         "<AdministrativeAreaName>????????????</AdministrativeAreaName>"
                         "<SubAdministrativeArea>"
                         "<SubAdministrativeAreaName>???????????????????????????? ???????????????????????????????? ??????????</SubAdministrativeAreaName>"
                         "<Locality>"
                         "<Premise><PremiseNumber>??1411</PremiseNumber></Premise>"
                         "</Locality></SubAdministrativeArea></AdministrativeArea></Country></AddressDetails>");
    }

    Y_UNIT_TEST(TestXalMissedTags) {
        const auto ru = NLocaleUtils::RU;

        TTestStorage ts;

        ts.Add("route", EKind::Route, "???????????????? ??????????", ru);
        ts.Add("area", EKind::Area, "?????????????????????? ??????????", ru);
        ts.Add("river", EKind::Hydro, "???????? ????????????", ru);

        ASSERT_EQUAL_XAL(Address2String(ts.GetAddress("ru, river"), ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country>"
                         "<AddressLine>???????? ????????????</AddressLine><CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>????????????</CountryName><Locality><Premise>"
                         "<PremiseName>???????? ????????????</PremiseName></Premise></Locality></Country></AddressDetails>");

        ASSERT_EQUAL_XAL(Address2String(ts.GetAddress("ru, mo, SVO"), ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country><AddressLine>???????????????????? ??????????????, ???????????????? ??????????????????????</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode><CountryName>????????????</CountryName>"
                         "<AdministrativeArea><AdministrativeAreaName>???????????????????? ??????????????</AdministrativeAreaName>"
                         "<Locality>"
                         "<DependentLocality><DependentLocalityName>???????????????? ??????????????????????</DependentLocalityName></DependentLocality>"
                         "</Locality></AdministrativeArea></Country></AddressDetails>");

        ASSERT_EQUAL_XAL(Address2String(ts.GetAddress("ru, mo, route"), ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country><AddressLine>???????????????????? ??????????????, ???????????????? ??????????</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode><CountryName>????????????</CountryName>"
                         "<AdministrativeArea><AdministrativeAreaName>???????????????????? ??????????????</AdministrativeAreaName>"
                         "<Locality><Thoroughfare><ThoroughfareName>???????????????? ??????????</ThoroughfareName>"
                         "</Thoroughfare></Locality></AdministrativeArea></Country></AddressDetails>");

        ASSERT_EQUAL_XAL(Address2String(ts.GetAddress("ru, mo, area, river"), ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country><AddressLine>???????????????????? ??????????????, ?????????????????????? ??????????, ???????? ????????????</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode><CountryName>????????????</CountryName>"
                         "<AdministrativeArea><AdministrativeAreaName>???????????????????? ??????????????</AdministrativeAreaName>"
                         "<SubAdministrativeArea><SubAdministrativeAreaName>?????????????????????? ??????????</SubAdministrativeAreaName>"
                         "<Locality><Premise><PremiseName>???????? ????????????</PremiseName></Premise></Locality>"
                         "</SubAdministrativeArea></AdministrativeArea></Country></AddressDetails>");
    }

    Y_UNIT_TEST(TestXalRepeatedTags) {
        const auto ru = NLocaleUtils::RU;

        TTestStorage ts;
        ts.Add("zel", EKind::Locality, "????????????????????", ru);
        ts.Add("11", EKind::Locality, "11-?? ????????????????????", ru);

        const TAddress a = ts.GetAddress("ru,msk,zel,11");

        ASSERT_EQUAL_XAL(Address2String(a, ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country>"
                         "<AddressLine>????????????, ????????????????????, 11-?? ????????????????????</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>????????????</CountryName>"
                         "<Locality><LocalityName>????????????</LocalityName>"
                         "<DependentLocality><DependentLocalityName>????????????????????</DependentLocalityName>"
                         "<DependentLocality><DependentLocalityName>11-?? ????????????????????</DependentLocalityName>"
                         "</DependentLocality></DependentLocality></Locality></Country></AddressDetails>");
    }

    Y_UNIT_TEST(TestXalRepeatedProvinceKind) {
        const auto ru = NLocaleUtils::RU;
        const auto en = NLocaleUtils::EN;

        TTestStorage ts;
        ts.Add("england", EKind::Region, "England", en);
        ts.Add("l1", EKind::Province, "London1", en);
        ts.Add("l2", EKind::Province, "London2", en);
        ts.Add("l3", EKind::Locality, "London", en);

        const TAddress a1 = ts.GetAddress("gb,england,l1,l2,l3");
        ASSERT_EQUAL_XAL(Address2String(a1, ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country>"
                         "<AddressLine>England, London1, London2, London</AddressLine>"
                         "<CountryNameCode>GB</CountryNameCode>"
                         "<CountryName>Great Britain</CountryName>"
                         "<AdministrativeArea><AdministrativeAreaName>England</AdministrativeAreaName>"
                         "<SubAdministrativeArea><SubAdministrativeAreaName>London1</SubAdministrativeAreaName>"
                         "<Locality><LocalityName>London</LocalityName>"
                         "</Locality>"
                         "</SubAdministrativeArea>"
                         "</AdministrativeArea>"
                         "</Country>"
                         "</AddressDetails>");

        UNIT_ASSERT_EQUAL(a1.GetFullpath(en), "Great Britain, England, London1, London2, London");
    }

    Y_UNIT_TEST(TestXalRegionProvinceArea) {
        const auto ru = NLocaleUtils::RU;
        const auto en = NLocaleUtils::EN;

        TTestStorage ts;
        ts.Add("england", EKind::Region, "England", en);
        ts.Add("l1", EKind::Province, "Greater London", en);
        ts.Add("l2", EKind::Area, "City of London", en);
        ts.Add("l3", EKind::Locality, "London", en);

        const TAddress a = ts.GetAddress("gb,england, l1, l2, l3");

        ASSERT_EQUAL_XAL(Address2String(a, ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country>"
                         "<AddressLine>England, Greater London, City of London, London</AddressLine>"
                         "<CountryNameCode>GB</CountryNameCode>"
                         "<CountryName>Great Britain</CountryName>"
                         "<AdministrativeArea><AdministrativeAreaName>England</AdministrativeAreaName>"
                         "<SubAdministrativeArea><SubAdministrativeAreaName>Greater London</SubAdministrativeAreaName>"
                         "<Locality><LocalityName>London</LocalityName>"
                         "</Locality>"
                         "</SubAdministrativeArea>"
                         "</AdministrativeArea>"
                         "</Country>"
                         "</AddressDetails>");
    }

    Y_UNIT_TEST(TestXalRepeatedArea) {
        const auto ru = NLocaleUtils::RU;
        const auto en = NLocaleUtils::EN;

        TTestStorage ts;
        ts.Add("england", EKind::Province, "England", en);
        ts.Add("l1", EKind::Area, "London", en);
        ts.Add("l2", EKind::Area, "London", en);
        ts.Add("l3", EKind::Locality, "London", en);

        const TAddress a = ts.GetAddress("gb,england,l1,l2,l3");

        ASSERT_EQUAL_XAL(Address2String(a, ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country>"
                         "<AddressLine>England, London, London, London</AddressLine>"
                         "<CountryNameCode>GB</CountryNameCode>"
                         "<CountryName>Great Britain</CountryName>"
                         "<AdministrativeArea><AdministrativeAreaName>England</AdministrativeAreaName>"
                         "<SubAdministrativeArea><SubAdministrativeAreaName>London</SubAdministrativeAreaName>"
                         "<Locality><LocalityName>London</LocalityName>"
                         "</Locality>"
                         "</SubAdministrativeArea>"
                         "</AdministrativeArea>"
                         "</Country>"
                         "</AddressDetails>");
    }

    Y_UNIT_TEST(TestNativeLanguage) {
        const auto ru = NLocaleUtils::PackLocale(NLocaleUtils::TLocale{NLocaleUtils::ELang::Ru});
        const auto en = NLocaleUtils::PackLocale(NLocaleUtils::TLocale{NLocaleUtils::ELang::En});
        const auto tr = NLocaleUtils::PackLocale(NLocaleUtils::TLocale{NLocaleUtils::ELang::Tr});
        const auto uk = NLocaleUtils::PackLocale(NLocaleUtils::TLocale{NLocaleUtils::ELang::Uk});
        const auto fr = NLocaleUtils::PackLocale(NLocaleUtils::TLocale{NLocaleUtils::ELang::Fr});
        const auto unk = NLocaleUtils::TPackedLocale{};

        TTestStorage ts;

        ts.AddMulti("ru", EKind::Country,
                    {{ru, "????????????"}, {en, "Russia"}, {tr, "Rusya"}}, "RU");

        ts.AddMulti("msk", EKind::Locality,
                    {{en, "Moscow"}, {tr, "Moskova"}});

        ts.AddMulti("street", EKind::Street,
                    {{en, "Leo Tolstoy street"}, {fr, "L??on Tolsto?? rue"}, {tr, "Lev Tolstoy sokak"}});

        ts.Add("house", EKind::House, "16", unk);

        const TAddress countryAddress = ts.GetAddress("ru");
        const TAddress localityAddress = ts.GetAddress("ru,msk");
        const TAddress streetAddress = ts.GetAddress("ru,msk,street");
        const TAddress houseAddress = ts.GetAddress("ru,msk,street,house");

        UNIT_ASSERT(HasLocale(countryAddress, ru));
        UNIT_ASSERT(HasLocale(localityAddress, en));
        UNIT_ASSERT(HasLocale(streetAddress, en));

        UNIT_ASSERT_EQUAL(streetAddress.GetAddressLine(uk), "Moscow, Leo Tolstoy street");
        UNIT_ASSERT_EQUAL(houseAddress.GetAddressLine(ru), "Moscow, Leo Tolstoy street, 16");
        UNIT_ASSERT_EQUAL(houseAddress.GetAddressLine(en), "Moscow, Leo Tolstoy street, 16");

        UNIT_ASSERT_EQUAL(streetAddress.GetFullpath(ru), "????????????, Moscow, Leo Tolstoy street");
        UNIT_ASSERT_EQUAL(streetAddress.GetFullpath(en), "Russia, Moscow, Leo Tolstoy street");
        UNIT_ASSERT_EQUAL(streetAddress.GetFullpath(tr), "Rusya, Moskova, Lev Tolstoy sokak");

        UNIT_ASSERT_EQUAL(streetAddress.GetFullpath(unk), "Russia, Moscow, Leo Tolstoy street");

        ASSERT_EQUAL_XAL(Address2String(streetAddress, ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country>"
                         "<AddressLine>Moscow, Leo Tolstoy street</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>????????????</CountryName>"
                         "<Locality>"
                         "<LocalityName>Moscow</LocalityName>"
                         "<Thoroughfare>"
                         "<ThoroughfareName>Leo Tolstoy street</ThoroughfareName>"
                         "</Thoroughfare></Locality></Country></AddressDetails>");

        ASSERT_EQUAL_XAL(Address2String(streetAddress, en),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country>"
                         "<AddressLine>Moscow, Leo Tolstoy street</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>Russia</CountryName>"
                         "<Locality>"
                         "<LocalityName>Moscow</LocalityName>"
                         "<Thoroughfare>"
                         "<ThoroughfareName>Leo Tolstoy street</ThoroughfareName>"
                         "</Thoroughfare></Locality></Country></AddressDetails>");

        ASSERT_EQUAL_XAL(Address2String(streetAddress, fr),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country>"
                         "<AddressLine>Moscow, L??on Tolsto?? rue</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>Russia</CountryName>"
                         "<Locality>"
                         "<LocalityName>Moscow</LocalityName>"
                         "<Thoroughfare>"
                         "<ThoroughfareName>L??on Tolsto?? rue</ThoroughfareName>"
                         "</Thoroughfare></Locality></Country></AddressDetails>");

        ASSERT_EQUAL_XAL(Address2String(streetAddress, tr),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country>"
                         "<AddressLine>Moskova, Lev Tolstoy sokak</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>Rusya</CountryName>"
                         "<Locality>"
                         "<LocalityName>Moskova</LocalityName>"
                         "<Thoroughfare>"
                         "<ThoroughfareName>Lev Tolstoy sokak</ThoroughfareName>"
                         "</Thoroughfare></Locality></Country></AddressDetails>");
    }

    Y_UNIT_TEST(TestPostalCode) {
        const auto ru = NLocaleUtils::RU;

        TTestStorage ts;
        ts.Add("leo", EKind::Street, "?????????? ???????? ????????????????", ru);
        ts.Add("vav", EKind::Street, "?????????? ????????????????", ru);

        TMAF leo1(EKind::House);
        leo1.SetName(ru, "1");
        leo1.SetPostalCode("119021");
        ts.Add("leo1", leo1);

        TMAF vav1(EKind::House);
        vav1.SetName(ru, "1");
        vav1.SetPostalCode("119333");
        ts.Add("vav1", vav1);

        UNIT_ASSERT_EQUAL(ts.GetAddress("ru").GetPostalCode(), "");
        UNIT_ASSERT_EQUAL(ts.GetAddress("ru, msk").GetPostalCode(), "");
        UNIT_ASSERT_EQUAL(ts.GetAddress("ru, msk, leo").GetPostalCode(), "");
        UNIT_ASSERT_EQUAL(ts.GetAddress("ru, msk, leo, leo1").GetPostalCode(), "119021");
        UNIT_ASSERT_EQUAL(ts.GetAddress("ru, msk, vav").GetPostalCode(), "");
        UNIT_ASSERT_EQUAL(ts.GetAddress("ru, msk, vav, vav1").GetPostalCode(), "119333");
    }

    Y_UNIT_TEST(TestEmpty) {
        const auto ru = NLocaleUtils::RU;

        TTestStorage ts;

        UNIT_ASSERT_NO_EXCEPTION(TAddress(ts.GetStorage(), TAddressFragmentIds()));

        const TAddress a = ts.GetAddress("ru");
        UNIT_ASSERT(HasLocale(a, ru));

        ASSERT_EQUAL_XAL(Address2String(a, ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\">"
                         "<Country><CountryNameCode>RU</CountryNameCode><CountryName>????????????</CountryName></Country>"
                         "</AddressDetails>");
    }

    Y_UNIT_TEST(TestXalAreas) {
        const auto ru = NLocaleUtils::RU;

        TTestStorage ts;

        ts.Add("toscana", EKind::Province, "??????????????", ru);
        ts.Add("flor1", EKind::Area, "??????????????????1", ru);
        ts.Add("flor2", EKind::Area, "??????????????????2", ru);
        ts.Add("flor", EKind::Locality, "?????????? ??????????????????", ru);

        ts.Add("??????????????", EKind::Province, "?????????????????? ??????????????", ru);
        ts.Add("??????????", EKind::Area, "?????????????????? ?????????? ????????????", ru);
        ts.Add("??????????", EKind::Locality, "????????????", ru);

        CheckAreas(ts.GetAddress("it, toscana, flor1, flor2, flor"), "??????????????", "??????????????????1");
        CheckAreas(ts.GetAddress("it, toscana, flor1, flor2"), "??????????????????1", "??????????????????2");
        CheckAreas(ts.GetAddress("it, toscana, flor1"), "??????????????", "??????????????????1");
        CheckAreas(ts.GetAddress("it, toscana"), "??????????????", "");

        CheckAreas(ts.GetAddress("ru, cfo, ??????????????, ??????????, ??????????"), "?????????????????? ??????????????", "?????????????????? ?????????? ????????????");
        CheckAreas(ts.GetAddress("ru, cfo, ??????????????, ??????????"), "?????????????????? ??????????????", "?????????????????? ?????????? ????????????");
        CheckAreas(ts.GetAddress("ru, cfo, ??????????????"), "??????", "?????????????????? ??????????????");
        CheckAreas(ts.GetAddress("ru, cfo"), "??????", "");
    }

    Y_UNIT_TEST(TestLocalityInsideDistrict) {
        const auto ru = NLocaleUtils::RU;

        TTestStorage ts;
        ts.Add("butovo", EKind::District, "????????????", ru);
        ts.Add("harvest", EKind::Locality, "?????????????? ????????????", ru);

        const TAddress h = ts.GetAddress("ru,msk,butovo,harvest");
        UNIT_ASSERT_EQUAL(h.GetFullpath(ru), "????????????, ????????????, ????????????, ?????????????? ????????????");

        ASSERT_EQUAL_XAL(Address2String(h, ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country>"
                         "<AddressLine>????????????, ????????????, ?????????????? ????????????</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>????????????</CountryName>"
                         "<Locality><LocalityName>????????????</LocalityName>"
                         "<DependentLocality><DependentLocalityName>????????????</DependentLocalityName>"
                         "<DependentLocality><DependentLocalityName>?????????????? ????????????</DependentLocalityName>"
                         "</DependentLocality></DependentLocality>"
                         "</Locality></Country></AddressDetails>");
    }

    Y_UNIT_TEST(TestAddressValidation) {
        TTestStorage ts;

        // toplevel
        UNIT_ASSERT_NO_EXCEPTION(ts.GetAddress("it"));
        UNIT_ASSERT_EXCEPTION(ts.GetAddress("cfo"), yexception);
        UNIT_ASSERT_EXCEPTION(ts.GetAddress("msk"), yexception);

        // containment
        UNIT_ASSERT_NO_EXCEPTION(ts.GetAddress("ru, cfo, msk"));
        UNIT_ASSERT_EXCEPTION(ts.GetAddress("ru, msk, cfo"), yexception);
        UNIT_ASSERT_EXCEPTION(ts.GetAddress("ru, it"), yexception);
    }

    Y_UNIT_TEST(TestXalAirports) {
        const auto ru = NLocaleUtils::RU;

        TTestStorage ts;
        ts.Add("td", EKind::Airport, "???????????????? ??", ru);

        const TAddress svo = ts.GetAddress("ru,msk, SVO");
        const TAddress td = ts.GetAddress("ru,msk, SVO, td");
        UNIT_ASSERT_EQUAL(svo.GetFullpath(ru), "????????????, ????????????, ???????????????? ??????????????????????");
        UNIT_ASSERT_EQUAL(td.GetFullpath(ru), "????????????, ????????????, ???????????????? ??????????????????????, ???????????????? ??");

        ASSERT_EQUAL_XAL(Address2String(svo, ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country>"
                         "<AddressLine>????????????, ???????????????? ??????????????????????</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>????????????</CountryName>"
                         "<Locality><LocalityName>????????????</LocalityName>"
                         "<DependentLocality><DependentLocalityName>???????????????? ??????????????????????</DependentLocalityName>"
                         "</DependentLocality>"
                         "</Locality></Country></AddressDetails>");

        ASSERT_EQUAL_XAL(Address2String(td, ru),
                         "<AddressDetails xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country>"
                         "<AddressLine>????????????, ???????????????? ??????????????????????, ???????????????? ??</AddressLine>"
                         "<CountryNameCode>RU</CountryNameCode>"
                         "<CountryName>????????????</CountryName>"
                         "<Locality><LocalityName>????????????</LocalityName>"
                         "<DependentLocality><DependentLocalityName>???????????????? ??????????????????????</DependentLocalityName>"
                         "<Premise><PremiseName>???????????????? ??</PremiseName></Premise>"
                         "</DependentLocality>"
                         "</Locality></Country></AddressDetails>");
    }
}
