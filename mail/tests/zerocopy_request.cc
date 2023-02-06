#include "zerocopy_wrapper.h"
#include "init_log.h"
#include <parser/request.h>
#include <catch.hpp>

using std::string;

typedef ymod_webserver::parser::request_parser<yplatform::zerocopy::streambuf::iterator>
    zerocopy_parser_t;

class t_zerocopy_parser
{
public:
    t_zerocopy_parser()
    {
        reset();
    }

    void reset(int max_fragmentation = 0)
    {
        default_ctx.reset(new ymod_webserver::context());
        default_ctx->local_port = 80;
        parser.reset(default_ctx);
        zc_wrap.reset(max_fragmentation);
    }

    void parse(read_buffer_t::iterator& i_start)
    {
        try
        {
            auto detach_pos = parser(zc_wrap.buffer->begin(), i_start, zc_wrap.buffer->end());
            zc_wrap.buffer->detach(detach_pos);
        }
        catch (ymod_webserver::parse_error& error)
        {
            std::cout << error.public_message() << ":\n\t" << error.private_message() << "\n";
            std::cout.flush();
            throw error;
        }
    }

public:
    zerocopy_parser_t parser;
    ymod_webserver::context_ptr default_ctx;
    zerocopy_wrapper zc_wrap;
};

TEST_CASE_METHOD(
    t_zerocopy_parser,
    "parser/request/zerocopy/headers/on_header_begin/1",
    "check for header_parser::on_header_begin")
{
    reset(5);
    auto i_start = zc_wrap.buffer->begin();

    zc_wrap.fill_buffers("POST /neo2/handlers/handlers.xml HTTP/1.1\r\n");
    parse(i_start);
    // check if asserts
    parse(i_start);
}

TEST_CASE_METHOD(
    t_zerocopy_parser,
    "parser/request/zerocopy/headers/on_header_begin/2",
    "check for header_parser::on_header_begin: not first header")
{
    auto i_start = zc_wrap.buffer->begin();
    string str = "POST /neo2/handlers/handlers.xml HTTP/1.1\r\nHost:mail.ru\r\n  ";

    zc_wrap.fill_buffers(str, str.length() - 2);

    parse(i_start);
    // check if asserts
    parse(i_start);
}

TEST_CASE_METHOD(t_zerocopy_parser, "parser/request/zerocopy/on_method/1", "")
{
    reset(1);
    auto i_start = zc_wrap.buffer->begin();
    zc_wrap.fill_buffers("PO");
    parse(i_start);
    zc_wrap.fill_buffers("ST ");
    parse(i_start);
    zc_wrap.fill_buffers("/neo2/handlers/handlers.xml HTTP/1.1\r\n\r\n");
    parse(i_start);

    CHECK(parser.is_finished());
    CHECK(parser.req()->raw_request_line == "POST /neo2/handlers/handlers.xml HTTP/1.1");
}