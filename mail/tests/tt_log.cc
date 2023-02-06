#include "src/log.cc"
#include "rands.hpp"
#include <yplatform/util/ntimer.h>
#include <catch.hpp>

using namespace multipaxos;

value_t make_value(std::string const& s)
{
    value_t v = std::make_shared<value_t>();
    v->assign(s.begin(), s.end());
    return v;
}

const int submits = 200000;

value_t v1 = make_value("AA");
value_t v2 = make_value("BB");

void func()
{
    for (int i = 0; i < submits / 3; ++i)
    {
        submit(v1);
    }
}

void run_pool()
{
    while (true)
    {
        try
        {
            pool->run();
            break;
        }
        catch (std::exception& e)
        {
            ;
        }
    }
}

int main()
{
    asio::io_service local_pool;
    pool = &local_pool;

    prepared = true;
    master = true;

    //    asio::io_service::work work(local_pool);
    yplatform::util::ntimer_t timer;

    timer.start();
    for (int i = 0; i < submits; ++i)
    {
        //        submit(v1);
        //        pool->run_one();
        pool->post(boost::bind(submit, v1));
    }
    boost::thread t1(run_pool);
    t1.join();
    //    t3.join();
    //    pool->run();

    local_pool.stop();
    auto time = timer.stop();

    std::cout << submits / (time / 1000000000.0) << " SPS" << std::endl;
    std::cout << "current: " << current << " window_end: " << window_end << " end: " << end
              << std::endl;

    return 0;
}
