#include <mail/webmail/wait_all/include/io_result.h>
#include <mail/webmail/coro_helpers/get_io/include/get_io.h>
#include <mail/webmail/wait_all/tests/common.h>


namespace {

using namespace testing;
using io_result::waitAll;
using boost::asio::yield_context;


struct WaitAllIOTest : BaseTest {

    template<typename expected_t>
    void runTestOnYieldAndCallback(const auto& preparedWait, const expected_t& expected) {
        std::atomic_size_t callCount = 0;
        const auto token = [&](io_result::error_code ec, expected_t actual) {
            ++callCount;
            EXPECT_EQ(expected, actual);
            EXPECT_FALSE(ec);
        };

        spawn([&] (yield_context yield) {
            const auto actual = preparedWait(io_result::make_yield_context(yield));
            EXPECT_EQ(expected, actual);
            ++callCount;
        });

        preparedWait(token);

        runIO();
        EXPECT_EQ(2U, callCount);
    }
};


TEST_P(WaitAllIOTest, intVectorVariadicOfFunctors) {
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

TEST_P(WaitAllIOTest, intVectorVectorOfFunctors) {
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



TEST_P(WaitAllIOTest, mixedTupleVariadicOfFunctors) {
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

INSTANTIATE_TEST_SUITE_P(
    WaitAllIOTests,
    WaitAllIOTest,
    ::testing::Values(
        1, 2
));

}
