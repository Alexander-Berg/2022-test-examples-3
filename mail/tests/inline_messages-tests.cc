#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/inline_messages.h>
#include <macs/mime_part_factory.h>

namespace {

using namespace testing;
using namespace mail_getter;

TEST(InlineMessages, getInlineMessages_MetaPartsWithInlineMessagePart_returnsInlineMessagesWithIt) {
    auto inlineMessagePart = macs::MimePartFactory().hid("1").contentType("message")
            .contentSubtype("rfc822").release();
    MetaParts metaParts = {{"1", std::move(inlineMessagePart)}};

    auto inlineMessages = getInlineMessages(metaParts);
    EXPECT_TRUE(inlineMessages.find("1") != inlineMessages.end());
}

TEST(InlineMessages, getInlineMessages_MetaPartsWithoutInlineMessagePart_returnsEmptyInlineMessages) {
    auto messagePart = macs::MimePartFactory().hid("1").contentType("img")
            .contentSubtype("gif").release();
    MetaParts metaParts = {{"1", std::move(messagePart)}};

    auto inlineMessages = getInlineMessages(metaParts);
    EXPECT_TRUE(inlineMessages.empty());
}

}
