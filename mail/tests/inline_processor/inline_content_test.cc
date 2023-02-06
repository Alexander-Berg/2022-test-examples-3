#include "inline_processor_test.h"

namespace sendbernar::tests {

struct InlineContentTest: public Test, public BaseData, public InlineContentData {
    void SetUp() override {
        InlineContentData::init();
        BaseData::init();

        handlers = {
            inlineContent
        };
        body = "|yandex_inline_content_stid_hid_mid|";
    }
};

TEST(InlineContentParseIdTest, shouldParseId) {
    std::string stid, hid, mid;
    EXPECT_TRUE(InlineContent::parseId("stid_hid_mid", stid, hid, mid));
    EXPECT_EQ(stid, "stid");
    EXPECT_EQ(hid, "hid");
    EXPECT_EQ(mid, "mid");
}

TEST(InlineContentParseIdTest, shouldNotParseIdIfThereAreNotThreeParts) {
    std::string stid, hid, mid;
    EXPECT_FALSE(InlineContent::parseId("stid_hid", stid, hid, mid));
    EXPECT_TRUE(stid.empty());
    EXPECT_TRUE(hid.empty());
    EXPECT_TRUE(mid.empty());

    EXPECT_FALSE(InlineContent::parseId("stid_hid_mid_uid", stid, hid, mid));
    EXPECT_TRUE(stid.empty());
    EXPECT_TRUE(hid.empty());
    EXPECT_TRUE(mid.empty());
}

TEST(InlineContentParseIdTest, shouldNotParseWithEmptyParts) {
    std::string stid, hid, mid;
    EXPECT_FALSE(InlineContent::parseId("stid__mid", stid, hid, mid));
    EXPECT_TRUE(stid.empty());
    EXPECT_TRUE(hid.empty());
    EXPECT_TRUE(mid.empty());
}

TEST_F(InlineContentTest, shouldReturnFalseInCaseOfCurrentStidIsInInvalidStidsList) {
    invalidStids.push_back("stid");
    EXPECT_FALSE(InlineContent::isValidStid("stid", "mid", validStids, invalidStids, metadata));
}

TEST_F(InlineContentTest, shouldReturnTrueInCaseOfCurrentStidIsInValidStidsList) {
    validStids.push_back("stid");
    EXPECT_TRUE(InlineContent::isValidStid("stid", "mid", validStids, invalidStids, metadata));
}

TEST_F(InlineContentTest, shouldReturnFalseIfThereIsAMessageWithMidAndUnequalStid) {
    EXPECT_CALL(*metadata, getById("mid"))
            .WillOnce(Return(envelopeWithStid("stid_")));

    EXPECT_FALSE(InlineContent::isValidStid("stid", "mid", validStids, invalidStids, metadata));
    EXPECT_TRUE(validStids.empty());
    EXPECT_THAT(invalidStids, UnorderedElementsAre("stid"));

}

TEST_F(InlineContentTest, shouldReturnTrueIfThereIsAMessageWithMidAndEqualStid) {
    EXPECT_CALL(*metadata, getById("mid"))
            .WillOnce(Return(envelopeWithStid("stid")));

    EXPECT_TRUE(InlineContent::isValidStid("stid", "mid", validStids, invalidStids, metadata));
    EXPECT_THAT(validStids, UnorderedElementsAre("stid"));
    EXPECT_TRUE(invalidStids.empty());
}

TEST_F(InlineContentTest, shouldReturnFalseIfThereIsASystemErrorIsThrown) {
    EXPECT_CALL(*metadata, getById("mid"))
            .WillOnce(Invoke([](const std::string&) -> macs::Envelope {
        throw macs::system_error(mail_errors::error_code());
    }));

    EXPECT_FALSE(InlineContent::isValidStid("stid", "mid", validStids, invalidStids, metadata));
    EXPECT_THAT(invalidStids, UnorderedElementsAre("stid"));
    EXPECT_TRUE(validStids.empty());
}

TEST_F(InlineContentTest, shouldPassAnException) {
    EXPECT_CALL(*metadata, getById("mid"))
            .WillOnce(Invoke([](const std::string&) -> macs::Envelope {
        throw std::runtime_error("");
    }));

    EXPECT_THROW(InlineContent::isValidStid("stid", "mid", validStids, invalidStids, metadata), std::runtime_error);
    EXPECT_TRUE(invalidStids.empty());
    EXPECT_TRUE(validStids.empty());
}

TEST(InlineContentTest_, shouldChoseNonemptyName) {
    struct MockFile {
        std::string fileName_, name_;

        std::string fileName() const {
            return fileName_;
        }

        std::string name() const {
            return name_;
        }
    };

    EXPECT_EQ(InlineContent::getFilename<MockFile>(MockFile{"fileName", "name"}), "fileName");
    EXPECT_EQ(InlineContent::getFilename<MockFile>(MockFile{"", "name"}), "name");
}

TEST_F(InlineContentTest, shouldCreateNewMessageAccess) {
    EXPECT_CALL(*storageService, createMessageAccess(Matcher<mail_getter::MessageAccessParams>(messageAccessParams), _, _))
            .WillOnce(Return(nullptr));

    EXPECT_CALL(*metadata, getMessageAccessParams(_))
            .WillOnce(Return(messageAccessParams));

    EXPECT_EQ(InlineContent::getMessageAccess("mid", messages, metadata, storageService, *recognizer), nullptr);
    EXPECT_EQ(messages.size(), 1ul);
}

TEST_F(InlineContentTest, shouldUseExistingMessageAccess) {
    messages.emplace("mid", nullptr);

    EXPECT_EQ(InlineContent::getMessageAccess("mid", messages, metadata, storageService, *recognizer), nullptr);
    EXPECT_EQ(messages.size(), 1ul);
}

TEST_F(InlineContentTest, shouldNotProcessAttachInCaseOfInvalidId) {
    body = "|yandex_inline_content_stid_hid|";
    const mail_getter::SanitizerParsedResponse resp = {
        body,
        {
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                              boost::make_optional<std::string>(body.substr(1, body.size()-2)),
                                              mail_getter::MarkupPosition{0, 36, 1, 34}}
        }
    };

    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(resp));

    const auto [attachments, html] = *inlineProcessor->process(body, requestId());

    ASSERT_TRUE(attachments.empty());
    ASSERT_EQ(html, "");
}

TEST_F(InlineContentTest, shouldNotProcessAttachInCaseOfInvalidStid) {
    const mail_getter::SanitizerParsedResponse resp = {
        body,
        {
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                              boost::make_optional<std::string>(body.substr(1, body.size()-2)),
                                              mail_getter::MarkupPosition{0, 36, 1, 34}}
        }
    };

    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(resp));

    EXPECT_CALL(*metadata, getById(_))
            .WillOnce(Invoke([](const std::string&) -> macs::Envelope {
        throw std::runtime_error("");
    }));

    const auto [attachments, html] = *inlineProcessor->process(body, requestId());

    ASSERT_TRUE(attachments.empty());
    ASSERT_EQ(html, "");
}

TEST_F(InlineContentTest, shouldProcessAttachment) {
    const mail_getter::SanitizerParsedResponse resp = {
        body,
        {
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                              boost::make_optional<std::string>(body.substr(1, body.size()-2)),
                                              mail_getter::MarkupPosition{0, 36, 1, 34}}
        }
    };

    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(resp));

    EXPECT_CALL(*messageAccess, getBody(_))
            .WillOnce(Return("content_body"));

    EXPECT_CALL(*messageAccess, getHeaderStruct(_))
            .WillOnce(Return(macs::MimePartFactory().name("name").cid("cid").release()));

    EXPECT_CALL(*storageService, createMessageAccess(Matcher<mail_getter::MessageAccessParams>(messageAccessParams), _, _))
            .WillOnce(Return(messageAccess));

    EXPECT_CALL(*metadata, getMessageAccessParams(_))
            .WillOnce(Return(messageAccessParams));

    EXPECT_CALL(*metadata, getById("mid"))
            .WillOnce(Return(envelopeWithStid("stid")));


    const auto [attachments, html] = *inlineProcessor->process(body, requestId());

    EXPECT_THAT(attachments, UnorderedElementsAre(RemoteAttachment{
       "content_body", "base64",
       "name", "inline",
       "cid", "",
       "http://host/message_part/name?hid=hid&amp;mid=stid&amp;name=name"
    }));

    ASSERT_EQ(html, "|cid:cid|"s);
}

}
