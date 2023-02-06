#define BOOST_SPIRIT_THREADSAFE
#define PHOENIX_THREADSAFE

#include "../mocks/mock_backend.h"
#include "../mocks/mbody.h"
#include "../mocks/session.h"
#include "../mocks/auth.h"
#include "../mocks/user_settings.h"
#include "../mocks/notifications.h"
#include "../mocks/search.h"
#include "../mocks/append.h"
#include <src/commands/factory.h>
#include <src/parser/grammar.h>
#include <src/common/zerocopy.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <sstream>

using namespace yimap;
using namespace yimap::backend;

struct CommandTestBase : ::testing::Test
{
    IOService io;
    ImapContextPtr context{ new ImapContext(io) };
    boost::shared_ptr<TestSession> session{ new TestSession(context) };
    std::shared_ptr<TestAuthBackend> testAuthBackend{ new TestAuthBackend };
    FakeMailbox mailbox{ createFakeMailboxFromJsonFile("data/fake_mailbox_cfg.json") };
    std::shared_ptr<MockBackend> testMetaBackend{ new MockBackend(context, mailbox) };
    std::shared_ptr<TestMbodyBackend> testMbodyBackend{ new TestMbodyBackend() };
    std::shared_ptr<TestUserSettingsBackend> testUserSettingsBackend{ new TestUserSettingsBackend };
    std::shared_ptr<TestNotificationsBackend> testNotifications{ new TestNotificationsBackend };
    std::shared_ptr<TestSearchBackend> testSearch{ new TestSearchBackend };
    std::shared_ptr<TestAppendBackend> testAppend{ new TestAppendBackend };
    SettingsPtr settings{ new Settings };
    std::shared_ptr<UserSettings> userSettings{ new UserSettings };
    backend::UserJournalPtr stubJournal = nullptr;

    string commandTag()
    {
        return "test-tag"s;
    }

    CommandTestBase()
    {
        context->stats = StatsPtr{ new Stats };
        context->settings = settings;
        context->userSettings = userSettings;
    }

    auto createAndStartCommand(const string& name, const string& args)
    {
        auto command = createCommand(name, args);
        startCommand(command);
        return command->getFuture();
    }

    CommandPtr createCommand(const string& name, const string& args)
    {
        using CommandData = std::pair<zerocopy::Segment, CommandPtr>;
        auto commandData = std::make_shared<CommandData>();
        zerocopy::Buffer buffer;
        std::ostream stream(&buffer);
        if (args.length())
        {
            stream << commandTag() << " "s << name << " "s << args << "\r\n"s;
        }
        else
        {
            stream << commandTag() << " "s << name << "\r\n"s;
        }
        commandData->first = buffer.detach(buffer.end());
        yimap::parser::ImapGrammar grammar;
        auto result = BSP::ast_parse<TreeFactory>(
            commandData->first.begin(), commandData->first.end(), grammar, BSP::nothing_p);
        auto ast = std::make_shared<CommandAST>(std::move(result));
        commandData->second = createCommandFromAst(ast);

        return std::shared_ptr<ImapCommand>(commandData, commandData->second.get());
    }

    CommandPtr createCommandFromAst(CommandASTPtr ast)
    {
        ImapCommandArgs args = { context,
                                 session,
                                 testAuthBackend,
                                 testMetaBackend,
                                 testMbodyBackend,
                                 testUserSettingsBackend,
                                 testNotifications,
                                 testSearch,
                                 testAppend,
                                 stubJournal,
                                 settings,
                                 &context->sessionLogger,
                                 ast };

        auto command = createImapCommand(ast, args);
        if (command)
        {
            command->init(context->stats);
        }
        return command;
    };

    void startCommand(CommandPtr command)
    {
        io.post([&]() { command->start(); });
        runIO();
    }

    void runIO()
    {
        io.reset();
        io.run_for(std::chrono::milliseconds(10));
    }

    FolderInfo selectFolder(const string& name, const string& fid)
    {
        testMetaBackend->folderPtrs.clear();
        auto folder = getFolder(name, fid);
        context->sessionState.selectFolder(folder);
        return folder->copyFolderInfo();
    }

    FolderInfo selectFolderReadOnly(const string& name, const string& fid)
    {
        auto folder = getFolder(name, fid);
        context->sessionState.selectFolderReadOnly(folder);
        return folder->copyFolderInfo();
    }

    FolderInfo getFolderInfo(const string& name, const string& fid)
    {
        return getFolder(name, fid)->copyFolderInfo();
    }

    FolderPtr getFolder(const string& name, const string& fid)
    {
        return testMetaBackend->getFolder(DBFolderId(name, fid)).get();
    }

    void markFolderShared(const string& fid)
    {
        for (auto& [name, folder] : *context->foldersCache.getFolders())
        {
            if (folder->fid == fid)
            {
                folder->isShared = true;
                return;
            }
        }
        throw std::runtime_error("folder with fid=" + fid + " not found");
    }

    void sendAuthResponse(AuthResult response)
    {
        if (testAuthBackend->promises.empty())
        {
            throw std::runtime_error("no promises");
        }
        testAuthBackend->promises.back().set(response);
        runIO();
    }

    void simulateAuthException()
    {
        if (testAuthBackend->promises.empty())
        {
            throw std::runtime_error("no promises");
        }
        testAuthBackend->promises.back().set_exception(std::domain_error("fail"));
        runIO();
    }

    void sendSettingsResponse(UserSettings response)
    {
        if (testUserSettingsBackend->promises.empty())
        {
            throw std::runtime_error("no promises");
        }
        testUserSettingsBackend->promises.back().set(response);
        runIO();
    }

    void simulateLoadSettingsException()
    {
        if (testUserSettingsBackend->promises.empty())
        {
            throw std::runtime_error("no promises");
        }
        testUserSettingsBackend->promises.back().set_exception(std::domain_error("fail"));
        runIO();
    }

    AuthResult goodAuthResponse(const string& uid, const string& login)
    {
        AuthResult ret;
        ret.loginFail = false;
        ret.uid = uid;
        ret.login = login;
        return ret;
    }

    AuthResult badAuthResponse(const string& uid, const string& login)
    {
        AuthResult ret;
        ret.loginFail = true;
        ret.uid = uid;
        ret.login = login;
        return ret;
    }

    AuthResult badKarmaAuthResponse(const string& uid, const string& login)
    {
        AuthResult ret;
        ret.loginFail = false;
        ret.karmaFail = true;
        ret.uid = uid;
        ret.login = login;
        return ret;
    }

    bool beginsWith(const string& str, const string& substr)
    {
        return str.find(substr) == 0;
    }

    virtual ~CommandTestBase() = default;
};
