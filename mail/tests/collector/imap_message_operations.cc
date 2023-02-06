#include <collector_ng/imap/operations/imap_message_operations.h>

#include <gtest.h>

using namespace yrpopper::collector::operations;
using namespace yrpopper::collector;
using namespace yrpopper;

class ImapFetchFlagsTestable : public ImapFetchMessage
{
public:
    ImapFetchFlagsTestable(ImapMessageHelperPtr messageHelper)
        : ImapFetchMessage(AsyncCallback(), messageHelper)
    {
    }

    std::string testMapFolderName(const ymod_imap_client::Utf8MailboxName& name)
    {
        return mapFolderName(name);
    }
};

TEST(ImapMessageOperations, FETCH_FLAGS_MAPPING)
{
    auto settings = std::make_shared<CollectorSettings>();
    settings->folderNamesMap = { { "INBOX", "\\Inbox" } };
    auto baseHelper = std::make_shared<ImapHelper>(
        rpop_context_ptr(), settings, ImapFolderListPtr());
    auto messageHelper =
        std::make_shared<ImapMessageHelper>(*baseHelper, ImapFolderPtr(), ImapFolder::initialState);
    auto operation = std::make_shared<ImapFetchFlagsTestable>(messageHelper);

    ymod_imap_client::Utf8MailboxName name("NotHierarhicalName", '|');
    EXPECT_EQ(operation->testMapFolderName(name), "NotHierarhicalName");

    name = ymod_imap_client::Utf8MailboxName("Usual|Hierarhical|Name", '|');
    EXPECT_EQ(operation->testMapFolderName(name), "Usual|Hierarhical|Name");

    name = ymod_imap_client::Utf8MailboxName("INBOX|Hierarhical|Name", '|');
    EXPECT_EQ(operation->testMapFolderName(name), "\\Inbox|Hierarhical|Name");

    name = ymod_imap_client::Utf8MailboxName("INBOX|StillNotRuined", '|');
    EXPECT_EQ(operation->testMapFolderName(name), "\\Inbox|StillNotRuined");
}
