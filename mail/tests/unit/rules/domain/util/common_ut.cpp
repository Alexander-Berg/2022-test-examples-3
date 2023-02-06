#include <mail/notsolitesrv/src/rules/domain/util/common.h>

#include <gtest/gtest.h>

namespace {

using NNotSoLiteSrv::EError;
using NNotSoLiteSrv::NMetaSaveOp::TRecipientMap;
using NNotSoLiteSrv::NRules::DomainRulesInUse;
using NNotSoLiteSrv::NRules::MakeUserFilter;
using NNotSoLiteSrv::NUser::ELoadStatus;
using NNotSoLiteSrv::NUser::TUser;

TEST(TestDomainRulesInUse, for_domain_rules_not_in_use_must_return_false) {
    TRecipientMap recipients{{"DeliveryId", {}}};
    EXPECT_FALSE(DomainRulesInUse(recipients));
}

TEST(TestDomainRulesInUse, for_domain_rules_in_use_must_return_true) {
    TRecipientMap recipients{{"DeliveryId", {.params{.use_domain_rules = true}}}};
    EXPECT_TRUE(DomainRulesInUse(recipients));
}

TEST(TestMakeUserFilter, must_make_user_filter) {
    const auto filter{MakeUserFilter()};
    TUser user {
        .Status = ELoadStatus::Unknown,
        .DeliveryResult{.ErrorCode{EError::FuritaResponseParseError}},
        .DeliveryParams{.NeedDelivery = false, .DeliveryId{"DeliveryId"}}
    };

    EXPECT_FALSE(filter(user));
    user.Status = ELoadStatus::Found;
    EXPECT_FALSE(filter(user));
    user.DeliveryResult.ErrorCode = EError::Ok;
    EXPECT_FALSE(filter(user));
    user.DeliveryParams.NeedDelivery = true;
    EXPECT_FALSE(filter(user));
    user.DeliveryParams.UseDomainRules = true;
    EXPECT_FALSE(filter(user));
    user.DeliveryParams.DeliveryId.clear();
    EXPECT_TRUE(filter(user));
}

}
