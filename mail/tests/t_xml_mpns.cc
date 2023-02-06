#include "mod_mpns/convert.h"
#include "read_json.h"
#include <catch.hpp>

using namespace yxiva;
using namespace mobile;

wns_notification_type::type name_to_mpns_type(const string_view& name)
{
    if (name == "toast") return wns_notification_type::toast;
    else if (name == "tile")
        return wns_notification_type::tile;
    else if (name == "raw")
        return wns_notification_type::raw;
    else
        FAIL("wrong type");
    // for fail case
    return wns_notification_type::COUNT;
}

TEST_CASE("mpns/xml", "")
{
    auto input = read_json("mpns_data_input.json");
    auto expected = read_json("mpns_expected_output.json");

    for (auto field = input.members_begin(); field != input.members_end(); ++field)
    {
        auto name = field.key();
        auto type = name_to_mpns_type(name);
        auto section_expected = expected[name];
        for (auto test_case = (*field).members_begin(); test_case != (*field).members_end();
             ++test_case)
        {
            auto case_name = test_case.key();
            auto case_result = operation::result(section_expected[case_name]["result"].to_string());
            auto case_data_expected = section_expected[case_name]["data"].to_string();
            INFO("test section " << name << ", test case " << case_name);
            string xml, case_data;
            auto&& val = *test_case;
            if (val.is_string())
            {
                case_data = val.to_string();
            }
            else
            {
                case_data = val.stringify();
            }
            auto res = convert_mpns(case_data, type, xml);
            CHECK(res.success() == case_result.success());
            if (res)
            {
                CHECK(xml == case_data_expected);
            }
            else
            {
                CHECK(res.error_reason == case_result.error_reason);
            }
        }
    }
}