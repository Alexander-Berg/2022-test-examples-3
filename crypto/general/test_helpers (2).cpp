#include "test_helpers.h"

using namespace NCrypta::NStyx;

TOblivionEvent NTest::CreateOblivionEvent(TInstant timestamp, const TString& obfuscated) {
    TOblivionEvent result;
    result.SetUnixtime(timestamp.Seconds());
    result.SetObfuscated(obfuscated);
    return result;
}

TPuidState NTest::GetPuidState(ui64 puid, const TVector<TOblivionEvent>& oblivionEvents) {
    TPuidState puidState;
    puidState.SetPuid(puid);

    for (const auto& event : oblivionEvents) {
        *(puidState.AddOblivionEvents()) = event;
    }

    return puidState;
}

TVector<TOblivionEvent> NTest::OblivionEventsToVector(const ::google::protobuf::RepeatedPtrField<NCrypta::NStyx::TOblivionEvent>& oblivionEvents) {
    TVector<TOblivionEvent> result(oblivionEvents.begin(), oblivionEvents.end());
    return result;
}
