#include <sintimers/test.h>
#include <utility>
namespace timers {

test_timer::test_timer(test_queue * owner) : owner_(owner)
{
}

void test_timer::async_wait(const std::chrono::steady_clock::duration & ainterval,
    const timer_callback & cb)
{
    owner_->timers.push_back(std::make_pair(ainterval, cb));
}

void test_timer::cancel()
{
    ;
}

timers::timer_ptr test_queue::create_timer()
{
    return boost::make_shared<test_timer>(this);
}

size_t test_queue::size()
{
    return timers.size();
}

}
