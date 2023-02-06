#pragma once

#include "index_iterator.h"

#include <kernel/indexdoc/omnidoc.h>
#include <kernel/doom/offroad_omni_wad/omni_io.h>
#include <kernel/doc_url_index/doc_url_index.h>

namespace NTestShard {

using TDocHash = ui64;
using TIo = NDoom::TOmniUrlIo;
using TAccessor = TDocOmniIndexAccessor<TIo>;

class TOmniUrlIterator {
public:
    TOmniUrlIterator(const TString& shardDir);

protected:
    struct THit {
        TStringBuf Url;
        TDoc Doc;
    };

    bool Read(THit* doc);

    TStringBuf Find(TDoc doc);

private:
    THolder<NDoom::IWad> Wad_;
    TDocOmniWadIndex Reader_;
    THolder<TAccessor> Accessor_;
    TDocUrlIndexManager IndexManager_;
    TDoc DocId_ = 0;
};

class TUrlIterator : public IIndexIterator, public TOmniUrlIterator {
public:
    TUrlIterator(const TString& shardDir);

    bool Read(TAttributeWithDocs* attr) override;
};

class TSiteIterator : public IIndexIterator, public TOmniUrlIterator {
public:
    TSiteIterator(const TString& shardDir);

    bool Read(TAttributeWithDocs* attr) override;

private:
    THashMap<TStringBuf, TVector<TDoc>> Docs;
};

}
