#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-folders.h>
#include "throw-wmi-helper.h"
#include <macs/data/folder_sort_options.h>

namespace {
    using namespace ::testing;
    using namespace ::macs;
    using namespace ::std;

    struct SortOptionsTest: public FoldersRepositoryTest {
        SortOptionsTest(void)
            : folder1(folders.folder("1", "fname1", Folder::noParent)),
              folder2(folders.folder("2", "fname2", Folder::noParent)),
              folder3(folders.folder("3", "fname3", Folder::noParent)),
              folderFromAnotherLevel(folders.folder("4", "fname4", "123")),
              sortOptions(folders)
        {}
        Folder folder1, folder2, folder3, folderFromAnotherLevel;
        macs::SortOptions sortOptions;
    };

    void setPosition(Folder& folder, size_t position) {
        FolderFactory factory(std::move(folder));
        factory.position(position);
        folder = factory.product();
    }

    TEST_F(SortOptionsTest, move_withUnorderedLabels_orderPositionsAlphabeticallyCaseInsensitive) {
        TestData foldersData;
        foldersData.push_back(folders.folder("1", "1", Folder::noParent));
        foldersData.push_back(folders.folder("2", "A", Folder::noParent));
        foldersData.push_back(folders.folder("3", "b", Folder::noParent));
        foldersData.push_back(folders.folder("4", "C", Folder::noParent));
        foldersData.push_back(folders.folder("5", "d", Folder::noParent));
        foldersData.push_back(folders.folder("6", "e", Folder::noParent));
        foldersData.push_back(folders.folder("7", "~", Folder::noParent));
        EXPECT_CALL(folders, syncGetFolders(_)).WillRepeatedly(GiveFolders(foldersData));
        EXPECT_CALL(folders, syncSetPosition("1", 100, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("2", 200, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("3", 300, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("4", 400, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("5", 500, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("6", 600, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("7", 700, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        sortOptions.move("1", "1");
    }

    TEST_F(SortOptionsTest, move_withUnorderedLabelsAndOrderedParent_orderPositionsAlphabetically) {
        TestData foldersData;
        macs::Folder parent(folders.folder("2160000150000537549", "kdsgfhdgKKGs", Folder::noParent));
        setPosition(parent, 123);
        foldersData.push_back(parent);
        foldersData.push_back(folders.folder("2160000150000537550", "AVLahdlgjdh", "2160000150000537549"));
        foldersData.push_back(folders.folder("2160000150000537551", "NB,MCNBDSgg", "2160000150000537549"));
        foldersData.push_back(folders.folder("2160000150000537552", "dhGLJdshglj", "2160000150000537549"));
        foldersData.push_back(folders.folder("2160000150000537553", "dhbsvhdkjVG", "2160000150000537549"));
        foldersData.push_back(folders.folder("2160000150000537554", "dsHGJHGDKJH", "2160000150000537549"));
        EXPECT_CALL(folders, syncGetFolders(_)).WillRepeatedly(GiveFolders(foldersData));
        EXPECT_CALL(folders, syncSetPosition("2160000150000537550", 100, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("2160000150000537553", 200, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("2160000150000537552", 300, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("2160000150000537551", 500, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("2160000150000537554", 150, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        sortOptions.move("2160000150000537554", "2160000150000537550");
    }

    TEST_F(SortOptionsTest, move_normalPositions_setsMedianPosition) {
        setPosition(folder1, 100);
        setPosition(folder2, 200);
        setPosition(folder3, 300);
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder1, folder2, folder3));
        EXPECT_CALL(folders, syncSetPosition("3", 150, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        sortOptions.move("3", "1");
    }

    TEST_F(SortOptionsTest, move_firstFolderWithNormalPositions_setsMedianPosition) {
        setPosition(folder1, 100);
        setPosition(folder2, 200);
        setPosition(folder3, 300);
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder1, folder2, folder3));
        EXPECT_CALL(folders, syncSetPosition("1", 250, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        sortOptions.move("1", "2");
    }

    TEST_F(SortOptionsTest, move_zeroPrevFid_setsBeforeFirst) {
        setPosition(folder1, 150);
        setPosition(folder2, 200);
        setPosition(folder3, 300);
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(folder1, folder2, folder3));
        EXPECT_CALL(folders, syncSetPosition("3", 75, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        sortOptions.move("3", Folder::noParent);
    }

    TEST_F(SortOptionsTest, move_insertBetweenSerialPositions_rearrangesPositionsAndSetsPosition) {
        setPosition(folder1, 123);
        setPosition(folder2, 124);
        setPosition(folder3, 200);
        EXPECT_CALL(folders, syncGetFolders(_)).WillRepeatedly(GiveFolders(folder1, folder2, folder3));
        EXPECT_CALL(folders, syncSetPosition("1", 100, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("2", 200, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        EXPECT_CALL(folders, syncSetPosition("3", 150, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        sortOptions.move("3", "1");
    }

    TEST_F(SortOptionsTest, move_insertBefore1WithPrevFromAnotherLevel_rearrangesPositionsAndSetsPosition) {
        setPosition(folder1, 1);
        setPosition(folder2, 200);
        EXPECT_CALL(folders, syncGetFolders(_)).WillRepeatedly(GiveFolders(folder1, folder2, folderFromAnotherLevel));
        EXPECT_CALL(folders, syncSetPosition("1", 50, _)).WillOnce(InvokeArgument<2>(macs::error_code(), 0));
        sortOptions.move("1", "4");
    }

}
