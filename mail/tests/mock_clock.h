#include <chrono>

struct mock_clock
{
    typedef std::chrono::system_clock::time_point time_point;
    static time_point tp_now;

    static time_point now()
    {
        return tp_now;
    }

    static void set_now(time_t t)
    {
        tp_now = std::chrono::system_clock::from_time_t(t);
    }

    static void wait(std::chrono::system_clock::duration delay)
    {
        tp_now += delay;
    }

    static time_t to_time_t(const time_point& t)
    {
        return std::chrono::system_clock::to_time_t(t);
    }

    static time_point from_time_t(time_t t)
    {
        return std::chrono::system_clock::from_time_t(t);
    }
};
