#include <parser/request.h>
#include <parser/body.h>
#include <validator.h>
#include <cassert>

int verify([[maybe_unused]] ymod_webserver::request_ptr req)
{
    assert(req->url.params.find("suid2")->second == "317140990");
    assert(req->url.params.find("data")->second == "test");
    assert(req->url.params.find("lll") != req->url.params.end());
    return 0;
}

int test()
{
    yplatform::zerocopy::streambuf buffer;
    std::ostream stream(&buffer);
    stream << "POST /notify HTTP/1.1\r\n"
           << "Host: attach10.mail.yandex.net:1080\r\n"
           << "Accept: */*\r\n"
           << "User-Agent: 317140990\r\n"
           << "Content-Type: application/x-www-form-urlencoded\r\n"
           << "Content-Length: 157\r\n"
           << "\r\n"
           << "suid2=317140990&data=test&lll";
    stream.flush();
    ymod_webserver::parser::request_parser<yplatform::zerocopy::streambuf::iterator> parser;
    parser.reset(ymod_webserver::context_ptr());
    yplatform::zerocopy::streambuf::iterator i_start = buffer.begin();
    parser(buffer.begin(), i_start, buffer.end());
    ymod_webserver::validator v;
    v(ymod_webserver::endpoint(), parser.req());
    yplatform::zerocopy::segment test = buffer.detach(i_start);
    parser.req()->raw_body = buffer.detach(buffer.end());
    ymod_webserver::parser::parse_body(parser.req());
    return verify(parser.req());
}

int main()
{
    // for (unsigned i = 1; i < 1000000; ++i) test();
    return test();
}
