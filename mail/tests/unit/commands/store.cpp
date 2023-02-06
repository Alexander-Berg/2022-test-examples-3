#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct StoreTest
    : public CommandTestBase
    , public ::testing::WithParamInterface<string>
{
    template <typename Arg>
    using MatcherFunctor = std::function<bool(const Arg&)>;
    string allSequence = "1:*"s;
    string emptySequence = "100:101"s;
    string inboxFid = "1"s;

    StoreTest()
    {
        selectFolder("Inbox"s, inboxFid);
    }

    auto commandName()
    {
        return GetParam();
    }

    auto runCommand(const string& command, const string& messageSequence, const string& flags)
    {
        return createAndStartCommand(command, messageSequence + " " + flags);
    }

    auto runTestCommand(const string& messageSequence, const string& flags)
    {
        return runCommand(commandName(), messageSequence, flags);
    }

    auto getFolderMessages(const string& fid)
    {
        std::vector<MessageData> res;
        for (auto& msg : testMetaBackend->getMailbox().messages[fid])
        {
            res.push_back(msg.second);
        }
        return res;
    }

    void disableAutoExpunge()
    {
        userSettings->enableAutoExpunge = false;
    }

    auto taggedOkResponse()
    {
        return commandTag() + " OK "s + commandName() + " Completed."s;
    }

    auto makeFullStoreResponse(const std::vector<MessageData>& messages)
    {
        std::vector<string> res;
        for (auto msg : messages)
        {
            res.push_back(
                "* " + std::to_string(msg.num) + " FETCH (UID " + std::to_string(msg.uid) +
                " FLAGS (" + static_cast<string>(msg.flags) + "))");
        }

        res.push_back(taggedOkResponse());
        return res;
    }

    auto makeFullStoreResponseWithAutoExpunge(const std::vector<MessageData>& messages)
    {
        std::vector<string> res;
        for (auto&& msg : messages)
        {
            res.push_back(
                "* " + std::to_string(msg.num) + " FETCH (UID " + std::to_string(msg.uid) +
                " FLAGS (\\Deleted))");
        }

        res.push_back(taggedOkResponse());
        return res;
    }

    auto readOnlyResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] " + commandName() +
            " Can not store in read-only folder."s;
    }

    auto storeNoMessagesResponse()
    {
        return commandTag() + " NO [CLIENTBUG] STORE Failed (no messages)"s;
    }

    auto uidStoreNoMessagesResponse()
    {
        return commandTag() + " OK [CLIENTBUG] UID STORE Completed (no messages)"s;
    }
};

TEST_P(StoreTest, addFlagCompletes)
{
    auto future = runTestCommand(allSequence, "+FLAGS (\\Flagged)");
    ASSERT_TRUE(future->ready());
}

TEST_P(StoreTest, addFlag)
{
    runTestCommand(allSequence, "+FLAGS (\\Flagged)");
    auto messages = getFolderMessages(inboxFid);
    MatcherFunctor<MessageData> hasDeletedFlag = [](const auto& msg) {
        return msg.flags.hasFlag("\\Flagged");
    };
    ASSERT_THAT(messages, testing::Each(testing::ResultOf(hasDeletedFlag, testing::Eq(true))));
}

TEST_P(StoreTest, addMultipleFlags)
{
    runTestCommand(allSequence, "+FLAGS (\\Flagged \\Flagged)");
    auto messages = getFolderMessages(inboxFid);
    MatcherFunctor<MessageData> hasRequiredFlags = [](const auto& msg) {
        return msg.flags.hasAllFlags(std::vector{ "\\Flagged", "\\Flagged" });
    };
    ASSERT_THAT(messages, testing::Each(testing::ResultOf(hasRequiredFlags, testing::Eq(true))));
}

TEST_P(StoreTest, addFlagResponse)
{
    runTestCommand(allSequence, "+FLAGS (\\Flagged)");
    auto correctResponseData = makeFullStoreResponse(getFolderMessages(inboxFid));
    ASSERT_THAT(session->outgoingData, testing::UnorderedElementsAreArray(correctResponseData));
}

TEST_P(StoreTest, addFlagSient)
{
    runTestCommand(allSequence, "+FLAGS.SILENT (\\Flagged)");
    ASSERT_THAT(session->outgoingData, testing::ElementsAre(taggedOkResponse()));
}

TEST_P(StoreTest, delFlagCompletes)
{
    auto future = runTestCommand(allSequence, "-FLAGS (\\Seen)");
    ASSERT_TRUE(future->ready());
}

TEST_P(StoreTest, delFlag)
{
    runTestCommand(allSequence, "-FLAGS (\\Seen)");
    auto messages = getFolderMessages(inboxFid);
    MatcherFunctor<MessageData> hasSeenFlag = [](const auto& msg) {
        return msg.flags.hasFlag("\\Seen");
    };
    ASSERT_THAT(messages, testing::Each(testing::ResultOf(hasSeenFlag, testing::Eq(false))));
}

TEST_P(StoreTest, delFlagResponse)
{
    runTestCommand(allSequence, "-FLAGS (\\Seen)");
    auto correctResponseData = makeFullStoreResponse(getFolderMessages(inboxFid));
    ASSERT_THAT(session->outgoingData, testing::UnorderedElementsAreArray(correctResponseData));
}

TEST_P(StoreTest, delFlagSient)
{
    runTestCommand(allSequence, "-FLAGS.SILENT (\\Seen)");
    ASSERT_THAT(session->outgoingData, testing::ElementsAre(taggedOkResponse()));
}

TEST_P(StoreTest, setFlagCompletes)
{
    auto future = runTestCommand(allSequence, "FLAGS (\\Flagged)");
    ASSERT_TRUE(future->ready());
}

TEST_P(StoreTest, setFlag)
{
    runTestCommand(allSequence, "FLAGS (\\Flagged)");
    auto messages = getFolderMessages(inboxFid);
    MatcherFunctor<MessageData> hasOnlyDeletedOrRecentFlag = [](const auto& msg) {
        auto flagsCopy = msg.flags;
        flagsCopy.delFlags(std::vector{ "\\Flagged"s, "\\Recent"s });
        return msg.flags.hasFlag("\\Flagged") && flagsCopy.empty();
    };
    ASSERT_THAT(
        messages, testing::Each(testing::ResultOf(hasOnlyDeletedOrRecentFlag, testing::Eq(true))));
}

TEST_P(StoreTest, setFlagResponse)
{
    runTestCommand(allSequence, "FLAGS (\\Flagged)");
    auto correctResponseData = makeFullStoreResponse(getFolderMessages(inboxFid));
    ASSERT_THAT(session->outgoingData, testing::UnorderedElementsAreArray(correctResponseData));
}

TEST_P(StoreTest, setFlagSient)
{
    runTestCommand(allSequence, "FLAGS.SILENT (\\Flagged)");
    ASSERT_THAT(session->outgoingData, testing::ElementsAre(taggedOkResponse()));
}

TEST_P(StoreTest, autoExpunge)
{
    auto future = runTestCommand(allSequence, "FLAGS (\\Deleted)");
    auto correctResponseData = makeFullStoreResponseWithAutoExpunge(getFolderMessages(inboxFid));
    ASSERT_THAT(session->outgoingData, testing::UnorderedElementsAreArray(correctResponseData));
    ASSERT_EQ(testMetaBackend->expungedMids(), testMetaBackend->getMidsByFid(inboxFid));
}

TEST_P(StoreTest, disabledAutoExpunge)
{
    disableAutoExpunge();
    runTestCommand(allSequence, "FLAGS (\\Deleted)");
    auto correctResponseData = makeFullStoreResponse(getFolderMessages(inboxFid));
    ASSERT_THAT(session->outgoingData, testing::UnorderedElementsAreArray(correctResponseData));
    ASSERT_TRUE(testMetaBackend->expungedMids().empty());
}

TEST_P(StoreTest, readOnlyFolder)
{
    selectFolderReadOnly("Inbox"s, inboxFid);
    auto future = runTestCommand(allSequence, "+FLAGS (\\Flagged)");
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(readOnlyResponse()));
}

TEST_F(StoreTest, storeEmptyMessagesRange)
{
    auto future = runCommand("STORE", emptySequence, "+FLAGS (\\Flagged)");
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(storeNoMessagesResponse()));
}

TEST_F(StoreTest, uidStoreEmptyMessagesRange)
{
    auto future = runCommand("UID STORE", emptySequence, "+FLAGS (\\Flagged)");
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(uidStoreNoMessagesResponse()));
}

INSTANTIATE_TEST_SUITE_P(InstantiationName, StoreTest, ::testing::Values("STORE"s, "UID STORE"s));
