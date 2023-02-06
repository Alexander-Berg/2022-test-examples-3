#include <libxml/tree.h>
#include <libxml/c14n.h>

#include <yandex/string_writer.h>

#include <cppunit/Asserter.h>
#include <cppunit/TestAssert.h>
#include <yandex/xml_unit_test.h>


#include <iostream>
#include <stdexcept>
using std::string;

namespace Yandex{
namespace XMLUnit{

/*
int         xmlC14NDocSaveTo (xmlDocPtr doc,
            xmlNodeSetPtr nodes,
            int exclusive,
            xmlChar **inclusive_ns_prefixes,
            int with_comments,
            xmlOutputBufferPtr buf);
*/

string CanonizeXMLDoc(xmlDocPtr doc)
{
    xmlKeepBlanksDefault(0);

    string formated;
    xmlOutputBufferPtr formater = xmlAllocOutputBuffer(NULL);
    formater->context = &formated;
    formater->writecallback = string_writer;

    xmlSaveFormatFileTo(formater, doc, 0, 1);

    xmlFreeDoc(doc);
    doc = NULL;

    doc = xmlParseMemory(formated.c_str(), formated.length());

    xmlOutputBufferPtr writer = xmlAllocOutputBuffer(NULL);

    string c14n;
    writer->context = &c14n;
    writer->writecallback = string_writer;

    xmlC14NDocSaveTo(doc, 0, 0, 0, 0, writer);

    xmlFreeDoc(doc);

    return c14n;
};

string CanonizeXMLFile(const string& file)
{
    xmlDocPtr doc = xmlParseFile(file.c_str());
    if(doc == 0)
        throw std::runtime_error(string("Can't open file : ") + file + "\n");

    return CanonizeXMLDoc(doc);
}

string CanonizeXML(const string& str)
{
    xmlDocPtr doc = xmlParseMemory(str.c_str(), str.length());

    if(doc == 0)
        throw std::runtime_error(string("Can't parse xml\n----\n") + str + "\n----\n");

    return CanonizeXMLDoc(doc);
}

};
};
