#ifndef DOBERMAN_TESTS_SUBSCRIPTION_REPOSITORY_MOCK_H_
#define DOBERMAN_TESTS_SUBSCRIPTION_REPOSITORY_MOCK_H_

#include <src/logic/subscription_repository.h>
#include <gmock/gmock.h>
#include "subscription_mock.h"

namespace doberman {
namespace testing {

using namespace ::testing;

struct SubscriptionRepositoryAccessMock{
    struct Ctx {};
    auto makeContext() { return Ctx{}; }
    MOCK_METHOD(std::vector<logic::SubscriptionData>, reserve, (Ctx, int), (const));
    MOCK_METHOD(std::vector<logic::SubscriptionData>, getReserved, (Ctx), (const));
    MOCK_METHOD(void, release, (Ctx, SubscriptionId), (const));
    MOCK_METHOD(void, decline, (Ctx, SubscriptionId, std::string), (const));
};

struct SubscriptionRepositoryMock{
    MOCK_METHOD(boost::optional<logic::SubscriptionData>, get, (), (const));
};

} // namespace test
} // namespace doberman

#endif /* DOBERMAN_TESTS_SUBSCRIPTION_REPOSITORY_MOCK_H_ */
