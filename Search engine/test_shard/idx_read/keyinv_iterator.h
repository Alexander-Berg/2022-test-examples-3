#pragma once

#include "index_iterator.h"
#include "keyinv_fwd.h"

namespace NTestShard {

class TKeyInvIterator : public IIndexIterator {
public:
    TKeyInvIterator(const TString& attribute, TKeySearcher* keySearcher, THitSearcher* hitSearcher);
    bool Read(TAttributeWithDocs* doc) override;

private:
    TString Attribute_;
    TKeyIterator Iterator_;
    size_t SizeLeft_ = 0;
    THitSearcher* HitSearcher_ = nullptr;
};

class TMultiKeyInvIterator : public TKeyInvIterator {
public:
    TMultiKeyInvIterator(const TString& attribute, TKeySearcher* keySearcher, THitSearcher* hitSearcher, ui32 maxAsteriskDocs, ui32 minPrefixLength);

    bool Read(TAttributeWithDocs* doc) override;

private:
    size_t LongestCommonQueryPrefix(const TUtf16String& l, const TUtf16String& r);
    size_t GetQueryStart(const TUtf16String& attr);
    void ResizeToPrefixLength(TUtf16String* str, ui32 prefixLength);

private:
    ui32 MaxAsteriskDocs_ = 0;
    ui32 MinPrefixLength_ = 0;
    ui32 QueryStartPos_ = 0;
    TAttributeWithDocs LastUnread_;
};

}
