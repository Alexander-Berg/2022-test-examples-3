#include "inline_processor_test.h"

namespace sendbernar::tests {

static std::string prefix = "mock_class";

struct MockClassHandler: public InlineClassHandler {
    std::string getPrefix() override {
        return prefix;
    }

    MOCK_METHOD(int, getAttachment, (const std::string&, RemoteAttachment&, InlineHandlerAttributes&), (override));
};

boost::optional<std::string> sanitizerPrefix() {
    return boost::optional<std::string>(prefix);
}

mail_getter::SanitizerParsedResponse sanitizerResponse(const std::string& body, const std::string& id = "") {
    return {
        body,
        {
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                        boost::make_optional<std::string>(prefix + id),
                        mail_getter::MarkupPosition{0, 12, 1, 10}}
        }
    };
}

struct InlineProcessorTest: public Test
                          , public BaseData {
    void SetUp() override {
        BaseData::init();
        classHandler = std::make_shared<StrictMock<MockClassHandler>>();
        handlers = {
            classHandler
        };
    }

    std::shared_ptr<StrictMock<MockClassHandler>> classHandler;
};

TEST_F(InlineProcessorTest, shouldNotChangeIfThereAreNoProperClassHandlers) {
    const mail_getter::SanitizerParsedResponse resp = {
        body,
        {
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Image,
                                              boost::none, mail_getter::MarkupPosition{5, 22, 10, 16}},
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Link,
                                              boost::none, mail_getter::MarkupPosition{3, 29, 9, 22}},
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Cid,
                                              boost::none, mail_getter::MarkupPosition{5, 18, 10, 12}},
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                              boost::make_optional<std::string>("test_class"),
                                              mail_getter::MarkupPosition{5, 67, 10, 61}}
        }
    };

    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(resp));

    const auto [attachments, html] = *inlineProcessor->process(body, requestId());

    ASSERT_TRUE(attachments.empty());
    ASSERT_EQ(html, body);
}

TEST_F(InlineProcessorTest, shouldReplaceAttachent) {
    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(sanitizerResponse(body)));

    EXPECT_CALL(*classHandler, getAttachment(_, _, _))
            .WillOnce(Invoke([](const std::string&, RemoteAttachment& att, InlineHandlerAttributes&) {
        att = RemoteAttachment{
            "body", "encoding",
            "name", "disposition",
            "cid", "remote_path",
            "original_url"
        };
        return 1;
    }));

    const auto [attachments, html] = *inlineProcessor->process(body, requestId());

    EXPECT_THAT(attachments, UnorderedElementsAre(RemoteAttachment{
        "body", "encoding",
        "name", "disposition",
        "cid", "remote_path",
        "original_url"
    }));
    ASSERT_EQ(html, "|cid:cid|");
}

TEST_F(InlineProcessorTest, shouldEraseAttachmentInCaseOfError) {
    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(sanitizerResponse(body)));
    EXPECT_CALL(*classHandler, getAttachment(_, _, _))
            .WillOnce(Invoke([](const std::string&, RemoteAttachment&, InlineHandlerAttributes&) {
        return 0;
    }));

    const auto [attachments, html] = *inlineProcessor->process(body, requestId());

    ASSERT_TRUE(attachments.empty());
    ASSERT_EQ(html, "");
}

TEST_F(InlineProcessorTest, shouldEraseAttachmentInCaseOfException) {
    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(sanitizerResponse(body)));
    EXPECT_CALL(*classHandler, getAttachment(_, _, _))
            .WillOnce(Invoke([](const std::string&, RemoteAttachment&, InlineHandlerAttributes&) -> int {
        throw std::runtime_error("");
    }));

    const auto [attachments, html] = *inlineProcessor->process(body, requestId());

    ASSERT_TRUE(attachments.empty());
    ASSERT_EQ(html, "");
}

TEST_F(InlineProcessorTest, shouldPassPostfixAsClassId) {
    const std::string id = "_1";

    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(sanitizerResponse(body, id)));

    EXPECT_CALL(*classHandler, getAttachment(_, _, _))
            .WillOnce(Invoke([&](const std::string& classId, RemoteAttachment& att, InlineHandlerAttributes&) {
        EXPECT_EQ(classId, id);
        att.cid = classId;
        return 1;
    }));

    const auto [attachments, html] = *inlineProcessor->process(body, requestId());

    ASSERT_EQ(html, "|cid:"s + id + "|"s);
};

}
