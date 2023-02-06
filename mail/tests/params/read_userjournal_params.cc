#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "parse.h"


using namespace testing;

namespace sendbernar {
namespace tests {

TEST(UserJournalParams, with_icookie_shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_HEADER("connection_id", "connectionId");
    RETURN_HEADER("X-Yandex-ExpBoxes", "expBoxes");
    RETURN_HEADER("X-Yandex-EnabledExpBoxes", "enabledExpBoxes");
    RETURN_HEADER("X-Yandex-ClientType", "clientType");
    RETURN_HEADER("X-Yandex-ClientVersion", "clientVersion");
    RETURN_HEADER("User-Agent", "userAgent");
    RETURN_HEADER("yandexuid", "yandexuid");
    RETURN_HEADER("icookie", "iCookie");

    const auto params = getUserJournalParams(REQ);

    EXPECT_EQ(params.connectionId, "connectionId");
    EXPECT_EQ(params.expBoxes, "expBoxes");
    EXPECT_EQ(params.enabledExpBoxes, "enabledExpBoxes");
    EXPECT_EQ(params.clientType, "clientType");
    EXPECT_EQ(params.clientVersion, "clientVersion");
    EXPECT_EQ(params.userAgent, "userAgent");
    EXPECT_EQ(params.yandexUid, "yandexuid");
    EXPECT_EQ(params.iCookie, "iCookie");
}

TEST(UserJournalParams, without_icookie_shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_HEADER("connection_id", "connectionId");
    RETURN_HEADER("X-Yandex-ExpBoxes", "expBoxes");
    RETURN_HEADER("X-Yandex-EnabledExpBoxes", "enabledExpBoxes");
    RETURN_HEADER("X-Yandex-ClientType", "clientType");
    RETURN_HEADER("X-Yandex-ClientVersion", "clientVersion");
    RETURN_HEADER("User-Agent", "userAgent");
    RETURN_HEADER("yandexuid", "yandexuid");
    RETURN_HEADER_OPT_EMPTY("icookie");

    const auto params = getUserJournalParams(REQ);

    EXPECT_EQ(params.connectionId, "connectionId");
    EXPECT_EQ(params.expBoxes, "expBoxes");
    EXPECT_EQ(params.enabledExpBoxes, "enabledExpBoxes");
    EXPECT_EQ(params.clientType, "clientType");
    EXPECT_EQ(params.clientVersion, "clientVersion");
    EXPECT_EQ(params.userAgent, "userAgent");
    EXPECT_EQ(params.yandexUid, "yandexuid");
    EXPECT_EQ(params.iCookie, "");
}

}
}
