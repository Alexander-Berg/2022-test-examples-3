#include "inline_processor_test.h"

namespace sendbernar::tests {

struct InlineFromVerstkaTest: public Test
                            , public BaseData
                            , public InlineFromVerstkaData {

    void SetUp() override {
        InlineFromVerstkaData::init();
        BaseData::init();

        handlers = {
            inlineFromVerstka
        };
        body = "|yandex_new_inline_stid|";
    }
};

TEST_F(InlineFromVerstkaTest, shouldReturnAttachment) {
    const mail_getter::SanitizerParsedResponse resp = {
        body,
        {
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                              boost::make_optional<std::string>(body.substr(1, body.size()-2)),
                                              mail_getter::MarkupPosition{0, 24, 1, 22}}
        }
    };

    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(resp));

    EXPECT_CALL(*storageService, createAttachmentStorage(""))
        .WillOnce(Return(attachmentStorage));

    EXPECT_CALL(*attachmentStorage, get(_, _, _))
            .WillOnce(Invoke([](const std::vector<std::string>&, mail_getter::AttachmentStorage::VectorOfAttachments& v, OptYieldContext) {
        v.push_back(std::make_shared<mail_getter::SimpleAttachment>("filename", "text/plain", "body", 0, 0));
        return 0;
    }));


    const auto [attachments, html] = *inlineProcessor->process(body, requestId());


    EXPECT_THAT(attachments, UnorderedElementsAre(RemoteAttachment{
       "body", "base64",
       "filename", "inline",
       "content_id", "",
       "http://host/message_part/filename?sid=stid&amp;name=filename"
    }));

    ASSERT_EQ(html, "|cid:content_id|"s);
}

}
