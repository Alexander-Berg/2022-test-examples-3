#pragma once

#include <mail/notsolitesrv/src/rules_applier/types/response.h>

#include <mail/notsolitesrv/tests/unit/util/email_address.h>

namespace NNotSoLiteSrv::NRulesApplier {

inline bool operator==(const TMessage& left, const TMessage& right) {
    return (left.From == right.From) && (left.Sender == right.Sender) && (left.ReplyTo == right.ReplyTo);
}

inline bool operator==(const TRecipient& left, const TRecipient& right) {
    return (left.Uid == right.Uid) && (left.UseFilters == right.UseFilters);
}

inline bool operator==(const TAutoreply& left, const TAutoreply& right) {
    return (left.Address == right.Address) && (left.Body == right.Body);
}

inline bool operator==(const TFolderCoords& left, const TFolderCoords& right) {
    return (left.Fid == right.Fid) && (left.Path == right.Path);
}

inline bool operator==(const TAppliedRules& left, const TAppliedRules& right) {
    return
        (left.Notifies == right.Notifies) &&
        (left.Replies == right.Replies) &&
        (left.Forwards == right.Forwards) &&
        (left.DestFolder == right.DestFolder) &&
        (left.Lids == right.Lids) &&
        (left.LabelSymbols == right.LabelSymbols) &&
        (left.RuleIds == right.RuleIds) &&
        (left.StoreAsDeleted == right.StoreAsDeleted) &&
        (left.NoSuchFolderAction == right.NoSuchFolderAction);
}

inline bool operator==(const TRulesApplierResponse& left, const TRulesApplierResponse& right) {
    return (left.AppliedRules == right.AppliedRules) && (left.FailedRecipients == right.FailedRecipients);
}

}
