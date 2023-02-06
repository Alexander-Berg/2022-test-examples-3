#include <yplatform/zerocopy/segment.h>
#include <yplatform/zerocopy/streambuf.h>

#include <boost/random/mersenne_twister.hpp>
#include <string>
#include <ostream>

#include "random_string.h"

template <class T>
yplatform::zerocopy::segment make_segment(const T& x)
{
    yplatform::zerocopy::streambuf buffer(512, 512);
    std::ostream stream(&buffer);
    stream << x << std::flush;
    return buffer.detach(buffer.end());
}

int do_test(size_t a_off, size_t a_sz, size_t b_off, size_t b_sz)
{
    boost::mt19937 rnd(42);
    random_string rnd_str;

    std::string a_str(rnd_str(rnd, a_sz));
    std::string b_str(rnd_str(rnd, b_sz));
    std::string ab_str(a_str + b_str);

    yplatform::zerocopy::segment a(make_segment(std::string(a_off, ' ') + a_str));
    yplatform::zerocopy::segment b(make_segment(std::string(b_off, ' ') + b_str));
    a = a.get_part(a.begin() + a_off, a.end());
    b = b.get_part(b.begin() + b_off, b.end());
    yplatform::zerocopy::segment ab(make_segment(ab_str));

    a.append(b);

    bool res = a.size() == ab.size() && std::equal(a.begin(), a.end(), ab.begin());
    if (!res)
        std::cerr << "a_off=" << a_off << ", a_sz=" << a_sz << ", b_off=" << b_off
                  << ", b_sz=" << b_sz << " failed" << std::endl;
    return res ? 0 : 1;
}

int main()
{
    int fails = 0;

    // All combinations of data alignment in fragments (left, none, right) and fragments count (1,
    // 2) for both segments
    fails += do_test(0, 12, 0, 12);
    fails += do_test(0, 12, 100, 12);
    fails += do_test(0, 12, 500, 12);
    fails += do_test(0, 12, 0, 524);
    fails += do_test(0, 12, 100, 524);
    fails += do_test(0, 12, 500, 524);

    fails += do_test(100, 12, 0, 12);
    fails += do_test(100, 12, 100, 12);
    fails += do_test(100, 12, 500, 12);
    fails += do_test(100, 12, 0, 524);
    fails += do_test(100, 12, 100, 524);
    fails += do_test(100, 12, 500, 524);

    fails += do_test(500, 12, 0, 12);
    fails += do_test(500, 12, 100, 12);
    fails += do_test(500, 12, 500, 12);
    fails += do_test(500, 12, 0, 524);
    fails += do_test(500, 12, 100, 524);
    fails += do_test(500, 12, 500, 524);

    fails += do_test(0, 524, 0, 12);
    fails += do_test(0, 524, 100, 12);
    fails += do_test(0, 524, 500, 12);
    fails += do_test(0, 524, 0, 524);
    fails += do_test(0, 524, 100, 524);
    fails += do_test(0, 524, 500, 524);

    fails += do_test(100, 524, 0, 12);
    fails += do_test(100, 524, 100, 12);
    fails += do_test(100, 524, 500, 12);
    fails += do_test(100, 524, 0, 524);
    fails += do_test(100, 524, 100, 524);
    fails += do_test(100, 524, 500, 524);

    fails += do_test(500, 524, 0, 12);
    fails += do_test(500, 524, 100, 12);
    fails += do_test(500, 524, 500, 12);
    fails += do_test(500, 524, 0, 524);
    fails += do_test(500, 524, 100, 524);
    fails += do_test(500, 524, 500, 524);

    // Some boundary values
    fails += do_test(0, 12, 0, 0);
    fails += do_test(100, 12, 0, 0);
    fails += do_test(500, 12, 0, 0);
    fails += do_test(0, 0, 0, 12);
    fails += do_test(0, 0, 100, 12);
    fails += do_test(0, 0, 500, 12);

    fails += do_test(0, 0, 0, 0);
    fails += do_test(100, 0, 0, 0);
    fails += do_test(512, 0, 0, 0);
    fails += do_test(0, 0, 100, 0);
    fails += do_test(0, 0, 512, 0);
    fails += do_test(100, 0, 100, 0);
    fails += do_test(512, 0, 512, 0);

    // Large
    fails += do_test(12, 3456, 78, 9012);

    return fails;
}
