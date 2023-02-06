#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <butil/http/headers.h>


using namespace testing;
using namespace http;

namespace {

TEST(HeadersAddMethod, shouldAddHeader) {
    headers h;
    h.add("name", "val");

    EXPECT_EQ(h.headers["name"][0], "val");
    EXPECT_EQ(h.headers["name"].size(), 1ul);
}

TEST(HeadersAddMethod, shouldAddHeaderWithSeveralValues) {
    headers h;
    h.add("name", "val");
    h.add("name", "val2");

    EXPECT_THAT(h.headers["name"], UnorderedElementsAre("val", "val2"));
    EXPECT_EQ(h.headers["name"].size(), 2ul);
}

TEST(HeadersAddMethod, shouldThrowAnExpectionOnEmptyName) {
    headers h;
    EXPECT_THROW(h.add("", "val"), std::invalid_argument);
}

TEST(HeadersAddMethod, shouldNotThrowAnExpectionOnEmptyValue) {
    headers h;
    h.add("name", "");

    EXPECT_EQ(h.headers["name"][0], "");
    EXPECT_EQ(h.headers["name"].size(), 1ul);
}

TEST(HeadersFormatMethod, shouldReturnFormattedString) {
    headers h;
    h.add("name", "value");
    h.add("name2", "value2");

    const std::string formattedString = "name: value\r\nname2: value2\r\n";

    EXPECT_EQ(formattedString, h.format());
}

TEST(HeadersFlattenMethod, shouldReturnFlatFormattedString) {
    headers h;
    h.add("name", "value");
    h.add("name", "value2");
    h.add("name2", "value2");

    EXPECT_THAT(h.flatten(), UnorderedElementsAre(std::make_pair("name", "value"),
                                                  std::make_pair("name2", "value2")));
}


}
