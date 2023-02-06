#pragma once

#include <library/cpp/charset/recyr.hh>

#include <util/generic/string.h>
#include <util/generic/vector.h>

namespace NTestShard {

using TDoc = ui32;

class TAttributeWithDocs {
public:
    TAttributeWithDocs() = default;

    const TVector<TDoc>& Docs() const {
        return Docs_;
    }

    const TString& Attribute() const {
        return Attribute_;
    }

    TUtf16String WideAttribute() const {
        return UTF8ToWide(Attribute_);
    }

    void AddDoc(TDoc doc) {
        Docs_.push_back(doc);
    }

    void SetAttribute(const TUtf16String& wide) {
        Attribute_ = WideToUTF8(wide);
    }

    void SetAttribute(const TString& str) {
        Attribute_ = str;
    }

    bool Empty() const {
        return Docs_.empty();
    }

    size_t Size() const {
        return Docs_.size();
    }

    void Clear() {
        Docs_.clear();
        Attribute_.clear();
    }

private:
    TVector<TDoc> Docs_;
    TString Attribute_;
};

class IIndexIterator {
public:
    virtual ~IIndexIterator() = default;
    virtual bool Read(TAttributeWithDocs* doc) = 0;
};

}
