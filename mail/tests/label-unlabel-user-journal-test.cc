#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-macs.h>
#include "user_journal_mock.h"

using namespace testing;

using MidList = std::list<macs::Mid>;
using TidList = std::list<macs::Tid>;
using LabelList = std::list<macs::Label>;
using StringVec = std::vector<std::string>;

struct LabelUnlabelTest: public Test {
    using RequestParameters = NiceMock<RequestParametersMock>;
    using Writer = StrictMock<WriterMock>;
    using UserJournal = StrictMock<UserJournalMock>;
    using Repository = StrictMock<MockEnvelopesRepository>;
    using Mapper = StrictMock<MapperMock>;

    std::shared_ptr<RequestParameters> requestInfoPtr;
    std::shared_ptr<Writer> writerPtr;
    boost::shared_ptr<UserJournal> journalPtr;
    std::shared_ptr<Repository> envelopesPtr;
    Mapper mapper;

    LabelUnlabelTest() : requestInfoPtr(new RequestParameters)
                       , writerPtr(new Writer)
                       , journalPtr(new UserJournal(Journal{requestInfoPtr, writerPtr}))
                       , envelopesPtr(new Repository(journalPtr))
                       , mapper() {}

    MidList mids;
    TidList tids;
    LabelList labels;
    macs::Uid uid;

    inline auto makeLabel(macs::Lid lid, macs::Label::Type type) {
        return macs::LabelFactory()
                .lid(lid)
                .type(type)
                .symbol(macs::Label::Symbol::none)
                .product();
    }

    inline auto makeLabel(macs::Lid lid, macs::Label::Symbol symbol) {
        return macs::LabelFactory()
                .lid(lid)
                .type(macs::Label::Type::system)
                .symbol(symbol)
                .product();
    }

    void SetUp() override {
        mids = MidList{ "100", "200", "300" };
        tids = TidList{ "10", "20", "30" };
        labels = LabelList{ makeLabel("1", macs::Label::Symbol::attached_label),
                            makeLabel("2", macs::Label::Type::social) };
        uid = "42";
    }
};

namespace macs {
inline bool operator==(const Label& lhs, const Label& rhs) {
    return (lhs.lid() == rhs.lid()) &&
           (lhs.type() == rhs.type()) &&
           (lhs.symbolicName() == rhs.symbolicName());
}
}

TEST_F(LabelUnlabelTest, shouldWriteToJournal_WhenLabel_ByMids) {
    Sequence seq;
    EXPECT_CALL(*envelopesPtr, syncAddLabels(labels, mids, _))
            .InSequence(seq)
            .WillOnce(InvokeArgument<2>(macs::error_code(), macs::Revision()));
    EXPECT_CALL(*journalPtr, asyncGetShardName(_))
            .InSequence(seq)
            .WillOnce(InvokeArgument<0>("pg"));
    EXPECT_CALL(*requestInfoPtr, uid())
            .InSequence(seq)
            .WillOnce(ReturnRef(uid));
    EXPECT_CALL(*writerPtr, write(uid, _))
            .InSequence(seq)
            .WillOnce(Invoke([&mapper = this->mapper](const std::string&, const Entry& e){
                e.map(mapper);
            }));

    EXPECT_CALL(mapper, mapValue(An<const std::string &>(), "state"));
    EXPECT_CALL(mapper, mapValue(An<const Date &>(), "date"));
    EXPECT_CALL(mapper, mapValue(An<std::time_t>(), "unixtime"));
    EXPECT_CALL(mapper, mapValue(TypedEq<const Target &>(Target::message), "target"));
    EXPECT_CALL(mapper, mapValue(std::string("pg"), "mdb"));
    EXPECT_CALL(mapper, mapValue(StringVec{"1", "2"}, "lids"));
    EXPECT_CALL(mapper, mapValue(StringVec{"system", "social"}, "labelTypes"));
    EXPECT_CALL(mapper, mapValue(StringVec{"attached_label", ""}, "labelSymbols"));
    EXPECT_CALL(mapper, mapValue(TypedEq<const Operation &>(Operation::label), "operation"));
    EXPECT_CALL(mapper, mapValue(TypedEq<std::size_t>(std::size_t(mids.size())), "affected"));
    EXPECT_CALL(mapper, mapValue(StringVec{"100", "200", "300"}, "mids"));

    envelopesPtr->markEnvelopes(mids, labels);
}

TEST_F(LabelUnlabelTest, shouldWriteToJournal_WhenUnlabel_ByMids) {
    Sequence seq;
    EXPECT_CALL(*envelopesPtr, syncRemoveLabels(labels, mids, _))
            .InSequence(seq)
            .WillOnce(InvokeArgument<2>(macs::error_code(), macs::Revision()));
    EXPECT_CALL(*journalPtr, asyncGetShardName(_))
            .InSequence(seq)
            .WillOnce(InvokeArgument<0>("pg"));
    EXPECT_CALL(*requestInfoPtr, uid())
            .InSequence(seq)
            .WillOnce(ReturnRef(uid));
    EXPECT_CALL(*writerPtr, write(uid, _))
            .InSequence(seq)
            .WillOnce(Invoke([&mapper = this->mapper](const std::string&, const Entry& e){
                e.map(mapper);
            }));

    EXPECT_CALL(mapper, mapValue(An<const std::string &>(), "state"));
    EXPECT_CALL(mapper, mapValue(An<const Date &>(), "date"));
    EXPECT_CALL(mapper, mapValue(An<std::time_t>(), "unixtime"));
    EXPECT_CALL(mapper, mapValue(TypedEq<const Target &>(Target::message), "target"));
    EXPECT_CALL(mapper, mapValue(std::string("pg"), "mdb"));
    EXPECT_CALL(mapper, mapValue(StringVec{"1", "2"}, "lids"));
    EXPECT_CALL(mapper, mapValue(StringVec{"system", "social"}, "labelTypes"));
    EXPECT_CALL(mapper, mapValue(StringVec{"attached_label", ""}, "labelSymbols"));
    EXPECT_CALL(mapper, mapValue(TypedEq<const Operation &>(Operation::unlabel), "operation"));
    EXPECT_CALL(mapper, mapValue(TypedEq<std::size_t>(std::size_t(mids.size())), "affected"));
    EXPECT_CALL(mapper, mapValue(StringVec{"100", "200", "300"}, "mids"));

    envelopesPtr->unmarkEnvelopes(mids, labels);
}

TEST_F(LabelUnlabelTest, shouldWriteToJournal_WhenLabel_ByTids) {
    Sequence seq;
    EXPECT_CALL(*envelopesPtr, syncAddLabelsByThreads(labels, tids, _))
            .InSequence(seq)
            .WillOnce(InvokeArgument<2>(macs::error_code(), macs::UpdateMessagesResult {13, 42}));
    EXPECT_CALL(*journalPtr, asyncGetShardName(_))
            .InSequence(seq)
            .WillOnce(InvokeArgument<0>("pg"));
    EXPECT_CALL(*requestInfoPtr, uid())
            .InSequence(seq)
            .WillOnce(ReturnRef(uid));
    EXPECT_CALL(*writerPtr, write(uid, _))
            .InSequence(seq)
            .WillOnce(Invoke([&mapper = this->mapper](const std::string&, const Entry& e){
                e.map(mapper);
            }));

    EXPECT_CALL(mapper, mapValue(An<const std::string &>(), "state"));
    EXPECT_CALL(mapper, mapValue(An<const Date &>(), "date"));
    EXPECT_CALL(mapper, mapValue(An<std::time_t>(), "unixtime"));
    EXPECT_CALL(mapper, mapValue(TypedEq<const Target &>(Target::message), "target"));
    EXPECT_CALL(mapper, mapValue(std::string("pg"), "mdb"));
    EXPECT_CALL(mapper, mapValue(StringVec{"1", "2"}, "lids"));
    EXPECT_CALL(mapper, mapValue(StringVec{"system", "social"}, "labelTypes"));
    EXPECT_CALL(mapper, mapValue(StringVec{"attached_label", ""}, "labelSymbols"));
    EXPECT_CALL(mapper, mapValue(TypedEq<const Operation &>(Operation::label), "operation"));
    EXPECT_CALL(mapper, mapValue(TypedEq<std::size_t>(std::size_t(42)), "affected"));
    EXPECT_CALL(mapper, mapValue(StringVec{"10", "20", "30"}, "tids"));

    envelopesPtr->markEnvelopesByThreads(tids, labels);
}

TEST_F(LabelUnlabelTest, shouldWriteToJournal_WhenUnlabel_ByTids) {
    Sequence seq;
    EXPECT_CALL(*envelopesPtr, syncRemoveLabelsByThreads(labels, tids, _))
            .InSequence(seq)
            .WillOnce(InvokeArgument<2>(macs::error_code(), macs::UpdateMessagesResult {13, 42}));
    EXPECT_CALL(*journalPtr, asyncGetShardName(_))
            .InSequence(seq)
            .WillOnce(InvokeArgument<0>("pg"));
    EXPECT_CALL(*requestInfoPtr, uid())
            .InSequence(seq)
            .WillOnce(ReturnRef(uid));
    EXPECT_CALL(*writerPtr, write(uid, _))
            .InSequence(seq)
            .WillOnce(Invoke([&mapper = this->mapper](const std::string&, const Entry& e){
                e.map(mapper);
            }));

    EXPECT_CALL(mapper, mapValue(An<const std::string &>(), "state"));
    EXPECT_CALL(mapper, mapValue(An<const Date &>(), "date"));
    EXPECT_CALL(mapper, mapValue(An<std::time_t>(), "unixtime"));
    EXPECT_CALL(mapper, mapValue(TypedEq<const Target &>(Target::message), "target"));
    EXPECT_CALL(mapper, mapValue(std::string("pg"), "mdb"));
    EXPECT_CALL(mapper, mapValue(StringVec{"1", "2"}, "lids"));
    EXPECT_CALL(mapper, mapValue(StringVec{"system", "social"}, "labelTypes"));
    EXPECT_CALL(mapper, mapValue(StringVec{"attached_label", ""}, "labelSymbols"));
    EXPECT_CALL(mapper, mapValue(TypedEq<const Operation &>(Operation::unlabel), "operation"));
    EXPECT_CALL(mapper, mapValue(TypedEq<std::size_t>(std::size_t(42)), "affected"));
    EXPECT_CALL(mapper, mapValue(StringVec{"10", "20", "30"}, "tids"));

    envelopesPtr->unmarkEnvelopesByThreads(tids, labels);
}
