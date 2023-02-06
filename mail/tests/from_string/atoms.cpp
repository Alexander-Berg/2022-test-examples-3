#include <recipient_parser/rules/atoms.hpp>

#include "atoms.hpp"
#include "detail/parse_template.hpp"

namespace rcpt_parser {

std::pair<bool, std::string::const_iterator> parse_dot_atom(
        const std::string& input,
        std::string& result) {
    return detail::parse_template<DotAtom>(input, result);
}

} // namespace rcpt_parser
