#ifndef DOBERMAN_TESTS_AGENT_LAUNCHER_MOCK_H_
#define DOBERMAN_TESTS_AGENT_LAUNCHER_MOCK_H_

#include <gmock/gmock.h>
#include <src/logic/agent_launcher.h>
#include <src/logic/envelope_copier.h>
#include "subscription_mock.h"
#include "shared_folder_mock.h"
#include "subscribed_folder_mock.h"
#include "change_queue_mock.h"

namespace doberman {
namespace testing {

using namespace ::testing;

struct AccessMock {
    MOCK_METHOD(SharedFolderAccessMock*, sharedFolder, (), (const));
    MOCK_METHOD(SubscribedFolderAccessMock*, subscribedFolder, (), (const));
    MOCK_METHOD(ChangeQueueAccessMock*, changeQueue, (), (const));
    MOCK_METHOD(SubscriptionAccessMock*, subscription, (), (const));
    logic::TrivialEnvelopeCopier envelopeCopier() const {return {};}
};

struct SpawnerMock {
    MOCK_METHOD(void, launch, (std::function<void(AccessMock&)>), (const));
};

} // namespace testing
} // namespace doberman

#endif /* DOBERMAN_TESTS_AGENT_LAUNCHER_MOCK_H_ */
