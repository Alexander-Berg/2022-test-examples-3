#ifndef RECIPIENT_PARSER_INCLUDE_ERROR_HPP_
#define RECIPIENT_PARSER_INCLUDE_ERROR_HPP_

#include <string>
#include <stdexcept>

namespace rcpt_parser {

struct ParseError : std::runtime_error {
    ParseError(const std::string& msg) : std::runtime_error(msg) {}
};

}

#endif //RECIPIENT_PARSER_INCLUDE_ERROR_HPP_
