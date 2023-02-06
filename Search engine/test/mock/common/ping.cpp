#include "ping.h"

#include <library/cpp/coroutine/engine/helper.h>

#include <util/network/sock.h>

namespace NBlobStorage::NProxy::NMock {

bool PingPort(ui16 port) {
    return NCoro::TryConnect("localhost", port);
}

void WaitPort(ui16 port) {
    WaitPort(port, TDuration::Max());
}

bool WaitPort(ui16 port, TDuration timeout) {
    return NCoro::WaitUntilConnectable("localhost", port, timeout);
}
} // namespace NBlobStorage::NProxy::NMock
