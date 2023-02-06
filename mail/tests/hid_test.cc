#define BOOST_TEST_MODULE hid_test
#include <tests_common.h>

#include <mimeparser/Hid.h>

BOOST_AUTO_TEST_SUITE(hid_test)

BOOST_AUTO_TEST_CASE(simple)
{
    Hid hid;
    BOOST_REQUIRE(1==hid.depth());
    BOOST_REQUIRE(hid.toString()=="1");
}

BOOST_AUTO_TEST_CASE(some_hid)
{
    string hidString("1.2.3.123.234");
    Hid hid(hidString);
    BOOST_REQUIRE(5==hid.depth());
    BOOST_REQUIRE(hid.toString()==hidString);
}

BOOST_AUTO_TEST_SUITE_END()
