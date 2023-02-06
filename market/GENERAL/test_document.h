#pragma once

#include <market/library/document_type/document_type.h>
#include <util/generic/fwd.h>
#include <util/string/cast.h>
#include <util/generic/maybe.h>
#include <util/generic/map.h>
#include <util/generic/vector.h>
#include <util/generic/set.h>
#include <set>

/**
 * @brief Поле документа. Читается парсером из текстового файла.
 * Набор полей представляет собой смесь готового документа и
 * каких-то еще данных. Т.е. набор полей чем-то похож на представление оффера в фиде: есть данные, которые напрямую кладутся в индекс,
 * и есть данные, которые проходят обработку и попадают в статистики и иные места.
 */
struct TField {
    enum class TType {
        ArchiveProperty,
        GroupingProperty,
        GroupingPropertyWithName,
        SearchLiteral,
        /// Какие-то данные, которые не предназначены для непосредственной записи в поля
        /// документа, а будут как-то обработаны. Например: данные о наличии в аутлетах
        SomeData,
    };

    static TType ParseFieldType(const TString& s);

    TString Name;
    TType Type;
    TString Value;
};

enum FieldType {
    FT_NONE = 0,
    FT_YBID = 1 << 0,
    FT_MBID = 1 << 1,
    FT_CBID = 1 << 2,
    FT_EXBID = 1 << 3,
    FT_FEE = 1 << 4,
    FT_DATSOURCE = 1 << 5,
    FT_VBID = 1 << 6,
    FT_IS_RECOMMENDED = 1 << 7,
    FT_BID_FLAGS = 1 << 8,
    FT_ANY = 1 << 10,
};

/**
 * @brief Набор полей описывающий один документ, который потом попадет в индекс.
 * Неоторые поля могут быть модифицированы перед записью в индекс или даже удалены/заменены. См. код.
 */
class TFieldBag: public TMultiMap<TString, TField> {
public:
    explicit TFieldBag(const TString& source);

    const TString& GetAttr(const TString& attr) const;

    template <class T>
    void CopyAttr(const TString& name, T& target) const {
        const auto& value = GetAttr(name);
        if (value) {
            target = FromString<T>(value);
        }
    }

    const TString& GetTitle() const;

    const TString& GetBody() const;

    Market::DocumentType::DocType DocType() const;

public:
    TMaybe<int> PriceOffset;
    // origin file the bag read from
    const TString Source;
    
    struct SGlParamValues {
        TVector<double> Values;
        TSet<uint64_t>  Ids;
    };
    TMap<int32_t, SGlParamValues> GlParams;
};

class TDocument;

/**
 * Intended to be created on stack:
 * Class Document has no virtual destructor and it's impossible to pass
 * the doc into IndexerObj.addDocument without leakage by a cast to the parent class.
 * But it's very desirable to have a check for group attributes count to avoid
 * index creation failures by mysterious errors.
 * That's why this wrapper class defines addGroupAttr method and provides interface
 * to access underlying real Document object using array operator.
 **/
class TestDocument: private TNonCopyable {
public:
    explicit TestDocument(const TFieldBag& bag);

    const TFieldBag& Fields();

    TMaybe<int> GetPriceOffset();

    void SetPriceOffset(int priceOffset);

    void AddGroupAttr(const TString& attr, int value);

    THolder<TDocument> Release();

    TDocument* operator->() const;

    void AddSearchLiteral(const TField& field);

    const TMap<int32_t, TFieldBag::SGlParamValues>* GlParams() { return &Bag.GlParams; }

private:
    const TFieldBag Bag;
    THolder<TDocument> Doc;
    TMaybe<int> PriceOffset;
    std::set<TString> Attrs;
};
