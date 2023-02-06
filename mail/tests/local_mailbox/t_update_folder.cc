#include <mailbox/local/update_folder_op.h>
#include <catch.hpp>

using namespace xeno::mailbox;
using local::update_folder_op;

std::string TEST_FOLDER_NAME = "TestFolder";
std::string TEST_FOLDER_FID = "100";
size_t TEST_FOLDER_UIDVALIDITY = 123;

std::string INBOX_NAME = "INBOX";
std::string INBOX_FID = "1";
size_t INBOX_UIDVALIDITY = 456;

struct service_fake
{
    using update_folder_cb = std::function<void(mail_errors::error_code)>;
    using get_folders_cb = std::function<void(mail_errors::error_code, macs::FolderSet)>;

    std::map<std::string, const macs::Folder> folders_map;
    std::map<std::string, macs::MailishFolderInfo> mailish_info_map;

    void updateFolder(macs::Folder folder, macs::MailishFolderInfo info, const update_folder_cb& cb)
    {
        folders_map.erase(folder.fid());
        folders_map.emplace(folder.fid(), folder);
        mailish_info_map[folder.fid()] = info;
        cb({});
    }

    void getAllFolders(const get_folders_cb& cb)
    {
        cb({}, macs::FolderSet(folders_map));
    }

    service_fake& mailish()
    {
        return *this;
    }

    service_fake& folders()
    {
        return *this;
    }
};

void add_folder(
    const std::string& fid,
    const std::string& name,
    size_t uidvalidity,
    service_fake& service)
{
    macs::FolderFactory folder_factory;
    folder_factory.name(name);
    folder_factory.fid(fid);
    service.folders_map.emplace(fid, folder_factory.product());

    macs::MailishFolderInfoFactory mailish_info_factory;
    mailish_info_factory.externalPath(name);
    mailish_info_factory.uidValidity(uidvalidity);
    service.mailish_info_map.emplace(fid, mailish_info_factory.release());
}

void add_system_folder(
    const std::string& fid,
    const std::string& name,
    size_t uidvalidity,
    macs::Folder::Symbol symbol,
    service_fake& service)
{
    macs::FolderFactory folder_factory;
    folder_factory.name(name);
    folder_factory.fid(fid);
    folder_factory.symbol(symbol);
    folder_factory.type(macs::Folder::Type::system);
    service.folders_map.emplace(fid, folder_factory.product());

    macs::MailishFolderInfoFactory mailish_info_factory;
    mailish_info_factory.externalPath(name);
    mailish_info_factory.uidValidity(uidvalidity);
    service.mailish_info_map.emplace(fid, mailish_info_factory.release());
}

service_fake make_service_with_folders()
{
    service_fake service;
    add_system_folder(
        INBOX_FID, INBOX_NAME, INBOX_UIDVALIDITY, macs::Folder::Symbol::inbox, service);
    add_folder(TEST_FOLDER_FID, TEST_FOLDER_NAME, TEST_FOLDER_UIDVALIDITY, service);
    return service;
}

void update_folder(const folder& folder, service_fake& service)
{
    auto cb = [](auto err) {
        if (err)
        {
            throw std::runtime_error("update folder error: " + err.message());
        }
    };
    auto op = std::make_shared<update_folder_op<service_fake*>>(&service, folder, fid_t_opt{}, cb);
    yplatform::spawn(op);
}

TEST_CASE("local_mb::update_folder: rename user folder")
{
    auto service = make_service_with_folders();
    folder folder(path_t("TestFolder2", '|'), TEST_FOLDER_FID, TEST_FOLDER_UIDVALIDITY);
    update_folder(folder, service);
    REQUIRE(service.folders_map[TEST_FOLDER_FID].name() == folder.path.get_name());
    REQUIRE(service.mailish_info_map[TEST_FOLDER_FID].externalPath() == folder.path.get_name());
}

TEST_CASE("local_mb::update_folder: rename system folder")
{
    auto service = make_service_with_folders();
    folder folder(path_t("Входящие", '|'), INBOX_FID, INBOX_UIDVALIDITY);
    update_folder(folder, service);
    REQUIRE(service.folders_map[INBOX_FID].name() == INBOX_NAME);
    REQUIRE(service.mailish_info_map[INBOX_FID].externalPath() == folder.path.get_name());
}
