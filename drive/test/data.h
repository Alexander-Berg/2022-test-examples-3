#pragma once

#include "lables.h"

#include <drive/backend/proto/drive.pb.h>

#include <drive/telematics/api/client.h>

#include <rtline/library/executor/abstract/data.h>
#include <rtline/library/executor/abstract/queue.h>

#include <util/random/random.h>

namespace NDrive {
    class TTestInputBase {
    public:
        TTestInputBase() = default;
        TTestInputBase(const TString& taskId, const TString& userId, const TString& inputId, i64 value)
            : TaskId(taskId)
            , UserId(userId)
            , InputId(inputId)
            , Value(value)
            , Created(Now())
        {
        }

        const TString& GetTaskId() const {
            return TaskId;
        }
        const TString& GetUserId() const {
            return UserId;
        }
        const TString& GetInputId() const {
            return InputId;
        }
        i64 GetValue() const {
            return Value;
        }

        TString GenerateId() const;
        static TString GenerateId(const TString& taskId, const TString& userId, const TString& inputId);

        bool Deserialize(const NDrive::NProto::TTestTaskInput& proto);
        void Serialize(NDrive::NProto::TTestTaskInput& proto) const;

    protected:
        TString TaskId;
        TString UserId;
        TString InputId;
        i64 Value = 0;

        TInstant Created;
    };

    class TCommonTestData: public IDistributedData {
    public:
        using TTestFactory = NObjectFactory::TParametrizedObjectFactory<TCommonTestData, TString, const TString&, const TString&>;

    public:
        using IDistributedData::IDistributedData;
        TCommonTestData(const TString& type, const TString& imei, const TString& userId, TInstant now);

        TString GenerateId() const;
        static TString GenerateId(const TString& imei, const TString& type, TInstant created);

        const TString& GetIMEI() const {
            return IMEI;
        }
        const TString& GetUserId() const {
            return UserId;
        }
        const TString& GetMessage() const {
            return Message;
        }
        TInstant GetCreatedTime() const {
            return Created;
        }
        NDrive::NProto::TTestTaskData::EStatus GetStatus() const {
            return Status;
        }
        const TTestInputBase* GetInput(const TString& id) const {
            return Inputs.FindPtr(id);
        }

        void AddInput(const TTestInputBase& data);
        void SetMessage(const TString& message);
        void SetStageTimeout(TDuration value);
        void SetStatus(NDrive::NProto::TTestTaskData::EStatus value, const TString& message = Default<TString>());
        bool Timeouted() const;

        NJson::TJsonValue GetDebugInfo() const;
        virtual NJson::TJsonValue GetInfo() const;

        virtual bool Deserialize(const TBlob& data) override;
        virtual TBlob Serialize() const override;

    public:
        static THolder<TCommonTestData> Create(const TString& type, const TString& imei, const TString& userId);

    protected:
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) = 0;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const = 0;

        void Deserialize(TTelematicsApi::THandlers& handle, const ::google::protobuf::RepeatedPtrField<TString>& field);
        void Serialize(const TTelematicsApi::THandlers& handle, ::google::protobuf::RepeatedPtrField<TString>* field) const;

        void OnStageChange();

    private:
        TString IMEI;
        TString UserId;

        TMap<TString, TTestInputBase> Inputs;
        NDrive::NProto::TTestTaskData::EStatus Status;
        TString Message;

        TInstant Created;
        TInstant StageChanged;
        TDuration StageTimeout;
    };

    template <class S, class T>
    class TStagedTestData: public TCommonTestData {
    public:
        using TStage = typename S::EStage;

    public:
        using TCommonTestData::TCommonTestData;
        TStagedTestData(const TString& name, const TString& imei, const TString& userId)
            : TCommonTestData(name, imei, userId, Now())
            , Stage(static_cast<TStage>(0))
        {
        }
        TStagedTestData(const TString& imei, const TString& userId)
            : TStagedTestData(T::Name(), imei, userId)
        {
        }

        TStage GetStage() const {
            return Stage;
        }
        void SetStage(TStage value) {
            Stage = value;
            OnStageChange();
        }

        virtual NJson::TJsonValue GetInfo() const final {
            auto result = TCommonTestData::GetInfo();
            result["stage"] = S::EStage_Name(GetStage());
            if (GetStatus() == NDrive::NProto::TTestTaskData::NEW || GetStatus() == NDrive::NProto::TTestTaskData::IN_PROGRESS) {
                FillInfo(result);
            } else {
                result["action"] = NJson::JSON_NULL;
                result["description"] = NJson::JSON_NULL;
            }
            return result;
        }

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const = 0;

    private:
        TStage Stage;
    };

    class TPingTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TPingScenario, TPingTestData> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TPingScenario, TPingTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestPing";
        }

    public:
        TTelematicsApi::THandler PingHandle;
        TTelematicsApi::THandlers StartOfLeaseHandle;
        TTelematicsApi::THandlers UnlockHoodHandle;
        TTelematicsApi::THandlers UnlockHoodCheckHandle;
        ui32 UnlockHoodTotal = 3;
        ui32 UnlockHoodAttempts = 0;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;
    };

    class TBaseStartEngineTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TStartEngineScenario, TBaseStartEngineTestData> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TStartEngineScenario, TBaseStartEngineTestData>;

    public:
        using TBase::TBase;

    public:
        TTelematicsApi::THandlers CheckStoppedHandle;
        TTelematicsApi::THandlers StartHandle;
        TTelematicsApi::THandlers CheckStartedHandle;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;
    };

    class TStartEngineTestData: public TBaseStartEngineTestData {
    public:
        TStartEngineTestData(const TDataConstructionContext& context)
            : TBaseStartEngineTestData(context)
        {
        }
        TStartEngineTestData(const TString& imei, const TString& userId)
            : TBaseStartEngineTestData(Name(), imei, userId)
        {
        }

        static TString Name() {
            return "TestStartEngine";
        }
    };

    class TStartEngineAgainTestData: public TBaseStartEngineTestData {
    public:
        TStartEngineAgainTestData(const TDataConstructionContext& context)
            : TBaseStartEngineTestData(context)
        {
        }
        TStartEngineAgainTestData(const TString& imei, const TString& userId)
            : TBaseStartEngineTestData(Name(), imei, userId)
        {
        }

        static TString Name() {
            return "TestStartEngineAgain";
        }
    };

    class TStopEngineTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TStopEngineScenario, TStopEngineTestData> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TStopEngineScenario, TStopEngineTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestStopEngine";
        }

    public:
        TTelematicsApi::THandlers CheckStartedHandle;
        TTelematicsApi::THandlers StopHandle;
        TTelematicsApi::THandlers CheckStoppedHandle;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;
    };

    class TBrakesAndRTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TBrakesAndRScenario, TBrakesAndRTestData> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TBrakesAndRScenario, TBrakesAndRTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestBrakesAndR";
        }

    public:
        TTelematicsApi::THandlers CheckStartedHandle;
        TTelematicsApi::THandlers CheckPHandle;
        TTelematicsApi::THandlers CheckBrakeHandle;
        TTelematicsApi::THandlers CheckRHandle;
        TTelematicsApi::THandlers CheckP2Handle;
        TTelematicsApi::THandlers CheckUnbrakeHandle;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;
    };

    template <class T>
    class TCountActionTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TBlinkScenario, T> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TBlinkScenario, T>;

    public:
        using TBase::TBase;

    public:
        TTelematicsApi::THandlers BlinkHandles;
        TTelematicsApi::THandlers FinishedHandles;
        ui32 SubStep = 0;
        ui32 Required = 1 + RandomNumber<ui32>() % 3;
        ui32 Done = 0;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;

        virtual TString GetActionDescription() const = 0;
        virtual TString GetCountDescription() const = 0;
        virtual TString GetPrepareDescription() const = 0;
        virtual const NDrive::NProto::TTestTaskData::TBlinkScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const = 0;
        virtual NDrive::NProto::TTestTaskData::TBlinkScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const = 0;
    };

    template <class T>
    class TOnOffTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TOnOffScenario, T> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TOnOffScenario, T>;

    public:
        using TBase::TBase;

    public:
        TTelematicsApi::THandlers OnHandle;
        TTelematicsApi::THandlers OffHandle;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;

        virtual TString GetOnDescription() const = 0;
        virtual TString GetOffDescription() const = 0;
        virtual const NDrive::NProto::TTestTaskData::TOnOffScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const = 0;
        virtual NDrive::NProto::TTestTaskData::TOnOffScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const = 0;
    };

    class TBlinkTestData: public TCountActionTestData<TBlinkTestData> {
    private:
        using TBase = TCountActionTestData<TBlinkTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestBlink";
        }
        TString GetActionDescription() const override {
            return CountBlinksMessage;
        }
        TString GetCountDescription() const override {
            return EnterBlinksMessage;
        }
        TString GetPrepareDescription() const override {
            return ReadyBlinksMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TBlinkScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetBlinkScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TBlinkScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableBlinkScenario();
        }
    };

    class THornTestData: public TCountActionTestData<THornTestData> {
    private:
        using TBase = TCountActionTestData<THornTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestHorn";
        }
        TString GetActionDescription() const override {
            return CountHornsMessage;
        }
        TString GetCountDescription() const override {
            return EnterHornsMessage;
        }
        TString GetPrepareDescription() const override {
            return ReadyHornsMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TBlinkScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetHornScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TBlinkScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableHornScenario();
        }
    };

    class THoodLockTestData: public TCountActionTestData<THoodLockTestData> {
    private:
        using TBase = TCountActionTestData<THoodLockTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestHoodLock";
        }
        TString GetActionDescription() const override {
            return CountHoodLockingsMessage;
        }
        TString GetCountDescription() const override {
            return EnterHoodLockingsMessage;
        }
        TString GetPrepareDescription() const override {
            return ReadyHoodLockingsMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TBlinkScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetHoodLockScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TBlinkScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableHoodLockScenario();
        }
    };

    class TDippedBeamTestData: public TOnOffTestData<TDippedBeamTestData> {
    private:
        using TBase = TOnOffTestData<TDippedBeamTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestDippedBeam";
        }
        virtual TString GetOnDescription() const override {
            return DippedBeamOnMessage;
        }
        virtual TString GetOffDescription() const override {
            return DippedBeamOffMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TOnOffScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetDippedBeamScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TOnOffScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableDippedBeamScenario();
        }
    };

    class THighBeamTestData: public TOnOffTestData<THighBeamTestData> {
    private:
        using TBase = TOnOffTestData<THighBeamTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestHighBeam";
        }
        virtual TString GetOnDescription() const override {
            return HighBeamOnMessage;
        }
        virtual TString GetOffDescription() const override {
            return HighBeamOffMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TOnOffScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetHighBeamScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TOnOffScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableHighBeamScenario();
        }
    };

    class THandbrakeTestData: public TOnOffTestData<THandbrakeTestData> {
    private:
        using TBase = TOnOffTestData<THandbrakeTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestHandbrake";
        }
        virtual TString GetOnDescription() const override {
            return HandbrakeOnMessage;
        }
        virtual TString GetOffDescription() const override {
            return HandbrakeOffMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TOnOffScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetHandbrakeScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TOnOffScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableHandbrakeScenario();
        }
    };

    class TDriverDoorTestData: public TOnOffTestData<TDriverDoorTestData> {
    private:
        using TBase = TOnOffTestData<TDriverDoorTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestDriverDoor";
        }
        virtual TString GetOnDescription() const override {
            return DriverDoorOnMessage;
        }
        virtual TString GetOffDescription() const override {
            return DriverDoorOffMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TOnOffScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetDriverDoorScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TOnOffScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableDriverDoorScenario();
        }
    };

    class TPassengerDoorTestData: public TOnOffTestData<TPassengerDoorTestData> {
    private:
        using TBase = TOnOffTestData<TPassengerDoorTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestPassengerDoor";
        }
        virtual TString GetOnDescription() const override {
            return PassengerDoorOnMessage;
        }
        virtual TString GetOffDescription() const override {
            return PassengerDoorOffMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TOnOffScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetPassengerDoorScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TOnOffScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutablePassengerDoorScenario();
        }
    };

    class TLeftRearDoorTestData: public TOnOffTestData<TLeftRearDoorTestData> {
    private:
        using TBase = TOnOffTestData<TLeftRearDoorTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestLeftRearDoor";
        }
        virtual TString GetOnDescription() const override {
            return LeftRearDoorOnMessage;
        }
        virtual TString GetOffDescription() const override {
            return LeftRearDoorOffMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TOnOffScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetLeftRearDoorScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TOnOffScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableLeftRearDoorScenario();
        }
    };

    class TRightRearDoorTestData: public TOnOffTestData<TRightRearDoorTestData> {
    private:
        using TBase = TOnOffTestData<TRightRearDoorTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestRightRearDoor";
        }
        virtual TString GetOnDescription() const override {
            return RightRearDoorOnMessage;
        }
        virtual TString GetOffDescription() const override {
            return RightRearDoorOffMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TOnOffScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetRightRearDoorScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TOnOffScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableRightRearDoorScenario();
        }
    };

    class THoodTestData: public TOnOffTestData<THoodTestData> {
    private:
        using TBase = TOnOffTestData<THoodTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestHood";
        }
        virtual TString GetOnDescription() const override {
            return HoodOnMessage;
        }
        virtual TString GetOffDescription() const override {
            return HoodOffMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TOnOffScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetHoodScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TOnOffScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableHoodScenario();
        }
    };

    class TTrunkTestData: public TOnOffTestData<TTrunkTestData> {
    private:
        using TBase = TOnOffTestData<TTrunkTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestTrunk";
        }
        virtual TString GetOnDescription() const override {
            return TrunkOnMessage;
        }
        virtual TString GetOffDescription() const override {
            return TrunkOffMessage;
        }
        virtual const NDrive::NProto::TTestTaskData::TOnOffScenario& GetScenario(const NDrive::NProto::TTestTaskData& proto) const override {
            return proto.GetTrunkScenario();
        }
        virtual NDrive::NProto::TTestTaskData::TOnOffScenario* MutableScenario(NDrive::NProto::TTestTaskData& proto) const override {
            return proto.MutableTrunkScenario();
        }
    };

    class TRadioblockTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TRadioblockScenario, TRadioblockTestData> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TRadioblockScenario, TRadioblockTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestRadioblock";
        }

    public:
        TTelematicsApi::THandlers StartedHandle;
        TTelematicsApi::THandlers EnableHandle;
        TTelematicsApi::THandlers EnabledHandle;
        TTelematicsApi::THandlers BrakeHandle;
        TTelematicsApi::THandlers RHandle;
        TTelematicsApi::THandlers StoppedHandle;
        TTelematicsApi::THandlers PHandle;
        TTelematicsApi::THandlers UnbrakeHandle;
        TTelematicsApi::THandlers DisableHandle;
        TTelematicsApi::THandlers DisabledHandle;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;
    };

    class TMoveblockTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TMoveblockScenario, TMoveblockTestData> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TMoveblockScenario, TMoveblockTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestMoveblock";
        }

    public:
        TTelematicsApi::THandlers StoppedInitialHandle;
        TTelematicsApi::THandlers EnableHandle;
        TTelematicsApi::THandlers EnabledHandle;
        TTelematicsApi::THandlers StartHandle;
        TTelematicsApi::THandlers StartedHandle;
        TTelematicsApi::THandlers StoppedHandle;
        TTelematicsApi::THandlers DisableHandle;
        TTelematicsApi::THandlers DisabledHandle;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;
    };

    class TWarmupTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TWarmupScenario, TWarmupTestData> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TWarmupScenario, TWarmupTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestWarmup";
        }

    public:
        TTelematicsApi::THandlers StartWarmupHandle;
        TTelematicsApi::THandlers StartedHandle;
        TTelematicsApi::THandlers StopWarmupHandle;
        TTelematicsApi::THandlers StoppedHandle;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;
    };

    class TWindowsTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TWindowsScenario, TWindowsTestData> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TWindowsScenario, TWindowsTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestWindows";
        }
        TString GetOpenInput() const {
            return "open-check-flag-" + ToString(OpenAttempts);
        }
        TString GetCloseInput() const {
            return "close-check-flag-" + ToString(OpenAttempts);
        }

    public:
        TTelematicsApi::THandlers OpenHandler;
        TTelematicsApi::THandlers CloseHandler;
        ui32 OpenAttempts = 0;
        ui32 CloseAttempts = 0;
        const ui32 MaxAttempts = 3;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;
    };

    class TLockDoorTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TDoorLockScenario, TLockDoorTestData> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TDoorLockScenario, TLockDoorTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestLockDoor";
        }
        static TString GetPrepareInput() {
            return "prepare-flag";
        }
        static TString GetLockCheckInput() {
            return "lock-check-flag";
        }
        static TString GetUnlockCheckInput() {
            return "unlock-check-flag";
        }

    public:
        TTelematicsApi::THandlers LockHandle;
        TTelematicsApi::THandlers UnlockHandle;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;
    };

    class TFinishTestData: public TStagedTestData<NDrive::NProto::TTestTaskData::TFinishScenario, TFinishTestData> {
    private:
        using TBase = TStagedTestData<NDrive::NProto::TTestTaskData::TFinishScenario, TFinishTestData>;

    public:
        using TBase::TBase;

        static TString Name() {
            return "TestFinish";
        }

    public:
        TTelematicsApi::THandlers UnlockHandle;

    protected:
        virtual void FillInfo(NJson::TJsonValue& result) const override;
        virtual bool Deserialize(const NDrive::NProto::TTestTaskData& proto) override;
        virtual void Serialize(NDrive::NProto::TTestTaskData& proto) const override;
    };
}
