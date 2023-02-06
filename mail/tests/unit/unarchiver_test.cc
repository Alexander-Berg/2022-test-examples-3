#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/barbet/service/include/unarchiver.h>

using namespace ::testing;

namespace barbet::tests {


struct MockPostman : public archive::Postman {
    MOCK_METHOD(Mid, restore, (const Message&, boost::asio::yield_context), (const, override));
};

struct MockUnarchiver : public archive::Unarchiver {
    MOCK_METHOD(std::vector<archive::ArchiveChunk>, getUserArchiveChunks, (const std::string&, YieldCtx), (const, override));
    MOCK_METHOD(std::vector<archive::ArchiveMessage>, getArchiveMessages, (const std::string&, YieldCtx), (const, override));

    MOCK_METHOD(void, restorationProgress, (std::int64_t, YieldCtx), (const, override));
    MOCK_METHOD(void, restorationError, (std::int64_t, YieldCtx), (const, override));
    MOCK_METHOD(void, restorationComplete, (std::int64_t, YieldCtx), (const, override));
};


struct UnarchiverTest : public Test {
    boost::asio::io_context context;
    std::shared_ptr<StrictMock<MockPostman>> postman;
    std::shared_ptr<StrictMock<MockUnarchiver>> unarchiver;

    void SetUp() {
        postman = std::make_shared<StrictMock<MockPostman>>();
        unarchiver = std::make_shared<StrictMock<MockUnarchiver>>();
    }

    template<class Fn>
    void spawn(Fn&& fn) {
        boost::asio::spawn(context, std::forward<Fn>(fn));
        context.run();
    }
};

using ArchiveChunks = std::vector<archive::ArchiveChunk>;
using ArchiveMessages = std::vector<archive::ArchiveMessage>;

std::string& uid() {
    static std::string res = "test_uid";
    return res;
}

std::string& mid() {
    static std::string res = "mid";
    return res;
}

std::string& s3Key() {
    static std::string res = "1/2_3";
    return res; 
}

std::vector<std::string>& validMailAttrs() {
    static std::vector<std::string> res = { "mulca-shared" };
    return res;
}

const std::int64_t previouslyRestored = ((std::int64_t)1 << 43);

TEST_F(UnarchiverTest, shouldNotFailWhenNoChunks) {
    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*unarchiver, getUserArchiveChunks(uid(), _))
                .WillOnce(Return(ArchiveChunks{}));
        
        EXPECT_CALL(*unarchiver, restorationComplete(previouslyRestored, _));

        unarchiver->restore(uid(), previouslyRestored, *postman, yield);
    });
}

TEST_F(UnarchiverTest, shouldNotFailWhenAllChunksAlreadyRestored) {
    spawn([=, this] (boost::asio::yield_context yield) {
        ArchiveChunks chunks = {
            {.key="1", .last_mid=50, .count=50},
            {.key="2", .last_mid=75, .count=25}
        };

        EXPECT_CALL(*unarchiver, getUserArchiveChunks(uid(), _))
                .WillOnce(Return(chunks));
        
        EXPECT_CALL(*unarchiver, restorationComplete(previouslyRestored, _));

        unarchiver->restore(uid(), previouslyRestored, *postman, yield);
    });
}


class UnarchiverWithPrevTest : public UnarchiverTest
                             , public WithParamInterface<std::int64_t> {
};

const size_t msgPerChunk = 23;
const size_t chunksCount = 53;
const std::int64_t last_mid = ((std::int64_t)1 << 43);

INSTANTIATE_TEST_SUITE_P(DifferentPreviouslyRestored,
                         UnarchiverWithPrevTest,
                         testing::Values(0, 1, 11, 42, 228, 1234, 1337));

TEST_P(UnarchiverWithPrevTest, shouldRestoreMessages) {
    spawn([=, this] (boost::asio::yield_context yield) {
        ArchiveMessages messages{msgPerChunk, {.st_id="test"}};
        ArchiveChunks chunks{chunksCount, {.key=s3Key(), .last_mid=last_mid, .count=static_cast<std::int64_t>(msgPerChunk)}};

        std::int64_t msgCount = chunksCount * msgPerChunk;
        const std::int64_t previouslyRestored = GetParam() % msgCount;
        msgCount = msgCount - previouslyRestored;

        EXPECT_CALL(*unarchiver, getUserArchiveChunks(uid(), _))
                .WillOnce(Return(chunks));

        EXPECT_CALL(*unarchiver, getArchiveMessages(s3Key(), _))
                .Times((msgCount + msgPerChunk - 1) / msgPerChunk)
                .WillRepeatedly(Return(messages));
        
        EXPECT_CALL(*unarchiver, restorationProgress(_, _)).Times(AtLeast(msgCount));

        EXPECT_CALL(*unarchiver, restorationComplete(msgCount + previouslyRestored, _));

        EXPECT_CALL(*postman, restore(_, _))
                .Times(msgCount)
                .WillRepeatedly(Return(mid()));

        unarchiver->restore(uid(), previouslyRestored, *postman, yield);
    });
}

TEST_P(UnarchiverWithPrevTest, shouldSaveCounterOfRestoreMessagesInCaseOfDeliveryError) {
    spawn([=, this] (boost::asio::yield_context yield) {
        ArchiveMessages messages{msgPerChunk, {.st_id="test"}};
        ArchiveChunks chunks{chunksCount, {.key=s3Key(), .last_mid=last_mid, .count=static_cast<std::int64_t>(msgPerChunk)}};

        std::int64_t msgCount = chunksCount * msgPerChunk;
        const std::int64_t previouslyRestored = GetParam() % msgCount;
        msgCount = msgCount - previouslyRestored;
        
        EXPECT_CALL(*unarchiver, getUserArchiveChunks(uid(), _))
                .WillOnce(Return(chunks));
        EXPECT_CALL(*unarchiver, getArchiveMessages(s3Key(), _))
                .WillRepeatedly(Return(messages));

        const std::int64_t randomNubmerToSubstruct = 11;
        const std::int64_t correctRestorations = msgCount - randomNubmerToSubstruct;
        EXPECT_CALL(*unarchiver, restorationProgress(_, _)).Times(correctRestorations);
        EXPECT_CALL(*unarchiver, restorationError(correctRestorations + previouslyRestored, _)).Times(1);
        Expectation goodCalls = EXPECT_CALL(*postman, restore(_, _))
                .Times(correctRestorations)
                .WillRepeatedly(Return(mid()));
        EXPECT_CALL(*postman, restore(_, _))
                .After(goodCalls)
                .WillOnce(Throw(std::runtime_error{""}));
        
        EXPECT_THROW(unarchiver->restore(uid(), previouslyRestored, *postman, yield), std::runtime_error);
    });
}



TEST_F(UnarchiverTest, shouldThrowWhenUnderlying_getUserArchiveChunks_Throw) {
    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*unarchiver, getUserArchiveChunks(uid(), _))
                .WillOnce(Throw(std::runtime_error{""}));
        EXPECT_THROW(unarchiver->restore(uid(), 0, *postman, yield), std::runtime_error);
    });
}

TEST_F(UnarchiverTest, shouldThrowWhenUnderlying_getArchiveMessages_Throw) {
    spawn([=, this] (boost::asio::yield_context yield) {
        ArchiveChunks chunks{1, {.count=1}};
        EXPECT_CALL(*unarchiver, getUserArchiveChunks(uid(), _))
                .WillRepeatedly(Return(chunks));
        
        EXPECT_CALL(*unarchiver, getArchiveMessages(_, _))
                .WillRepeatedly(Throw(std::runtime_error{""}));
        
        EXPECT_THROW(unarchiver->restore(uid(), 0, *postman, yield), std::runtime_error);
        EXPECT_THROW(unarchiver->restore(uid(), 0, *postman, yield), std::runtime_error);
    });
}


TEST_F(UnarchiverTest, shouldThrowWhenArchiveSizeMismatch) {
    spawn([=, this] (boost::asio::yield_context yield) {
        ArchiveChunks chunks{1, {.count=10}};
        EXPECT_CALL(*unarchiver, getUserArchiveChunks(uid(), _))
                .WillRepeatedly(Return(chunks));
        
        EXPECT_CALL(*unarchiver, getArchiveMessages(_, _))
                .WillRepeatedly(Return(ArchiveMessages{1}));

        EXPECT_CALL(*unarchiver, restorationError(_, _)).Times(1);
        
        EXPECT_THROW(unarchiver->restore(uid(), 0, *postman, yield), std::runtime_error);
    });
}


}
