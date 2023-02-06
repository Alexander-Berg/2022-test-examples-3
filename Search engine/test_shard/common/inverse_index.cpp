#include "attribute_tree.h"
#include "inverse_index.h"

#include <util/random/fast.h>
#include <util/random/entropy.h>

namespace NTestShard {

TInverseIndex::TInverseIndex(const TSingleAttribute& attribute) {
    for (const auto& p : attribute) {
        Index_.emplace(std::make_pair(p.first, p.second));
    }
}

void TInverseIndex::ApplyOperation(EOperation op, TInverseIndex&& other) {
    switch (op) {
    case EOperation::Or:
        Unite(std::move(other));
        break;
    case EOperation::And:
        Intersect(std::move(other));
        break;
    default:
        break;
    }
}

void TInverseIndex::Intersect(TInverseIndex&& other) {
    TVector<TIterator> toErase;
    for (TIterator it = begin(); it != end(); ++it) {
        TIterator otherIt = other.Index_.find(it->first);
        if (otherIt == other.Index_.end()) {
            toErase.push_back(it);
        } else {
            it->second.Merge(EOperation::And, std::move(otherIt->second));
        }
    }
    for (const TIterator& it : toErase) {
        Index_.erase(it);
    }
}

void TInverseIndex::Unite(TInverseIndex&& other) {
    TVector<TIterator> allIters;
    for (TIterator it = other.begin(); it != other.end(); ++it) {
        allIters.push_back(it);
    }
    TFastRng<ui64> rng(Seed());
    TContainer NewIndex_;
    for (auto& p : Index_) {
        ui64 i = rng.GenRand() % allIters.size();
        TIterator it = allIters[i];
        TDocId otherDoc = it->first;
        p.second.Merge(EOperation::Or, it->second);
        NewIndex_.emplace(p.first, p.second);
        NewIndex_.emplace(otherDoc, std::move(p.second));
    }
    Index_ = std::move(NewIndex_);
}

}
