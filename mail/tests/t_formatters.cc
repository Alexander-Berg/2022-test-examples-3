#include "web/formatters/json.h"
#include "yxiva/core/json.h"
#include "yxiva/core/quote_xml.h"
#include "yxiva/core/message.h"
#include <catch.hpp>
#include <sstream>

namespace yxiva {

TEST_CASE("formatters/json/simple", "")
{
    message msg;
    user_info ui;

    msg.uid = user_id("123456000");
    msg.service = "mail";
    msg.data["key1"] = "value1";
    msg.data["key2"] = "value2";
    msg.data["key3"] = "value3";

    std::ostringstream ostr;
    formatters::json formatter;
    formatter(ui, msg, "context-id-here", ostr);

    //    std::cout << ostr.str () << "\n";
}

TEST_CASE("formatters/json/special_sharacters", "test escaping 0x00..0x1F")
{
    message msg;
    user_info ui;

    string bad_str = "";
    for (int i = 0x1F; i > 0; i--)
    {
        bad_str += static_cast<char>(i);
        bad_str += "ok";
    }

    msg.data[bad_str] = bad_str;

    std::ostringstream ostr;
    formatters::json formatter;
    formatter(ui, msg, "context-id-here", ostr);

    string raw = ostr.str();
    //    std::cout << "raw json: '" << raw << "'\n";
    for (auto it = raw.begin(); it != raw.end(); it++)
    {
        int char_num = static_cast<int>(*it);
        REQUIRE(char_num > 0x1F);
    }
}

TEST_CASE("formatters/json/try_parse", "")
{
    message msg;
    user_info ui;

    string bad_str = "http://some/unknown/url";
    msg.data[bad_str] = bad_str;
    msg.data["что-то\1 \2в utf-8"] = "что\3-то \rеще";

    std::ostringstream ostr;
    formatters::json formatter;
    formatter(ui, msg, "context-id-here", ostr);

    string raw = ostr.str();

    json_value root;
    REQUIRE(!root.parse(raw));

    //    std::cout << root << std::endl;
}

}
