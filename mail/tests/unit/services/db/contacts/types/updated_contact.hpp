#pragma once

#include <src/services/db/contacts/types/reflection/updated_contact.hpp>
#include <tests/unit/types/jsonb.hpp>

namespace collie::services::db::contacts {

static bool operator==(const UpdatedContact& left, const UpdatedContact& right) {
    return boost::fusion::operator==(left, right);
}

}
