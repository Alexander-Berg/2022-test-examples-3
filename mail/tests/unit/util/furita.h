#pragma once

#include <mail/notsolitesrv/src/furita/types/response.h>
#include <mail/notsolitesrv/src/rules/domain/types/furita.h>

namespace NNotSoLiteSrv {

namespace NFurita {

static inline bool operator==(const TFuritaAction& left, const TFuritaAction& right) {
    return (left.Verified == right.Verified) && (left.Parameter == right.Parameter) &&
        (left.Type == right.Type);
}

static inline bool operator==(const TFuritaRule& left, const TFuritaRule& right) {
    return (left.Id == right.Id) && (left.Priority == right.Priority) && (left.Query == right.Query) &&
        (left.Enabled == right.Enabled) && (left.Stop == right.Stop) && (left.Actions == right.Actions);
}

static inline bool operator==(const TFuritaListResponse& left, const TFuritaListResponse& right) {
    return (left.Rules == right.Rules);
}

static inline bool operator==(const TFuritaDomainRuleScope& left, const TFuritaDomainRuleScope& right) {
    return (left.Direction == right.Direction);
}

static inline bool operator==(const TFuritaDomainActionData& left, const TFuritaDomainActionData& right) {
    return (left.Email == right.Email);
}

static inline bool operator==(const TFuritaDomainAction& left, const TFuritaDomainAction& right) {
    return ((left.Action == right.Action) && (left.Data == right.Data));
}

static inline bool operator==(const TFuritaDomainRule& left, const TFuritaDomainRule& right) {
    return (left.Terminal == right.Terminal) && (left.Scope == right.Scope) &&
        (left.ConditionQuery == right.ConditionQuery) && (left.Actions == right.Actions);
}

static inline bool operator==(const TFuritaGetResponse& left, const TFuritaGetResponse& right) {
    return (left.Rules == right.Rules) && (left.Revision == right.Revision);
}

}

namespace NRules {

static inline bool operator==(const TDomainRulesAccumulatedResult& left,
    const TDomainRulesAccumulatedResult& right)
{
    return (left.Forwards == right.Forwards) && (left.Drop == right.Drop) &&
        (left.AppliedDomainRuleIds == right.AppliedDomainRuleIds);
}

}

}
