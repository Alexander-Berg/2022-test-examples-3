#include <yplatform/zerocopy/streambuf.h>

#include <gtest.h>

namespace {

using namespace testing;
using namespace yplatform::zerocopy;

const std::size_t RESERVED_SIZE = 1024;

template <class T>
class ReservedDescendingAllocator : public std::allocator<T>
{
public:
    using base_type = std::allocator<T>;
    using size_type = std::size_t;
    using pointer = T*;
    using const_pointer = const T*;

    ReservedDescendingAllocator() noexcept = default;

    template <class Other>
    struct rebind
    {
        typedef ReservedDescendingAllocator<Other> other;
    };

    pointer allocate(size_type count)
    {
        return base_type::allocate(count);
    }

    void deallocate(pointer data, size_type count)
    {
        base_type::deallocate(data, count);
    }

private:
    std::allocator<T> base_;
};

template <>
class ReservedDescendingAllocator<char> : public std::allocator<char>
{
public:
    using base_type = std::allocator<char>;
    using size_type = std::size_t;
    using pointer = char*;
    using const_pointer = const char*;

    ReservedDescendingAllocator() : buffer_(std::make_shared<Buffer>())
    {
        add(buffer_);
    }

    template <class Other>
    struct rebind
    {
        typedef ReservedDescendingAllocator<Other> other;
    };

    pointer allocate(size_type count)
    {
        const auto left = buffer_->last - buffer_->data_;
        if (count > 0 && left > 0 && static_cast<std::size_t>(left) >= count)
        {
            buffer_->last -= count;
            return buffer_->last;
        }
        else
        {
            return base_type::allocate(count);
        }
    }

    void deallocate(pointer data, size_type count)
    {
        if (!owns(data, count))
        {
            base_type::deallocate(data, count);
        }
    }

private:
    struct Buffer
    {
        char data_[RESERVED_SIZE];
        char* last;

        Buffer() : last(data_ + RESERVED_SIZE)
        {
        }
    };

    std::shared_ptr<Buffer> buffer_;

    Buffer& buffer()
    {
        return *buffer_;
    }

    static void add(const std::shared_ptr<Buffer>& buffer)
    {
        static std::vector<std::shared_ptr<Buffer>> buffers;
        buffers.push_back(buffer);
    }

    bool owns(pointer data, size_type)
    {
        return buffer_->data_ <= data && data < buffer_->data_ + RESERVED_SIZE;
    }
};

struct StreambufTest : public Test
{
    static const std::size_t fragmentSize;
    /**
     * Stream buffer wrapper to access protected members for diagnostic
     */
    struct Streambuf : public basic_streambuf<>
    {
        typedef basic_streambuf<> Base;
        Streambuf() : Base(16, 32, 1, fragmentSize, 1024)
        {
        }
#define __UNPROTECT_CONST_MEMBER0(type, name)                                                      \
    type name() const                                                                              \
    {                                                                                              \
        return Base::name();                                                                       \
    }
#define __UNPROTECT_MEMBER1(type, name, type0)                                                     \
    type name(type0 arg0)                                                                          \
    {                                                                                              \
        return Base::name(arg0);                                                                   \
    }
        __UNPROTECT_CONST_MEMBER0(char_type*, gptr);
        __UNPROTECT_CONST_MEMBER0(char_type*, egptr);
        __UNPROTECT_CONST_MEMBER0(char_type*, eback);
        __UNPROTECT_CONST_MEMBER0(char_type*, epptr);
        __UNPROTECT_CONST_MEMBER0(char_type*, pbase);
        __UNPROTECT_CONST_MEMBER0(char_type*, pptr);
        __UNPROTECT_CONST_MEMBER0(const fragment_list&, fragments);
        __UNPROTECT_MEMBER1(void, reserve, std::size_t);
    };

    std::string string(const boost::asio::const_buffer& b) const
    {
        return std::string(boost::asio::buffer_cast<const char*>(b), boost::asio::buffer_size(b));
    }
};

const std::size_t StreambufTest::fragmentSize = 16;

TEST_F(StreambufTest, constructor)
{
    Streambuf s;
}

TEST_F(StreambufTest, constructor_setsEbackGptrEgptr_areEqual)
{
    Streambuf s;
    ASSERT_EQ(s.eback(), s.gptr());
    ASSERT_EQ(s.gptr(), s.egptr());
    ASSERT_EQ(s.eback(), s.egptr());
}

TEST_F(StreambufTest, constructor_setsPutSize_intoFragmentSize)
{
    Streambuf s;
    ASSERT_EQ(std::size_t(std::distance(s.pbase(), s.epptr())), fragmentSize);
}

TEST_F(StreambufTest, begin_isEqualTo_gptr)
{
    Streambuf s;
    ASSERT_EQ(&(*(s.begin())), s.gptr());
}

TEST_F(StreambufTest, end_isEqualTo_pptr)
{
    Streambuf s;
    ASSERT_EQ(&(*(s.begin())), s.pptr());
}

TEST_F(StreambufTest, reserve_withSizeMoreThanFragment_addAnotherFragments)
{
    Streambuf s;
    s.reserve(fragmentSize + 1);
    ASSERT_EQ(s.fragments().size(), 2ul);
}

TEST_F(StreambufTest, reserve_withSizeLessOrEqualToFragment_addNoFragment)
{
    Streambuf s;
    s.reserve(fragmentSize);
    ASSERT_EQ(s.fragments().size(), 1ul);
}

TEST_F(StreambufTest, reserve_withSizeMoreThanMaxSize_throwsException)
{
    Streambuf s;
    ASSERT_THROW(s.reserve(2048), std::length_error);
}

TEST_F(StreambufTest, iostreamInterface_withStringOut_inputSameString)
{
    Streambuf s;
    std::iostream io(&s);
    io << "0123456789ABCDEF0123456789ABCDEF!";
    std::string str;
    io >> str;
    ASSERT_EQ(str, "0123456789ABCDEF0123456789ABCDEF!");
}

TEST_F(StreambufTest, ostreamInterface_withString_asioBuffersReturnsStringFragments)
{
    Streambuf s;
    std::ostream io(&s);
    io << "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF!";
    Streambuf::const_buffers_type buffers(s.data());
    ASSERT_EQ(string(buffers[0]), "0123456789ABCDEF");
    ASSERT_EQ(string(buffers[1]), "0123456789ABCDEF0123456789ABCDEF");
    ASSERT_EQ(string(buffers[2]), "!");
}

TEST_F(StreambufTest, ostreamInterface_withString_iteratorRangeConstructsEqualString)
{
    Streambuf s;
    std::ostream io(&s);
    io << "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF!";
    EXPECT_EQ("0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF!", std::string(s.begin(), s.end()));
}

TEST_F(
    StreambufTest,
    ostreamInterface_withStringAndOneByteFragmentAndDescendingAddressesAllocator_asioBuffersReturnsStringFragments)
{
    basic_streambuf<char, std::char_traits<char>, ReservedDescendingAllocator<char>> s(1, 1, 1, 1);
    std::ostream io(&s);
    io << "012";
    Streambuf::const_buffers_type buffers(s.data());
    EXPECT_EQ(string(buffers[0]), "0");
    EXPECT_EQ(string(buffers[1]), "1");
    EXPECT_EQ(string(buffers[2]), "2");
}

TEST_F(
    StreambufTest,
    ostreamInterface_withStringAndOneByteFragment_asioBuffersReturnsStringFragments)
{
    basic_streambuf<char, std::char_traits<char>, std::allocator<char>> s(1, 1, 1, 1);
    std::ostream io(&s);
    io << "012";
    Streambuf::const_buffers_type buffers(s.data());
    EXPECT_EQ(string(buffers[0]), "0");
    EXPECT_EQ(string(buffers[1]), "1");
    EXPECT_EQ(string(buffers[2]), "2");
}

TEST_F(
    StreambufTest,
    ostreamInterface_withStringAndOneByteFragmentAndDescendingAddressesAllocator_iteratorRangeConstructsEqualString)
{
    basic_streambuf<char, std::char_traits<char>, ReservedDescendingAllocator<char>> s(1, 1, 1, 1);
    std::ostream io(&s);
    io << "012";
    EXPECT_EQ("012", std::string(s.begin(), s.end()));
}

TEST_F(
    StreambufTest,
    ostreamInterface_withStringAndOneByteFragment_iteratorRangeConstructsEqualString)
{
    basic_streambuf<char, std::char_traits<char>, std::allocator<char>> s(1, 1, 1, 1);
    std::ostream io(&s);
    io << "012";
    EXPECT_EQ("012", std::string(s.begin(), s.end()));
}

TEST_F(StreambufTest, detach_withBegin_doesNotChangesStreambuf)
{
    Streambuf s;
    std::ostream io(&s);
    io << "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF!";
    s.detach(s.begin());
    EXPECT_EQ("0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF!", std::string(s.begin(), s.end()));
}

TEST_F(
    StreambufTest,
    detach_withBeginAndOneByteFragmentSizeAndDescendingAddressesAllocator_doesNotChangesStreambuf)
{
    basic_streambuf<char, std::char_traits<char>, ReservedDescendingAllocator<char>> s(1, 1, 1, 1);
    std::ostream io(&s);
    io << "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF!";
    s.detach(s.begin());
    EXPECT_EQ("0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF!", std::string(s.begin(), s.end()));
}

} // namespace
