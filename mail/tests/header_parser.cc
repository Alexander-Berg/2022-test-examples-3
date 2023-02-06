#define BOOST_TEST_MODULE header_parser
#include <tests_common.h>

#include <mimeparser/HeaderParser.h>

#include "File.h"
#include "DummyHeaderHandler.h"

BOOST_AUTO_TEST_SUITE(header_parser)

BOOST_AUTO_TEST_CASE(simple_test)
{
    File file("./header_parser.header.1.txt");
    DummyHeaderHandler<string::iterator> handler;
    HeaderParser<string::iterator, DummyHeaderHandler<string::iterator> > headerParser(file.contents().begin(), handler);
    //headerParser.push(file.contents().end());
    string::iterator it=file.contents().begin();
    for (; it!=file.contents().end(); ++it) {
        headerParser.push(it);
    }
    headerParser.stop();
}

BOOST_AUTO_TEST_CASE(default_handler)
{
    File file("./header_parser.header.1.txt");
    HeaderHandler<string::iterator> headerHandler;
    HeaderParser<string::iterator> headerParser(file.contents().begin(), headerHandler);
    string::iterator it=file.contents().begin();
    for (; it!=file.contents().end(); ++it) {
        headerParser.push(it);
    }
    headerParser.stop();
}

BOOST_AUTO_TEST_CASE(parse_full_mode)
{
    using rfc2822::rfc2822address;
    typedef HeaderHandler<string::const_iterator> Handler;
    typedef HeaderParser<string::const_iterator> Parser;
    File file("./header_parser.header.1.txt");
    Handler headerHandler(Handler::PARSE_FULL_MODE);
    Parser headerParser(file.contents().begin(), headerHandler);
    headerParser.push(file.contents().end());
    headerParser.stop();
    const std::string& messageId=headerHandler.data().messageId();
    const rfc2822address& address=*(headerHandler.data().from());
    BOOST_REQUIRE("<3391269528740@daria1.yandex.ru>"==messageId);
    BOOST_REQUIRE("tramsmm-mirror@yandex.ru"==address.begin()->second);
}

BOOST_AUTO_TEST_CASE(multipart)
{
    File file("./multipart.txt");
    HeaderHandler<string::iterator> headerHandler;
    HeaderParser<string::iterator> headerParser(file.contents().begin(), headerHandler);
    string::iterator it=file.contents().begin();
    for (; it!=file.contents().end(); ++it) {
        headerParser.push(it);
    }
    headerParser.stop();
}

BOOST_AUTO_TEST_SUITE_END()
