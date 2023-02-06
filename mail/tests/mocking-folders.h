#ifndef MACS_MOCKING_FOLDERS_H
#define MACS_MOCKING_FOLDERS_H
#include <gmock/gmock.h>
#include <macs/folders_repository.h>
#include <boost/shared_ptr.hpp>

#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Winconsistent-missing-override"
#endif

struct MockFoldersRepository: public macs::FoldersRepository {
    MOCK_METHOD(macs::MailboxSpaceInfo, syncGetMailboxSpaceInfo, (), (const, override));
    MOCK_METHOD(void, syncGetMailboxRevision, (macs::OnRevisionReceive), (const, override));
    MOCK_METHOD(void, syncGetFolders, (macs::OnFolders), (const, override));
    MOCK_METHOD(void, syncGetPop3Folders, (macs::OnPop3Folders), (const, override));
    MOCK_METHOD(void, syncResetUnvisited, (const std::string&, macs::OnUpdate), (const, override));
    MOCK_METHOD(void, syncSubscribeToSharedFolders, (const std::string&, macs::OnFoldersMap), (const, override));
    MOCK_METHOD(void, syncUnsubscribeFromSharedFolders, (const std::string&, macs::OnFoldersMap), (const, override));
    MOCK_METHOD(void, syncCreateFolder, (const std::string&, const std::string&, const macs::Folder::Symbol&,
                      macs::OnUpdateFolder), (const, override));
    MOCK_METHOD(void, syncGetOrCreateFolder, (const std::string&, const std::string&, const macs::Folder::Symbol&,
                      macs::OnUpdateFolder), (const, override));
    MOCK_METHOD(void, syncCreateFolderByPath, (macs::Folder::Path, macs::OnUpdateFolder), (const, override));
    MOCK_METHOD(void, syncModifyFolder, (const macs::Folder&,
                      macs::OnUpdateFolder), (const, override));
    MOCK_METHOD(void, syncModifyFolderToPath, (const macs::Folder&,
                      const macs::Folder::Path& path,
                      macs::OnUpdateFolder), (const, override));
    MOCK_METHOD(void, syncSetPosition, (const std::string&, size_t, macs::OnUpdate), (const, override));
    MOCK_METHOD(void, syncEraseFolder, (const std::string&, macs::OnUpdate), (const, override));
    MOCK_METHOD(void, syncClearFolderCascade, (const std::string&,
                                                    const macs::FolderSet &,
                                                    macs::OnUpdateMessages), (const, override));
    MOCK_METHOD(void, syncMoveAll, (const macs::Folder&,
                                         const macs::Folder&,
                                         macs::OnUpdateMessages), (const, override));
    MOCK_METHOD(void, syncSetFolderSymbol, (const std::string&, const macs::Folder::Symbol&,
            macs::OnUpdate), (const, override));
    MOCK_METHOD(void, syncSetPop3, (const std::vector<std::string>&,
                                         const macs::FolderSet & fs,
                                         macs::OnUpdate), (const, override));
    MOCK_METHOD(void, asyncSetArchivationRule, (const macs::Fid&,
                                                     const macs::Folder::ArchivationType&,
                                                     uint32_t, uint32_t,
                                                     macs::OnUpdate), (const, override));
    MOCK_METHOD(void, asyncRemoveArchivationRule, (const macs::Fid&,
                                                        macs::OnUpdate), (const, override));

    macs::FolderFactory factory(void) {
        return getFolderFactory();
    }

    macs::FolderFactory folder(const std::string& id,
                              const std::string& name = "",
                              const std::string& parent = macs::Folder::noParent,
                              const size_t messagesCount = 0) {
        return factory().fid(id).name(name).parentId(parent).messages(messagesCount);
    }

    macs::FolderFactory system(macs::FolderFactory factory) {
        return factory.type(macs::Folder::Type::system);
    }

    const macs::Folder::SymbolSet& defaultFoldersSymbols() const override {
        static const macs::Folder::SymbolSet result;
        return result;
    }
};

struct FoldersRepositoryTest: public testing::Test {
    typedef testing::StrictMock<MockFoldersRepository> Repository;
    std::shared_ptr<Repository> foldersPtr;
    Repository &folders;

    typedef decltype(testing::InvokeArgument<0>(macs::error_code(), macs::FolderSet())) FoldersInvoker;
    typedef std::vector<macs::Folder> TestData;

    FoldersRepositoryTest() : foldersPtr(new Repository), folders(*foldersPtr) {}

    static FoldersInvoker GiveFolders(const TestData& args) {
        macs::FoldersMap ret;
        for( const auto & i : args ) {
            ret.insert({i.fid(), i});
        }
        return testing::InvokeArgument<0>(macs::error_code(),
                macs::FolderSet(ret));
    }

    static FoldersInvoker GiveFolders(const macs::Folder& f1) {
        return GiveFolders(TestData{f1});
    }

    static FoldersInvoker GiveFolders(const macs::Folder& f1,
                                      const macs::Folder& f2) {
        return GiveFolders(TestData{f1, f2});
    }

    static FoldersInvoker GiveFolders(const macs::Folder& f1,
                                      const macs::Folder& f2,
                                      const macs::Folder& f3) {
        return GiveFolders(TestData{f1, f2, f3});
    }

    static FoldersInvoker GiveFolders(std::initializer_list<macs::Folder> folders) {
        return GiveFolders(TestData{ folders });
    }
};

struct FolderMatcher {
    FolderMatcher(const std::string& name)
        : name(name), checkParentId(false), checkFid(false)
    {}

    bool operator()(const macs::Folder& folder) const {
        return folder.name() == name
            && (!checkParentId || folder.parentId() == parentId)
            && (!checkFid || folder.fid() == fid);
    }

    FolderMatcher& withParentId(const std::string& parentId) {
        checkParentId = true;
        this->parentId = parentId;
        return *this;
    }

    FolderMatcher& withFid(const std::string& fid) {
        checkFid = true;
        this->fid = fid;
        return *this;
    }

    std::string name, parentId, fid;
    bool checkParentId, checkFid;
};

typedef testing::internal::TrulyMatcher<FolderMatcher> TrulyFolderMatcher;
typedef testing::PolymorphicMatcher<TrulyFolderMatcher> PolyFolderMatcher;

inline PolyFolderMatcher matchFolder(const macs::Folder& folder) {
    return testing::Truly(FolderMatcher(folder.name())
                          .withParentId(folder.parentId())
                          .withFid(folder.fid()));
}

#ifdef __clang__
#pragma clang diagnostic pop
#endif

#endif // MACS_MOCKING_FOLDERS_H
