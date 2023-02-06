#include <mail/hound/include/internal/v2/mids_by_tids_and_lids/method.h>
#include <gmock/gmock.h>

namespace {

using namespace hound::server::handlers::v2::mids_by_tidsandlids;
using namespace macs;
using namespace testing;

using io::sync_context;

struct MailboxMock {
    MOCK_METHOD(MidList, midsByTidsAndLids, (Tids, Lids), (const));

    void expect(const Tids& tids, const Lids& lids, const Mids& mids) const {
        EXPECT_CALL(*this, midsByTidsAndLids(tids, lids)).WillOnce(Return(mids));
    }
};

struct MailboxGetterMock {
    MOCK_METHOD(const MailboxMock&, call, (), (const));

    const auto& operator()() const {
        return call();
    }

    void expect() const {
        EXPECT_CALL(*this, call()).WillOnce(ReturnRef(mailbox));
    }

    MailboxMock mailbox;
};

class MethodTest : public Test {
protected:
    static std::string ErrorMessage(const yamail::expected<Response>& result) {
        return result.error().message();
    }

    const Lids lids{"lid0", "lid1","lid2"};
    const Tids tids{"tid0", "tid1","tid2"};
    const std::size_t limit{200};
    Method<MailboxGetterMock> method;
};

TEST_F(MethodTest, tidsLimit) {
    const auto localLimit(2u);
    const auto result(method({tids, lids}, localLimit));

    const auto formatMessage([](uint32_t count, uint32_t limit) {
        std::stringstream message;
        message << "TID count (" << count << ") is greater than the limit (" << limit << ")";
        return message.str();
    });

    EXPECT_EQ(formatMessage(tids.size(), localLimit), ErrorMessage(result));
}

TEST_F(MethodTest, absentTidNotAllowed) {
    const auto result(method({{}, lids}, limit));
    EXPECT_EQ("TID parameters are required", ErrorMessage(result));
}

TEST_F(MethodTest, absentLidNotAllowed) {
    const auto result(method({tids, {}}, limit));
    EXPECT_EQ("LID parameters are required", ErrorMessage(result));
}

TEST_F(MethodTest, emptyTidNotAllowed) {
    const auto result(method({{"tid0", "","tid2"}, lids}, limit));
    EXPECT_EQ("TID must not be empty", ErrorMessage(result));
}

TEST_F(MethodTest, emptyLidNotAllowed) {
    const auto result(method({tids, {"lid0", "","lid2"}}, limit));
    EXPECT_EQ("LID must not be empty", ErrorMessage(result));
}

TEST_F(MethodTest, correctWorkflow) {
    InSequence sequence;
    method.getMailbox.expect();

    const Mids mids{"mid0", "mid1"};
    method.getMailbox.mailbox.expect(tids, lids, mids);

    const auto result(method({tids, lids}, limit));
    EXPECT_EQ(mids, result.value().mids);
}

}
