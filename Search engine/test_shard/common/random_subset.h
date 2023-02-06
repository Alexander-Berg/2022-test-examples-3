#pragma once

#include <util/generic/vector.h>
#include <util/random/fast.h>
#include <util/random/entropy.h>


namespace NPrivate {

template<typename TGen, typename TInt = size_t>
TVector<TInt> GetRandomSubsetImpl(TInt setSize, TInt subsetSize, TGen&& gen) {
    TVector<TInt> subset;
    for (TInt i = 0; i < setSize; ++i) {
        if (i < subsetSize) {
            subset.push_back(i);
        } else {
            const TInt j = gen.GenRand() % (i + 1);
            if (j < subsetSize) {
                subset[j] = i;
            }
        }
    }
    return subset;
}

}

template<class TInt = size_t>
TVector<TInt> GetRandomSubset(TInt setSize, TInt subsetSize) {
    if (setSize < TReallyFastRng32::RandMax()) {
        return NPrivate::GetRandomSubsetImpl(setSize, subsetSize, TReallyFastRng32(Seed()));
    } else {
        return NPrivate::GetRandomSubsetImpl(setSize, subsetSize, TFastRng64(Seed()));
    }
}
