#pragma once

#include <util/generic/string.h>

namespace NBlobStorage::NProxy::NMock {

struct TStatus {
    bool Valid = true;
    TString Message;
};

} // namespace NBlobStorage::NProxy::NMock
