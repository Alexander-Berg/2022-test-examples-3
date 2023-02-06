#pragma once

#include "attribute_tree.fwd.h"

#include <util/generic/hash.h>
#include <util/generic/string.h>

namespace NTestShard {

using TDocId = ui32;

class TSingleAttribute {
public:
    using TContainer = THashMap<TDocId, TString>;
    using TIterator = TContainer::iterator;
    using TConstIterator = TContainer::const_iterator;

    void Add(TDocId key, const TString& value) {
        Attributes_.emplace(key, value);
    }

    void Add(TDocId key, TString&& value) {
        Attributes_.emplace(key, std::move(value));
    }

    bool Has(TDocId key) const {
        return Attributes_.contains(key);
    }

    TStringBuf Get(TDocId key) const {
        return Attributes_.at(key);
    }

    TIterator begin() {
        return Attributes_.begin();
    }

    TIterator end() {
        return Attributes_.end();
    }

    TConstIterator begin() const {
        return Attributes_.begin();
    }

    TConstIterator end() const {
        return Attributes_.end();
    }

    size_t Size() const {
        return Attributes_.size();
    }

private:
    TContainer Attributes_;
};

class TAttributeCache {
public:
    const TSingleAttribute& Get(const TString& key) const {
        return Cache_.at(key);
    }

    bool Has(const TString& key) const {
        return Cache_.contains(key);
    }

    void Add(const TString& key, TSingleAttribute&& attribute) {
        Cache_.emplace(key, std::move(attribute));
    }

private:
    THashMap<TString, TSingleAttribute> Cache_;
};

class TInverseIndex {
public:
    using TContainer = THashMultiMap<TDocId, TAttrRefTree>;
    using TIterator = TContainer::iterator;
    using TConstIterator = TContainer::const_iterator;

    TInverseIndex(const TSingleAttribute& attribute);

    void ApplyOperation(EOperation op, TInverseIndex&& other);

    TIterator begin() {
        return Index_.begin();
    }

    TIterator end() {
        return Index_.end();
    }

    TConstIterator begin() const {
        return Index_.begin();
    }

    TConstIterator end() const {
        return Index_.end();
    }

private:
    void Intersect(TInverseIndex&& other);
    void Unite(TInverseIndex&& other);

private:
    TContainer Index_;
};

}
