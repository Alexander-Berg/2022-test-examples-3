#include "task.h"

#include <drive/backend/abstract/frontend.h>

#include <util/stream/buffer.h>

#define VEGA_HOOD_LOCKED NVega::DigitalInput<1>()
#define VEGA_RADIOBLOCK NVega::DigitalOutput<10>()
#define VEGA_MOVEBLOCK NVega::DigitalOutput<4>()

bool NDrive::TTestInputTask::DoExecute(IDistributedTask::TPtr self) noexcept {
    Y_UNUSED(self);
    const auto d = GetData();
    CHECK_WITH_LOG(d);
    auto& data = d->GetDataAs<TCommonTestData>();

    data.AddInput(*this);
    d->Store2();
    return true;
}

bool NDrive::TTestInputTask::Deserialize(const TBlob& data) {
    NDrive::NProto::TTestTaskInput proto;
    if (!proto.ParseFromArray(data.Data(), data.Size())) {
        return false;
    }
    return TTestInputBase::Deserialize(proto);
}

TBlob NDrive::TTestInputTask::Serialize() const {
    NDrive::NProto::TTestTaskInput proto;
    TTestInputBase::Serialize(proto);

    TBufferOutput stream;
    CHECK_WITH_LOG(proto.SerializeToArcadiaStream(&stream));
    return TBlob::FromBuffer(stream.Buffer());
}

THolder<NDrive::TCommonTestTask> NDrive::TCommonTestTask::Create(const NDrive::TCommonTestData& data) {
    return THolder(TTestFactory::Construct(data.GetType(), data));
}

bool NDrive::TCommonTestTask::DoExecute(IDistributedTask::TPtr self) noexcept {
    const auto d = GetData();
    CHECK_WITH_LOG(d);

    auto& data = d->GetDataAs<TCommonTestData>();
    switch (data.GetStatus()) {
    case NDrive::NProto::TTestTaskData::NEW:
        data.SetStatus(NDrive::NProto::TTestTaskData::IN_PROGRESS);
        if (!d->Store2()) {
            return true;
        }
        // fallthrough
    case NDrive::NProto::TTestTaskData::IN_PROGRESS:
        if (data.Timeouted()) {
            return Rollback(d, self, "Timeouted");
        }
        break;
    default:
        return true;
    }

    try {
        return DoDoExecute(d, self);
    } catch (const std::exception& e) {
        return Rollback(d, self, FormatExc(e));
    }
}

bool NDrive::TCommonTestTask::Rollback(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr /*self*/, const TString& message) {
    auto& data = d->GetDataAs<TCommonTestData>();
    return Fail(d, data, message);
}

bool NDrive::TCommonTestTask::AwaitCommand(const TString& imei, TTelematicsApi::THandlers& handle, NVega::ECommandCode code) const {
    const auto& ctx = Executor->GetContextAs<NDrive::IServer>();
    const auto& api = ctx.GetTelematicsApi();

    const ui32 retries = 3;
    const auto request = [&] {
        return api.Command(imei, code);
    };
    return api.Await(handle, retries, request);
}

bool NDrive::TCommonTestTask::AwaitSet(const TString& imei, TTelematicsApi::THandlers& handle, ui16 id, ui64 value) const {
    const auto& ctx = Executor->GetContextAs<NDrive::IServer>();
    const auto& api = ctx.GetTelematicsApi();

    const ui32 retries = 3;
    const auto request = [&] {
        return api.SetParameter(imei, id, 0, value);
    };
    return api.Await(handle, retries, request);
}

bool NDrive::TCommonTestTask::AwaitValue(const TString& imei, NDrive::TTelematicsApi::THandlers& handle, ui16 id, ui64 value) const {
    const auto& ctx = Executor->GetContextAs<NDrive::IServer>();
    const auto& api = ctx.GetTelematicsApi();

    const ui32 retries = 30;
    const auto request = [&] {
        return api.GetParameter(imei, id);
    };
    const auto checker = [&] (const TTelematicsApi::THandler& handler) {
        return api.GetValue<ui64>(handler) == value;
    };
    return api.Await(handle, retries, request, checker);
}

bool NDrive::TCommonTestTask::Fail(IDDataStorage::TGuard::TPtr d, TCommonTestData& data, const TString& message) const {
    data.SetStatus(NDrive::NProto::TTestTaskData::FAILURE, message);
    data.SetFinished(Now());
    d->Store2();
    return true;
}

bool NDrive::TCommonTestTask::Finish(IDDataStorage::TGuard::TPtr d, TCommonTestData& data) const {
    data.SetStatus(NDrive::NProto::TTestTaskData::SUCCESS);
    data.SetFinished(Now());
    d->Store2();
    return true;
}

bool NDrive::TCommonTestTask::Reschedule(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self, bool unlock) const {
    CHECK_WITH_LOG(d);
    CHECK_WITH_LOG(self);
    d->Store2();
    Executor->RescheduleTask(self.Get(), Now() + TDuration::Seconds(1), unlock);
    return false;
}

bool NDrive::TPingTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    const auto& ctx = Executor->GetContextAs<NDrive::IServer>();
    auto& data = d->GetDataAs<TPingTestData>();
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TPingScenario::SENDING) {
        data.PingHandle = ctx.GetTelematicsApi().Ping(data.GetIMEI());
        data.SetStage(NDrive::NProto::TTestTaskData::TPingScenario::RECEIVING);
        return Reschedule(d, self);
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TPingScenario::RECEIVING) {
        if (ctx.GetTelematicsApi().Await(data.PingHandle)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TPingScenario::START_OF_LEASE);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TPingScenario::START_OF_LEASE) {
        if (AwaitCommand(data.GetIMEI(), data.StartOfLeaseHandle, NVega::ECommandCode::YADRIVE_START_OF_LEASE)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TPingScenario::UNLOCK_HOOD);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TPingScenario::UNLOCK_HOOD) {
        for (; data.UnlockHoodAttempts < data.UnlockHoodTotal; ++data.UnlockHoodAttempts) {
            if (AwaitCommand(data.GetIMEI(), data.UnlockHoodHandle, NVega::ECommandCode::YADRIVE_UNLOCK_HOOD)) {
                data.UnlockHoodHandle.clear();
            } else {
                return Reschedule(d, self);
            }
        }
        data.SetStage(NDrive::NProto::TTestTaskData::TPingScenario::UNLOCK_HOOD_CHECK);
        // fallthrough
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TPingScenario::UNLOCK_HOOD_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.UnlockHoodCheckHandle, VEGA_HOOD_LOCKED, 0)) {
            return Finish(d, data);
        } else {
            return Reschedule(d, self);
        }
    }
    ythrow yexception() << "unexpected status";
}

bool NDrive::TStartEngineTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<TBaseStartEngineTestData>();
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TStartEngineScenario::STOPPED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.CheckStoppedHandle, CAN_ENGINE_IS_ON, 0)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TStartEngineScenario::SELF_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TStartEngineScenario::STARING) {
        if (AwaitCommand(data.GetIMEI(), data.StartHandle, NVega::ECommandCode::START_ENGINE)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TStartEngineScenario::SELF_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TStartEngineScenario::SELF_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.CheckStartedHandle, CAN_ENGINE_IS_ON, 1)) {
            return Finish(d, data);
        } else {
            return Reschedule(d, self);
        }
    }
    ythrow yexception() << "unexpected stage";
}

bool NDrive::TStopEngineTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<TStopEngineTestData>();
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TStopEngineScenario::STARTED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.CheckStartedHandle, CAN_ENGINE_IS_ON, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TStopEngineScenario::SELF_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TStopEngineScenario::STOPPING) {
        if (AwaitCommand(data.GetIMEI(), data.StopHandle, NVega::ECommandCode::STOP_ENGINE)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TStopEngineScenario::SELF_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TStopEngineScenario::SELF_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.CheckStoppedHandle, CAN_ENGINE_IS_ON, 0)) {
            return Finish(d, data);
        } else {
            return Reschedule(d, self);
        }
    }
    ythrow yexception() << "unexpected stage";
}

bool NDrive::TBrakesAndRTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<TBrakesAndRTestData>();
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBrakesAndRScenario::STARTED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.CheckStartedHandle, CAN_ENGINE_IS_ON, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TBrakesAndRScenario::P_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBrakesAndRScenario::P_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.CheckPHandle, CAN_PARKING, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TBrakesAndRScenario::BRAKE);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBrakesAndRScenario::BRAKE) {
        if (AwaitValue(data.GetIMEI(), data.CheckBrakeHandle, CAN_PEDAL_BREAK, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TBrakesAndRScenario::R);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBrakesAndRScenario::R) {
        if (AwaitValue(data.GetIMEI(), data.CheckRHandle, CAN_REVERSE_GEAR, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TBrakesAndRScenario::P);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBrakesAndRScenario::P) {
        if (AwaitValue(data.GetIMEI(), data.CheckP2Handle, CAN_PARKING, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TBrakesAndRScenario::UNBRAKE);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBrakesAndRScenario::UNBRAKE) {
        if (AwaitValue(data.GetIMEI(), data.CheckUnbrakeHandle, CAN_PEDAL_BREAK, 0)) {
            return Finish(d, data);
        } else {
            return Reschedule(d, self);
        }
    }
    ythrow yexception() << "unexpected stage";
}

template <class T>
bool NDrive::TCountActionTestTask<T>::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<T>();
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBlinkScenario::PREPARE) {
        auto input = data.GetInput("ready-flag");
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue()) {
                data.SetStage(NDrive::NProto::TTestTaskData::TBlinkScenario::BLINK);
                // fallthrough
            } else {
                return Fail(d, data, YouAreNotPreparedMessage);
            }
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBlinkScenario::BLINK) {
        for (; data.Done < data.Required; ++data.Done) {
            if (AwaitCommand(data.GetIMEI(), data.BlinkHandles, GetCode())) {
                for (auto&& i : data.BlinkHandles) {
                    data.FinishedHandles.push_back(std::move(i));
                }
                data.BlinkHandles.clear();
            } else {
                return Reschedule(d, self);
            }
        }
        data.SetStage(NDrive::NProto::TTestTaskData::TBlinkScenario::COUNT);
        // fallthrough
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBlinkScenario::COUNT) {
        auto input = data.GetInput("blinks-count");
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue() == data.Required) {
                return Finish(d, data);
            } else {
                return Fail(d, data, Sprintf(IncorrectNumberMessage.c_str(), data.Required, input->GetValue()));
            }
        }
    }
    ythrow yexception() << "unexpected stage";
}

bool NDrive::THoodLockTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<THoodLockTestData>();
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBlinkScenario::PREPARE) {
        auto input = data.GetInput("ready-flag");
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue()) {
                data.SetStage(NDrive::NProto::TTestTaskData::TBlinkScenario::BLINK);
                // fallthrough
            } else {
                return Fail(d, data, YouAreNotPreparedMessage);
            }
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBlinkScenario::BLINK) {
        for (; data.Done < data.Required; ++data.Done) {
            if (data.SubStep == 0) {
                if (AwaitCommand(data.GetIMEI(), data.BlinkHandles, NVega::ECommandCode::YADRIVE_LOCK_HOOD)) {
                    for (auto&& i : data.BlinkHandles) {
                        data.FinishedHandles.push_back(std::move(i));
                    }
                    data.BlinkHandles.clear();
                    data.SubStep = 1;
                } else {
                    return Reschedule(d, self);
                }
            }
            if (data.SubStep == 1) {
                if (AwaitCommand(data.GetIMEI(), data.BlinkHandles, NVega::ECommandCode::YADRIVE_UNLOCK_HOOD)) {
                    for (auto&& i : data.BlinkHandles) {
                        data.FinishedHandles.push_back(std::move(i));
                    }
                    data.BlinkHandles.clear();
                    data.SubStep = 0;
                } else {
                    return Reschedule(d, self);
                }
            }
        }
        data.SetStage(NDrive::NProto::TTestTaskData::TBlinkScenario::COUNT);
        // fallthrough
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TBlinkScenario::COUNT) {
        auto input = data.GetInput("blinks-count");
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue() == data.Required) {
                return Finish(d, data);
            } else {
                return Fail(d, data, Sprintf(IncorrectNumberMessage.c_str(), data.Required, input->GetValue()));
            }
        }
    }
    ythrow yexception() << "unexpected stage";
}

template <class T>
bool NDrive::TOnOffTestTask<T>::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<T>();
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TOnOffScenario::ON) {
        if (AwaitValue(data.GetIMEI(), data.OnHandle, GetSensor(), 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TOnOffScenario::OFF);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TOnOffScenario::OFF) {
        if (AwaitValue(data.GetIMEI(), data.OnHandle, GetSensor(), 0)) {
            return Finish(d, data);
        } else {
            return Reschedule(d, self);
        }
    }
    ythrow yexception() << "unexpected stage";
}

bool NDrive::TRadioblockTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<TRadioblockTestData>();
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::STARTED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.StartedHandle, CAN_ENGINE_IS_ON, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::ENABLE);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::ENABLE) {
        if (AwaitSet(data.GetIMEI(), data.EnableHandle, VEGA_RADIOBLOCK, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::ENABLED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::ENABLED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.EnabledHandle, VEGA_RADIOBLOCK, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::BRAKE);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::BRAKE) {
        if (AwaitValue(data.GetIMEI(), data.BrakeHandle, CAN_PEDAL_BREAK, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::R);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::R) {
        if (AwaitValue(data.GetIMEI(), data.RHandle, CAN_REVERSE_GEAR, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::STOPPED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::STOPPED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.StoppedHandle, CAN_ENGINE_IS_ON, 0)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::P);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::P) {
        if (AwaitValue(data.GetIMEI(), data.PHandle, CAN_PARKING, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::UNBRAKE);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::UNBRAKE) {
        if (AwaitValue(data.GetIMEI(), data.UnbrakeHandle, CAN_PEDAL_BREAK, 0)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::DISABLE);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::DISABLE) {
        if (AwaitSet(data.GetIMEI(), data.DisableHandle, VEGA_RADIOBLOCK, 0)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::DISABLED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::DISABLED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.DisabledHandle, VEGA_RADIOBLOCK, 0)) {
            return Finish(d, data);
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::ROLLBACK_DISABLE) {
        if (AwaitSet(data.GetIMEI(), data.DisableHandle, VEGA_RADIOBLOCK, 0)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::ROLLBACK_DISABLED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TRadioblockScenario::ROLLBACK_DISABLED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.DisabledHandle, VEGA_RADIOBLOCK, 0)) {
            return Fail(d, data, "rolled back: " + data.GetMessage());
        } else {
            return Reschedule(d, self);
        }
    }
    ythrow yexception() << "unexpected stage";
}

bool NDrive::TRadioblockTestTask::Rollback(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self, const TString& message) {
    auto& data = d->GetDataAs<TRadioblockTestData>();
    switch (data.GetStage()) {
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::BRAKE:
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::R:
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::STOPPED_CHECK:
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::P:
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::UNBRAKE:
        data.SetMessage(message);
        data.SetStage(NDrive::NProto::TTestTaskData::TRadioblockScenario::ROLLBACK_DISABLE);
        return Reschedule(d, self);
    default:
        return Fail(d, data, message);
    }
}

bool NDrive::TMoveblockTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<TMoveblockTestData>();
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::STOPPED_CHECK_INITIAL) {
        if (AwaitValue(data.GetIMEI(), data.StoppedInitialHandle, CAN_ENGINE_IS_ON, 0)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TMoveblockScenario::ENABLE);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::ENABLE) {
        if (AwaitSet(data.GetIMEI(), data.EnableHandle, VEGA_MOVEBLOCK, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TMoveblockScenario::ENABLED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::ENABLED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.EnabledHandle, VEGA_MOVEBLOCK, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TMoveblockScenario::STARTED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::START) {
        if (AwaitCommand(data.GetIMEI(), data.StartHandle, NVega::ECommandCode::START_ENGINE)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TMoveblockScenario::STARTED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::STARTED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.StartedHandle, CAN_ENGINE_IS_ON, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TMoveblockScenario::SMASH);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::SMASH) {
        auto input = data.GetInput("smashed-flag");
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue()) {
                data.SetStage(NDrive::NProto::TTestTaskData::TMoveblockScenario::STOPPED_CHECK);
                // fallthrough
            } else {
                return Fail(d, data, YouAreNotPreparedMessage);
            }
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::STOPPED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.StoppedHandle, CAN_ENGINE_IS_ON, 0)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TMoveblockScenario::DISABLE);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::DISABLE) {
        if (AwaitSet(data.GetIMEI(), data.DisableHandle, VEGA_MOVEBLOCK, 0)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TMoveblockScenario::DISABLED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::DISABLED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.DisabledHandle, VEGA_MOVEBLOCK, 0)) {
            return Finish(d, data);
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::ROLLBACK_DISABLE) {
        if (AwaitSet(data.GetIMEI(), data.DisableHandle, VEGA_MOVEBLOCK, 0)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TMoveblockScenario::ROLLBACK_DISABLED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    }
    if (data.GetStage() == NDrive::NProto::TTestTaskData::TMoveblockScenario::ROLLBACK_DISABLED_CHECK) {
        if (AwaitValue(data.GetIMEI(), data.DisabledHandle, VEGA_MOVEBLOCK, 0)) {
            return Fail(d, data, "rolled back: " + data.GetMessage());
        } else {
            return Reschedule(d, self);
        }
    }
    ythrow yexception() << "unexpected stage";
}

bool NDrive::TMoveblockTestTask::Rollback(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self, const TString& message) {
    auto& data = d->GetDataAs<TMoveblockTestData>();
    switch (data.GetStage()) {
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::START:
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::STARTED_CHECK:
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::SMASH:
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::STOPPED_CHECK:
        data.SetMessage(message);
        data.SetStage(NDrive::NProto::TTestTaskData::TMoveblockScenario::ROLLBACK_DISABLE);
        return Reschedule(d, self);
    default:
        return Fail(d, data, message);
    }
}

bool NDrive::TWarmupTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<TWarmupTestData>();
    switch (data.GetStage()) {
    case NDrive::NProto::TTestTaskData::TWarmupScenario::PREPARE: {
        auto input = data.GetInput("prepared-flag");
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue()) {
                data.SetStage(NDrive::NProto::TTestTaskData::TWarmupScenario::START_WARMUP);
                // fallthrough
            } else {
                return Fail(d, data, YouAreNotPreparedMessage);
            }
        }
    }
    case NDrive::NProto::TTestTaskData::TWarmupScenario::START_WARMUP:
        if (AwaitCommand(data.GetIMEI(), data.StartWarmupHandle, NVega::ECommandCode::YADRIVE_WARMING)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TWarmupScenario::STARTED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    case NDrive::NProto::TTestTaskData::TWarmupScenario::STARTED_CHECK:
        if (AwaitValue(data.GetIMEI(), data.StartedHandle, CAN_ENGINE_IS_ON, 1)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TWarmupScenario::STOP_WARMUP);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    case NDrive::NProto::TTestTaskData::TWarmupScenario::STOP_WARMUP:
        if (AwaitCommand(data.GetIMEI(), data.StopWarmupHandle, NVega::ECommandCode::YADRIVE_STOP_WARMING)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TWarmupScenario::STOPPED_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    case NDrive::NProto::TTestTaskData::TWarmupScenario::STOPPED_CHECK:
        if (AwaitValue(data.GetIMEI(), data.StartedHandle, CAN_ENGINE_IS_ON, 0)) {
            return Finish(d, data);
        } else {
            return Reschedule(d, self);
        }
    }
}

bool NDrive::TWindowsTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<TWindowsTestData>();
    switch (data.GetStage()) {
    case NDrive::NProto::TTestTaskData::TWindowsScenario::OPEN_WINDOWS:
        if (AwaitCommand(data.GetIMEI(), data.OpenHandler, NVega::ECommandCode::WINDOWS_OPENING_29S)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TWindowsScenario::OPEN_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    case NDrive::NProto::TTestTaskData::TWindowsScenario::OPEN_CHECK: {
        auto input = data.GetInput(data.GetOpenInput());
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue()) {
                data.SetStage(NDrive::NProto::TTestTaskData::TWindowsScenario::CLOSE_WINDOWS);
                // fallthrough
            } else {
                if (data.OpenAttempts < data.MaxAttempts) {
                    data.SetStage(NDrive::NProto::TTestTaskData::TWindowsScenario::OPEN_CHECK);
                    data.OpenAttempts++;
                    return Reschedule(d, self);
                } else {
                    return Fail(d, data, YouAreNotPreparedMessage);
                }
            }
        }
    }
    case NDrive::NProto::TTestTaskData::TWindowsScenario::CLOSE_WINDOWS:
        if (AwaitCommand(data.GetIMEI(), data.CloseHandler, NVega::ECommandCode::WINDOWS_CLOSING_29S)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TWindowsScenario::CLOSE_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    case NDrive::NProto::TTestTaskData::TWindowsScenario::CLOSE_CHECK: {
        auto input = data.GetInput(data.GetCloseInput());
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue()) {
                return Finish(d, data);
            } else {
                if (data.CloseAttempts < data.MaxAttempts) {
                    data.SetStage(NDrive::NProto::TTestTaskData::TWindowsScenario::CLOSE_WINDOWS);
                    data.CloseAttempts++;
                    return Reschedule(d, self);
                } else {
                    return Fail(d, data, YouAreNotPreparedMessage);
                }
            }
        }
    }
    }
}

bool NDrive::TLockDoorTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<TLockDoorTestData>();
    switch (data.GetStage()) {
    case NDrive::NProto::TTestTaskData::TDoorLockScenario::PREPARE: {
        auto input = data.GetInput(data.GetPrepareInput());
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue()) {
                data.SetStage(NDrive::NProto::TTestTaskData::TDoorLockScenario::LOCK);
                // fallthrough
            } else {
                return Fail(d, data, YouAreNotPreparedMessage);
            }
        }
    }
    case NDrive::NProto::TTestTaskData::TDoorLockScenario::LOCK:
        if (AwaitCommand(data.GetIMEI(), data.LockHandle, NVega::ECommandCode::CLOSE_DOORS)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TDoorLockScenario::LOCK_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    case NDrive::NProto::TTestTaskData::TDoorLockScenario::LOCK_CHECK: {
        auto input = data.GetInput(data.GetLockCheckInput());
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue()) {
                data.SetStage(NDrive::NProto::TTestTaskData::TDoorLockScenario::UNLOCK);
                // fallthrough
            } else {
                return Fail(d, data, LockDoorLockFailedMessage);
            }
        }
    }
    case NDrive::NProto::TTestTaskData::TDoorLockScenario::UNLOCK:
        if (AwaitCommand(data.GetIMEI(), data.UnlockHandle, NVega::ECommandCode::OPEN_DOORS)) {
            data.SetStage(NDrive::NProto::TTestTaskData::TDoorLockScenario::UNLOCK_CHECK);
            // fallthrough
        } else {
            return Reschedule(d, self);
        }
    case NDrive::NProto::TTestTaskData::TDoorLockScenario::UNLOCK_CHECK: {
        auto input = data.GetInput(data.GetUnlockCheckInput());
        if (!input) {
            return Reschedule(d, self, /*unlock=*/true);
        } else {
            if (input->GetValue()) {
                return Finish(d, data);
            } else {
                return Fail(d, data, LockDoorUnlockFailedMessage);
            }
        }
    }
    }
}

bool NDrive::TFinishTestTask::DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) {
    auto& data = d->GetDataAs<TFinishTestData>();
    switch (data.GetStage()) {
    case NDrive::NProto::TTestTaskData::TFinishScenario::UNLOCK:
        if (AwaitCommand(data.GetIMEI(), data.UnlockHandle, NVega::ECommandCode::YADRIVE_START_OF_LEASE)) {
            return Finish(d, data);
        } else {
            return Reschedule(d, self);
        }
    }
}

template <class T>
struct TRegisterTask {
    IDistributedTask::TFactory::TRegistrator<T> DistributedTask;
    NDrive::TCommonTestTask::TTestFactory::TRegistrator<T> CommonTestTask;

    TRegisterTask(const TString& name)
        : DistributedTask(name)
        , CommonTestTask(name)
    {
    }
};

IDistributedTask::TFactory::TRegistrator<NDrive::TTestInputTask> TestInputTask("TestInputData");

TRegisterTask<NDrive::TPingTestTask> PingTest("TestPing");
TRegisterTask<NDrive::THornTestTask> HornTest("TestHorn");
TRegisterTask<NDrive::TBlinkTestTask> BlinkTest("TestBlink");
TRegisterTask<NDrive::TStartEngineTestTask> StartEngineTest("TestStartEngine");
TRegisterTask<NDrive::TWindowsTestTask> WindowsTest("TestWindows");
TRegisterTask<NDrive::TBrakesAndRTestTask> BrakesAndRTest("TestBrakesAndR");
//TRegisterTask<NDrive::TRadioblockTestTask> RadioblockTest("TestRadioblock");
//TRegisterTask<NDrive::TMoveblockTestTask> MoveblockTest("TestMoveblock");
TRegisterTask<NDrive::TStartEngineTestTask> StartEngineAgainTest("TestStartEngineAgain");

TRegisterTask<NDrive::TDippedBeamTestTask> DippedBeamTest("TestDippedBeam");
TRegisterTask<NDrive::THighBeamTestTask> HighBeamTest("TestHighBeam");
TRegisterTask<NDrive::THandbrakeTestTask> HandbrakeTest("TestHandbrake");

TRegisterTask<NDrive::TDriverDoorTestTask> DriverDoorTest("TestDriverDoor");
TRegisterTask<NDrive::TLeftRearDoorTestTask> LeftRearDoorTest("TestLeftRearDoor");
TRegisterTask<NDrive::TTrunkTestTask> TrunkTest("TestTrunk");
TRegisterTask<NDrive::TRightRearDoorTestTask> RightRearDoorTest("TestRightRearDoor");
TRegisterTask<NDrive::TPassengerDoorTestTask> PassengerDoorTest("TestPassengerDoor");
TRegisterTask<NDrive::THoodTestTask> HoodTest("TestHood");
TRegisterTask<NDrive::THoodLockTestTask> HoodLockTest("TestHoodLock");

TRegisterTask<NDrive::TStopEngineTestTask> StopEngineTest("TestStopEngine");
TRegisterTask<NDrive::TWarmupTestTask> WarmupTest("TestWarmup");
TRegisterTask<NDrive::TLockDoorTestTask> LockDoorTest("TestLockDoor");
TRegisterTask<NDrive::TFinishTestTask> FinishTest("TestFinish");
