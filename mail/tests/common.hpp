#ifndef RECIPIENT_PARSER_TESTS_DEBUGGING_TEST_HPP_
#define RECIPIENT_PARSER_TESTS_DEBUGGING_TEST_HPP_

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <type_traits>

namespace rcpt_parser {

class PrinterTest : public ::testing::Test {
public:
    PrinterTest() {
        buffer.str( std::string() ); // clears the buffer.
        sbuf = std::cerr.rdbuf();
        std::cerr.rdbuf( buffer.rdbuf() );
    }

    virtual ~PrinterTest() {
        std::cerr.rdbuf( sbuf );
        const ::testing::TestInfo* const test_info =
                ::testing::UnitTest::GetInstance()->current_test_info();
        if ( test_info && test_info->result()->Failed() ) {
            std::cerr << std::endl << "Captured output from "
                    << test_info->test_case_name()
                    << " is:"
                    << std::endl
                    << buffer.str()
                    << std::endl;
        }
    }

    std::stringstream buffer;
    std::streambuf* sbuf;
};

template<typename Result>
struct ParserParams {
    std::string input, unparsed;
    Result result;
    ParserParams(std::string input, Result result, std::string unparsed = std::string())
            : input(input), unparsed(unparsed), result(result) {}

    template<typename T>
    ParserParams(
            T&& input,
            typename std::enable_if<
                !std::is_convertible<T, ParserParams>::value
            >::type* = 0,       //catches copy-constructor otherwise
            typename std::enable_if<
                std::is_constructible<Result, T>::value
            >::type* = 0)       //if result(input) is possible
        : input(input), result{input} {}

    template<typename T>
    ParserParams(
            T&& input,
            typename std::enable_if<
                !std::is_convertible<T, ParserParams>::value
            >::type* = 0,       //catches copy-constructor otherwise
            typename std::enable_if<
                !std::is_constructible<Result, T>::value
            >::type* = 0)       //if result(input) is impossible
        : input(input) {}
};

template<typename Result>
inline std::ostream& operator<<(std::ostream& out, const ParserParams<Result>& params) {
    out << "ParserParams(";
            out <<     "input=\""    << params.input;
            out << "\", result=\""   << params.result;
            out << "\", unparsed=\"" << params.unparsed;
            out << "\")";
    return out;
}

using SParserParams = ParserParams<std::string>;

template<typename Result>
struct ParserTest : PrinterTest, ::testing::WithParamInterface<ParserParams<Result>> {
    using Params = ParserParams<Result>;

    virtual ~ParserTest() {}

    template<typename ParseFunc>
    void test_parser(ParseFunc f) {
        return test_parser(f, this->GetParam());
    }

    template<typename ParseFunc>
    void test_parser(ParseFunc f, const Params& params) {
        const std::string& input = params.input;
        Result parsed;

        auto result = f(input, parsed);
        auto success = result.first;
        auto unparsed = std::string(result.second, input.end());

        ASSERT_TRUE(success);
        ASSERT_EQ(unparsed, params.unparsed);
        ASSERT_EQ(parsed, params.result);
    }

    template<typename ParseFunc>
    void test_parser_fails(ParseFunc f) {
        test_parser_fails(f, this->GetParam());
    }

    template<typename ParseFunc>
    void test_parser_fails(ParseFunc f, const Params& params) {
        const auto input = params.input;
        Result result;
        ASSERT_FALSE(f(input, result).first);
    }
};

using SParserTest = ParserTest<std::string>;

}

#endif /* RECIPIENT_PARSER_TESTS_DEBUGGING_TEST_HPP_ */
