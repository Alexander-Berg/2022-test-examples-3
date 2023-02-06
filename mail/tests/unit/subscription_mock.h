#ifndef DOBERMAN_TESTS_SUBSCRIPTION_MOCK_H_
#define DOBERMAN_TESTS_SUBSCRIPTION_MOCK_H_

#include <src/logic/subscription.h>
#include <gmock/gmock.h>
#include "shared_folder_mock.h"
#include "subscribed_folder_mock.h"
#include "change_queue_mock.h"

namespace doberman {
namespace testing {

using namespace ::testing;

const SubscriptionId dummySid{"uid", 1};

struct SubscriptionMock {
    using SharedFolder = SharedFolderMock;
    using SubscribedFolder = ::doberman::logic::SubscribedFolder<SubscribedFolderAccessMock*>;
    using ChangeQueue = ::doberman::logic::ChangeQueue<ChangeQueueAccessMock*>;
    using State = ::doberman::SubscriptionState;

    MOCK_METHOD(const SubscriptionId&, id, (), (const));
    MOCK_METHOD(SharedFolder&, src, (), (const));
    MOCK_METHOD(SubscribedFolder&, dst, (), (const));
    MOCK_METHOD(ChangeQueue&, changes, (), (const));
    MOCK_METHOD(State, state, (), (const));
    MOCK_METHOD((std::tuple<State, bool>), init, (), ());
    MOCK_METHOD(State, sync, (), ());
    MOCK_METHOD(State, fail, (std::string), ());
    MOCK_METHOD(State, finish, (), ());
    MOCK_METHOD((std::tuple<State, bool>), clear, (), ());
};

struct SubscriptionAccessMock {
    using State = ::doberman::SubscriptionState;
    const SubscriptionId& makeContext(const SubscriptionId& id, const logic::Subscriber&) { return id; }
    MOCK_METHOD(State, state, (SubscriptionId), (const));
    MOCK_METHOD((std::tuple<State, bool>), init, (SubscriptionId), (const));
    MOCK_METHOD(State, sync, (SubscriptionId), (const));
    MOCK_METHOD(State, fail, (SubscriptionId, std::string), (const));
    MOCK_METHOD(State, finish, (SubscriptionId), (const));
    MOCK_METHOD((std::tuple<State, bool>), clear, (SubscriptionId), (const));
};

} // namespace test
} // namespace doberman

#endif /* DOBERMAN_TESTS_SUBSCRIPTION_MOCK_H_ */
