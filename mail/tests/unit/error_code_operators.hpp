#pragma once

#include <src/error_code.hpp>

namespace collie {

static inline std::ostream& operator <<(std::ostream& stream, const error_code& value) {
    return stream << value.category().name() << ":" << value.base().message();
}

} // namespace collie
