#ifndef RECIPIENT_PARSER_FROM_STRING_ADDR_SPEC_HPP_
#define RECIPIENT_PARSER_FROM_STRING_ADDR_SPEC_HPP_

#include <string>

#include <recipient_parser/types/addr_spec.hpp>

namespace rcpt_parser {

std::pair<bool, std::string::const_iterator> parse_local_part(const std::string& input, std::string& result);
std::pair<bool, std::string::const_iterator> parse_domain(const std::string& input, std::string& result);
std::pair<bool, std::string::const_iterator> parse_addr_spec(const std::string& input, types::AddrSpec& result);

}

#endif /* RECIPIENT_PARSER_FROM_STRING_EMAIL_HPP_ */
