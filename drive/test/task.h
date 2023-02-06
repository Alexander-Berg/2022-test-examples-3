#pragma once

#include "data.h"

#include <drive/telematics/api/client.h>

#include <rtline/library/executor/abstract/task.h>

namespace NDrive {
    class TTestInputTask
        : public IDistributedTask
        , public TTestInputBase
    {
    private:
        using TBase = IDistributedTask;

    public:
        using TBase::TBase;
        TTestInputTask(const TTestInputBase& data)
            : TBase(TTaskConstructionContext{ "TestInputData", data.GenerateId() })
            , TTestInputBase(data)
        {
        }

        virtual bool DoExecute(IDistributedTask::TPtr self) noexcept override;

        virtual bool Deserialize(const TBlob& data) override;
        virtual TBlob Serialize() const override;
    };

    class TCommonTestTask: public IDistributedTask {
    public:
        using TTestFactory = NObjectFactory::TParametrizedObjectFactory<TCommonTestTask, TString, const TCommonTestData&>;

    private:
        using TBase = IDistributedTask;

    public:
        using TBase::TBase;
        TCommonTestTask(const TCommonTestData& data)
            : TBase(TTaskConstructionContext{ data.GetType(), data.GenerateId() })
        {
        }

        virtual bool DoExecute(IDistributedTask::TPtr self) noexcept override;

    public:
        static THolder<TCommonTestTask> Create(const NDrive::TCommonTestData& data);

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) = 0;
        virtual bool Rollback(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self, const TString& message);

        bool AwaitCommand(const TString& imei, TTelematicsApi::THandlers& handle, NVega::ECommandCode code) const;
        bool AwaitSet(const TString& imei, TTelematicsApi::THandlers& handle, ui16 id, ui64 value) const;
        bool AwaitValue(const TString& imei, TTelematicsApi::THandlers& handle, ui16 id, ui64 value) const;

        bool Fail(IDDataStorage::TGuard::TPtr d, TCommonTestData& data, const TString& message) const;
        bool Finish(IDDataStorage::TGuard::TPtr d, TCommonTestData& data) const;
        bool Reschedule(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self, bool unlock = false) const;
    };

    class TPingTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
    };

    class TStartEngineTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
    };

    class TStopEngineTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
    };

    class TBrakesAndRTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
    };

    template <class T>
    class TCountActionTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
        virtual NVega::ECommandCode GetCode() const = 0;
    };

    template <class T>
    class TOnOffTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
        virtual ui16 GetSensor() const = 0;
    };

    class TBlinkTestTask: public TCountActionTestTask<TBlinkTestData> {
    public:
        using TCountActionTestTask::TCountActionTestTask;

    protected:
        virtual NVega::ECommandCode GetCode() const override {
            return NVega::ECommandCode::HORN_AND_BLINK;
        }
    };

    class THornTestTask: public TCountActionTestTask<THornTestData> {
    public:
        using TCountActionTestTask::TCountActionTestTask;

    protected:
        virtual NVega::ECommandCode GetCode() const override {
            return NVega::ECommandCode::HORN;
        }
    };

    class THoodLockTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
    };

    class TDippedBeamTestTask: public TOnOffTestTask<TDippedBeamTestData> {
    public:
        using TOnOffTestTask::TOnOffTestTask;

    protected:
        virtual ui16 GetSensor() const override {
            return CAN_DIPPED_BEAM;
        }
    };

    class THighBeamTestTask: public TOnOffTestTask<THighBeamTestData> {
    public:
        using TOnOffTestTask::TOnOffTestTask;

    protected:
        virtual ui16 GetSensor() const override {
            return CAN_HIGH_BEAM;
        }
    };

    class THandbrakeTestTask: public TOnOffTestTask<THandbrakeTestData> {
    public:
        using TOnOffTestTask::TOnOffTestTask;

    protected:
        virtual ui16 GetSensor() const override {
            return CAN_HAND_BREAK;
        }
    };

    class TDriverDoorTestTask: public TOnOffTestTask<TDriverDoorTestData> {
    public:
        using TOnOffTestTask::TOnOffTestTask;

    protected:
        virtual ui16 GetSensor() const override {
            return CAN_DRIVER_DOOR;
        }
    };

    class TPassengerDoorTestTask: public TOnOffTestTask<TPassengerDoorTestData> {
    public:
        using TOnOffTestTask::TOnOffTestTask;

    protected:
        virtual ui16 GetSensor() const override {
            return CAN_PASS_DOOR;
        }
    };

    class TLeftRearDoorTestTask: public TOnOffTestTask<TLeftRearDoorTestData> {
    public:
        using TOnOffTestTask::TOnOffTestTask;

    protected:
        virtual ui16 GetSensor() const override {
            return CAN_L_REAR_DOOR;
        }
    };

    class TRightRearDoorTestTask: public TOnOffTestTask<TRightRearDoorTestData> {
    public:
        using TOnOffTestTask::TOnOffTestTask;

    protected:
        virtual ui16 GetSensor() const override {
            return CAN_R_REAR_DOOR;
        }
    };

    class THoodTestTask: public TOnOffTestTask<THoodTestData> {
    public:
        using TOnOffTestTask::TOnOffTestTask;

    protected:
        virtual ui16 GetSensor() const override {
            return CAN_HOOD;
        }
    };

    class TTrunkTestTask: public TOnOffTestTask<TTrunkTestData> {
    public:
        using TOnOffTestTask::TOnOffTestTask;

    protected:
        virtual ui16 GetSensor() const override {
            return CAN_TRUNK;
        }
    };

    class TRadioblockTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
        virtual bool Rollback(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self, const TString& message) override;
    };

    class TMoveblockTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
        virtual bool Rollback(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self, const TString& message) override;
    };

    class TWarmupTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
    };

    class TWindowsTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
    };

    class TLockDoorTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
    };

    class TFinishTestTask: public TCommonTestTask {
    public:
        using TCommonTestTask::TCommonTestTask;

    protected:
        virtual bool DoDoExecute(IDDataStorage::TGuard::TPtr d, IDistributedTask::TPtr self) override;
    };
}
