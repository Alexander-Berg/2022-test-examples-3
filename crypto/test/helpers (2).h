#pragma once

#include <crypta/styx/services/common/db_state/db_state.h>
#include <crypta/styx/services/common/data/proto_comparators.h>

#include <util/generic/vector.h>

namespace NCrypta::NStyx::NTest {
    TDbState GetEmptyDbState(TDuration minDeleteInterval);
    NCrypta::NStyx::TRepeatedOblivionEvents GetOblivionEvents(const TVector<NCrypta::NStyx::TOblivionEvent>& vector);
}
