#pragma once

#include <src/logic/interface/types/reflection/created_tag.hpp>

namespace collie::logic {

static inline bool operator ==(const CreatedTag& left, const CreatedTag& right) {
    return boost::fusion::operator==(left, right);
}

}
