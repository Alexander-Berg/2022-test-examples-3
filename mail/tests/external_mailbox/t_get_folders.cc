#include <mailbox/external/get_folders.h>

#include <ymod_imapclient/imap_result.h>
#include <yplatform/coroutine.h>
#include <yplatform/log.h>
#include <yplatform/application/config/yaml_to_ptree.h>

#include <catch.hpp>
#include <memory>

using namespace xeno;
using namespace xeno::mailbox::external;

static const std::string EXTERNAL_MAILBOX_CONF{ "input_data/external_mailbox_settings.yml" };

struct imap_client_fake
{
    template <typename Handler>
    void list(Handler&& h)
    {
        h(code::ok, folders);
    }

    const ymod_imap_client::Capability& get_capability()
    {
        return capability;
    }

    ymod_imap_client::ImapListPtr folders =
        std::make_shared<ymod_imap_client::ImapList>(ymod_imap_client::ImapMailboxList());
    ymod_imap_client::Capability capability;
};

struct convert_imap_folders_test : public yplatform::log::contains_logger
{
    convert_imap_folders_test()
    {
        yplatform::ptree ptree;
        utils::config::yaml_to_ptree::convert(EXTERNAL_MAILBOX_CONF, ptree);
        conf->update(ptree);
    }

    imap_client_fake client;
    settings_ptr conf = std::make_shared<settings>();

    void add_imap_folder(const std::string& name, uint32_t flags = 0)
    {
        auto utf8_name = ymod_imap_client::Utf8MailboxName(name, '|');
        client.folders->mailboxes.push_back(
            std::make_shared<ymod_imap_client::ImapListItem>(utf8_name, flags));
    }
};

TEST_CASE_METHOD(convert_imap_folders_test, "determine folder type by flag and by name")
{
    add_imap_folder("IAmSent", ymod_imap_client::ListResponse::ff_sent);
    add_imap_folder("INBOX");
    add_imap_folder("IAmUser");

    auto folders = convert_imap_folders(client.folders, conf->folders_types);
    for (auto& folder : *folders)
    {
        if (folder.path.to_string() == "IAmSent")
        {
            REQUIRE(folder.type == mailbox::folder::type_t::sent);
        }

        if (folder.path.to_string() == "INBOX")
        {
            REQUIRE(folder.type == mailbox::folder::type_t::inbox);
        }

        if (folder.path.to_string() == "IAmUser")
        {
            REQUIRE(folder.type == mailbox::folder::type_t::user);
        }
    }
}

TEST_CASE_METHOD(
    convert_imap_folders_test,
    "only one system folder determined when multiple candidates by name")
{
    add_imap_folder("Spam");
    add_imap_folder("Junk");

    auto folders = convert_imap_folders(client.folders, conf->folders_types);
    for (auto& folder : *folders)
    {
        if (folder.path.to_string() == "Spam")
        {
            REQUIRE(folder.type == mailbox::folder::type_t::spam);
        }
        else
        {
            REQUIRE(folder.type == mailbox::folder::type_t::user);
        }
    }
}

TEST_CASE_METHOD(
    convert_imap_folders_test,
    "only one system folder determined when multiple candidates by flags")
{
    add_imap_folder("IamSpam", ymod_imap_client::ListResponse::ff_spam);
    add_imap_folder("IamJunk", ymod_imap_client::ListResponse::ff_spam);
    add_imap_folder("IamCustomName", ymod_imap_client::ListResponse::ff_spam);

    auto folders = convert_imap_folders(client.folders, conf->folders_types);
    for (auto& folder : *folders)
    {
        if (folder.path.to_string() == "IamSpam")
        {
            REQUIRE(folder.type == mailbox::folder::type_t::spam);
        }
        else
        {
            REQUIRE(folder.type == mailbox::folder::type_t::user);
        }
    }
}

TEST_CASE_METHOD(
    convert_imap_folders_test,
    "flags have higher priority than name for type determination")
{
    add_imap_folder("Junk");
    add_imap_folder("NoSpam", ymod_imap_client::ListResponse::ff_spam);
    add_imap_folder("Spam");

    auto folders = convert_imap_folders(client.folders, conf->folders_types);
    for (auto& folder : *folders)
    {
        if (folder.path.to_string() == "NoSpam")
        {
            REQUIRE(folder.type == mailbox::folder::type_t::spam);
        }
        else
        {
            REQUIRE(folder.type == mailbox::folder::type_t::user);
        }
    }
}

TEST_CASE_METHOD(convert_imap_folders_test, "not add Gmail folder if there no child")
{
    add_imap_folder("[Gmail]", ymod_imap_client::ListResponse::ff_noselect);
    add_imap_folder("Spam");

    auto folders = fix_folders_hierarchy(client.folders, true);
    REQUIRE(folders->mailboxes.size() == 1);
    REQUIRE(folders->mailboxes[0]->name.asString() == "Spam");
}

TEST_CASE_METHOD(convert_imap_folders_test, "not add Gmail folder if there no child with user type")
{
    add_imap_folder("[Gmail]", ymod_imap_client::ListResponse::ff_noselect);
    add_imap_folder("[Gmail]|Spam", ymod_imap_client::ListResponse::ff_spam);

    auto folders = fix_folders_hierarchy(client.folders, true);
    REQUIRE(folders->mailboxes.size() == 1);
    REQUIRE(folders->mailboxes[0]->name.asString() == "[Gmail]|Spam");
}

TEST_CASE_METHOD(convert_imap_folders_test, "add Gmail folder if there child with user type")
{
    add_imap_folder("[Gmail]", ymod_imap_client::ListResponse::ff_noselect);
    add_imap_folder("[Gmail]|CustomName");

    auto folders = fix_folders_hierarchy(client.folders, true);
    REQUIRE(folders->mailboxes.size() == 2);
    REQUIRE(folders->mailboxes[0]->name.asString() == "[Gmail]");
    REQUIRE(folders->mailboxes[1]->name.asString() == "[Gmail]|CustomName");
}

TEST_CASE_METHOD(
    convert_imap_folders_test,
    "add Gmail folder only once if there children with user type")
{
    add_imap_folder("[A", ymod_imap_client::ListResponse::ff_noselect);
    add_imap_folder("[GB");
    add_imap_folder("[Gmail]|Spam", ymod_imap_client::ListResponse::ff_spam);
    add_imap_folder("[Gmail]", ymod_imap_client::ListResponse::ff_noselect);
    add_imap_folder("[Gmail]|Sx");
    add_imap_folder("[Gmail]|Sw");

    auto folders = fix_folders_hierarchy(client.folders, true);
    REQUIRE(folders->mailboxes.size() == 6);
}

TEST_CASE_METHOD(convert_imap_folders_test, "add missed parent")
{
    add_imap_folder("Parent|Child");

    auto folders = fix_folders_hierarchy(client.folders, false);
    REQUIRE(folders->mailboxes.size() == 2);
    REQUIRE(folders->mailboxes[0]->name.asString() == "Parent");
    REQUIRE(folders->mailboxes[1]->name.asString() == "Parent|Child");
}

TEST_CASE_METHOD(convert_imap_folders_test, "add missed parent only one time with several children")
{
    add_imap_folder("Parent|Child1");
    add_imap_folder("Parent|Child2");

    auto folders = fix_folders_hierarchy(client.folders, false);
    REQUIRE(folders->mailboxes.size() == 3);
    REQUIRE(folders->mailboxes[0]->name.asString() == "Parent");
    REQUIRE(folders->mailboxes[1]->name.asString() == "Parent|Child1");
    REQUIRE(folders->mailboxes[2]->name.asString() == "Parent|Child2");
}

TEST_CASE_METHOD(convert_imap_folders_test, "fix_folders_hierarchy sorts result")
{
    add_imap_folder("X");
    add_imap_folder("X|Y|Z");
    add_imap_folder("A|B");
    add_imap_folder("A|C|D|E");
    add_imap_folder("A|C|F");
    add_imap_folder("A|C|F|F|F");
    add_imap_folder("[Gmail]|Sx");
    add_imap_folder("[Gmail]|Spam", ymod_imap_client::ListResponse::ff_spam);
    add_imap_folder("[Gmail]", ymod_imap_client::ListResponse::ff_noselect);

    auto folders = fix_folders_hierarchy(client.folders, true);
    REQUIRE(folders->mailboxes.size() == 14);
    REQUIRE(folders->mailboxes[0]->name.asString() == "A");
    REQUIRE(folders->mailboxes[1]->name.asString() == "A|B");
    REQUIRE(folders->mailboxes[2]->name.asString() == "A|C");
    REQUIRE(folders->mailboxes[3]->name.asString() == "A|C|D");
    REQUIRE(folders->mailboxes[4]->name.asString() == "A|C|D|E");
    REQUIRE(folders->mailboxes[5]->name.asString() == "A|C|F");
    REQUIRE(folders->mailboxes[6]->name.asString() == "A|C|F|F");
    REQUIRE(folders->mailboxes[7]->name.asString() == "A|C|F|F|F");
    REQUIRE(folders->mailboxes[8]->name.asString() == "X");
    REQUIRE(folders->mailboxes[9]->name.asString() == "X|Y");
    REQUIRE(folders->mailboxes[10]->name.asString() == "X|Y|Z");
    REQUIRE(folders->mailboxes[11]->name.asString() == "[Gmail]");
    REQUIRE(folders->mailboxes[12]->name.asString() == "[Gmail]|Spam");
    REQUIRE(folders->mailboxes[13]->name.asString() == "[Gmail]|Sx");
}
