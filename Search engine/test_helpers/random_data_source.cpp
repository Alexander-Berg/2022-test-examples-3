#include "random_data_source.h"

#include <util/generic/algorithm.h>
#include <util/generic/yexception.h>

namespace NPlutonium::NChunkler {

TRandomDataSource::TRandomDataSource(ui32 minValue, ui32 maxValue, ui32 maxDeletes, ui32 maxUpdates, ui32 seed)
    : Gen_(seed)
    , ValueDistrib_(minValue, maxValue)
    , DelDistrib_(0, maxDeletes)
    , UpdDistrib_(0, maxUpdates)
{
}

ui64 TRandomDataSource::NextIteration(TVector<ui32>* keysToRemove, TVector<ui32>* keysToUpdate, TVector<ui32>* expectedKeys, TVector<ui64>* expectedTimestamps, ui64 ttl) {
    const ui64 genIndex = NextGeneration_++;
    FillVector(keysToRemove, DelDistrib_(Gen_));
    FillVector(keysToUpdate, UpdDistrib_(Gen_));
    for (const ui32 n : *keysToRemove) {
        State_.erase(n);
    }
    for (const ui32 n : *keysToUpdate) {
        State_[n] = genIndex;
    }

    if (ttl > 0 && genIndex >= ttl) {
        const ui64 threshold = genIndex - ttl;
        TVector<ui32> keysToRemove;
        for (const auto [key, ts] : State_) {
            if (ts <= threshold) {
                keysToRemove.push_back(key);
            }
        }
        for (ui32 key : keysToRemove) {
            State_.erase(key);
        }
    }

    GetExpectedKeysAndTimestamp(expectedKeys, expectedTimestamps);

    return genIndex;
}

void TRandomDataSource::GetExpectedKeysAndTimestamp(TVector<ui32>* expectedKeys, TVector<ui64>* expectedTimestamps) const {
    Y_ENSURE(expectedKeys);

    expectedKeys->clear();
    if (expectedTimestamps) {
        expectedTimestamps->clear();
    }
    for (const auto [n, ts] : State_) {
        expectedKeys->push_back(n);
        if (expectedTimestamps) {
            expectedTimestamps->push_back(ts);
        }
    }
}

void TRandomDataSource::FillVector(TVector<ui32>* numbers, ui32 size) {
    numbers->resize(size);
    for (ui32& n : *numbers) {
        n = ValueDistrib_(Gen_);
    }
    SortUnique(*numbers);
}

}
