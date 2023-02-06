#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct SearchTest : CommandTestBase
{
    ImapCommandFuturePtr future;

    SearchTest()
    {
        context->sessionState.setAuthenticated();
        selectFolder("INBOX"s, "1"s);
    }

    void search(const string& args)
    {
        future = CommandTestBase::createAndStartCommand("SEARCH"s, args);
    }

    void uidSearch(const string& args)
    {
        future = CommandTestBase::createAndStartCommand("UID SEARCH"s, args);
    }

    bool completed()
    {
        return future && future->ready();
    }

    auto searchResultAll()
    {
        return "* SEARCH 1 2 3";
    }

    auto uidSearchResultAll()
    {
        return "* SEARCH 2 3 6";
    }

    auto searchResultEmpty()
    {
        return "* SEARCH";
    }

    auto searchResult(const string& result)
    {
        return "* SEARCH " + result;
    }

    auto searchOkResponse()
    {
        return commandTag() + " OK SEARCH Completed"s;
    }

    auto uidSearchOkResponse()
    {
        return commandTag() + " OK UID SEARCH Completed"s;
    }

    auto searchNoBackendErrorResponse()
    {
        return commandTag() + " NO [UNAVAILABLE] SEARCH Backend error"s;
    }

    auto dump()
    {
        return session->dumpOutput();
    }
};

TEST_F(SearchTest, searchAll)
{
    search("ALL");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResultAll())) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, searchAllWithCharset)
{
    search("CHARSET UTF-8 ALL");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResultAll())) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, searchByExistingUidReturnsNum)
{
    search("UID 2:2");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResult("1"))) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, searchByNonExistingUidReturnsNothing)
{
    search("UID 1:1");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResultEmpty())) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, searchByExistingNumReturnsNum)
{
    search("2:2");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResult("2"))) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, searchNotAll)
{
    search("NOT ALL");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResultEmpty())) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, searchNotCorcreteUid)
{
    search("NOT 1:1");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResult("2 3"))) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, searchAllAndNot)
{
    search("ALL NOT UID 2:2");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResult("2 3"))) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, searchTooHighRangeReturnsTopMessage)
{
    search("UID 100:*");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResult("3"))) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, searchSeqSetOrThrows)
{
    EXPECT_THROW(search("1:1 OR 2:2"), std::domain_error);
}

TEST_F(SearchTest, searchSeqSetAnd)
{
    search("1:1 2:2");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResultEmpty())) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, noSearchRequestsForSeqSetSearch)
{
    search("1:*");
    ASSERT_TRUE(completed());
    ASSERT_EQ(testSearch->requests.size(), 0);
}

TEST_F(SearchTest, oneSearchRequestForBody)
{
    search("BODY sometext");
    ASSERT_TRUE(completed());
    ASSERT_EQ(testSearch->requests.size(), 1);
}

TEST_F(SearchTest, oneSearchRequestForBodyAndSeqSet)
{
    search("1:* BODY sometext");
    ASSERT_TRUE(completed());
    ASSERT_EQ(testSearch->requests.size(), 1);
}

TEST_F(SearchTest, oneSearchRequestForBodyOrSeqSet)
{
    search("OR 1:10 BODY sometext");
    ASSERT_TRUE(completed());
    ASSERT_EQ(testSearch->requests.size(), 1);
}

TEST_F(SearchTest, oneSearchRequestForSeqSetOrNotBody)
{
    search("OR 1:10 NOT BODY sometext");
    ASSERT_TRUE(completed());
    ASSERT_EQ(testSearch->requests.size(), 1);
}

TEST_F(SearchTest, respondNoIfSearchFails)
{
    search("OR 1:10 NOT BODY sometext");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 1) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchNoBackendErrorResponse())) << dump();
}

TEST_F(SearchTest, respondAfterSearchFinish)
{
    testSearch->response = { {}, { "1" } }; // note that fake mailbox mids are generated
    search("BODY sometext");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], searchResult("1"))) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], searchOkResponse())) << dump();
}

TEST_F(SearchTest, uidSearchExplicitAll)
{
    uidSearch("ALL");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], uidSearchResultAll())) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], uidSearchOkResponse())) << dump();
}

TEST_F(SearchTest, uidSearchAllByRange)
{
    uidSearch("1:*");
    ASSERT_TRUE(completed());
    ASSERT_EQ(session->outgoingData.size(), 2) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], uidSearchResultAll())) << dump();
    ASSERT_TRUE(beginsWith(session->outgoingData[1], uidSearchOkResponse())) << dump();
}

// TODO test size, headers, date criteria