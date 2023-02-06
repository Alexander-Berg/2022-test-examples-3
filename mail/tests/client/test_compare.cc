#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <tests/common.hpp>

#include <iostream>
#include <set>
#include <vector>
#include <algorithm>

imap_uidls_ptr testOldInterface(const string& implName)
{
    L_(info) << prefix;
    L_(info) << "Starting Old IMAP Client interface test for implementation " << implName;
    L_(info) << prefix << std::endl;

    imap_uidls_ptr resultFolders;

    try
    {
        auto clientModule = yplatform::find<ymod_imap_client::call>(implName);
        auto context = boost::make_shared<TestContext>();

        // Connecting...
        auto ipString = clientModule->connect(context, "imap.yandex.ru", 993, true, true).get();
        L_(info) << "Connected to [" << *ipString << "] imap server.";

        // Logging in...
        auto loginOk = clientModule->login(context, "imap.pg", "testqa", true).get();
        if (!loginOk) throw std::runtime_error("Failed to login");
        else
            L_(info) << "Login — OK";

        // Loading folders...
        std::set<string> folders = { "Drafts", "INBOX", "Outbox", "Sent", "Spam", "Trash" };
        std::set<string> intersection;

        resultFolders = clientModule->load_uidls(context, true).get();
        for (auto folder : resultFolders->children_)
        {
            if (folders.find(folder->name_) != folders.end()) intersection.insert(folder->name_);
            L_(info) << "\t\t FOLDER: " << folder->name_
                     << " uidvalidity: " << folder->uidvalidity_;
        }

        if (intersection.size() != folders.size())
            throw std::runtime_error("Failed to load correct folder list");
        else
            L_(info) << "Folder list — OK";

        clientModule->quit(context, true).get();

        L_(info) << prefix;
        L_(info) << "Old client test for " << implName << " implementation — OK";
    }
    catch (const std::exception& e)
    {
        L_(info) << "ImapTestFailed: " << e.what();
        L_(info) << prefix;
        L_(info) << "Old interface test for " << implName << " implementation — FAILED";
    }
    L_(info) << prefix << std::endl << std::endl << std::endl;
    return resultFolders;
}

void testImapClient()
{
    auto foldersNew = testOldInterface("imap_client");
    auto foldersOld = testOldInterface("imap_client_old");

    L_(info) << prefix;
    if (foldersOld && foldersNew && *foldersOld == *foldersNew)
    {
        L_(info) << "OK — Got equal uidls for both implementations";
    }
    else
    {
        L_(info) << "FAILED — Uidls are not equal";
    }
    L_(info) << prefix << std::endl;
}

void mainTest(const std::string& /* configPath */)
{
    testImapClient();
}
