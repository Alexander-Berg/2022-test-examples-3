#include <gtest/gtest.h>

#include <parser/message.h>

using namespace ymod_smtpserver;
using namespace testing;

struct MessageParserRemoveLeadingDots: public Test {

    MessageParserRemoveLeadingDots(): parser(true) {}

    parser::MessageParser parser;
    std::string::const_iterator processed;
};

struct MessageParserNotModifyMessage: public Test {

    MessageParserNotModifyMessage(): parser(false) {}

    parser::MessageParser parser;
    std::string::const_iterator processed;
};

struct MessageParserNotModifyMessageTest: public MessageParserNotModifyMessage {
    const std::string buf = "From: from@ya.ru\r\n\r\n.. data\r\n.\r\n Next message ...";
    std::string::const_iterator eom = buf.begin() + buf.find("\n.\r\n") + 1;
};

struct MessageParserRemoveLeadingDotsTest: public MessageParserRemoveLeadingDots {
    const std::string buf = "From: from@ya.ru\r\n\r\n. one dot\r\n"
        "... three dots\r\n"
        "without dots\r\n"
        ".\r\r\n\n"
        "..\r\n"
        ".\r\nNext message ...";
    const std::string withoutDots = "From: from@ya.ru\r\n\r\n one dot\r\n"
        ".. three dots\r\n"
        "without dots\r\n"
        "\r\r\n\n"
        ".\r\n";
    std::string::const_iterator eom = buf.begin() + buf.find("\n.\r\n") + 1;
};

TEST_F(MessageParserNotModifyMessageTest, ParseEmptyChunk) {
    EXPECT_FALSE(parser.parse(buf.begin(), buf.begin(), processed));
    EXPECT_EQ(processed, buf.begin());
}

TEST_F(MessageParserNotModifyMessageTest, ParseOneChunk) {
    // all data in one chunk
    EXPECT_TRUE(parser.parse(buf.begin(), buf.end(), processed));
    EXPECT_EQ(processed, eom + 3);
    EXPECT_EQ(parser.getMessage()->size(), static_cast<size_t>(std::distance(buf.begin(), eom)));
}

TEST_F(MessageParserNotModifyMessageTest, ParseTwoChunks) {
    // first chunk ends with "\r\n"
    auto middle = buf.begin() + buf.find("\r\n") + 2;
    EXPECT_FALSE(parser.parse(buf.begin(), middle, processed));
    EXPECT_EQ(processed, middle);

    // second chunk contains the eom marker
    EXPECT_TRUE(parser.parse(middle, buf.end(), processed));
    EXPECT_EQ(processed, eom + 3);
    EXPECT_EQ(parser.getMessage()->size(), static_cast<size_t>(std::distance(buf.begin(), eom)));
}

TEST_F(MessageParserRemoveLeadingDotsTest, ParseOneChunk) {
    // all data in one chunk
    EXPECT_TRUE(parser.parse(buf.begin(), buf.end(), processed));
    EXPECT_EQ(processed, eom + 3);
    EXPECT_EQ(parser.getMessage()->size() + 4, static_cast<size_t>(std::distance(buf.begin(), eom)));
    EXPECT_EQ(*parser.getMessage(), withoutDots);
}

TEST_F(MessageParserRemoveLeadingDotsTest, ParseCharByChar) {
    processed = buf.begin();
    for (auto end = buf.begin() + 1; end < buf.end(); ++end) {
        if (parser.parse(processed, end, processed)) {
            break;
        }
    }
    EXPECT_EQ(processed, eom + 3);
    EXPECT_EQ(parser.getMessage()->size() + 4, static_cast<size_t>(std::distance(buf.begin(), eom)));
    EXPECT_EQ(*parser.getMessage(), withoutDots);
}
