#include "service.h"

#include <search/base_search/rs_proxy/test/mock/common/ping.h>

#include <util/datetime/base.h>

namespace NBlobStorage::NProxy::NMock {

bool IService::Ping() {
    return PingPort(Port());
}

void IService::WaitUntilReady() {
    WaitPort(AdminPort());
    WaitPort(Port());
}

}
