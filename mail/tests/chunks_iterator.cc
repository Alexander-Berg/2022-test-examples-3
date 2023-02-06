// dummy test as example
#define BOOST_TEST_MODULE chunks_iterator
#include <tests_common.h>

#include <mimeparser/chunks_iterator.h>
#include <mimeparser/Chunks.h>

BOOST_AUTO_TEST_SUITE(chunks_iterator)

typedef Chunks<string::iterator> StringChunks;
typedef StringChunks::iterator CIterator;

BOOST_AUTO_TEST_CASE(use_case)
{
    string first("first");
    string second("second");
    string firstsecond=first+second;
    StringChunks chunks;
    BOOST_REQUIRE(0==chunks.numberOfChunks());
    BOOST_REQUIRE(0==chunks.size());
    chunks.appendChunk(first.begin(), first.end());
    BOOST_REQUIRE(1==chunks.numberOfChunks());
    BOOST_REQUIRE(first.size()==chunks.size());
    chunks.appendChunk(second.begin(), second.end());
    BOOST_REQUIRE(2==chunks.numberOfChunks());
    BOOST_REQUIRE(first.size()+second.size()==chunks.size());
    BOOST_REQUIRE(!lexicographical_compare(
                      firstsecond.begin(), firstsecond.end(),
                      chunks.begin(), chunks.end()
                  )
                 );
    BOOST_REQUIRE(!lexicographical_compare(
                      chunks.begin(), chunks.end(),
                      firstsecond.begin(), firstsecond.end()
                  )
                 );
}

BOOST_AUTO_TEST_CASE(operator_square_brackets)
{
    string one("one");
    string two("two");
    string onetwo=one+two;
    StringChunks chunks;
    chunks.appendChunk(one.begin(), one.end());
    chunks.appendChunk(two.begin(), two.end());
    for (unsigned int i=0; i<onetwo.size(); ++i) {
        BOOST_REQUIRE(chunks[i]==onetwo[i]);
    }
}

BOOST_AUTO_TEST_CASE(iterator_increment)
{
    string foo("foo");
    string bar("bar");
    string foobar=foo+bar;
    StringChunks chunks;
    chunks.appendChunk(foo);
    chunks.appendChunk(bar);
    size_t offset=0;
    for (CIterator it=chunks.begin(); it!=chunks.end(); ++it) {
        BOOST_REQUIRE(*it==foobar[offset]);
        ++offset;
    }
}

BOOST_AUTO_TEST_CASE(iterator_advance)
{
    string foo("foo");
    string bar("bar");
    string foobar=foo+bar;
    StringChunks chunks;
    chunks.appendChunk(foo);
    chunks.appendChunk(bar);
    size_t offset=0;
    for (CIterator it=chunks.begin(); it!=chunks.end();) {
        if (offset==foobar.size()) {
            break;
        }
        BOOST_REQUIRE(*(it+offset)==foobar[offset]);
        ++offset;
    }
}

BOOST_AUTO_TEST_CASE(distance_to)
{
    string foo("foo");
    string bar("bar");
    string foobar=foo+bar;
    StringChunks chunks;
    chunks.appendChunk(foo);
    chunks.appendChunk(bar);
    for (size_t i=0; i<foobar.size(); ++i) {
        CIterator it=chunks.begin();
        it+=i;
        BOOST_REQUIRE((it-chunks.begin())==static_cast<int>(i));
    }
}

/// @todo empty chunk test, etc.

BOOST_AUTO_TEST_SUITE_END()
