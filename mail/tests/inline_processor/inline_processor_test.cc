#include "inline_processor_test.h"

namespace sendbernar::tests {

struct AllHandlersTest: public Test
                      , public BaseData
                      , public SmilesData
                      , public InlineContentData
                      , public InlineFromVerstkaData {

    void SetUp() override {
        BaseData::init();
        SmilesData::init();
        InlineContentData::init();
        InlineFromVerstkaData::init();

        handlers = {
            inlineContent,
            smiles,
            inlineFromVerstka
        };
        body = "|yandex_smile_smile||yandex_inline_content_stid_hid_mid||yandex_new_inline_stid|";
    }
};

TEST_F(AllHandlersTest, shouldParseAllInlineAttaches) {
    const mail_getter::SanitizerParsedResponse resp = {
        body,
        {
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                              boost::make_optional<std::string>("yandex_smile_smile"),
                                              mail_getter::MarkupPosition{0, 20, 1, 18}},
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                              boost::make_optional<std::string>("yandex_inline_content_stid_hid_mid"),
                                              mail_getter::MarkupPosition{20, 36, 21, 34}},
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                              boost::make_optional<std::string>("yandex_new_inline_stid"),
                                              mail_getter::MarkupPosition{56, 24, 57, 22}}
        }
    };

    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(resp));

    EXPECT_CALL(*messageAccess, getBody(_))
            .WillOnce(Return("content_body"));

    EXPECT_CALL(*messageAccess, getHeaderStruct(_))
            .WillOnce(Return(macs::MimePartFactory().name("name").cid("content_with_cid").release()));

    EXPECT_CALL(*storageService, createMessageAccess(Matcher<mail_getter::MessageAccessParams>(messageAccessParams), _, _))
            .WillOnce(Return(messageAccess));

    EXPECT_CALL(*metadata, getMessageAccessParams(_))
            .WillOnce(Return(messageAccessParams));

    EXPECT_CALL(*metadata, getById("mid"))
            .WillOnce(Return(envelopeWithStid("stid")));

    const auto h = createTypedDummy(yhttp::response{.status=200, .body=smileBody});

    EXPECT_CALL(*storageService, createAttachmentStorage(""))
        .WillOnce(Return(attachmentStorage));

    EXPECT_CALL(*attachmentStorage, get(_, _, _))
            .WillOnce(Invoke([](const std::vector<std::string>&, mail_getter::AttachmentStorage::VectorOfAttachments& v, OptYieldContext) {
        v.push_back(std::make_shared<mail_getter::SimpleAttachment>("filename", "text/plain", "body", 0, 0));
        return 0;
    }));

    const auto [attachments, html] = *makeInlineProcessor(h)->process(body, requestId());

    EXPECT_THAT(attachments, UnorderedElementsAre(RemoteAttachment{
       "content_body", "base64",
       "name", "inline",
       "content_with_cid", "",
       "http://host/message_part/name?hid=hid&amp;mid=stid&amp;name=name"
    }, RemoteAttachment{
        smileBody, "base64",
        "image3", "inline",
        "smilesmile", smilesHost + fmt::format(endpoint.method(), fmt::arg("smile_id", "smile")),
        ""
    }, RemoteAttachment{
        "body", "base64",
        "filename", "inline",
        "content_id", "",
        "http://host/message_part/filename?sid=stid&amp;name=filename"
    }));
    EXPECT_THAT(html, "|cid:smilesmile||cid:content_with_cid||cid:content_id|");
}

}
