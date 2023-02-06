#include "../src/filter/parser/decode_filter_v1.h"
#include "../src/filter/parser/decode_filter_v2.h"
#include <yxiva/core/filter/filter_set.h>
#include <yxiva/core/filter/parse.h>
#include <yxiva/core/message.h>
#include <catch.hpp>

using namespace yxiva;

using filter::OP_NOT;
using filter::OP_AND;

// #define DEBUG_TESTS

void check_expression_parser_fail(const char* input)
{
    filter::parser::expression_parser parser;
#ifdef DEBUG_TESTS
    std::cout << "input: " << input << std::endl;
#endif
    REQUIRE(parser(input) == false);
}

void check_expression_parser_ok(
    const char* input,
    const std::vector<filter::operator_t>& operators,
    const std::vector<std::string>& operands,
    const std::set<std::string>& vars_names)
{
    filter::parser::expression_parser parser;
    REQUIRE(parser(input) == true);
    filter::expression expr;
    std::set<std::string> names;
    parser.move_result_to(expr, names);
#ifdef DEBUG_TESTS
    std::cout << "input: " << input << std::endl;
    std::cout << "operators: ";
    for (auto& op : expr.operators)
        std::cout << (op == OP_AND ? '&' : '!') << " ";
    std::cout << "\n";
    std::cout << "expected.operators: ";
    for (auto& op : operators)
        std::cout << (op == OP_AND ? '&' : '!') << " ";
    std::cout << "\n";
    std::cout << "operands: ";
    for (auto& op : expr.operands)
        std::cout << op << " ";
    std::cout << "\n";
    std::cout << "expected.operands: ";
    for (auto& op : operands)
        std::cout << op << " ";
    std::cout << "\n";
#endif
    CHECK(expr.operators == std::vector<filter::operator_t>(operators));
    CHECK(expr.operands == std::vector<std::string>(operands));

    REQUIRE(names == vars_names);
}

TEST_CASE("filters/parser/expression_parser/empty", "")
{
    filter::parser::expression_parser parser;
    REQUIRE(parser("") == true);
    REQUIRE(parser("    ") == true);
    REQUIRE(parser("\t\t") == true);
}

TEST_CASE("filters/parser/expression_parser/no_spaces", "ok, without spaces")
{
    check_expression_parser_ok("A", {}, { "A" }, { "A" });
    check_expression_parser_ok("A&!B", { OP_AND, OP_NOT }, { "A", "B" }, { "A", "B" });
    check_expression_parser_ok("!A&B", { OP_NOT, OP_AND }, { "A", "B" }, { "A", "B" });
    check_expression_parser_ok(
        "A&B&C&D&A&B", { 5UL, OP_AND }, { "A", "B", "C", "D", "A", "B" }, { "A", "B", "C", "D" });
    check_expression_parser_ok(
        "A&!B&C&!D&A&!B",
        { OP_AND, OP_NOT, OP_AND, OP_AND, OP_NOT, OP_AND, OP_AND, OP_NOT },
        { "A", "B", "C", "D", "A", "B" },
        { "A", "B", "C", "D" });
    check_expression_parser_ok(
        "!A&!B&!C&!D&!A&!B",
        { OP_NOT, OP_AND, OP_NOT, OP_AND, OP_NOT, OP_AND, OP_NOT, OP_AND, OP_NOT, OP_AND, OP_NOT },
        { "A", "B", "C", "D", "A", "B" },
        { "A", "B", "C", "D" });
}

TEST_CASE("filters/parser/expression_parser/spaces", "ok, with spaces")
{
    check_expression_parser_ok("  A", {}, { "A" }, { "A" });
    check_expression_parser_ok("  A   ", {}, { "A" }, { "A" });
    check_expression_parser_ok("A   ", {}, { "A" }, { "A" });
    check_expression_parser_ok("A & !B", { OP_AND, OP_NOT }, { "A", "B" }, { "A", "B" });
    check_expression_parser_ok("!A & B", { OP_NOT, OP_AND }, { "A", "B" }, { "A", "B" });
    check_expression_parser_ok(
        "A & B & C & D & A & B",
        { 5UL, OP_AND },
        { "A", "B", "C", "D", "A", "B" },
        { "A", "B", "C", "D" });
    check_expression_parser_ok(
        "A & !B & C & !D & A & !B",
        { OP_AND, OP_NOT, OP_AND, OP_AND, OP_NOT, OP_AND, OP_AND, OP_NOT },
        { "A", "B", "C", "D", "A", "B" },
        { "A", "B", "C", "D" });
    check_expression_parser_ok(
        "!A & !B & !C & !D & !A & !B",
        { OP_NOT, OP_AND, OP_NOT, OP_AND, OP_NOT, OP_AND, OP_NOT, OP_AND, OP_NOT, OP_AND, OP_NOT },
        { "A", "B", "C", "D", "A", "B" },
        { "A", "B", "C", "D" });
}

TEST_CASE("filters/parser/expression_parser/var_names", "ok, long variable names")
{
    check_expression_parser_ok("VAR123", {}, { "VAR123" }, { "VAR123" });
    check_expression_parser_ok("Transform", {}, { "Transform" }, { "Transform" });
    check_expression_parser_ok(
        "Transform & mIce", { OP_AND }, { "Transform", "mIce" }, { "Transform", "mIce" });
    check_expression_parser_ok(
        "No & cheese & for & you",
        { 3UL, OP_AND },
        { "No", "cheese", "for", "you" },
        { "No", "cheese", "for", "you" });
}

TEST_CASE("filters/parser/expression_parser/bad_var_names", "invalid var name")
{
    check_expression_parser_fail("1");
    check_expression_parser_fail("01293");
    check_expression_parser_fail("1AVD");
    check_expression_parser_fail("_Abc_A");
    check_expression_parser_fail("_");
    check_expression_parser_fail("A & _");
    check_expression_parser_fail("B & _ & A");
}

TEST_CASE("filters/parser/expression_parser/invalid_sequence/undefined_variables", "")
{
    check_expression_parser_fail("A B C");
    check_expression_parser_fail("A & B C");
    check_expression_parser_fail("A & B C & D");
    check_expression_parser_fail("A & Bddd C & D");
}

TEST_CASE("filters/parser/expression_parser/invalid_sequence/unknown_operators", "")
{
    check_expression_parser_fail("& A");
    check_expression_parser_fail("A &");
    check_expression_parser_fail("&");
    check_expression_parser_fail("&&&");
    check_expression_parser_fail("!");
    check_expression_parser_fail("!!!&");
    check_expression_parser_fail("&!!!");
    check_expression_parser_fail("A & & B");
}
