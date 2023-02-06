#include <iostream>
#include <string>
#include <deque>
#include <parser/websocket.h>
#include <session.h>
#include <websocket_stream.h>
#include <sstream>
#include <ymod_webserver/header.h>
#include <boost/algorithm/string/predicate.hpp>

using std::string;
using std::stringstream;

using ymod_webserver::upgrade_proto_header;
using ymod_webserver::upgrade_to_websocket75;
using ymod_webserver::upgrade_to_websocket13;

#ifndef CATCH_CONFIG_MAIN
#define CATCH_CONFIG_MAIN
#endif
#include <catch.hpp>

class t_stream
{
public:
    t_stream& operator<<(int val)
    {
        str_stream << static_cast<char>(0xFF & val);
        return *this;
    }

    t_stream& operator<<(char* val)
    {
        str_stream << val;
        return *this;
    }

    t_stream& operator<<(const char* val)
    {
        str_stream << val;
        return *this;
    }

    t_stream& operator<<(t_stream const& other)
    {
        str_stream << other.str();
        return *this;
    }

    string str() const
    {
        return str_stream.str();
    }

    void clear()
    {
        str_stream.str("");
        str_stream.clear();
    }

private:
    stringstream str_stream;
};

class t_websocket_parser
{
public:
    typedef ymod_webserver::parser::websocket<std::string::const_iterator> ws_parser_t;
    typedef ws_parser_t::message_t ws_message_t;

    t_websocket_parser()
    {
    }

    string parse(upgrade_proto_header parser_version = upgrade_to_websocket75, bool check = true)
    {
        ws_parser = ws_parser_t(parser_version);
        string::const_iterator i_start;
        string raw_str = raw_message.str();
        ws_parser(raw_str.begin(), i_start = raw_str.begin(), raw_str.end());

        auto result_msg = ws_parser.msg();
        string result_str = std::string(result_msg.first, result_msg.second);

        if (check)
        {
            REQUIRE(ws_parser.is_finished());
            REQUIRE(result_str == expected.str());
        }

        headers = ws_parser.headers();
        return result_str;
    }

    void check_opcode(uint8_t opcode)
    {
        CHECK(headers.opcode == opcode);
    }

    t_stream raw_message;
    t_stream expected;
    ws_parser_t ws_parser;
    ymod_webserver::websocket::message headers;
};

TEST_CASE_METHOD(t_websocket_parser, "parser/websocket/1/text/valid", "text message parse")
{
    expected << "Hello World!";
    raw_message << 0 << expected << 0xFF;
    parse();
}

TEST_CASE_METHOD(
    t_websocket_parser,
    "parser/websocket/1/text/invalid",
    "invalid text message parse")
{
    expected << "Hello World!";
    raw_message << 1 << expected << 0xFF;
    REQUIRE_THROWS(parse());
}

TEST_CASE_METHOD(t_websocket_parser, "parser/websocket/1/binary/1", "")
{
    raw_message << 0x80 << 0x2B;
    for (int i = 0; i < 0x2B; ++i)
        expected << 'A';
    raw_message << expected;
    parse();
}

TEST_CASE_METHOD(t_websocket_parser, "parser/websocket/1/binary/2", "")
{
    raw_message << 0x80 << 0x81 << 0x20;
    for (int i = 0; i < 160; ++i)
        expected << 'A';
    raw_message << expected;
    parse();
}

TEST_CASE_METHOD(
    t_websocket_parser,
    "parser/websocket/2/text/unmasked",
    "A single-frame unmasked text message")
{
    expected << "Hello";
    raw_message << 0x81 << 0x05 << expected;
    parse(upgrade_to_websocket13);
    check_opcode(ymod_webserver::websocket::message::opcode_text);
    CHECK(headers.length == 5);
}

TEST_CASE_METHOD(
    t_websocket_parser,
    "parser/websocket/2/text/masked",
    "A single-frame masked text message")
{
    expected << "Hello";
    raw_message << 0x81
                << 0x85
                /*mask*/
                << 0x37 << 0xfa << 0x21
                << 0x3d
                /*payload*/
                << expected;

    parse(upgrade_to_websocket13);
    check_opcode(ymod_webserver::websocket::message::opcode_text);
    CHECK(headers.length == 5);
}

TEST_CASE_METHOD(
    t_websocket_parser,
    "parser/websocket/2/binary/medium",
    "256 bytes binary message in a single unmasked frame")
{
    raw_message << 0x82 << 0x7E << 0x01 << 0x00;
    for (int i = 0; i < 0x0100; ++i)
        expected << 'A';
    raw_message << expected;
    parse(upgrade_to_websocket13);
    check_opcode(ymod_webserver::websocket::message::opcode_binary);
    CHECK(headers.length == 0x100);
}

TEST_CASE_METHOD(
    t_websocket_parser,
    "parser/websocket/2/binary/large",
    "64KiB binary message in a single unmasked frame")
{
    raw_message << 0x82 << 0x7F << 0x00 << 0x00 << 0x00 << 0x00 << 0x00 << 0x01 << 0x00 << 0x00;
    for (int i = 0; i < 0x10000; ++i)
        expected << 'A';
    raw_message << expected;
    parse(upgrade_to_websocket13);
    check_opcode(ymod_webserver::websocket::message::opcode_binary);
    CHECK(headers.length == 0x10000);
}

TEST_CASE_METHOD(
    t_websocket_parser,
    "parser/websocket/3/binary/length",
    "external payload length = 128")
{
    raw_message << 0x82 << 0x7E << 0x00 << 0x80;
    for (int i = 0; i < 0x80; ++i)
        expected << 'A';
    raw_message << expected;
    parse(upgrade_to_websocket13);
    check_opcode(ymod_webserver::websocket::message::opcode_binary);
    CHECK(headers.length == 0x80);
}

TEST_CASE_METHOD(t_websocket_parser, "parser/websocket/2/ping/unmasked", "Unmasked Ping request")
{
    expected << "Hello";
    raw_message << 0x89
                << 0x05
                /*payload*/
                << expected;
    parse(upgrade_to_websocket13);
    check_opcode(ymod_webserver::websocket::message::opcode_ping);
}

TEST_CASE_METHOD(t_websocket_parser, "parser/websocket/2/ping/masked", "Masked Ping response")
{
    expected << "Hello";
    raw_message << 0x8a
                << 0x85
                /*mask*/
                << 0x37 << 0xfa << 0x21
                << 0x3d
                /*payload*/
                << expected;

    parse(upgrade_to_websocket13);
    check_opcode(ymod_webserver::websocket::message::opcode_pong);
}

TEST_CASE_METHOD(
    t_websocket_parser,
    "parser/websocket/2/parse_websocket_2/empty",
    "Bad iterators: begin==end")
{
    ws_parser = ws_parser_t(upgrade_to_websocket13);
    string::const_iterator i_start;
    string raw_str = "";
    auto result_iter = ws_parser(raw_str.begin(), i_start = raw_str.begin(), raw_str.end());

    CHECK(result_iter == raw_str.begin());
    CHECK(i_start == raw_str.begin());
    REQUIRE(!ws_parser.is_finished());
}

TEST_CASE_METHOD(
    t_websocket_parser,
    "parser/websocket/2/parse_websocket_2/equal_iters",
    "Bad iterators: begin==end")
{
    expected << "Hello";
    raw_message << 0x89
                << 0x05
                /*payload*/
                << expected;

    ws_parser = ws_parser_t(upgrade_to_websocket75);
    string::const_iterator i_start;
    string raw_str = raw_message.str();
    auto result_iter = ws_parser(raw_str.begin(), i_start = raw_str.begin(), raw_str.begin());

    CHECK(result_iter == raw_str.begin());
    CHECK(i_start == raw_str.begin());
    REQUIRE(!ws_parser.is_finished());
}

TEST_CASE_METHOD(
    t_websocket_parser,
    "parser/websocket/2/parse_websocket_2/start_equals_end",
    "Bad iterators: start == end")
{
    expected << "Hello";
    raw_message << 0x89
                << 0x05
                /*payload*/
                << expected;

    ws_parser = ws_parser_t(upgrade_to_websocket75);
    string::const_iterator i_start;
    string raw_str = raw_message.str();
    auto result_iter = ws_parser(raw_str.begin(), i_start = raw_str.end(), raw_str.end());

    CHECK(i_start == raw_str.end());
    CHECK(result_iter == raw_str.begin());
    REQUIRE(!ws_parser.is_finished());
}

TEST_CASE_METHOD(
    t_websocket_parser,
    "parser/websocket/2/parse_websocket_2/start_near_end",
    "Bad iterators: start = end - 1")
{
    expected << "Hello";
    raw_message << 0x89
                << 0x05
                /*payload*/
                << expected;

    ws_parser = ws_parser_t(upgrade_to_websocket13);
    string raw_str = raw_message.str();
    string::const_iterator i_start = raw_str.end();
    i_start--;
    string::const_iterator begin = raw_str.begin();
    string::const_iterator end = raw_str.end();

    auto result_iter = ws_parser(begin, i_start, end);
    CHECK((i_start - raw_str.end()) == 0);
    CHECK((result_iter - raw_str.end()) == 0);
    REQUIRE(!ws_parser.is_finished());

    result_iter = ws_parser(result_iter, i_start, end);
    CHECK((i_start - raw_str.end()) == 0);
    CHECK((result_iter - raw_str.end()) == 0);
    REQUIRE(!ws_parser.is_finished());
}

TEST_CASE("parser/websocket/out_test", "")
{
    yplatform::zerocopy::streambuf buffer;
    std::ostream stream(&buffer);
    ymod_webserver::parser::make_websocket_binary_size(stream, 160);
    stream.flush();
    buffer.commit(buffer.size());
    yplatform::zerocopy::segment seg = buffer.detach(buffer.end());
    yplatform::zerocopy::segment::iterator i = seg.begin();
    REQUIRE(static_cast<uint8_t>(*i) == 0x80);
    ++i;
    REQUIRE(static_cast<uint8_t>(*i) == 0x81);
    ++i;
    REQUIRE(static_cast<uint8_t>(*i) == 0x20);
}

TEST_CASE("parser/websocket/mask_test", "")
{
    typedef boost::iterator_range<ymod_webserver::websocket::mask_iterator<std::string::iterator>>
        msg_t;
    std::string test = "AAAAAAA";
    ymod_webserver::websocket::mask_t mask;
    mask[0] = 1;
    mask[1] = 2;
    mask[2] = 3;
    mask[3] = 4;
    msg_t msg = boost::make_iterator_range(
        ymod_webserver::websocket::mask_iterator<std::string::iterator>(test.begin(), mask),
        ymod_webserver::websocket::mask_iterator<std::string::iterator>(test.end(), mask));
    REQUIRE(boost::iequals(
        boost::make_iterator_range(
            ymod_webserver::websocket::unmask_iterator<msg_t::iterator>(msg.begin(), mask),
            ymod_webserver::websocket::unmask_iterator<msg_t::iterator>(msg.end(), mask)),
        "AAAAAAA"));
}

TEST_CASE("parser/websocket/1/stream_parse", "")
{
    ymod_webserver::parser::websocket<std::string::const_iterator> p;
    std::deque<std::string> multi_result;
    t_stream multi_stream;
    t_stream result;

    result << "Hello World!";
    multi_stream << 0 << result << 0xFF;
    multi_result.push_back(result.str());

    result.clear();
    for (int i = 0; i < 43; ++i, result << 'A')
        ;
    multi_stream << 0x80 << 0x2B << result;
    multi_result.push_back(result.str());

    result.clear();
    for (int i = 0; i < 160; ++i, result << 'A')
        ;
    multi_stream << 0x80 << 0x81 << 0x20 << result;
    multi_result.push_back(result.str());

    std::string multi_buff = multi_stream.str();
    std::string::const_iterator to_parse = multi_buff.begin();
    std::string::const_iterator to_parse_end = multi_buff.end();
    for (std::deque<std::string>::const_iterator i = multi_result.begin(); i != multi_result.end();
         ++i)
    {
        p.reset();
        p(to_parse, to_parse, to_parse_end);
        REQUIRE(p.is_finished());
        CHECK(std::string(p.msg().first, p.msg().second) == *i);
    }
    REQUIRE(to_parse == to_parse_end);
}
