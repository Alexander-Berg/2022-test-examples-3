#pragma once

#include <search/meta/scatter/source.h>
#include <search/meta/scatter/options/options.h>

#include <library/cpp/neh/rpc.h>

#include <util/system/guard.h>
#include <util/system/mutex.h>

struct TTestService
    : public NNeh::IService
{
public:
    explicit TTestService(const TString& clientDescr, const TString& serverScheme = "http", const TString& sourceScheme = "http",  const NScatter::TSourceOptions& opts = {}, size_t backendsCount = 0);
    TTestService()
        : TTestService("TEST_SERVICE_SOURCE")
    {
    }

    ~TTestService();

    void Stop();

    NNeh::IServicesRef StartService(const TString& scheme, size_t* port);

    void ServeRequest(const NNeh::IRequestRef& req);

    virtual void DoServeRequest(const NNeh::IRequestRef& req) = 0;
    virtual void DoServeMessage(const NNeh::IRequestRef&) {}

    size_t GetRequestCount() const {
        with_lock (Mutex_) {
            return Requests.size();
        }
    }
public:
    NNeh::IServicesRef Loop;

    TString SourceScript;
    THolder<NScatter::ISource> Source;

    TVector<TString> Requests;

    size_t Port;
private:
    TMutex Mutex_;
};

template <typename T>
class TServiceHolder
    : public T
    , TNonCopyable
{
public:
    using T::T;

    ~TServiceHolder() {
        T::Stop();
    }
};
