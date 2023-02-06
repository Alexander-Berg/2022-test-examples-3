#pragma once

#include "vcard.hpp"

#include <src/logic/interface/types/reflection/new_contact.hpp>

namespace collie::logic {

static bool operator==(const NewContact& left, const NewContact& right) {
    return boost::fusion::operator==(left, right);
}

}
