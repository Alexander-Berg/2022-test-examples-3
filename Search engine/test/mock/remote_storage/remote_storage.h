#pragma once

#include <library/cpp/testing/common/network.h>

#include <search/base_search/rs_proxy/test/mock/deploy/service.h>

#include <util/folder/path.h>
#include <util/stream/file.h>

namespace NBlobStorage::NProxy::NMock {

struct TRemoteStorageOptions {
    NTesting::TPortHolder Port = NTesting::GetFreePort();

    TFsPath Root;
    TString ChunkConfName = "chunk.conf";
};

struct TMetrics {
    ui64 NumRequests = 0;

    ui64 FailedLoads = 0;
    ui64 SuccessLoads = 0;
};

class TRemoteStorage : public IService {
public:
    TRemoteStorage(TRemoteStorageOptions options);
    ~TRemoteStorage();

    TRemoteStorage(TRemoteStorage&& rhs) noexcept;
    TRemoteStorage& operator=(TRemoteStorage&& rhs) noexcept;

    void Rescan(TConstArrayRef<TResource> resources, TArrayRef<TStatus> statuses) override;
    void Stop() override;

    ui16 Port() const override;
    ui16 AdminPort() const override;
    TFsPath Storage() const override;

    TMetrics Metrics() const;
    void ResetMetrics() const;

private:
    class TImpl;
    THolder<TImpl> Impl_;
};

} // namespace NBlobStorage::NProxy::NMock

