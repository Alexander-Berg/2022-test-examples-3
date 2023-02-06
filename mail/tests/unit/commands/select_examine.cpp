#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct SelectExamineTest
    : public CommandTestBase
    , public ::testing::WithParamInterface<string>
{
    string testFolderName = "Inbox"s;
    string badEncodingFolderName = "папка"s;
    string nonExistingFolderName = "no-such-folder"s;
    string nonSelectableFolderName = "Zombie"s;
    string invalidFolderName = "||invalid"s;

    std::map<string, int> commands = { { "SELECT"s, CMD_SELECT }, { "EXAMINE"s, CMD_EXAMINE } };

    SelectExamineTest()
    {
        context->sessionState.setAuthenticated();
    }

    auto commandName()
    {
        // We use parametrized tests to run all cases for all similar commands
        return GetParam();
    }

    auto runCommand(const string& commandName, const string& folderName)
    {
        auto command = CommandTestBase::createCommand(commandName, folderName);
        startCommand(command);
        return command->getFuture();
    }

    auto runCommand(const string& folderName)
    {
        return runCommand(commandName(), folderName);
    }

    auto runExamine(const string& folderName)
    {
        return runCommand("EXAMINE", folderName);
    }

    auto runSelect(const string& folderName)
    {
        return runCommand("SELECT", folderName);
    }

    auto examineOkResponse()
    {
        return commandTag() + " OK [READ-ONLY] EXAMINE Completed."s;
    }

    auto selectOkResponse()
    {
        return commandTag() + " OK [READ-WRITE] SELECT Completed."s;
    }

    auto wrongSessionStateResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] "s + commandName() +
            " Wrong session state for command"s;
    }

    auto folderEncodingErrorResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] "s + commandName() + " Folder encoding error"s;
    }

    auto noSuchFolderResponse()
    {
        return commandTag() + " NO [CLIENTBUG] "s + commandName() + " No such folder"s;
    }

    auto nonselectableFolderResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] "s + commandName() + " Nonselectable folder"s;
    }

    auto invalidFolderResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] "s + commandName() + " Bad folder name"s;
    }

    auto getFolderByName(const std::string& folderName)
    {
        auto folderList = testMetaBackend->folderList();
        auto folderId = folderList->getDBFolderId(folderName);
        return testMetaBackend->getFolder(folderId).get();
    }

    auto makeCorrectExamineResponseData(FolderPtr folder)
    {
        std::vector<string> res;
        res.emplace_back(
            "* FLAGS (\\Answered \\Seen \\Draft "s + (folder->isShared() ? ""s : "\\Deleted "s) +
            "$Forwarded)"s);
        res.emplace_back("* "s + std::to_string(folder->messages_count()) + " EXISTS"s);
        res.emplace_back("* "s + std::to_string(folder->recent_messages()) + " RECENT"s);
        res.emplace_back(
            "* OK [PERMANENTFLAGS (\\Answered \\Seen \\Draft \\Flagged "s +
            (folder->isShared() ? ""s : "\\Deleted "s) + "$Forwarded \\*)] Limited"s);

        auto folderInfo = folder->copyFolderInfo();
        res.emplace_back("* OK [UIDNEXT "s + std::to_string(folderInfo.uidNext) + "] Ok"s);
        res.emplace_back("* OK [UIDVALIDITY "s + std::to_string(folderInfo.uidValidity) + "] Ok"s);
        return res;
    }
};

TEST_P(SelectExamineTest, examineFolder)
{
    auto future = runCommand(testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_FALSE(session->outgoingData.empty());

    auto correctResponseData = makeCorrectExamineResponseData(getFolderByName(testFolderName));
    auto fullResponseSize = correctResponseData.size() + 1;
    ASSERT_EQ(session->outgoingData.size(), fullResponseSize);

    for (int i = 0; i < correctResponseData.size(); i++)
    {
        ASSERT_THAT(session->outgoingData, testing::Contains(correctResponseData[i]));
    }
}

TEST_F(SelectExamineTest, examineOk)
{
    auto future = runExamine(testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(beginsWith(session->outgoingData.back(), examineOkResponse()))
        << session->dumpOutput();
    ASSERT_TRUE(context->sessionState.selectedFolder.readOnly());
}

TEST_F(SelectExamineTest, selectOk)
{
    auto future = runSelect(testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(beginsWith(session->outgoingData.back(), selectOkResponse()))
        << session->dumpOutput();
    ASSERT_FALSE(context->sessionState.selectedFolder.readOnly());
}

TEST_P(SelectExamineTest, examineNotAuthenticated)
{
    context->sessionState.state = yimap::ImapContext::SessionState::INIT;
    auto future = runCommand(testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], wrongSessionStateResponse()))
        << session->dumpOutput();
}

TEST_P(SelectExamineTest, examineBadEncoding)
{
    auto future = runCommand(badEncodingFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], folderEncodingErrorResponse()))
        << session->dumpOutput();
}

TEST_P(SelectExamineTest, examineNonExistingFolder)
{
    auto future = runCommand(nonExistingFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], noSuchFolderResponse()))
        << session->dumpOutput();
}

TEST_P(SelectExamineTest, examineNonSelectableFolder)
{
    auto future = runCommand(nonSelectableFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], nonselectableFolderResponse()))
        << session->dumpOutput();
}

TEST_P(SelectExamineTest, examineInvalidFolder)
{
    auto future = runCommand(invalidFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], invalidFolderResponse()))
        << session->dumpOutput();
}

INSTANTIATE_TEST_SUITE_P(
    InstantiationName,
    SelectExamineTest,
    ::testing::Values("SELECT"s, "EXAMINE"s));
