#pragma once

#include "inverse_index.h"

#include <util/folder/path.h>
#include <util/generic/fwd.h>

namespace NTestShard {

class TIndexCache {
public:
    TIndexCache(const TString& dir, ui32 docs, ui32 uniqueId);

    void Add(const TString& name, const TSingleAttribute& attr);
    TMaybe<TSingleAttribute> Get(const TString& name);

private:
    bool Exists(const TString& name);
    void Serialize(IOutputStream* os, const TSingleAttribute& attr);
    TSingleAttribute Load(IInputStream* is);

    TFsPath GetPath(const TString& name) const;

private:
    TFsPath Dir_;
    ui32 Docs_ = 0;
    ui32 UniqueId_ = 0;
    bool Disabled_ = false;
};

}
