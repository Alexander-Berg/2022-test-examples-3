#pragma once

#include <mail/notsolitesrv/src/tupita/types/request.h>
#include <mail/notsolitesrv/src/tupita/types/response.h>

#include <mail/notsolitesrv/tests/unit/util/email_address.h>

namespace NNotSoLiteSrv::NTupita {

inline bool operator==(const TTupitaMessageLabelType& left, const TTupitaMessageLabelType& right) {
    return (left.Title == right.Title);
}

inline bool operator==(const TTupitaMessageLabel& left, const TTupitaMessageLabel& right) {
    return (left.Name == right.Name) && (left.IsSystem == right.IsSystem) && (left.IsUser == right.IsUser) &&
        (left.Type == right.Type);
}

inline bool operator==(const TTupitaMessage& left, const TTupitaMessage& right) {
    return
        (left.Subject == right.Subject) &&
        (left.From == right.From) &&
        (left.To == right.To) &&
        (left.Cc == right.Cc) &&
        (left.Stid == right.Stid) &&
        (left.Spam == right.Spam) &&
        (left.Types == right.Types) &&
        (left.AttachmentsCount == right.AttachmentsCount) &&
        (left.LabelsInfo == right.LabelsInfo) &&
        (left.Firstline == right.Firstline);
}

inline bool operator==(const TTupitaQuery& left, const TTupitaQuery& right) {
    return (left.Id == right.Id) && (left.Query == right.Query) && (left.Stop == right.Stop);
}

inline bool operator==(const TTupitaUser& left, const TTupitaUser& right) {
    return (left.Uid == right.Uid) && (left.Queries == right.Queries) && (left.Spam == right.Spam);
}

inline bool operator==(const TTupitaUserWithMatchedQueries& left,
    const TTupitaUserWithMatchedQueries& right)
{
    return (left.MatchedQueries == right.MatchedQueries);
}

inline bool operator==(const TTupitaCheckResponse& left, const TTupitaCheckResponse& right) {
    return (left.Result == right.Result);
}

}
