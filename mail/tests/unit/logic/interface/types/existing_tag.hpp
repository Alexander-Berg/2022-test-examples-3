#pragma once

#include <src/logic/interface/types/reflection/existing_tag.hpp>

namespace collie::logic {

static inline bool operator ==(const ExistingTag& lhs, const ExistingTag& rhs) {
    return boost::fusion::operator==(left, right);
}

}
