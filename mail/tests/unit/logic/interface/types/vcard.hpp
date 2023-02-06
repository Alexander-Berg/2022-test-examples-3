#pragma once

#include <src/logic/interface/types/reflection/vcard.hpp>

namespace collie::logic {

static bool operator==(const TelephoneNumber& left, const TelephoneNumber& right) {
    return boost::fusion::operator==(left, right);
}

static bool operator==(const Vcard& left, const Vcard& right) {
    return std::tie(left.names, left.emails, left.telephone_numbers) ==
            std::tie(right.names, right.emails, right.telephone_numbers);
}

}
