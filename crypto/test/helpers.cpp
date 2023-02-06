#include "helpers.h"

namespace NCrypta::NStyx::NTest {
    TDbState GetEmptyDbState(TDuration minDeleteInterval) {
        TDbState::TPuidStates emptyPuidStates;
        return TDbState(std::move(emptyPuidStates), minDeleteInterval);
    }

    NCrypta::NStyx::TRepeatedOblivionEvents GetOblivionEvents(const TVector<NCrypta::NStyx::TOblivionEvent>& events) {
        NCrypta::NStyx::TRepeatedOblivionEvents result;
        for (const auto& event : events) {
            *result.Add() = event;
        }
        return result;
    }
}
