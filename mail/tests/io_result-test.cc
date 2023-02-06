#include <utility>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <io_result/io_result.h>
#include <boost/asio/io_service.hpp>
#include <boost/asio/spawn.hpp>
#include <boost/asio/dispatch.hpp>

using namespace ::testing;
using namespace ::io_result;
namespace asio = ::boost::asio;

enum class Error {
    logic = 1
};

inline error_code::base_type make_error_code(Error e) {
    return { static_cast<int>(e), boost::system::system_category() };
}

namespace boost {
namespace system {

template <>
struct is_error_code_enum<Error> : std::true_type {};

} // namespace system
} // namespace boost

namespace {

class Revision {
    int value = 0;
public:
    Revision() = default;
    Revision(const int value) : value(value) {}

    bool operator== (const Revision& that) const noexcept {
        return value == that.value;
    }
};

using RevSequence = Hook<hooks::Sequence<Revision>>;
using OnUpdate = Hook<Revision>;

struct Mock {
    MOCK_METHOD(void, asyncMethodInternal, (OnUpdate), (const));
    MOCK_METHOD(void, asyncSequenceMethodInternal, (RevSequence), (const));

    template <typename Handler>
    auto asyncMethod(Handler handler) {
        detail::init_async_result<Handler, OnUpdate> init(handler);
        asyncMethodInternal(init.handler);
        return init.result.get();
    }

    template <typename Handler>
    auto asyncSequenceMethod(Handler handler) {
        detail::init_async_result<Handler, RevSequence> init(handler);
        asyncSequenceMethodInternal(init.handler);
        return init.result.get();
    }
};


template <typename T, typename Seq>
void forwardSequence(Hook<io_result::Sequence<T>> h, Seq&& s) {
    struct Continuation {
        decltype(std::begin(s)) i;
        decltype(std::end(s)) last;
        Hook<io_result::Sequence<T>> h;
        void operator()() {
            if(i != last) {
                T val = std::move(*i);
                ++i;
                h({std::move(val), *this});
            } else {
                h();
            }
        }
    } c{std::begin(s), std::end(s), std::move(h)};
    c();
}

TEST(TestMacsIo, methodWithPromise_noError_returnsFutureWithValue) {
    Mock mock;
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(
                    InvokeArgument<0>(Revision{1}));
    auto f = mock.asyncMethod(use_future);
    ASSERT_EQ(f.get(), Revision{1});
}

TEST(TestMacsIo, methodWithPromise_withError_returnsFutureWithException) {
    Mock mock;
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(
                    InvokeArgument<0>(error_code{Error::logic}));
    auto f = mock.asyncMethod(use_future);
    EXPECT_THROW(f.get(), system_error);
}

TEST(TestMacsIo, methodWithSyncContext_noError_returnsValue) {
    Mock mock;
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(
                    InvokeArgument<0>(Revision{1}));
    auto res = mock.asyncMethod(use_sync);
    ASSERT_EQ(res, Revision{1});
}

TEST(TestMacsIo, methodWithSyncContext_withError_throwsException) {
    Mock mock;
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(
                    InvokeArgument<0>(error_code{Error::logic}));
    EXPECT_THROW(mock.asyncMethod(use_sync), system_error);
}

TEST(TestMacsIo, methodWithSyncContextAndErrorCode_withError_returnsDefaultValueAndError) {
    Mock mock;
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(
                    InvokeArgument<0>(error_code{Error::logic}));
    error_code ec;
    auto res = mock.asyncMethod(use_sync[ec]);
    ASSERT_EQ(ec, Error::logic);
    ASSERT_EQ(res, Revision{});
}


TEST(TestMacsIo, methodWithYieldContext_noError_returnsValue) {
    asio::io_service ios;
    Mock mock;
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(Invoke([&](OnUpdate h){
        ios.post([h](){h(Revision{1});});
    }));
    asio::spawn(ios, [&](auto yield) {
        auto myield = make_yield_context(yield);
        auto res = mock.asyncMethod(myield);
        ASSERT_EQ(res, Revision{1});
    });
    ios.run();
}

TEST(TestMacsIo, methodWithYieldContext_callbackCalledInSameCoroWithNoError_returnsValue) {
    asio::io_service ios;
    Mock mock;
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(Invoke([&](OnUpdate h){
        h(Revision{1});
    }));
    asio::spawn(ios, [&](auto yield) {
        auto myield = make_yield_context(yield);
        auto res = mock.asyncMethod(myield);
        ASSERT_EQ(res, Revision{1});
    });
    ios.run();
}

TEST(TestMacsIo, methodWithYieldContext_callbackCalledInSameCoroWithNoError_noReschedule) {
    Mock mock;
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(Invoke([&](OnUpdate h){
        h(Revision{1});
    }));
    bool rescheduled = false;
    asio::io_service ios;
    asio::io_service::strand s(ios);
    asio::spawn(s, [&mock, &rescheduled](auto yield) {
        auto myield = make_yield_context(yield);
        mock.asyncMethod(myield);
        ASSERT_FALSE(rescheduled);
    });
    s.post([&]{rescheduled = true;});
    ios.run();
}

TEST(TestMacsIo, methodSequenceWithYieldContext_noError_returnsValue) {
    asio::io_service ios;
    Mock mock;
    EXPECT_CALL(mock, asyncSequenceMethodInternal(_)).WillOnce(Invoke([&](RevSequence h){
        ios.post([h, &ios] { h({Revision{1}, [h, &ios] {
            ios.post([h, &ios] { h({Revision{2}, [h, &ios] {
                ios.post([h]{h();});
            } }); });
        } }); });
    }));
    std::vector<Revision> rvec;
    asio::spawn(ios, [&](auto yield) {
        auto myield = make_yield_context(yield);
        auto res = mock.asyncSequenceMethod(myield);
        std::vector<Revision> r{res.begin(), res.end()};
        rvec = std::move(r);
    });
    ios.run();
    ASSERT_THAT(rvec, ElementsAre(Revision{1}, Revision{2}));
}

TEST(TestMacsIo, methodSequenceWithYieldContext_tryingToIncrementInvalidCopyOfIterator_throwsOutOfRange) {
    asio::io_service ios;
    Mock mock;
    EXPECT_CALL(mock, asyncSequenceMethodInternal(_)).WillOnce(Invoke([&](RevSequence h){
        ios.post([h, &ios] { h({Revision{1}, [h, &ios] {
            ios.post([h, &ios] { h({Revision{2}, [h, &ios] {
                ios.post([h](){h();});
            } }); });
        } }); });
    }));
    using Range = decltype(mock.asyncSequenceMethod(
            make_yield_context(std::declval<asio::yield_context>())));
    using Iter = decltype(std::declval<Range>().begin());
    Iter i;
    asio::spawn(ios, [&](asio::yield_context yield) {
        auto myield = make_yield_context(yield);
        auto res = mock.asyncSequenceMethod(myield);
        i = res.begin();
        std::vector<Revision> receivedData{res.begin(), res.end()};
    });
    ios.run();
    EXPECT_THROW(++i, std::out_of_range);
}

TEST(TestMacsIo, methodSequenceWithYieldContext_withHandlerCalledInSameCoroWithNonEmptyResult_returnsResult) {
    asio::io_service ios;
    Mock mock;
    InSequence s;
    std::vector<Revision> seq{{1}, {2}};
    EXPECT_CALL(mock, asyncSequenceMethodInternal(_)).WillOnce(Invoke([&](RevSequence h){
        forwardSequence(std::move(h), seq);
    }));
    std::vector<Revision> rvec;
    asio::spawn(ios, [&](auto yield) {
        auto res = mock.asyncSequenceMethod(make_yield_context(yield));
        rvec.assign(res.begin(), res.end());
    });
    ios.run();
    ASSERT_THAT(rvec, ElementsAre(Revision{1}, Revision{2}));
}

TEST(TestMacsIo, methodWithYieldContext_withError_throwsException) {
    asio::io_service ios;
    Mock mock;
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(Invoke([&](OnUpdate h){
        ios.post([h](){h(error_code{Error::logic});});
    }));
    asio::spawn(ios, [&](auto yield) {
        auto myield = make_yield_context(yield);
        EXPECT_THROW( mock.asyncMethod(myield), system_error);
    });
    ios.run();
}

TEST(TestMacsIo, methodWithYieldContextAndErrorCode_withError_returnsDefaultValueAndError) {
    asio::io_service ios;
    Mock mock;
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(Invoke([&](OnUpdate h){
        ios.post([h](){h(error_code{Error::logic});});
    }));
    asio::spawn(ios, [&](auto yield) {
        auto myield = make_yield_context(yield);
        error_code ec;
        auto res = mock.asyncMethod(myield[ec]);
        ASSERT_EQ(ec, Error::logic);
        ASSERT_EQ(res, Revision{});
    });
    ios.run();
}

TEST(TestMacsIo, methodSequenceWithYieldContext_interleavedWithMethodWithYeldContext_returnsResult) {
    asio::io_service ios;
    Mock mock;
    std::vector<Revision> seq{{1}, {3}};
    EXPECT_CALL(mock, asyncSequenceMethodInternal(_)).WillOnce(Invoke([&](RevSequence h){
        ios.post([h,&seq](){forwardSequence(h, seq);});
    }));
    EXPECT_CALL(mock, asyncMethodInternal(_)).WillOnce(Invoke([&](OnUpdate h){
        ios.post([h](){h(Revision{2});});
    }));
    std::vector<Revision> rvec;
    asio::spawn(ios, [&](auto yield) {
        auto res = mock.asyncSequenceMethod(make_yield_context(yield));
        auto i = res.begin();
        rvec.push_back(*i);
        rvec.push_back(mock.asyncMethod(make_yield_context(yield)));
        ++i;
        rvec.push_back(*i);
    });
    ios.run();
    ASSERT_THAT(rvec, ElementsAre(Revision{1}, Revision{2}, Revision{3}));
}

TEST(TestMacsIo, dispatchCoroHandlerFromDifferentThread_shouldRunInInitialIoContext) {
    asio::io_context io1;
    asio::io_context io2;
    auto work1 = asio::make_work_guard(io1);
    auto work2 = asio::make_work_guard(io2);
    std::atomic_bool called {false};

    asio::spawn(io1, [&] (asio::yield_context yield) {
        [&] (auto yield) {
            const auto tid = std::this_thread::get_id();

            detail::init_async_result<decltype(yield), OnUpdate> init(yield);
            asio::dispatch(bind(init.handler, error_code(), Revision {42}));
            EXPECT_EQ(init.result.get(), Revision {42});
            EXPECT_EQ(tid, std::this_thread::get_id());

            work1.reset();
            work2.reset();
            called = true;
        } (make_yield_context(yield));
    });

    std::thread t([&] {
        io2.run();
    });

    io1.run();
    t.join();

    EXPECT_TRUE(called.load());
}

}
