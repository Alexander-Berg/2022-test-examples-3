#define BOOST_TEST_MODULE header_field
#include <tests_common.h>

#include <mimeparser/HeaderField.h>

BOOST_AUTO_TEST_SUITE(header_field)

BOOST_AUTO_TEST_CASE(simple_test)
{
    const string field="Header : token1/token2; \n param1=value1 \r\n x";
    HeaderField headerField(field.begin(), field.end());
    BOOST_REQUIRE(headerField.name()=="Header");
    BOOST_REQUIRE(headerField.value()==" token1/token2;  param1=value1  x");
}

BOOST_AUTO_TEST_SUITE_END()
