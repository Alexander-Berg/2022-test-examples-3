#include <mail/webmail/wait_all/include/boost.h>
#include <mail/webmail/wait_all/tests/common.h>


namespace {

using namespace testing;
using boost::waitAll;
using coro::getIoContext;
using boost::asio::yield_context;


struct WaitAllBoostTest : public BaseTest {

    template<typename expected_t>
    void runTestOnYieldAndCallback(const auto& preparedWait, const expected_t& expected) {
        std::atomic_size_t callCount = 0;
        const auto token = [&](boost::system::error_code ec, auto actual) {
            ++callCount;
            EXPECT_FALSE(ec);
            if constexpr(not std::is_same_v<decltype(actual), wait_all::detail::Void>) {
                EXPECT_EQ(expected, actual);
            }
        };

        spawn([&] (yield_context yield) {
            const auto actual = preparedWait(yield);
            EXPECT_EQ(expected, actual);
            ++callCount;
        });

        preparedWait(token);

        runIO();
        EXPECT_EQ(2U, callCount);
    }
};


TEST_P(WaitAllBoostTest, intVectorVariadicOfFunctors) {
    const std::vector<int> expected = {11, 2, 3, 4, 5};
    using expected_t = std::remove_cv_t<decltype(expected)>;

    const auto preparedWait = [&](auto&& ct) {
        return waitAll<expected_t>(
            io, std::forward<decltype(ct)>(ct),
            returnInt(11),
            returnInt<2>,
            returnInt<3>,
            returnInt<4>,
            returnInt(5)
        );
    };

    runTestOnYieldAndCallback(preparedWait, expected);
}

TEST_P(WaitAllBoostTest, intVectorVectorOfFunctors) {
    const std::vector<int> expected = {11, 2, 3, 4, 5};
    using expected_t = std::remove_cv_t<decltype(expected)>;

    std::vector<std::function<int()>> functors;
    functors.reserve(expected.size());
    for (int e : expected) {
        functors.emplace_back(returnInt(e));
    }

    const auto preparedWait = [&](auto&& ct) {
        return waitAll<expected_t>(
            io, std::forward<decltype(ct)>(ct),
            functors
        );
    };

    runTestOnYieldAndCallback(preparedWait, expected);
}



TEST_P(WaitAllBoostTest, mixedTupleVariadicOfFunctors) {
    const std::tuple expected = {1, std::string{"3"}, std::vector{4,5}};
    using expected_t = std::remove_cv_t<decltype(expected)>;

    const auto preparedWait = [&](auto&& ct) {
        return waitAll<expected_t>(
            io, std::forward<decltype(ct)>(ct),
            returnInt<1>,
            [](auto y) {return sleepAndReturn<std::string>("3", y);},
            [](auto y) {return sleepAndReturn<std::vector<int>>(std::vector{4,5}, y);}
        );
    };

    runTestOnYieldAndCallback(preparedWait, expected);
}

TEST_P(WaitAllBoostTest, structVariadicOfFunctors) {
    struct expected_t {
        int i;
        std::string s;
        float f;
        auto operator<=>(const expected_t&) const = default;
    };
    const expected_t expected = {1, std::string{"3"}, 3.14};

    const auto preparedWait = [&](auto&& ct) {
        return waitAll<expected_t>(
            io, std::forward<decltype(ct)>(ct),
            returnInt<1>,
            [](auto y) {return sleepAndReturn<std::string>("3", y);},
            [](auto y) {return sleepAndReturn<float>(3.14, y);}
        );
    };

    runTestOnYieldAndCallback(preparedWait, expected);
}


TEST_P(WaitAllBoostTest, voidVariadicOfFunctors) {
    std::atomic_size_t callCount = 0;
    const auto run = [&] (yield_context y) -> void {
        EXPECT_EQ(&io, &getIoContext(y));
        ++callCount;
    };

    const auto preparedWait = [&](auto&& ct) {
        waitAll<void>(
            io, std::forward<decltype(ct)>(ct),
            run, run, run,
            returnInt<1>,
            returnInt<2>
        );
        return true;
    };
    runTestOnYieldAndCallback(preparedWait, true);
    EXPECT_EQ(6U, callCount);
}

TEST_P(WaitAllBoostTest, voidVectorOfFunctors) {
    const size_t COUNT = 3;
    std::atomic_size_t callCount = 0;
    const auto run = [&] (yield_context y) -> void {
        EXPECT_EQ(&io, &getIoContext(y));
        ++callCount;
    };
    std::vector<std::function<void(yield_context)>> functors(COUNT, run);

    const auto preparedWait = [&](auto&& ct) {
        waitAll<void>(
            io, std::forward<decltype(ct)>(ct),
            functors
        );
        return true;
    };
    runTestOnYieldAndCallback(preparedWait, true);
    EXPECT_EQ(2 * COUNT, callCount);
}


TEST_P(WaitAllBoostTest, shouldThrowException) {
    const auto run = [&] (yield_context y) -> void {
        EXPECT_EQ(&io, &getIoContext(y));
        throw std::invalid_argument("");
    };
    bool called = false;
    spawn([&] (yield_context yield) {

        try {
            waitAll<void>(
                io, yield,
                run, run, run
            );
        }
        catch (const boost::system::system_error&) {
            called = true;
        }
        catch (...) {
            EXPECT_TRUE(false);
        }

        boost::system::error_code ec;
        waitAll<void>(
            io, yield[ec],
            run, run, run
        );
        EXPECT_TRUE(ec);
    });

    io.run();
    EXPECT_TRUE(called);
}


TEST_P(WaitAllBoostTest, twoIOSShouldSucceed) {
    std::atomic_size_t callCount = 0;
    const auto run = [&] (yield_context y) -> void {
        EXPECT_EQ(&io, &getIoContext(y));
        ++callCount;
    };
    std::atomic_bool called = false;
    boost::asio::io_context second_io;

    std::thread th1([&]() {
        while(!called) {
            second_io.reset();
            second_io.run();
        }
    });

    std::thread th2([&]() {
        while(!called) {
            io.reset();
            runIO();
        }
    });

    boost::asio::spawn(second_io, [&] (yield_context yield) {

        waitAll<void>(
            io, yield,
            run, run, run
        );
        called = true;
    });

    th1.join();
    EXPECT_EQ(3U, callCount);

    th2.join();

    EXPECT_EQ(3U, callCount);
    EXPECT_TRUE(called);
}


TEST_P(WaitAllBoostTest, twoIOSShouldSucceedSequence) {
    boost::asio::io_context second_io;
    size_t callCount = 0;
    const auto run = [&] (yield_context y) -> void {
        EXPECT_EQ(&io, &getIoContext(y));
        ++callCount;
    };
    bool called = false;
    boost::asio::spawn(second_io, [&] (yield_context yield) {

        waitAll<void>(
            io, yield,
            run, run, run
        );
        called = true;
    });

    EXPECT_EQ(1U, second_io.run_one());
    EXPECT_EQ(0U, callCount);
    EXPECT_FALSE(called);

    for(size_t i = 1; i <= 3; ++i) {
        EXPECT_EQ(1U, io.run_one());
        EXPECT_EQ(i, callCount);
    }

    EXPECT_EQ(3U, callCount);
    EXPECT_TRUE(called);
}


INSTANTIATE_TEST_SUITE_P(
    WaitAllBoostTests,
    WaitAllBoostTest,
    ::testing::Values(
        1, 2
));

}
