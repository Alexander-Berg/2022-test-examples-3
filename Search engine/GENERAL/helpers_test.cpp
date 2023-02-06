#include "helpers.h"
//#include "yt/core/actions/future.h"

#include <library/cpp/testing/gtest/gtest.h>


namespace NPlutonium {
namespace {

struct TTestPromises {
    std::vector<NYT::TPromise<ui64>> Promises;
    std::vector<NYT::TFuture<ui64>> Futures;

    TTestPromises(size_t count, const THashSet<size_t>& gaps) {
        for (size_t i = 0; i < count; ++i) {
            if (gaps.contains(i)) {
                Promises.emplace_back(NYT::NewPromise<ui64>());
            } else {
                Promises.emplace_back(NYT::MakePromise<ui64>(i));
            }
            Futures.emplace_back(Promises.back());
        }
    }
};

}  // anonymous namespace

TEST(WaitUntilAllSetNoExcept, AllSet) {
    TTestPromises env(4, {});
    NYT::TError result = WaitUntilAllSetNoExcept(env.Futures);
    ASSERT_TRUE(result.IsOK());
}

TEST(WaitUntilAllSetNoExcept, SingleError) {
    TTestPromises env(4, {0});
    env.Promises[0].Set(NYT::TError(NYT::EErrorCode::Generic, "Something wrong"));

    NYT::TError result = WaitUntilAllSetNoExcept(env.Futures);
    ASSERT_FALSE(result.IsOK());
    ASSERT_EQ(result.GetCode(), NYT::EErrorCode::Generic);
    ASSERT_EQ(result.GetMessage(), "Something wrong");
}

TEST(WaitUntilAllSetNoExcept, SingleCancel) {
    TTestPromises env(4, {0});
    env.Promises[0].Set(NYT::TError(NYT::EErrorCode::Canceled, "Won't fix"));

    NYT::TError result = WaitUntilAllSetNoExcept(env.Futures);
    ASSERT_FALSE(result.IsOK());
    ASSERT_EQ(result.GetCode(), NYT::EErrorCode::Canceled);
    ASSERT_EQ(result.GetMessage(), "Won't fix");
}

TEST(WaitUntilAllSetNoExcept, CancelPlusError) {
    TTestPromises env(4, {1, 3});
    env.Promises[1].Set(NYT::TError(NYT::EErrorCode::Canceled, "Won't fix"));
    env.Promises[3].Set(NYT::TError(NYT::EErrorCode::Generic, "Something wrong"));

    NYT::TError result = WaitUntilAllSetNoExcept(env.Futures);
    ASSERT_FALSE(result.IsOK());
    ASSERT_EQ(result.GetCode(), NYT::EErrorCode::Generic);
    ASSERT_EQ(result.GetMessage(), "Something wrong");
}

}
