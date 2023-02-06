#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct SubscribeUnsubscribeTest : CommandTestBase
{
    ImapCommandFuturePtr future;

    SubscribeUnsubscribeTest()
    {
        context->sessionState.setAuthenticated();
    }

    void startSubscribeInbox()
    {
        auto command = createCommand("SUBSCRIBE"s, "Inbox"s);
        startCommand(command);
        future = command->getFuture();
    }

    void startUnsubscribeInbox()
    {
        auto command = createCommand("UNSUBSCRIBE"s, "Inbox"s);
        startCommand(command);
        future = command->getFuture();
    }

    bool completed()
    {
        return future && future->ready();
    }

    void setInboxNotSubscribed()
    {
        getInbox()->subscribed = false;
    }

    void setInboxSubscribed()
    {
        getInbox()->subscribed = true;
    }

    bool inboxIsSubscribed()
    {
        return getInbox()->subscribed;
    }

    FullFolderInfoPtr getInbox()
    {
        auto folderList = testMetaBackend->folderList();
        return folderList->at("Inbox");
    }

    auto subscribeOkResponse()
    {
        return commandTag() + " OK SUBSCRIBE Completed";
    }

    auto unsubscribeOkResponse()
    {
        return commandTag() + " OK UNSUBSCRIBE Completed";
    }

    auto subscribeNoBackendErrorResponse()
    {
        return commandTag() + " NO [UNAVAILABLE] SUBSCRIBE Backend error";
    }
};

TEST_F(SubscribeUnsubscribeTest, subscribeSetsSubscribed)
{
    setInboxNotSubscribed();
    startSubscribeInbox();
    ASSERT_TRUE(inboxIsSubscribed()) << session->dumpOutput();
}

TEST_F(SubscribeUnsubscribeTest, subscribeRespondOk)
{
    setInboxNotSubscribed();
    startSubscribeInbox();
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], subscribeOkResponse()))
        << session->outgoingData[0];
}

TEST_F(SubscribeUnsubscribeTest, doesNothingForAlreadySubscribed)
{
    setInboxSubscribed();
    startSubscribeInbox();
    ASSERT_TRUE(inboxIsSubscribed()) << session->dumpOutput();
}

TEST_F(SubscribeUnsubscribeTest, unsubscribeSetsUnsubscribed)
{
    setInboxSubscribed();
    startUnsubscribeInbox();
    ASSERT_FALSE(inboxIsSubscribed()) << session->dumpOutput();
}

TEST_F(SubscribeUnsubscribeTest, unsubscribeRespondOk)
{
    setInboxSubscribed();
    startUnsubscribeInbox();
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], unsubscribeOkResponse()))
        << session->outgoingData[0];
}

TEST_F(SubscribeUnsubscribeTest, respondNoIfBackendFails)
{
    testMetaBackend->faulty = true;
    setInboxNotSubscribed();
    startSubscribeInbox();
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], subscribeNoBackendErrorResponse()))
        << session->outgoingData[0];
}