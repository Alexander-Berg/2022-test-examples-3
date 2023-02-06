#define BOOST_TEST_MODULE message_handler
#include <tests_common.h>

#include <mimeparser/MessageParser.h>
#include <mimeparser/MessageHandlerHelper.h>

#include "DummyMessageHandler.h"
#include "File.h"

BOOST_AUTO_TEST_SUITE(message_handler)

typedef std::string::const_iterator Iterator;
typedef HeaderHandler<Iterator> HHandler;
typedef MessageHandler<Iterator> MHandler;
typedef DummyMessageHandler<Iterator> DummyHandler;
typedef MessageHandlerTee<MHandler, DummyHandler> TeeHandler;
typedef MessageParser<Iterator, TeeHandler> Parser;

BOOST_AUTO_TEST_CASE(tee_handler)
{
    File file("./multipart.txt");
    MHandler mHandler;
    DummyHandler dummyHandler;
    TeeHandler teeHandler(mHandler, dummyHandler);
    Iterator start=file.contents().begin();
    Parser parser(start, teeHandler);
    parser.push(file.contents().end());
    parser.stop();
    BOOST_REQUIRE(5==dummyHandler.partNumber());
}

BOOST_AUTO_TEST_CASE(helper_handler)
{
    File file("./multipart.txt");
    typedef MessageHandlerHelper<DummyHandler> Helper;
    DummyHandler dummyHandler;
    Helper helper(dummyHandler);
    Parser parser(file.contents().begin(), helper.handler());
    parser.push(file.contents().end());
    parser.stop();
    BOOST_REQUIRE(5==dummyHandler.partNumber());
}

BOOST_AUTO_TEST_CASE(simple_function)
{
    File file("./multipart.txt");
    DummyHandler dummyHandler;
    std::string& content=file.contents();
    parse_message(dummyHandler, content.begin(), content.end());
    BOOST_REQUIRE(5==dummyHandler.partNumber());
}

BOOST_AUTO_TEST_CASE(mulca_dummy)
{
    const std::string result="\
<?xml version=\"1.0\" encoding=\"utf-8\"?>\n\
<message>\n\
<part id=\"1\" offset=\"0\" length=\"200\"\n\
>\n\
\n\
</part>\n\
</message>\n\
";
    std::stringstream ss;
    MessageStructure messageStructure;
    messageStructure.setPartStructure(new PartStructure());
    messageStructure.partStructure()->info().setLength(200);
    ss << messageStructure;
    BOOST_REQUIRE(result==ss.str());
}

BOOST_AUTO_TEST_SUITE_END()
