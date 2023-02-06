#pragma once

#include <src/services/db/contacts/types/reflection/updated_contacts_email.hpp>

namespace collie::services::db::contacts {

static bool operator==(const UpdatedContactsEmail& left, const UpdatedContactsEmail& right) {
    return boost::fusion::operator==(left, right);
}

}
