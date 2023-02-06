#include <market/library/process_log/message.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <string>


using namespace NMarket::NProcessLog;


TEST(ProcessLog, MessageConstructor)
{
    TLogMessage msg("110", "Test message");

    ASSERT_EQ("110", msg.Code);
    ASSERT_EQ("Test message", msg.Text.Str());
}


TEST(ProcessLog, MessageCopy)
{
    TLogMessage msg("110", "Test message");
    msg.YtLogMsg.set_feed_id(123);
    msg.YtLogMsg.set_technical_details("tech");

    TLogMessage copied(msg);
    ASSERT_EQ(123, copied.YtLogMsg.feed_id());
    ASSERT_EQ("tech", copied.YtLogMsg.technical_details());
}


TEST(ProcessLog, MessageParenOperator)
{
    TLogMessage msg("110", "Test message");
    msg.YtLogMsg.set_feed_id(123);
    msg.YtLogMsg.set_technical_details("tech");

    ASSERT_EQ(123, msg().YtLogMsg.feed_id());
    ASSERT_EQ("tech", msg().YtLogMsg.technical_details());
}


TEST(ProcessLog, MessageAppendText)
{
    TLogMessage msg("110", "One");

    TString six(" Six");
    std::string seven(" Seven");

    msg.AppendText(" Two")                          // check "const char*"
       .AppendText(" Three")                        // check chaining
       .AppendText(" Four", " Five", six, seven)    // check parameter pack
       .AppendText(six)                             // check single-parameter(TString)
       .AppendText(seven);                          // check single-parameter(std::string)

    ASSERT_EQ("One Two Three Four Five Six Seven Six Seven", msg.Text.Str());
}
