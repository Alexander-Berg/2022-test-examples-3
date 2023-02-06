#pragma once

#include <drive/backend/processors/common_app/processor.h>

namespace NDrive {
    class TCommonTestData;
}

class TTestProcessorConfig: public TCommonAppConfig {
public:
    struct TScenario {
        TString Name;
        TString Description;
        TSet<TString> AllowedModels;
        TSet<TString> BannedModels;
    };
    using TScenarios = TVector<TScenario>;

private:
    using TBase = TCommonAppConfig;

public:
    using TBase::TBase;

    const TScenarios& GetScenarios() const {
        return Scenarios;
    }
    const TString& GetStorageName() const {
        return StorageName;
    }

    virtual IRequestProcessor::TPtr DoConstructAuthProcessor(IReplyContext::TPtr context, IAuthModule::TPtr authModule, const IServerBase* server) const override;

    virtual void CheckServerForProcessor(const IServerBase* server) const override;
    virtual void DoInit(const TYandexConfig::Section* section) override;
    virtual void ToString(IOutputStream& os) const override;

private:
    TScenarios Scenarios;
    TString StorageName;
};

class TCommonTestProcessor: public TCommonServiceAppProcessorBase {
protected:
    const TTestProcessorConfig& Config;

public:
    TCommonTestProcessor(const TTestProcessorConfig& config, IReplyContext::TPtr context, IAuthModule::TPtr auth, const NDrive::IServer* server)
        : TCommonServiceAppProcessorBase(config, context, auth, server)
        , Config(config)
    {
    }

    TAtomicSharedPtr<NDrive::TCommonTestData> GetData(const TString& id) const;
    TAtomicSharedPtr<NRTLine::IVersionedStorage> GetStorage() const;
    NJson::TJsonValue GetNullInfo(const TString& type) const;
    TString GetIMEI(const NJson::TJsonValue& post) const;
    TString GetIMEI(const TCgiParameters& cgi) const;
    TString GetIMEI(const TString& carId) const;
    TString GetModel(const TCgiParameters& cgi) const;
    TString GetModel(const TString& carId) const;
    TString GetStatePath(const TString& imei) const;

protected:
    bool RemoveTag(const TString& carId, TUserPermissions::TPtr permissions) const;
};

class TCreateTestProcessor: public TCommonTestProcessor {
public:
    using TCommonTestProcessor::TCommonTestProcessor;

protected:
    virtual void ProcessServiceRequest(TJsonReport::TGuard& g, TUserPermissions::TPtr permissions, const NJson::TJsonValue& requestData) override;
};

class TInputTestProcessor: public TCommonTestProcessor {
public:
    using TCommonTestProcessor::TCommonTestProcessor;

protected:
    virtual void ProcessServiceRequest(TJsonReport::TGuard& g, TUserPermissions::TPtr permissions, const NJson::TJsonValue& requestData) override;
};

class TListTestProcessor: public TCommonTestProcessor {
public:
    using TCommonTestProcessor::TCommonTestProcessor;

protected:
    virtual void ProcessServiceRequest(TJsonReport::TGuard& g, TUserPermissions::TPtr permissions, const NJson::TJsonValue& requestData) override;
};

class TViewTestProcessor: public TCommonTestProcessor {
public:
    using TCommonTestProcessor::TCommonTestProcessor;

protected:
    virtual void ProcessServiceRequest(TJsonReport::TGuard& g, TUserPermissions::TPtr permissions, const NJson::TJsonValue& requestData) override;
};
