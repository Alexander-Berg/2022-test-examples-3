#pragma once

#include <util/folder/path.h>

namespace NBlobStorage::NProxy::NMock {

struct TResource {
    TString Namespace;
    TFsPath RootPath;
    TFsPath LocalPath;

    TFsPath RealPath() const {
        return RootPath / LocalPath;
    }
};

} // namespace NBlobStorage::NProxy::NMock
