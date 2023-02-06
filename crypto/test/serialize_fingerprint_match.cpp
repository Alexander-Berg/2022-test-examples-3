#include <crypta/graph/rtmr/lib/common/serialize_fingerprint_match.h>

#include <crypta/lib/native/time/shifted_clock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NGraph;

Y_UNIT_TEST_SUITE(SerializeFingerprintMatch) {
    Y_UNIT_TEST(Positive) {
        const TString yuid = "yandexuid";

        TYuidMessage yuidMessage;
        yuidMessage.SetTimestamp(100);
        yuidMessage.SetYandexuid(yuid);

        TParsedBsWatchRow bsWatchRow;
        bsWatchRow.SetOSFamily("iOS");
        bsWatchRow.SetOSVersion("1.2.3");
        bsWatchRow.SetBrowserName("Firefox");
        bsWatchRow.SetBrowserVersion("4.5.6");
        bsWatchRow.SetFpc("fpc");
        bsWatchRow.SetOriginalDomain("www.ya.ru");
        bsWatchRow.SetDomain("ya.ru");
        bsWatchRow.SetTimestamp(200);

        TShiftedClock::FreezeTimestamp(1587639158);

        const TString reference(R"JSON({"browser":"Firefox","browser_version":"4.5.6","domain":"ya.ru","domain_cookie":"fpc","original_domain":"www.ya.ru","osversion":"1.2.3","osfamily":"iOS","timestamp":200,"yuid":"yandexuid","source":"test","current_ts":1587639158})JSON");

        UNIT_ASSERT_STRINGS_EQUAL(reference, SerializeFingerprintMatch(yuidMessage, bsWatchRow, "test"));
        UNIT_ASSERT_STRINGS_EQUAL(reference, SerializeMatch(yuid, bsWatchRow, "test"));
    }
}
