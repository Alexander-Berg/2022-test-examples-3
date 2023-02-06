#pragma once

#include <macs_pg/service/service.h>
#include <gmock/gmock.h>

namespace macs::tests {

class MetadataMock: public macs::Service {
public:
    MOCK_METHOD(macs::LabelsRepository&, labels, (), (override));
    MOCK_METHOD(const macs::LabelsRepository&, labels, (), (const, override));

    MOCK_METHOD(macs::FoldersRepository&, folders, (), (override));
    MOCK_METHOD(const macs::FoldersRepository&, folders, (), (const, override));

    MOCK_METHOD(macs::EnvelopesRepository&, envelopes, (), (override));
    MOCK_METHOD(const macs::EnvelopesRepository&, envelopes, (), (const, override));

    MOCK_METHOD(macs::ThreadsRepository&, threads, (), (override));
    MOCK_METHOD(const macs::ThreadsRepository&, threads, (), (const, override));

    MOCK_METHOD(macs::ImapRepository&, imapRepo, (), (override));
    MOCK_METHOD(const macs::ImapRepository&, imapRepo, (), (const, override));

    MOCK_METHOD(macs::DatabaseInfo&, databaseInfo, (), (override));
    MOCK_METHOD(const macs::DatabaseInfo&, databaseInfo, (), (const, override));

    MOCK_METHOD(const macs::SharedFoldersRepository&, sharedFolders, (), (const, override));
    MOCK_METHOD(const macs::SubscribedFoldersRepository&, subscribedFolders, (), (const, override));
    MOCK_METHOD(const macs::SubscriptionRepository&, subscriptions, (), (const, override));

    MOCK_METHOD(const macs::MailishRepository&, mailish, (), (const, override));

    MOCK_METHOD(const macs::UserChangeLogRepository&, changelog, (), (const, override));

    MOCK_METHOD(const macs::TabsRepository&, tabs, (), (const, override));

    MOCK_METHOD(const macs::CollectorsRepository&, collectors, (), (const, override));

    MOCK_METHOD(const macs::SettingsRepository&, settings, (), (const, override));
    MOCK_METHOD(macs::SettingsRepository&, settings, (), (override));

    MOCK_METHOD(const macs::UsersRepository&, users, (), (const, override));
    MOCK_METHOD(macs::UsersRepository&, users, (), ());

    MOCK_METHOD(const macs::BackupsRepository&, backups, (), (const, override));

    MOCK_METHOD(const macs::StickersRepository&, stickers, (), (const, override));

    MOCK_METHOD(void, runTransactionalInternal, (TransactionBodyFunc, macs::OnExecute), (const, override));
};

using Metadata = testing::StrictMock<MetadataMock>;
using MetadataPtr = boost::shared_ptr<Metadata>;

}

