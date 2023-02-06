#pragma once

#include <src/services/abook/types/reflection/search_contacts_result.hpp>

namespace collie::services::abook {

static bool operator==(const Name& left, const Name& right) {
    return boost::fusion::operator==(left, right);
}

static bool operator==(const Birthdate& left, const Birthdate& right) {
    return boost::fusion::operator==(left, right);
}

static bool operator==(const Phone& left, const Phone& right) {
    return boost::fusion::operator==(left, right);
}

static bool operator==(const Photo& left, const Photo& right) {
    return boost::fusion::operator==(left, right);
}

static bool operator==(const YaDirectory& left, const YaDirectory& right) {
    return boost::fusion::operator==(left, right);
}

static bool operator==(const Contact& left, const Contact& right) {
    return boost::fusion::operator==(left, right);
}

static bool operator==(const ContactEmail& left, const ContactEmail& right) {
    return boost::fusion::operator==(left, right);
}

static bool operator==(const SearchContactsGroupedResult& left, const SearchContactsGroupedResult& right) {
    return boost::fusion::operator==(left, right);
}

static bool operator==(const SearchContactsUngroupedResult& left,
        const SearchContactsUngroupedResult& right) {
    return boost::fusion::operator==(left, right);
}

}
