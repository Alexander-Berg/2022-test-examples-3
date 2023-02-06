#include "data.h"

#include <library/cpp/logger/global/global.h>
#include <library/cpp/protobuf/json/proto2json.h>

#include <util/random/random.h>
#include <util/stream/buffer.h>
#include <util/stream/str.h>
#include <util/string/join.h>

TString NDrive::TTestInputBase::GenerateId() const {
    return GenerateId(TaskId, UserId, InputId);
}

bool NDrive::TTestInputBase::Deserialize(const NDrive::NProto::TTestTaskInput& proto) {
    TaskId = proto.GetTaskId();
    UserId = proto.GetUserId();
    InputId = proto.GetInputId();
    Created = TInstant::Seconds(proto.GetCreated());
    if (proto.HasIntegerValue()) {
        Value = proto.GetIntegerValue();
        return true;
    } else {
        return false;
    }
}

void NDrive::TTestInputBase::Serialize(NDrive::NProto::TTestTaskInput& proto) const {
    proto.SetTaskId(TaskId);
    proto.SetUserId(UserId);
    proto.SetInputId(InputId);
    proto.SetCreated(Created.Seconds());
    proto.SetIntegerValue(Value);
}

TString NDrive::TTestInputBase::GenerateId(const TString& taskId, const TString& userId, const TString& inputId) {
    return Join('-', "input", taskId, userId, inputId);
}

THolder<NDrive::TCommonTestData> NDrive::TCommonTestData::Create(const TString& type, const TString& imei, const TString& userId) {
    return THolder(TTestFactory::Construct(type, imei, userId));
}

NDrive::TCommonTestData::TCommonTestData(const TString& type, const TString& imei, const TString& userId, TInstant now)
    : IDistributedData(TDataConstructionContext{ type, GenerateId(imei, type, now) })
    , IMEI(imei)
    , UserId(userId)
    , Status(NDrive::NProto::TTestTaskData::NEW)
    , Created(now)
    , StageChanged(now)
    , StageTimeout(TDuration::Seconds(60))
{
}

TString NDrive::TCommonTestData::GenerateId(const TString& imei, const TString& type, TInstant created) {
    TStringStream ss;
    ss << imei << '-';
    ss << type << '-';
    ss << created.Seconds();
    return ss.Str();
}

TString NDrive::TCommonTestData::GenerateId() const {
    return GenerateId(IMEI, GetType(), Created);
}

NJson::TJsonValue NDrive::TCommonTestData::GetInfo() const {
    NJson::TJsonValue result;
    result["id"] = GenerateId();
    result["message"] = GetMessage();
    result["type"] = GetType();
    result["status"] = NDrive::NProto::TTestTaskData::EStatus_Name(GetStatus());
    return result;
}

NJson::TJsonValue NDrive::TCommonTestData::GetDebugInfo() const {
    NJson::TJsonValue result;
    NDrive::NProto::TTestTaskData proto;
    Serialize(proto);
    NProtobufJson::Proto2Json(proto, result);
    return result;
}

void NDrive::TCommonTestData::AddInput(const TTestInputBase& data) {
    Inputs[data.GetInputId()] = data;
}

void NDrive::TCommonTestData::SetMessage(const TString& message) {
    if (message) {
        Message = message;
    }
}

void NDrive::TCommonTestData::SetStageTimeout(TDuration value) {
    StageTimeout = value;
}

void NDrive::TCommonTestData::SetStatus(NDrive::NProto::TTestTaskData::EStatus value, const TString& message /*= Default<TString>()*/) {
    Status = value;
    SetMessage(message);
}

bool NDrive::TCommonTestData::Timeouted() const {
    if (StageTimeout) {
        return Now() > StageChanged + StageTimeout;
    } else {
        return false;
    }
}

bool NDrive::TCommonTestData::Deserialize(const TBlob& data) {
    NDrive::NProto::TTestTaskData proto;
    if (!proto.ParseFromArray(data.Data(), data.Size())) {
        return false;
    }
    if (!Deserialize(proto)) {
        return false;
    }

    IMEI = proto.GetIMEI();
    UserId = proto.GetUserId();
    Message = proto.GetMessage();
    Status = proto.GetStatus();
    Created = TInstant::Seconds(proto.GetCreated());
    StageChanged = TInstant::Seconds(proto.GetStageChanged());
    StageTimeout = TDuration::Seconds(proto.GetStageTimeout());

    for (auto&& i : proto.GetInput()) {
        TTestInputBase input;
        if (!input.Deserialize(i)) {
            return false;
        }
        AddInput(input);
    }

    return true;
}

TBlob NDrive::TCommonTestData::Serialize() const {
    NDrive::NProto::TTestTaskData proto;
    Serialize(proto);
    proto.SetIMEI(IMEI);
    proto.SetUserId(UserId);
    proto.SetMessage(Message);
    proto.SetStatus(Status);
    proto.SetCreated(Created.Seconds());
    proto.SetStageChanged(StageChanged.Seconds());
    proto.SetStageTimeout(StageTimeout.Seconds());

    for (auto&& i : Inputs) {
        const auto& input = i.second;
        auto p = proto.AddInput();
        CHECK_WITH_LOG(p);
        input.Serialize(*p);
    }

    TBufferOutput stream;
    CHECK_WITH_LOG(proto.SerializeToArcadiaStream(&stream));
    return TBlob::FromBuffer(stream.Buffer());
}

void NDrive::TCommonTestData::OnStageChange() {
    StageChanged = Now();
}

void NDrive::TCommonTestData::Deserialize(TTelematicsApi::THandlers& handle, const ::google::protobuf::RepeatedPtrField<TString>& field) {
    handle.clear();
    for (auto&& value : field) {
        NDrive::TTelematicsApi::THandler h = { value };
        handle.push_back(std::move(h));
    }
}

void NDrive::TCommonTestData::Serialize(const TTelematicsApi::THandlers& handle, ::google::protobuf::RepeatedPtrField<TString>* field) const {
    CHECK_WITH_LOG(field);
    for (auto&& element : handle) {
        *field->Add() = element.Id;
    }
}

bool NDrive::TPingTestData::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = proto.GetPingScenario();
    SetStage(scenario.GetStage());
    PingHandle.Id = scenario.GetPingTask()[0];
    TCommonTestData::Deserialize(StartOfLeaseHandle, scenario.GetStartOfLeaseTask());
    TCommonTestData::Deserialize(UnlockHoodHandle, scenario.GetUnlockHoodTask());
    TCommonTestData::Deserialize(UnlockHoodCheckHandle, scenario.GetUnlockHoodCheckTask());
    UnlockHoodAttempts = scenario.GetUnlockHoodAttempts();
    return true;
}

void NDrive::TPingTestData::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = proto.MutablePingScenario();
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(GetStage());
    scenario->AddPingTask(PingHandle.Id);
    TCommonTestData::Serialize(StartOfLeaseHandle, scenario->MutableStartOfLeaseTask());
    TCommonTestData::Serialize(UnlockHoodHandle, scenario->MutableUnlockHoodTask());
    TCommonTestData::Serialize(UnlockHoodCheckHandle, scenario->MutableUnlockHoodCheckTask());
    scenario->SetUnlockHoodAttempts(UnlockHoodAttempts);
}

void NDrive::TPingTestData::FillInfo(NJson::TJsonValue& result) const {
    {
        result["action"] = "wait";
        switch (GetStage()) {
        case NDrive::NProto::TTestTaskData::TPingScenario::SENDING:
            result["description"] = SendingPingMessage;
            break;
        case NDrive::NProto::TTestTaskData::TPingScenario::RECEIVING:
            result["description"] = ReceivingPingMessage;
            break;
        case NDrive::NProto::TTestTaskData::TPingScenario::START_OF_LEASE:
            result["description"] = UnlockingCarMessage;
            break;
        case NDrive::NProto::TTestTaskData::TPingScenario::UNLOCK_HOOD:
            result["description"] = UnlockingHoodMessage;
            break;
        case NDrive::NProto::TTestTaskData::TPingScenario::UNLOCK_HOOD_CHECK:
            result["description"] = CheckUnlockedHoodMessage;
            break;
        }
    }
}

bool NDrive::TBaseStartEngineTestData::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = proto.GetStartEngineScenario();
    SetStage(scenario.GetStage());
    TCommonTestData::Deserialize(CheckStoppedHandle, scenario.GetCheckStoppedTask());
    TCommonTestData::Deserialize(StartHandle, scenario.GetStartTask());
    TCommonTestData::Deserialize(CheckStartedHandle, scenario.GetCheckStartedTask());
    return true;
}

void NDrive::TBaseStartEngineTestData::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = proto.MutableStartEngineScenario();
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(GetStage());
    TCommonTestData::Serialize(CheckStoppedHandle, scenario->MutableCheckStoppedTask());
    TCommonTestData::Serialize(StartHandle, scenario->MutableStartTask());
    TCommonTestData::Serialize(CheckStartedHandle, scenario->MutableCheckStartedTask());
}

void NDrive::TBaseStartEngineTestData::FillInfo(NJson::TJsonValue& result) const {
    {
        switch (GetStage()) {
        case NDrive::NProto::TTestTaskData::TStartEngineScenario::STOPPED_CHECK:
            result["action"] = "wait";
            result["description"] = CheckStoppedEngineMessage;
            break;
        case NDrive::NProto::TTestTaskData::TStartEngineScenario::STARING:
            result["action"] = "wait";
            result["description"] = StartingEngineMessage;
            break;
        case NDrive::NProto::TTestTaskData::TStartEngineScenario::SELF_CHECK:
            result["action"] = "do";
            result["description"] = StartEngineMessage;
            break;
        }
    }
}

bool NDrive::TStopEngineTestData::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = proto.GetStopEngineScenario();
    SetStage(scenario.GetStage());
    TCommonTestData::Deserialize(CheckStoppedHandle, scenario.GetCheckStoppedTask());
    TCommonTestData::Deserialize(StopHandle, scenario.GetStopTask());
    TCommonTestData::Deserialize(CheckStartedHandle, scenario.GetCheckStartedTask());
    return true;
}

void NDrive::TStopEngineTestData::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = proto.MutableStopEngineScenario();
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(GetStage());
    TCommonTestData::Serialize(CheckStoppedHandle, scenario->MutableCheckStoppedTask());
    TCommonTestData::Serialize(StopHandle, scenario->MutableStopTask());
    TCommonTestData::Serialize(CheckStartedHandle, scenario->MutableCheckStartedTask());
}

void NDrive::TStopEngineTestData::FillInfo(NJson::TJsonValue& result) const {
    {
        switch (GetStage()) {
        case NDrive::NProto::TTestTaskData::TStopEngineScenario::STARTED_CHECK:
            result["action"] = "wait";
            result["description"] = CheckStartedEngineMessage;
            break;
        case NDrive::NProto::TTestTaskData::TStopEngineScenario::STOPPING:
            result["action"] = "wait";
            result["description"] = StoppingEngineMessage;
            break;
        case NDrive::NProto::TTestTaskData::TStopEngineScenario::SELF_CHECK:
            result["action"] = "do";
            result["description"] = StopEngineMessage;
            break;
        }
    }
}

bool NDrive::TBrakesAndRTestData::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = proto.GetBrakesAndRScenario();
    SetStage(scenario.GetStage());
    TCommonTestData::Deserialize(CheckStartedHandle, scenario.GetCheckStartedTask());
    TCommonTestData::Deserialize(CheckPHandle, scenario.GetCheckPTask());
    TCommonTestData::Deserialize(CheckBrakeHandle, scenario.GetCheckBrakeTask());
    TCommonTestData::Deserialize(CheckRHandle, scenario.GetCheckRTask());
    TCommonTestData::Deserialize(CheckP2Handle, scenario.GetCheckP2Task());
    TCommonTestData::Deserialize(CheckUnbrakeHandle, scenario.GetCheckUnbrakeTask());
    return true;
}

void NDrive::TBrakesAndRTestData::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = proto.MutableBrakesAndRScenario();
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(GetStage());
    TCommonTestData::Serialize(CheckStartedHandle, scenario->MutableCheckStartedTask());
    TCommonTestData::Serialize(CheckPHandle, scenario->MutableCheckPTask());
    TCommonTestData::Serialize(CheckBrakeHandle, scenario->MutableCheckBrakeTask());
    TCommonTestData::Serialize(CheckRHandle, scenario->MutableCheckRTask());
    TCommonTestData::Serialize(CheckP2Handle, scenario->MutableCheckP2Task());
    TCommonTestData::Serialize(CheckUnbrakeHandle, scenario->MutableCheckUnbrakeTask());
}

void NDrive::TBrakesAndRTestData::FillInfo(NJson::TJsonValue& result) const {
    {
        switch (GetStage()) {
        case NDrive::NProto::TTestTaskData::TBrakesAndRScenario::STARTED_CHECK:
            result["action"] = "wait";
            result["description"] = CheckStartedEngineMessage;
            break;
        case NDrive::NProto::TTestTaskData::TBrakesAndRScenario::P_CHECK:
            result["action"] = "wait";
            result["description"] = CheckPMessage;
            break;
        case NDrive::NProto::TTestTaskData::TBrakesAndRScenario::BRAKE:
            result["action"] = "do";
            result["description"] = PressBrakePedalMessage;
            break;
        case NDrive::NProto::TTestTaskData::TBrakesAndRScenario::R:
            result["action"] = "do";
            result["description"] = SelectRMessage;
            break;
        case NDrive::NProto::TTestTaskData::TBrakesAndRScenario::P:
            result["action"] = "do";
            result["description"] = SelectPMessage;
            break;
        case NDrive::NProto::TTestTaskData::TBrakesAndRScenario::UNBRAKE:
            result["action"] = "do";
            result["description"] = ReleaseBrakePedalMessage;
            break;
        }
    }
}

template <class T>
bool NDrive::TCountActionTestData<T>::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = GetScenario(proto);
    TBase::SetStage(scenario.GetStage());
    Required = scenario.GetBlinksRequired();
    Done = scenario.GetBlinksDone();
    SubStep = scenario.GetSubStep();
    TCommonTestData::Deserialize(BlinkHandles, scenario.GetBlinkTasks());
    TCommonTestData::Deserialize(FinishedHandles, scenario.GetFinishedTasks());
    return true;
}

template <class T>
void NDrive::TCountActionTestData<T>::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = MutableScenario(proto);
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(TBase::GetStage());
    scenario->SetBlinksRequired(Required);
    scenario->SetBlinksDone(Done);
    scenario->SetSubStep(SubStep);
    TCommonTestData::Serialize(BlinkHandles, scenario->MutableBlinkTasks());
    TCommonTestData::Serialize(FinishedHandles, scenario->MutableFinishedTasks());
}

template <class T>
void NDrive::TCountActionTestData<T>::FillInfo(NJson::TJsonValue& result) const {
    {
        switch (TBase::GetStage()) {
        case NDrive::NProto::TTestTaskData::TBlinkScenario::PREPARE:
            result["action"] = "input";
            result["description"] = GetPrepareDescription();
            result["input"]["id"] = "ready-flag";
            result["input"]["type"] = "bool";
            break;
        case NDrive::NProto::TTestTaskData::TBlinkScenario::BLINK:
            result["action"] = "do";
            result["description"] = GetActionDescription();
            break;
        case NDrive::NProto::TTestTaskData::TBlinkScenario::COUNT:
            result["action"] = "input";
            result["description"] = GetCountDescription();
            result["input"]["id"] = "blinks-count";
            result["input"]["type"] = "integer";
            result["input"]["options"].AppendValue(1);
            result["input"]["options"].AppendValue(2);
            result["input"]["options"].AppendValue(3);
            break;
        }
    }
}

template <class T>
void NDrive::TOnOffTestData<T>::FillInfo(NJson::TJsonValue& result) const {
    switch (TBase::GetStage()) {
    case NDrive::NProto::TTestTaskData::TOnOffScenario::ON:
        result["action"] = "do";
        result["description"] = GetOnDescription();
        break;
    case NDrive::NProto::TTestTaskData::TOnOffScenario::OFF:
        result["action"] = "do";
        result["description"] = GetOffDescription();
        break;
    }
}

template <class T>
bool NDrive::TOnOffTestData<T>::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = GetScenario(proto);
    TBase::SetStage(scenario.GetStage());
    TCommonTestData::Deserialize(OnHandle, scenario.GetOnTask());
    TCommonTestData::Deserialize(OffHandle, scenario.GetOffTask());
    return true;
}

template <class T>
void NDrive::TOnOffTestData<T>::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = MutableScenario(proto);
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(TBase::GetStage());
    TCommonTestData::Serialize(OnHandle, scenario->MutableOnTask());
    TCommonTestData::Serialize(OffHandle, scenario->MutableOffTask());
}

void NDrive::TRadioblockTestData::FillInfo(NJson::TJsonValue& result) const {
    Y_UNUSED(result);
    switch (GetStage()) {
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::STARTED_CHECK:
        result["action"] = "wait";
        result["description"] = CheckStartedEngineMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::ENABLE:
        result["action"] = "wait";
        result["description"] = EnablingRadioblockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::ENABLED_CHECK:
        result["action"] = "wait";
        result["description"] = CheckEnabledRadioblockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::BRAKE:
        result["action"] = "do";
        result["description"] = PressBrakePedalMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::R:
        result["action"] = "do";
        result["description"] = SelectRMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::STOPPED_CHECK:
        result["action"] = "wait";
        result["description"] = CheckStoppedEngineMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::P:
        result["action"] = "do";
        result["description"] = SelectPMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::UNBRAKE:
        result["action"] = "do";
        result["description"] = ReleaseBrakePedalMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::DISABLE:
        result["action"] = "wait";
        result["description"] = DisablingRadioblockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::DISABLED_CHECK:
        result["action"] = "wait";
        result["description"] = CheckDisabledRadioblockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::ROLLBACK_DISABLE:
        result["action"] = "wait";
        result["description"] = RollbackMessage + DisablingRadioblockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TRadioblockScenario::ROLLBACK_DISABLED_CHECK:
        result["action"] = "wait";
        result["description"] = RollbackMessage + CheckDisabledRadioblockMessage;
        break;
    default:
        break;
    }
}

bool NDrive::TRadioblockTestData::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = proto.GetRadioblockScenario();
    TBase::SetStage(scenario.GetStage());
    TCommonTestData::Deserialize(StartedHandle, scenario.GetStartedTask());
    TCommonTestData::Deserialize(EnableHandle, scenario.GetEnableTask());
    TCommonTestData::Deserialize(EnabledHandle, scenario.GetEnabledTask());
    TCommonTestData::Deserialize(BrakeHandle, scenario.GetBrakeTask());
    TCommonTestData::Deserialize(RHandle, scenario.GetRTask());
    TCommonTestData::Deserialize(StoppedHandle, scenario.GetStoppedTask());
    TCommonTestData::Deserialize(PHandle, scenario.GetPTask());
    TCommonTestData::Deserialize(UnbrakeHandle, scenario.GetUnbrakeTask());
    TCommonTestData::Deserialize(DisableHandle, scenario.GetDisableTask());
    TCommonTestData::Deserialize(DisableHandle, scenario.GetDisabledTask());
    return true;
}

void NDrive::TRadioblockTestData::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = proto.MutableRadioblockScenario();
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(TBase::GetStage());
    TCommonTestData::Serialize(StartedHandle, scenario->MutableStartedTask());
    TCommonTestData::Serialize(EnableHandle, scenario->MutableEnableTask());
    TCommonTestData::Serialize(EnabledHandle, scenario->MutableEnabledTask());
    TCommonTestData::Serialize(BrakeHandle, scenario->MutableBrakeTask());
    TCommonTestData::Serialize(RHandle, scenario->MutableRTask());
    TCommonTestData::Serialize(StoppedHandle, scenario->MutableStoppedTask());
    TCommonTestData::Serialize(PHandle, scenario->MutablePTask());
    TCommonTestData::Serialize(UnbrakeHandle, scenario->MutableUnbrakeTask());
    TCommonTestData::Serialize(DisableHandle, scenario->MutableDisableTask());
    TCommonTestData::Serialize(DisableHandle, scenario->MutableDisabledTask());
}

void NDrive::TMoveblockTestData::FillInfo(NJson::TJsonValue& result) const {
    switch (GetStage()) {
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::STOPPED_CHECK_INITIAL:
        result["action"] = "wait";
        result["description"] = CheckStoppedEngineMessage;
        break;
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::ENABLE:
        result["action"] = "wait";
        result["description"] = EnablingMoveblockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::ENABLED_CHECK:
        result["action"] = "wait";
        result["description"] = CheckEnabledMoveblockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::START:
        result["action"] = "wait";
        result["description"] = StartingEngineMessage;
        break;
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::STARTED_CHECK:
        result["action"] = "do";
        result["description"] = StartEngineMessage;
        break;
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::SMASH:
        result["action"] = "input";
        result["description"] = SmashMessage;
        result["input"]["id"] = "smashed-flag";
        result["input"]["type"] = "bool";
        break;
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::STOPPED_CHECK:
        result["action"] = "wait";
        result["description"] = CheckStoppedEngineMessage;
        break;
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::DISABLE:
        result["action"] = "wait";
        result["description"] = DisablingMoveblockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::DISABLED_CHECK:
        result["action"] = "wait";
        result["description"] = CheckDisabledMoveblockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::ROLLBACK_DISABLE:
        result["action"] = "wait";
        result["description"] = RollbackMessage + DisablingMoveblockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TMoveblockScenario::ROLLBACK_DISABLED_CHECK:
        result["action"] = "wait";
        result["description"] = RollbackMessage + CheckDisabledMoveblockMessage;
        break;
    default:
        break;
    }
}

bool NDrive::TMoveblockTestData::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = proto.GetMoveblockScenario();
    TBase::SetStage(scenario.GetStage());
    TCommonTestData::Deserialize(StoppedInitialHandle, scenario.GetStoppedInitialTask());
    TCommonTestData::Deserialize(EnableHandle, scenario.GetEnableTask());
    TCommonTestData::Deserialize(EnabledHandle, scenario.GetEnabledTask());
    TCommonTestData::Deserialize(StartHandle, scenario.GetStartTask());
    TCommonTestData::Deserialize(StartedHandle, scenario.GetStartedTask());
    TCommonTestData::Deserialize(StoppedHandle, scenario.GetStoppedTask());
    TCommonTestData::Deserialize(DisableHandle, scenario.GetDisableTask());
    TCommonTestData::Deserialize(DisabledHandle, scenario.GetDisabledTask());
    return true;
}

void NDrive::TMoveblockTestData::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = proto.MutableMoveblockScenario();
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(TBase::GetStage());
    TCommonTestData::Serialize(StoppedInitialHandle, scenario->MutableStoppedInitialTask());
    TCommonTestData::Serialize(EnableHandle, scenario->MutableEnableTask());
    TCommonTestData::Serialize(EnabledHandle, scenario->MutableEnabledTask());
    TCommonTestData::Serialize(StartHandle, scenario->MutableStartTask());
    TCommonTestData::Serialize(StartedHandle, scenario->MutableStartedTask());
    TCommonTestData::Serialize(StoppedHandle, scenario->MutableStoppedTask());
    TCommonTestData::Serialize(DisableHandle, scenario->MutableDisableTask());
    TCommonTestData::Serialize(DisabledHandle, scenario->MutableDisabledTask());
}

void NDrive::TWarmupTestData::FillInfo(NJson::TJsonValue& result) const {
    switch (GetStage()) {
    case NDrive::NProto::TTestTaskData::TWarmupScenario::PREPARE:
        result["action"] = "input";
        result["description"] = WarmupPrepareMessage;
        result["input"]["id"] = "prepared-flag";
        result["input"]["type"] = "bool";
        break;
    case NDrive::NProto::TTestTaskData::TWarmupScenario::START_WARMUP:
        result["action"] = "wait";
        result["description"] = WarmupStartMessage;
        break;
    case NDrive::NProto::TTestTaskData::TWarmupScenario::STARTED_CHECK:
        result["action"] = "do";
        result["description"] = CheckStartedEngineMessage;
        break;
    case NDrive::NProto::TTestTaskData::TWarmupScenario::STOP_WARMUP:
        result["action"] = "wait";
        result["description"] = WarmupStopMessage;
        break;
    case NDrive::NProto::TTestTaskData::TWarmupScenario::STOPPED_CHECK:
        result["action"] = "wait";
        result["description"] = CheckStoppedEngineMessage;
        break;
    }
}

bool NDrive::TWarmupTestData::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = proto.GetWarmupScenario();
    TBase::SetStage(scenario.GetStage());
    TCommonTestData::Deserialize(StartWarmupHandle, scenario.GetStartWarmupTask());
    TCommonTestData::Deserialize(StartedHandle, scenario.GetStartedTask());
    TCommonTestData::Deserialize(StopWarmupHandle, scenario.GetStopWarmupTask());
    TCommonTestData::Deserialize(StoppedHandle, scenario.GetStoppedTask());
    return true;
}

void NDrive::TWarmupTestData::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = proto.MutableWarmupScenario();
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(TBase::GetStage());
    TCommonTestData::Serialize(StartWarmupHandle, scenario->MutableStartWarmupTask());
    TCommonTestData::Serialize(StartedHandle, scenario->MutableStartedTask());
    TCommonTestData::Serialize(StopWarmupHandle, scenario->MutableStopWarmupTask());
    TCommonTestData::Serialize(StoppedHandle, scenario->MutableStoppedTask());
}

void NDrive::TWindowsTestData::FillInfo(NJson::TJsonValue& result) const {
    switch (GetStage()) {
    case NDrive::NProto::TTestTaskData::TWindowsScenario::OPEN_WINDOWS:
        result["action"] = "wait";
        result["description"] = WindowsOpeningMessage;
        break;
    case NDrive::NProto::TTestTaskData::TWindowsScenario::OPEN_CHECK:
        result["action"] = "input";
        result["description"] = WindowsOpenMessage;
        result["input"]["id"] = GetOpenInput();
        result["input"]["type"] = "bool";
        break;
    case NDrive::NProto::TTestTaskData::TWindowsScenario::CLOSE_WINDOWS:
        result["action"] = "wait";
        result["description"] = WindowsClosingMessage;
        break;
    case NDrive::NProto::TTestTaskData::TWindowsScenario::CLOSE_CHECK:
        result["action"] = "input";
        result["description"] = WindowsCloseCheckMessage;
        result["input"]["id"] = GetCloseInput();
        result["input"]["type"] = "bool";
        break;
    }
}

bool NDrive::TWindowsTestData::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = proto.GetWindowsScenario();
    TBase::SetStage(scenario.GetStage());
    TCommonTestData::Deserialize(OpenHandler, scenario.GetOpenTask());
    TCommonTestData::Deserialize(CloseHandler, scenario.GetCloseTask());
    OpenAttempts = scenario.GetOpenAttempts();
    CloseAttempts = scenario.GetCloseAttempts();
    return true;
}

void NDrive::TWindowsTestData::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = proto.MutableWindowsScenario();
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(TBase::GetStage());
    TCommonTestData::Serialize(OpenHandler, scenario->MutableOpenTask());
    TCommonTestData::Serialize(CloseHandler, scenario->MutableCloseTask());
    scenario->SetOpenAttempts(OpenAttempts);
    scenario->SetCloseAttempts(CloseAttempts);
}

void NDrive::TLockDoorTestData::FillInfo(NJson::TJsonValue& result) const {
    switch (GetStage()) {
    case NDrive::NProto::TTestTaskData::TDoorLockScenario::PREPARE:
        result["action"] = "input";
        result["description"] = LockDoorPrepareMessage;
        result["input"]["id"] = GetPrepareInput();
        result["input"]["type"] = "bool";
        break;
    case NDrive::NProto::TTestTaskData::TDoorLockScenario::LOCK:
        result["action"] = "wait";
        result["description"] = LockDoorLockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TDoorLockScenario::LOCK_CHECK:
        result["action"] = "input";
        result["description"] = LockDoorLockCheckMessage;
        result["input"]["id"] = GetLockCheckInput();
        result["input"]["type"] = "bool";
        break;
    case NDrive::NProto::TTestTaskData::TDoorLockScenario::UNLOCK:
        result["action"] = "wait";
        result["description"] = LockDoorUnlockMessage;
        break;
    case NDrive::NProto::TTestTaskData::TDoorLockScenario::UNLOCK_CHECK:
        result["action"] = "input";
        result["description"] = LockDoorUnlockCheckMessage;
        result["input"]["id"] = GetUnlockCheckInput();
        result["input"]["type"] = "bool";
        break;
    }
}

bool NDrive::TLockDoorTestData::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = proto.GetDoorLockScenario();
    TBase::SetStage(scenario.GetStage());
    TCommonTestData::Deserialize(LockHandle, scenario.GetLockTask());
    TCommonTestData::Deserialize(UnlockHandle, scenario.GetUnlockTask());
    return true;
}

void NDrive::TLockDoorTestData::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = proto.MutableDoorLockScenario();
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(TBase::GetStage());
    TCommonTestData::Serialize(LockHandle, scenario->MutableLockTask());
    TCommonTestData::Serialize(UnlockHandle, scenario->MutableUnlockTask());
}

void NDrive::TFinishTestData::FillInfo(NJson::TJsonValue& result) const {
    switch (GetStage()) {
    case NDrive::NProto::TTestTaskData::TFinishScenario::UNLOCK:
        result["action"] = "wait";
        result["description"] = FinishUnlockMessage;
        break;
    }
}

bool NDrive::TFinishTestData::Deserialize(const NDrive::NProto::TTestTaskData& proto) {
    const auto& scenario = proto.GetFinishScenario();
    TBase::SetStage(scenario.GetStage());
    TCommonTestData::Deserialize(UnlockHandle, scenario.GetUnlockTask());
    return true;
}

void NDrive::TFinishTestData::Serialize(NDrive::NProto::TTestTaskData& proto) const {
    auto scenario = proto.MutableFinishScenario();
    CHECK_WITH_LOG(scenario);
    scenario->SetStage(TBase::GetStage());
    TCommonTestData::Serialize(UnlockHandle, scenario->MutableUnlockTask());
}

template <class T>
class TRegisterData {
private:
    IDistributedData::TFactory::TRegistrator<T> DistributedData{ T::Name() };
    NDrive::TCommonTestData::TTestFactory::TRegistrator<T> CommonTestData{ T::Name() };
};

TRegisterData<NDrive::TPingTestData> PingTestData;
TRegisterData<NDrive::TStartEngineTestData> StartEngineTestData;
TRegisterData<NDrive::TStopEngineTestData> StopEngineTestData;
TRegisterData<NDrive::TStartEngineAgainTestData> StartEngineTestDataAgain;
TRegisterData<NDrive::TBrakesAndRTestData> BrakesAndRTestData;
TRegisterData<NDrive::TRadioblockTestData> RadioblockTestData;
TRegisterData<NDrive::TMoveblockTestData> MoveblockTestData;
TRegisterData<NDrive::TBlinkTestData> BlinkTestData;
TRegisterData<NDrive::THornTestData> HornTestData;
TRegisterData<NDrive::THoodLockTestData> HoodLockTestData;
TRegisterData<NDrive::TDippedBeamTestData> DippedBeamTestData;
TRegisterData<NDrive::THighBeamTestData> HighBeamTestData;
TRegisterData<NDrive::THandbrakeTestData> HandbrakeTestData;
TRegisterData<NDrive::TDriverDoorTestData> DriverDoorTestData;
TRegisterData<NDrive::TPassengerDoorTestData> PassengerDoorTestData;
TRegisterData<NDrive::TLeftRearDoorTestData> LeftRearDoorTestData;
TRegisterData<NDrive::TRightRearDoorTestData> RightRearDoorTestData;
TRegisterData<NDrive::THoodTestData> HoodTestData;
TRegisterData<NDrive::TTrunkTestData> TrunkTestData;
TRegisterData<NDrive::TWarmupTestData> WarmupTestData;
TRegisterData<NDrive::TWindowsTestData> WindowsTestData;
TRegisterData<NDrive::TLockDoorTestData> LockDoorTestData;
TRegisterData<NDrive::TFinishTestData> FinishTestData;
