#define BOOST_TEST_MODULE unfold_iterator
#include <tests_common.h>

#include <mimeparser/unfold_iterator.h>

BOOST_AUTO_TEST_SUITE(unfold_iterator)

typedef Unfold<string::iterator> StringUnfold;
typedef StringUnfold::iterator uIterator;

BOOST_AUTO_TEST_CASE(unfold_test)
{
    string folded("Test: Me\n Right\r\n Now");
    uIterator begin=StringUnfold::create(folded.begin(), folded.end());
    uIterator end=StringUnfold::create(folded.end(), folded.end());
    std::string result;
    copy(begin, end, back_inserter<string>(result));
    BOOST_REQUIRE(result=="Test: Me Right Now");
}

BOOST_AUTO_TEST_SUITE_END()
