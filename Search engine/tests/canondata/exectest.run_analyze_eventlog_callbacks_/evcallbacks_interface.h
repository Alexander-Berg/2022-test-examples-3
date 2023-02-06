#pragma once

#include <library/cpp/eventlog/dumper/tunable_event_processor.h>
#include <search/tools/analyze_eventlog/lib.h>

#include <library/cpp/eventlog/eventlog.h>

#include <util/system/types.h>

class IEventCallbacks : public IHistogramCalcer, public ITunableEventProcessor {
public:
    virtual void OnRawEvent(const TEvent* ev) = 0;
    virtual void OnBeginFrame(ui64 timestamp, ui32 frameId) = 0;
    virtual void OnEndFrame(ui64 timestamp, ui32 frameId) = 0;

    void SetEventProcessor(IEventProcessor* processor) override {
        Processor_ = processor;
    }

    void AddOptions(NLastGetopt::TOpts& /*opts*/) override {
    }

protected:
    IEventProcessor* Processor_ = nullptr;
};

#include <search/idl/events.ev.pb.h>

namespace NEvClass {

class IEventCallbacks : public ::IEventCallbacks {
public:
    IEventCallbacks() = default;

    virtual ~IEventCallbacks() {
    }

    void OnRawEvent(const TEvent* ev) override {
        switch (ev->Class) {
            case TAppHostRole::ID: {
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TAppHostRole>());
                break;
            }
            case TRearrangeInfo::ID: {
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TRearrangeInfo>());
                break;
            }
        }
    }

    virtual void OnEvent(ui64 /*timestamp*/, ui32 /*frameId*/, const TAppHostRole&) {
    }

    virtual void OnEvent(ui64 /*timestamp*/, ui32 /*frameId*/, const TRearrangeInfo&) {
    }

    void OnBeginFrame(ui64 /*timestamp*/, ui32 /*frameId*/) override {
    }

    void OnEndFrame(ui64 /*timestamp*/, ui32 /*frameId*/) override {
    }

    void ProcessEvent(const TEvent* ev) override {
        switch (ev->Class) {
            case TAppHostRole::ID: {
                if (Processor_) {
                    Processor_->ProcessEvent(ev);
                }
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TAppHostRole>());
                break;
            }
            case TRearrangeInfo::ID: {
                if (Processor_) {
                    Processor_->ProcessEvent(ev);
                }
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TRearrangeInfo>());
                break;
            }
        }
    }

};

} // namespace NEvClass

#include <apphost/lib/event_log/decl/events.ev.pb.h>

namespace NAppHost {

class IEventCallbacks : public ::IEventCallbacks {
public:
    IEventCallbacks() = default;

    virtual ~IEventCallbacks() {
    }

    void OnRawEvent(const TEvent* ev) override {
        switch (ev->Class) {
            case TInputDump::ID: {
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TInputDump>());
                break;
            }
            case TStartRequest::ID: {
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TStartRequest>());
                break;
            }
            case TGrpcSendRequest::ID: {
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TGrpcSendRequest>());
                break;
            }
            case TSourceSuccess::ID: {
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TSourceSuccess>());
                break;
            }
            case TReqID::ID: {
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TReqID>());
                break;
            }
            case TSourceError::ID: {
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TSourceError>());
                break;
            }
            case TSourceStart::ID: {
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TSourceStart>());
                break;
            }
        }
    }

    virtual void OnEvent(ui64 /*timestamp*/, ui32 /*frameId*/, const TInputDump&) {
    }

    virtual void OnEvent(ui64 /*timestamp*/, ui32 /*frameId*/, const TStartRequest&) {
    }

    virtual void OnEvent(ui64 /*timestamp*/, ui32 /*frameId*/, const TGrpcSendRequest&) {
    }

    virtual void OnEvent(ui64 /*timestamp*/, ui32 /*frameId*/, const TSourceSuccess&) {
    }

    virtual void OnEvent(ui64 /*timestamp*/, ui32 /*frameId*/, const TReqID&) {
    }

    virtual void OnEvent(ui64 /*timestamp*/, ui32 /*frameId*/, const TSourceError&) {
    }

    virtual void OnEvent(ui64 /*timestamp*/, ui32 /*frameId*/, const TSourceStart&) {
    }

    void OnBeginFrame(ui64 /*timestamp*/, ui32 /*frameId*/) override {
    }

    void OnEndFrame(ui64 /*timestamp*/, ui32 /*frameId*/) override {
    }

    void ProcessEvent(const TEvent* ev) override {
        switch (ev->Class) {
            case TInputDump::ID: {
                if (Processor_) {
                    Processor_->ProcessEvent(ev);
                }
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TInputDump>());
                break;
            }
            case TStartRequest::ID: {
                if (Processor_) {
                    Processor_->ProcessEvent(ev);
                }
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TStartRequest>());
                break;
            }
            case TGrpcSendRequest::ID: {
                if (Processor_) {
                    Processor_->ProcessEvent(ev);
                }
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TGrpcSendRequest>());
                break;
            }
            case TSourceSuccess::ID: {
                if (Processor_) {
                    Processor_->ProcessEvent(ev);
                }
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TSourceSuccess>());
                break;
            }
            case TReqID::ID: {
                if (Processor_) {
                    Processor_->ProcessEvent(ev);
                }
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TReqID>());
                break;
            }
            case TSourceError::ID: {
                if (Processor_) {
                    Processor_->ProcessEvent(ev);
                }
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TSourceError>());
                break;
            }
            case TSourceStart::ID: {
                if (Processor_) {
                    Processor_->ProcessEvent(ev);
                }
                OnEvent(ev->Timestamp, ev->FrameId, *ev->Get<TSourceStart>());
                break;
            }
        }
    }

};

} // namespace NAppHost
