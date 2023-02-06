#pragma once

#include "existing_email.hpp"

#include <src/logic/interface/types/reflection/existing_emails.hpp>

namespace collie::logic {

static bool operator==(const ExistingEmails& left, const ExistingEmails& right) {
    return boost::fusion::operator==(left, right);
}

}
