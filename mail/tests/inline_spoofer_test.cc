#include "test_with_yield_context.h"
#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/message_access_mock.h>
#include <mail_getter/service_mock.h>
#include <mail_getter/attachment_storage_mock.h>
#include <macs/mime_part_factory.h>

#include <internal/logger.h>
#include <internal/inline_spoofer.h>

namespace boost::asio {

inline std::ostream& operator <<(std::ostream& stream, yield_context) {
    return stream;
}

} // namespace boost::asio

namespace {

using namespace testing;
using namespace msg_body;
using namespace mail_getter;
using namespace mail_getter::attach_sid;

CidPartInfo getCidPart() {
    CidPartInfo cidPart;
    cidPart.hid = "1.1.2";
    cidPart.mid = "2160000000004476215";
    cidPart.stid = "1000007.1120000000330824.170573770323993403125548517005";
    cidPart.name = "avatar.jpg";
    return cidPart;
}

class InlineSpooferTest: public TestWithYieldContext {
public:
    InlineSpooferTest()
        : logger_(std::make_shared<ContextLogger>(makeLoggerWithRequestId("")))
        , mockStorageService_(std::make_shared<ServiceMock>())
        , mockStorage_(std::make_shared<AttachmentStorageMock>())
    {
        config_.webattachServer = "webattach.mail.yandex.net";
    }
protected:
    const std::string aesKeyId = "42";
    const crypto::AesKey aesKey{"\x6F\x03\xA0\x6A\x48\xAE\x1E\xA2\x11\x52\xD0\xFE\x59\xBC\x7D\x7F"
                                "\xDC\x42\xB5\x8E\xF9\xD2\xC0\x0A\xEF\xD7\xB4\x73\x2D\x5A\x4A\xA6"};
    const std::string hmacKeyId = "42";
    const crypto::HmacKey hmacKey{"\x7E\xCF\xCA\xB4\xA4\x2F\x35\x47\xE6\x32\x89\xA0\x10\x15\x9E\xC7"
                                  "\x07\x50\xE6\xDC\x89\xFB\xF8\x82\x6C\x96\xED\x43\x0C\x18\x59\xB6"};

    const attach_sid::KeyContainer container{ {{aesKeyId, aesKey}}, {{hmacKeyId, hmacKey}} };
    const attach_sid::Keys keys = Keys(container, aesKeyId, hmacKeyId);

    Configuration config_;
    LogPtr logger_;
    std::shared_ptr<ServiceMock> mockStorageService_;
    std::shared_ptr<AttachmentStorageMock> mockStorage_;
    MessageAccessMock mockMessageAccess_;
};

TEST_F(InlineSpooferTest, draftSpoofer_noErrors_replaceAttachAndReturnUrl) {
    withSpawn([&] (YieldCtx yc) {
        AttachmentStorage::MetaInfo metaInfo;
        metaInfo.viewLargeUrl = "webattach.mail.yandex.net/message_part_real/?sid=sid&no_disposition=y";
        const auto inlineSpoofer = DraftInlineSpoofer {config_, logger_, mockStorageService_, mockMessageAccess_, keys, yc};

        EXPECT_CALL(mockMessageAccess_, getBody("1.1.2"))
            .WillOnce(Return("image_blob"));
        const MetaPart mta = macs::MimePartFactory().hid("1.1.2").contentType("image")
                .contentSubtype("jpg").release();
        EXPECT_CALL(mockMessageAccess_, getHeaderStruct("1.1.2"))
            .WillOnce(Return(mta));
        EXPECT_CALL(*mockStorageService_, createAttachmentStorage("webattach.mail.yandex.net"))
            .WillOnce(Return(mockStorage_));
        EXPECT_CALL(*mockStorage_, add(_, _, _, _, _, _, _))
            .WillOnce(DoAll(SetArgReferee<2>(metaInfo), Return(0)));
        const std::string actual = inlineSpoofer.getUrl(getCidPart());

        EXPECT_THAT(actual, StartsWith("webattach.mail.yandex.net/message_part_real/?sid="));
        EXPECT_THAT(actual, HasSubstr("no_disposition=y"));
        EXPECT_THAT(actual, HasSubstr("yandex_class=yandex_new_inline_"));
    });
}

TEST_F(InlineSpooferTest, draftSpoofer_getBodyError_returnEmpty) {
    withSpawn([&] (YieldCtx yc) {
        const auto inlineSpoofer = DraftInlineSpoofer {config_, logger_, mockStorageService_, mockMessageAccess_, keys, yc};

        EXPECT_CALL(mockMessageAccess_, getBody("1.1.2"))
            .WillOnce(Throw(std::runtime_error("error")));
        const std::string actual = inlineSpoofer.getUrl(getCidPart());

        EXPECT_EQ("", actual);
    });
}

TEST_F(InlineSpooferTest, draftSpoofer_getHeaderStructError_returnEmpty) {
    withSpawn([&] (YieldCtx yc) {
        const auto inlineSpoofer = DraftInlineSpoofer {config_, logger_, mockStorageService_, mockMessageAccess_, keys, yc};

        EXPECT_CALL(mockMessageAccess_, getBody("1.1.2"))
            .WillOnce(Return("image_blob"));
        EXPECT_CALL(mockMessageAccess_, getHeaderStruct("1.1.2"))
            .WillOnce(Throw(std::runtime_error("error")));
        const std::string actual = inlineSpoofer.getUrl(getCidPart());

        EXPECT_EQ("", actual);
    });
}

TEST_F(InlineSpooferTest, draftSpoofer_uploadError_returnEmpty) {
    withSpawn([&] (YieldCtx yc) {
        const auto inlineSpoofer = DraftInlineSpoofer {config_, logger_, mockStorageService_, mockMessageAccess_, keys, yc};

        EXPECT_CALL(mockMessageAccess_, getBody("1.1.2"))
            .WillOnce(Return("image_blob"));
        const MetaPart mta = macs::MimePartFactory().hid("1.1.2").contentType("image")
                .contentSubtype("jpg").release();
        EXPECT_CALL(mockMessageAccess_, getHeaderStruct("1.1.2"))
            .WillOnce(Return(mta));
        EXPECT_CALL(*mockStorageService_, createAttachmentStorage("webattach.mail.yandex.net"))
            .WillOnce(Return(mockStorage_));
        EXPECT_CALL(*mockStorage_, add(_, _, _, _, _, _, _))
            .WillOnce(Return(-1));
        const std::string actual = inlineSpoofer.getUrl(getCidPart());

        EXPECT_EQ("", actual);
    });
}

TEST_F(InlineSpooferTest, mainSpoofer_getUrl_returnInlineUrl) {
    MainInlineSpoofer inlineSpoofer("12345");
    const std::string actual = inlineSpoofer.getUrl(getCidPart());

    EXPECT_EQ("../message_part/avatar.jpg?_uid=12345&amp;hid=1.1.2&amp;ids=2160000000004476215&amp;name=avatar.jpg&amp;yandex_class=yandex_inline_content_1000007.1120000000330824.170573770323993403125548517005_1.1.2_2160000000004476215",
        actual);
}

} // namespace
