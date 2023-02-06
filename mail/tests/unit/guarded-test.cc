#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/detail/guarded.h>

using namespace ::testing;

namespace {

struct MutexMock {
    using LockHandle = int;
    using YieldContext = int;

    MOCK_METHOD(LockHandle, lock, (YieldContext), ());
    MOCK_METHOD(LockHandle, try_lock, (YieldContext), ());
    MOCK_METHOD(void, unlock, (LockHandle), ());
};

struct MutexMockSingletone {
    using LockHandle = int;
    using YieldContext = int;

    LockHandle lock(YieldContext ctx) { return mock->lock(ctx); }
    LockHandle try_lock(YieldContext ctx) { return mock->try_lock(ctx); }
    void unlock(LockHandle h) { mock->unlock(h); }

    static MutexMock* mock;
};

MutexMock* MutexMockSingletone::mock = nullptr;

struct GuardedTest : public Test {
    void setMock(MutexMock& mock) {
        MutexMockSingletone::mock = &mock;
    }
    ::doberman::detail::Guarded<int, MutexMockSingletone> guarded{1};
};

TEST_F(GuardedTest, lock_withYieldContext_returnsTrueHandleAndUnlockWithLockHandle) {
    StrictMock<MutexMock> mock;
    setMock(mock);
    InSequence s;
    EXPECT_CALL(mock, lock(999)).WillOnce(Return(333));
    EXPECT_CALL(mock, unlock(333)).WillOnce(Return());
    EXPECT_TRUE(guarded.lock(999));
}

TEST_F(GuardedTest, tryLock_withYieldContextAndTryLockTrue_returnsTrueHandleAndUnlockWithLockHandle) {
    StrictMock<MutexMock> mock;
    setMock(mock);
    InSequence s;
    EXPECT_CALL(mock, try_lock(999)).WillOnce(Return(333));
    EXPECT_CALL(mock, unlock(333)).WillOnce(Return());
    EXPECT_TRUE(guarded.tryLock(999));
}

TEST_F(GuardedTest, tryLock_withYieldContextAndTryLockNull_returnsFalseHandle) {
    StrictMock<MutexMock> mock;
    setMock(mock);
    InSequence s;
    EXPECT_CALL(mock, try_lock(999)).WillOnce(Return(0));
    EXPECT_FALSE(guarded.tryLock(999));
}

}
