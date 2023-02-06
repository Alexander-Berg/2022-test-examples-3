#pragma once

#include <mail/notsolitesrv/src/types/email_address.h>

namespace NNotSoLiteSrv {

inline bool operator==(const TEmailAddress& left, const TEmailAddress& right) {
    return (left.Local == right.Local) && (left.Domain == right.Domain) &&
        (left.DisplayName == right.DisplayName);
}

}
