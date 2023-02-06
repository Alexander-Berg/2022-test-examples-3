#include <src/backend/backend.h>

#include <sstream>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace yimap;
using namespace yimap::backend;

class TestNotificationsBackend : public NotificationsBackend
{
public:
    void subscribe(WeakEventHandlerPtr handler) override
    {
        subscribed = true;
        subscriptions.push_back(handler);
    }

    bool subscribed = false;
    std::vector<WeakEventHandlerPtr> subscriptions;
};