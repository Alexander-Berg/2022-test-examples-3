#include "t_operations.h"

#include <catch.hpp>
#include <boost/asio/yield.hpp>

using namespace xeno;

template <typename Environment>
void num_appender::operator()(Environment&& env, error /*err*/)
{
    auto wrapped = wrap(env, *this, uninterruptible);
    vec_.async_push_back(num_, wrapped);
}

template <typename Environment>
void num_appender::operator()(Environment&& env, error /*err*/, bool result)
{
    if (!result)
    {
        if (--retries_ > 0)
        {
            (*this)(std::forward<Environment>(env));
        }
        else
        {
            env(code::need_restart);
        }
    }
    else
    {
        env();
    }
}

template <typename Environment>
void cycle_appender::operator()(Environment&& env, error /*err*/)
{
    reenter(*this)
    {
        while (current_ < to_)
        {
            yield num_appender(vec_, current_)(wrap(env, *this));
            ++current_;
        }
    }

    if (is_complete())
    {
        env(code::ok);
    }
}

template <typename Environment, typename... Args>
void main::handle_operation_interrupt(error error, Environment&& env, Args&&... args)
{
    ++interruptions_count_;
    try
    {
        if (error)
        {
            errors_.push_back(error);

            env.mark_handled();
            run();
        }
        else
        {
            auto& handler = env.get_operation_handler();
            handler(error, std::forward<Args>(args)...);
        }
    }
    catch (const std::exception& e)
    {
    }
};

void main::handle_operation_finish(iteration_stat_ptr /*stat*/, error error)
{
    if (error)
    {
        errors_.push_back(error);
    }
}

void main::run()
{
    std::weak_ptr<main> weak_this = shared_from_this();
    xeno::iteration_stat_ptr stat = std::make_shared<xeno::iteration_stat>();
    auto opHandler = [stat, weak_this](error error) {
        auto main = weak_this.lock();
        if (main) main->handle_operation_finish(stat, error);
    };
    auto env = xeno::make_env<
        main,
        decltype(opHandler),
        ext_mb::ext_mailbox_mock_ptr,
        loc_mb::loc_mailbox_mock_ptr>(
        io, ctx, YGLOBAL_LOGGER, stat, weak_this, std::move(opHandler));
    cycle_appender(from_, to_, vec_)(std::move(env));
}

TEST_CASE("operations")
{
    boost::asio::io_service io;
    auto ctx = boost::make_shared<xeno::context>();
    async_vector vec;
    auto m = std::make_shared<main>(1, 10, vec, &io, ctx);
    {
        m->run();
        io.run();
    }

    auto data = std::vector<int>{ 1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    REQUIRE(vec.get_data() == data);

    auto tries = std::vector<int>{ 1, 2, 3, 4, 5, 5, 5, 1, 2, 3, 4, 5, 5, 6, 7, 8, 9 };
    REQUIRE(vec.get_tries() == tries);

    auto errors = std::vector<error>{ code::need_restart };
    REQUIRE(m->get_errors() == errors);
    REQUIRE(m->get_interruptions_count() == 15);
}
