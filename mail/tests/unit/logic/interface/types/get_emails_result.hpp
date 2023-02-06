#pragma once

#include "existing_emails.hpp"

#include <src/logic/interface/types/reflection/get_emails_result.hpp>

namespace collie::logic {

static bool operator==(const GetEmailsResult& left, const GetEmailsResult& right) {
    return boost::fusion::operator==(left, right);
}

}
