#include <internal/folder/create_folder_by_path.h>
#include "mocks/transactional_mock.h"
#include "path_to_query_conf.h"

namespace macs {

bool operator ==(const Folder& lhs, const Folder& rhs) {
    return lhs.fid() == rhs.fid() && lhs.parentId() == rhs.parentId() && lhs.name() == rhs.name();
}

static std::ostream& operator <<(std::ostream& stream, const macs::Folder& value) {
    return stream << "Folder {fid=\"" << value.fid()
        << "\", parentId=\"" << value.parentId()
        << "\", name=\"" << value.name() << "\"}";
}

} // namespace macs

namespace {

using namespace testing;
using namespace macs;
using namespace macs::pg;
using namespace macs::pg::query;
using namespace pgg::query;
using namespace tests;

struct OnUpdateFolderMock {
    struct Impl {
        MOCK_METHOD(void, call, (error_code, Folder), ());
    };

    std::shared_ptr<Impl> impl = std::make_shared<Impl>();

    void operator ()(error_code ec, Folder folder) {
        impl->call(ec, folder);
    }
};

struct CreateFolderByPathTest : public Test {
    const RepositoryPtr queryRepository = readQueryConfFile(pathToQueryConf());
    OnUpdateFolderMock hook;
    const Folder folder1 = macs::FolderFactory().fid("1").parentId(Folder::noParent).name("folder1");
    const Folder folder2 = macs::FolderFactory().fid("2").parentId(folder1.fid()).name("folder2");
    const std::unordered_map<std::string, Folder> folders {{
        {folder1.name(), folder1},
        {folder2.name(), folder2},
    }};
    const Folder::Path path {std::vector<macs::Folder::Name>{folder1.name(), folder2.name()}};
    const std::string uid {"42"};
    const RequestInfo requestInfo;
    const Milliseconds timeout {13};
    Coroutine coroutine;
    TransactionalMock transactional;
    const AllFoldersList allFolderList = queryRepository->query<AllFoldersList>();
    const std::vector<Row> existingFolders {{
        Row {{
            {"fid", std::int32_t(1)},
            {"parent_fid", boost::optional<std::int32_t>()},
            {"type", std::string("user")},
            {"folder_path", std::vector<std::string>({"folder1"})},
        }},
        Row {{
            {"fid", std::int32_t(2)},
            {"parent_fid", boost::optional<std::int32_t>(1)},
            {"type", std::string("user")},
            {"folder_path", std::vector<std::string>({"folder2"})},
        }},
    }};
    std::vector<Row> maxExistingFolders;
    CreateFolderByPath operation {queryRepository, path, uid, requestInfo, timeout, hook};

    CreateFolderByPathTest() {
        for (std::int32_t fid = 1; fid <= std::int32_t(FolderSet::MAX_FOLDERS_NUMBER); ++fid) {
            maxExistingFolders.push_back(Row {{
                {"fid", fid},
                {"parent_fid", boost::optional<std::int32_t>()},
                {"type", std::string("user")},
                {"folder_path", std::vector<std::string>({"max_folder_" + std::to_string(fid)})},
            }});
        }
    }

    CreateFolder createFolderQuery(const FolderName& folderName, const ParentFolderId& parentFolderId) const {
        return queryRepository->query<CreateFolder>(folderName, parentFolderId);
    }
};

TEST_F(CreateFolderByPathTest, when_error_on_begin_should_call_hook_with_same_error) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(operationAborted));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), Folder()));

    operation(coroutine, transactional);
}

TEST_F(CreateFolderByPathTest, when_error_on_fetch_all_folder_list_should_call_hook_with_same_error) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(allFolderList)), _)).WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), Folder()));
    operation(coroutine, transactional);
}

TEST_F(CreateFolderByPathTest, when_all_folders_are_created_should_call_hook_with_last_folder) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    auto rows = existingFolders;
    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(allFolderList)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, rows));
    operation(coroutine, transactional);

    EXPECT_CALL(transactional, commitImpl(_)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    EXPECT_CALL(*hook.impl, call(error_code(), folder2));
    operation(coroutine, transactional);
}

TEST_F(CreateFolderByPathTest, when_folders_are_not_created_and_there_is_error_on_create_folder_should_call_hook_with_same_error) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(allFolderList)), _)).WillOnce(InvokeArgument<1>(pgg_error_code{}, FakeDataRange()));
    operation(coroutine, transactional);

    const auto createFolder = createFolderQuery(FolderName(folder1.name()), ParentFolderId(folder1.parentId()));
    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(createFolder)), _)).WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), Folder()));
    operation(coroutine, transactional);
}

TEST_F(CreateFolderByPathTest, when_folders_are_not_created_should_create_folders_and_call_hook_with_last_created_folder) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(allFolderList)), _)).WillOnce(InvokeArgument<1>(pgg_error_code{}, FakeDataRange()));
    operation(coroutine, transactional);

    for (std::size_t i = 0; i < path.size(); ++i) {
        auto pathIt = path.begin();
        std::advance(pathIt, i);
        const auto& folder = folders.at(*pathIt);
        const auto createFolder = createFolderQuery(FolderName(folder.name()), ParentFolderId(folder.parentId()));
        std::vector<Row> rows({existingFolders[i]});
        EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(createFolder)), _))
                .WillOnce(InvokeArgument<1>(pgg_error_code{}, rows));
        operation(coroutine, transactional);
    }

    EXPECT_CALL(transactional, commitImpl(_)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    EXPECT_CALL(*hook.impl, call(error_code(), folder2));
    operation(coroutine, transactional);
}

TEST_F(CreateFolderByPathTest, when_error_on_commit_should_call_hook_with_same_error) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(allFolderList)), _)).WillOnce(InvokeArgument<1>(pgg_error_code{}, FakeDataRange()));
    operation(coroutine, transactional);

    for (std::size_t i = 0; i < path.size(); ++i) {
        auto pathIt = path.begin();
        std::advance(pathIt, i);
        const auto& folder = folders.at(*pathIt);
        const auto createFolder = createFolderQuery(FolderName(folder.name()), ParentFolderId(folder.parentId()));
        std::vector<Row> rows({existingFolders[i]});
        EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(createFolder)), _))
                .WillOnce(InvokeArgument<1>(pgg_error_code{}, rows));
        operation(coroutine, transactional);
    }

    EXPECT_CALL(transactional, commitImpl(_)).WillOnce(InvokeArgument<0>(operationAborted));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), Folder()));
    operation(coroutine, transactional);
}

TEST_F(CreateFolderByPathTest, when_number_of_existing_folders_is_max_should_call_hook_with_error) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    auto rows = maxExistingFolders;
    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(allFolderList)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, rows));
    operation(coroutine, transactional);

    EXPECT_CALL(*hook.impl, call(error_code(macs::error::foldersLimitExceeded), Folder()));
    operation(coroutine, transactional);
}

TEST_F(CreateFolderByPathTest, when_number_of_folders_would_be_greater_than_max_should_call_hook_with_error) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    auto rows = maxExistingFolders;
    rows.pop_back();
    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(allFolderList)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, rows));
    operation(coroutine, transactional);

    EXPECT_CALL(*hook.impl, call(error_code(macs::error::foldersLimitExceeded), Folder()));
    operation(coroutine, transactional);
}

} // namespace
