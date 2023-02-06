#include <recipient_parser/rules/quoted_string.hpp>

#include "quoted_string.hpp"
#include "detail/parse_template.hpp"

namespace rcpt_parser {

std::pair<bool, std::string::const_iterator> parse_quoted_string(
        const std::string& input,
        std::string& result) {
    return detail::parse_template<QuotedString>(input, result);
}

} // namespace rcpt_parser
