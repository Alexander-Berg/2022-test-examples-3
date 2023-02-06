#include <catch.hpp>

#include "../src/session/stream_parser.h"
#include <ymod_messenger/detail/header.h>
#include <ymod_messenger/detail/message_handler_wrapper.h>
#include <ymod_messenger/detail/segment.h>
#include <ymod_messenger/types.h>
#include <boost/asio/buffer.hpp>

namespace ymod_messenger {

std::vector<char> pack_header(message_type type, size_t buffer1_sz)
{
    std::vector<char> v(detail::header_size(1));
    detail::pack_header(type, buffer1_sz, v.data(), v.size());
    return v;
}

std::vector<char> pack_header(message_type type, size_t buffer1_sz, size_t buffer2_sz)
{
    std::vector<char> v(detail::header_size(2));
    detail::pack_header(type, buffer1_sz, buffer2_sz, v.data(), v.size());
    return v;
}

std::vector<char> to_vector(const std::string& src)
{
    std::vector<char> result;
    result.resize(src.size());
    memcpy(&result[0], src.data(), src.size());
    return result;
}

void write(const std::vector<char>& data, stream_parser& parser, size_t consume = 0)
{
    parser.reserve_buffer(data.size());
    if (parser.buffer_free_size() < data.size())
        throw std::runtime_error("failed to reserve_buffer");
    std::copy(data.data(), data.data() + data.size(), parser.buffer());
    parser.consume_buffer(consume ? consume : data.size());
}

bool operator==(const shared_buffer& a, const std::string& b)
{
    return a.size() == b.size() && std::equal(a.data(), a.data() + a.size(), b.data());
}

using detail::unpack_context;
using detail::unpack_header;

TEST_CASE("unpack/header/no_result_if_insufficient_bytes", "")
{
    auto packed = pack_header(1, 100, 200);
    unpack_context ctx;
    REQUIRE(!ctx.header_parsed);
    for (size_t i = 0; i < packed.size(); ++i)
    {
        REQUIRE(unpack_header(packed.data(), i, ctx) == 0);
        REQUIRE(!ctx.header_parsed);
    }
}

TEST_CASE("unpack/header/success_if_enough_bytes", "")
{
    SECTION("one buffer sequence", "")
    {
        auto packed = pack_header(7, 100);
        unpack_context ctx;
        REQUIRE(unpack_header(packed.data(), packed.size(), ctx) == packed.size());
        REQUIRE(ctx.header_parsed);
        REQUIRE(ctx.type == 7);
        REQUIRE(ctx.seq_len == 1);
        REQUIRE(ctx.buffer_size[0] == 100);
        REQUIRE(ctx.buffers_parsed == 0);
    }

    SECTION("two buffers sequence", "")
    {
        auto packed = pack_header(9, 200, 300);
        unpack_context ctx;
        REQUIRE(unpack_header(packed.data(), packed.size(), ctx) == packed.size());
        REQUIRE(ctx.header_parsed);
        REQUIRE(ctx.type == 9);
        REQUIRE(ctx.seq_len == 2);
        REQUIRE(ctx.buffer_size[0] == 200);
        REQUIRE(ctx.buffer_size[1] == 300);
        REQUIRE(ctx.buffers_parsed == 0);
    }
}

TEST_CASE("unpack/header/success_if_extra_bytes", "")
{
    auto packed = pack_header(7, 100);
    packed.resize(packed.size() + 8);
    unpack_context ctx;
    REQUIRE(unpack_header(packed.data(), packed.size(), ctx) == packed.size() - 8);
    REQUIRE(ctx.header_parsed);
    REQUIRE(ctx.type == 7);
    REQUIRE(ctx.seq_len == 1);
    REQUIRE(ctx.buffer_size[0] == 100);
    REQUIRE(ctx.buffers_parsed == 0);
}

struct T_stream_parser
{
    stream_parser parser{ 16 };
    message_type type;
    shared_buffers seq;
    std::vector<char> header = pack_header(9, 32, 64);
    std::vector<char> header2 = pack_header(14, 7);
};

TEST_CASE_METHOD(T_stream_parser, "unpack/stream/no_result_on_insufficient_bytes_header", "")
{
    for (size_t i = 0; i < header.size(); ++i)
    {
        stream_parser local_parser;
        write(header, local_parser, i);
        REQUIRE(!local_parser.next(type, seq));
    }
}

TEST_CASE_METHOD(T_stream_parser, "unpack/stream/no_result_if_fragments_no_received", "")
{
    write(header, parser);
    for (size_t i = 0; i < 32 + 63; ++i)
    {
        write(to_vector("a"), parser);
        REQUIRE(!parser.next(type, seq));
    }
}

TEST_CASE_METHOD(T_stream_parser, "unpack/stream/success_if_enough_buffer", "")
{
    write(header, parser);
    write(to_vector(std::string(32, 'a')), parser);
    write(to_vector(std::string(64, 'f')), parser);
    REQUIRE(parser.next(type, seq));
    REQUIRE(type == 9);
    REQUIRE(seq.size() == 2);
    REQUIRE(seq[0].size() == 32);
    REQUIRE(seq[1].size() == 64);
    REQUIRE(seq[0] == std::string(32, 'a'));
    REQUIRE(seq[1] == std::string(64, 'f'));
}

TEST_CASE_METHOD(T_stream_parser, "unpack/stream/success_if_extra_data", "")
{
    write(header, parser);
    write(to_vector(std::string(32 + 64 + 64, 'a')), parser);
    REQUIRE(parser.next(type, seq));
    REQUIRE(type == 9);
    REQUIRE(seq.size() == 2);
    REQUIRE(seq[0].size() == 32);
    REQUIRE(seq[1].size() == 64);
}

TEST_CASE_METHOD(T_stream_parser, "unpack/stream/success_next_message", "")
{
    write(header, parser);
    write(to_vector(std::string(32 + 64, 'a')), parser);
    parser.next(type, seq);

    write(header2, parser);
    write(to_vector(std::string(7, 'w')), parser);
    REQUIRE(parser.next(type, seq));
    REQUIRE(type == 14);
    REQUIRE(seq.size() == 1);
    REQUIRE(seq[0].size() == 7);
}

TEST_CASE_METHOD(T_stream_parser, "unpack/stream/success_next_message_if_extra_data", "")
{
    write(header, parser);
    write(to_vector(std::string(32 + 64, 'a')), parser);
    parser.next(type, seq);

    write(header2, parser);
    write(to_vector(std::string(100, 'w')), parser);
    REQUIRE(parser.next(type, seq));
    REQUIRE(type == 14);
    REQUIRE(seq.size() == 1);
    REQUIRE(seq[0].size() == 7);
    REQUIRE(seq[0] == std::string(7, 'w'));
}

TEST_CASE("unpack/stream/read_two_fragments_to_different_buffers", "")
{
    message_type type;
    shared_buffers seq;
    auto header_sz = detail::header_size(2);
    stream_parser parser(header_sz + 24);
    auto buf = pack_header(11, 16, 8);
    for (auto c : to_vector(std::string(16, 'a')))
        buf.push_back(c);
    for (auto c : to_vector(std::string(8, 'b')))
        buf.push_back(c);
    write(buf, parser);
    REQUIRE(parser.next(type, seq));
    REQUIRE(seq.size() == 2);
    auto buf0 = seq[0].data() - header_sz;
    auto buf1 = seq[1].data() - header_sz - 16;
    REQUIRE(buf0 == buf1);
}

TEST_CASE("unpack/stream/read_two_fragments_to_single_shared_buffer", "")
{
    message_type type;
    shared_buffers seq;
    auto header_sz = detail::header_size(2);
    stream_parser parser(header_sz + 20);
    auto buf = pack_header(11, 16, 8);
    for (auto c : to_vector(std::string(16, 'a')))
        buf.push_back(c);
    for (auto c : to_vector(std::string(4, 'b')))
        buf.push_back(c);
    write(buf, parser);
    parser.next(type, seq);
    REQUIRE(parser.buffer_free_size() == 0);
    write(to_vector(std::string(4, 'b')), parser);
    REQUIRE(parser.next(type, seq));
    REQUIRE(seq.size() == 2);
    auto buf0 = seq[0].data() - header_sz;
    auto buf1 = seq[1].data() - header_sz - 16;
    REQUIRE(buf0 != buf1);
}

TEST_CASE("unpack/stream/dont_reallocate_if_already_parsed_header_in_buffer", "")
{
    message_type type;
    shared_buffers seq;
    stream_parser parser(detail::header_size(2));
    write(pack_header(6, 1, 1), parser);
    auto buf_with_header = parser.buffer() - detail::header_size(2);
    parser.next(type, seq);
    REQUIRE(parser.buffer_free_size() == 0);
    write(to_vector(std::string(2, 'a')), parser);
    auto buf_with_fragments = parser.buffer() - 2;
    REQUIRE(buf_with_header == buf_with_fragments);
}

TEST_CASE("unpack/stream/exception_on_trash_in_buffer", "")
{
    message_type type;
    shared_buffers seq;
    stream_parser parser;
    write(to_vector("jhadsiufjaoperfjhiuakfauifjouahjfoajfoiaejhrgioaekrgh"), parser);
    REQUIRE_THROWS(parser.next(type, seq));
}

// TEST_CASE("unpack/msgpack_fragment/unpack_gives_the_same_value_as_unpack", "")
// {
//     auto fragment = fragment(detail::create_msgpack_buffer(16.85));
//     double unpacked = 0.0;
//     auto handler = [&](const std::string&, double value){unpacked = value;};
//     detail::message_handler_wrapper<double, decltype(handler)> wrapper(std::move(handler));
//     shared_buffer buffers[1] = {fragment.to_shared_buffer()};
//     wrapper("addr", shared_buffers(buffers, 1));
//     REQUIRE(unpacked == 16.85);
// }

TEST_CASE("pack/huge_fragments_are_denied", "")
{
    message_type type;
    shared_buffers seq;
    stream_parser parser;
    write(pack_header(7, detail::MAX_FRAGMENT_SIZE + 1), parser);
    write(to_vector(std::string(detail::MAX_FRAGMENT_SIZE + 1, 'a')), parser);
    REQUIRE_THROWS(parser.next(type, seq));
}

TEST_CASE("pack/segment", "")
{
    auto segment = detail::create_msgpack_segment(7, 16.85);

    message_type type;
    shared_buffers seq;
    stream_parser parser;
    for (auto& f : segment)
    {
        parser.reserve_buffer(f.size());
        std::copy(f.data(), f.data() + f.size(), parser.buffer());
        parser.consume_buffer(f.size());
    }
    REQUIRE(parser.next(type, seq));

    double unpacked = 0.0;
    auto handler = [&](const std::string&, double value) { unpacked = value; };
    detail::message_handler_wrapper<double, decltype(handler)> wrapper(std::move(handler));
    wrapper("address", seq);
    REQUIRE(unpacked == 16.85);

    // write(to_vector(std::string(detail::MAX_FRAGMENT_SIZE+1, 'a')), parser);
}

}
