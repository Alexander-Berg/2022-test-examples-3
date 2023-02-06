#include <crypta/graph/rtmr/lib/common/validate.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

using namespace NCrypta::NGraph;

Y_UNIT_TEST_SUITE(Validate) {
    namespace {
        const TString VALID_BROWSER_INFO(
            "ti:10:vc:w:ilt:0JzQtdCx0LXQu9GM:ns:1574690798525:s:375x667x32:sk:2:adb:2:fpr:216613626101:cn:2:w:980x1544:"
            "z:600:i:20191126000639:et:1574690800:en:utf-8:c:1:la:ru-ru:pv:1:ls:1227983968052:rqn:4:rn:512275418:"
            "hid:383112761:ds:0,0,931,27,0,0,0,75,52,,,,1071:wn:40984:hl:3:gdpr:14:v:1747:wv:2:rqnl:1:st:1574690800:"
            "u:1574690780148878922:zz:1574143498360448903937877323134:pu:4154290908:"
            "fip:9cfb67bbfcc75549ad5789f86d6dbf20-7950ec0297c12322859860922e071362-a81f3b9bcdd80a361c14af38dc09b309-a81f3b9bcdd80a361c14af38dc09b309:"
            "t:Мебель"
        );
        const TString CY2_BROWSER_INFO(
            "ti:10:vc:w:ilt:0JzQtdCx0LXQu9GM:ns:1574690798525:s:375x667x32:sk:2:adb:2:fpr:216613626101:cn:2:w:980x1544:"
            "z:600:i:20191126000639:et:1574690800:en:utf-8:c:1:la:ru-ru:pv:1:ls:1227983968052:rqn:4:rn:512275418:"
            "hid:383112761:ds:0,0,931,27,0,0,0,75,52,,,,1071:wn:40984:hl:3:gdpr:14:v:1747:wv:2:rqnl:1:st:1574690800:"
            "u:1574690780148878922:zz:1574143498360448903937877323134:pu:4154290908:cy:2:"
            "fip:9cfb67bbfcc75549ad5789f86d6dbf20-7950ec0297c12322859860922e071362-a81f3b9bcdd80a361c14af38dc09b309-a81f3b9bcdd80a361c14af38dc09b309:"
            "t:Мебель"
        );
        const TString ROSTELECOM_BROWSER_INFO("nb:1:cl:528:ar:1:rt:1:gdpr:14:vf:12vwkywz4p6my78c6y:fp:2913:fu:1:en:utf-8:la:"
            "ru-ru:v:611:cn:1:dp:0:ls:197098018031:hid:272560633:z:180:i:20210819163545:et:1629380146:c:1"
            ":rn:291658671:rqn:2:u:1629380131617397677:w:414x829:s:414x896x32:sk:2:eu:0:ns:1629380127766:"
            "ds:,,,,,,,,,7116,7116,14,:wv:2:pp:3629563401:pu:17655341131629380131617397677:rqnl:1:ti:0:st:1629380146"
        );

        const TString VALID_COUNTER_ID("12");
        const TString VALID_REFERER("https://www.yandex.ru");
        const uatraits::detector::result_type VALID_TRAITS({
            {"ITP", "true"},
        });
        const TString VALID_DOMAIN_USER_ID("1574690780148878922");
        const TMaybe<TString> VALID_ORIGINAL_DOMAIN("ideanadom.ru");

        const TString BROWSER_INFO_WITH_FORCE_URL("fu:2:" + VALID_BROWSER_INFO);
        const TString BROWSER_INFO_WITH_REFERER_REPLACED("fu:1:" + VALID_BROWSER_INFO);
        const TString TECHNICAL_COUNTER_ID("3");
        const TString NONYANDEX_REFERER("https://www.google.com");
        const uatraits::detector::result_type ROBOT_TRAITS({
            {"ITP",     "true"},
            {"isRobot", "true"},
        });
        const TString NO_DOMAIN_USER_ID("0");
        const TMaybe<TString> YASTATIC_ORIGINAL_DOMAIN("yastatic.net");

        const NGeobase::TLookup& GetGeoData() {
            static const NGeobase::TLookup geoData(GetWorkPath() + "/geodata6.bin");
            return geoData;
        }

        const TGeoRegion FAR_EASTERN_FEDERAL_DISTRICT = 73;
        const TGeoRegion MADRID = 10435;
        const TGeoRegion USA = 84;

        const TString VALID_SSL_TICKET = "fccb285eb18fe0c748f3228f53f94b61";
        const TString UPPERCASED_SSL_TICKET = "FCCB285EB18FE0C748F3228F53F94B61";
        const TString EMPTY_SSL_TICKET = "";
        const TString NO_SSL_TICKET = "";
        const TString LONGER_SSL_TICKET = "fccb285eb18fe0c748f3228f53f94b611";
        const TString EXTRA_SYMBOL_SSL_TICKET = "fccb285eb18fe0c74zf3228f53f94b61";

        const TString GOOD_HITLOGID = "1234567890123456789";
        const TString LONG_HITLOGID = "12345678901234567891";
        const TString SHORT_HITLOGID = "123456789012345678";
        const TString BAD_HITLOGID = "123456789012345";
        const TString EMPTY_HITLOGID = "";
        const TString GOOD_YCLID = "1234567890123456789";
        const TString BAD_YCLID = "";
        const TString SHORT_YCLID = "12345678901";

        const TString IPV4_ADDRESS = "8.8.8.8";
        const TString IPV6_ADDRESS = "fe80::57a5:b2c7:fd1f:49e1";
        const TString IPV46_ADDRESS = "::ffff:192.0.2.128";
        const TString EMPTY = "";

        const TString PrivateRelayIp{"172.224.254.23"};
        const TString PrivateRelayv4Inv6{"::ffff:172.224.254.23"};
        const TString PrivateRelayip6{"2a04:4e41:002f:0071::1:1:12"};
        const TString IvalidPrivateRelayIp{"SWAG YOLO"};
        const TString NonPrivateRelayIp{"8.8.8.8"};
    }

    Y_UNIT_TEST(TestHitlogIdValidation) {
        UNIT_ASSERT(ValidateHitlogId(GOOD_HITLOGID));
        UNIT_ASSERT(ValidateHitlogId(LONG_HITLOGID));
        UNIT_ASSERT(ValidateHitlogId(SHORT_HITLOGID));
        UNIT_ASSERT(!ValidateHitlogId(BAD_HITLOGID));
        UNIT_ASSERT(!ValidateHitlogId(EMPTY_HITLOGID));
    }

    Y_UNIT_TEST(TestYClidValidation) {
        UNIT_ASSERT(ValidateYClid(GOOD_YCLID));
        UNIT_ASSERT(!ValidateYClid(BAD_YCLID));
        UNIT_ASSERT(!ValidateYClid(SHORT_YCLID));
        UNIT_ASSERT(!ValidateYClid(EMPTY));
    }

    Y_UNIT_TEST(ValidateBrowserInfo) {
        UNIT_ASSERT(ValidateBrowserInfo(VALID_BROWSER_INFO));
        UNIT_ASSERT(!ValidateBrowserInfo(BROWSER_INFO_WITH_FORCE_URL));
        UNIT_ASSERT(ValidateBrowserInfo(BROWSER_INFO_WITH_REFERER_REPLACED));
    }

    Y_UNIT_TEST(ValidateUATraitsPositive) {
        UNIT_ASSERT(ValidateUATraits(VALID_TRAITS));
    }

    Y_UNIT_TEST(ValidateUATraitsNegative) {
        UNIT_ASSERT(!ValidateUATraits(ROBOT_TRAITS));
    }

    Y_UNIT_TEST(ValidateDomainPositive) {
        UNIT_ASSERT(ValidateDomain(*VALID_ORIGINAL_DOMAIN));
    }

    Y_UNIT_TEST(ValidateDomainNegative) {
        UNIT_ASSERT(!ValidateDomain(*YASTATIC_ORIGINAL_DOMAIN));
    }

    Y_UNIT_TEST(IsCorrectDomainOptional) {
        TMaybe<TString> validDomain{"yandex.ru"};

        UNIT_ASSERT(!IsCorrectDomain(Nothing()));
        UNIT_ASSERT(IsCorrectDomain(validDomain));
    }

    Y_UNIT_TEST(ValidateYandexRefererPositive) {
        UNIT_ASSERT(ValidateYandexReferer(VALID_REFERER, VALID_ORIGINAL_DOMAIN));
    }

    Y_UNIT_TEST(ValidateYandexRefererNegative) {
        UNIT_ASSERT(!ValidateYandexReferer(NONYANDEX_REFERER, VALID_ORIGINAL_DOMAIN));
    }

    Y_UNIT_TEST(IsGDPRPositive) {
        UNIT_ASSERT(IsGDPR(GetGeoData(), USA));
        UNIT_ASSERT(IsGDPR(GetGeoData(), MADRID));
    }

    Y_UNIT_TEST(IsGDPRNegative) {
        UNIT_ASSERT(!IsGDPR(GetGeoData(), FAR_EASTERN_FEDERAL_DISTRICT));
    }

    Y_UNIT_TEST(TestPrivateRelayDetection) {
        UNIT_ASSERT(IsPrivateRelayIp(GetGeoData(), PrivateRelayIp));
        UNIT_ASSERT(IsPrivateRelayIp(GetGeoData(), PrivateRelayv4Inv6));
        UNIT_ASSERT(IsPrivateRelayIp(GetGeoData(), PrivateRelayip6));
        UNIT_ASSERT(!IsPrivateRelayIp(GetGeoData(), IvalidPrivateRelayIp));
        UNIT_ASSERT(!IsPrivateRelayIp(GetGeoData(), NonPrivateRelayIp));
    }

    Y_UNIT_TEST(IsRostelecom) {
        UNIT_ASSERT(!IsRostelecomExtfp(VALID_BROWSER_INFO));
        UNIT_ASSERT(!IsRostelecomExtfp(CY2_BROWSER_INFO));
        UNIT_ASSERT(IsRostelecomExtfp(ROSTELECOM_BROWSER_INFO));
    }

    Y_UNIT_TEST(ShouldOneSaveYuid) {
        UNIT_ASSERT(!ShouldSaveYuid(""));
        UNIT_ASSERT(ShouldSaveYuid(VALID_BROWSER_INFO));
        UNIT_ASSERT(ShouldSaveYuid("cy:0"));
        UNIT_ASSERT(!ShouldSaveYuid("cy:1"));
        UNIT_ASSERT(!ShouldSaveYuid(CY2_BROWSER_INFO));
        UNIT_ASSERT(ShouldSaveYuid("cy:somestangevalue"));
    }

    Y_UNIT_TEST(ValidateSslSessionTicketIV) {
        UNIT_ASSERT(ValidateSslSessionTicketIV(VALID_SSL_TICKET));
        UNIT_ASSERT(ValidateSslSessionTicketIV(UPPERCASED_SSL_TICKET));
        UNIT_ASSERT(!ValidateSslSessionTicketIV(EMPTY_SSL_TICKET));
        UNIT_ASSERT(!ValidateSslSessionTicketIV(NO_SSL_TICKET));
        UNIT_ASSERT(!ValidateSslSessionTicketIV(LONGER_SSL_TICKET));
        UNIT_ASSERT(!ValidateSslSessionTicketIV(EXTRA_SYMBOL_SSL_TICKET));
    }

    Y_UNIT_TEST(ValidateAddress46) {
        UNIT_ASSERT(ValidateAddress4(IPV4_ADDRESS));
        UNIT_ASSERT(!ValidateAddress4(IPV46_ADDRESS));
        UNIT_ASSERT(!ValidateAddress4(EMPTY));
        UNIT_ASSERT(!ValidateAddress4(VALID_REFERER));
        UNIT_ASSERT(ValidateAddress6(IPV6_ADDRESS));
        UNIT_ASSERT(ValidateAddress6(IPV46_ADDRESS));
        UNIT_ASSERT(!ValidateAddress6(EMPTY));
        UNIT_ASSERT(!ValidateAddress6(VALID_REFERER));
    }
    Y_UNIT_TEST(ValidateAddress) {
        UNIT_ASSERT(ValidateAddress(IPV4_ADDRESS));
        UNIT_ASSERT(ValidateAddress(IPV6_ADDRESS));
        UNIT_ASSERT(ValidateAddress(IPV46_ADDRESS));
        UNIT_ASSERT(!ValidateAddress(EMPTY));
        UNIT_ASSERT(!ValidateAddress(VALID_REFERER));
    }

    Y_UNIT_TEST(ValidateDomain) {
        UNIT_ASSERT(IsYandexDomain("ya.ru"));
        UNIT_ASSERT(IsYandexDomain("yandex.ru"));
        UNIT_ASSERT(IsYandexDomain("yandex.com"));
        UNIT_ASSERT(IsYandexDomain("yandex.com.ru"));
        UNIT_ASSERT(IsYandexDomain("yandex.by"));
        UNIT_ASSERT(IsYandexDomain("yandex.kz"));
        UNIT_ASSERT(IsYandexDomain("yandex.fr"));
        UNIT_ASSERT(IsYandexDomain("yandex.md"));
        UNIT_ASSERT(IsYandexDomain("yandex.com.am"));
        UNIT_ASSERT(IsYandexDomain("yandex.ge"));

        UNIT_ASSERT(!IsYandexDomain("ya.py"));
        UNIT_ASSERT(!IsYandexDomain("ya.ry"));
        UNIT_ASSERT(!IsYandexDomain("xyandex.ru"));
        UNIT_ASSERT(!IsYandexDomain("not-a-yandex.ru"));
        UNIT_ASSERT(!IsYandexDomain("not-a-yandex.by"));
        UNIT_ASSERT(!IsYandexDomain("yandex-yandex.kz"));
        UNIT_ASSERT(!IsYandexDomain("taksi-yandex.md"));

        UNIT_ASSERT(IsYandexDomain("www.yandex.ru"));
        UNIT_ASSERT(IsYandexDomain("zen.yandex.ru"));
        UNIT_ASSERT(IsYandexDomain("maps.yandex.com"));
        UNIT_ASSERT(IsYandexDomain("crypta.yandex.com.ru"));
        UNIT_ASSERT(IsYandexDomain("taxi.yandex.by"));
        UNIT_ASSERT(IsYandexDomain("eda.yandex.kz"));
        UNIT_ASSERT(IsYandexDomain("lavka.yandex.fr"));
        UNIT_ASSERT(IsYandexDomain("market.yandex.md"));
        UNIT_ASSERT(IsYandexDomain("pokupki.market.yandex.com.am"));
        UNIT_ASSERT(IsYandexDomain("fishing.at.yandex.ge"));
    }
}
