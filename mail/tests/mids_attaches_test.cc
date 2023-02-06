#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "headers/mock_rfc_message.h"
#include <mail/sendbernar/composer/include/mids_attaches.h>
#include <mail/sendbernar/composer/tests/mock/mail_getter.h>
#include <mail/sendbernar/core/include/logger.h>

using namespace testing;
using namespace sendbernar::tests;

namespace sendbernar {

struct TestMidsAttaches: public MidsAttaches {
    using MidsAttaches::MidsAttaches;

    MOCK_METHOD(MessageAccessPtr, messageAccess, (mail_getter::MidStidMime), (const, override));
    MOCK_METHOD((std::pair<macs::error_code, macs::MidsWithMimes>), getMimes, (), (const, override));
};

struct MidsAttachesTest: public Test {
    std::shared_ptr<StrictMock<MockRfcMessage>> rfcMessage;
    std::shared_ptr<StrictMock<MockRecognizerWrapper>> recognizer;
    std::shared_ptr<StrictMock<MockMessageAccess>> messageAccess;
    const std::size_t attachmentsMaxSize = 10;
    std::size_t totalAttachmentSize = 0;
    macs::MidsWithMimes midsWithMimes;

    void SetUp() override {
        rfcMessage = std::make_shared<StrictMock<MockRfcMessage>>();
        recognizer = std::make_shared<StrictMock<MockRecognizerWrapper>>();
        messageAccess = std::make_shared<StrictMock<MockMessageAccess>>();
        totalAttachmentSize = 0;
        midsWithMimes = macs::MidsWithMimes {
            std::make_tuple("mid1", "stid1", std::vector<macs::MimePart>()),
            std::make_tuple("mid2", "stid2", std::vector<macs::MimePart>()),
        };
    }

    std::shared_ptr<StrictMock<TestMidsAttaches>> midsAttaches() const {
        params::SendMessage p {
            .attaches = params::Attaches {
                .forward_mids = boost::make_optional(std::vector<macs::Mid>{
                    "mid1"
                })
            }
        };

        return std::make_shared<StrictMock<TestMidsAttaches>>(p, nullptr, nullptr, *recognizer, attachmentsMaxSize,
                                                              getContextLogger("", boost::none));
    }
};

TEST_F(MidsAttachesTest, shouldThrowAnExceptionInCaseOfError) {
    const auto mock = midsAttaches();
    EXPECT_CALL(*mock, getMimes())
            .WillOnce(Return(std::make_pair(make_error(ErrorResult::bbError, ""), macs::MidsWithMimes())));

    EXPECT_THROW(mock->apply(*rfcMessage, totalAttachmentSize), std::runtime_error);
}

TEST_F(MidsAttachesTest, shouldReturnAttachmentTooBigInCaseOfTheLimitIsExceeded) {
    const auto mock = midsAttaches();
    EXPECT_CALL(*mock, getMimes())
            .WillOnce(Return(std::make_pair(mail_errors::error_code(), midsWithMimes)));

    EXPECT_CALL(*mock, messageAccess(_))
            .WillOnce(Return(messageAccess));

    EXPECT_CALL(*messageAccess, getWhole())
            .WillOnce(Return(std::string('~', attachmentsMaxSize+1)));

    EXPECT_EQ(mock->apply(*rfcMessage, totalAttachmentSize), ComposeResult::ATTACHMENT_TOO_BIG);
}

TEST_F(MidsAttachesTest, shouldAddAttachments) {
    const auto mock = midsAttaches();
    EXPECT_CALL(*mock, getMimes())
            .WillOnce(Return(std::make_pair(mail_errors::error_code(), midsWithMimes)));

    EXPECT_CALL(*mock, messageAccess(_))
            .Times(2)
            .WillRepeatedly(Return(messageAccess));

    const std::string one = "one";
    const std::string two = "two";

    EXPECT_CALL(*messageAccess, getWhole())
            .WillOnce(Return(one))
            .WillOnce(Return(two));

    EXPECT_CALL(*rfcMessage, addRfc822Part(one))
            .Times(1);
    EXPECT_CALL(*rfcMessage, addRfc822Part(two))
            .Times(1);

    EXPECT_EQ(mock->apply(*rfcMessage, totalAttachmentSize), ComposeResult::DONE);
    EXPECT_EQ(totalAttachmentSize, one.size() + two.size());
}

}
