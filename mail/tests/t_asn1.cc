#include "../src/asn1/time.h"
#include <catch.hpp>
#include <vector>
#include <string>

using namespace yxiva::asn1;

struct test_data
{
    std::string time;
    int type;
    std::string result;
    int64_t ts;
};

const std::vector<test_data> CASES = {
    { "190427151532Z", V_ASN1_UTCTIME, "", 1556378132 },
    { "190427151532-0301", V_ASN1_UTCTIME, "", 1556388992 },
    { "190427151532+0300", V_ASN1_UTCTIME, "", 1556367332 },
    { "1904271515Z", V_ASN1_UTCTIME, "", 1556378100 },
    { "1904271515-0301", V_ASN1_UTCTIME, "", 1556388960 },
    { "1904271515+0301", V_ASN1_UTCTIME, "", 1556367240 },
    { "5004271515Z", V_ASN1_UTCTIME, "", -621074700 }, // Must be treated as 1950.
    { "8004271515Z", V_ASN1_UTCTIME, "", 325696500 },  // Must be treated as 1980.
    { "4904271515Z", V_ASN1_UTCTIME, "", 2503149300 }, // Must be treated as 2049.
    { "20490427151532Z", V_ASN1_GENERALIZEDTIME, "unsupported time format", 0 },
    { "49042715Z", V_ASN1_UTCTIME, "unknown format", 0 },
    { "49042715151617Z", V_ASN1_UTCTIME, "unknown format", 0 },
    { "190DEF151532Z", V_ASN1_UTCTIME, "conversion failed", 0 },
};

ASN1_TIME init_time(const test_data& data)
{
    return { static_cast<int>(data.time.size()),
             data.type,
             reinterpret_cast<unsigned char*>(const_cast<char*>(data.time.data())),
             0 };
}

TEST_CASE("asn1/time/time_conversion")
{
    for (auto& data : CASES)
    {
        int64_t ts;
        auto time = init_time(data);
        auto res = convert_asn1time(&time, ts);
        INFO(data.time);
        CHECK(res.error_reason == data.result);
        if (res && res.error_reason == data.result)
        {
            CHECK(ts == data.ts);
        }
    }
}
