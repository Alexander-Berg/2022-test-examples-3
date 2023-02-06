#include <parser/rfc822/rfc822.h>

#include <yplatform.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace yimap;

#define MESSAGE_PATH "data/simple_message.eml"

TEST(RFC822, TestParsing)
{
    auto messageData = readFile(MESSAGE_PATH);
    rfc822::MessageDataReactor mdata;
    rfc822::parseMessage(messageData, mdata, std::vector<std::string>());

    EXPECT_EQ(
        "Hello Joe, do you think we can meet at 3:30 tomorrow?\n",
        std::string(mdata.body.begin(), mdata.body.end()));
}
