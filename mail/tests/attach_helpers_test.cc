#include <boost/optional/optional_io.hpp>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/content_type_mock.h>
#include "headers/mock_rfc_message.h"
#include <mail/sendbernar/composer/include/attach_helpers.h>
#include <mail/sendbernar/composer/tests/mock/mail_getter.h>
#include <mail_getter/SimpleAttachment.h>

using namespace testing;
using namespace sendbernar::tests;

namespace MimeParser {

inline bool operator==(const Hid& h1, const Hid& h2) {
    return h1.toString() == h2.toString();
}

}

namespace sendbernar {
namespace compose {
inline bool operator==(const Attachment& a, const Attachment& b) {
    return a.hid_ == b.hid_ && a.oldHid_ == b.oldHid_ && a.contentType_ == b.contentType_ &&
            a.fileName_ == b.fileName_&& a.id_ == b.id_ && a.size_ == b.size_ && a.hash_ == b.hash_;
}
}

struct AttachHelpersTest: public Test {
    mail_getter::attach_sid::KeyContainer keyContainer;
    std::shared_ptr<StrictMock<ContentTypeDetectorMock>> detector;
    std::shared_ptr<StrictMock<MockRfcMessage>> rfcMessage;
    std::shared_ptr<StrictMock<MockRecognizerWrapper>> recognizer;
    std::size_t totalAttachmentsSize = 0;
    std::vector<compose::Attachment> nonEmptyAttachments;
    std::vector<macs::Stid> sids;
    mail_getter::AttachmentStorage::VectorOfAttachments storageResponse;

    const std::string textPlain = "text/plain";
    const std::string defaultType = MimeType::DEFAULT_TYPE + MimeType::DELIMITER + MimeType::DEFAULT_SUBTYPE;
    const MimeType textPlainType = MimeType("text", "plain");

    void SetUp() override {
        detector = std::make_shared<StrictMock<ContentTypeDetectorMock>>();
        rfcMessage = std::make_shared<StrictMock<MockRfcMessage>>();
        recognizer = std::make_shared<StrictMock<MockRecognizerWrapper>>();
        totalAttachmentsSize = 0;
        sids = std::vector<macs::Stid>{"1", "2"};
        nonEmptyAttachments = {
            compose::Attachment{
                .size_ = 1
            }, compose::Attachment{
                .size_ = 2
            }
        };
        storageResponse = {
            std::make_shared<mail_getter::SimpleAttachment>(textPlain, textPlain, "body1"),
            std::make_shared<mail_getter::SimpleAttachment>(defaultType, defaultType, "body2"),
        };
    }
};

struct AddAttachmentToMessageTest: public AttachHelpersTest {
    const std::string filenameWithDirname = "foo/bar";
    const std::string filenameWithoutDirname = "bar";
    const std::string rfc822 = "message/rfc822";
    const std::string multipart = "multipart/anything";
    const std::string body = "body";

    AttachHelpers helpers() const {
        return AttachHelpers(*detector, *recognizer, storageResponse, {"", ""});
    }
};

struct MockAttachHelpers: public AttachHelpers {
    using AttachHelpers::AttachHelpers;

    MOCK_METHOD((std::tuple<MimeParser::Hid, std::string, std::string>), addAttachmentToMessage, (RfcMessage&,
                                                                                                     std::string,
                                                                                                     std::string,
                                                                                                     const std::string&), (const, override));
};

struct GetStorageAttachmentsTest: public AttachHelpersTest {
    std::shared_ptr<StrictMock<MockAttachHelpers>> helpers(const std::vector<std::string>& sids) const {
        return std::make_shared<StrictMock<MockAttachHelpers>>(*detector, *recognizer, storageResponse, sids);
    }
};

struct IntegrationGetStorageAttachmentsTest: public GetStorageAttachmentsTest { };

TEST_F(AddAttachmentToMessageTest, shouldTruncateDirnameFromFilename) {
    EXPECT_CALL(*rfcMessage, addBase64File(_, _, _))
            .WillOnce(Return(MimeParser::Hid()));

    EXPECT_EQ(helpers().addAttachmentToMessage(*rfcMessage, filenameWithDirname, textPlain, ""),
              std::make_tuple(MimeParser::Hid(), filenameWithoutDirname, textPlain));
}

TEST_F(AddAttachmentToMessageTest, shouldDetectTypeIfItIsSetToDefault) {
    EXPECT_CALL(*rfcMessage, addBase64File(_, _, _))
            .WillOnce(Return(MimeParser::Hid()));

    EXPECT_CALL(*detector, detect(filenameWithoutDirname, body))
            .WillOnce(Return(textPlainType));

    EXPECT_EQ(helpers().addAttachmentToMessage(*rfcMessage, filenameWithoutDirname, defaultType, body),
              std::make_tuple(MimeParser::Hid(), filenameWithoutDirname, textPlain));
}

TEST_F(AddAttachmentToMessageTest, shouldAdjustMessageRfc822Type) {
    EXPECT_CALL(*rfcMessage, addBase64File(_, _, _))
            .WillOnce(Return(MimeParser::Hid()));

    EXPECT_EQ(helpers().addAttachmentToMessage(*rfcMessage, filenameWithoutDirname, rfc822, body),
              std::make_tuple(MimeParser::Hid(), filenameWithoutDirname, defaultType));
}

TEST_F(AddAttachmentToMessageTest, shouldAdjustMultipartType) {
    EXPECT_CALL(*rfcMessage, addBase64File(_, _, _))
            .WillOnce(Return(MimeParser::Hid()));

    EXPECT_EQ(helpers().addAttachmentToMessage(*rfcMessage, filenameWithoutDirname, multipart, body),
              std::make_tuple(MimeParser::Hid(), filenameWithoutDirname, defaultType));
}

TEST_F(AddAttachmentToMessageTest, shouldReturnHid) {
    EXPECT_CALL(*rfcMessage, addBase64File(textPlainType, body, filenameWithoutDirname))
            .WillOnce(Return(MimeParser::Hid("1.2")));

    EXPECT_EQ(helpers().addAttachmentToMessage(*rfcMessage, filenameWithoutDirname, textPlain, body),
              std::make_tuple(MimeParser::Hid("1.2"), filenameWithoutDirname, textPlain));
}

TEST_F(GetStorageAttachmentsTest, shouldThrowAnExceptionIfSizeOfIdsAndSizeOfStorageAttachmentsAreDifferent) {
    EXPECT_THROW(helpers({"1"})->getStorageAttachments(*rfcMessage), std::runtime_error);
}

TEST_F(GetStorageAttachmentsTest, shouldDetectTypeIfItIsSetToDefault) {
    const auto mock = helpers({"1", "2"});
    EXPECT_CALL(*mock, addAttachmentToMessage(_, _, textPlain, _))
            .Times(2)
            .WillRepeatedly(Return(std::make_tuple(MimeParser::Hid("1.1"), textPlain, textPlain)));

    EXPECT_CALL(*detector, detect(defaultType, _))
            .WillOnce(Return(textPlainType));

    mock->getStorageAttachments(*rfcMessage);
}

TEST_F(IntegrationGetStorageAttachmentsTest, shouldReturnAttachments) {
    const auto mock = helpers({"1", "2"});

    EXPECT_CALL(*mock, addAttachmentToMessage(_, textPlain, textPlain, "body1"))
            .WillOnce(Return(std::make_tuple(MimeParser::Hid("1.1"), textPlain, textPlain)));
    EXPECT_CALL(*mock, addAttachmentToMessage(_, defaultType, textPlain, "body2"))
            .WillOnce(Return(std::make_tuple(MimeParser::Hid("1.2"), defaultType, textPlain)));

    EXPECT_CALL(*detector, detect(defaultType, _))
            .WillOnce(Return(textPlainType));

    const std::vector<compose::Attachment> expectedElements = {
        compose::Attachment{
            .hid_ = "1.1",
            .oldHid_ = "",
            .contentType_ = textPlain,
            .fileName_ = textPlain,
            .id_ = "1",
            .size_ = 5,
            .hash_ = "bXzxPdiq2xH5PgQkzOMjwlIhLF3vVHBmTVt9omEX3mMR67rGafv54K//nHbI0rwKo/ZTnGPTbyPBgv/RW4XwTw=="
        }, compose::Attachment{
            .hid_ = "1.2",
            .oldHid_ = "",
            .contentType_ = textPlain,
            .fileName_ = defaultType,
            .id_ = "2",
            .size_ = 5,
            .hash_ = "uJtDflpQfl+ad4YvhAMkFjYAKEMyedN6NsdXQrogeG4+oi2qV5qx4S9dcYBFzE4m6q8nrdqgZjPy0LWs7I3Vmg=="
        }
    };

    EXPECT_THAT(mock->getStorageAttachments(*rfcMessage),
                ElementsAreArray(expectedElements));
}



}
