#include <crypta/lib/native/http/request_parser/request_parser.h>

#include <library/cpp/cgiparam/cgiparam.h>
#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NHttp;

TEST(NRequestParser, ParseCgi) {
    TRequest request;
    NRequestParser::Parse(request, TCgiParameters("subclient=crypta&yandexuid=123"));

    ASSERT_EQ("crypta", request.Subclient);
}
