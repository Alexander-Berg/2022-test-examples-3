#include "common.h"

#include <yplatform/log.h>
#include <catch.hpp>

namespace mb = xeno::mailbox;

struct cache_mailbox_test
{
    using cache_mailbox_ptr = std::shared_ptr<mb::cache_mailbox>;
    using error = xeno::error;

    cache_mailbox_test()
    {
        YLOG_GLOBAL(info) << "cache_mailbox_test() -----------------------------";
    }

    cache_mailbox_ptr cache_mb = std::make_shared<mb::cache_mailbox>();
};

TEST_CASE_METHOD(cache_mailbox_test, "test get subfolders")
{
    auto compare_folders = [](const mb::folder& lhs, const mb::folder& rhs) {
        return lhs.path.to_string() == rhs.path.to_string();
    };
    char delim = '/';
    auto a = mb::folder(mb::path_t("a", delim), "a");
    auto ab = mb::folder(mb::path_t("ab", delim), "ab");
    auto abc = mb::folder(mb::path_t("abc", delim), "abc");
    auto a_b = mb::folder(mb::path_t("a/b", delim), "a/b");
    auto a_b_a = mb::folder(mb::path_t("a/b/a", delim), "a/b/a");
    auto a_b_a_b = mb::folder(mb::path_t("a/b/a/b", delim), "a/b/a/b");
    auto a_b_c = mb::folder(mb::path_t("a/b/c", delim), "a/b/c");
    auto b = mb::folder(mb::path_t("b", delim), "b");
    auto b_c = mb::folder(mb::path_t("b/c", delim), "b/c");
    mb::folder_vector folders{ a, ab, abc, a_b, a_b_a, a_b_a_b, a_b_c, b, b_c };
    cache_mb->update_folders_from_external(std::make_shared<mb::folder_vector>(folders));
    auto subfolders_a = cache_mb->get_subfolders(a.fid);
    REQUIRE(subfolders_a->size() == 4);
    REQUIRE(compare_folders((*subfolders_a)[0], a_b));
    REQUIRE(compare_folders((*subfolders_a)[1], a_b_a));
    REQUIRE(compare_folders((*subfolders_a)[2], a_b_a_b));
    REQUIRE(compare_folders((*subfolders_a)[3], a_b_c));
    auto subfolders_a_b_a = cache_mb->get_subfolders(a_b_a.fid);
    REQUIRE(subfolders_a_b_a->size() == 1);
    REQUIRE(compare_folders((*subfolders_a_b_a)[0], a_b_a_b));
    auto subfolders_ab = cache_mb->get_subfolders(ab.fid);
    REQUIRE(subfolders_ab->empty());
    auto subfolders_b = cache_mb->get_subfolders(b.fid);
    REQUIRE(subfolders_b->size() == 1);
    REQUIRE(compare_folders((*subfolders_b)[0], b_c));
}

TEST_CASE_METHOD(cache_mailbox_test, "test correct update type: user->system")
{
    char delim = '|';
    mb::fid_t fid = "1";

    auto folder = mb::folder(mb::path_t("was_user_now_system", delim), fid);
    folder.type = mb::folder::type_t::user;

    mb::folder_vector folders{ folder };
    cache_mb->update_folders_from_local(std::make_shared<mb::folder_vector>(folders));

    folder.type = mb::folder::type_t::inbox;
    folder.fid = "";
    mb::folder_vector new_folders{ folder };
    cache_mb->update_folders_from_external(std::make_shared<mb::folder_vector>(new_folders));
    REQUIRE(cache_mb->get_folder(folder.path).status == mb::folder::status_t::to_delete);
}

TEST_CASE_METHOD(cache_mailbox_test, "test correct update type: system->system")
{
    char delim = '|';

    mb::fid_t old_outbox_fid = "1";
    auto old_outbox = mb::folder(mb::path_t("was_outbox_become_inbox", delim), old_outbox_fid);
    old_outbox.type = mb::folder::type_t::outbox;

    mb::fid_t old_inbox_fid = "2";
    auto old_inbox = mb::folder(mb::path_t("was_inbox", delim), old_inbox_fid);
    old_inbox.type = mb::folder::type_t::inbox;

    mb::folder_vector folders{ old_outbox, old_inbox };
    cache_mb->update_folders_from_local(std::make_shared<mb::folder_vector>(folders));

    mb::fid_t now_inbox_fid;
    auto now_inbox = mb::folder(mb::path_t("was_outbox_become_inbox", delim), now_inbox_fid);
    now_inbox.type = mb::folder::type_t::inbox;

    old_inbox.fid = "";

    mb::folder_vector new_folders{ now_inbox, old_inbox };
    cache_mb->update_folders_from_external(std::make_shared<mb::folder_vector>(new_folders));
    REQUIRE(cache_mb->get_folder(old_outbox.path).status == mb::folder::status_t::to_delete);
}

TEST_CASE_METHOD(cache_mailbox_test, "test correct update type: system->user")
{
    char delim = '|';
    mb::fid_t fid = "1";
    auto folder = mb::folder(mb::path_t("was_system_now_user", delim), fid);
    folder.type = mb::folder::type_t::inbox;

    mb::folder_vector folders{ folder };
    cache_mb->update_folders_from_local(std::make_shared<mb::folder_vector>(folders));

    folder.type = mb::folder::type_t::user;
    folder.fid = "";
    mb::folder_vector new_folders{ folder };
    cache_mb->update_folders_from_external(std::make_shared<mb::folder_vector>(new_folders));
    REQUIRE(cache_mb->get_folder(folder.path).status == mb::folder::status_t::to_delete);
}

TEST_CASE_METHOD(
    cache_mailbox_test,
    "test mark to delete system folder with type in initial_folders")
{
    char delim = '|';
    mb::fid_t fid = "1";
    auto folder = mb::folder(mb::path_t("inbox", delim), fid);
    folder.type = mb::folder::type_t::inbox;
    mb::folder_vector folders{ folder };
    cache_mb->update_folders_from_local(std::make_shared<mb::folder_vector>(folders));

    auto system_folder = mb::folder(mb::path_t("", delim), fid);
    system_folder.type = mb::folder::type_t::inbox;
    mb::folder_vector initial_folders{ system_folder };
    cache_mb->set_initial_folders(std::make_shared<mb::folder_vector>(initial_folders));

    folder.type = mb::folder::type_t::drafts;
    folder.fid = "";
    folders = { folder };
    cache_mb->update_folders_from_external(std::make_shared<mb::folder_vector>(folders));
    REQUIRE(cache_mb->get_folder(folder.path).status == mb::folder::status_t::to_delete);
}

TEST_CASE_METHOD(
    cache_mailbox_test,
    "test mark to delete system folder with type in initial_folders with another fid")
{
    char delim = '|';
    mb::fid_t fid = "1";
    auto folder = mb::folder(mb::path_t("inbox", delim), fid);
    folder.type = mb::folder::type_t::inbox;
    mb::folder_vector folders{ folder };
    cache_mb->update_folders_from_local(std::make_shared<mb::folder_vector>(folders));

    mb::fid_t system_folder_fid = "2";
    auto system_folder = mb::folder(mb::path_t("", delim), system_folder_fid);
    system_folder.type = mb::folder::type_t::inbox;
    mb::folder_vector initial_folders{ system_folder };
    cache_mb->set_initial_folders(std::make_shared<mb::folder_vector>(initial_folders));

    try
    {
        folder.type = mb::folder::type_t::drafts;
        folders = { folder };
        cache_mb->update_folders_from_external(std::make_shared<mb::folder_vector>(folders));
    }
    catch (const std::exception& e)
    {
        REQUIRE(std::string(e.what()) == "2 different folders with type: inbox");
    }
}

TEST_CASE_METHOD(cache_mailbox_test, "test delete system folder with type in initial_folders")
{
    char delim = '|';
    mb::fid_t fid = "1";

    auto folder = mb::folder(mb::path_t("inbox", delim), fid);
    folder.type = mb::folder::type_t::inbox;
    mb::folder_vector folders{ folder };
    cache_mb->update_folders_from_local(std::make_shared<mb::folder_vector>(folders));

    auto system_folder = mb::folder(mb::path_t("", delim), fid);
    system_folder.type = mb::folder::type_t::inbox;
    mb::folder_vector initial_folders{ system_folder };
    cache_mb->set_initial_folders(std::make_shared<mb::folder_vector>(initial_folders));

    cache_mb->delete_folder_by_path(folder.path);
    try
    {
        auto folder_in_cache = cache_mb->get_folder(folder.path);
    }
    catch (const std::exception& e)
    {
        REQUIRE(std::string(e.what()) == "folder not found inbox");
    }
}

TEST_CASE_METHOD(
    cache_mailbox_test,
    "test delete system folder with type in initial_folders with another fid")
{
    char delim = '|';
    mb::fid_t fid = "1";
    auto folder = mb::folder(mb::path_t("inbox", delim), fid);
    folder.type = mb::folder::type_t::inbox;
    mb::folder_vector folders{ folder };
    cache_mb->update_folders_from_local(std::make_shared<mb::folder_vector>(folders));

    mb::fid_t system_folder_fid = "2";
    auto system_folder = mb::folder(mb::path_t("", delim), system_folder_fid);
    system_folder.type = mb::folder::type_t::inbox;
    mb::folder_vector initial_folders{ system_folder };
    cache_mb->set_initial_folders(std::make_shared<mb::folder_vector>(initial_folders));

    try
    {
        cache_mb->delete_folder_by_path(folder.path);
    }
    catch (const std::exception& e)
    {
        REQUIRE(std::string(e.what()) == "2 different folders with type: inbox");
    }
}

TEST_CASE_METHOD(cache_mailbox_test, "test delete folder top when delete folder")
{
    char delim = '|';
    mb::fid_t fid = "1";
    auto folder = mb::folder(mb::path_t("inbox", delim), fid);
    folder.type = mb::folder::type_t::inbox;
    mb::folder_vector folders{ folder };
    cache_mb->update_folders_from_local(std::make_shared<mb::folder_vector>(folders));

    auto state = cache_mb->sync_newest_state();
    state->folders.insert({ folder.path, {} });

    cache_mb->delete_folder_by_path(folder.path);
    REQUIRE(state->folders.find(folder.path) == state->folders.end());
}
