#include <internal/shared_folders/create_shared_folder_with_archivation.h>

#include "mocks/transactional_mock.h"
#include "path_to_query_conf.h"

namespace macs {

bool operator ==(const Revision& lhs, const Revision& rhs) {
    return lhs.value() == rhs.value();
}

static std::ostream& operator <<(std::ostream& stream, const macs::Revision& rev) {
    return stream << "Revision=" << rev.value() << "}";
}

} // namespace macs

namespace {

using namespace testing;
using namespace macs;
using namespace macs::pg;
using namespace macs::pg::query;
using namespace pgg::query;
using namespace tests;

struct OnUpdateMock {
    struct Impl {
        MOCK_METHOD(void, call, (error_code, macs::Revision), ());
    };

    std::shared_ptr<Impl> impl = std::make_shared<Impl>();

    void operator ()(error_code ec, macs::Revision revision) {
        impl->call(ec, revision);
    }
};

struct CreateSharedFolderWithArchivationTest : public Test {
    using Type = ::macs::Folder::ArchivationType;

    const RepositoryPtr queryRepository = readQueryConfFile(pathToQueryConf());
    OnUpdateMock hook;

    const std::string uid {"42"};
    const macs::Fid fid {"fid"};
    const Type type {Type::archive};
    const uint32_t keep_days = 30;
    const uint32_t max_size = 1000;

    const RequestInfo requestInfo{{}, {}, {}, {}};
    const Milliseconds timeout {13};
    Coroutine coroutine;
    TransactionalMock transactional;

    CreateSharedFolderWithArchivation operation {
        queryRepository, uid, fid, type, keep_days, max_size,
        requestInfo, timeout, hook};

    const CreateSharedFolder createSharedFolderQuery =
            queryRepository->query<CreateSharedFolder>(query::FolderId(fid));

    const SetFolderArchivationRules setArchivationRulesQuery =
            queryRepository->query<SetFolderArchivationRules>(
                query::FolderId(fid), ArchivationType(type),
                ArchivationTtl(keep_days), MaxFolderSize(max_size));

    Row revisionRow(std::uint64_t value) {
        return {{{"revision", value}}};
    }
};

TEST_F(CreateSharedFolderWithArchivationTest, whenError_onBegin_shouldCallHookWithSameError) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(operationAborted));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), macs::Revision()));

    operation(coroutine, transactional);
}

TEST_F(CreateSharedFolderWithArchivationTest, whenError_onCreateSharedFolder_shouldCallHookWithSameError) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(createSharedFolderQuery)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), macs::Revision()));
    operation(coroutine, transactional);
}

TEST_F(CreateSharedFolderWithArchivationTest, whenError_onSetArchivationRule_shouldCallHookWithSameError) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    std::vector<Row> rows({revisionRow(42)});
    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(createSharedFolderQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, rows));
    operation(coroutine, transactional);

    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(setArchivationRulesQuery)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), macs::Revision()));
    operation(coroutine, transactional);
}

TEST_F(CreateSharedFolderWithArchivationTest, whenError_onCommit_shouldCallHookWithSameError) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    std::vector<Row> rows1({revisionRow(42)});
    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(createSharedFolderQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, rows1));
    operation(coroutine, transactional);

    std::vector<Row> rows2({revisionRow(43)});
    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(setArchivationRulesQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, rows2));
    operation(coroutine, transactional);

    EXPECT_CALL(transactional, commitImpl(_)).WillOnce(InvokeArgument<0>(operationAborted));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), macs::Revision()));
    operation(coroutine, transactional);
}

TEST_F(CreateSharedFolderWithArchivationTest, whenSuccess_shouldCallHookWithLastRevision) {
    const InSequence s;

    EXPECT_CALL(transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    std::vector<Row> rows1({revisionRow(42)});
    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(createSharedFolderQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, rows1));
    operation(coroutine, transactional);

    std::vector<Row> rows2({revisionRow(43)});
    EXPECT_CALL(transactional, fetchImpl(Eq(ByRef(setArchivationRulesQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, rows2));
    operation(coroutine, transactional);

    EXPECT_CALL(transactional, commitImpl(_)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    operation(coroutine, transactional);

    EXPECT_CALL(*hook.impl, call(error_code(), macs::Revision(43)));
    operation(coroutine, transactional);
}

} // namespace
