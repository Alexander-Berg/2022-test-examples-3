#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <butil/butil.h>
#include <mimeparser/qp.h>

namespace {

template <class Iterator>
size_t test_length(const boost::iterator_range<Iterator>& range) {
    using namespace mail::utils::qp;
    typedef LengthCalculator<Iterator> Calculator;
    Calculator calculator;
    Iterator begin=range.begin();
    Iterator end=range.end();
    for (Iterator it=begin; it!=end; ++it) {
        calculator.push(it, it+1);
    }
    calculator.stop();
    return calculator.length();
}

TEST(quoted_printable, length)
{
    using namespace mail::utils;
    std::string qp_string("Test=20is=\r\n=20nice=20");
    size_t length=qp::calculate_length<std::string::const_iterator>(qp_string);
    ASSERT_TRUE(decode_qp(qp_string).length()==length);
    length=test_length<std::string::const_iterator>(qp_string);
    ASSERT_TRUE(decode_qp(qp_string).length()==length);
}
}
