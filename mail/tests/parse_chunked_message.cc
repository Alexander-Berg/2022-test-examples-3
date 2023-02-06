#define BOOST_TEST_MODULE parse_chunked_message
#include <tests_common.h>

#include <string>

#include "File.h"
#include <mimeparser/Mulca.h>
#include <mimeparser/chunks_iterator.h>
#include <mimeparser/Chunks.h>

BOOST_AUTO_TEST_SUITE(parse_chunked_message)

typedef Chunks<std::string::const_iterator> StringChunks;

std::string continuousParse(const std::string& content)
{
    using MimeParser::Mulca::parse_message;
    return parse_message(content);
}

std::string chunkedParse(const std::string& content)
{
    using MimeParser::Mulca::parse_range;
    StringChunks stringChunks;
    std::string::const_iterator it=content.begin();
    if (content.end()==it) {
        stringChunks.appendChunk(it,it); // empty chunk
    } else {
        while (it!=content.end()) {
            std::string::const_iterator jt=it+1;
            stringChunks.appendChunk(it, jt);
            it=jt;
        }
    }
    return parse_range(stringChunks.begin(), stringChunks.end());
}

bool isEqualParsed(const std::string& filename)
{
    File file(filename);
    file.contents();
    const std::string& content=file.contents();
    const std::string continuous=continuousParse(content);
    const std::string chunked=chunkedParse(content);
    return continuous==chunked;
}


BOOST_AUTO_TEST_CASE(parse_chunked)
{
    int argc=boost::unit_test::framework::master_test_suite().argc;
    char** argv=boost::unit_test::framework::master_test_suite().argv;
    std::string path;
    if (1>=argc) {
        path="./multipart.txt";
    } else {
        path=argv[1];
    }
    BOOST_REQUIRE(isEqualParsed(path));
}

BOOST_AUTO_TEST_SUITE_END()
