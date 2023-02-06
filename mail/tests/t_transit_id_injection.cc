#include "../src/convey/settings.h"
#include <catch.hpp>

TEST_CASE("transit_id_injection/matching", "")
{
    yxiva::hub::experiments_settings st;
    st.transit_id_injection.app_blacklist = { "apns:ru.yandex.mail", "fcm:ru.yandex.disk" };

    auto& rules = st.transit_id_injection;
    REQUIRE(rules.match("apns:ru.yandex.money", "1") == true);
    REQUIRE(rules.match("apns:ru.yandex.mail", "1") == false);

    st.transit_id_injection.uids_whitelist = { { "apns:ru.yandex.mail", { "1" } } };

    REQUIRE(rules.match("apns:ru.yandex.mail", "1") == true);
    REQUIRE(rules.match("apns:ru.yandex.mail", "2") == false);

    st.transit_id_injection.experiment = { { "fcm:ru.yandex.disk", 10 } }; // 1%

    REQUIRE(rules.match("fcm:ru.yandex.disk", "1") == true);
    REQUIRE(rules.match("fcm:ru.yandex.disk", "10") == false);
    REQUIRE(rules.match("fcm:ru.yandex.disk", "90000") == false);

    REQUIRE(rules.match("apns:ru.yandex.disk", "1") == true);
}
