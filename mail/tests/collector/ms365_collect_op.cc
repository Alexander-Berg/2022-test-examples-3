#include <mocks/db_http_collector_interface.h>
#include <mocks/mock_manager.h>
#include <mocks/oauth.h>
#include <mocks/ms365_client.h>
#include <mocks/processor_service.h>
#include <collector_ng/http/ms365_collect_op.h>
#include <yplatform/util/unique_id.h>
#include <set>
#include <gtest.h>

using namespace yrpopper;
using namespace yrpopper::collector;

using yrpopper::collector::operations::MS365CollectOp;
using yrpopper::collector::CollectorSettings;

auto mockOauth = std::make_shared<mock::OauthServiceImpl>();
auto messageProcessor = std::make_shared<mock::ProcessorService>();
mock::mock_manager mock_manager{ std::static_pointer_cast<mock::mock>(mockOauth), std::static_pointer_cast<mock::mock>(messageProcessor) };

auto defaultCollectorSettings = std::make_shared<CollectorSettings>();

struct TestMS365CollectOpTraits
{
    using HttpClient = mock::MS365Client;
    using DBInterface = mock::HttpCollectorInterface;
    using DBInterfaceProvider = mock::InterfaceProvider;
    using MessageProcessor = mock::ProcessorService;
};

using TestMS365CollectOp = MS365CollectOp<TestMS365CollectOpTraits>;

class MS365CollectOpTestable : public TestMS365CollectOp
{
public:
    MS365CollectOpTestable(rpop_context_ptr context, const CollectorSettings& settings)
        : TestMS365CollectOp(context, settings)
    {
        mock_manager.init_mock();
        initMS365Client();
        initDBInterface();
    }

    http_folders_ptr testGetFolders()
    {
        return getFolders();
    }

    void testProcessFolder(http_folder_ptr folder)
    {
        processFolder(folder);
    }

    processor::MessagePtr testPrepareMessage(
        const MS365Message& msg,
        const std::string& folder_name,
        std::string&& content)
    {
        return prepareMessage(msg, folder_name, std::move(content));
    }

    void testProcessMessage(const MS365Message& msg, const std::string& folder_name)
    {
        processMessage(msg, folder_name);
    }

    void testProcessMessages(const MS365MessageList& messages, http_folder_ptr folder)
    {
        processMessages(messages, folder);
    }

    std::string testMapFolderName(const std::string& name, char delim)
    {
        return mapFolderName(name, delim);
    }

    std::shared_ptr<mock::MS365Client> getMS365Client()
    {
        return ms365Client;
    }

    std::shared_ptr<mock::HttpCollectorInterface> getDBInterface()
    {
        return dbInterface;
    }
};

rpop_context_ptr createContext()
{
    auto seed = 42ull;
    auto t = boost::make_shared<task>();
    auto ctx = boost::make_shared<rpop_context>(
        t, yplatform::util::make_unique_id(seed), false, false, std::make_shared<promise_void_t>());
    return ctx;
}

std::shared_ptr<MS365CollectOpTestable> createOp(
    std::shared_ptr<CollectorSettings> settings = nullptr)
{
    if (!settings) settings = defaultCollectorSettings;
    return std::make_shared<MS365CollectOpTestable>(createContext(), *settings);
}

MS365FolderList testMS365Folders()
{
    MS365FolderList folders = { { "fd1", "Inbox", 1, 99, "Inbox" },
                                  { "fd2", "Nested", 0, 2, "Inbox/Nested" },
                                  { "fd3", "Sent", 0, 3, "Sent" } };
    return folders;
}

MS365MessageList testMS365Messages()
{
    MS365MessageList messages = {
        { "m1", "f1", "<12345@ya.ru>", time_t{ 2000 }, time_t{ 1000 }, true, "Test 1" },
        { "m2", "f1", "<12346@ya.ru>", time_t{ 2001 }, time_t{ 1001 }, true, "Test 2" },
        { "m3", "f1", "<12347@ya.ru>", time_t{ 2002 }, time_t{ 1002 }, false, "Test 3" }
    };
    return messages;
}

http_folder_ptr testDBHttpFolder()
{
    return boost::make_shared<http_folder>(
        http_folder{ 1, "f1", "Inbox", time_t{}, time_t{}, 3, 2, 1, 0 });
}

http_folders_ptr testDBHttpFolders()
{
    auto folders = boost::make_shared<http_folders>();

    auto f1 = boost::make_shared<http_folder>(
        http_folder{ 1, "f1", "Inbox", time_t{}, time_t{}, 3, 2, 1, 0 });
    folders->emplace(f1->name, f1);

    auto f2 = boost::make_shared<http_folder>(
        http_folder{ 2, "f2", "Inbox/Nested", time_t{}, time_t{}, 3, 2, 1, 0 });
    folders->emplace(f2->name, f2);

    return folders;
}

TEST(MS365CollectOp, PrepareMessage_fill_fileds)
{
    auto op = createOp();

    MS365Message ms365Msg;
    ms365Msg.isRead = true;
    ms365Msg.internetMessageId = "12345";
    std::string folderName = "Inbox";
    std::string content = "content";

    auto msg = op->testPrepareMessage(ms365Msg, folderName, std::move(content));
    EXPECT_EQ(msg->seen, ms365Msg.isRead);
    EXPECT_EQ(msg->srcFolder, folderName);
    EXPECT_EQ(msg->dstFolder, folderName);
    EXPECT_EQ(msg->dstDelim, MS365_FOLDER_DELIM);
}

TEST(MS365CollectOp, getFolders_append_folder_if_not_exists)
{
    auto op = createOp();

    auto ms365Folders = testMS365Folders();
    op->getMS365Client()->setFetchFoldersResult(ms365Folders);

    auto folders = *op->testGetFolders().get();
    EXPECT_EQ(folders.size(), ms365Folders.size());
    EXPECT_TRUE(std::all_of(ms365Folders.begin(), ms365Folders.end(), [folders](auto& f) {
        return folders.count(f.path);
    })) << "All ms365 folders must be presented in result folders";

    for (auto&& oFolder : ms365Folders)
    {
        auto it = folders.find(oFolder.path);
        ASSERT_NE(it, folders.end());

        auto folder = it->second;
        EXPECT_EQ(folder->external_folder_id, oFolder.id);
        EXPECT_EQ(folder->name, oFolder.path);
        EXPECT_EQ(folder->message_count, oFolder.totalItemCount);
    }
}

TEST(MS365CollectOp, getFolders_update_folders_fetched_from_db)
{
    auto op = createOp();

    auto ms365Folders = testMS365Folders();
    op->getMS365Client()->setFetchFoldersResult(ms365Folders);
    auto dbFolders = testDBHttpFolders();
    op->getDBInterface()->setFolders(dbFolders);

    auto folders = *op->testGetFolders().get();
    EXPECT_EQ(folders.size(), ms365Folders.size());

    for (auto&& oFolder : ms365Folders)
    {
        auto it1 = dbFolders->find(oFolder.path);
        if (it1 == dbFolders->end()) continue;

        auto it2 = folders.find(oFolder.path);
        ASSERT_NE(it2, folders.end());

        auto folder = it2->second;
        EXPECT_EQ(folder->external_folder_id, oFolder.id);
        EXPECT_EQ(folder->name, oFolder.path);
        EXPECT_EQ(folder->message_count, oFolder.totalItemCount);
    }
}

TEST(MS365CollectOp, getFolders_skip_folder_if_removed_in_external)
{
    auto op = createOp();

    MS365FolderList ms365Folders = { { "fd1", "Inbox", 1, 99, "Inbox" } };
    op->getMS365Client()->setFetchFoldersResult(ms365Folders);
    auto dbFolders = testDBHttpFolders();
    op->getDBInterface()->setFolders(dbFolders);

    auto folders = *op->testGetFolders().get();
    EXPECT_EQ(folders.size(), ms365Folders.size());

    for (auto&& oFolder : ms365Folders)
    {
        auto it2 = folders.find(oFolder.path);
        ASSERT_NE(it2, folders.end());
    }
}

TEST(MS365CollectOp, proccessFolder_update_messagesCount_collectedCount_lastSyncedTs_in_db)
{
    auto op = createOp();

    auto folder = testDBHttpFolder();
    auto copy = boost::make_shared<http_folder>(*folder);
    auto messages = testMS365Messages();
    op->getMS365Client()->setFetchMessagesResult(messages);

    op->testProcessFolder(folder);

    auto stored = op->getDBInterface()->lastUpdatedOrCreatedFolder;
    ASSERT_NE(stored, nullptr);
    EXPECT_EQ(stored->message_count, copy->message_count);
    EXPECT_EQ(stored->collected_count - copy->collected_count, messages.size());
    EXPECT_EQ(folder->last_synced_message_ts, messages.back().lastModifiedDateTime);
    EXPECT_EQ(folder->last_synced_message_received_ts, messages.back().receivedDateTime);
}

TEST(MS365CollectOp, proccessFolder_update_errorCount_badRetries_in_db)
{
    auto settings = std::make_shared<CollectorSettings>();
    settings->httpCollector.maxBadRetriesPerMessage = 1;
    auto op = createOp(settings);

    auto folder = testDBHttpFolder();
    auto copy = boost::make_shared<http_folder>(*folder);
    op->getMS365Client()->setFetchMessagesResult(testMS365Messages());
    op->getMS365Client()->setDownloadMessageResult(
        std::make_exception_ptr(ymod_httpclient::request_timeout_error()));

    op->testProcessFolder(folder);
    op->testProcessFolder(folder);

    auto stored = op->getDBInterface()->lastUpdatedOrCreatedFolder;
    ASSERT_NE(stored, nullptr);
    EXPECT_EQ(stored->bad_message_retries, 1u);
    EXPECT_EQ(stored->error_count - copy->error_count, 1u);
}

TEST(MS365CollectOp, processFolder_update_folder_in_db_after_error_on_fetchMessages)
{
    auto op = createOp();

    auto folder = testDBHttpFolder();
    auto copy = boost::make_shared<http_folder>(*folder);
    op->getMS365Client()->setFetchMessagesResult(
        std::make_exception_ptr(ymod_httpclient::request_timeout_error()));

    EXPECT_THROW(op->testProcessFolder(folder), TimeoutError);

    auto stored = op->getDBInterface()->lastUpdatedOrCreatedFolder;
    ASSERT_NE(stored, nullptr);
    EXPECT_EQ(stored->message_count, copy->message_count);
}

TEST(MS365CollectOp, procccessMessages_update_folder_counters_on_success)
{
    auto op = createOp();

    auto folder = testDBHttpFolder();
    auto before = boost::make_shared<http_folder>(*folder);
    auto messages = testMS365Messages();

    op->testProcessMessages(messages, folder);

    EXPECT_EQ(folder->message_count, before->message_count);
    EXPECT_EQ(folder->collected_count - before->collected_count, messages.size());
    EXPECT_EQ(folder->bad_message_retries, 0u);
    EXPECT_EQ(folder->error_count - before->error_count, 0u);
    EXPECT_EQ(folder->last_synced_message_ts, messages.back().lastModifiedDateTime);
    EXPECT_EQ(folder->last_synced_message_received_ts, messages.back().receivedDateTime);
}

TEST(MS365CollectOp, proccessMessages_update_badRetries_on_error)
{
    auto settings = std::make_shared<CollectorSettings>();
    settings->httpCollector.maxBadRetriesPerMessage = 1;
    auto op = createOp(settings);

    auto folder = testDBHttpFolder();
    auto before = boost::make_shared<http_folder>(*folder);
    auto messages = testMS365Messages();
    op->getMS365Client()->setDownloadMessageResult(
        std::make_exception_ptr(ymod_httpclient::request_timeout_error()));

    op->testProcessMessages(messages, folder);

    EXPECT_EQ(folder->message_count, before->message_count);
    EXPECT_EQ(folder->collected_count - before->collected_count, 0u);
    EXPECT_EQ(folder->bad_message_retries, 1u);
    EXPECT_EQ(folder->error_count - before->error_count, 0u);
    EXPECT_EQ(folder->last_synced_message_ts, before->last_synced_message_ts);
    EXPECT_EQ(folder->last_synced_message_received_ts, before->last_synced_message_received_ts);
}

TEST(MS365CollectOp, proccessMessages_update_errorCount_on_error)
{
    auto settings = std::make_shared<CollectorSettings>();
    settings->httpCollector.maxBadRetriesPerMessage = 1;
    auto op = createOp(settings);

    auto folder = testDBHttpFolder();
    auto before = boost::make_shared<http_folder>(*folder);
    auto messages = testMS365Messages();
    op->getMS365Client()->setDownloadMessageResult(
        std::make_exception_ptr(ymod_httpclient::request_timeout_error()));

    op->testProcessMessages(messages, folder);
    op->testProcessMessages(messages, folder);

    EXPECT_EQ(folder->message_count, before->message_count);
    EXPECT_EQ(folder->collected_count - before->collected_count, 0u);
    EXPECT_EQ(folder->bad_message_retries, 1u);
    EXPECT_EQ(folder->error_count - before->error_count, 1u);
    EXPECT_EQ(folder->last_synced_message_ts, messages.front().lastModifiedDateTime);
    EXPECT_EQ(folder->last_synced_message_received_ts, messages.front().receivedDateTime);
}

TEST(MS365CollectOp, proccessMessages_skip_messages_after_bad_retries)
{
    auto op = createOp();

    auto folder = testDBHttpFolder();
    auto before = boost::make_shared<http_folder>(*folder);
    auto messages = testMS365Messages();
    op->getMS365Client()->setDownloadMessageResult(
        std::make_exception_ptr(ymod_httpclient::request_timeout_error()));

    op->testProcessMessages(messages, folder);

    EXPECT_EQ(folder->message_count, before->message_count);
    EXPECT_EQ(folder->collected_count - before->collected_count, 0u);
    EXPECT_EQ(folder->bad_message_retries, 0u);
    EXPECT_EQ(folder->error_count - before->error_count, messages.size());
    EXPECT_EQ(folder->last_synced_message_ts, messages.back().lastModifiedDateTime);
    EXPECT_EQ(folder->last_synced_message_received_ts, messages.back().receivedDateTime);
}
