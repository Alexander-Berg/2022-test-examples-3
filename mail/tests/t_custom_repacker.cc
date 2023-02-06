#include <yxiva/core/repacker.h>
#include <catch.hpp>

using namespace yxiva;

TEST_CASE("custom_repacker/from_json", "")
{
    std::ifstream test_data_file("repacker_parser.json");
    std::stringstream test_data_raw;
    test_data_raw << test_data_file.rdbuf();
    test_data_file.close();
    json_value test_data = json_parse(test_data_raw.str());

    for (auto itest_case = test_data.members_begin(); itest_case != test_data.members_end();
         ++itest_case)
    {
        SECTION(string(itest_case.key()))
        {
            auto&& test_case = *itest_case;
            std::string src;
            if (test_case["repack"].is_string()) src = test_case["repack"].to_string();
            else
                src = json_write(test_case["repack"]);

            custom_repacker repack;
            auto parse_result = custom_repacker::from_json_string(src, repack);
            REQUIRE(parse_result == test_case["expect"]["parsed"].to_bool());
        }
    }
}
