#define BOOST_TEST_MODULE eol_handler
#include <tests_common.h>

#include <mimeparser/HeaderParser.h>

using namespace MimeParser::Parsers;

BOOST_AUTO_TEST_SUITE(eol_handler)


template <class Iterator>
struct Counter {
    typedef boost::iterator_range<Iterator> Range;
public:
    Counter()
        : m_counter(0)
        , m_n(0)
        , m_rn(0)
    {}
    bool endOfLine(const Range& range) {
        ++m_counter;
        if (1==range.size()) {
            ++m_n;
        } else if (2==range.size()) {
            ++m_rn;
        }
        return true;
    }
    unsigned int count() {
        return m_counter;
    }
    unsigned int n() {
        return m_n;
    }
    unsigned int rn() {
        return m_rn;
    }
private:
    unsigned int m_counter;
    unsigned int m_n;
    unsigned int m_rn;
};

BOOST_AUTO_TEST_CASE(count)
{
    typedef Counter<string::iterator> StringCounter;
    typedef EolParser<string::iterator, StringCounter> StringEventEol;
    string testString("\r\n\r\n\n\n  \n \n  \r\n \r\n \n\r ");
    StringCounter counter;
    string::iterator it=testString.begin();
    StringEventEol eventeol(it, counter);
    for (; it!=testString.end(); ++it) {
        eventeol.push(it);
    }
    eventeol.push(it);
    BOOST_REQUIRE(9==counter.count());
    BOOST_REQUIRE(5==counter.n());
    BOOST_REQUIRE(4==counter.rn());
    //std::cerr << counter.count() << std::endl;
    //std::cerr << counter.n() << std::endl;
    //std::cerr << counter.rn() << std::endl;
}

BOOST_AUTO_TEST_SUITE_END()
