#pragma once

#include <tests/unit/error_code_operators.hpp>

#include <src/server/error_category.hpp>

namespace collie::server {

static inline std::ostream& operator <<(std::ostream& stream, Error value) {
    return stream << error_code(value).message();
}

static inline bool operator ==(const error_code& lhs, Error rhs) {
    return lhs == error_code(rhs);
}

} // namespace collie::server
