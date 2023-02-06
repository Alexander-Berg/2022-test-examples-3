#include "mocks/transactional_mock.h"

namespace pgg {
namespace query {

bool operator ==(const Query& lhs, const Query& rhs) {
    return std::strcmp(lhs.name(), rhs.name()) == 0;
}

} // namespace query
} // namespace pgg

namespace tests {

std::ostream& operator <<(std::ostream& stream, const error_code& value) {
    return stream << "error_code {value=" << value.value()
        << ", message=\"" << value.message()
        << ", what=\"" << value.what() << "\"}";
}

std::ostream& operator <<(std::ostream& stream, const Query& value) {
    return stream << "Query {name=\"" << value.name() << "\"}";
}

} // namespace tests
