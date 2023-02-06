#pragma once

#include "index_iterator.h"
#include "keyinv_fwd.h"

namespace NTestShard {

class TKeyInvSearcher {
public:
    TKeyInvSearcher(const TString& shardDir);

    THolder<IIndexIterator> GetIterator(const TString& attribute, ui32 maxAsteriskDocs, ui32 minAsteriskPrefix) const;

private:
    THolder<NDoom::IWad> KeyWad_;
    THolder<NDoom::IWad> InvWad_;

    THolder<TKeySearcher> KeySearcher_;
    THolder<THitSearcher> HitSearcher_;
};

}
