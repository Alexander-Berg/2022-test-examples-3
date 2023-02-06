#pragma once

#include <util/generic/string.h>
#include <util/generic/strbuf.h>
#include <array>

const TString XMLNS = "xmlns";
const TString GML = "http://www.opengis.net/gml";
const TString YMAPS = "http://maps.yandex.ru/ymaps/1.x";
const TString BUSINESS = "http://maps.yandex.ru/business/1.x";
const TString ADDRESS = "http://maps.yandex.ru/address/1.x";

template <class TNode>
void FillBsuDocument(TNode root) {
    TNode geoObjectCollection = root.AddElement("GeoObjectCollection");

    TNode featureMember = geoObjectCollection.AddElement("featureMember");
    featureMember.SetAttr(XMLNS, GML);

    TNode geoObject = featureMember.AddElement("GeoObject");
    geoObject.SetAttr(XMLNS, YMAPS);

    TNode metaDataProperty = geoObject.AddElement("metaDataProperty");
    metaDataProperty.SetAttr(XMLNS, GML);

    TNode companyMetaData = metaDataProperty.AddElement("CompanyMetaData");
    companyMetaData.SetAttr(XMLNS, BUSINESS);
    companyMetaData.SetAttr("id", "1306328558");

    companyMetaData.AddText("name", "Белорусский государственный университет");
    companyMetaData.AddOptionalText("shortName", "БГУ");
    companyMetaData.AddText("address", "Беларусь, Минск, проспект Независимости, 4");

    TNode address = companyMetaData.AddElement("Address");
    address.SetAttr(XMLNS, ADDRESS);
    address.AddText("country_code", "BY");
    address.AddText("formatted", "Беларусь, Минск, проспект Независимости, 4");

    auto addComponent = [&address](TStringBuf kind, TStringBuf name) {
        TNode component = address.AddElement("Component");
        component.AddText("kind", kind);
        component.AddText("name", name);
    };
    addComponent("country", "Беларусь");
    addComponent("province", "Минск");
    addComponent("locality", "Минск");
    addComponent("street", "проспект Независимости");
    addComponent("house", "4");

    companyMetaData.AddText("url", "http://www.bsu.by/");

    TNode categories = companyMetaData.AddElement("Categories");
    TNode category = categories.AddElement("Category");
    category.SetAttr("class", "university");
    category.AddText("name", "ВУЗ");
    TNode tags = category.AddElement("Tags");
    for (TStringBuf tag : {"id:184106140", "icon:university", "short_name:ВУЗ", "plural_name:ВУЗы"}) {
        tags.AddText("tag", tag);
    }

    TNode internal = companyMetaData.AddElement("internal");
    internal.AddText("group_size", 1);
    const std::array<int, 7> geoids = {{102268, 157, 29630, 149, 166, 10001, 10000}};
    internal.AddTexts("geoid", geoids);
    internal.AddText("company_id", 1306328558ULL);
    internal.AddElement("featureData");
}
