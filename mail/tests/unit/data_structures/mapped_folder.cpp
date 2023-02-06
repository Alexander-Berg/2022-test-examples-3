#include <backend/backend.h>
#include <common/folder.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

TEST(DataStructures, TEST_MAPPED_FOLDER_EXPUNGE)
{
    yimap::FolderInfo folderInfo;
    yimap::UidMapData uidMapData;

    auto folder = yimap::backend::CreatePgMappedFolder(folderInfo, uidMapData, false);

    yimap::MessageData first;
    first.uid = 1;
    first.baseUid = 1;
    first.offset = 0;

    yimap::MessageData second;
    second.uid = 2;
    second.baseUid = 1;
    second.offset = 1;

    // Emulate append two messages
    yimap::MessagesVector beforeExpunge;
    beforeExpunge.push_back(first);
    beforeExpunge.push_back(second);
    folder->insertMessages(beforeExpunge);

    // Emulate expunge
    first.deleted = true;
    yimap::MessagesVector chunk;
    chunk.push_back(first);
    folder->update(chunk);
    folder->updateToRevision(false);

    yimap::MessageData third;
    third.uid = 3;
    third.baseUid = 2;
    third.offset = 1;

    // Emulate append message after expunge
    yimap::MessagesVector afterExpunge;
    afterExpunge.push_back(third);
    folder->insertMessages(afterExpunge);

    yimap::seq_range range(0, 3, true);
    range += yimap::range_t(2, 2);
    ;
    auto resultFetch = folder->filterByRanges(range, yimap::MessageData::id);
    EXPECT_EQ(resultFetch->size(), 1);

    yimap::MessageData resultMess = resultFetch->pop();
    EXPECT_EQ(resultMess.uid, 2);
    EXPECT_EQ(resultMess.baseUid, 2);
    EXPECT_EQ(resultMess.offset, 0);
}