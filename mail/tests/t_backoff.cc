#include <catch.hpp>
#include <ymod_httpclient/detail/backoff.h>

using namespace ymod_httpclient;
using namespace ymod_httpclient::detail;

struct max_generator
{
    int64_t operator()(int64_t min, int64_t max)
    {
        *last_min = min;
        *last_max = max;
        return max;
    }

    shared_ptr<int64_t> last_min = std::make_shared<int64_t>(-1);
    shared_ptr<int64_t> last_max = std::make_shared<int64_t>(-1);
};

struct t_backoff
{
    auto calc_delay(unsigned attempt_num, task_context_ptr ctx = nullptr)
    {
        backoff_impl<max_generator> backoff(settings, min_request_duration, generator);
        return backoff.calc_delay(ctx ? ctx : this->ctx, attempt_num);
    }

    auto context_with_deadline(time_traits::duration deadline_delay = time_traits::duration::zero())
    {
        auto ctx = boost::make_shared<yplatform::task_context>();
        ctx->deadline(time_traits::clock::now() + deadline_delay);
        return ctx;
    }

    auto get_milliseconds(time_traits::duration d)
    {
        return time_traits::duration_cast<time_traits::milliseconds>(d).count();
    }

    auto exponintial_delay(int attempt)
    {
        auto delay_ms = get_milliseconds(settings.base) * pow(settings.multiplier, attempt - 1);
        return time_traits::milliseconds(static_cast<int64_t>(delay_ms));
    }

    backoff_settings settings{ .base = time_traits::milliseconds(100),
                               .max = time_traits::milliseconds(1000),
                               .multiplier = 2.0 };
    time_traits::duration min_request_duration = time_traits::milliseconds(1);
    task_context_ptr ctx = boost::make_shared<yplatform::task_context>();
    max_generator generator;
};

TEST_CASE_METHOD(t_backoff, "first attempt without delay")
{
    REQUIRE(calc_delay(0) == time_traits::duration::zero());
    // ensure no random involved on first attempt
    REQUIRE(*generator.last_min == -1);
    REQUIRE(*generator.last_max == -1);
}

TEST_CASE_METHOD(t_backoff, "second attempt uses base_delay as max limit")
{
    REQUIRE(calc_delay(1) == settings.base);
    // ensure correct lower bound
    REQUIRE(*generator.last_min == 0);
}

TEST_CASE_METHOD(t_backoff, "delay max limit grows exponentially with attempt increment")
{
    for (int i = 1; i < 5; i++)
    {
        REQUIRE(calc_delay(i) == exponintial_delay(i));
        // ensure correct lower bound
        REQUIRE(*generator.last_min == 0);
    }
}

TEST_CASE_METHOD(t_backoff, "delay max limit not exceed settings max limit")
{
    REQUIRE(calc_delay(10) == settings.max);
}

TEST_CASE_METHOD(t_backoff, "delay max limit not exceed context deadline")
{
    auto small_delay = time_traits::milliseconds(200);
    REQUIRE(calc_delay(10, context_with_deadline(small_delay)) < small_delay);
}

TEST_CASE_METHOD(t_backoff, "no delay if context deadline reached")
{
    REQUIRE(calc_delay(10, context_with_deadline()) == time_traits::duration::zero());
}

TEST_CASE_METHOD(t_backoff, "no overflow on big attempt number")
{
    REQUIRE(calc_delay(70) == settings.max);
}
