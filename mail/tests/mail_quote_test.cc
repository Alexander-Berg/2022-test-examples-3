#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/mail_quote.h>
#include <internal/mail_quote/parser.h>
#include <internal/mail_quote/tokenizer.h>

#include "mail_quote_test/verified_visitor.h"
#include "mail_quote_test/verified_paragraph_visitor.h"

namespace msg_body {

using namespace testing;
using namespace mail_quote;

typedef Test MailQuoteTokenizerTest;

namespace mail_quote {

std::ostream& operator <<(std::ostream& stream, const Token::Type& tokenType) {
    switch (tokenType) {
        case Token::Type::Ampersand:
            return stream << "Ampersand";
        case Token::Type::CarriageReturn:
            return stream << "CarriageReturn";
        case Token::Type::GreaterThan:
            return stream << "GreaterThan";
        case Token::Type::LessThan:
            return stream << "LessThan";
        case Token::Type::LineDelimiter:
            return stream << "LineDelimiter";
        case Token::Type::Space:
            return stream << "Space";
        case Token::Type::Symbols:
            return stream << "Symbols";
        case Token::Type::Tab:
            return stream << "Tab";
        case Token::Type::SignatureSeparator:
            return stream << "SignatureSeparator";
        default:
            return stream << "Unknown";
    }
}

std::ostream& operator <<(std::ostream& stream, const Token& token) {
    return stream << token.type();
}

std::ostream& operator <<(std::ostream& stream, const TokenSequence& tokenSequence) {
    std::copy(tokenSequence.begin(), tokenSequence.end(), std::ostream_iterator<Token>(stream, ", "));
    return stream;
}

std::string verifiedMailQuote(const std::string& text) {
    const auto tokens = mail_quote::tokenize(text);
    mail_quote::Parser parse(tokens.begin(), tokens.end());
    mail_quote::VerifiedParagraphVisitor paragraphVisitor(tokens.end());
    mail_quote::VerifiedVisitor visitor(paragraphVisitor);
    parse(visitor);
    auto result = visitor.result();

    hidePGP(result);

    return result;
}

}

TEST(MailQuoteTokenizerTest, empty_content_should_be_empty) {
    ASSERT_TRUE(tokenize(std::string()).empty());
}

TEST(MailQuoteTokenizerTest, content_with_ampersand_should_return_ampersand_token) {
    ASSERT_EQ(tokenize("&"), TokenSequence({Token::ampersand}));
}

TEST(MailQuoteTokenizerTest, content_with_gt_should_return_greater_than_token) {
    ASSERT_EQ(tokenize(">"), TokenSequence({Token::greaterThan}));
}

TEST(MailQuoteTokenizerTest, content_with_lt_should_return_less_than_token) {
    ASSERT_EQ(tokenize("<"), TokenSequence({Token::lessThan}));
}

TEST(MailQuoteTokenizerTest, content_with_cr_should_return_carriage_return_token) {
    ASSERT_EQ(tokenize("\r"), TokenSequence({Token::carriageReturn}));
}

TEST(MailQuoteTokenizerTest, content_with_lf_should_return_line_delimiter_token) {
    ASSERT_EQ(tokenize("\n"), TokenSequence({Token(Token::Type::LineDelimiter, "\n")}));
}

TEST(MailQuoteTokenizerTest, content_with_cr_lf_should_return_line_delimiter_token) {
    ASSERT_EQ(tokenize("\r\n"), TokenSequence({Token::lineDelimiter}));
}

TEST(MailQuoteTokenizerTest, content_with_space_should_return_space_token) {
    ASSERT_EQ(tokenize(" "), TokenSequence({Token::space}));
}

TEST(MailQuoteTokenizerTest, content_with_tab_should_return_tab_token) {
    ASSERT_EQ(tokenize("\t"), TokenSequence({Token::tab}));
}

TEST(MailQuoteTokenizerTest, content_with_not_special_symbols_should_return_tab_token) {
    ASSERT_EQ(tokenize("abc"), TokenSequence({Token(Token::Type::Symbols, "abc")}));
}

TEST(MailQuoteTokenizerTest, sequence_of_all_symbol_types_should_return_appropriate_token_sequence) {
    ASSERT_EQ(tokenize("&><\r \n\r\n\tabc"), TokenSequence({
        Token::ampersand,
        Token::greaterThan,
        Token::lessThan,
        Token::carriageReturn,
        Token::space,
        Token(Token::Type::LineDelimiter, "\n"),
        Token::lineDelimiter,
        Token::tab,
        Token(Token::Type::Symbols, "abc"),
    }));
}

TEST(MailQuoteTokinizerTest, content_with_signature_separator_should_return_signature_separator_token) {
    ASSERT_EQ(tokenize("-- \r\n"), TokenSequence({Token::signatureSeparator}));
}

TEST(MailQuoteTokinizerTest, content_with_signature_separator_without_cr_should_return_signature_separator_token) {
    ASSERT_EQ(tokenize("-- \n"), TokenSequence({Token(Token::Type::SignatureSeparator, "-- \n")}));
}

TEST(MailQuoteTokinizerTest, content_with_signature_separator_without_space_should_return_signature_separator_token) {
    ASSERT_EQ(tokenize("--\r\n"), TokenSequence({Token(Token::Type::SignatureSeparator, "--\r\n")}));
}

TEST(MailQuoteTokinizerTest, content_with_signature_separator_without_space_and_cr_should_return_signature_separator_token) {
    ASSERT_EQ(tokenize("--\n"), TokenSequence({Token(Token::Type::SignatureSeparator, "--\n")}));
}

TEST(MailQuoteTokinizerTest, content_with_signature_separator_not_at_begin_of_line_should_not_return_sequence_without_signature_separator_token) {
    ASSERT_EQ(tokenize(">-- \r\n"), TokenSequence({
        Token::greaterThan,
        Token(Token::Type::Symbols, "--"),
        Token::space,
        Token::lineDelimiter,
    }));
}

typedef Test MailQuoteTest;

TEST(MailQuoteTest, hasQuotesAndPGP_MailQuote_quotesBoth) {
    const std::string unquoted = "before quot\r\n"
                                 "> quot 1\r\n"
                                 "> quot 2\r\n"
                                 "after quot, before pgp\r\n"
                                 "-----BEGIN PGP SIGNED MESSAGE-----\r\n"
                                 "pgp header to be removed\r\n"
                                 "\r\n"
                                 "pgped message body\r\n"
                                 "-----BEGIN PGP SIGNATURE-----\r\n"
                                 "signatire header\r\n"
                                 "same thing\r\n"
                                 "\r\n"
                                 "the signature itself\r\n"
                                 "-----END PGP SIGNATURE-----\r\n"
                                 "after pgp";
    const std::string quoted =   "<p>before quot<br /></p>"
                                 "<blockquote class=\"wmi-quote\">&#160;quot 1<br />"
                                 "&#160;quot 2<br /></blockquote>"
                                 "<p>after quot, before pgp<br /><br /><br />"
                                 "pgped message body<br />"
                                 "<blockquote class=\"wmi-pgp\">-----BEGIN PGP SIGNATURE-----<br />"
                                 "signatire header<br />"
                                 "same thing<br />"
                                 "<br />"
                                 "the signature itself<br />"
                                 "-----END PGP SIGNATURE-----</blockquote><br />"
                                 "after pgp<br /></p>";
    ASSERT_EQ(quoted, verifiedMailQuote(unquoted));
    ASSERT_EQ(quoted, mailQuote(unquoted));
}

TEST(MailQuoteTest, pgpInsideQuotes_MailQuote_singleQuotes) {
    const std::string unquoted =
"wertyertyety\r\n"
"цукенукенкен\r\n"
"цукнукенке\r\n"
"\r\n"
"16.10.2013 12:01, Alexandr пишет:\r\n"
"> -----BEGIN PGP SIGNED MESSAGE----- Hash: SHA1 856856785678678 C\r\n"
"> уважением, АлександрSign only -----BEGIN PGP SIGNATURE----- Version:\r\n"
"> GnuPG v1.4.12 (GNU/Linux)\r\n"
"> iQEcBAEBAgAGBQJSXivJAAoJEASLuvLLVaCkFIMH/1li2lQhgeggFYrfO8AuiAav\r\n"
"> 2eSVeAnNqgSK/Sg3FQxdW+C8z0r4JzE0+EZhToeCKOlLF0akT0nLUj3+YGbY+9U5\r\n"
"> Uzn+NC2/SgXbqz6sQLMT5JeKaLi4KKAENwPm5Pmk8u+ijIk3lKvmjM/sCQqwIryr\r\n"
"> qeHhqcqZ8Oa/VRPP9li8/FSNeGOfw/Rvl9c24SQJRInQ6h/M5lQvm4ZayyEyz9pk\r\n"
"> +DRI2dNlkmaUPDYwiMzBhLGbWHzJy4X2IKhN+h7qTtjWcUUiLzrKEpwwcMnhz27+\r\n"
"> WSe14aLBiukwj4fLs+EiO+hLpS8GRurBL1Ff/jnVSc8kIr2INKbpiEZAP2qkua8= =y5eo\r\n"
"> -----END PGP SIGNATURE-----\r\n"
;

    const std::string quoted =
"<p>"
    "wertyertyety"
    "<br />"
    "цукенукенкен"
    "<br />"
    "цукнукенке"
    "<br />"
    "<br />"
    "16.10.2013 12:01, Alexandr пишет:"
    "<br />"
"</p>"
"<blockquote class=\"wmi-quote\">"
    "&#160;-----BEGIN PGP SIGNED MESSAGE----- Hash: SHA1 856856785678678 C"
    "<br />"
    "&#160;уважением, АлександрSign only "
    "<blockquote class=\"wmi-pgp\">"
        "-----BEGIN PGP SIGNATURE----- Version:"
        "<br />"
        "&#160;GnuPG v1.4.12 (GNU/Linux)"
        "<br />"
        "&#160;iQEcBAEBAgAGBQJSXivJAAoJEASLuvLLVaCkFIMH/1li2lQhgeggFYrfO8AuiAav"
        "<br />"
        "&#160;2eSVeAnNqgSK/Sg3FQxdW+C8z0r4JzE0+EZhToeCKOlLF0akT0nLUj3+YGbY+9U5"
        "<br />"
        "&#160;Uzn+NC2/SgXbqz6sQLMT5JeKaLi4KKAENwPm5Pmk8u+ijIk3lKvmjM/sCQqwIryr"
        "<br />"
        "&#160;qeHhqcqZ8Oa/VRPP9li8/FSNeGOfw/Rvl9c24SQJRInQ6h/M5lQvm4ZayyEyz9pk"
        "<br />"
        "&#160;+DRI2dNlkmaUPDYwiMzBhLGbWHzJy4X2IKhN+h7qTtjWcUUiLzrKEpwwcMnhz27+"
        "<br />"
        "&#160;WSe14aLBiukwj4fLs+EiO+hLpS8GRurBL1Ff/jnVSc8kIr2INKbpiEZAP2qkua8= =y5eo"
        "<br />"
        "&#160;-----END PGP SIGNATURE-----"
    "</blockquote>"
    "<br />"
"</blockquote>"
;

    ASSERT_EQ(quoted, verifiedMailQuote(unquoted));
    ASSERT_EQ(quoted, mailQuote(unquoted));

}

TEST(MailQuoteTest, empty_message_should_be_empty) {
    ASSERT_TRUE(verifiedMailQuote(std::string()).empty());
    ASSERT_TRUE(mailQuote(std::string()).empty());
}

TEST(MailQuoteTest, message_with_text_should_wrap_into_p_tag_and_add_br_tag) {
    const std::string content = "text";

    const std::string expected =
            "<p>"
                "text<br />"
            "</p>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_lf_should_replace_by_br_tag) {
    const std::string content =
            "first line\r\n"
            "second line\r"
            "third line\n"
            "end";

    const std::string expected =
            "<p>"
                "first line<br />"
                "second line"
                "third line<br />"
                "end<br />"
            "</p>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_empty_text_lines_should_insert_same_number_of_br) {
    const std::string content =
            "\r\n"
            "\r\n"
            "\r\n";

    const std::string expected =
            "<p>"
                "<br />"
                "<br />"
                "<br />"
            "</p>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_one_quote_symbol_should_be_empty) {
    const std::string content = ">";
    const std::string expected;

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_only_quotation_should_wrap_into_blockquote) {
    const std::string content =
            ">quote";

    const std::string expected =
            "<blockquote class=\"wmi-quote\">"
                "quote<br />"
            "</blockquote>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_quote_should_wrap_quote_into_blockquote_tag_after_p_tag) {
    const std::string content =
            "text\r\n"
            ">quote\r\n";

    const std::string expected =
            "<p>"
                "text<br />"
            "</p>"
            "<blockquote class=\"wmi-quote\">"
                "quote<br />"
            "</blockquote>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_two_quotes_levels_should_wrap_each_quote_into_blockquote_tag_after_p_tag) {
    const std::string content =
            "text\r\n"
            ">first quote\r\n"
            ">>second quote\r\n";

    const std::string expected =
            "<p>"
                "text<br />"
            "</p>"
            "<blockquote class=\"wmi-quote\">"
                "first quote<br />"
                "<blockquote class=\"wmi-quote\">"
                    "second quote<br />"
                "</blockquote>"
            "</blockquote>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_empty_qutation_lines_should_wrap_into_blockquote_and_insert_same_number_of_br) {
    const std::string content =
            ">\r\n"
            ">\r\n"
            ">\r\n";

    const std::string expected =
            "<blockquote class=\"wmi-quote\">"
                "<br />"
                "<br />"
                "<br />"
            "</blockquote>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_xml_entities_should_encode) {
    const std::string content = "b > a && b < c";

    const std::string expected =
            "<p>"
                "b &gt; a &amp;&amp; b &lt; c<br />"
            "</p>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_leading_tabs_should_encode) {
    const std::string content =
            "\ta\r\n"
            ">\tb\r\n"
            ">>\tc\r\n";

    const std::string expected =
            "<p>"
                "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;a<br />"
            "</p>"
            "<blockquote class=\"wmi-quote\">"
                "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;b<br />"
                "<blockquote class=\"wmi-quote\">"
                    "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;c<br />"
                "</blockquote>"
            "</blockquote>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_leading_spaces_should_encode) {
    const std::string content =
            " a\r\n"
            "> b\r\n"
            ">> c\r\n";

    const std::string expected =
            "<p>"
                "&#160;a<br />"
            "</p>"
            "<blockquote class=\"wmi-quote\">"
                "&#160;b<br />"
                "<blockquote class=\"wmi-quote\">"
                    "&#160;c<br />"
                "</blockquote>"
            "</blockquote>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_signature_should_wrap_into_wmi_sign) {
    const std::string content =
            "text\r\n"
            "-- \r\n"
            "signature\r\n";

    const std::string expected =
            "<p>"
                "text<br />"
            "</p>"
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "signature<br />"
            "</span>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_only_signature_separator_should_wrap_into_wmi_sign) {
    const std::string content =
            "-- \r\n";

    const std::string expected =
            "<span class=\"wmi-sign\">"
                "-- <br />"
            "</span>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_two_signature_separators_should_wrap_into_one_wmi_sign) {
    const std::string content =
            "-- \r\n"
            "-- \r\n";

    const std::string expected =
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "-- <br />"
            "</span>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_signature_separator_without_cr_should_wrap_into_wmi_sign) {
    const std::string content =
            "-- \n"
            "signature\r\n";

    const std::string expected =
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "signature<br />"
            "</span>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_signature_separator_without_space_should_wrap_into_wmi_sign) {
    const std::string content =
            "--\r\n"
            "signature\r\n";

    const std::string expected =
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "signature<br />"
            "</span>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_signature_separator_without_space_and_cr_should_wrap_into_wmi_sign) {
    const std::string content =
            "--\n"
            "signature\r\n";

    const std::string expected =
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "signature<br />"
            "</span>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_multiline_signature_should_wrap_into_one_wmi_sign) {
    const std::string content =
            "text\r\n"
            "-- \r\n"
            "signature first line\r\n"
            "signature second line\r\n";

    const std::string expected =
            "<p>"
                "text<br />"
            "</p>"
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "signature first line<br />"
                "signature second line<br />"
            "</span>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_signature_after_quote_should_wrap_into_wmi_sign) {
    const std::string content =
            "text\r\n"
            ">quote\r\n"
            "-- \r\n"
            "signature\r\n";

    const std::string expected =
            "<p>"
                "text<br />"
            "</p>"
            "<blockquote class=\"wmi-quote\">"
                "quote<br />"
            "</blockquote>"
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "signature<br />"
            "</span>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_empty_line_after_signature_should_wrap_into_wmi_sign_before_empty_line) {
    const std::string content =
            "first text line\r\n"
            "-- \r\n"
            "signature first line\r\n"
            "signature second line\r\n"
            "\r\n"
            "second text line\r\n";

    const std::string expected =
            "<p>"
                "first text line<br />"
            "</p>"
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "signature first line<br />"
                "signature second line<br />"
            "</span>"
            "<p>"
                "<br />second text line<br />"
            "</p>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_two_signatures_separated_by_empty_line_should_wrap_each_into_wmi_sign) {
    const std::string content =
            "-- \r\n"
            "first signature\r\n"
            "\r\n"
            "-- \r\n"
            "second signature\r\n";

    const std::string expected =
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "first signature<br />"
            "</span>"
            "<p>"
                "<br />"
            "</p>"
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "second signature<br />"
            "</span>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_quote_inside_signature_should_wrap_into_wmi_sign) {
    const std::string content =
            "text\r\n"
            "-- \r\n"
            "signature first line\r\n"
            ">quote\r\n"
            "signature second line\r\n";

    const std::string expected =
            "<p>"
                "text<br />"
            "</p>"
            "<span class=\"wmi-sign\">"
                "-- <br />"
                "signature first line<br />"
                "<blockquote class=\"wmi-quote\">"
                    "quote<br />"
                "</blockquote>"
                "signature second line<br />"
            "</span>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_signature_quote_inside_should_wrap_only_quote) {
    const std::string content =
            ">-- \r\n"
            ">quoted signature\r\n";

    const std::string expected =
            "<blockquote class=\"wmi-quote\">"
                "-- <br />"
                "quoted signature<br />"
            "</blockquote>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_quote_inside_text_should_wrap_quote_inside_text_and_split_text) {
    const std::string content =
            "first text line\r\n"
            ">quote\r\n"
            "second text line\r\n";

    const std::string expected =
            "<p>"
                "first text line<br />"
            "</p>"
            "<blockquote class=\"wmi-quote\">"
                "quote<br />"
            "</blockquote>"
            "<p>"
                "second text line<br />"
            "</p>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_text_inside_quote_should_wrap_text_inside_quote_and_split_quote) {
    const std::string content =
            ">first quote line\r\n"
            "text\r\n"
            ">second quote line\r\n";

    const std::string expected =
            "<blockquote class=\"wmi-quote\">"
                "first quote line<br />"
            "</blockquote>"
            "<p>"
                "text<br />"
            "</p>"
            "<blockquote class=\"wmi-quote\">"
                "second quote line<br />"
            "</blockquote>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_second_level_quote_inside_first_level_should_wrap_second_inside_first) {
    const std::string content =
            "text\r\n"
            ">first level quote\r\n"
            ">>second level quote\r\n"
            ">again first level quote\r\n";

    const std::string expected =
            "<p>"
                "text<br />"
            "</p>"
            "<blockquote class=\"wmi-quote\">"
                "first level quote<br />"
                "<blockquote class=\"wmi-quote\">"
                    "second level quote<br />"
                "</blockquote>"
                "again first level quote<br />"
            "</blockquote>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

TEST(MailQuoteTest, message_with_third_level_quote_after_first_should_wrap_third_inside_empty_second_level) {
    const std::string content =
            "text\r\n"
            ">first level quote\r\n"
            ">>>jump to third level quote\r\n";

    const std::string expected =
            "<p>"
                "text<br />"
            "</p>"
            "<blockquote class=\"wmi-quote\">"
                "first level quote<br />"
                "<blockquote class=\"wmi-quote\">"
                    "<blockquote class=\"wmi-quote\">"
                        "jump to third level quote<br />"
                    "</blockquote>"
                "</blockquote>"
            "</blockquote>";

    ASSERT_EQ(expected, verifiedMailQuote(content));
    ASSERT_EQ(expected, mailQuote(content));
}

}
