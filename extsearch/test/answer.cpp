#include "answer.h"

using namespace NImages::NLinkSelector;

bool TRankPair::operator<(const TRankPair& second) const {
    if (Result < second.Result) {
        return true;
    }
    if (Result > second.Result) {
        return false;
    }
    return (Truth < second.Truth);
}

bool TRankPair::LessByTruth(const TRankPair& s) const {
    TRankPair first(Result, Truth);
    TRankPair second(s.Result, s.Truth);
    return first < second;
}

bool TRankPair::operator==(const TRankPair& second) {
    return Truth == second.Truth && Result == second.Result;
}

size_t TRankAnswer::size() const {
    return RankedSet.size();
}

TRankPair TRankAnswer::operator[](const size_t i) const {
    return TRankPair(RankedSet[i].Truth, RankedSet[i].Result);
};

void TRankAnswer::SortDataByResult() {
    Sort(RankedSet.begin(), RankedSet.end());
}

void TRankAnswer::Add(TRankPair newElement) {
    RankedSet.push_back(newElement);
}

void TRankComparation::AddTruth(const TString& key, float value) {
    Truth[key] = value;
}
void TRankComparation::SetRankerResult(float result) {
    RankerResult = result;
}

TRankPair TRankComparation::GetRank(const TString& key) const {
    return TRankPair(Truth.at(key), RankerResult);
}

TRankPair TRankComparation::GetRank(const THashMap<TString, float>& weights) const {
    float result = 0;
    for (const auto& weight : weights) {
        result += Truth.at(weight.first) * weight.second;
    }
    return TRankPair(result, RankerResult);
    ;
}
