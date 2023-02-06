#pragma once

#include <crypta/styx/services/common/data/proto/puid_state.pb.h>

#include <util/datetime/base.h>
#include <util/generic/vector.h>

namespace NCrypta::NStyx::NTest {
    TOblivionEvent CreateOblivionEvent(TInstant timestamp, const TString& obfuscated);
    TPuidState GetPuidState(ui64 puid, const TVector<TOblivionEvent>& oblivionEvents);
    TVector<TOblivionEvent> OblivionEventsToVector(const ::google::protobuf::RepeatedPtrField<NCrypta::NStyx::TOblivionEvent>& oblivionEvents);
}
