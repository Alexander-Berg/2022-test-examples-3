#pragma once

#include "email_address.h"

#include <mail/notsolitesrv/src/firstline/types/request.h>
#include <mail/notsolitesrv/src/firstline/types/response.h>

namespace NNotSoLiteSrv::NFirstline {

inline bool operator==(const TFirstlineResponse& left, const TFirstlineResponse& right) {
    return left.Firstline == right.Firstline;
}

inline bool operator==(const TFirstlineRequest& left, const TFirstlineRequest& right) {
    return left.IsHtml == right.IsHtml
        && left.Part == right.Part
        && left.Subject == right.Subject
        && left.From == right.From
        && left.IsPeopleType == right.IsPeopleType;
}

} // namespace NNotSoLiteSrv::NFirstline
