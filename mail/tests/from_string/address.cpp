#include <recipient_parser/rules/address.hpp>

#include <recipient_parser/parse.hpp>

#include "address.hpp"
#include "detail/parse_template.hpp"

namespace rcpt_parser {

std::pair<bool, std::string::const_iterator> parse_name_addr(const std::string& input, types::NameAddr& result) {
    return detail::parse_template<NameAddr>(input, result);
}

std::pair<bool, std::string::const_iterator> test_parse_mailbox(const std::string& input, types::Mailbox& result) {
    auto iter = input.begin();
    auto parse_result = parse_mailbox(iter, input.end());
    if( parse_result ) {
        result = *parse_result;
    }
    return {parse_result.is_initialized(), iter};
}

std::pair<bool, std::string::const_iterator> parse_group(const std::string& input, types::MailboxGroup& result) {
    return detail::parse_template<Group>(input, result);
}

std::pair<bool, std::string::const_iterator> test_parse_address(const std::string& input, types::Address& result) {
    auto iter = input.begin();
    auto parse_result = parse_address(iter, input.end());
    if( parse_result ) {
        result = *parse_result;
    }
    return {parse_result.is_initialized(), iter};
}

} // namespace rcpt_parser
