#ifndef XML_UNIT_TEST_H
#define XML_UNIT_TEST_H

#include <string>
#include <exception>
#include <cppunit/Asserter.h>
#include <cppunit/TestAssert.h>
#include <cppunit/SourceLine.h>

namespace Yandex{
namespace XMLUnit{

std::string CanonizeXML(const std::string& str);
std::string CanonizeXMLFile(const std::string& file);

};
};

#define XMLUNIT_ASSERT_EQUAL( expected, actual ) \
try{ \
    CppUnit::assertEquals( ::Yandex::XMLUnit::CanonizeXML(expected), ::Yandex::XMLUnit::CanonizeXML(actual), CPPUNIT_SOURCELINE(), ""); \
} \
catch(std::exception& ex) { \
    CppUnit::Asserter::fail(ex.what(), CPPUNIT_SOURCELINE()); \
}


#define XMLUNIT_ASSERT_EQUAL_TO_FILE( file, actual ) \
try{ \
    CppUnit::assertEquals( ::Yandex::XMLUnit::CanonizeXMLFile(file), ::Yandex::XMLUnit::CanonizeXML(actual), CPPUNIT_SOURCELINE(), ""); \
} \
catch(std::exception& ex) { \
    CppUnit::Asserter::fail(ex.what(), CPPUNIT_SOURCELINE()); \
}




#endif
