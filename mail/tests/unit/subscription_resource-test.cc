#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "cache_mocks.h"

#include <src/access_impl/subscription_resource.h>

namespace {
using namespace ::testing;

struct GetResourceMock {
    MOCK_METHOD(bool, get, (Yield), (const));
};

using ::doberman::access_impl::SubscriptionResource;
struct SubscriptionResourceTest : public Test {
    GetResourceMock getResource;
    SubscriptionResource<MutexMock, std::function<bool(Yield)>> subscriptionResource{[&](Yield y){ return getResource.get(y);}};
};

TEST_F(SubscriptionResourceTest, get_withSubscriber_callsMutexForUidBeforeGetResource) {
    MutexMock mutex;
    InSequence s;
    EXPECT_CALL(mutex, lock_(_, _)).WillOnce(Return(this));
    EXPECT_CALL(getResource, get(_)).WillOnce(Return(true));

    {
        auto res = subscriptionResource.get({{"SubscriberUid"}}, Yield{});
        //Expecting unlock at destruction.
        EXPECT_CALL(mutex, unlock_(_)).WillOnce(Return());
    }


}

}
