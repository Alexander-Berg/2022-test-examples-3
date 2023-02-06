#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/furita/ymod_db/include/repository.h>


namespace furita {

struct MockRepository : public Repository {
    MOCK_METHOD(void, asyncSetDomainRules, (const SetDomainRulesParams&, OnSetDomainRules), (const, override));
    MOCK_METHOD(void, asyncGetDomainRules, (const GetDomainRulesParams&, OnGetDomainRules), (const, override));
};

} // namespace furita
