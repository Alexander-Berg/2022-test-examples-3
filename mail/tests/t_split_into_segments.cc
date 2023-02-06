#include <send/split_into_segments.h>
#include <catch.hpp>

namespace fan {

typedef vector<int> src_t;
typedef vector<vector<int>> res_t;

vector<int> make_iota_vector(size_t size)
{
    src_t res(size);
    std::iota(res.begin(), res.end(), 0);
    return res;
}

TEST_CASE("split_into_segments/empty_src_produces_empty_result")
{
    REQUIRE(split_into_segments(make_iota_vector(0), 2) == res_t{});
}

TEST_CASE("split_into_segments/doesnt_produce_empty_segments")
{
    REQUIRE(split_into_segments(make_iota_vector(1), 2) == res_t{ { 0 } });
}

TEST_CASE("split_into_segments/can_produce_equal_segments")
{
    REQUIRE(split_into_segments(make_iota_vector(2), 2) == res_t{ { 0 }, { 1 } });
    REQUIRE(split_into_segments(make_iota_vector(4), 2) == res_t{ { 0, 1 }, { 2, 3 } });
}

TEST_CASE("split_into_segments/produces_not_more_than_max_segments_count")
{
    REQUIRE(split_into_segments(make_iota_vector(3), 2) == res_t{ { 0, 1 }, { 2 } });
    REQUIRE(split_into_segments(make_iota_vector(5), 2) == res_t{ { 0, 1, 2 }, { 3, 4 } });
}

}
