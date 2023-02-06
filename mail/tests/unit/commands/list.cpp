#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct ListLsubTest : public CommandTestBase
{
    ListLsubTest()
    {
        context->sessionState.setAuthenticated();
    }

    auto runCommand(const string& name, const string& referenceName, const string& folderWildcard)
    {
        return createAndStartCommand(
            name, "\""s + referenceName + "\" \""s + folderWildcard + "\""s);
    }

    auto runList(const string& referenceName, const string& folderWildcard)
    {
        return runCommand("LIST", referenceName, folderWildcard);
    }

    auto runLSub(const string& referenceName, const string& folderWildcard)
    {
        return runCommand("LSUB", referenceName, folderWildcard);
    }

    auto runXList(const string& referenceName, const string& folderWildcard)
    {
        return runCommand("XLIST", referenceName, folderWildcard);
    }

    auto taggedOkResponse(const string& command)
    {
        return commandTag() + " OK "s + command + " Completed."s;
    }

    auto makeFolderFlags(FullFolderInfoPtr folder)
    {
        string res = folder->hasChildren ? "\\HasChildren"s : "\\HasNoChildren"s;
        if (folder->symbol == "zombie")
        {
            return res + " \\Noselect"s;
        }

        res += " \\Unmarked";
        if (folder->symbol == "inbox")
        {
            res += " \\NoInferiors";
        }
        else if (folder->symbol == "spam")
        {
            res += " \\Junk";
        }
        return res;
    }

    auto makeXlistFolderFlags(FullFolderInfoPtr folder)
    {
        auto xListFlag = testMetaBackend->getFolders().get()->xlistFromSymbol(folder->symbol);
        auto flags = makeFolderFlags(folder);
        if (xListFlag.size())
        {
            flags += " "s + xListFlag;
        }
        return flags;
    }

    auto makeUntaggedFolderResponse(const string& command, const string& flags, const string& name)
    {
        bool quoted = false;
        for (auto c : name)
        {
            if (!isalnum(c))
            {
                quoted = true;
                break;
            }
        }
        return "* "s + command + " ("s + flags + ") \"|\" "s + (quoted ? "\""s : ""s) + name +
            (quoted ? "\""s : ""s);
    }

    auto makeFullListResponse()
    {
        std::vector<std::string> result;
        auto folderList = testMetaBackend->getFolders().get();
        for (auto& pair : *folderList)
        {
            result.push_back(
                makeUntaggedFolderResponse("LIST", makeFolderFlags(pair.second), pair.first));
        }
        result.push_back(taggedOkResponse("LIST"));
        return result;
    }

    auto makeFullXListResponse()
    {
        std::vector<std::string> result;
        auto folderList = testMetaBackend->getFolders().get();
        for (auto& pair : *folderList)
        {
            result.push_back(
                makeUntaggedFolderResponse("XLIST", makeXlistFolderFlags(pair.second), pair.first));
        }
        result.push_back(taggedOkResponse("XLIST"));
        return result;
    }

    auto makeFullLSubResponse()
    {
        std::vector<std::string> result;
        auto folderList = testMetaBackend->getFolders().get();
        prepareSubsribedParents(folderList);

        for (auto& pair : *folderList)
        {
            if (!pair.second->subscribed) continue;

            result.push_back(
                makeUntaggedFolderResponse("LSUB", makeFolderFlags(pair.second), pair.first));
        }
        result.push_back(taggedOkResponse("LSUB"));
        return result;
    }

    auto makeInboxUntaggedListResponse()
    {
        auto inbox = testMetaBackend->getFolders().get()->at("Inbox");
        return makeUntaggedFolderResponse("LIST", makeFolderFlags(inbox), "INBOX");
    }

    void prepareSubsribedParents(FolderListPtr folders)
    {
        for (auto& [name, folder] : *folders)
        {
            if (folder->subscribed)
            {
                auto parentName = name;
                size_t pos;
                while ((pos = parentName.rfind("|")) != string::npos)
                {
                    parentName = parentName.substr(0, pos);
                    auto parentFolder = folders->at(parentName);

                    if (parentFolder->subscribed) break;

                    parentFolder->subscribed = true;
                    parentFolder->symbol = "zombie";
                }
            }
        }
    }
};

TEST_F(ListLsubTest, listAllFolders)
{
    auto future = runList("", "*");
    ASSERT_TRUE(future->ready());
    ASSERT_THAT(session->outgoingData, testing::ElementsAreArray(makeFullListResponse()));
}

TEST_F(ListLsubTest, listInbox)
{
    auto future = runList("", "inb*");
    ASSERT_TRUE(future->ready());
    // XXX
    auto inboxUntaggedResponse = makeUntaggedFolderResponse(
        "LIST", makeFolderFlags(testMetaBackend->getFolders().get()->at("Inbox")), "INBOX");
    ASSERT_THAT(
        session->outgoingData,
        testing::ElementsAre(makeInboxUntaggedListResponse(), taggedOkResponse("LIST")));
}

TEST_F(ListLsubTest, xlistAllFolders)
{
    auto future = runXList("", "*");
    ASSERT_TRUE(future->ready());
    ASSERT_THAT(session->outgoingData, testing::ElementsAreArray(makeFullXListResponse()));
}

TEST_F(ListLsubTest, lsubAllFolders)
{
    auto future = runLSub("", "*");
    ASSERT_TRUE(future->ready());
    ASSERT_THAT(session->outgoingData, testing::ElementsAreArray(makeFullLSubResponse()));
}
