#pragma once

#include <util/generic/map.h>
#include <util/generic/vector.h>

#include <random>

namespace NPlutonium::NChunkler {

struct TRandomDataSource {
    TRandomDataSource(ui32 minValue, ui32 maxValue, ui32 maxDeletes, ui32 maxUpdates, ui32 seed = 0);

    ui64 NextIteration(TVector<ui32>* keysToRemove, TVector<ui32>* keysToUpdate, TVector<ui32>* expectedKeys, TVector<ui64>* expectedTimestamps = nullptr, ui64 ttl = 0);
    void GetExpectedKeysAndTimestamp(TVector<ui32>* expectedKeys, TVector<ui64>* expectedTimestamps) const;

private:
    void FillVector(TVector<ui32>* numbers, ui32 size);

    std::mt19937 Gen_;
    std::uniform_int_distribution<> ValueDistrib_;
    std::uniform_int_distribution<> DelDistrib_;
    std::uniform_int_distribution<> UpdDistrib_;

    TMap<ui32, ui64> State_;
    ui64 NextGeneration_ = 1;
};

}
