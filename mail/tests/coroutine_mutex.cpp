#include <coroutine_mutex/coroutine_mutex.hpp>

#include <gtest/gtest.h>

namespace {

#ifdef CORO_MUTEX_USE_OLD_ASIO_INTERFACE
using io_context = boost::asio::io_service;
#else
using boost::asio::io_context;
#endif

using namespace testing;
using namespace coro;

struct MutexSingleIoServiceTest : public Test {
    io_context io;

    template <class Coroutine>
    void spawn(Coroutine coroutine) {
        boost::asio::spawn(io, std::forward<Coroutine>(coroutine));
    }
};

TEST_F(MutexSingleIoServiceTest, lock_unlocked_and_unlock_should_succeed) {
    using boost::asio::yield_context;
    Mutex mutex;
    spawn([&] (yield_context yield) {
        const auto h = mutex.lock(yield);
        mutex.unlock(h);
    });
    io.run();
}

TEST_F(MutexSingleIoServiceTest, lock_locked_and_release_waiting_should_succeed) {
    using boost::asio::yield_context;
    Mutex mutex;
    bool locked = false;
    spawn([&] (yield_context yield) {
        const auto h = mutex.lock(yield);
        spawn([&] (yield_context yield) {
            mutex.lock(yield);
            locked = true;
        });
        mutex.unlock(h);
    });
    io.run();
    EXPECT_TRUE(locked);
}

TEST_F(MutexSingleIoServiceTest, unlock_by_not_owner_should_throw_exception) {
    using boost::asio::yield_context;
    Mutex mutex;
    spawn([&] (yield_context yield) {
        mutex.lock(yield);
        spawn([&] (yield_context yield) {
            ASSERT_THROW(mutex.unlock(&yield), std::logic_error);
        });
    });
    io.run();
}

} // namespace
