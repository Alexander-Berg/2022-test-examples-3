#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/content_type_mock.h>
#include "headers/mock_rfc_message.h"
#include <mail/sendbernar/composer/include/parts_json.h>
#include <mail/sendbernar/composer/tests/mock/mail_getter.h>
#include <mail_getter/SimpleAttachment.h>
#include <mail/sendbernar/core/include/logger.h>
#include <macs/mime_part_factory.h>
#include <boost/optional/optional_io.hpp>

using namespace testing;
using namespace sendbernar::tests;

namespace MimeParser {
inline bool operator==(const Hid& h1, const Hid& h2) {
    return h1.toString() == h2.toString();
}
}

namespace macs {
inline bool operator==(const macs::MimePart& a, const macs::MimePart& b) {
    return a.hid() == b.hid() && a.contentType() == b.contentType() && a.contentSubtype() == b.contentSubtype() &&
           a.boundary() == b.boundary() && a.name() == b.name() && a.charset() == b.charset() && a.encoding() == b.encoding() &&
           a.contentDisposition() == b.contentDisposition() && a.fileName() == b.fileName() && a.cid() == b.cid() &&
           a.offsetBegin() == b.offsetBegin() && a.offsetEnd() == b.offsetEnd();
}
}

namespace sendbernar {
namespace compose {
inline bool operator==(const Attachment& a, const Attachment& b) {
    return a.hid_ == b.hid_ && a.oldHid_ == b.oldHid_ && a.contentType_ == b.contentType_ &&
           a.fileName_ == b.fileName_&& a.id_ == b.id_ && a.size_ == b.size_ && a.hash_ == b.hash_;
}

inline std::ostream& operator<<(std::ostream& out, const Attachment& a) {
    out << "hid: " << a.hid_ << " oldHid: " << a.oldHid_ << " contentType: " << a.contentType_
        << " fileName: " << a.fileName_ << " id: " << a.id_ << " size: " << a.size_ << " hash: " << a.hash_;
    return out;
}
}

struct TestPartsJsonAttachments: public PartsJsonAttachments {
    using PartsJsonAttachments::PartsJsonAttachments;

    MOCK_METHOD(MessageAccessPtr, messageAccess, (const macs::Mid&), (const, override));
    MOCK_METHOD(boost::optional<compose::Attachment>, getPartAttachment, (MessageAccessPtr, RfcMessage&,
                                                                                        const std::string&,
                                                                                        const RemoteAttachments&), (const, override));
};

struct TestPartsJsonAttachmentsWithMockedAddMessageAttachments: public TestPartsJsonAttachments {
    using TestPartsJsonAttachments::TestPartsJsonAttachments;

    MOCK_METHOD(CachedComposeResult, addMessageAttachments, (RfcMessage&, std::vector<compose::Attachment>&,
                                                                  std::size_t&, const std::string&, const std::vector<std::string>&,
                                                                  const std::vector<RemoteAttachment>&), (const, override));
};

struct PartsJsonAttachmentsTest: public Test {
    std::shared_ptr<StrictMock<ContentTypeDetectorMock>> detector;
    std::shared_ptr<StrictMock<MockRfcMessage>> rfcMessage;
    std::shared_ptr<StrictMock<MockRecognizerWrapper>> recognizer;
    const std::size_t attachmentsMaxSize = 10;

    void SetUp() override {
        detector = std::make_shared<StrictMock<ContentTypeDetectorMock>>();
        rfcMessage = std::make_shared<StrictMock<MockRfcMessage>>();
        recognizer = std::make_shared<StrictMock<MockRecognizerWrapper>>();
    }

    template<class TestClass>
    std::shared_ptr<TestClass> partsJson() const {
        params::SendMessage p {
            .attaches = params::Attaches {
                .parts_json = boost::make_optional(std::vector<params::IdetifiableMessagePart>{
                    params::IdetifiableMessagePart("mid1", "1.1"),
                    params::IdetifiableMessagePart("mid1", "1.2"),
                    params::IdetifiableMessagePart("mid2", "1.1")
                })
            }
        };

        return std::make_shared<TestClass>(p, nullptr, *detector, *recognizer, nullptr, attachmentsMaxSize,
                                           getContextLogger("", boost::none));
    }
};

struct GetPartTest: public PartsJsonAttachmentsTest {
    std::shared_ptr<StrictMock<MockMessageAccess>> messageAccess;
    const std::string oldHid = "1";

    void SetUp() override {
        PartsJsonAttachmentsTest::SetUp();
        messageAccess = std::make_shared<StrictMock<MockMessageAccess>>();
    }
};

struct AddMessageAttachmentsTest: public PartsJsonAttachmentsTest {
    std::vector<compose::Attachment> attachments;
    std::size_t totalAttachmentsSize = 0;

    void SetUp() override {
        PartsJsonAttachmentsTest::SetUp();
        totalAttachmentsSize = 0;
    }
};

struct PartsJsonApplyTest: public AddMessageAttachmentsTest { };


TEST_F(PartsJsonAttachmentsTest, shouldChoseAttrFilenameIfItIsNotEmpty) {
    EXPECT_EQ(TestPartsJsonAttachments::nameOrFilename("name", "filename"), "filename");
}

TEST_F(PartsJsonAttachmentsTest, shouldChoseAttrNameIfFilenameIsEmpty) {
    EXPECT_EQ(TestPartsJsonAttachments::nameOrFilename("name", ""), "name");
}

TEST_F(GetPartTest, shouldReturnEmptyAttachmentIfTheCidIsDuplicated) {
    const RemoteAttachments attLst {
        RemoteAttachment {
            .cid="cid1"
        }, RemoteAttachment {
            .cid="cid2"
        }, RemoteAttachment{
            .cid="cid3"
        }
    };

    EXPECT_CALL(*messageAccess, getHeaderStruct(oldHid))
            .WillOnce(Return(macs::MimePartFactory().cid("cid2").release()));

    EXPECT_EQ(partsJson<PartsJsonAttachments>()->getPartAttachment(messageAccess, *rfcMessage, oldHid, attLst), boost::none);
}

TEST_F(GetPartTest, shouldReturnAttachment) {
    const RemoteAttachments attLst {
        RemoteAttachment {
            .cid="cid1"
        }
    };
    
    const std::string filename = "filename";
    const std::string content = "body";
    const std::string hid = "1.3";
    const MimeType mimeType("text", "plain");
    const MetaPart attr = macs::MimePartFactory().cid("cid2").name("name").fileName(filename).release();

    EXPECT_CALL(*rfcMessage, addHid(mimeType, attr, content, filename))
            .WillOnce(Return(MimeParser::Hid(hid)));

    EXPECT_CALL(*messageAccess, getHeaderStruct(oldHid))
            .WillOnce(Return(attr));
    EXPECT_CALL(*messageAccess, getBody(oldHid))
            .WillOnce(Return(content));

    EXPECT_CALL(*detector, detect(filename, content))
            .WillOnce(Return(mimeType));

    compose::Attachment result {
        .hid_ = hid,
        .oldHid_ = oldHid,
        .contentType_ = mimeType.toString(),
        .fileName_ = filename,
        .size_ = content.size(),
        .hash_ = "h0w4rqbhSqHKkChFHBeK47SC4OyZgfzpO4E/LtYyighzpmlElrCi2zEjDqqq3jLrrwTX8T11zD4LZJ5gjLe7uA=="
    };
    EXPECT_EQ(*partsJson<PartsJsonAttachments>()->getPartAttachment(messageAccess, *rfcMessage, oldHid, attLst), result);
}

TEST_F(AddMessageAttachmentsTest, shouldAddAttachments) {
    const auto mock = partsJson<TestPartsJsonAttachments>();

    const std::vector<compose::Attachment> expected {
        compose::Attachment {
            .hid_ = "1.1",
            .size_ = 1,
        }, compose::Attachment {
            .hid_ = "1.2",
            .size_ = 1,
        }, compose::Attachment {
            .hid_ = "1.3",
            .size_ = 1,
        }
    };

    EXPECT_CALL(*mock, messageAccess(_))
            .WillOnce(Return(nullptr));

    MessageAccessPtr ma;

    EXPECT_CALL(*mock, getPartAttachment(ma, _, _, _))
            .WillOnce(Return(boost::make_optional(expected[0])))
            .WillOnce(Return(boost::make_optional(expected[1])))
            .WillOnce(Return(boost::make_optional(expected[2])));

    const std::vector<RemoteAttachment> attLst;

    EXPECT_EQ(mock->addMessageAttachments(*rfcMessage, attachments, totalAttachmentsSize,
                                       "mid", {"1.1", "1.2", "1.3"}, attLst),
              ComposeResult::DONE);
    EXPECT_EQ(totalAttachmentsSize, 3ul);
    EXPECT_THAT(attachments, ElementsAreArray(expected));
}

TEST_F(AddMessageAttachmentsTest, shouldReturnPartsJsonInvalidIfTheExceptionIsThrown) {
    const auto mock = partsJson<TestPartsJsonAttachments>();

    EXPECT_CALL(*mock, messageAccess(_))
            .WillOnce(Throw(std::runtime_error("")));

    EXPECT_EQ(mock->addMessageAttachments(*rfcMessage, attachments, totalAttachmentsSize, "", {}, {}),
              ComposeResult::PARTS_JSON_INVALID);
}

TEST_F(AddMessageAttachmentsTest, shouldReturnAttachmentTooBigIfTheLimitIsExceeded) {
    const auto mock = partsJson<TestPartsJsonAttachments>();

    const std::vector<compose::Attachment> expected {
        compose::Attachment {
            .hid_ = "1.1",
            .size_ = 1,
        }, compose::Attachment {
            .hid_ = "1.2",
            .size_ = 1,
        }, compose::Attachment {
            .hid_ = "1.3",
            .size_ = attachmentsMaxSize,
        }
    };

    EXPECT_CALL(*mock, messageAccess(_))
            .WillOnce(Return(nullptr));

    MessageAccessPtr ma;

    EXPECT_CALL(*mock, getPartAttachment(ma, _, _, _))
            .WillOnce(Return(boost::make_optional(expected[0])))
            .WillOnce(Return(boost::make_optional(expected[1])))
            .WillOnce(Return(boost::make_optional(expected[2])));

    const std::vector<RemoteAttachment> attLst;

    EXPECT_EQ(mock->addMessageAttachments(*rfcMessage, attachments, totalAttachmentsSize,
                                       "mid", {"1.1", "1.2", "1.3"}, attLst),
              ComposeResult::ATTACHMENT_TOO_BIG);
}

TEST_F(PartsJsonApplyTest, shouldReturnFirstMetError) {
    const auto mock = partsJson<TestPartsJsonAttachmentsWithMockedAddMessageAttachments>();

    EXPECT_CALL(*mock, addMessageAttachments(_, _, _, _, _, _))
            .WillOnce(Return(ComposeResult::DONE))
            .WillOnce(Return(ComposeResult::PARTS_JSON_INVALID));

    EXPECT_EQ(mock->apply(*rfcMessage, attachments, totalAttachmentsSize, {}),
              ComposeResult::PARTS_JSON_INVALID);
}


}
