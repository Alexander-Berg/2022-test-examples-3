#include <iostream>
#include <transfer/chunked.h>
#include <parser/request.h>

using std::string;

#define FAILED_MESSAGE                                                                             \
    {                                                                                              \
        std::cerr << "[FAILED] " << __FILE__ << ":" << __LINE__ << "\n";                           \
        return false;                                                                              \
    }

bool test1()
{
    const string REQ =
        "OPTIONS /api/ping?par1=arg1&par2=arg2;par3&par4;par5=1 HTTP/15.143\r\nHost: "
        "http://mail.yandex.ru\r\nTransfer-Encoding: "
        "chunked\r\n\r\n1A\r\n12345678901234567890123456\r\nA\r\n1234567890\r\n0\r\n\r\n";

    ymod_webserver::parser::request_parser<string::const_iterator> parser;
    parser.reset(ymod_webserver::context_ptr());
    string::const_iterator body_start = REQ.begin();
    body_start = parser(REQ.begin(), body_start, REQ.end());
    if (!parser.is_finished()) FAILED_MESSAGE
    ymod_webserver::transfer::chunked<string::const_iterator, std::ostringstream> chunked_parser(
        parser.req());
    std::ostringstream result_stream;
    chunked_parser(body_start, body_start, REQ.end(), result_stream);
    if (!chunked_parser.is_finished()) FAILED_MESSAGE

    if (result_stream.str() != "123456789012345678901234561234567890")
    {
        std::cerr << result_stream.str();
        FAILED_MESSAGE
    }
    return true;
}

bool test2()
{
    const string REQ =
        "OPTIONS /api/ping?par1=arg1&par2=arg2;par3&par4;par5=1 HTTP/15.143\r\nTransfer-Encoding: "
        "chunked\r\n\r\n1A;a1=v1;a2=v2\r\n12345678901234567890123456\r\nA\r\n1234567890\r\n0;a3="
        "v3\r\nX-Info: my_test\r\n\r\n";

    ymod_webserver::parser::request_parser<string::const_iterator> parser;
    parser.reset(ymod_webserver::context_ptr());
    string::const_iterator body_start = REQ.begin();
    body_start = parser(REQ.begin(), body_start, REQ.end());
    if (!parser.is_finished()) FAILED_MESSAGE
    ymod_webserver::transfer::chunked<string::const_iterator, std::ostringstream> chunked_parser(
        parser.req());
    std::ostringstream result_stream;
    chunked_parser(body_start, body_start, REQ.end(), result_stream);
    if (!chunked_parser.is_finished()) FAILED_MESSAGE

    if (result_stream.str() != "123456789012345678901234561234567890") FAILED_MESSAGE

    if (parser.req()->headers["x-info"] != "my_test") FAILED_MESSAGE

    return true;
}

int main()
{
    if (!test1()) return 1;
    if (!test2()) return 1;
    return 0;
}
