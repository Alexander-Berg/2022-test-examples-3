#pragma once

#include <src/types/reflection/email_with_tags.hpp>

namespace collie {

static bool operator==(const EmailWithTags& left, const EmailWithTags& right) {
    return boost::fusion::operator==(left, right);
}

}
