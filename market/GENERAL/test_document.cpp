#include <market/tools/microindexer/test_document.h>

#include <util/string/builder.h>
#include <util/generic/vector.h>
#include <util/string/split.h>
#include <market/library/libindexer/src/Document.h>
#include <market/library/index_constants/index_constants.h>
#include <market/library/algorithm/digest_base64.h>
#include <market/library/algorithm/Hex.h>
#include <util/string/vector.h>

using namespace Market;

namespace {
    using TType = TField::TType;

    const TVector<std::pair<TType, TVector<TString>>> translate = {
        {TType::ArchiveProperty, {"", "P", "PROP", "PROPERTY"}},
        {TType::GroupingProperty, {"G"}},
        {TType::GroupingPropertyWithName, {"GN"}},
        {TType::SearchLiteral, {"S"}},
        {TType::SomeData, {"SOMEDATA"}},
    };
}

TType TField::ParseFieldType(const TString& s) {
    for (const auto& p : translate) {
        const auto& type = p.first;
        const auto& strings = p.second;
        if (IsIn(strings, s)) {
            return type;
        }
    }
    ythrow yexception() << "unknown field type: " << s;
}

TFieldBag::TFieldBag(const TString& source)
    : PriceOffset(Nothing())
    , Source(source)
{
}

const TString& TFieldBag::GetAttr(const TString& attr) const {
    static TString empty_string;
    const auto it = this->find(attr);
    if (it != end()) {
        return it->second.Value;
    }
    return empty_string;
}

const TString& TFieldBag::GetTitle() const {
    return GetAttr("titleutf8");
}

const TString& TFieldBag::GetBody() const {
    return GetAttr("Body");
}

DocumentType::DocType TFieldBag::DocType() const {
    const auto& docTypeStr = GetAttr("doc_type");
    uint8_t docType;
    if (TryFromString<uint8_t>(docTypeStr, docType)) {
        return static_cast<DocumentType::DocType>(docType);
    }
    return DocumentType::DocType::DT_UNKNOWN;
}

TestDocument::TestDocument(const TFieldBag& bag)
    : Bag(bag)
    , Doc(new TDocument())
    , PriceOffset(Nothing())
{
}

const TFieldBag& TestDocument::Fields() {
    return Bag;
}

TMaybe<int> TestDocument::GetPriceOffset() {
    return PriceOffset;
}

void TestDocument::SetPriceOffset(int priceOffset) {
    PriceOffset = priceOffset;
}

void TestDocument::AddGroupAttr(const TString& attr, int value) {
    if (!Attrs.insert(attr).second) {
        auto msg = TStringBuilder()
                   << "Group attribute " << attr << " already added into the bag from source " << Bag.Source;
        ythrow yexception() << msg;
    }

    Doc->AddGroupAttr(attr, value);
}

THolder<TDocument> TestDocument::Release() {
    return std::move(Doc);
}

TDocument* TestDocument::operator->() const {
    return Doc.Get();
}

void TestDocument::AddSearchLiteral(const TField& field) {
    using namespace NReportIndex;
    if (field.Name == NSearchLiterals::WARE_MD5) {
        Doc->AddProperty(field.Name, field.Value);
        const TString decoded = NMarket::NDigestBase64::Decode(field.Value);
        Doc->AddSearchLiteral(field.Name, hexStr(decoded.c_str(), decoded.length()).c_str());
    } else if (field.Name == NSearchLiterals::SKU) {
        const auto split = SplitString(field.Value, ",");
        if (split) {
            Doc->AddSearchLiteral(field.Name, split[0]);
            Doc->AddSearchLiteral(NSearchLiterals::MARKET_SKU, split[0]);
            Doc->AddSearchLiteral(NSearchLiterals::DELIVERY_OFFSET, split[0]);
        }
    } else if (field.Name == NSearchLiterals::NAVIGATION_NODE_ID) {
        const auto split = SplitString(field.Value, ",");
        for (const auto& nid : split) {
            Doc->AddSearchLiteral(field.Name, nid);
        }
    } else {
        Doc->AddSearchLiteral(field.Name, field.Value);
    }
}
