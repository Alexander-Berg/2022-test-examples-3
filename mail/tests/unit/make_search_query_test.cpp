#include <gtest/gtest.h>
#include <boost/make_shared.hpp>
#include <boost/algorithm/string/join.hpp>
#include "../../src/processor/make_search_helper.hpp"

namespace furita {

TEST(MakeSearchQuery, make_search_query_from_matches) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "from",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::MATCHES,
        0)
    );

    EXPECT_EQ("(hdr_from_email:\"test\" OR hdr_from_display_name:\"test\")",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_from_not_matches) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "from",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::MATCHES,
        1)
    );

    EXPECT_EQ("uid:2525 AND NOT (hdr_from_email:\"test\" OR hdr_from_display_name:\"test\")",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_from_contains) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "from",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("hdr_from_keyword:*test*",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_from_contains_cyrillic_letters) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "from",
        "Иван Иванович",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("hdr_from_keyword:*Иван\\ Иванович*",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_from_contains_cyrillic_yo_letters) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "from",
        "Фёдор Ёмануэльевич",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("hdr_from_keyword:*Фёдор\\ Ёмануэльевич*",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_from_contains_tabs) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "from",
        "test\tmy\t\ttest",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("hdr_from_keyword:*test\\\tmy\\\t\\\ttest*",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_tocc_matches) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "tocc",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::MATCHES,
        0)
    );

    EXPECT_EQ("(hdr_to_email:\"test\" OR hdr_to_display_name:\"test\" OR hdr_cc_email:\"test\" OR hdr_cc_display_name:\"test\")",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_tocc_contains) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "tocc",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("(hdr_to_keyword:*test* OR hdr_cc_keyword:*test*)",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_subject_matches) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "subject",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::MATCHES,
        0)
    );

    EXPECT_EQ("hdr_subject:test",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_subject_contains) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "subject",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("hdr_subject_keyword:*test*",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_subject_contains_with_asterisk_should_be_double_escaped) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "subject",
        "test*test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("hdr_subject_keyword:*test\\\\*test*",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_subject_contains_with_question_should_be_double_escaped) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "subject",
        "test?test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("hdr_subject_keyword:*test\\\\?test*",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_subject_contains_with_other_ctrl_should_be_escaped_once) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "subject",
        R"(test\"'!^(){}[]:~-+ test)",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ(R"(hdr_subject_keyword:*test\\\"\'\!\^\(\)\{\}\[\]\:\~\-\+\ test*)",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_body) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "body",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("(body_text:\"test\" OR pure_body:\"test\")",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_body_with_cyrillic_letters) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "body",
        "Здарова, Михалыч!",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("(body_text:\"Здарова,\\ Михалыч\\!\" OR pure_body:\"Здарова,\\ Михалыч\\!\")",
              processor::msq::make_search_query(rule, "2525"));
}


TEST(MakeSearchQuery, make_search_query_body_with_quotes_and_slashes) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "body",
        "test with quotes (\") and slashes (\\) and pluses (+)",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("(body_text:\"test\\ with\\ quotes\\ \\(\\\"\\)\\ and\\ slashes\\ \\(\\\\\\)\\ and\\ pluses\\ \\(\\+\\)\" OR pure_body:\"test\\ with\\ quotes\\ \\(\\\"\\)\\ and\\ slashes\\ \\(\\\\\\)\\ and\\ pluses\\ \\(\\+\\)\")",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_filename_matches) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "filename",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::MATCHES,
        0)
    );

    EXPECT_EQ("attachname:test",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_filename_contains) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "filename",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("(attachname:*test* OR attachname_keyword:*test*)",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_header_matches) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "ZZZ-Mail",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::MATCHES,
        0)
    );

    EXPECT_EQ("headers:\"ZZZ-Mail: test\"",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_two_headers_matches_positive_first) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
            "ZZZ-Mail",
            "test",
            rules::condition::link_type::AND,
            rules::condition::oper_type::MATCHES,
            0)
    );
    rule->conditions->push_back(boost::make_shared<rules::condition>(
            "YYY-Mail",
            "woow",
            rules::condition::link_type::AND,
            rules::condition::oper_type::MATCHES,
            1)
    );

    EXPECT_EQ("headers:\"ZZZ-Mail: test\" AND NOT headers:\"YYY-Mail: woow\" AND NOT hid:0",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_two_headers_matches_negative_first) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
            "ZZZ-Mail",
            "test",
            rules::condition::link_type::AND,
            rules::condition::oper_type::MATCHES,
            1)
    );
    rule->conditions->push_back(boost::make_shared<rules::condition>(
            "YYY-Mail",
            "woow",
            rules::condition::link_type::AND,
            rules::condition::oper_type::MATCHES,
            0)
    );

    EXPECT_EQ("uid:2525 AND NOT headers:\"ZZZ-Mail: test\" AND NOT hid:0 AND headers:\"YYY-Mail: woow\"",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_header_contains) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "ZZZ-Mail",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0)
    );

    EXPECT_EQ("headers:ZZZ-Mail\\:\\ *test*",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_header_not_contains) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "ZZZ-Mail",
        "test",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        1)
    );

    EXPECT_EQ("uid:2525 AND NOT headers:ZZZ-Mail\\:\\ *test* AND NOT hid:0",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_header_exists) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
        "ZZZ-Mail",
        "",
        rules::condition::link_type::AND,
        rules::condition::oper_type::EXISTS,
        0)
    );

    EXPECT_EQ("headers:ZZZ-Mail\\:*",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_type_matches) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rules::condition_ptr cond = boost::make_shared<rules::condition>(
        "",
        "17",
        rules::condition::link_type::AND,
        rules::condition::oper_type::MATCHES,
        0);
    cond->field_type = "type";

    rule->conditions->push_back(cond);

    EXPECT_EQ("message_type:17",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_spam_flag) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rules::condition_ptr cond = boost::make_shared<rules::condition>(
            "spam",
            "",
            rules::condition::link_type::AND,
            rules::condition::oper_type::MATCHES,
            0);
    cond->field_type = "flag";
    rule->conditions->push_back(cond);

    EXPECT_EQ("folder_type: spam",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_nospam_flag) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rules::condition_ptr cond = boost::make_shared<rules::condition>(
            "nospam",
            "",
            rules::condition::link_type::AND,
            rules::condition::oper_type::MATCHES,
            0);
    cond->field_type = "flag";
    rule->conditions->push_back(cond);

    EXPECT_EQ("uid:2525 AND NOT folder_type: spam",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_nospam_flag_or_subject) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    rule->conditions->push_back(boost::make_shared<rules::condition>(
            "subject",
            "Hate",
            rules::condition::link_type::OR,
            rules::condition::oper_type::MATCHES,
            0)
    );
    rule->conditions->push_back(boost::make_shared<rules::condition>(
            "subject",
            "Love",
            rules::condition::link_type::OR,
            rules::condition::oper_type::MATCHES,
            0)
    );
    rules::condition_ptr cond = boost::make_shared<rules::condition>(
            "nospam",
            "",
            rules::condition::link_type::AND,
            rules::condition::oper_type::MATCHES,
            0);
    cond->field_type = "flag";
    rule->conditions->push_back(cond);

    EXPECT_EQ("(hdr_subject:Hate OR hdr_subject:Love) AND NOT folder_type: spam",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_ya_systype_matches) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    auto cond = boost::make_shared<rules::condition>(
        "ya_systype",
        "17",
        rules::condition::link_type::AND,
        rules::condition::oper_type::MATCHES,
        0);
    cond->field_type = "flag";
    rule->conditions->push_back(cond);

    EXPECT_EQ("message_type:17",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_ya_systype_contains_throws) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    auto cond = boost::make_shared<rules::condition>(
        "ya_systype",
        "17",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0);
    cond->field_type = "flag";
    rule->conditions->push_back(cond);

    EXPECT_THROW(processor::msq::make_search_query(rule, "2525"), std::runtime_error);
}

TEST(MakeSearchQuery, make_search_query_ya_syslabel_matches) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    auto cond = boost::make_shared<rules::condition>(
        "ya_syslabel",
        "vtnrf0vkcom",
        rules::condition::link_type::AND,
        rules::condition::oper_type::MATCHES,
        0);
    cond->field_type = "flag";
    rule->conditions->push_back(cond);

    EXPECT_EQ("domain_label:vtnrf0vkcom",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_ya_syslabel_contains) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    auto cond = boost::make_shared<rules::condition>(
        "ya_syslabel",
        "vtnrf0vkcom",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0);
    cond->field_type = "flag";
    rule->conditions->push_back(cond);

    EXPECT_EQ("domain_label:vtnrf0vkcom",
              processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_trim_address_fields) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    const std::vector<std::string> address_fields{
        "to",
        "cc",
        "from",
        "tocc"
    };
    std::vector<std::string> expected;
    for (const auto& field: address_fields) {
        auto cond = boost::make_shared<rules::condition>(
            field,
            "  " + field + "@domain    ",
            rules::condition::link_type::AND,
            rules::condition::oper_type::CONTAINS,
            0);
        rule->conditions->push_back(cond);
        if (field != "tocc") {
            expected.emplace_back("hdr_" + field + "_keyword:*" + field + "@domain*");
        } else {
            expected.emplace_back("(hdr_to_keyword:*" + field + "@domain* OR hdr_cc_keyword:*" + field + "@domain*)");
        }
    }
    auto expected_query = boost::join(expected, " AND ");

    EXPECT_EQ(expected_query, processor::msq::make_search_query(rule, "2525"));
}

TEST(MakeSearchQuery, make_search_query_dont_trim_nonaddress_fields) {
    rules::rule_ptr rule = boost::make_shared<rules::rule>();
    rule->conditions = boost::make_shared<rules::condition_list>();

    auto cond = boost::make_shared<rules::condition>(
        "subject",
        "  subj ",
        rules::condition::link_type::AND,
        rules::condition::oper_type::CONTAINS,
        0);
    rule->conditions->push_back(cond);

    EXPECT_EQ("hdr_subject_keyword:*\\ \\ subj\\ *", processor::msq::make_search_query(rule, "2525"));
}

}
