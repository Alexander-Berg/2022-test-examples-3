#include "lang_config.h"
#include <common/folder_list.h>
#include <common/helpers/utf7imap.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace yimap;
using namespace testing;

struct FolderListTest : Test
{
    LanguageConfigPtr languageConfig = std::make_shared<LanguageConfig>(
        makeLanguageConfig(DEFAULT_LANGUAGE_CONFIG, true, true, "ru"));

    FolderList makeFolderList(std::map<std::string, std::string> folders)
    {
        RawFolderListPtr rawList = std::make_shared<RawFolderList>();
        for (auto& [name, symbol] : folders)
        {
            auto folderInfo =
                std::make_shared<FullFolderInfo>(FullFolderInfo{ .name = name, .symbol = symbol });
            rawList->push_back(folderInfo);
        }

        return FolderList(rawList, languageConfig);
    }
};

TEST_F(FolderListTest, foldersWithSameNameShouldntMerge)
{
    auto folderList = makeFolderList({ { "Sent", "sent" }, { "Отправленные", "" } });
    auto sentDisplayName = folderNameToUtf7Imap("Отправленные", '|');
    auto userDisplayName = folderNameToUtf7Imap("Отправленные_0", '|');

    ASSERT_THAT(folderList, SizeIs(2));
    ASSERT_THAT(folderList, Contains(Key(sentDisplayName)));
    ASSERT_EQ(folderList.at(sentDisplayName)->symbol, "sent");
    ASSERT_EQ(folderList.at(sentDisplayName)->name, "Sent");

    ASSERT_THAT(folderList, Contains(Key(userDisplayName)));
    ASSERT_EQ(folderList.at(userDisplayName)->symbol, "");
    ASSERT_EQ(folderList.at(userDisplayName)->name, "Отправленные");
}
