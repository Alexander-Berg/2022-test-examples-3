#include <iostream>
#include <sstream>
#include <http_parser/chunked.h>

using namespace http_parser;

#define FAILED_MESSAGE                                                                             \
    {                                                                                              \
        std::cerr << "[FAILED] " << __FILE__ << ":" << __LINE__ << "\n";                           \
        return false;                                                                              \
    }

bool test1()
{
    const string REQ = "1A\r\n12345678901234567890123456\r\nA\r\n1234567890\r\n0\r\n\r\n";

    string::const_iterator body_start = REQ.begin();
    chunked<string::const_iterator, std::ostringstream> chunked_parser;
    std::ostringstream result_stream;
    auto consume = chunked_parser(body_start, body_start, REQ.end(), result_stream);
    if (!chunked_parser.is_finished()) FAILED_MESSAGE
    if (consume != REQ.end()) FAILED_MESSAGE
    if (result_stream.str() != "123456789012345678901234561234567890")
    {
        std::cerr << result_stream.str();
        FAILED_MESSAGE
    }
    return true;
}

bool test2()
{
    const string REQ = "1A;a1=v1;a2=v2\r\n12345678901234567890123456\r\nA\r\n1234567890\r\n0;a3="
                       "v3\r\nX-Info: my_test\r\n\r\n";

    string::const_iterator body_start = REQ.begin();
    chunked<string::const_iterator, std::ostringstream> chunked_parser;
    std::ostringstream result_stream;
    auto consume = chunked_parser(body_start, body_start, REQ.end(), result_stream);
    if (!chunked_parser.is_finished()) FAILED_MESSAGE
    if (consume != REQ.end()) FAILED_MESSAGE
    if (result_stream.str() != "123456789012345678901234561234567890") FAILED_MESSAGE

    // if (parser.req()->headers["x-info"] != "my_test") FAILED_MESSAGE

    return true;
}

bool test_single_chunk_not_in_one_read()
{
    typedef string::iterator iterator_t;
    string REQ;
    auto begin = REQ.begin();
    auto saved = begin;
    auto end = REQ.end();
    std::size_t offset = 0;
    chunked<iterator_t, std::ostringstream> chunked_parser;
    std::ostringstream result_stream;

    auto parse_helper = [&](const char* data) {
        REQ.append(data);
        begin = REQ.begin();
        saved = begin + offset;
        end = REQ.end();
        begin = chunked_parser(begin, saved, end, result_stream);
        offset = saved - begin;
        REQ.erase(REQ.begin(), begin);
    };

    parse_helper("1A\r\n1234567890123456789");
    if (chunked_parser.is_finished()) FAILED_MESSAGE

    parse_helper("0123456");
    if (chunked_parser.is_finished()) FAILED_MESSAGE

    parse_helper("\r\n");
    if (chunked_parser.is_finished()) FAILED_MESSAGE

    parse_helper("0\r\n");
    if (chunked_parser.is_finished()) FAILED_MESSAGE

    parse_helper("\r\n");
    if (!chunked_parser.is_finished()) FAILED_MESSAGE
    if (result_stream.str() != "12345678901234567890123456") FAILED_MESSAGE

    return true;
}

bool test_output_to_string()
{
    const string REQ = "1A;a1=v1;a2=v2\r\n12345678901234567890123456\r\nA\r\n1234567890\r\n0;a3="
                       "v3\r\nX-Info: my_test\r\n\r\n";

    string::const_iterator body_start = REQ.begin();
    chunked<string::const_iterator, string> chunked_parser;
    string result_str;
    auto consume = chunked_parser(body_start, body_start, REQ.end(), result_str);
    if (!chunked_parser.is_finished()) FAILED_MESSAGE
    if (consume != REQ.end()) FAILED_MESSAGE
    if (result_str != "123456789012345678901234561234567890") FAILED_MESSAGE

    return true;
}

int main()
{
    if (!test1()) return 1;
    if (!test2()) return 1;
    if (!test_single_chunk_not_in_one_read()) return 1;
    if (!test_output_to_string()) return 1;
    return 0;
}
