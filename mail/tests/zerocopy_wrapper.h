#pragma once
#include <string>
#include <yplatform/zerocopy/streambuf.h>

using std::string;

typedef yplatform::zerocopy::streambuf read_buffer_t;
typedef boost::shared_ptr<read_buffer_t> read_buffer_ptr;
typedef std::vector<boost::asio::mutable_buffer, std::allocator<boost::asio::mutable_buffer>>
    mutable_buffers_t;

class zerocopy_wrapper
{
public:
    zerocopy_wrapper()
    {
        reset();
    }

    void reset(std::size_t max_fragmentation = 0)
    {
        if (max_fragmentation)
        {
            buffer.reset(new yplatform::zerocopy::streambuf(
                /*min_fragmentation*/ 1, /*max_fragmentation*/ max_fragmentation));
        }
        else
        {
            buffer.reset(new yplatform::zerocopy::streambuf());
        }
    }

    void fill_buffers(const string& str)
    {
        fill_buffers(str, str.length());
    }

    void fill_buffers(const string& str, std::size_t commit_size)
    {
        mutable_buffers_t m_buffers = buffer->prepare(commit_size);

        auto temp_buffer = boost::asio::buffer(str);
        boost::asio::buffer_copy(m_buffers, temp_buffer);

        buffer->commit(commit_size);
    }

public:
    read_buffer_ptr buffer;
};
