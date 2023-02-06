#ifndef RECIPIENT_PARSER_FROM_STRING_QUOTED_PAIR_HPP_
#define RECIPIENT_PARSER_FROM_STRING_QUOTED_PAIR_HPP_

#include <string>

namespace rcpt_parser {

std::pair<bool, std::string::const_iterator> parse_quoted_pair(
        const std::string& input,
        std::string& result);

}

#endif /* RECIPIENT_PARSER_FROM_STRING_QUOTED_PAIR_HPP_ */
