#include <stdexcept>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <message_body/message_part.h>
#include <macs/mime_part_factory.h>

namespace msg_body {

using namespace testing;

TEST(MessagePartTest, isWindatLowerCase) {
    MessagePart part;
    part.headerStruct = macs::MimePartFactory().name("winmail.dat").release();
    part.contentType = MimeType("application", "ms-tnef");
    ASSERT_TRUE(isWindat(part));
}

TEST(MessagePartTest, isWindatMixedCase) {
    MessagePart part;
    part.headerStruct = macs::MimePartFactory().name("WINmail.dat").release();
    part.contentType = MimeType("application", "mS-TNEF");
    ASSERT_TRUE(isWindat(part));
}

TEST(MessagePartTest, isWindatNonWindat) {
    MessagePart part;
    part.headerStruct = macs::MimePartFactory().name("easy.pdf").release();
    part.contentType = MimeType("application", "pdf");
    ASSERT_FALSE(isWindat(part));
}

}
