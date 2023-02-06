#include "patch_applier_test_data.h"

#include <extsearch/geo/kernel/pbreport/fwd.h>
#include <extsearch/geo/kernel/pbreport/pb.h>
#include <extsearch/geo/kernel/pbreport/intermediate/metadatacollection.h>
#include <extsearch/geo/kernel/protobuf_io/io.h>
#include <extsearch/geo/kernel/common/kind.h>                // EKind
#include <extsearch/geo/kernel/localeutils/constants.h>      // TGeosearch::NLocaleUtils::RU
#include <extsearch/geo/kernel/localeutils/locale.h>         // TGeosearch::NLocaleUtils::TLocale
#include <extsearch/geo/kernel/working_hours/timeinterval.h> //NGeosearch::NWorkingHours::TTimeInterval

#include <sprav/protos/export.pb.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/xml/document/xml-document.h>

#include <google/protobuf/text_format.h>

#include <util/draft/date.h>
#include <util/string/split.h>

using namespace NFastExport;
namespace NLocaleUtils = NGeosearch::NLocaleUtils;

namespace {
    NGeosearch::NProtos::TLocalizedString LsFromPairs(TVector<std::pair<TString, TString>> pairs) {
        NGeosearch::NProtos::TLocalizedString res;
        for (const auto& p : pairs) {
            NGeosearch::NProtos::TLocalizedString_LocaleToDataItem ltd;
            ltd.SetKey(NLocaleUtils::PackLocale(FromString<NLocaleUtils::TLocale>(p.first)));
            ltd.SetValue(p.second);
            (*res.AddLocaleToData()) = ltd;
        }
        return res;
    }

    TVector<NGeosearch::NProtos::TAddressFragment>
    FragmentsFromPairs(TVector<std::pair<NGeosearch::NCommon::EKind, TString>> pairs) {
        TVector<NGeosearch::NProtos::TAddressFragment> res;
        res.reserve(pairs.size());
        for (const auto& p : pairs) {
            NGeosearch::NProtos::TAddressFragment source;

            NGeosearch::NProtos::TAddressFragment::NamesItem name;
            name.SetKey(NLocaleUtils::RU);
            name.SetValue(p.second);

            (*source.AddNames()) = name;

            source.SetKind(ui32(p.first));
            res.push_back(std::move(source));
        }
        return res;
    }

    using NGeosearch::NWorkingHours::EDay;
    using NGeosearch::NWorkingHours::TTime;

    NGeosearch::NProtos::TTimeInterval TimeIntervalFromPair(std::pair<TTime, TTime> pair) {
        NGeosearch::NProtos::TTimeInterval ti;
        ti.SetFrom(pair.first.GetMinutesFromWeekStart());
        ti.SetTo(pair.second.GetMinutesFromWeekStart());
        return ti;
    }

    TVector<NGeosearch::NProtos::TTimeInterval>
    TimeIntervalsFromPairs(TVector<std::pair<TTime, TTime>> pairs) {
        TVector<NGeosearch::NProtos::TTimeInterval> res;
        res.reserve(pairs.size());
        for (const auto& p : pairs) {
            res.push_back(TimeIntervalFromPair(p));
        }
        return res;
    }

    void AddPhone(TSnippetPatch& patch) {
        /*
  <Phones>
    <Phone type="phone" hide="0">
      <formatted>+7 (908) 234-58-88</formatted>
      <country>7</country>
      <prefix>909</prefix>
      <number>2345888</number>
      <info xml:lang="ru">бла-бла-бла</info>
      <info xml:lang="en">bla-bla-bla</info>
    </Phone>
  </Phones>
         */
        TPhonePatch phones;
        NGeosearch::NProtos::TPhone phone;
        phone.SetFormatted("+7 (908) 234-58-88");
        phone.SetCountry("7");
        phone.SetPrefix("909");
        phone.SetNumber("2345888");

        (*phone.MutableInfo()) = LsFromPairs({{"ru", "бла-бла-бла"}, {"en", "bla-bla-bla"}});

        (*phones.AddValues()) = phone;
        (*patch.MutablePhones()) = phones;
    }

    void AddHiddenPhone(TSnippetPatch& patch) {
        TPhonePatch phones;
        NGeosearch::NProtos::TPhone phone;
        phone.SetFormatted("+7 (908) 234-58-89");
        phone.SetCountry("7");
        phone.SetPrefix("909");
        phone.SetNumber("2345889");

        (*phones.AddValues()) = phone;
        (*patch.MutableHiddenPhones()) = phones;
    }

    void AddAddress(TSnippetPatch& patch) {
        TAddressPatch address;

        /*
    <Address xmlns="http://maps.yandex.ru/geocoder/internal/1.x">
      <country_code>RU</country_code>
      <postal_code>603666</postal_code>
      <formatted xml:lang="ru">Россия, Нижний Новгород, Ковалихинская-Ижорская улица, 73Б</formatted>
      <Component>
        <kind>country</kind>
        <name locale="ru">Россия</name>
      </Component>
      <Component>
        <kind>province</kind>
        <name locale="ru">Приволжский федеральный округ</name>
      </Component>
      <Component>
        <kind>province</kind>
        <name locale="ru">Нижегородская область</name>
      </Component>
      <Component>
        <kind>area</kind>
        <name locale="ru">городской округ Нижний Новгород</name>
      </Component>
      <Component>
        <kind>locality</kind>
        <name locale="ru">Нижний Новгород</name>
      </Component>
      <Component>
        <kind>street</kind>
        <name locale="ru">Ижорская улица</name>
      </Component>
      <Component>
        <kind>house</kind>
        <name locale="ru">50/2</name>
      </Component>
    </Address>
         */

        address.SetCountryCode("RU");

        address.SetAddressLine("Россия, Нижний Новгород, Ковалихинская-Ижорская улица, 73Б");

        for (const auto& f : FragmentsFromPairs({{NGeosearch::NCommon::EKind::Country, "Россия"},
                                                 {NGeosearch::NCommon::EKind::Province, "Приволжский федеральный округ"},
                                                 {NGeosearch::NCommon::EKind::Province, "Нижегородская область"},
                                                 {NGeosearch::NCommon::EKind::Area, "городской округ Нижний Новгород"},
                                                 {NGeosearch::NCommon::EKind::Locality, "Нижний Новгород"},
                                                 {NGeosearch::NCommon::EKind::Street, "Ижорская улица"},
                                                 {NGeosearch::NCommon::EKind::House, "50/2"}})) {
            (*address.AddFragments()) = f;
        }

        address.SetExtraInfo("подъезд 1");

        (*patch.MutableAddress()) = address;
    }

    void AddTurkeyAddress(TSnippetPatch& patch) {
        TAddressPatch address;

        address.SetCountryCode("TR");

        address.SetAddressLine("Kavaklıdere Mh., Karanfil Sk., No: 38/A, Çankaya, Ankara, Turkey");

        for (const auto& f : FragmentsFromPairs({{NGeosearch::NCommon::EKind::Country, "Turkey"},
                                                 {NGeosearch::NCommon::EKind::Area, "Ankara"},
                                                 {NGeosearch::NCommon::EKind::Locality, "Çankaya"},
                                                 {NGeosearch::NCommon::EKind::District, "Kavaklıdere Mh."},
                                                 {NGeosearch::NCommon::EKind::Street, "Karanfil Sk."},
                                                 {NGeosearch::NCommon::EKind::House, "No: 38/A"}})) {
            (*address.AddFragments()) = f;
        }

        (*patch.MutableAddress()) = address;
    }

    void AddHours(TSnippetPatch& patch) {
        NGeosearch::NProtos::THours hours;

        /*
        <Hours>
            <Availability>
                <Monday/>
                <Tuesday/>
                <Wednesday/>
                <Thursday/>
                <Friday/>
                <Interval from="10:00:00" to="17:00:00"/>
            </Availability>
            <Availability>
                <Saturday/>
                <Interval from="10:00:00" to="16:00:00"/>
            </Availability>
            <tzOffset>10800</tzOffset>
       </Hours>
        */

        for (const auto& interval : TimeIntervalsFromPairs({{TTime(EDay::MONDAY, 10, 0), TTime(EDay::MONDAY, 17, 0)},
                                                            {TTime(EDay::TUESDAY, 10, 0), TTime(EDay::TUESDAY, 17, 0)},
                                                            {TTime(EDay::WEDNESDAY, 10, 0), TTime(EDay::WEDNESDAY, 17, 0)},
                                                            {TTime(EDay::THURSDAY, 10, 0), TTime(EDay::THURSDAY, 17, 0)},
                                                            {TTime(EDay::FRIDAY, 10, 0), TTime(EDay::FRIDAY, 17, 0)},
                                                            {TTime(EDay::SATURDAY, 10, 0), TTime(EDay::SATURDAY, 16, 0)}})) {
            (*hours.AddIntervals()) = interval;
        }
        hours.SetTimezoneOffset(180);
        (*patch.MutableHours()) = hours;
    }

    void AddEmptyHours(TSnippetPatch& patch) {
        // The patch states that working hours of the company are unknown.
        patch.MutableHours();
    }

    void AddScheduledHours(TSnippetPatch& patch) {
        google::protobuf::Map<TString, NGeosearch::NProtos::THours> scheduledHours;

        /*
         <ScheduledHours>
          <Availability iso_date="2018-11-05">
            <Interval from="00:00" to="05:00"/>
            <Monday/>
          </Availability>
          <Availability iso_date="2018-11-05">
            <Interval from="06:00" to="10:00"/>
            <Monday/>
          </Availability>
          <Availability iso_date="2018-11-05">
            <Interval from="11:00" to="24:00"/>
            <Monday/>
          </Availability>
          <Availability iso_date="2018-11-06">
            <Interval from="00:00" to="10:00"/>
            <Tuesday/>
          </Availability>
          <Availability iso_date="2018-11-07">
            <Interval from="00:00" to="00:00"/>
            <Wednesday/>
          </Availability>
          <Availability iso_date="2018-11-09">
            <Interval from="00:00" to="00:00"/>
            <Friday/>
          </Availability>
          <Availability iso_date="2018-11-11">
            <Interval from="00:00" to="05:00"/>
            <Sunday/>
          </Availability>
          <Availability iso_date="2018-11-11">
            <Interval from="05:00" to="24:00"/>
            <Sunday/>
          </Availability>
        </ScheduledHours>
         */
        (*scheduledHours["20181105"].AddIntervals()) = TimeIntervalFromPair({TTime(EDay::MONDAY, 0, 0), TTime(EDay::MONDAY, 5, 0)});
        (*scheduledHours["20181105"].AddIntervals()) = TimeIntervalFromPair({TTime(EDay::MONDAY, 6, 0), TTime(EDay::MONDAY, 10, 0)});
        (*scheduledHours["20181105"].AddIntervals()) = TimeIntervalFromPair({TTime(EDay::MONDAY, 11, 0), TTime(EDay::MONDAY, 24, 0)});
        (*scheduledHours["20181106"].AddIntervals()) = TimeIntervalFromPair({TTime(EDay::TUESDAY, 0, 0), TTime(EDay::TUESDAY, 10, 0)});
        scheduledHours["20181107"] = NGeosearch::NProtos::THours();
        scheduledHours["20181109"] = NGeosearch::NProtos::THours();
        (*scheduledHours["20181111"].AddIntervals()) = TimeIntervalFromPair({TTime(EDay::SUNDAY, 5, 0), TTime(EDay::SUNDAY, 24, 0)});

        (*patch.MutableScheduledHours()->MutableValues()) = scheduledHours;
    }

    void AddClosedInfo(TSnippetPatch& patch, bool close, bool permanent) {
        TClosedInfoPatch closedInfo;

        closedInfo.SetIsClosed(close);
        closedInfo.SetIsClosedPermanently(permanent);
        closedInfo.SetIsUnreliable(permanent);

        (*patch.MutableClosedInfo()) = closedInfo;
    }

    void AddTags(TSnippetPatch& patch, bool withClosedForVisitors = false) {
        TTagPatch tagPatch;

        TTag tag;
        tag.SetId("moved"), tag.SetValue("");
        (*tagPatch.AddValues()) = tag;
        tag.SetId("unusual_hours"), tag.SetValue("2018-11-04");
        (*tagPatch.AddValues()) = tag;
        tag.SetId("unusual_hours"), tag.SetValue("2018-11-05");
        (*tagPatch.AddValues()) = tag;
        if (withClosedForVisitors) {
            tag.SetId("closed_for_quarantine"), tag.SetValue("0");
            (*tagPatch.AddValues()) = tag;
        }

        (*patch.MutableTags()) = tagPatch;
    }

    void AddRubrics(TSnippetPatch& patch) {
        TRubricPatch rubricPatch;

        {
            NFastExport::TRubric rubric;
            rubric.SetId(1);
            rubric.SetName("Кафе");
            rubric.SetAppleLevel(1);
            (*rubricPatch.AddValues()) = rubric;
        }
        {
            NFastExport::TRubric rubric;
            rubric.SetId(2);
            rubric.SetName("Ресторан");
            rubric.SetAppleLevel(2);
            (*rubricPatch.AddValues()) = rubric;
        }
        {
            NFastExport::TRubric rubric;
            rubric.SetId(3);
            rubric.SetName("Бар");
            rubric.SetClass("Eat");
            rubric.SetAppleLevel(3);
            rubric.SetAppleRubric("Bar");
            rubric.SetSeoname("bar");
            rubric.AddTags("id:3");
            rubric.AddTags("plural_name:Бары");
            (*rubricPatch.AddValues()) = rubric;
        }
        {
            NFastExport::TRubric rubric;
            rubric.SetId(4);
            rubric.SetName("Ночной клуб");
            rubric.SetAppleLevel(4);
            (*rubricPatch.AddValues()) = rubric;
        }

        (*patch.MutableRubrics()) = rubricPatch;
    }

    void AddEmptyRubrics(TSnippetPatch& patch) {
        TRubricPatch rubricPatch;
        (*patch.MutableRubrics()) = rubricPatch;
    }

    void AddLocation(TSnippetPatch& patch, bool addEntrances) {
        TLocationPatch locationPatch;

        auto* point = locationPatch.MutablePoint();
        point->SetLon(30.001);
        point->SetLat(50.001);

        auto* span = locationPatch.MutableSpan();
        auto* lowerCorner = span->MutableLowerCorner();
        lowerCorner->SetLon(30.0005);
        lowerCorner->SetLat(50.0005);
        auto* upperCorner = span->MutableUpperCorner();
        upperCorner->SetLon(30.0015);
        upperCorner->SetLat(50.0015);

        if (addEntrances) {
            auto* ent1 = locationPatch.AddEntrance();
            auto entCoords1 = ent1->MutablePoint();
            entCoords1->SetLon(30.00075);
            entCoords1->SetLat(50.00075);
            auto* ent2 = locationPatch.AddEntrance();
            auto entCoords2 = ent2->MutablePoint();
            entCoords2->SetLon(30.00125);
            entCoords2->SetLat(50.00125);
            ent2->SetNormalAzimuth(45);
        }

        locationPatch.SetPrecision("exact");

        (*patch.MutableLocation()) = locationPatch;
    }

    void AddLinks(TSnippetPatch& patch, bool addSocialLinks, bool addSignUpLinks = false) {
        TLinksPatch linksPatch;

        {
            auto* link = linksPatch.AddValues();
            link->SetType("mining");
            link->SetAref("provider1");
            link->SetUrl("http://www.provider1.ru");
        }
        {
            auto* link = linksPatch.AddValues();
            link->SetType("booking");
            link->SetAref("provider2");
            link->SetUrl("http://www.provider2.ru");
        }
        {
            auto* link = linksPatch.AddValues();
            link->SetType("booking");
            link->SetAref("yandex-eda");
            link->SetUrl("http://eda.yandex/restaurant/stolovka");
        }
        if (addSocialLinks) {
            auto* link = linksPatch.AddValues();
            link->SetType("social");
            link->SetAref("provider3");
            link->SetUrl("http://vk.com/group_1");
        }
        if (addSocialLinks) {
            auto* link = linksPatch.AddValues();
            link->SetType("social");
            link->SetAref("provider4");
            link->SetUrl("http://facebook.com/1");
        }
        if (addSignUpLinks) {
            auto* link = linksPatch.AddValues();
            link->SetType("social");
            link->SetAref("yclients");
            link->SetUrl("https://n100827.yclients.com/?utm_source=Yandex_maps&utm_medium=cpm");
        }

        (*patch.MutableLinks()) = linksPatch;
    }

    void AddAttributions(TSnippetPatch& patch) {
        TAttributionsPatch attributionsPatch;

        {
            auto* attribution = attributionsPatch.AddValues();
            attribution->SetCode("provider1");
            attribution->SetName("Переименованный поставщик №1");
            attribution->SetUri("http://www.provider1-new.ru");
        }
        {
            auto* attribution = attributionsPatch.AddValues();
            attribution->SetCode("provider3");
            attribution->SetName("Поставщик №3");
            attribution->SetUri("http://www.provider3.ru");
        }

        (*patch.MutableAttributions()) = attributionsPatch;
    }

    void AddOtherAttributions(TSnippetPatch& patch) {
        TAttributionsPatch attributionsPatch;

        {
            auto* attribution = attributionsPatch.AddValues();
            attribution->SetCode("provider2");
            attribution->SetName("Поставщик №2");
            attribution->SetUri("http://www.provider2.ru");
        }
        {
            auto* attribution = attributionsPatch.AddValues();
            attribution->SetCode("provider4");
            attribution->SetName("Поставщик №4");
            attribution->SetUri("http://www.provider4.ru");
        }

        (*patch.MutableAttributions()) = attributionsPatch;
    }

    void AddFeatures(TSnippetPatch& patch) {
        TFeaturesPatch featuresPatch;

        {
            auto* feature = featuresPatch.AddValues();
            feature->SetId("cuisine");
            feature->SetType("enum");
            feature->SetAref("provider1");
            feature->SetName("кухня");
            {
                auto featureValue = feature->AddValues();
                featureValue->SetValue("итальянская");
                featureValue->SetEnumValueId("italian");
            }
            {
                auto featureValue = feature->AddValues();
                featureValue->SetValue("русская");
                featureValue->SetEnumValueId("russian");
            }
            feature->SetDisplayMode(NFastExport::TFeature::AlwaysShow);
        }
        {
            auto* feature = featuresPatch.AddValues();
            feature->SetId("wifi");
            feature->SetType("bool");
            feature->SetAref("provider2");
            feature->SetName("есть wifi");
            {
                auto featureValue = feature->AddValues();
                featureValue->SetValue("1");
            }
            feature->SetDisplayMode(NFastExport::TFeature::AlwaysShow);
        }
        {
            auto* feature = featuresPatch.AddValues();
            feature->SetId("description");
            feature->SetType("text");
            feature->SetAref("provider3");
            feature->SetName("описание");
            {
                auto featureValue = feature->AddValues();
                featureValue->SetValue("Описание №1");
            }
            {
                auto featureValue = feature->AddValues();
                featureValue->SetValue("Описание №2");
            }
            feature->SetDisplayMode(NFastExport::TFeature::AlwaysShow);
        }
        {
            auto* feature = featuresPatch.AddValues();
            feature->SetId("secret");
            feature->SetType("bool");
            feature->SetAref("provider4");
            feature->SetName("секрет");
            {
                auto featureValue = feature->AddValues();
                featureValue->SetValue("1");
            }
            feature->SetDisplayMode(NFastExport::TFeature::AlwaysHide);
        }
        {
            auto* feature = featuresPatch.AddValues();
            feature->SetId("food");
            feature->SetType("feature_group");
            feature->SetName("Еда");
            feature->AddFeatureGroupIds("cuisine");
            feature->AddFeatureGroupIds("wifi");
            feature->SetDisplayMode(NFastExport::TFeature::AlwaysHide);
        }

        (*patch.MutableFeatures()) = featuresPatch;
    }

    void AddAdverts(TSnippetPatch& patch) {
        TAdvertPatch advertPatch;

        advertPatch.add_pageid("123");

        advertPatch.mutable_advert()->mutable_text_data()->set_text("Приведи друга и получи скидку!");
        advertPatch.mutable_advert()->mutable_text_data()->set_title("Акция «Приведи друга»");
        advertPatch.mutable_advert()->mutable_text_data()->set_url("http://action.com/");

        advertPatch.mutable_advert()->mutable_promo()->set_title("Осенняя распродажа");
        advertPatch.mutable_advert()->mutable_promo()->set_details("До конца сентября все товары почти даром!");
        advertPatch.mutable_advert()->mutable_promo()->Mutablebanner()->set_url("http://promo.com");

        auto phone0 = advertPatch.AddPhone();
        phone0->set_formatted("123-45-67");
        phone0->set_number("1234567");
        phone0->set_type(NGeosearch::NProtos::EPhoneType::Phone);
        auto phone1 = advertPatch.AddPhone();
        phone1->set_formatted("890-12-34");
        phone1->set_number("8901234");
        phone1->set_type(NGeosearch::NProtos::EPhoneType::Fax);

        advertPatch.set_hidebookinglinks(true);

        auto property0 = advertPatch.mutable_advert()->add_property();
        property0->set_key("actionType");
        property0->set_value("url");
        auto property1 = advertPatch.mutable_advert()->add_property();
        property1->set_key("actionTitle");
        property1->set_value("Забронировать столик");

        {
            auto action = advertPatch.mutable_advert()->add_action();
            action->set_type("OpenSite");
            auto titleProp = action->add_property();
            titleProp->set_key("title");
            titleProp->set_value("Записаться");
            auto typeProp = action->add_property();
            typeProp->set_key("extype");
            typeProp->set_value("boolking");
            auto urlProp = action->add_property();
            urlProp->set_key("url");
            urlProp->set_value("http://action.com");
        }

        {
            auto product = advertPatch.mutable_advert()->add_product();
            product->set_title("Специльное предложение");
            product->mutable_photo()->set_url("http://photo_url.com");
            product->mutable_price()->set_currency("RUB");
            product->mutable_price()->set_text("100 р");
            product->mutable_price()->set_value(100);
        }

        *patch.AddAdverts() = advertPatch;
    }

    void AddVisualHintsTags(TSnippetPatch& patch) {
        const auto addTag = [&](auto id, auto value) {
            auto* tag = patch.MutableTags()->AddValues();
            tag->SetId(id);
            tag->SetValue(value);
        };
        addTag("snippet_show_verified", "show_verified");
        addTag("snippet_show_subline", "menu");
        addTag("snippet_show_subline", "average_bill2");
        addTag("deatailview_show_feedback_button", "show_feedback_button");
        addTag("detailview_show_taxi_button", "not_show_taxi_button");
    }

    void AddEntranceMetadata(TMetadataCollection& collection) {
        auto* entranceMetadata = collection.AppendMetadata(NGeosearch::NPbReport::NPb::entrance::ENTRANCE_METADATA);
        auto* entrance = entranceMetadata->add_entrance();
        entrance->mutable_point()->set_lon(1.2);
        entrance->mutable_point()->set_lat(3.4);
    }

    void AddRoutePointMetadata(TMetadataCollection& collection) {
        auto* routePointMetadata = collection.AppendMetadata(NGeosearch::NPbReport::NPb::search::route_point::ROUTE_POINT_METADATA);
        auto* entrance = routePointMetadata->add_entrance();
        entrance->mutable_point()->set_lon(5.6);
        entrance->mutable_point()->set_lat(7.8);
        routePointMetadata->set_route_point_context("");
    }

    void AddVisualHintsMetadata(TMetadataCollection& collection) {
        collection.AppendMetadata(NGeosearch::NPbReport::NPb::search::visual_hints::GEO_OBJECT_METADATA);
    }
} // namespace

namespace NTestData {
    TSnippetPatch GenerateSnippetPatch(int patchNumber) {
        TSnippetPatch res;
        TUrlPatch urls;
        TEmailsPatch emails;
        res.SetLocale("ru");

        switch (patchNumber) {
            case 0:
                res.SetName("Лига джентельменов с другим названием");
                res.SetShortName("Лига джентельменов короткая");
                urls.AddValues("http://domi.kz/");
                (*res.MutableUrls()) = urls;
                AddPhone(res);
                AddHiddenPhone(res);
                AddAddress(res);
                AddHours(res);
                AddScheduledHours(res);
                AddTags(res);
                res.AddGeoIds(5);
                emails.AddValues("liga@mail.ru");
                emails.AddValues("mail@liga.ru");
                (*res.MutableEmails()) = emails;
                AddClosedInfo(res, false, false);
                AddRubrics(res);
                AddLocation(res, true);
                AddLinks(res, false);
                AddAttributions(res);
                AddFeatures(res);
                break;
            case 1:
                res.SetName("");
                res.SetShortName("");
                (*res.MutableUrls()) = urls;
                AddHiddenPhone(res);
                AddHours(res);
                AddAddress(res);
                AddTags(res, true);
                res.AddGeoIds(5);
                AddClosedInfo(res, true, true);
                AddEmptyRubrics(res);
                AddLocation(res, false);
                AddLinks(res, true);
                AddOtherAttributions(res);
                break;
            case 2:
                AddClosedInfo(res, false, false);
                urls.AddValues("http://goodsite.ru");
                urls.AddValues("http://badsite.ru");
                (*res.MutableUrls()) = urls;
                AddHours(res);
                AddTurkeyAddress(res);
                AddLinks(res, false);
                break;
            case 3:
                AddHours(res);
                (*res.MutableUrls()) = urls;
                {
                    auto* link = res.MutableLinks()->AddValues();
                    link->SetType("booking");
                    link->SetAref("yandex-eda");
                    link->SetUrl("http://eda.yandex/restaurant/stolovka");
                }
                AddClosedInfo(res, true, false);
                break;
            case 4:
                AddEmptyHours(res);
                AddClosedInfo(res, false, false);
                break;
            case 5:
                AddEmptyHours(res);
                AddClosedInfo(res, true, true);
                break;
            case 6:
                AddLinks(res, true);
                AddAdverts(res);
                break;
            case 7:
                AddVisualHintsTags(res);
                break;
            case 8:
                AddLinks(res, true, true);
                AddAdverts(res);
        }

        return res;
    }

    NFastExport::TDeleteUrlsPatch GenerateDeleteUrlPatch() {
        NFastExport::TDeleteUrlsPatch patch;

        auto* rknUrl = patch.AddBannedUrls();
        rknUrl->SetTrieName("rkn.trie");
        rknUrl->SetUrl("http://badsite.ru");

        auto* edaUrl = patch.AddBannedUrls();
        edaUrl->SetTrieName("eda_blacklist");
        edaUrl->SetUrl("http://eda.yandex/restaurant/stolovka");

        return patch;
    }

    TAttributions GetAttributions() {
        THashMap<TString, TString> attributions;

        const TString testData = NResource::Find("business_response.xml");
        NXml::TDocument doc(testData, NXml::TDocument::Source::String);
        NXml::TNode attributionNode = doc.Root().FirstChild("Attribution").FirstChild("Source");
        while (!attributionNode.IsNull()) {
            const TString providerId = attributionNode.Attr<TString>("id");
            attributions.insert(std::make_pair(providerId, attributionNode.ToString()));

            attributionNode = attributionNode.NextSibling("Source");
        }

        return attributions;
    };

    TVector<TTestDataProvider> CreateDocs() {
        auto metadatas = LoadTestProtos<NGeosearch::NPbReport::NIntermediate::TMetadata>("business_response_proto.txt");
        TVector<TTestDataProvider> docs;
        for (const auto& metadata : metadatas) {
            TTestDataProvider doc;
            doc.MetadataCollection().Append(metadata);
            docs.push_back(std::move(doc));
        }
        docs[0].AddDocAttribute(GC_SERP_SUBTITLE_TYPE_GTA, "average_bill2");

        return docs;
    }

    TTestDataProvider CreateDocWithEntrances(bool entranceMetadata, bool routePointMetadata) {
        TTestDataProvider doc;
        auto metadatas = LoadTestProtos<NGeosearch::NPbReport::NIntermediate::TMetadata>("business_response_proto.txt");
        Y_ENSURE(!metadatas.empty());

        auto& collection = doc.MetadataCollection();
        collection.Append(metadatas[0]);
        if (entranceMetadata) {
            AddEntranceMetadata(collection);
        }
        if (routePointMetadata) {
            AddRoutePointMetadata(collection);
        }

        return doc;
    }

    TTestDataProvider CreateDocWithVisualHints() {
        TTestDataProvider doc;
        const TString testData = NResource::Find("business_response_proto.txt");
        auto metadatas = LoadTestProtos<NGeosearch::NPbReport::NIntermediate::TMetadata>("business_response_proto.txt");
        Y_ENSURE(!metadatas.empty());

        auto& collection = doc.MetadataCollection();
        collection.Append(metadatas[0]);
        AddVisualHintsMetadata(collection);

        return doc;
    }

    NFastExport::TSnippetPatch ConstructClosedForVisitorsPatch() {
        NFastExport::TSnippetPatch patch;

        TTagPatch tagPatch;

        TTag tag;
        tag.SetId("closed_for_visitors");
        tag.SetValue("");
        (*tagPatch.AddValues()) = tag;

        (*patch.MutableTags()) = tagPatch;

        return patch;
    }

    NFastExport::TSnippetPatch ConstructClosedPatch(bool permanently) {
        NFastExport::TSnippetPatch patch;
        AddClosedInfo(patch, true, permanently);
        return patch;
    }

    template <class TMessage>
    TVector<TMessage> LoadTestProtos(const TString& filename) {
        TVector<TMessage> res;
        const TString testData = NResource::Find(filename);
        for (const auto& protoStr : StringSplitter(testData).SplitByString("---")) {
            TMessage proto;
            Y_ENSURE(NProtoBuf::TextFormat::ParseFromString(TString{protoStr.Token()}, &proto));
            res.push_back(proto);
        }

        return res;
    }
    template TVector<NSpravExport::TExportedCompany> LoadTestProtos(const TString& filename);
    template TVector<NSpravExport::TExportedRubric> LoadTestProtos(const TString& filename);
    template TVector<NSpravExport::TExportedProvider> LoadTestProtos(const TString& filename);
    template TVector<NSpravExport::TExportedFeature> LoadTestProtos(const TString& filename);
    template TVector<NSpravTDS::Company> LoadTestProtos(const TString& filename);
    template TVector<NGeosearch::NPbReport::NIntermediate::TMetadata> LoadTestProtos(const TString& filename);

    void AddTycoonExtraDataToPatch(NFastExport::TSnippetPatch& patch, const NSpravTDS::Company& data) {
        *(patch.MutableExtraTycoonData()) = data;
    }

    void AddDescriptionToPatch(NFastExport::TSnippetPatch& patch, const TString& description) {
        patch.MutableExtraTycoonData()->mutable_profile()->set_description(description);
    }

    void AddTycoonExtraDataToAttributes(TTestDataProvider& dataProvider, const NSpravTDS::Company& data) {
        if (dataProvider.HasDocAttribute("tycoon_extra_data/1.x")) {
            dataProvider.EraseDocAttribute("tycoon_extra_data/1.x");
        }
        dataProvider.AddDocAttribute("tycoon_extra_data/1.x", NGeosearch::Base64Serialize(data));
    }

    NSpravTDS::Company ParseTycoonExtraData(TTestDataProvider& dataProvider) {
        NSpravTDS::Company result;
        NGeosearch::ParseFromBase64String(result, dataProvider.GetDocAttribute("tycoon_extra_data/1.x"));
        return result;
    }
} // namespace NTestData
