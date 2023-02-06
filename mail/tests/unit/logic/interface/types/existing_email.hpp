#pragma once

#include <src/logic/interface/types/reflection/existing_email.hpp>

namespace collie::logic {

static bool operator==(const ExistingEmail& left, const ExistingEmail& right) {
    return boost::fusion::operator==(left, right);
}

}
