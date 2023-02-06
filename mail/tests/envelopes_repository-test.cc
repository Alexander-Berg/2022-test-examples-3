#include <macs/connection_info.h>
#include <macs/tests/mocking-macs.h>

namespace {

using namespace macs;
using namespace macs::io;
using namespace macs::io::detail;
using namespace testing;

class EnvelopesRepositoryTest : public Test {
protected:

    void SetUp() override {
        env = std::make_shared<MockEnvelopesRepository>();
    }

    static auto GetMidsHandler() {
        sync_context context;
        return OnMidsReceive{init_async_result<sync_context, OnMidsReceive>{context}.handler};
    }

    static auto GetMimesHandler() {
        sync_context context;
        return OnMidsWithMimesAndAttaches{init_async_result<sync_context, OnMidsWithMimesAndAttaches>{context}.handler};
    }
    
    std::shared_ptr<MockEnvelopesRepository> env;
    Lids lids{"1", "2","3"};
    Tids tids{"100", "101","102"};
    Mids mids{"12345677890"};
};

TEST_F(EnvelopesRepositoryTest, emptyLidNotAllowed) {
    lids.emplace_back("");
    EXPECT_THROW(env->getMidsByTidsAndLids(tids, lids, GetMidsHandler()), ParamsException);
}

TEST_F(EnvelopesRepositoryTest, emptyTidNotAllowed) {
    tids.emplace_back("");
    EXPECT_THROW(env->getMidsByTidsAndLids(tids, lids, GetMidsHandler()), ParamsException);
}

TEST_F(EnvelopesRepositoryTest, getMidsByTidsAndLidsMustCallSync) {
    EXPECT_CALL(*env, syncGetMidsByTidsAndLids(tids, lids, _));
    env->getMidsByTidsAndLids(tids, lids, GetMidsHandler());
}

TEST_F(EnvelopesRepositoryTest, getMimesWithAttaches) {
    EXPECT_CALL(*env, syncGetMimesWithAttaches(mids, _));
    env->getMimesWithAttaches(mids, GetMimesHandler());
}

TEST_F(EnvelopesRepositoryTest, getMimesWithAttachesWithDeleted) {
    EXPECT_CALL(*env, syncGetMimesWithAttachesWithDeleted(mids, _));
    env->getMimesWithAttachesWithDeleted(mids, GetMimesHandler());
}

TEST_F(EnvelopesRepositoryTest, removeWithSingleMidCallSyncErase) {
    EXPECT_CALL(*env, syncErase(mids, _)).
        WillOnce(InvokeArgument<1>(macs::error_code(), Revision()));
    env->remove(mids.front());
}

TEST_F(EnvelopesRepositoryTest, removeWithMidListCallSyncErase) {
    EXPECT_CALL(*env, syncErase(mids, _)).
        WillOnce(InvokeArgument<1>(macs::error_code(), Revision()));
    env->remove(mids);
}

TEST_F(EnvelopesRepositoryTest, forceRemoveWithSingleMidCallSyncForceErase) {
    EXPECT_CALL(*env, syncForceErase(mids, _)).
        WillOnce(InvokeArgument<1>(macs::error_code(), Revision()));
    env->forceRemove(mids.front());
}

TEST_F(EnvelopesRepositoryTest, forceRemoveWithMidListCallSyncForceErase) {
    EXPECT_CALL(*env, syncForceErase(mids, _)).
        WillOnce(InvokeArgument<1>(macs::error_code(), Revision()));
    env->forceRemove(mids);
}

}
