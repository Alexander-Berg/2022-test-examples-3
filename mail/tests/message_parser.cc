#define BOOST_TEST_MODULE message_parser
#include <tests_common.h>

#include <mimeparser/MessageParser.h>

#include "File.h"
#include "DummyMessageHandler.h"

unsigned int getPartNumber(const PartStructure& partStructure)
{
    unsigned int noOfChilds = 0;
    for (unsigned int ind = 0; ind < partStructure.size(); ++ind) {
        noOfChilds += getPartNumber(partStructure.getChild(ind));
    }
    return noOfChilds + 1;
}

template <class MyMessageHandler, class MyFactory>
void parseFile(File& file, MyMessageHandler& messageHandler,
               MyFactory& headerHandlerFactory = MyFactory())
{
    MessageParser<string::iterator, MyMessageHandler, MyFactory >
    messageParser(file.contents().begin(),
                  messageHandler, headerHandlerFactory);
    string::iterator it=file.contents().begin();
    for (; it!=file.contents().end(); ++it) {
        messageParser.push(it);
    }
    messageParser.push(file.contents().end());
    messageParser.stop();
}

BOOST_AUTO_TEST_SUITE(message_parser)

BOOST_AUTO_TEST_CASE(multipart)
{
    File file("multipart.txt");
    HeaderHandlerFactory<string::iterator> headerHandlerFactory;
    DummyMessageHandler<string::iterator> dummyMessageHandler;
    parseFile(file, dummyMessageHandler, headerHandlerFactory);
    MessageHandler<string::iterator> messageHandler;
    parseFile(file, messageHandler, headerHandlerFactory);
    BOOST_CHECK_EQUAL(dummyMessageHandler.partNumber(),
                      getPartNumber(*messageHandler.result().partStructure()));
}

BOOST_AUTO_TEST_CASE(inline_message)
{
    File file("inline_message.txt");
    HeaderHandlerFactory<string::iterator> headerHandlerFactory;
    headerHandlerFactory.setParseInline(true);
    DummyMessageHandler<string::iterator> dummyMessageHandler;
    parseFile(file, dummyMessageHandler, headerHandlerFactory);
    MessageHandler<string::iterator> messageHandler;
    parseFile(file, messageHandler, headerHandlerFactory);
    BOOST_CHECK_EQUAL(dummyMessageHandler.partNumber(),
                      getPartNumber(*messageHandler.result().partStructure()));
}

BOOST_AUTO_TEST_SUITE_END()
