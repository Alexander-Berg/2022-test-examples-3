#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/spaniel/service/include/message_access.h>
#include <mail/spaniel/ymod_db/tests/mock_repository.h>


using namespace ::testing;

namespace spaniel::tests {


struct CheckMessageAccessTest: public Test {
    std::shared_ptr<yplatform::reactor> reactor;
    ConfigPtr config;
    std::shared_ptr<StrictMock<MockRepository>> repo;

    void SetUp() override {
        reactor = std::make_shared<yplatform::reactor>();
        reactor->init(1, 1);

        config = std::make_shared<Config>();
        repo = std::make_shared<StrictMock<MockRepository>>();
        config->repo = repo;
    }

    template<class Fn>
    void spawn(Fn fn) {
        boost::asio::spawn(*reactor->io(), fn);
        reactor->io()->run();
    }

    void databaseReturns(bool val) const {
        EXPECT_CALL(*repo, asyncMessagesInSearch(_, _, _))
        .WillOnce(Invoke([=] (const auto&, const auto&, OnMessagesInSearch cb) {
            cb(val);
        }));
    }

    void call(boost::asio::yield_context yield) const {
        checkMessageAccess(CommonParams(), SingleMessageAccessParams(), config, yield);
    }
};


TEST_F(CheckMessageAccessTest, shouldPassIfDatabaseReturnsTrue) {
    databaseReturns(true);

    spawn([=] (boost::asio::yield_context yield) {
        ASSERT_NO_THROW(this->call(yield));
    });
}

TEST_F(CheckMessageAccessTest, shouldThrowAnExceptionIfDatabaseReturnsFalse) {
    databaseReturns(false);

    spawn([=] (boost::asio::yield_context yield) {
        ASSERT_THROW(this->call(yield), mail_errors::system_error);
    });
}

}
