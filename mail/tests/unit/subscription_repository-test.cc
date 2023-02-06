#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "subscription_repository_mock.h"
#include "subscription_io.h"

namespace doberman {
inline std::ostream& operator << (std::ostream& s, const SubscriptionId& id) {
    return s << '{' << id.uid << ", " << id.id << '}' << std::endl;
}
}

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using doberman::SubscriptionId;
using doberman::logic::SubscriptionData;
using DataRange = std::vector<SubscriptionData>;

struct SubscriptionRepositoryTest : public Test {
    NiceMock<SubscriptionRepositoryAccessMock> mock;

    auto makeRepository(int maxCount = 100500) {
        return ::doberman::logic::makeSubscriptionRepository(&mock, maxCount);
    }

    auto data(::macs::SubscriptionId sid, ::doberman::Uid uid) {
        return SubscriptionData{{uid, sid}, {{uid}, "fid"}, {"subscriber"}};
    }
};

TEST_F(SubscriptionRepositoryTest, ctor_withWorkerId_getsBindedSubscriptions) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{}));
    auto repo = makeRepository();
}

TEST_F(SubscriptionRepositoryTest, ctor_withBindedSubscriptions_releasesEnqueuedSubscriptionsAtDestruction) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{data(777, "555")}));
    EXPECT_CALL(mock, release(_, SubscriptionId{"555", 777})).WillOnce(Return());
    auto repo = makeRepository();
}

TEST_F(SubscriptionRepositoryTest, ctor_withMaxCount2And4BindedSubscriptions_releasesOverdraftedSubscriptions) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{
        data(1, "1"), data(2, "1"), data(999, "333"), data(777, "555")}));
    EXPECT_CALL(mock, release(_, _)).Times(2);
    auto repo = makeRepository(2);
    EXPECT_EQ(repo.count(), 2);
    EXPECT_EQ(repo.overdraft(), 0);
    // Release others at the destruction of repo.
    EXPECT_CALL(mock, release(_, _)).WillRepeatedly(Return());
}

TEST_F(SubscriptionRepositoryTest, get_withEnqueuedSubscriptions_returnDequeuedSubscriptions) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{
        data(1, "1"), data(2, "1")}));
    auto repo = makeRepository();
    auto h1 = repo.get();
    EXPECT_EQ(h1->id, SubscriptionId("1", 1));
    auto h2 = repo.get();
    EXPECT_EQ(h2->id, SubscriptionId("1", 2));
}

TEST_F(SubscriptionRepositoryTest, get_withNoSubscriptions_returnNull) {
    auto repo = makeRepository();
    EXPECT_TRUE(!repo.get());
}

TEST_F(SubscriptionRepositoryTest, get_withEnqueuedSubscription_popsSubscriptionFromQueue) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{data(777, "555")}));
    auto repo = makeRepository();
    auto h = repo.get();
    EXPECT_EQ(h->id, SubscriptionId("555", 777));
}

TEST_F(SubscriptionRepositoryTest, subscriptionHandle_onDestruction_releasesSubscription) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{data(777, "555")}));
    auto repo = makeRepository();
    {
        auto h = repo.get();
        EXPECT_CALL(mock, release(_, SubscriptionId{"555", 777})).WillOnce(Return());
    }
}

TEST_F(SubscriptionRepositoryTest, release_withHandle_releasesSubscription) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{data(777, "555")}));
    auto repo = makeRepository();
    EXPECT_CALL(mock, release(_, SubscriptionId{"555", 777})).WillOnce(Return());
    repo.release(repo.get());
}

TEST_F(SubscriptionRepositoryTest, handle_onRelease_reducesCount) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{data(777, "555")}));
    auto repo = makeRepository();
    auto h = repo.get();
    EXPECT_EQ(repo.count(), 1);
    repo.release(std::move(h));
    EXPECT_EQ(repo.count(), 0);
}

TEST_F(SubscriptionRepositoryTest, handle_onRelease_increasesCredit) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{data(777, "555")}));
    auto repo = makeRepository(1);
    auto h = repo.get();
    EXPECT_EQ(repo.credit(), 0);
    repo.release(std::move(h));
    EXPECT_EQ(repo.credit(), 1);
}

TEST_F(SubscriptionRepositoryTest, decline_withHandle_declinesSubscriptionWithMessage) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{data(777, "555")}));
    auto repo = makeRepository();
    EXPECT_CALL(mock, decline(_, SubscriptionId{"555", 777}, "message")).WillOnce(Return());
    repo.decline(repo.get(), "message");
}

TEST_F(SubscriptionRepositoryTest, decline_withHandle_reducesCount) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{data(777, "555")}));
    auto repo = makeRepository();
    auto h = repo.get();
    EXPECT_EQ(repo.count(), 1);
    repo.decline(std::move(h), "");
    EXPECT_EQ(repo.count(), 0);
}

TEST_F(SubscriptionRepositoryTest, decline_withHandle_increasesCredit) {
    EXPECT_CALL(mock, getReserved(_)).WillOnce(Return(DataRange{data(777, "555")}));
    auto repo = makeRepository(1);
    auto h = repo.get();
    EXPECT_EQ(repo.credit(), 0);
    repo.decline(std::move(h), "");
    EXPECT_EQ(repo.credit(), 1);
}

TEST_F(SubscriptionRepositoryTest, get_withNoEnqueuedSubscriptionAndCredit_callsReserveWithCredit) {
    EXPECT_CALL(mock, reserve(_, 1)).WillOnce(Return(DataRange{data(777, "555")}));
    auto repo = makeRepository(1);
    auto h = repo.get();
    EXPECT_EQ(h->id, SubscriptionId("555", 777));
}

TEST_F(SubscriptionRepositoryTest, get_withNoEnqueuedSubscriptionAndNoCredit_callsReserveWithCredit) {
    EXPECT_CALL(mock, reserve(_, 0)).WillOnce(Return(DataRange{}));
    auto repo = makeRepository(0);
    EXPECT_TRUE(!repo.get());
}

}
