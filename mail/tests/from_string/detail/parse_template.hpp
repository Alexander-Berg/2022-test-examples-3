#ifndef RECIPIENT_PARSER_SRC_FROM_STRING_DETAIL_PARSE_TEMPLATE_HPP_
#define RECIPIENT_PARSER_SRC_FROM_STRING_DETAIL_PARSE_TEMPLATE_HPP_

#include "error.hpp"
#include "grammar_from_rule.hpp"

namespace rcpt_parser {
namespace detail {

using StrCIter = std::string::const_iterator;
using Result = std::pair<bool, StrCIter>;

template<template <typename> class Rule>
Result parse_template(const std::string& input) {
    using StringRule = Rule<StrCIter>;
    using StringGrammar = GrammarFromRule<StringRule>;

    StringGrammar parser(true);

    auto iter = input.begin();
    auto success = qi::parse(iter, input.end(), parser);
    return {success, iter};
}

template<template <typename> class Rule>
Result parse_template(
        const std::string& input,
        typename Rule<StrCIter>::attr_type& result) {
    using StringRule = Rule<StrCIter>;
    using StringGrammar = GrammarFromRule<StringRule>;

    StringGrammar parser(true);

    auto iter = input.begin();
    auto success = qi::parse(iter, input.end(), parser, result);
    return {success, iter};
}

}} // namespace rcpt_parser::detail

#endif // RECIPIENT_PARSER_SRC_FROM_STRING_DETAIL_PARSE_TEMPLATE_HPP_
