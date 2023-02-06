#include "async_task_pool.h"

#include <yt/yt/core/concurrency/delayed_executor.h>
#include <yt/yt/core/logging/log.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/ptr.h>
#include <util/generic/vector.h>
#include <util/random/fast.h>
#include <util/random/shuffle.h>
#include <util/system/mutex.h>


static NYT::NLogging::TLogger Logger{"async_task_pool_test"};

namespace NPlutonium {

namespace {
    const ui64 MaxInFlightTasks = 2;
    std::atomic<ui64> InFlightCounter = 0;
}

struct TTestTaskParams {
    TVector<TDuration> Durations;
    ui64 TargetAttempt = Max<ui64>();
    ui64 Id = Max<ui64>();
};

struct TTestTaskContext : TThrRefBase {
    TTestTaskContext(TTestTaskParams params)
        : Params_(std::move(params))
    {
    }

    ui64 GetId() const {
        return Params_.Id;
    }

    void DoSomeWork() {
        const ui64 attempt = CurrentAttempt_++;
        Y_VERIFY(attempt < Params_.Durations.size());

        const ui64 inFlightBefore = InFlightCounter.fetch_add(1);
        YT_LOG_INFO("start task #%v, attempt #%v, inflight: %v -> %v", Params_.Id, attempt, inFlightBefore, inFlightBefore + 1);
        InFlightError = InFlightError || inFlightBefore + 1 > MaxInFlightTasks;

        NYT::NConcurrency::TDelayedExecutor::WaitForDuration(Params_.Durations[attempt]);

        const ui64 inFlightAfter = InFlightCounter.fetch_sub(1);
        YT_LOG_INFO("finishing task #%v, attempt #%v, inflight: %v -> %v", Params_.Id, attempt, inFlightAfter, inFlightAfter - 1);
        InFlightError = InFlightError || inFlightAfter > MaxInFlightTasks;

        Y_ENSURE(Params_.TargetAttempt <= attempt, "Cannot finish task #" << Params_.Id << ", attempt #" << attempt << " < #" << Params_.TargetAttempt);
        Y_ENSURE(!InFlightError, "too many parallel tasks");

        YT_LOG_INFO("FINISHED task #%v, attempt #%v, inflight: %v -> %v", Params_.Id, attempt, inFlightAfter, inFlightAfter - 1);
    }

private:
    const TTestTaskParams Params_;
    ui64 CurrentAttempt_ = 0;
    bool InFlightError = false;
};

TTestTaskParams GenerateSingleTaskParams(
    ui64 id,
    TDuration baseDuration,
    TDuration delta,
    ui64 minAttempts,
    ui64 maxAttempts,
    TFastRng64& rng
) {
    TVector<TDuration> durations(Reserve(maxAttempts));
    for (ui64 j = 0; j < maxAttempts; ++j) {
        durations.push_back(baseDuration + TDuration::MicroSeconds(rng.Uniform(delta.MicroSeconds())));
    }
    return TTestTaskParams{
        .Durations = std::move(durations),
        .TargetAttempt = rng.Uniform(minAttempts, maxAttempts),
        .Id = id,
    };
}

TVector<TTestTaskParams> GenerateMultipleTaskParams(
    ui64 totalTasks,
    ui64 fromId,
    TDuration baseDuration,
    TDuration delta,
    ui64 minAttempts,
    ui64 maxAttempts,
    TFastRng64& rng
) {
    TVector<TTestTaskParams> tasks(Reserve(totalTasks));
    for (ui64 id = 0; id < totalTasks; ++id) {
        tasks.push_back(GenerateSingleTaskParams(
            id + fromId,
            baseDuration,
            delta,
            minAttempts,
            maxAttempts,
            rng
        ));
    }
    return tasks;
}

TEST(AsyncTaskPool, Simple) {
    const ui64 maxAttempts = 5;
    TIntrusivePtr<TAsyncTaskPool> taskPool = TAsyncTaskPool::Create(TAsyncTaskPoolConfig{
        .Name = "test_task_queue",
        .ParallelTasks_ = MaxInFlightTasks,
        .MaxAttemptsPerTask = maxAttempts,
    });

    TFastRng64 rng{0x1ce1f2e507541a05, 0x07d45659, 0x7b8771030dd9917e, 0x2d6636ce};
    auto taskParams = GenerateMultipleTaskParams(
        10,
        0,
        TDuration::MilliSeconds(10),
        TDuration::MilliSeconds(10),
        0,
        maxAttempts,
        rng);

    std::atomic<ui64> done = 0;
    for (auto& params : taskParams) {
        auto task = MakeIntrusive<TTestTaskContext>(params);
        taskPool->ScheduleTask(0,
            [task = std::move(task), &done]() -> NYT::TFuture<void> {
                task->DoSomeWork();
                done.fetch_add(1);
                return NYT::VoidFuture;
            },
            [](const NYT::TError& error, ui64 attempt) {
                YT_LOG_INFO("error occured during attempt #%v, message: %qv", attempt, NYT::ToString(error));
            });
    }

    YT_LOG_INFO("Waiting...");
    EXPECT_NO_THROW(taskPool->StopAndWait(false).ThrowOnError());
    EXPECT_NO_THROW(taskPool->StopAndWait(false).ThrowOnError());

    YT_LOG_INFO("Finished waiting...");
    ASSERT_EQ(done.load(), taskParams.size());

    YT_LOG_INFO("Try resume...");

    auto lastTask = MakeIntrusive<TTestTaskContext>(GenerateSingleTaskParams(42,
        TDuration::MilliSeconds(10),
        TDuration::MilliSeconds(10),
        0,
        maxAttempts,
        rng));
    std::atomic<bool> lastTaskFinished = false;
    auto addLastTask = [taskPool, lastTask, &lastTaskFinished]() {
        taskPool->ScheduleTask(0, [lastTask, &lastTaskFinished]() -> NYT::TFuture<void> {
            lastTask->DoSomeWork();
            lastTaskFinished.store(true);
            return NYT::VoidFuture;
        });
    };

    //EXPECT_THROW(addLastTask(), yexception);
    taskPool->Resume();

    EXPECT_NO_THROW(addLastTask());
    EXPECT_NO_THROW(taskPool->StopAndWait(false).ThrowOnError());
    ASSERT_TRUE(lastTaskFinished.load());
}

struct TSyncHashSet {
    void Insert(ui64 n) {
        auto g = Guard(Mutex_);
        Values_.insert(n);
    }
    void Erase(ui64 n) {
        auto g = Guard(Mutex_);
        Values_.erase(n);
    }

    TVector<ui64> GetSortedValues() const {
        TVector<ui64> result(Values_.begin(), Values_.end());
        Sort(result);
        return result;
    }

private:
    TMutex Mutex_;
    THashSet<ui64> Values_;
};

TEST(AsyncTaskPool, FailedTasks) {
    const ui64 maxAttempts = 3;
    TIntrusivePtr<TAsyncTaskPool> taskPool = TAsyncTaskPool::Create(TAsyncTaskPoolConfig{
        .Name = "test_task_queue",
        .ParallelTasks_ = MaxInFlightTasks,
        .MaxAttemptsPerTask = maxAttempts,
    });

    TFastRng64 rng{0x1ce1f2e507541a05, 0x07d45659, 0x7b8771030dd9917e, 0x2d6636ce};
    auto successTasks = GenerateMultipleTaskParams(
        10,
        0,
        TDuration::MilliSeconds(10),
        TDuration::MilliSeconds(10),
        0,
        maxAttempts,
        rng);
    auto failedTasks = GenerateMultipleTaskParams(
        2,
        100,
        TDuration::MilliSeconds(10),
        TDuration::MilliSeconds(10),
        maxAttempts,
        maxAttempts + 1,
        rng);

    TVector<TTestTaskParams> allTasks = successTasks;
    allTasks.insert(allTasks.end(), failedTasks.begin(), failedTasks.end());
    Shuffle(allTasks.begin(), allTasks.end(), rng);

    TSyncHashSet allNotStarted;
    for (auto& task : allTasks) {
        allNotStarted.Insert(task.Id);
    }
    TSyncHashSet sucessNotFinished;
    for (auto& task : successTasks) {
        sucessNotFinished.Insert(task.Id);
    }

    for (auto& params : allTasks) {
        auto task = MakeIntrusive<TTestTaskContext>(params);

        NYT::TFuture<void> f = taskPool->ScheduleTask(0,
            [task = std::move(task), &allNotStarted, &sucessNotFinished]() -> NYT::TFuture<void> {
                allNotStarted.Erase(task->GetId());
                task->DoSomeWork();
                sucessNotFinished.Erase(task->GetId());
                return NYT::VoidFuture;
            },
            [](const NYT::TError& error, ui64 attempt) {
                YT_LOG_INFO("error occured during attempt #%v, message: %qv", attempt, NYT::ToString(error));
            });
    }

    YT_LOG_INFO("Waiting...");

    EXPECT_THROW(taskPool->StopAndWait(false).ThrowOnError(), NYT::TErrorException);
    EXPECT_NO_THROW(taskPool->StopAndWait(false).ThrowOnError());

    YT_LOG_INFO("Finished waiting...");

    ASSERT_EQ(allNotStarted.GetSortedValues(), TVector<ui64>{});
    ASSERT_EQ(sucessNotFinished.GetSortedValues(), TVector<ui64>{});
}

TEST(AsyncTaskPool, ResumeWhileStopping) {
    TIntrusivePtr<TAsyncTaskPool> taskPool = TAsyncTaskPool::Create(TAsyncTaskPoolConfig{
        .Name = "test_task_queue",
        .ParallelTasks_ = MaxInFlightTasks,
    });

    YT_LOG_INFO("Starting task");
    auto taskF = taskPool->ScheduleTask(0, [](){
        YT_LOG_INFO("Sleeping");
        Sleep(TDuration::Seconds(1));
        return NYT::VoidFuture;
    });

    YT_LOG_INFO("Stopping pool");
    auto stopF = taskPool->StopAsync();

    YT_LOG_INFO("Resuming pool");
    taskPool->Resume();

    ASSERT_TRUE(taskF.IsSet());
    ASSERT_TRUE(stopF.IsSet());
}

}
