#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs/mime_part_factory.h>
#include <mail_getter/meta_parts_converter.h>
#include <mail_getter/message_access_mock.h>
#include <library/cpp/testing/gtest_boost_extensions/extensions.h>

#include <mail/hound/include/internal/wmi/yplatform/helpers/mimes_helpers.h>
#include "mimes.h"

namespace {

using namespace testing;
using namespace hound::helpers::mimes;
using namespace mail_getter;

TEST(IsInlineTest, rfc822Part_returnTrue) {
    const auto mimePart = macs::MimePartFactory().hid("1.1").contentType("message")
        .contentSubtype("rfc822").release();
    EXPECT_TRUE(isInlinePart(mimePart));
}

TEST(IsInlineTest, messagePartialPart_returnFalse) {
    const auto mimePart = macs::MimePartFactory().hid("1.1").contentType("message")
        .contentSubtype("partial").release();
    EXPECT_FALSE(isInlinePart(mimePart));
}

TEST(IsInlineTest, textHtmlPart_returnFalse) {
    const auto mimePart = macs::MimePartFactory().hid("1.1").contentType("text")
        .contentSubtype("html").release();
    EXPECT_FALSE(isInlinePart(mimePart));
}

TEST(ExistChildrenMimePartsTest, noChildrenMimeParts_returnFalse) {
    macs::MimeParts mimeParts = {
        macs::MimePartFactory().hid("1.1").contentType("text").contentSubtype("plain").release(),
        macs::MimePartFactory().hid("1.2").contentType("application").contentSubtype("pdf").release(),
        macs::MimePartFactory().hid("1.4").contentType("image").contentSubtype("png").release(),
    };
    EXPECT_FALSE(existChildrenMimeParts(mimeParts, "1.3"));
}

TEST(ExistChildrenMimePartsTest, emptyMimeParts_returnFalse) {
    EXPECT_FALSE(existChildrenMimeParts({}, "1.3"));
}

TEST(ExistChildrenMimePartsTest, hasChildrenMimeParts_returnTrue) {
    macs::MimeParts mimeParts = {
        macs::MimePartFactory().hid("1.1").contentType("text").contentSubtype("plain").release(),
        macs::MimePartFactory().hid("1.2").contentType("application").contentSubtype("pdf").release(),
        macs::MimePartFactory().hid("1.3.1").contentType("text").contentSubtype("html").release(),
        macs::MimePartFactory().hid("1.3.2").contentType("image").contentSubtype("png").release(),
    };
    EXPECT_TRUE(existChildrenMimeParts(mimeParts, "1.3"));
}

class MetaPartsConverterMock: public MetaPartsConverter {
public:
    MOCK_METHOD(MetaParts, getMetaPartsFromXml, (const Stid&, OptYieldContext), (const, override));
    MOCK_METHOD(MetaParts, extractMetaPartsFromInline, (MessageAccessPtr, const MetaPart&, OptYieldContext), (const, override));
};

TEST(ExtractInlinePartsTest, severalInlineParts_returnAll) {
    const auto converterMock = std::make_shared<MetaPartsConverterMock>();
    const auto maMock = std::make_shared<MessageAccessMock>();

    const macs::MimeParts mimeParts = {
        macs::MimePartFactory().hid("1.1").contentType("text").contentSubtype("plain").release(),
        macs::MimePartFactory().hid("1.2").contentType("application").contentSubtype("pdf").release(),
        macs::MimePartFactory().hid("1.3").contentType("message").contentSubtype("rfc822").release(),
    };
    const MetaParts inlineMetaParts = {
        {"1.3.1", macs::MimePartFactory().hid("1.3.1").contentType("text")
                      .contentSubtype("html").release()},
        {"1.3.2", macs::MimePartFactory().hid("1.3.2").contentType("image")
                      .contentSubtype("jpg").release()},
        {"1.3.3", macs::MimePartFactory().hid("1.3.3").contentType("application")
                      .contentSubtype("pdf").release()},
    };
    EXPECT_CALL(*converterMock, extractMetaPartsFromInline(_, mimeParts[2], _))
        .WillOnce(Return(inlineMetaParts));

    const macs::MimeParts expected = {
        macs::MimePartFactory().hid("1.3.1").contentType("text").contentSubtype("html").release(),
        macs::MimePartFactory().hid("1.3.2").contentType("image").contentSubtype("jpg").release(),
        macs::MimePartFactory().hid("1.3.3").contentType("application").contentSubtype("pdf").release(),
    };

    const macs::MimeParts actual = extractInlineParts(maMock, mimeParts, converterMock);
    EXPECT_EQ(expected, actual);
}

} // namespace


