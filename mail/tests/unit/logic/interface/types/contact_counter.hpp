#pragma once

#include <src/logic/interface/types/contact_counter.hpp>
#include <src/logic/interface/types/reflection/contact_counter.hpp>

namespace collie::logic {

static inline bool operator ==(const ContactCounter& lhs, const ContactCounter& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

}
