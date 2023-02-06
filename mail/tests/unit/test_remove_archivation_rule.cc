#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include <internal/server/handlers/remove_archivation_rule.h>
#include "test_mocks.h"

using namespace testing;

namespace york {
namespace tests {

struct RemoveArchivationRuleMacsTest: public Test {
    ContextMock ctx;
    MacsMock<sync> macs;
};

TEST_F(RemoveArchivationRuleMacsTest, callsFolderRemoveArchivationRuleResponsesOk) {
    EXPECT_CALL(macs, removeArchivationRule("fid", _)).Times(1);

    EXPECT_CALL(ctx.resp, ok(An<server::handlers::RemoveArchivationRuleResult>())).Times(1);

    server::handlers::executeMacsRemoveArchivationRule(macs, ctx,
        RemoveArchivationRuleParams{"uid", boost::optional<std::string>("fid")},
        macs::io::use_sync);
}

TEST_F(RemoveArchivationRuleMacsTest, sharedFidNotSet_usesInboxAndResponsesOk) {
    macs::Folder i = macs::FolderFactory().fid("2").name("inbox").parentId(macs::Folder::noParent).messages(0).symbol(macs::Folder::Symbol::inbox);
    macs::FoldersMap sharFs;
    sharFs.insert({"2", i});

    EXPECT_CALL(macs, getAllFolders(_)).WillOnce(Return(sharFs));
    EXPECT_CALL(macs, removeArchivationRule("2", _)).Times(1);

    EXPECT_CALL(ctx.resp, ok(An<server::handlers::RemoveArchivationRuleResult>())).Times(1);

    server::handlers::executeMacsRemoveArchivationRule(macs, ctx,
        RemoveArchivationRuleParams{"uid", boost::none},
        macs::io::use_sync);
}

} //namespace tests
} //namespace york
