#include "../src/api/subscribe.h"
#include <catch.hpp>
#include <vector>

using yxiva::local_id_t;

string correct_range(local_id_t bottom, local_id_t top, local_id_t in_pos, unsigned in_count)
{
    auto pos = in_pos;
    auto count = in_count;
    yxiva::hub::api::detail::correct_range(bottom, top, pos, count);
    return std::to_string(pos) + " " + std::to_string(count);
}

TEST_CASE("correct_subscribe_range", "")
{
    using ranges_t = std::vector<string>;
    ranges_t results = {
        correct_range(2, 6, 1, 3), correct_range(2, 6, 1, 1), correct_range(2, 6, 3, 1),
        correct_range(2, 6, 3, 2), correct_range(2, 6, 4, 3), correct_range(2, 6, 7, 1),
        correct_range(2, 6, 1, 6), correct_range(2, 6, 0, 3), correct_range(2, 6, 0, 5),
        correct_range(2, 6, 0, 4), correct_range(2, 6, 2, 4), correct_range(0, 0, 0, 0),
        correct_range(0, 0, 0, 5), correct_range(0, 0, 5, 5), correct_range(0, 0, 5, 0),
    };
    ranges_t expected = {
        "3 3", "5 1", "5 1", "4 2", "4 2", "6 0", "1 5", "3 3",
        "1 5", "2 4", "2 4", "0 0", "0 0", "0 0", "0 0",
    };

    REQUIRE(results == expected);
}
