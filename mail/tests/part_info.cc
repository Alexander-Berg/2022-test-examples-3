#define BOOST_TEST_MODULE part_info
#include <tests_common.h>

#include <mimeparser/MessageStructure.h>

BOOST_AUTO_TEST_SUITE(part_info)

BOOST_AUTO_TEST_CASE(part_info_test)
{
    PartStructure partStructure;
    partStructure.newChild();
    partStructure.newChild().newChild();
    //cerr << partStructure << endl;
}

BOOST_AUTO_TEST_SUITE_END()
