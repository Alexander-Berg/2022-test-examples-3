#define BOOST_TEST_MODULE header_handler
#include <tests_common.h>

#include <mimeparser/HeaderHandlerHelper.h>

#include "DummyHeaderHandler.h"
#include "File.h"

BOOST_AUTO_TEST_SUITE(header_handler)

typedef std::string::const_iterator Iterator;
typedef HeaderHandler<Iterator> HHandler;
typedef DummyHeaderHandler<Iterator> DummyHHandler;
typedef HeaderHandlerTee<HHandler, DummyHHandler> TeeHHandler;

BOOST_AUTO_TEST_CASE(tee_handler)
{
    File file("./multipart.txt");
    HHandler hHandler;
    DummyHHandler dummyHHandler;
    TeeHHandler headerHandler(hHandler, dummyHHandler);
    HeaderParser<Iterator, TeeHHandler> headerParser(file.contents().begin(), headerHandler);
    string::iterator it=file.contents().begin();
    for (; it!=file.contents().end(); ++it) {
        headerParser.push(it);
    }
    headerParser.stop();
    BOOST_REQUIRE(23==dummyHHandler.headerFieldCount());
}

BOOST_AUTO_TEST_CASE(helper_handler)
{
    File file("./multipart.txt");
    typedef HeaderHandlerHelper<DummyHHandler> Helper;
    typedef Helper::Parser Parser;
    DummyHHandler dummyHHandler;
    Helper helper(dummyHHandler);
    Parser headerParser(file.contents().begin(), helper.handler());
    headerParser.push(file.contents().end());
    headerParser.stop();
    BOOST_REQUIRE(23==dummyHHandler.headerFieldCount());
}

BOOST_AUTO_TEST_CASE(simple_function)
{
    File file("./multipart.txt");
    DummyHHandler handler;
    parse_header(handler, file.contents().begin(), file.contents().end());
    BOOST_REQUIRE(23==handler.headerFieldCount());
}

BOOST_AUTO_TEST_SUITE_END()
