#define CATCH_CONFIG_MAIN
#include "progressive_retrier.h"
#include <catch.hpp>

using namespace pipeline;

static boost::asio::io_service io;

ProgressiveRetrier::intervals_t make_intervals(std::size_t count)
{
    ProgressiveRetrier::intervals_t intervals;
    intervals.resize(count);
    return intervals;
}

TEST_CASE("progressive_retrier", "no intervals")
{
    auto retrier = std::make_shared<ProgressiveRetrier>(io, make_intervals(0U));
    REQUIRE(retrier->is_retry_started() == false);
    REQUIRE(retrier->is_last_retry_interval() == true);
}

TEST_CASE("progressive_retrier", "is_retry_started")
{
    auto retrier = std::make_shared<ProgressiveRetrier>(io, make_intervals(2U));
    REQUIRE(retrier->is_retry_started() == false);
    retrier->start([] { /*noop*/ });
    REQUIRE(retrier->is_retry_started() == false);

    for (int i = 0; i < 4; i++) {
        retrier->next_retry();
        REQUIRE(retrier->is_retry_started() == true);
    }

    retrier->reset();
    REQUIRE(retrier->is_retry_started() == false);
}

TEST_CASE("progressive_retrier", "is_last_retry_interval")
{
    auto retrier = std::make_shared<ProgressiveRetrier>(io, make_intervals(1U));
    REQUIRE(retrier->is_last_retry_interval() == true);
    retrier->start(ProgressiveRetrier::callback_t());
    REQUIRE(retrier->is_last_retry_interval() == true);
    retrier->next_retry();
    REQUIRE(retrier->is_last_retry_interval() == true);
    retrier->next_retry();
    REQUIRE(retrier->is_last_retry_interval() == true);
    retrier->reset();
    REQUIRE(retrier->is_last_retry_interval() == true);
}
