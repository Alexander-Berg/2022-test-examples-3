#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "headers/mock_rfc_message.h"
#include <mail_getter/content_type_mock.h>
#include <mail/sendbernar/composer/include/body_and_attaches.h>
#include <mail/sendbernar/composer/tests/mock/mail_getter.h>

using namespace testing;
using namespace sendbernar::tests;

bool operator==(const RemoteAttachment& r1, const RemoteAttachment& r2) {
    return r1.body == r2.body && r1.encoding == r2.encoding && r1.name == r2.name &&
           r1.disposition == r2.disposition && r1.cid == r2.cid && r1.remote_path == r2.remote_path &&
           r1.original_url == r2.original_url;
}

namespace sendbernar {

std::ostream& operator<<(std::ostream& out, ContentTypeEncoding cte) {
    out << toString(cte);
    return out;
}

struct TestBodyAndAttaches: public BodyAndAttaches {
    using BodyAndAttaches::BodyAndAttaches;

    MOCK_METHOD(RfcMessagePtr, newMessage, (), (const, override));
};

DiskAttachBuilder builder() {
    return DiskAttachBuilder(DiskConfig{
        .host="host",
        .previewHost="previewHost"
    });
}

struct BodyAndAttachesTest: public Test {
    const std::size_t attachmentsMaxSize = 100;
    std::shared_ptr<StrictMock<ContentTypeDetectorMock>> detector;
    std::shared_ptr<StrictMock<MockRfcMessage>> rfcMessage;
    std::shared_ptr<StrictMock<MockRfcMessage>> subMessage;
    std::shared_ptr<StrictMock<MockRfcMessage>> attachMessage;
    std::shared_ptr<StrictMock<MockMultipartRelaxedMessage>> relaxedMessage;
    std::shared_ptr<StrictMock<MockRecognizerWrapper>> recognizer;
    std::size_t totalAttachmentSize = 0;
    RemoteAttachments remoteAtts;
    MimeType type;

    const std::string body8Bit = "восьмибитовое тело";
    const std::string body7Bit = "seven bit body";
    const std::string bodyEmpty = "";
    const std::string bodyBase64NonPrintable = "\a";
    const std::string attachesTitle = "Files on Yandex.Disk are attached to the email: \n";

    std::string addTitle(const std::string& body) {
        return BodyAndAttaches::attachesWithTitle(body, attachesTitle);
    }

    std::shared_ptr<StrictMock<TestBodyAndAttaches>> bodyAndAttaches() {
        return std::make_shared<StrictMock<TestBodyAndAttaches>>(params::SendMessage{}, attachmentsMaxSize, attachesTitle, *detector,
                                                                 builder(), *recognizer, getContextLogger("", boost::none));
    }

    std::shared_ptr<StrictMock<TestBodyAndAttaches>> bodyAndAttachesForce7Bit() {
        params::SendMessage params;
        params.message.force7bit = true;
        return std::make_shared<StrictMock<TestBodyAndAttaches>>(params, attachmentsMaxSize, attachesTitle, *detector,
                                                                 builder(), *recognizer, getContextLogger("", boost::none));
    }

    std::shared_ptr<StrictMock<TestBodyAndAttaches>> bodyAndAttaches8BitNarodAttach() {
        params::SendMessage params;
        params.attaches.disk_attaches = "русская кодировка";
        return std::make_shared<StrictMock<TestBodyAndAttaches>>(params, attachmentsMaxSize, attachesTitle, *detector,
                                                                 builder(), *recognizer, getContextLogger("", boost::none));
    }

    std::shared_ptr<StrictMock<TestBodyAndAttaches>> narodAttach(const std::string& narod) {
        params::SendMessage params;
        params.attaches.disk_attaches = narod;
        return std::make_shared<StrictMock<TestBodyAndAttaches>>(params, attachmentsMaxSize, attachesTitle, *detector,
                                                                 builder(), *recognizer, getContextLogger("", boost::none));
    }

    std::shared_ptr<StrictMock<TestBodyAndAttaches>> narodAttachWithForce7Bit(const std::string& narod) {
        params::SendMessage params;
        params.attaches.disk_attaches = narod;
        params.message.force7bit = true;
        return std::make_shared<StrictMock<TestBodyAndAttaches>>(params, attachmentsMaxSize, attachesTitle, *detector,
                                                                 builder(), *recognizer, getContextLogger("", boost::none));
    }

    void SetUp() override {
        detector = std::make_shared<StrictMock<ContentTypeDetectorMock>>();
        rfcMessage = std::make_shared<StrictMock<MockRfcMessage>>();
        subMessage = std::make_shared<StrictMock<MockRfcMessage>>();
        attachMessage = std::make_shared<StrictMock<MockRfcMessage>>();
        relaxedMessage = std::make_shared<StrictMock<MockMultipartRelaxedMessage>>();
        recognizer = std::make_shared<StrictMock<MockRecognizerWrapper>>();
        totalAttachmentSize = 0;
        type = MimeType("text", "plain");
        remoteAtts = RemoteAttachments();
    }
};

struct BodyAndAttachesSetBodyTest: public BodyAndAttachesTest { };
struct BodyAndAttachesSetNarodAttachTest: public BodyAndAttachesTest { };
struct BodyAndAttachesProcessAttachmentsTest: public BodyAndAttachesTest { };

TEST_F(BodyAndAttachesSetBodyTest, shouldSetUtf8TypeInCaseOfBodyHave8bit) {
    EXPECT_CALL(*rfcMessage, addBody(body8Bit, _, _)).Times(1);
    EXPECT_CALL(*rfcMessage, setUtf8()).Times(1);

    setBody(body8Bit, bodyEmpty, false, false, *rfcMessage);
}

TEST_F(BodyAndAttachesSetBodyTest, shouldSetUtf8TypeInCaseOfNarodHave8bit) {
    EXPECT_CALL(*rfcMessage, addBody(body7Bit, _, _)).Times(1);
    EXPECT_CALL(*rfcMessage, setUtf8()).Times(1);

    setBody(body7Bit, body8Bit, false, false, *rfcMessage);
}

TEST_F(BodyAndAttachesSetBodyTest, shouldSet7BitEncoding) {
    EXPECT_CALL(*rfcMessage, addBody(body7Bit, ContentTypeEncoding::SevenBit, false)).Times(1);
    setBody(body7Bit, bodyEmpty, false, false, *rfcMessage);
}

TEST_F(BodyAndAttachesSetBodyTest, shouldSet8BitEncoding) {
    EXPECT_CALL(*rfcMessage, setUtf8()).Times(1);
    EXPECT_CALL(*rfcMessage, addBody(body8Bit, ContentTypeEncoding::EightBit, false)).Times(1);
    setBody(body8Bit, bodyEmpty, false, false, *rfcMessage);
}

TEST_F(BodyAndAttachesSetBodyTest, shouldSetBase64Encoding) {
    EXPECT_CALL(*rfcMessage, addBody(bodyBase64NonPrintable, ContentTypeEncoding::Base64, false)).Times(1);
    setBody(bodyBase64NonPrintable, bodyEmpty, false, false, *rfcMessage);
}

TEST_F(BodyAndAttachesSetBodyTest, shouldSetBase64InsteadOn8BitIfThereIsASetFlag) {
    EXPECT_CALL(*rfcMessage, setUtf8()).Times(1);
    EXPECT_CALL(*rfcMessage, addBody(body8Bit, ContentTypeEncoding::Base64, false)).Times(1);
    setBody(body8Bit, bodyEmpty, true, false, *rfcMessage);;
}

TEST_F(BodyAndAttachesProcessAttachmentsTest, shouldAccumulateAttachmentsSizeAndReturnDoneInCaseOfEverythingIsOk) {
    remoteAtts = {
        RemoteAttachment{
            .body="1"
        }, RemoteAttachment{
            .body="12"
        }, RemoteAttachment{
            .body="123"
        },
    };

    EXPECT_CALL(*detector, detectByContent(remoteAtts[0].body)).WillOnce(Return(type));
    EXPECT_CALL(*detector, detectByContent(remoteAtts[1].body)).WillOnce(Return(type));
    EXPECT_CALL(*detector, detectByContent(remoteAtts[2].body)).WillOnce(Return(type));

    EXPECT_CALL(*relaxedMessage, addRemoteAttach(type, remoteAtts[0])).Times(1);
    EXPECT_CALL(*relaxedMessage, addRemoteAttach(type, remoteAtts[1])).Times(1);
    EXPECT_CALL(*relaxedMessage, addRemoteAttach(type, remoteAtts[2])).Times(1);

    auto mock = bodyAndAttaches();

    EXPECT_EQ(mock->processAttaches(*relaxedMessage, remoteAtts, totalAttachmentSize), ComposeResult::DONE);
    EXPECT_EQ(totalAttachmentSize, 6ul);
}

TEST_F(BodyAndAttachesProcessAttachmentsTest, shoultReturnAttachmentTooBigInCaseOfSpecialLimitIsExceeded) {
    remoteAtts = {
        RemoteAttachment{
            .body="1"
        }, RemoteAttachment{
            .body="12"
        }, RemoteAttachment{
            .body=std::string('~', attachmentsMaxSize + 1)
        },
    };
    std::size_t expectedSize = std::accumulate(remoteAtts.begin(), remoteAtts.end(), 0ul, [](std::size_t total, const auto& i) {
        return total + i.body.size();
    });

    EXPECT_CALL(*detector, detectByContent(remoteAtts[0].body)).WillOnce(Return(type));
    EXPECT_CALL(*detector, detectByContent(remoteAtts[1].body)).WillOnce(Return(type));

    EXPECT_CALL(*relaxedMessage, addRemoteAttach(type, remoteAtts[0])).Times(1);
    EXPECT_CALL(*relaxedMessage, addRemoteAttach(type, remoteAtts[1])).Times(1);

    auto mock = bodyAndAttaches();

    EXPECT_EQ(mock->processAttaches(*relaxedMessage, remoteAtts, totalAttachmentSize), ComposeResult::ATTACHMENT_TOO_BIG);
    EXPECT_EQ(totalAttachmentSize, expectedSize);
}

TEST_F(BodyAndAttachesSetNarodAttachTest, shouldSet7BitEncoding) {
    {
        auto mock = narodAttach(body7Bit);
        auto a = mock->narodAttach();

        EXPECT_CALL(*rfcMessage, addNarodAttach(addTitle(body7Bit), ContentTypeEncoding::SevenBit)).Times(1);
        mock->setNarodAttach(body7Bit, *rfcMessage);
    }

    {
        auto mock = narodAttach(body7Bit);

        EXPECT_CALL(*rfcMessage, addNarodAttach(addTitle(body7Bit), ContentTypeEncoding::SevenBit)).Times(1);
        mock->setNarodAttach(body8Bit, *rfcMessage);
    }
}

TEST_F(BodyAndAttachesSetNarodAttachTest, shouldSet8BitEncoding) {
    auto mock = narodAttach(body8Bit);

    EXPECT_CALL(*rfcMessage, addNarodAttach(addTitle(body8Bit), ContentTypeEncoding::EightBit)).Times(1);
    mock->setNarodAttach(body8Bit, *rfcMessage);
}

TEST_F(BodyAndAttachesSetNarodAttachTest, shouldSetBase64Encoding) {
    auto mock = narodAttach(bodyBase64NonPrintable);

    EXPECT_CALL(*rfcMessage, addNarodAttach(addTitle(bodyBase64NonPrintable), ContentTypeEncoding::Base64)).Times(1);
    mock->setNarodAttach(body7Bit, *rfcMessage);
}

TEST_F(BodyAndAttachesSetNarodAttachTest, shouldSetBase64InsteadOn8BitIfThereIsASetFlag) {
    auto mock = narodAttachWithForce7Bit(body8Bit);

    EXPECT_CALL(*rfcMessage, addNarodAttach(addTitle(body8Bit), ContentTypeEncoding::Base64)).Times(1);
    mock->setNarodAttach(body8Bit, *rfcMessage);
}

TEST_F(BodyAndAttachesTest, shouldThrowAnExceptionInCaseOfEmptyBodyAndNonemptyInlineAttaches) {
    remoteAtts = {
        RemoteAttachment {
            .body="1"
        }
    };

    EXPECT_THROW(bodyAndAttaches()->apply(*rfcMessage, bodyEmpty, remoteAtts, totalAttachmentSize), std::runtime_error);
}

TEST_F(BodyAndAttachesTest, shouldReturnDoneInCaseOfEmptyBody) {
    EXPECT_EQ(bodyAndAttaches()->apply(*rfcMessage, bodyEmpty, remoteAtts, totalAttachmentSize),
              ComposeResult::DONE);
}

TEST_F(BodyAndAttachesTest, shouldReturnDoneInCaseOfExistingAttaches) {
    remoteAtts = {
        RemoteAttachment {
            .body="1"
        }, RemoteAttachment {
            .body="12"
        }, RemoteAttachment {
            .body="123"
        },
    };

    auto mock = narodAttach(body8Bit);

    EXPECT_CALL(*mock, newMessage()).WillOnce(Return(subMessage)).WillOnce(Return(attachMessage));

    EXPECT_CALL(*subMessage, setUtf8()).Times(1);
    EXPECT_CALL(*subMessage, addBody(body8Bit, ContentTypeEncoding::EightBit, false)).Times(1);

    EXPECT_CALL(*attachMessage, addRelatedPart(Matcher<RfcMessagePtr>(subMessage))).WillOnce(Return(relaxedMessage));

    EXPECT_CALL(*detector, detectByContent(remoteAtts[0].body)).WillOnce(Return(type));
    EXPECT_CALL(*detector, detectByContent(remoteAtts[1].body)).WillOnce(Return(type));
    EXPECT_CALL(*detector, detectByContent(remoteAtts[2].body)).WillOnce(Return(type));

    EXPECT_CALL(*relaxedMessage, addRemoteAttach(type, remoteAtts[0])).Times(1);
    EXPECT_CALL(*relaxedMessage, addRemoteAttach(type, remoteAtts[1])).Times(1);
    EXPECT_CALL(*relaxedMessage, addRemoteAttach(type, remoteAtts[2])).Times(1);

    EXPECT_CALL(*rfcMessage, addPart(Matcher<RfcMessagePtr>(attachMessage))).Times(1);
    EXPECT_CALL(*rfcMessage, addNarodAttach(addTitle(body8Bit), ContentTypeEncoding::EightBit)).Times(1);

    EXPECT_EQ(mock->apply(*rfcMessage, body8Bit, remoteAtts, totalAttachmentSize),
              ComposeResult::DONE);
    EXPECT_EQ(totalAttachmentSize, 6ul);
}

TEST_F(BodyAndAttachesTest, shouldReturnDoneInCaseOfMissingAttaches) {
    auto mock = bodyAndAttaches();

    EXPECT_CALL(*mock, newMessage()).WillOnce(Return(subMessage));

    EXPECT_CALL(*subMessage, setUtf8()).Times(1);
    EXPECT_CALL(*subMessage, addBody(body8Bit, ContentTypeEncoding::EightBit, false)).Times(1);

    EXPECT_CALL(*rfcMessage, addPart(Matcher<RfcMessagePtr>(subMessage))).Times(1);

    EXPECT_EQ(mock->apply(*rfcMessage, body8Bit, remoteAtts, totalAttachmentSize),
              ComposeResult::DONE);
    EXPECT_EQ(totalAttachmentSize, 0ul);
}

TEST_F(BodyAndAttachesTest, shouldForwardErrorFromProcessAttaches) {
    remoteAtts = {
        RemoteAttachment{
            .body=std::string('~', attachmentsMaxSize + 1)
        }
    };

    auto mock = bodyAndAttaches();

    EXPECT_CALL(*mock, newMessage()).WillOnce(Return(subMessage)).WillOnce(Return(attachMessage));

    EXPECT_CALL(*subMessage, setUtf8()).Times(1);
    EXPECT_CALL(*subMessage, addBody(body8Bit, ContentTypeEncoding::EightBit, false)).Times(1);

    EXPECT_CALL(*attachMessage, addRelatedPart(Matcher<RfcMessagePtr>(subMessage))).WillOnce(Return(relaxedMessage));

    EXPECT_EQ(mock->apply(*rfcMessage, body8Bit, remoteAtts, totalAttachmentSize),
              ComposeResult::ATTACHMENT_TOO_BIG);
    EXPECT_EQ(totalAttachmentSize, remoteAtts[0].body.size());
}

TEST_F(BodyAndAttachesSetNarodAttachTest, shouldNotAddAttachTitleWhenEmptyBody) {
    auto mock = narodAttach(bodyEmpty);

    EXPECT_CALL(*rfcMessage, addNarodAttach(bodyEmpty, ContentTypeEncoding::SevenBit)).Times(1);
    mock->setNarodAttach(bodyEmpty, *rfcMessage);
}

}
