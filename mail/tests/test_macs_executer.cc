#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/server/handlers/macs.h>

namespace {

#define ASSERT_THROW_CODE(statement, errcode)           \
    try {                                               \
        statement;                                      \
        FAIL() << "Expect exception to be thrown";      \
    } catch(const boost::system::system_error& e) {     \
        ASSERT_EQ(errcode, e.code());                   \
    }

#define ASSERT_THROW_CATEGORY(statement, errcat)        \
    try {                                               \
        statement;                                      \
        FAIL() << "Expect exception to be thrown";      \
    } catch(const boost::system::system_error& e) {     \
        ASSERT_EQ(errcat, e.code().category());         \
    }

using namespace ::testing;
using ::hound::server::handlers::UserStrategy;
using ::hound::server::handlers::UserType;

struct Request {};
struct MailMetadata {};
using MailMetadataPtr = std::shared_ptr<MailMetadata>;

struct MacsHandlerMock {
    MOCK_METHOD(void, executeMacs, (std::shared_ptr<Request>, MailMetadataPtr&), (const));
    MOCK_METHOD(MailMetadataPtr, getMetadata, (Request&, UserType userType), (const));
};

struct MacsExecuterTest : public Test {
    StrictMock<MacsHandlerMock> mock;

    void execute(UserStrategy userStrategy) const {
        ::hound::server::handlers::MacsExecuter(userStrategy).execute(
                    std::make_shared<Request>(),
                    [&](auto r, UserType u){ return this->mock.getMetadata(r, u); },
                    [&](auto r, auto m){ this->mock.executeMacs(r, m); }
        );
    }

    auto uidNotFoundError() {
        using namespace ::sharpei::client;
        return boost::system::system_error(make_error_code(Errors::UidNotFound));
    }
    auto sharpeiError() {
        using namespace ::sharpei::client;
        return boost::system::system_error(make_error_code(Errors::ShardNotFound));
    }
    auto macsError() {
        using namespace ::macs::error;
        return boost::system::system_error(make_error_code(noSuchFolder));
    }
    auto otherError() {
        return std::runtime_error("runtime error");
    }
};

TEST_F(MacsExecuterTest,
       executeWithExistingOnly_getMetadataForExisting_executeMacs) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Return());

    execute(UserStrategy::existingOnly);
}

TEST_F(MacsExecuterTest,
       executeWithExistingOnly_getMetadataForExisting_uidNotFoundErrorOnExecuteMacs_throws) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(uidNotFoundError()));

    ASSERT_THROW_CODE(execute(UserStrategy::existingOnly), ::sharpei::client::Errors::UidNotFound);
}

TEST_F(MacsExecuterTest,
       executeWithExistingOnly_getMetadataForExisting_sharpeiErrorOnExecuteMacs_throws) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(sharpeiError()));

    ASSERT_THROW_CATEGORY(execute(UserStrategy::existingOnly), ::sharpei::client::getErrorCategory());
}

TEST_F(MacsExecuterTest,
       executeWithExistingOnly_getMetadataForExisting_macsErrorOnExecuteMacs_throws) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(macsError()));

    ASSERT_THROW_CATEGORY(execute(UserStrategy::existingOnly), ::macs::error::getCategory());
}

TEST_F(MacsExecuterTest,
       executeWithExistingOnly_getMetadataForExisting_otherErrorOnExecuteMacs_throws) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(otherError()));

    ASSERT_THROW(execute(UserStrategy::existingOnly), std::exception);
}


TEST_F(MacsExecuterTest,
       executeWithExistingThenDeleted_getMetadataForExisting_executeMacs) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Return());

    execute(UserStrategy::existingThenDeleted);
}

TEST_F(MacsExecuterTest,
       executeWithExistingThenDeleted_getMetadataForExisting_uidNotFoundErrorOnExecuteMacs_getMetadataForDeleted_executeMacs) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(uidNotFoundError()));
    EXPECT_CALL(mock, getMetadata(_, UserType::deleted)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Return());

    execute(UserStrategy::existingThenDeleted);
}

TEST_F(MacsExecuterTest,
       executeWithExistingThenDeleted_getMetadataForExisting_uidNotFoundErrorOnExecuteMacs_getMetadataForDeleted_uidNotFoundErrorOnExecuteMacs_throws) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(uidNotFoundError()));
    EXPECT_CALL(mock, getMetadata(_, UserType::deleted)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(uidNotFoundError()));

    ASSERT_THROW_CODE(execute(UserStrategy::existingThenDeleted), ::sharpei::client::Errors::UidNotFound);
}

TEST_F(MacsExecuterTest,
       executeWithExistingThenDeleted_getMetadataForExisting_uidNotFoundErrorOnExecuteMacs_getMetadataForDeleted_sharpeiErrorOnExecuteMacs_throws) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(uidNotFoundError()));
    EXPECT_CALL(mock, getMetadata(_, UserType::deleted)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(sharpeiError()));

    ASSERT_THROW_CATEGORY(execute(UserStrategy::existingThenDeleted), ::sharpei::client::getErrorCategory());
}

TEST_F(MacsExecuterTest,
       executeWithExistingThenDeleted_getMetadataForExisting_uidNotFoundErrorOnExecuteMacs_getMetadataForDeleted_macsErrorOnExecuteMacs_throws) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(uidNotFoundError()));
    EXPECT_CALL(mock, getMetadata(_, UserType::deleted)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(macsError()));

    ASSERT_THROW_CATEGORY(execute(UserStrategy::existingThenDeleted), ::macs::error::getCategory());
}

TEST_F(MacsExecuterTest,
       executeWithExistingThenDeleted_getMetadataForExisting_uidNotFoundErrorOnExecuteMacs_getMetadataForDeleted_otherErrorOnExecuteMacs_throws) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(uidNotFoundError()));
    EXPECT_CALL(mock, getMetadata(_, UserType::deleted)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(otherError()));

    ASSERT_THROW(execute(UserStrategy::existingThenDeleted), std::exception);
}

TEST_F(MacsExecuterTest,
       executeWithExistingThenDeleted_getMetadataForExisting_sharpeiErrorOnExecuteMacs_getMetadataForDeleted_executeMacs) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(sharpeiError()));

    ASSERT_THROW_CATEGORY(execute(UserStrategy::existingThenDeleted), ::sharpei::client::getErrorCategory());
}

TEST_F(MacsExecuterTest,
       executeWithExistingThenDeleted_getMetadataForExisting_macsErrorOnExecuteMacs_throws) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(macsError()));

    ASSERT_THROW_CATEGORY(execute(UserStrategy::existingThenDeleted), ::macs::error::getCategory());
}

TEST_F(MacsExecuterTest,
       executeWithExistingThenDeleted_getMetadataForExisting_otherErrorOnExecuteMacs_throws) {
    InSequence seq;
    EXPECT_CALL(mock, getMetadata(_, UserType::existing)).WillOnce(Return(std::make_shared<MailMetadata>()));
    EXPECT_CALL(mock, executeMacs(_, _)).WillOnce(Throw(otherError()));

    ASSERT_THROW(execute(UserStrategy::existingThenDeleted), std::exception);
}

}

