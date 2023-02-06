#include <crypta/lib/native/yt/dyntables/retrying_client/combine_first_ok.h>
#include <yt/yt/core/misc/shutdown.h>
#include <yt/yt/core/concurrency/scheduler.h>

#include <util/generic/refcount.h>

#include <library/cpp/testing/unittest/registar.h>

#include <iostream>

Y_UNIT_TEST_SUITE(CombineFirstOk) {
    Y_UNIT_TEST(OkAfterWaitFor) {
        TAtomicCounter futureExecuted1 = 0;
        TAtomicCounter futureExecuted2 = 0;

        TVector<NYT::TFuture<void>> futures = {
                NYT::NConcurrency::TDelayedExecutor::MakeDelayed(TDuration::MilliSeconds(100)).Apply(BIND([]() { return NYT::MakeFuture(NYT::TError("fail")); })),
                NYT::NConcurrency::TDelayedExecutor::MakeDelayed(TDuration::MilliSeconds(200)).Apply(BIND([&]() { futureExecuted1.Inc(); })),
                NYT::NConcurrency::TDelayedExecutor::MakeDelayed(TDuration::MilliSeconds(300)).Apply(BIND([&]() { futureExecuted2.Inc(); }))
        };

        UNIT_ASSERT(NYT::NConcurrency::WaitFor(NYT::AnySucceeded(futures)).IsOK());

        Sleep(TDuration::Seconds(1));

        UNIT_ASSERT_EQUAL_C(1, futureExecuted1.Val(), "First future not executed");
        UNIT_ASSERT_EQUAL_C(0, futureExecuted2.Val(), "Second future executed");

        NYT::Shutdown();
    }

    Y_UNIT_TEST(OkBeforeWaitFor) {
        TAtomicCounter futureExecuted = 0;

        TVector<NYT::TFuture<void>> futures = {
                NYT::NConcurrency::TDelayedExecutor::MakeDelayed(TDuration::MilliSeconds(100)).Apply(BIND([]() { return NYT::MakeFuture(NYT::TError("fail")); })),
                NYT::NConcurrency::TDelayedExecutor::MakeDelayed(TDuration::MilliSeconds(200)).Apply(BIND([&]() { futureExecuted.Inc(); })),
                NYT::NConcurrency::TDelayedExecutor::MakeDelayed(TDuration::MilliSeconds(300)).Apply(BIND([&]() { futureExecuted.Inc(); }))
        };

        Sleep(TDuration::Seconds(1));

        UNIT_ASSERT(NYT::NConcurrency::WaitFor(NYT::AnySucceeded(futures)).IsOK());

        UNIT_ASSERT_C(futureExecuted.Val() > 0, "Future not executed");

        NYT::Shutdown();
    }

    Y_UNIT_TEST(Error) {
        TVector<NYT::TFuture<void>> futures = {
                NYT::NConcurrency::TDelayedExecutor::MakeDelayed(TDuration::MilliSeconds(100)).Apply(BIND([]() { return NYT::MakeFuture(NYT::TError("fail1")); })),
                NYT::NConcurrency::TDelayedExecutor::MakeDelayed(TDuration::MilliSeconds(200)).Apply(BIND([]() { return NYT::MakeFuture(NYT::TError("fail2")); })),
                NYT::NConcurrency::TDelayedExecutor::MakeDelayed(TDuration::MilliSeconds(300)).Apply(BIND([]() { return NYT::MakeFuture(NYT::TError("fail3")); }))
        };

        auto result = NYT::NConcurrency::WaitFor(NYT::AnySucceeded(futures));

        UNIT_ASSERT(!result.IsOK());
        UNIT_ASSERT_EQUAL(NYT::EErrorCode::FutureCombinerFailure, result.GetCode());
        UNIT_ASSERT_EQUAL(3, result.InnerErrors().size());
        UNIT_ASSERT_STRINGS_EQUAL("fail1", result.InnerErrors()[0].GetMessage());
        UNIT_ASSERT_STRINGS_EQUAL("fail2", result.InnerErrors()[1].GetMessage());
        UNIT_ASSERT_STRINGS_EQUAL("fail3", result.InnerErrors()[2].GetMessage());

        NYT::Shutdown();
    }
}
