#define CATCH_CONFIG_MAIN
#include "directed_buffer.h"
#include <catch.hpp>
#include <boost/algorithm/cxx11/iota.hpp>
#include <algorithm>
#include <numeric>
#include <vector>

using std::vector;
using std::string;
using namespace pipeline;

namespace {
const unsigned default_buffer_size = 10U;

std::vector<int> init_and_fill(int size) {
    std::vector<int> array(size);
    //std::iota(array.begin(), array.end(), 1);
    boost::algorithm::iota(array, 1);
    return array;
}
}

class t_stream : public DirectedBuffer<int>
{
    typedef DirectedBuffer <int> base_t;
public:
    t_stream(std::size_t capacity) : base_t(capacity)
    {}
};

class t_stream_buffer
{
public:
    t_stream_buffer()
    : buffer(default_buffer_size)
    {}

    void reset(std::size_t capacity = default_buffer_size)
    {
        buffer = t_stream(capacity);
    }

    t_stream buffer;
};

TEST_CASE_METHOD(t_stream_buffer, "buffer/put", "")
{
    std::vector<int> values = init_and_fill(5);
    REQUIRE(buffer.empty());
    REQUIRE(values.end() == buffer.put_range(values.begin(), values.end()));
    for (unsigned i = 0; i < values.size(); i++) {
        REQUIRE((buffer[i] == values[i]));
    }
}

TEST_CASE_METHOD(t_stream_buffer, "buffer/put/overflow", "")
{
    const unsigned count = 3;
    reset(count);
    std::vector<int> values = init_and_fill(5);

    REQUIRE(buffer.put_range(values.begin(), values.end()) == values.begin() + count);
    REQUIRE(buffer.put_range(values.begin(), values.end()) == values.begin());
    REQUIRE(buffer.size() == count);
    for (unsigned i = 0; i < count; i++) {
        REQUIRE((buffer[i] == values[i]));
    }
}

TEST_CASE_METHOD(t_stream_buffer, "buffer/committed_end", "one put -- several commits")
{
    std::vector<int> values = init_and_fill(5);
    buffer.put_range(values.begin(), values.end());
    REQUIRE_THROWS(buffer.commit(-1));
    buffer.commit(1);
    REQUIRE(buffer.committed_end() == 0);
    buffer.commit(2);
    REQUIRE(buffer.committed_end() == 0);
    buffer.commit(0);
    REQUIRE(buffer.committed_end() == 3);
    buffer.commit(3);
    REQUIRE_NOTHROW(buffer.commit(3));
    REQUIRE(buffer.committed_end() == 4);
    buffer.commit(4);
    REQUIRE(buffer.committed_end() == 5);
    REQUIRE_NOTHROW(buffer.commit(4));
    REQUIRE_THROWS(buffer.commit(5));
}

TEST_CASE_METHOD(t_stream_buffer, "buffer/commit_until", "")
{
    std::vector<int> values = init_and_fill(5);
    buffer.put_range(values.begin(), values.end());
    buffer.commit_until(2);
    REQUIRE(buffer.committed_end() == 3);
    buffer.commit_until(1);
    REQUIRE(buffer.committed_end() == 3);
    buffer.commit_until(3);
    REQUIRE(buffer.committed_end() == 4);
    buffer.commit_until(4);
    REQUIRE(buffer.committed_end() == 5);
    buffer.commit_until(4);
    REQUIRE(buffer.committed_end() == 5);
}

TEST_CASE_METHOD(t_stream_buffer, "buffer/consume", "")
{
    reset(10);
    std::vector<int> values = init_and_fill(10);
    buffer.put_range(values.begin(), values.end());
    REQUIRE(buffer.begin_id() == 0);

    buffer.commit_until(2);
    auto committed = buffer.consume();
    REQUIRE(buffer.committed_end() == 3);
    REQUIRE(buffer.begin_id() == 3);
    REQUIRE(buffer.size() == values.size() - 3);
    REQUIRE(committed->size() == 3);

    buffer.commit_until(4);
    buffer.commit_until(6);
    committed = buffer.consume();
    REQUIRE(buffer.committed_end() == 7);
    REQUIRE(buffer.begin_id() == 7);
    REQUIRE(buffer.size() == values.size() - 7);
    REQUIRE(committed->size() == 4);

    committed = buffer.consume();
    REQUIRE(buffer.begin_id() == 7);
    REQUIRE(buffer.size() == values.size() - 7);
    REQUIRE(!committed);
}

TEST_CASE_METHOD(t_stream_buffer, "buffer/consume/count", "")
{
    reset(10);
    std::vector<int> values = init_and_fill(10);
    buffer.put_range(values.begin(), values.end());
    REQUIRE(buffer.begin_id() == 0);

    buffer.commit_until(9);
    auto committed = buffer.consume(0);
    REQUIRE(buffer.begin_id() == 0);
    REQUIRE(buffer.size() == values.size());
    REQUIRE(!committed);

    committed = buffer.consume(3);
    REQUIRE(buffer.begin_id() == 3);
    REQUIRE(buffer.size() == values.size() - 3);
    REQUIRE(committed->size() == 3);

    committed = buffer.consume(4);
    REQUIRE(buffer.begin_id() == 7);
    REQUIRE(buffer.size() == values.size() - 7);
    REQUIRE(committed->size() == 4);

    committed = buffer.consume(10);
    REQUIRE(buffer.begin_id() == 10);
    REQUIRE(buffer.size() == 0);
    REQUIRE(committed->size() == 3);
}

TEST_CASE_METHOD(t_stream_buffer, "buffer/commit/put", "")
{
    const unsigned count = 10;
    reset(count);
    const unsigned step = 3;
    std::vector<int> values = init_and_fill(step);

    for (unsigned commit_id = 0; commit_id < count; commit_id += step) {
        buffer.put_range(values.begin(), values.end());
        std::size_t commit_end = std::min(commit_id + step, count);
        buffer.commit_until(commit_end - 1);
        REQUIRE(buffer.committed_end() == commit_end);
    }
}

TEST_CASE_METHOD(t_stream_buffer, "buffer/commit/put/overflow", "put and commit more than capacity")
{
    reset(20);
    std::vector<int> values = init_and_fill(10);
    std::size_t size = 0;
    for (int i = 0; i < 3; i++) {
        REQUIRE(buffer.put_range(values.begin(), values.end()) == values.end());
        size += values.size() / 2;
        buffer.commit_until(buffer.begin_id() + values.size()/2 - 1);
        buffer.consume();
        REQUIRE(buffer.size() == size);
    }
    REQUIRE(buffer.put_range(values.begin(), values.end()) == values.begin() + values.size()/2);

    std::size_t values_offset = values.size() / 2;
    std::size_t committed_offset = values.size() * 3 / 2;
    for (unsigned i = 0; i < buffer.size(); i++) {
        REQUIRE((buffer[committed_offset + i] == values[(values_offset + i) % values.size()]));
    }
}

TEST_CASE_METHOD(t_stream_buffer, "buffer/commit/combine_operations", "")
{
    reset(10);
    std::vector<int> values = init_and_fill(4);

    buffer.put_range(values.begin(), values.end());
    for (unsigned i = 0; i < values.size() / 2 + 1; i++) {
        buffer.commit(i);
    }
    buffer.commit(values.size() / 2);
    REQUIRE(buffer.committed_end() == values.size() / 2 + 1);

    for (auto i = values.size() / 2 + 1; i < values.size(); i++) {
        buffer.commit(i);
    }
    REQUIRE(buffer.committed_end() == values.size());

    // put shouldn't affect committed_end
    buffer.put_range(values.begin(), values.end());
    buffer.commit(values.size());
    REQUIRE(buffer.committed_end() == values.size() + 1);
    buffer.put_range(values.begin(), values.end());
    buffer.commit(values.size() + 1);
    REQUIRE(buffer.committed_end() == values.size() + 2);
}
