#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include <internal/server/handlers/set_archivation_rule.h>
#include "test_mocks.h"

using namespace testing;

namespace york {
namespace tests {

struct SetArchivationRuleMacsTest: public Test {
    ContextMock ctx;
    MacsMock<sync> macs;
    using ArchivationType = macs::Folder::ArchivationType;
    const FolderArchivation cfg {"", 1000, 30};

    void execute(SetArchivationRuleParams params) {
        server::handlers::executeMacsSetArchivationRule(
                    macs, ctx, params, cfg, log::none, macs::io::use_sync);
    }
};

TEST_F(SetArchivationRuleMacsTest, sharedFolderFidNotFound_responses400) {
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{}));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    execute({"uid", boost::optional<std::string>("fid"), "archive", boost::none, boost::none});
}

TEST_F(SetArchivationRuleMacsTest, withTypeArchive_callsFolderSetArchivationRuleWithArchive_responsesOk) {
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macs, setArchivationRule("fid", ArchivationType{ArchivationType::archive}, 30, 1000, _)).Times(1);

    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SetArchivationRuleResult>())).Times(1);

    execute({"uid", boost::optional<std::string>("fid"), "archive",
             boost::optional<uint32_t>(30), boost::optional<uint32_t>(1000)});
}

TEST_F(SetArchivationRuleMacsTest, withTypeClean_callsFolderSetArchivationRuleWithClean_responsesOk) {
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macs, setArchivationRule("fid", ArchivationType{ArchivationType::clean}, 30, 1000, _)).Times(1);

    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SetArchivationRuleResult>())).Times(1);

    execute({"uid", boost::optional<std::string>("fid"), "clean",
             boost::optional<uint32_t>(30), boost::optional<uint32_t>(1000)});
}

TEST_F(SetArchivationRuleMacsTest, withTypeUnknown_responses400) {
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(ctx.resp, badRequest(_)).Times(1);

    execute({"uid", boost::optional<std::string>("fid"), "blahblah", boost::none, boost::none});
}

TEST_F(SetArchivationRuleMacsTest, sharedFidNotSet_usesInboxAndResponsesOk) {
    macs::Folder i = macs::FolderFactory().fid("2").name("inbox").parentId(macs::Folder::noParent).messages(0).symbol(macs::Folder::Symbol::inbox);
    macs::FoldersMap sharFs;
    sharFs.insert({"2", i});

    EXPECT_CALL(macs, getAllFolders(_)).WillOnce(Return(sharFs));
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"2"}));
    EXPECT_CALL(macs, setArchivationRule("2", ArchivationType{ArchivationType::archive}, 30, 1000, _)).Times(1);

    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SetArchivationRuleResult>())).Times(1);

    execute({"uid", boost::none, "archive",
             boost::optional<uint32_t>(30), boost::optional<uint32_t>(1000)});
}

TEST_F(SetArchivationRuleMacsTest, withKeepDaysNotSet_callsFolderSetArchivationRuleWithDefaultKeepDays_responsesOk) {
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macs, setArchivationRule("fid", ArchivationType{ArchivationType::archive}, cfg.keep_days, 1000, _)).Times(1);

    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SetArchivationRuleResult>())).Times(1);

    execute({"uid", boost::optional<std::string>("fid"), "archive",
             boost::none, boost::optional<uint32_t>(1000)});
}

TEST_F(SetArchivationRuleMacsTest, withKeepDaysGreaterThenDefault_callsFolderSetArchivationRuleWithDefaultKeepDays_responsesOk) {
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macs, setArchivationRule("fid", ArchivationType{ArchivationType::archive}, cfg.keep_days, 1000, _)).Times(1);

    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SetArchivationRuleResult>())).Times(1);

    execute({"uid", boost::optional<std::string>("fid"), "archive",
             boost::optional<uint32_t>(cfg.keep_days + 1), boost::optional<uint32_t>(1000)});
}

TEST_F(SetArchivationRuleMacsTest, withMaxSizeNotSet_callsFolderSetArchivationRuleWithDefaultMaxSize_responsesOk) {
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macs, setArchivationRule("fid", ArchivationType{ArchivationType::archive}, 30, cfg.max_folder_size, _)).Times(1);

    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SetArchivationRuleResult>())).Times(1);

    execute({"uid", boost::optional<std::string>("fid"), "archive",
             boost::optional<uint32_t>(30), boost::none});
}

TEST_F(SetArchivationRuleMacsTest, withMaxSizeGreaterThenDefault_callsFolderSetArchivationRuleWithDefaultMaxSize_responsesOk) {
    EXPECT_CALL(macs, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macs, setArchivationRule("fid", ArchivationType{ArchivationType::archive}, 30, cfg.max_folder_size, _)).Times(1);

    EXPECT_CALL(ctx.resp, ok(An<server::handlers::SetArchivationRuleResult>())).Times(1);

    execute({"uid", boost::optional<std::string>("fid"), "archive",
             boost::optional<uint32_t>(30), boost::optional<uint32_t>(cfg.max_folder_size + 1)});
}

} //namespace tests
} //namespace york
