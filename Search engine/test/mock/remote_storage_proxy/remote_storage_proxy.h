#pragma once

#include <library/cpp/testing/common/network.h>

#include <search/base_search/daemons/rs_proxy/daemon/daemon.h>
#include <search/base_search/rs_proxy/test/mock/deploy/service.h>

namespace NBlobStorage::NProxy::NMock {

struct TRemoteStorageProxyOptions {
    NTesting::TPortHolder Port = NTesting::GetFreePort();
    NTesting::TPortHolder AdminPort = NTesting::GetFreePort();

    TFsPath Root;
    NBlobStorage::NProxy::TConfig Config;
};

class TRemoteStorageProxy : public IService {
public:
    TRemoteStorageProxy(TRemoteStorageProxyOptions options);
    ~TRemoteStorageProxy();

    void Rescan(TConstArrayRef<TResource> resources, TArrayRef<TStatus> statuses) override;
    void Stop() override;

    ui16 Port() const override;
    ui16 AdminPort() const override;
    TFsPath Storage() const override;

private:
    class TImpl;
    THolder<TImpl> Impl_;
};

} // namespace NBlobStorage::NProxy::NMock
