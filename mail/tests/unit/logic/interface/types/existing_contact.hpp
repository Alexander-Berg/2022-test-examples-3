#pragma once

#include "vcard.hpp"

#include <src/logic/interface/types/reflection/existing_contact.hpp>

#include <tests/unit/types/email_with_tags.hpp>

namespace collie::logic {

static bool operator==(const ExistingContact& left, const ExistingContact& right) {
    return boost::fusion::operator==(left, right);
}

}
