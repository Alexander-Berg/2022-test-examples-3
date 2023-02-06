#pragma once

#include <drive/backend/rt_background/manager/manager.h>

#include <drive/backend/proto/background.pb.h>
#include <drive/backend/tags/tags_filter.h>

class TRTBackgroundMessageIncoming: public IMessage {
private:
    R_READONLY(TVector<i32>, PingValues);
public:
    void AddPingValue(const i32 value) {
        PingValues.emplace_back(value);
    }
};

class TRTBackgroundMessageOut: public IMessage {
private:
    R_READONLY(i32, Value, -10000);
public:

    TRTBackgroundMessageOut() = default;

    TRTBackgroundMessageOut(const i32 value)
        : Value(value)
    {

    }
};

class TRTBackgroundTesterState: public IProtoStateSerializable<NDrive::NProto::TTesterState> {
private:
    R_FIELD(i32, Value, 0);
    static TFactory::TRegistrator<TRTBackgroundTesterState> Registrator;
protected:
    virtual void SerializeToProto(NDrive::NProto::TTesterState& proto) const override {
        proto.SetValue(Value);
    }

    virtual bool DeserializeFromProto(const NDrive::NProto::TTesterState& proto) override {
        Value = proto.GetValue();
        return true;
    }
public:
    NJson::TJsonValue GetReport() const override {
        NJson::TJsonValue result = NJson::JSON_MAP;
        JWRITE(result, "value", Value);
        return result;
    }

    TString GetType() const override;
};

class TRTBackgroundTester: public IRTRegularBackgroundProcess, public IMessageProcessor {
private:
    using TBase = IRTRegularBackgroundProcess;
    R_FIELD(i32, PingValue, 0);
private:
    static TFactory::TRegistrator<TRTBackgroundTester> Registrator;
    bool Started = false;
protected:
    virtual bool DoStart(const TRTBackgroundProcessContainer& /*container*/) override {
        Started = true;
        RegisterGlobalMessageProcessor(this);
        return true;
    }
public:

    TRTBackgroundTester() = default;
    ~TRTBackgroundTester() {
        if (Started) {
            UnregisterGlobalMessageProcessor(this);
        }
    }

    virtual bool Process(IMessage* message) override;

    virtual TString Name() const override {
        return ::ToString(ui64(this));
    }

    static TString GetTypeName() {
        return "tester";
    }

    virtual TString GetType() const override {
        return GetTypeName();
    }

    using TBase::TBase;

    virtual TExpectedState DoExecute(TAtomicSharedPtr<IRTBackgroundProcessState> state, const TExecutionContext& context) const override;

    virtual NJson::TJsonValue DoSerializeToJson() const override {
        NJson::TJsonValue result = TBase::DoSerializeToJson();
        JWRITE(result, "ping_value", PingValue);
        return result;
    }

    virtual bool DoDeserializeFromJson(const NJson::TJsonValue& jsonInfo) override {
        if (!TBase::DoDeserializeFromJson(jsonInfo)) {
            return false;
        }
        JREAD_INT(jsonInfo, "ping_value", PingValue);
        return true;
    }

};
