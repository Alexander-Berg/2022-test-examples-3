#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/barbet/service/include/handlers/backup.h>


using namespace ::testing;

namespace barbet::tests {

const std::time_t now = 1000;
const std::time_t seconds = 1;
const std::time_t nowInTheFuture = 1002;
const macs::Backup strangePrimaryBackupWithCreatedInFuture {
    .created=now*2
};

TEST(BackupFrequency, shouldReturnFalseInCaseOfEmptyBackups) {
    EXPECT_FALSE(backupIsTooFrequent(macs::BackupStatus(), now, seconds));
}

TEST(BackupFrequency, shouldReturnFalseInCaseOfOldBackup) {
    EXPECT_FALSE(backupIsTooFrequent(macs::BackupStatus {
        .primary=macs::Backup {
            .created=now/2
        },
        .additional=macs::Backup {
            .created=now/2,
            .state=macs::BackupState::error
        }
    }, now, seconds));
}

TEST(BackupFrequency, shouldChosePrimaryIfAdditionalIsEmpty) {
    const macs::Backup backup {
        .created=now
    };

    EXPECT_TRUE(backupIsTooFrequent(macs::BackupStatus {
        .primary=backup
    }, now, seconds));

    EXPECT_FALSE(backupIsTooFrequent(macs::BackupStatus {
        .primary=backup
    }, nowInTheFuture, seconds));
}

TEST(BackupFrequency, shouldChoseAdditionalIfStateIsNotError) {
    const macs::Backup backup {
        .created=now,
        .state=macs::BackupState::in_progress
    };

    EXPECT_TRUE(backupIsTooFrequent(macs::BackupStatus {
        .primary=strangePrimaryBackupWithCreatedInFuture,
        .additional=backup
    }, now, seconds));

    EXPECT_FALSE(backupIsTooFrequent(macs::BackupStatus {
        .primary=strangePrimaryBackupWithCreatedInFuture,
        .additional=backup
    }, nowInTheFuture, seconds));
}

TEST(BackupFrequency, shouldChosePrimaryIfAdditionalStateIsError) {
    const macs::Backup backup {
        .created=now,
        .state=macs::BackupState::error
    };

    EXPECT_TRUE(backupIsTooFrequent(macs::BackupStatus {
        .primary=strangePrimaryBackupWithCreatedInFuture,
        .additional=backup
    }, now, seconds));

    EXPECT_FALSE(backupIsTooFrequent(macs::BackupStatus {
        .primary=strangePrimaryBackupWithCreatedInFuture,
        .additional=backup
    }, nowInTheFuture*2, seconds));
}

}
