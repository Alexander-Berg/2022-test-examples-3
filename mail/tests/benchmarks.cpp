#include <coroutine_mutex/coroutine_mutex.hpp>
#ifndef CORO_MUTEX_USE_OLD_ASIO_INTERFACE
#include <boost/asio/executor_work_guard.hpp>
#endif

#include <gtest/gtest.h>

#include <thread>

namespace {

using namespace testing;

#ifdef CORO_MUTEX_USE_OLD_ASIO_INTERFACE
using io_context = boost::asio::io_service;
#else
using boost::asio::io_context;
#endif

class StdMutexWrapper {
public:
    using LockHandle = const void *;
    using YieldContext = boost::asio::yield_context;

    LockHandle lock(const YieldContext&) {
        impl_.lock();
        return nullptr;
    }

    void unlock(const void*) {
        impl_.unlock();
    }

private:
    std::mutex impl_;
};

struct Element {
    std::size_t id;
    std::chrono::steady_clock::time_point time;
    std::thread::id thread;
};

template <class Mutex>
std::chrono::steady_clock::duration coro_benchmark(std::size_t services_count, std::size_t coro_per_service, std::size_t iterations_per_coro) {
    const auto start = std::chrono::steady_clock::now();

    Mutex m;
    std::deque<Element> q;

    struct Service {
        std::shared_ptr<io_context> io;
        std::thread worker;

        Service(std::shared_ptr<io_context> io)
            : io(std::move(io)) {
        }
    };

    std::vector<std::shared_ptr<Service>> services;

    for (std::size_t i = 0; i < services_count; ++i) {
        services.push_back(std::make_shared<Service>(std::make_shared<io_context>()));
    }

    for (std::size_t i = 0; i < services_count; ++i) {
        const auto& service = services[i];
    #ifdef CORO_MUTEX_USE_OLD_ASIO_INTERFACE
        const auto work = std::make_shared<boost::asio::io_service::work>(*service->io);
    #else
        const auto work = boost::asio::make_work_guard(*service->io);
    #endif
        for (std::size_t j = 0; j < coro_per_service; ++j) {
            boost::asio::spawn(*service->io,
                [&m, &q, iterations_per_coro, service, work] (boost::asio::yield_context yield) {
                    for (std::size_t it = 0; it < iterations_per_coro; ++it) {
                        coro::lock_guard<Mutex> h(m, yield);
                        q.push_back(Element {
                            q.size(),
                            std::chrono::steady_clock::now(),
                            std::this_thread::get_id(),
                        });
                    }
                });
        }
    }

    for (const auto& service : services) {
        service->worker = std::thread([io = service->io] { return io->run(); });
    }

    for (const auto& service : services) {
        service->worker.join();
    }

    const auto finish = std::chrono::steady_clock::now();

    return finish - start;
}

std::chrono::steady_clock::duration threads_benchmark(std::size_t threads_count, std::size_t iterations) {
    const auto start = std::chrono::steady_clock::now();

    std::mutex m;
    std::deque<Element> q;

    std::vector<std::thread> threads;

    for (std::size_t t = 0; t < threads_count; ++t) {
        threads.emplace_back([&] {
            for (std::size_t it = 0; it < iterations; ++it) {
                m.lock();
                q.push_back(Element {
                    q.size(),
                    std::chrono::steady_clock::now(),
                    std::this_thread::get_id(),
                });
                m.unlock();
            }
        });
    }

    for (auto& thread : threads) {
        thread.join();
    }

    const auto finish = std::chrono::steady_clock::now();

    return finish - start;
}

struct BenchmarksResult {
    std::chrono::steady_clock::duration threads_time;
    std::chrono::steady_clock::duration coroutine_std_mutex_wrapper_time;
    std::chrono::steady_clock::duration coroutine_mutex_time;
};

BenchmarksResult benchmarks(std::size_t threads, std::size_t coroutines, std::size_t iterations) {
    const auto threads_time = threads_benchmark(threads, iterations / threads);
    const auto coroutine_std_mutex_wrapper_time = coro_benchmark<StdMutexWrapper>(threads, coroutines, iterations / (coroutines * threads));
    const auto coroutine_mutex_time = coro_benchmark<coro::Mutex>(threads, coroutines, iterations / (coroutines * threads));

    return BenchmarksResult {
        threads_time,
        coroutine_std_mutex_wrapper_time,
        coroutine_mutex_time,
    };
}

TEST(MutexBenchmarks, run_all) {
    std::cout << "number\tthreads_count\tcoroutines_count\titerations_count\tthreads_time\tcoroutine_std_mutex_wrapper_time\tcoroutine_mutex_time\n";
    const std::size_t iterations = 1 << 21;
    std::size_t number = 1;
    for (std::size_t threads = 1; threads <= 8; threads *= 2) {
        for (std::size_t coroutines = 1; coroutines <= 32; coroutines *= 2) {
            const auto result = benchmarks(threads, coroutines, iterations);
            std::cout << (number++) << '\t'
                << threads << '\t'
                << coroutines << '\t'
                << iterations << '\t'
                << result.threads_time.count() << '\t'
                << result.coroutine_std_mutex_wrapper_time.count() << '\t'
                << result.coroutine_mutex_time.count() << '\n';
        }
    }
}

} // namespace
