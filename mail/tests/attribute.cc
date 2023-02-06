#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <logdog/attribute.h>

namespace {

#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-const-variable"
#endif
LOGDOG_DEFINE_ATTRIBUTE(int, test_attribute)
LOGDOG_DEFINE_ATTRIBUTE(int, test_attribute2)
#ifdef __clang__
#pragma clang diagnostic pop
#endif

TEST(has_attribute, should_return_std_true_type_if_attribute_found) {
    const auto sequence = std::make_tuple(test_attribute=0, test_attribute2=1);
    static_assert(logdog::has_attribute(sequence, test_attribute),
        "should return std::true_type");
}

TEST(has_attribute, should_return_std_false_type_if_attribute_not_found) {
    const auto sequence = std::make_tuple(test_attribute2=1);
    static_assert(!logdog::has_attribute(sequence, test_attribute),
        "should return std::false_type");
}

TEST(has_attribute, should_return_std_true_type_if_literal_attribute_found) {
    using namespace logdog::literals;
    const auto sequence = std::make_tuple(test_attribute=0, test_attribute2=1, "test_attribute3"_a=5);
    static_assert(logdog::has_attribute(sequence, "test_attribute3"_a),
        "should return std::true_type");
}

}
