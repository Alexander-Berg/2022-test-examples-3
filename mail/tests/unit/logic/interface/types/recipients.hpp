#pragma once

#include <src/logic/interface/types/reflection/recipients.hpp>

namespace collie::logic {

static bool operator==(const Recipients& left, const Recipients& right) {
    return boost::fusion::operator==(left, right);
}

}
