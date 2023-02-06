#pragma once

#include <search/reqparam/reqparam.h>
#include <kernel/search_daemon_iface/cntintrf.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace NGeosearch::NTestUtils {

    class TMockReqResults: public IReqResults {
    public:
        TMockReqResults();
        virtual ~TMockReqResults();

        MOCK_METHOD(const char*, Request, (), (override));
        MOCK_METHOD(const char*, ErrorText, (), (override));
        MOCK_METHOD(int, ErrorCode, (unsigned), (const, override));
        MOCK_METHOD(int, SerpErrorCode, (), (const, override));
        MOCK_METHOD(const char*, ReqId, (), (const, override));
        MOCK_METHOD(unsigned, WizardRuleNameCount, (), (const, override));
        MOCK_METHOD(const char*, WizardRuleName, (unsigned), (const, override));
        MOCK_METHOD(unsigned, WizardRulePropertyNameCount, (const char*), (const, override));
        MOCK_METHOD(const char*, WizardRulePropertyName, (const char*, unsigned), (const, override));
        MOCK_METHOD(unsigned, WizardRulePropertyCount, (const char*, const char*), (const, override));
        MOCK_METHOD(const char*, WizardRuleProperty, (const char*, const char*, unsigned), (const, override));
    };

} // namespace NGeosearch::NTestUtils
