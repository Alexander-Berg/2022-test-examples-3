#include <recipient_parser/rules/quoted_pair.hpp>

#include "quoted_pair.hpp"
#include "detail/parse_template.hpp"

namespace rcpt_parser {

std::pair<bool, std::string::const_iterator> parse_quoted_pair(
        const std::string& input,
        std::string& result) {
    return detail::parse_template<QuotedPair>(input, result);
}

} // namespace rcpt_parser
