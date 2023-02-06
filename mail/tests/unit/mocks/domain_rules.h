#pragma once

#include <mail/notsolitesrv/src/rules/domain/idomain_rules.h>

#include <gmock/gmock.h>

class TDomainRulesMock : public NNotSoLiteSrv::NRules::IDomainRules {
public:
    MOCK_METHOD(
        void,
        SetParams,
        (NNotSoLiteSrv::TMessagePtr,
         NNotSoLiteSrv::NMetaSaveOp::TRequest,
         NNotSoLiteSrv::NRules::TDomainRulesCallback),
        (override));

    MOCK_METHOD(
        void,
        Call,
        (NNotSoLiteSrv::NRules::IDomainRules::TYieldCtx,
         NNotSoLiteSrv::TErrorCode,
         NNotSoLiteSrv::NRules::IDomainRules::TResult),
        (const));

    void operator()(
        NNotSoLiteSrv::NRules::IDomainRules::TYieldCtx yieldCtx,
        NNotSoLiteSrv::TErrorCode errorCode = {},
        NNotSoLiteSrv::NRules::IDomainRules::TResult result = {}) override
    {
        Call(yieldCtx, errorCode, result);
    }

    void SetCallback(const NNotSoLiteSrv::NRules::TDomainRulesCallback& callback) {
        Callback = callback;
    }

    NNotSoLiteSrv::NRules::TDomainRulesCallback& GetCallback() {
        return Callback;
    }

private:
    NNotSoLiteSrv::NRules::TDomainRulesCallback Callback;
};
