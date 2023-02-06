#include "keyinv_searcher.h"
#include "keyinv_fwd.h"
#include "keyinv_iterator.h"

#include <util/folder/path.h>
#include <util/string/util.h>

namespace NTestShard {

TKeyInvSearcher::TKeyInvSearcher(const TString& shard) {
    TString keyFile = TFsPath(shard) / "indexattrs.key.wad";
    TString invFile = TFsPath(shard) / "indexattrs.inv.wad";

    KeyWad_ = NDoom::IWad::Open(keyFile);
    InvWad_ = NDoom::IWad::Open(invFile);

    KeySearcher_ = MakeHolder<TKeySearcher>(KeyWad_.Get());
    HitSearcher_ = MakeHolder<THitSearcher>(InvWad_.Get());
}

THolder<IIndexIterator> TKeyInvSearcher::GetIterator(const TString& attribute, ui32 maxAsteriskDocs, ui32 minAsteriskPrefix) const {
    TString clone = attribute;
    RemoveAll(clone, '*');
    bool hasAsterisk = clone.size() < attribute.size();
    if (hasAsterisk) {
        return MakeHolder<TMultiKeyInvIterator>(clone, KeySearcher_.Get(), HitSearcher_.Get(), maxAsteriskDocs, minAsteriskPrefix);
    } else {
        return MakeHolder<TKeyInvIterator>(clone, KeySearcher_.Get(), HitSearcher_.Get());
    }
}

}
