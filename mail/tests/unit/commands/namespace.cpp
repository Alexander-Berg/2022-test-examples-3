#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct NamespaceTest : CommandTestBase
{
    ImapCommandFuturePtr future;

    NamespaceTest()
    {
        context->sessionState.setAuthenticated();
    }

    void createAndStartCommand()
    {
        future = CommandTestBase::createAndStartCommand("NAMESPACE"s, ""s);
    }

    bool completed()
    {
        return future && future->ready();
    }

    bool completedNoException()
    {
        return completed() && !future->has_exception();
    }

    auto namespaceList()
    {
        return ""s;
    }

    auto okResponse()
    {
        return commandTag() + " OK NAMESPACE Completed."s;
    }
};

TEST_F(NamespaceTest, finishedJustAfterStart)
{
    createAndStartCommand();
    ASSERT_TRUE(completedNoException());
}

TEST_F(NamespaceTest, finishedOk)
{
    createAndStartCommand();
    ASSERT_EQ(session->outgoingData.size(), 2) << session->dumpOutput();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], namespaceList())) << session->dumpOutput();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], okResponse())) << session->dumpOutput();
}

// TODO test diff output