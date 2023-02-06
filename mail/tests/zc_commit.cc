#include <yplatform/zerocopy/streambuf.h>
#include <string>
#include <cassert>

template <typename Iterator>
Iterator parse(Iterator begin, Iterator end)
{
    for (; begin != end && *begin == 't'; ++begin)
        ;
    assert(begin == end);
    return begin;
}

template <typename Range>
void test_range(const Range& r)
{
    std::string temp(r.begin(), r.end());
    assert(temp == std::string(512, 't'));
}

int test()
{
    yplatform::zerocopy::streambuf buffer;
    std::string test(512, 't');
    std::copy(test.begin(), test.end(), boost::asio::buffer_cast<char*>(buffer.prepare(512)[0]));
    buffer.commit(512);
    test_range(buffer.detach(parse(buffer.begin(), buffer.end())));
    return 0;
}

void test_size()
{
    yplatform::zerocopy::streambuf buffer;
    {
        std::ostream stream(&buffer);
        int max = 2537;
        for (int i = 0; i < max; ++i)
            stream << 'a';
    }
    yplatform::zerocopy::segment seg = buffer.detach(buffer.end());
    std::size_t s1 = seg.size();
    std::size_t s2 = static_cast<std::size_t>(std::distance(seg.begin(), seg.end()));
    assert(s2 == 2537);
    assert(s1 == 2537);
}

void test_empty()
{
    yplatform::zerocopy::segment seg;
    assert(seg.size() == 0);
    assert(seg.begin() == seg.end());
}

int main()
{
    test_size();
    test_empty();
    return test();
}
