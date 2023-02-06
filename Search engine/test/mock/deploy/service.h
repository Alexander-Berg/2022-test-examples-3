#pragma once

#include "resource.h"
#include "status.h"

#include <util/generic/array_ref.h>

namespace NBlobStorage::NProxy::NMock {

class IService {
public:
    virtual ~IService() = default;

    virtual ui16 Port() const = 0;
    virtual ui16 AdminPort() const = 0;
    virtual TFsPath Storage() const = 0;

    virtual void Rescan(TConstArrayRef<TResource> resources, TArrayRef<TStatus> statuses) = 0;
    virtual void Stop() = 0;
    virtual bool Ping();

public:
    void WaitUntilReady();
};

} // namespace NBlobStorage::NProxy::NMock
