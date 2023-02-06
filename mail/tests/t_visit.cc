#include <yplatform/util/visit.h>
#include <catch.hpp>
#include <variant>
#include <memory>

enum
{
    t_int,
    t_double
};

enum
{
    lvalue_ref,
    rvalue_ref
};

TEST_CASE("visit/should_call_corresponding_handler")
{
    std::variant<int, double> dummy = double{};
    auto matched_type = yplatform::visit(
        dummy, [](int) { return t_int; }, [](double) { return t_double; });
    REQUIRE(matched_type == t_double);
}

TEST_CASE("visit/should_call_corresponding_handler_from_lvalue")
{
    std::variant<int> dummy;
    auto matched_type = yplatform::visit(
        dummy, [](int&) { return lvalue_ref; }, [](int&&) { return rvalue_ref; });
    REQUIRE(matched_type == lvalue_ref);
}

TEST_CASE("visit/should_call_corresponding_handler_from_xvalue")
{
    std::variant<int> dummy;
    auto matched_type = yplatform::visit(
        std::move(dummy), [](int&) { return lvalue_ref; }, [](int&&) { return rvalue_ref; });
    REQUIRE(matched_type == rvalue_ref);
}

TEST_CASE("visit/should_call_corresponding_handler_from_prvalue")
{
    auto matched_type = yplatform::visit(
        std::variant<int>{}, [](int&) { return lvalue_ref; }, [](int&&) { return rvalue_ref; });
    REQUIRE(matched_type == rvalue_ref);
}

TEST_CASE("visit/should_allow_to_modify_source_value")
{
    const int DEFAULT_VALUE = 0, NEW_VALUE = 1;
    std::variant<int> dummy(DEFAULT_VALUE);
    yplatform::visit(dummy, [](int& dummy_ref) { dummy_ref = NEW_VALUE; });
    REQUIRE(std::get<int>(dummy) == NEW_VALUE);
}

TEST_CASE("visit/should_not_create_copies")
{
    std::variant<std::unique_ptr<int>> dummy = std::make_unique<int>();
    yplatform::visit(std::move(dummy), [](std::unique_ptr<int>&& dummy) {
        std::unique_ptr<int> new_dummy = std::move(dummy);
    });
    REQUIRE(!std::get<0>(dummy));
}
