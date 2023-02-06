#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/content_type_mock.h>
#include "headers/mock_rfc_message.h"
#include <mail/sendbernar/composer/include/sids_attachments.h>
#include <mail/sendbernar/composer/tests/mock/mail_getter.h>
#include <mail_getter/SimpleAttachment.h>

using namespace testing;
using namespace sendbernar::tests;

namespace sendbernar {
namespace compose {
inline bool operator==(const compose::Attachment& a, const compose::Attachment& b) {
    return a.hid_ == b.hid_ && a.oldHid_ == b.oldHid_ && a.contentType_ == b.contentType_ &&
            a.fileName_ == b.fileName_&& a.id_ == b.id_ && a.size_ == b.size_ && a.hash_ == b.hash_;
}
}

struct TestSidsAttachments: public SidsAttachments {
    using SidsAttachments::SidsAttachments;

    MOCK_METHOD(std::vector<compose::Attachment>, getStorageAttachments, (RfcMessage&, const VectorOfAttachments&), (const, override));
    MOCK_METHOD((std::pair<int, VectorOfAttachments>), getIds, (const std::vector<macs::Stid>&), (const, override));
    MOCK_METHOD(macs::Stid, decryptId, (const std::string&), (const, override));
};

struct SidsAttachmentsTest: public Test {
    mail_getter::attach_sid::KeyContainer keyContainer;
    std::shared_ptr<StrictMock<ContentTypeDetectorMock>> detector;
    std::shared_ptr<StrictMock<MockRfcMessage>> rfcMessage;
    std::shared_ptr<StrictMock<MockRecognizerWrapper>> recognizer;
    std::size_t totalAttachmentsSize = 0;
    std::vector<compose::Attachment> allAttachments;
    std::vector<compose::Attachment> nonEmptyAttachments;
    std::vector<std::string> stids;
    std::vector<macs::Stid> sids;
    const int storageSuccessCode = 0;
    const int storageErrorCode = 1;
    SidsAttachments::VectorOfAttachments storageResponse;
    SidsAttachments::VectorOfAttachments emptyStorageResponse;

    void SetUp() override {
        detector = std::make_shared<StrictMock<ContentTypeDetectorMock>>();
        rfcMessage = std::make_shared<StrictMock<MockRfcMessage>>();
        recognizer = std::make_shared<StrictMock<MockRecognizerWrapper>>();
        totalAttachmentsSize = 0;
        allAttachments.clear();
        sids = std::vector<macs::Stid>{"1", "2", "3"};
        stids = std::vector<std::string>{"stid1", "stid2", "stid3"};
        nonEmptyAttachments = {
            compose::Attachment{
                .size_ = 1
            }, compose::Attachment{
                .size_ = 2
            }, compose::Attachment{
                .size_ = 3
            }
        };

        storageResponse = {
            std::make_shared<mail_getter::SimpleAttachment>("", "", ""),
            std::make_shared<mail_getter::SimpleAttachment>("", "", ""),
            std::make_shared<mail_getter::SimpleAttachment>("", "", ""),
        };
    }

    std::shared_ptr<StrictMock<TestSidsAttachments>> emptySids() const {
        return std::make_shared<StrictMock<TestSidsAttachments>>(params::SendMessage{}, keyContainer, nullptr,
                                                                 *detector, *recognizer,
                                                                 getContextLogger("", boost::none));
    }

    std::shared_ptr<StrictMock<TestSidsAttachments>> threeSids() const {
        params::SendMessage p {
            .attaches = params::Attaches {
                .uploaded_attach_stids = boost::make_optional(sids)
            }
        };

        return std::make_shared<StrictMock<TestSidsAttachments>>(p, keyContainer, nullptr, *detector, *recognizer,
                                                                 getContextLogger("", boost::none));
    }

    void expectNormalSids(std::shared_ptr<StrictMock<TestSidsAttachments>>& h) {
        for (std::size_t i = 0; i < sids.size(); i++) {
            EXPECT_CALL(*h, decryptId(sids[i]))
                    .WillOnce(Return(stids[i]));
        }
    }
};

TEST_F(SidsAttachmentsTest, shouldReturnDoneInCaseOfEmptySids) {
    EXPECT_EQ(emptySids()->apply(*rfcMessage, allAttachments, totalAttachmentsSize), ComposeResult::DONE);
}

TEST_F(SidsAttachmentsTest, shouldReturnAttachmentStorageErrorInCaseOfAnyException) {
    auto mockAttachments = threeSids();
    EXPECT_CALL(*mockAttachments, decryptId(sids[0]))
            .WillOnce(Return("stid"));
    EXPECT_CALL(*mockAttachments, decryptId(sids[1]))
            .WillOnce(Invoke([] (const std::string&) -> std::string {
        throw std::runtime_error("");
    }));

    EXPECT_EQ(mockAttachments->apply(*rfcMessage, allAttachments, totalAttachmentsSize), ComposeResult::ATTACHMENT_STORAGE_ERROR);
}

TEST_F(SidsAttachmentsTest, shouldReturnAttachmentStorageErrorInCaseOfErrorFromStorage) {
    auto mockAttachments = threeSids();
    expectNormalSids(mockAttachments);

    EXPECT_CALL(*mockAttachments, getIds(stids))
            .WillOnce(Return(std::make_pair(storageErrorCode, emptyStorageResponse)));

    EXPECT_EQ(mockAttachments->apply(*rfcMessage, allAttachments, totalAttachmentsSize), ComposeResult::ATTACHMENT_STORAGE_ERROR);
}

TEST_F(SidsAttachmentsTest, shouldReturnAttachmentStorageErrorInCaseOfDifferentSizeOfSidsAndStorageResponse) {
    auto mockAttachments = threeSids();
    expectNormalSids(mockAttachments);

    EXPECT_CALL(*mockAttachments, getIds(stids))
            .WillOnce(Return(std::make_pair(storageSuccessCode, emptyStorageResponse)));

    EXPECT_EQ(mockAttachments->apply(*rfcMessage, allAttachments, totalAttachmentsSize), ComposeResult::ATTACHMENT_STORAGE_ERROR);
}

TEST_F(SidsAttachmentsTest, shouldReturnDoneAndAttachmentsAndSize) {
    auto mockAttachments = threeSids();
    expectNormalSids(mockAttachments);

    EXPECT_CALL(*mockAttachments, getIds(stids))
            .WillOnce(Return(std::make_pair(storageSuccessCode, storageResponse)));
    EXPECT_CALL(*mockAttachments, getStorageAttachments(_, storageResponse))
            .WillOnce(Return(nonEmptyAttachments));

    EXPECT_EQ(mockAttachments->apply(*rfcMessage, allAttachments, totalAttachmentsSize), ComposeResult::DONE);
    EXPECT_THAT(allAttachments, UnorderedElementsAreArray(nonEmptyAttachments));
    EXPECT_EQ(totalAttachmentsSize, 6ul);
}


}
