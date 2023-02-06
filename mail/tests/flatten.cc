#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <logdog/detail/flatten.h>

#include <boost/hana/ext/std/tuple.hpp>

#include <tuple>
#include <functional>

namespace {

using namespace logdog;
namespace hana = boost::hana;

constexpr auto v1 = 9;
constexpr auto t1 = std::make_tuple(1, 2, 3);
constexpr auto t2 = hana::make_tuple(4, 5);
constexpr auto t3 = std::make_tuple(t2, 6, 7, 8);
constexpr auto tuple = std::tie(t1, t3, v1);

static_assert(
    flatten::detail::get(tuple, flatten::detail::advance_to_value(flatten::detail::pointer(tuple)))
    ==
    1
, "should point to the first value");

static_assert(
    flatten::view(tuple)
    ==
    hana::make_tuple(1, 2, 3, 4, 5, 6, 7, 8, 9)
, "");

static_assert(std::is_same_v<
    decltype(flatten::view(tuple)),
    hana::tuple<const int&, const int&, const int&, const int&, const int&, const int&, const int&, const int&, const int&>>,
    "flatten::view should store references");

static_assert(!std::is_same_v<
    decltype(flatten::view(tuple)),
    hana::tuple<int, int, int, int, int, int, int, int, int>>,
    "flatten::view should not store values");

static_assert(std::is_same_v<
    decltype(flatten::copy(tuple)),
    hana::tuple<int, int, int, int, int, int, int, int, int>>,
    "flatten::copy should store values");

static_assert(
    flatten::copy(tuple)
    ==
    hana::make_tuple(1, 2, 3, 4, 5, 6, 7, 8, 9)
, "");

}
