#pragma once

#include <search/base_search/daemons/rs_proxy/daemon.h>
#include <search/base_search/rs_proxy/test/mock/deploy/service.h>

namespace NBlobStorage::NProxy::NTest {

class TRemoteStorageProxyWrapper : public NMock::IService {
public:
    void Start();
    void Stop();

private:
};

} // namespace NBlobStorage::NProxy::NTest
