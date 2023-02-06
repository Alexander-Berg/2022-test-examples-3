/*
 * utils_test.cpp
 *
 * Developed by Vasily Tchekalkin <Bacek@yandex.ru>
 * Copyright (c) 2005 Yandex.ru
 *
 * $Id: utils_test.cpp,v 1.1 2005-04-05 05:56:44 bacek Exp $
 *
 * Changelog:
 * $Log: not supported by cvs2svn $
 * 05/04/2005 - created
 *
 */

#include <string>

#include <yandex/utils.h>

#include <cppunit/extensions/HelperMacros.h>

#include <libxml/tree.h>

class UtilsTest : public CppUnit::TestFixture
{
	CPPUNIT_TEST_SUITE( UtilsTest );

	CPPUNIT_TEST (testWriteDoc);
	CPPUNIT_TEST (testWriteNode);
	CPPUNIT_TEST_SUITE_END ();
public:

	void testWriteDoc()
	{
		xmlDocPtr doc = xmlNewDoc(0);

		xmlNodePtr root = xmlNewNode(0, BAD_CAST "tag");
		xmlDocSetRootElement(doc, root);

		xmlNodeAddContent(root, BAD_CAST "content");

		const std::string rv = WriteXMLToString(doc);

		xmlFreeDoc(doc);

		CPPUNIT_ASSERT_EQUAL(std::string("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tag>content</tag>\n"), rv);
	}

	void testWriteNode()
	{
		xmlNodePtr node = xmlNewNode(0, BAD_CAST "tag");
		xmlNodeAddContent(node, BAD_CAST "content");

		const std::string rv = WriteXMLToString(node);

		xmlFreeNode(node);

		CPPUNIT_ASSERT_EQUAL(std::string("<tag>content</tag>"), rv);
	}
};

CPPUNIT_TEST_SUITE_REGISTRATION( UtilsTest );


