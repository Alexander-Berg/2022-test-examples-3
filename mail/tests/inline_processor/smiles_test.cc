#include "inline_processor_test.h"

namespace sendbernar::tests {

using namespace http_getter;

struct SmilesTest: public Test, public BaseData, public SmilesData {
    void SetUp() override {
        BaseData::init();
        SmilesData::init();

        handlers = {
            smiles
        };

        body = "|yandex_smile_"s + classId + "|"s;
    }
};

TEST_F(SmilesTest, shouldReturnSmile) {
    const mail_getter::SanitizerParsedResponse resp = {
        body,
        {
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                              boost::make_optional<std::string>(body.substr(1, body.size()-2)),
                                              mail_getter::MarkupPosition{0, 21, 1, 19}}
        }
    };

    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(resp));

    const auto h = createTypedDummy(yhttp::response{.status=200, .body=smileBody});
    const auto [attachments, html] = *makeInlineProcessor(h)->process(body, requestId());

    auto e = endpoint.format(fmt::arg("smile_id", classId));
    EXPECT_THAT(attachments, UnorderedElementsAre(RemoteAttachment{
        smileBody, "base64",
        "image1", "inline",
        "smile"s + classId, smilesHost + e.method(),
        ""
    }));
    ASSERT_EQ(html, "|cid:smile"s + classId + "|"s);
}

TEST_F(SmilesTest, shouldNotReturnSmileInCaseOfError) {
    const mail_getter::SanitizerParsedResponse resp = {
        body,
        {
            mail_getter::SanitizerMarkupEntry{mail_getter::SanitizerMarkupType_Class,
                                              boost::make_optional<std::string>(body.substr(1, body.size()-2)),
                                              mail_getter::MarkupPosition{0, 21, 1, 19}}
        }
    };

    EXPECT_CALL(*sanitizer, sanitize(body, requestId()))
            .WillOnce(Return(resp));

    const auto h = createTypedDummy(yhttp::response{.status=400});
    const auto [attachments, html] = *makeInlineProcessor(h)->process(body, requestId());

    ASSERT_TRUE(attachments.empty());
    ASSERT_EQ(html, "");
}

}
