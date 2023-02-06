#pragma once

#include <src/logic/interface/types/reflection/created_contacts.hpp>

namespace collie::logic {

static bool operator==(const CreatedContacts& left, const CreatedContacts& right) {
    return boost::fusion::operator==(left, right);
}

}
