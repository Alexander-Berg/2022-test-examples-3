#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/server/query_params.h>
#include <macs/label_factory.h>
#include <macs/folder_factory.h>
#include <macs/tab_factory.h>

namespace macs{

inline bool operator==(const Label& lhs, const Label& rhs) {
    return lhs.lid() == rhs.lid()
            && lhs.name() == rhs.name();
}

inline bool operator==(const Folder& lhs, const Folder& rhs) {
    return lhs.fid() == rhs.fid()
            && lhs.name() == rhs.name();
}

inline bool operator==(const Tab& lhs, const Tab& rhs) {
    return lhs.type() == rhs.type();
}

}

namespace {

#define ASSERT_THROW_CODE(Statement, Code) \
    try { \
        Statement; \
        FAIL() << "Should throw exception here"; \
    } catch (const boost::system::system_error& e) { \
        ASSERT_EQ(e.code(), Code); \
    }

using namespace ::testing;
namespace error = ::hound::server::error;
using namespace ::hound::server;

struct MailboxMock {
    MOCK_METHOD(macs::LabelSet, labels, (), (const));
    MOCK_METHOD(macs::FolderSet, folders, (), (const));
    MOCK_METHOD(macs::TabSet, tabs, (), (const));
};

struct RequestMock {
    MOCK_METHOD(boost::optional<std::string>, tryGetArg, (const std::string&), (const));

    auto getOptionalArg(const std::string& name) const {
        return tryGetArg(name);
    }

    auto getArg(const std::string& name) const {
        return getOptionalArg(name).get_value_or("");
    }

    bool getArgument(const std::string& name, std::string& value) const {
        auto arg = getOptionalArg(name);
        if (!arg) {
            return false;
        }
        value = *arg;
        return true;
    }

    template <class Out>
    void getArgList(const std::string& name, Out out) const {
        while (true) {
            auto arg = getOptionalArg(name);
            if (!arg) {
                return;
            }
            out = *arg;
        }
    }
};

struct QueryParamsTest : public Test {
    StrictMock<MailboxMock> mailbox;
    StrictMock<RequestMock> request;
    QueryParams queryParams;

    static auto ReturnOpt(std::string s) {
        return Return(boost::make_optional<std::string>(std::move(s)));
    }
};

TEST_F(QueryParamsTest, forEmptyParam_shouldParseNothing_returnsOk) {
    struct Params {} params;
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
}

TEST_F(QueryParamsTest, forPageParam_shouldParse_forNoCount_returnsInvalidArgument) {
    struct Params : QueryParams::Page {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("count")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forPageParam_shouldParse_forBadCount_returnsInvalidArgument) {
    struct Params : QueryParams::Page {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("count")).WillOnce(ReturnOpt("asdf"));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forPageParam_shouldParse_forNoFirstOrPage_returnsInvalidArgument) {
    struct Params : QueryParams::Page {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("count")).WillOnce(ReturnOpt("100"));
    EXPECT_CALL(request, tryGetArg("first")).WillOnce(Return(boost::none));
    EXPECT_CALL(request, tryGetArg("page")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forPageParam_shouldParse_forBadFirst_returnsInvalidArgument) {
    struct Params : QueryParams::Page {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("count")).WillOnce(ReturnOpt("100"));
    EXPECT_CALL(request, tryGetArg("first")).WillOnce(ReturnOpt("asdf"));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forPageParam_shouldParse_forGoodFirst_returnsOk) {
    struct Params : QueryParams::Page {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("count")).WillOnce(ReturnOpt("100"));
    EXPECT_CALL(request, tryGetArg("first")).WillOnce(ReturnOpt("10"));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.count, 100);
    ASSERT_EQ(params.first, 10);
}

TEST_F(QueryParamsTest, forPageParam_shouldParse_forNoFirstBadPage_returnsInvalidArgument) {
    struct Params : QueryParams::Page {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("count")).WillOnce(ReturnOpt("100"));
    EXPECT_CALL(request, tryGetArg("first")).WillOnce(Return(boost::none));
    EXPECT_CALL(request, tryGetArg("page")).WillOnce(ReturnOpt("asdf"));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forPageParam_shouldParse_forZeroPage_returnsInvalidArgument) {
    struct Params : QueryParams::Page {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("count")).WillOnce(ReturnOpt("100"));
    EXPECT_CALL(request, tryGetArg("first")).WillOnce(Return(boost::none));
    EXPECT_CALL(request, tryGetArg("page")).WillOnce(ReturnOpt("0"));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forPageParam_shouldParse_forGoodPage_returnsOk) {
    struct Params : QueryParams::Page {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("count")).WillOnce(ReturnOpt("100"));
    EXPECT_CALL(request, tryGetArg("first")).WillOnce(Return(boost::none));
    EXPECT_CALL(request, tryGetArg("page")).WillOnce(ReturnOpt("2"));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.count, 101);
    ASSERT_EQ(params.first, 100);
}


TEST_F(QueryParamsTest, forTimestampRangeParam_shouldParse_forNoSinceAndTill_returnsOk) {
    struct Params : QueryParams::TimestampRange {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("since")).WillOnce(Return(boost::none));
    EXPECT_CALL(request, tryGetArg("till")).WillOnce(Return(boost::none));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_FALSE(params.timestampRange);
}

TEST_F(QueryParamsTest, forTimestampRangeParam_shouldParse_forBadSince_returnsInvalidArgument) {
    struct Params : QueryParams::TimestampRange {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("since")).WillOnce(ReturnOpt("asdf"));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forTimestampRangeParam_shouldParse_forBadTill_returnsInvalidArgument) {
    struct Params : QueryParams::TimestampRange {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("since")).WillOnce(ReturnOpt("42"));
    EXPECT_CALL(request, tryGetArg("till")).WillOnce(ReturnOpt("asdf"));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forTimestampRangeParam_shouldParse_forNoSince_setsSinceToDefault_returnsOk) {
    struct Params : QueryParams::TimestampRange {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("since")).WillOnce(Return(boost::none));
    EXPECT_CALL(request, tryGetArg("till")).WillOnce(ReturnOpt("42"));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_TRUE(params.timestampRange);
    ASSERT_EQ((*params.timestampRange).first, 0);
    ASSERT_EQ((*params.timestampRange).second, 42);
}

TEST_F(QueryParamsTest, forTimestampRangeParam_shouldParse_forNoTill_setsTillToDefault_returnsOk) {
    struct Params : QueryParams::TimestampRange {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("since")).WillOnce(ReturnOpt("13"));
    EXPECT_CALL(request, tryGetArg("till")).WillOnce(Return(boost::none));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_TRUE(params.timestampRange);
    ASSERT_EQ((*params.timestampRange).first, 13);
    ASSERT_EQ((*params.timestampRange).second, 0);
}

TEST_F(QueryParamsTest, forTimestampRangeParam_shouldParse_forSinceAndTill_returnsOk) {
    struct Params : QueryParams::TimestampRange {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("since")).WillOnce(ReturnOpt("13"));
    EXPECT_CALL(request, tryGetArg("till")).WillOnce(ReturnOpt("42"));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_TRUE(params.timestampRange);
    ASSERT_EQ((*params.timestampRange).first, 13);
    ASSERT_EQ((*params.timestampRange).second, 42);
}


TEST_F(QueryParamsTest, forMidParam_shouldParse_forNoMid_returnsInvalidArgument) {
    struct Params : QueryParams::Mid {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("mid")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forMidParam_shouldParse_forGoodMid_returnsOk) {
    struct Params : QueryParams::Mid {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("mid")).WillOnce(ReturnOpt("mid1"));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.mid, "mid1");
}


TEST_F(QueryParamsTest, forDeviationParam_shouldParse_forNoDeviation_returnsInvalidArgument) {
    struct Params : QueryParams::Deviation {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("deviation")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forDeviationParam_shouldParse_forBadDeviation_returnsInvalidArgument) {
    struct Params : QueryParams::Deviation {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("deviation")).WillOnce(ReturnOpt("asdf"));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forDeviationParam_shouldParse_forGoodDeviation_returnsOk) {
    struct Params : QueryParams::Deviation {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("deviation")).WillOnce(ReturnOpt("10"));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.deviation, 10UL);
}


TEST_F(QueryParamsTest, forLabelParam_shouldParse_forNoLid_returnsInvalidArgument) {
    struct Params : QueryParams::Label {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("lid")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forLabelParam_shouldParse_forUnexistingLid_throwSystemError) {
    struct Params : QueryParams::Label {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("lid")).WillOnce(ReturnOpt("lid1"));
    EXPECT_CALL(mailbox, labels()).WillOnce(Return(macs::LabelSet{}));
    ASSERT_THROW_CODE(queryParams.getParams(params, request, mailbox), macs::error::noSuchLabel);
}

TEST_F(QueryParamsTest, forLabelParam_shouldParse_forExistingLid_returnsOk) {
    struct Params : QueryParams::Label {} params;
    macs::Label lbl = macs::LabelFactory().lid("lid1").product();
    macs::LabelSet set;
    set["lid1"] = lbl;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("lid")).WillOnce(ReturnOpt("lid1"));
    EXPECT_CALL(mailbox, labels()).WillOnce(Return(set));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.label, lbl);
}


TEST_F(QueryParamsTest, forFolderParam_shouldParse_forNoFid_returnsInvalidArgument) {
    struct Params : QueryParams::Folder {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("fid")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forFolderParam_shouldParse_forUnexistingFid_throwSystemError) {
    struct Params : QueryParams::Folder {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("fid")).WillOnce(ReturnOpt("fid1"));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet{}));
    ASSERT_THROW_CODE(queryParams.getParams(params, request, mailbox), macs::error::noSuchFolder);
}

TEST_F(QueryParamsTest, forFolderParam_shouldParse_forExistingFid_returnsOk) {
    struct Params : QueryParams::Folder {} params;
    macs::Folder fld = macs::FolderFactory().fid("fid1").product();
    macs::FoldersMap map = {{"fid1", fld},};
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("fid")).WillOnce(ReturnOpt("fid1"));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet(map)));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.folder, fld);
}


TEST_F(QueryParamsTest, forFolderOrInboxParam_shouldParse_forNoFid_usesInbox_returnsOk) {
    struct Params : QueryParams::FolderOrInbox {} params;
    macs::Folder inbox = macs::FolderFactory().fid("in").symbol(macs::Folder::Symbol::inbox).product();
    macs::FoldersMap map = {{"in", inbox},};
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("fid")).WillOnce(Return(boost::none));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet(map)));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.folder, inbox);
}

TEST_F(QueryParamsTest, forFolderOrInboxParam_shouldParse_forUnexistingFid_throwSystemError) {
    struct Params : QueryParams::FolderOrInbox {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("fid")).WillOnce(ReturnOpt("fid1"));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet{}));
    ASSERT_THROW_CODE(queryParams.getParams(params, request, mailbox), macs::error::noSuchFolder);
}

TEST_F(QueryParamsTest, forFolderOrInboxParam_shouldParse_forExistingFid_returnsOk) {
    struct Params : QueryParams::FolderOrInbox {} params;
    macs::Folder fld = macs::FolderFactory().fid("fid1").product();
    macs::FoldersMap map = {{"fid1", fld},};
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("fid")).WillOnce(ReturnOpt("fid1"));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet(map)));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.folder, fld);
}


TEST_F(QueryParamsTest, forFoldersParam_shouldParse_forNoFid_returnsInvalidArgument) {
    struct Params : QueryParams::Folders {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("fid")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forFoldersParam_shouldParse_forUnexistingFid_throwSystemError) {
    struct Params : QueryParams::Folders {} params;
    InSequence seq;
    macs::Folder fld = macs::FolderFactory().fid("fid1").product();
    macs::FoldersMap map = {{"fid", fld},};
    EXPECT_CALL(request, tryGetArg("fid"))
            .WillOnce(ReturnOpt("fid1"))
            .WillOnce(ReturnOpt("fid2"))
            .WillOnce(Return(boost::none));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet(map)));
    ASSERT_THROW_CODE(queryParams.getParams(params, request, mailbox), macs::error::noSuchFolder);
}

TEST_F(QueryParamsTest, forFoldersParam_shouldParse_forAllExistingFids_returnsOk) {
    struct Params : QueryParams::Folders {} params;
    macs::FoldersMap map = {
        {"fid1", macs::FolderFactory().fid("fid1").product()},
        {"fid2", macs::FolderFactory().fid("fid2").product()}
    };
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("fid"))
            .WillOnce(ReturnOpt("fid1"))
            .WillOnce(ReturnOpt("fid2"))
            .WillOnce(Return(boost::none));
    EXPECT_CALL(mailbox, folders()).WillOnce(Return(macs::FolderSet(map)));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_THAT(params.folders, ElementsAre(map["fid1"], map["fid2"]));
}


TEST_F(QueryParamsTest, forTidParam_shouldParse_forNoTid_returnsInvalidArgument) {
    struct Params : QueryParams::Tid {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("tid")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forTidParam_shouldParse_forGoodTid_returnsOk) {
    struct Params : QueryParams::Tid {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("tid")).WillOnce(ReturnOpt("tid1"));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.tid, "tid1");
}


TEST_F(QueryParamsTest, forTidsParam_shouldParse_forNoTid_returnsInvalidArgument) {
    struct Params : QueryParams::Tids {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("tid")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forTidsParam_shouldParse_forGoodTids_returnsOk) {
    struct Params : QueryParams::Tids {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("tid"))
            .WillOnce(ReturnOpt("tid1"))
            .WillOnce(ReturnOpt("tid2"))
            .WillOnce(Return(boost::none));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_THAT(params.tids, ElementsAre("tid1", "tid2"));
}


TEST_F(QueryParamsTest, forLidsParam_shouldParse_forNoLid_returnsInvalidArgument) {
    struct Params : QueryParams::Lids {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("lid")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forLidsParam_shouldParse_forGoodLids_returnsOk) {
    struct Params : QueryParams::Lids {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("lid"))
            .WillOnce(ReturnOpt("lid1"))
            .WillOnce(ReturnOpt("lid2"))
            .WillOnce(Return(boost::none));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_THAT(params.lids, ElementsAre("lid1", "lid2"));
}


TEST_F(QueryParamsTest, forTabParam_shouldParse_forNoTab_returnsInvalidArgument) {
    struct Params : QueryParams::Tab {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("tab")).WillOnce(Return(boost::none));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forTabParam_shouldParse_forUnexistingTab_throwSystemError) {
    struct Params : QueryParams::Tab {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("tab")).WillOnce(ReturnOpt("asdf"));
    EXPECT_CALL(mailbox, tabs()).WillOnce(Return(macs::TabSet{}));
    ASSERT_THROW_CODE(queryParams.getParams(params, request, mailbox), macs::error::noSuchTab);
}

TEST_F(QueryParamsTest, forTabParam_shouldParse_forExistingTab_returnsOk) {
    struct Params : QueryParams::Tab {} params;
    macs::Tab tab = macs::TabFactory().type(macs::Tab::Type::news).release();
    macs::TabsMap map;
    map[macs::Tab::Type::news] = tab;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("tab")).WillOnce(ReturnOpt("news"));
    EXPECT_CALL(mailbox, tabs()).WillOnce(Return(macs::TabSet(map)));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.tab, tab);
}


TEST_F(QueryParamsTest, forMessageFormatParam_shouldParse_forNoFormat_returnsOk) {
    struct Params : QueryParams::MessageFormat {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("format")).WillOnce(Return(boost::none));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.messageFormat, QueryParams::MessageFormat::Format::none);
}

TEST_F(QueryParamsTest, forMessageFormatParam_shouldParse_forBadFromat_returnsInvalidArgument) {
    struct Params : QueryParams::MessageFormat {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("format")).WillOnce(ReturnOpt("asdf"));
    ASSERT_EQ(queryParams.getParams(params, request, mailbox), error_code{error::invalidArgument});
}

TEST_F(QueryParamsTest, forMessageFormatParam_shouldParse_forGoodFormat_returnsOk) {
    struct Params : QueryParams::MessageFormat {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("format")).WillOnce(ReturnOpt("full"));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.messageFormat, QueryParams::MessageFormat::Format::full);
}


struct SortTypeParamsTest : public ::testing::TestWithParam<std::pair<boost::optional<std::string>, macs::EnvelopesSorting>> {
    StrictMock<MailboxMock> mailbox;
    StrictMock<RequestMock> request;
    QueryParams queryParams;
};

INSTANTIATE_TEST_SUITE_P(forSortTypeParam_shouldParse_returnsOk, SortTypeParamsTest, ::testing::Values(
    std::make_pair(boost::none, macs::EnvelopesSorting()),
    std::make_pair(boost::make_optional<std::string>("asdf"), macs::EnvelopesSorting()),
    std::make_pair(boost::make_optional<std::string>("date_descending"),
                   macs::EnvelopesSorting(macs::SortingType_descending)),
    std::make_pair(boost::make_optional<std::string>("date_ascending"),
                   macs::EnvelopesSorting(macs::SortingType_ascending))
));

TEST_P(SortTypeParamsTest, forSortTypeParam_shouldParse_returnsOk) {
    const auto [retArg, sortType] = GetParam();
    struct Params : QueryParams::SortType {} params;
    InSequence seq;
    EXPECT_CALL(request, tryGetArg("sort_type")).WillOnce(Return(retArg));
    ASSERT_FALSE(queryParams.getParams(params, request, mailbox));
    ASSERT_EQ(params.sortType, sortType);
}

#undef ASSERT_THROW_CODE

}

