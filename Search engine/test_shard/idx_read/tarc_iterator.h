#pragma once

#include "index_iterator.h"

#include <util/generic/hash.h>

namespace NTestShard {

class TArcIterator : public IIndexIterator {
public:
    TArcIterator(THashMap<ui32, TString>&& quotes, bool shouldEscape = true);

    bool Read(TAttributeWithDocs* doc) override;

private:
    bool ShouldEscape_ = true;
    THashMap<ui32, TString> Quotes_;
    typename THashMap<ui32, TString>::iterator It_;
};

}
