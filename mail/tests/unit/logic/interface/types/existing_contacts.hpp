#pragma once

#include "existing_contact.hpp"

#include <src/logic/interface/types/reflection/existing_contacts.hpp>

namespace collie::logic {

static bool operator==(const ExistingContacts& left, const ExistingContacts& right) {
    return boost::fusion::operator==(left, right);
}

}
