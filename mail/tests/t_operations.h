#pragma once

#include "mailbox_mocks.h"
#include "common.h"

#include <src/common/context.h>
#include <src/xeno/operations/environment.h>

#include <boost/asio/io_service.hpp>
#include <boost/asio/coroutine.hpp>
#include <vector>

using error = xeno::error;

// Just a test example class that emulates async push_back operations
class async_vector
{
    static const int error_number = 5;
    static const int tries_count = 5;

public:
    async_vector()
    {
    }

    template <typename Handler>
    void async_push_back(int value, Handler handler)
    {
        tries_.push_back(value);
        auto success = true;

        if (value != error_number)
        {
            data_.push_back(value);
        }
        else if (errors_counter_ > 1)
        {
            --errors_counter_;
            success = false;
        }
        else
        {
            data_.push_back(value);
        }

        handler(xeno::code::ok, success);
    }

    const std::vector<int>& get_data()
    {
        return data_;
    }
    const std::vector<int>& get_tries()
    {
        return tries_;
    }

private:
    std::vector<int> data_;
    std::vector<int> tries_;

    int errors_counter_ = tries_count;
};

class num_appender
{
    using op_error = error;

public:
    num_appender(async_vector& vec, int num) : vec_(vec), num_(num)
    {
    }

    template <typename Environment>
    void operator()(Environment&& env, op_error error = op_error());
    template <typename Environment>
    void operator()(Environment&& env, op_error error, bool result);

private:
    async_vector& vec_;
    const int num_;

    int retries_ = 3;
};

class cycle_appender : public boost::asio::coroutine
{
    using op_error = error;

public:
    cycle_appender(int from, int to, async_vector& vec)
        : from_(std::min(from, to)), to_(std::max(from, to)), vec_(vec), current_(from)
    {
    }

    template <typename Environment>
    void operator()(Environment&& env, op_error error = op_error());

private:
    const int from_;
    const int to_;
    async_vector& vec_;

    int current_;
};

class main : public std::enable_shared_from_this<main>
{
    template <typename OpHandler>
    using env_t = xeno::interruptible_environment<
        main,
        OpHandler,
        ext_mb::ext_mailbox_mock_ptr,
        loc_mb::loc_mailbox_mock_ptr>;
    using op_error = error;

public:
    main(int from, int to, async_vector& vec, boost::asio::io_service* io, xeno::context_ptr ctx)
        : from_(from), to_(to), vec_(vec), io(io), ctx(ctx)
    {
    }

    template <typename Environment, typename... Args>
    void handle_operation_interrupt(op_error error, Environment&& env, Args&&... args);

    void handle_operation_finish(xeno::iteration_stat_ptr stat, op_error err);

    void run();

    const std::vector<op_error>& get_errors()
    {
        return errors_;
    }

    int get_interruptions_count()
    {
        return interruptions_count_;
    }

private:
    const int from_;
    const int to_;
    async_vector& vec_;
    boost::asio::io_service* io;
    xeno::context_ptr ctx;

    std::vector<op_error> errors_;
    int interruptions_count_ = 0;
};
