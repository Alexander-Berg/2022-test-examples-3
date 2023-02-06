#include <stdlib.h>
#include <string>
#include <exception>
#include <iostream>
#include <stdexcept>
#include <yandex/xconfig.h>
#include <cppunit/extensions/HelperMacros.h>

/*
 * How to use this test as a quick introduction:
 * 1. Look at the functions GetParts and GetFirst.
 * 1.1 Function GetParts may be called both in root context, as a member
 * of XConfig object, and in node context, as a member of Part object.
 * In first case XPath expression must be always constructed with xml tree
 * root as starting point. See testMain for example. In second case XPath
 * may be relative to the xml tree node this Part object is refering to.
 * See testChain for example.
 * 1.2 Function GetFirst may be called in root, nodeset and node conetxts.
 * See testChain for example of usage in root and node context. GetFirst in
 * nodeset context is similar to obtaining zero indexed node in set, bu also
 * provides testing for number of nodes.
 * 2. Look at functions asLong and asString.
 * 3. Look at two variations of Parse function. Example of use is in
 * xconfigtest constructor.
 * 4. Look at testIfExists for an example of GetIfExists function usage. Pay
 * attention to the NOTE attached.
 */
class xconfigtest: public CppUnit::TestFixture {
    CPPUNIT_TEST_SUITE( xconfigtest );

    CPPUNIT_TEST (testMain);
    CPPUNIT_TEST (testRelative);
    CPPUNIT_TEST (testEmpty);
    CPPUNIT_TEST (testChain);
    CPPUNIT_TEST (testIfExists);
    CPPUNIT_TEST (testInMemory);

    CPPUNIT_TEST_SUITE_END();
public:
    xconfigtest() {
        int res = setenv("YANDEX_XML_CONFIG", "./testconfig.xml", 1);
        if(0 != res) {
            throw(std::runtime_error(std::string("Failed to set environment")));
        }
        try {
            cfgnf.Parse();
        }
        catch(const std::exception & exc) {
            std::stringstream straux;
            straux << "Caught unexpected exception: " << exc.what();
            CPPUNIT_FAIL(straux.str().c_str());
            throw;
        }
        try {
            cfg.Parse("./testconfig.xml");
        }
        catch(const std::exception & exc) {
            std::stringstream straux;
            straux << "Caught unexpected exception: " << exc.what();
            CPPUNIT_FAIL(straux.str().c_str());
            throw;
        }
    }
    virtual ~xconfigtest() {
        unsetenv("YANDEX_XML_CONFIG");
    }
    void testMain() {
        xmlConfig::Parts mparts = cfg.GetParts("/config/mainpart/*");
        CPPUNIT_ASSERT_EQUAL(mparts.Size(), 2);
    }
    void testRelative() {
        xmlConfig::Part mpart = cfg.GetFirst("/config/mainpart");
        // this test also shows how to chose particular part by it's attribute
        // note [@number=\"10\"] part of xpath expression - it will match only
        // part node that is child of the current node and has attribute number
        // with value of 10
        std::string somedata = mpart.GetFirst("part[@number=\"10\"]").asString();
        CPPUNIT_ASSERT_EQUAL(somedata, std::string("some data"));
        // this one demonstrates convertion on a fly for both attribute and
        // node contents
        long number = mpart.GetFirst("otherpart/@number").asLong();
        CPPUNIT_ASSERT_EQUAL(number, static_cast<long>(20));
        long ndata = mpart.GetFirst("otherpart").asLong();
        CPPUNIT_ASSERT_EQUAL(ndata, static_cast<long>(10000));
    }
    void testEmpty() {
        bool wasexcept = false;
        try {
            // following must fail with exception
            // you may derive what querry lead to exception by looking at
            // the value returned by what()
            xmlConfig::Part fail = cfg.GetFirst("/config/emptypart/somefoo");
        }
        catch(...) {
            wasexcept = true;
        }
        CPPUNIT_ASSERT_EQUAL(wasexcept, true);
    }
    void testChain() {
        // GetFirst in root context
        xmlConfig::Part inc = cfg.GetFirst("/config/inctest/inc");
        // GetFirst in node context with chained call to asString
        std::string data = inc.GetFirst(".//sample[@id=\"2\"]").asString();
        CPPUNIT_ASSERT_EQUAL(data, std::string("data"));
        xmlConfig::Parts topsamples = inc.GetParts("../sample");
        CPPUNIT_ASSERT_EQUAL(topsamples.Size(), static_cast<int>(1));
    }
    void testIfExists() {
        long int value = 100;
        // value should not change here
        bool failed = !(cfg.GetIfExists("/config/emptypart/somefoo", value));
        CPPUNIT_ASSERT_EQUAL(failed, true);
        CPPUNIT_ASSERT_EQUAL(value, static_cast<long>(100));
        // teststr should be set to empty here
        std::string teststr("blah");
        // NOTE: if you'll request contents of the node containing only
        // whitespaces, new line and indentation for instance, you may
        // perceive it as empty, but the string will contain those whitespaces
        // after the function call.
        bool succeed = cfg.GetIfExists("/config/emptypart", teststr);
        CPPUNIT_ASSERT_EQUAL(succeed, true);
        CPPUNIT_ASSERT_EQUAL(teststr.size(), static_cast<std::string::size_type>(0));
        // following two should succeed
        succeed = cfg.GetIfExists("/config/mainpart/part", teststr);
        CPPUNIT_ASSERT_EQUAL(succeed, true);
        CPPUNIT_ASSERT_EQUAL(teststr, std::string("some data"));
        succeed = cfg.GetIfExists("/config/mainpart/part/@number", value);
        CPPUNIT_ASSERT_EQUAL(succeed, true);
        CPPUNIT_ASSERT_EQUAL(value, static_cast<long>(10));
    }
    void testInMemory() {
        xmlConfig::XConfig cfg;
        try {
            std::string xmltext(
            "<?xml version=\"1.0\"?>" \
            "<config xmlns:xi=\"http://www.w3.org/2001/XInclude\">" \
            "    <mainpart>" \
            "        <part number=\"10\">some data</part>" \
            "        <otherpart number=\"20\">10000</otherpart>" \
            "    </mainpart>" \
            "    <emptypart/>" \
            "    <inctest>" \
            "        <xi:include href=\"include.xml\" />" \
            "        <xi:include href=\"include.xml\" xpointer=\"xpointer(/inc/incpartone/*)\" />" \
            "    </inctest>" \
            "</config>");
            cfg.Parse(xmltext);
        }
        catch(const std::exception & exc) {
            std::stringstream straux;
            straux << "Caught unexpected exception: " << exc.what();
            CPPUNIT_FAIL(straux.str().c_str());
            throw;
        }
        xmlConfig::Parts mparts = cfg.GetParts("/config/mainpart/*");
        CPPUNIT_ASSERT_EQUAL(mparts.Size(), 2);
    }
private:
    xmlConfig::XConfig cfgnf;
    xmlConfig::XConfig cfg;
};

CPPUNIT_TEST_SUITE_REGISTRATION( xconfigtest );
