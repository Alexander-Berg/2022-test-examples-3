#pragma once

#include <util/generic/vector.h>
#include <util/generic/string.h>

namespace NTestShard {

using TDoc = ui32;
using TDocHash = ui64;

class TUrlHasher {
public:
    TUrlHasher(const TString& shardDir);

    TDocHash GetHash(TDoc doc) const;
    TString GetZHash(TDoc doc) const;
    size_t DocCount() const;

private:
    void InitFromWad(const TString& invHashPath);
    void InitFromNonWad(const TString& invHashPath);

private:
    TVector<TDocHash> Hash_;
};

}
