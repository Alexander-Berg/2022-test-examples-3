#ifndef RECIPIENT_PARSER_FROM_STRING_ADDRESS_HPP_
#define RECIPIENT_PARSER_FROM_STRING_ADDRESS_HPP_

#include <string>

#include <recipient_parser/types/name_addr.hpp>
#include <recipient_parser/types/mailbox_group.hpp>
#include <recipient_parser/types/address.hpp>

namespace rcpt_parser {

std::pair<bool, std::string::const_iterator> parse_name_addr(const std::string& input, types::NameAddr& result);
std::pair<bool, std::string::const_iterator> test_parse_mailbox(const std::string& input, types::Mailbox& result);
std::pair<bool, std::string::const_iterator> parse_group(const std::string& input, types::MailboxGroup& result);

std::pair<bool, std::string::const_iterator> test_parse_address(const std::string& input, types::Address& result);
}

#endif /* RECIPIENT_PARSER_FROM_STRING_EMAIL_HPP_ */
