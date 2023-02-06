#include <crypta/lib/native/fingerprint/fingerprint.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta;

Y_UNIT_TEST_SUITE(NFingerprint) {
    Y_UNIT_TEST(Ipv4) {
        const auto& fingerprint = NFingerprint::CalculateFingerprint(
                "http%253A%252F%252Ffrontend.vh.yandex.ru%252Fplayer%252F412281911886479781",
                Ip4Or6FromString("145.255.21.73"),
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36 OPR/63.0.3368.94"
        );
        UNIT_ASSERT_STRINGS_EQUAL("12669173260552367902:7737714067419288846", fingerprint);
    }

    Y_UNIT_TEST(Ipv6) {
        const auto& fingerprint = NFingerprint::CalculateFingerprint(
                "https://static.ngs.ru/news/preview/f840dc0082b343b10d87ab480384999c0eaccd8e_709_473_c.jpg",
                Ip4Or6FromString("2a00:1fa0:6e3:c272:3939:40bc:eda:e24e"),
                "Mozilla/5.0 (Linux; U; Android 7.1.2; en-us; Redmi 5A Build/N2G47H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/9.2.5"
        );
        UNIT_ASSERT_STRINGS_EQUAL("1326957309734237178:14150240432335983958", fingerprint);
    }
}
