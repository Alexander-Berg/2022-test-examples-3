#ifndef DOBERMAN_TESTS_CHANGE_QUEUE_MOCK_H_
#define DOBERMAN_TESTS_CHANGE_QUEUE_MOCK_H_

#include <src/logic/change_queue.h>
#include <gmock/gmock.h>

#include "change_mock.h"

namespace doberman {
namespace testing {

using namespace ::testing;

struct ChangeQueueMock {
    MOCK_METHOD(std::shared_ptr<const logic::Change>, top, (), (const));
    MOCK_METHOD(void, pop, (), ());
};

struct ChangeQueueAccessMock {
    auto makeContext(const SubscriptionId& id) { return id; }
    MOCK_METHOD(std::shared_ptr<const logic::Change>, top, (SubscriptionId), (const));
    MOCK_METHOD(void, pop, (SubscriptionId), ());
};


} // namespace test
} // namespace doberman


#endif /* DOBERMAN_TESTS_CHANGE_QUEUE_MOCK_H_ */
