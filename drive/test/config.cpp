#include "config.h"

#include <drive/backend/database/drive_api.h>

TRTBackgroundTester::TFactory::TRegistrator<TRTBackgroundTester> TRTBackgroundTester::Registrator(TRTBackgroundTester::GetTypeName());
TRTBackgroundTesterState::TFactory::TRegistrator<TRTBackgroundTesterState> TRTBackgroundTesterState::Registrator(TRTBackgroundTester::GetTypeName());

bool TRTBackgroundTester::Process(IMessage* message) {
    TRTBackgroundMessageIncoming* messIn = dynamic_cast<TRTBackgroundMessageIncoming*>(message);
    if (messIn) {
        messIn->AddPingValue(PingValue);
        return true;
    }
    return false;
}

TExpectedState TRTBackgroundTester::DoExecute(TAtomicSharedPtr<IRTBackgroundProcessState> state, const TExecutionContext& /*context*/) const {
    const TRTBackgroundTesterState* stateTester = dynamic_cast<const TRTBackgroundTesterState*>(state.Get());
    SendGlobalMessage<TRTBackgroundMessageOut>(stateTester ? stateTester->GetValue() : 0);
    THolder<TRTBackgroundTesterState> nextState(new TRTBackgroundTesterState());
    nextState->SetValue(stateTester ? (stateTester->GetValue() + PingValue) : 0);
    return nextState;
}

TString TRTBackgroundTesterState::GetType() const {
    return TRTBackgroundTester::GetTypeName();
}
