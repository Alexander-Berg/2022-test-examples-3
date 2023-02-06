#pragma once

#include <src/services/db/contacts/types/reflection/new_contacts_email.hpp>

namespace collie::services::db::contacts {

static bool operator==(const NewContactsEmail& left, const NewContactsEmail& right) {
    return boost::fusion::operator==(left, right);
}

}
