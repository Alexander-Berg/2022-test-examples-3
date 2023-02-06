#pragma once

#include <util/system/types.h>
#include <util/datetime/base.h>

namespace NBlobStorage::NProxy::NMock {

bool PingPort(ui16 port);

void WaitPort(ui16 port);
bool WaitPort(ui16 port, TDuration timeout);

} // namespace NBlobStorage::NProxy::NMock
