#include "helpers.h"

#include <crypta/styx/services/common/data/test_helpers.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/datetime/base.h>

using namespace NCrypta::NStyx;
using namespace NCrypta::NStyx::NTest;

namespace {
    const ui64 MIN_DELETE_INTERVAL_SEC = 30;
    const auto MIN_DELETE_INTERVAL = TDuration::Seconds(MIN_DELETE_INTERVAL_SEC);
    const ui64 PUID = 4ull * 1024 * 1024 * 1024;
    const TString FIRST_OBFUSCATED = "364206ed5dbce8dee8fa77ae5e22b0a2";
    const TString SECOND_OBFUSCATED = "e474d974c5795c958a8d21c73b7f7e29";
}

const auto FIRST_DELETE_TIMESTAMP = TInstant::Seconds(MIN_DELETE_INTERVAL_SEC);
const auto SECOND_DELETE_TIMESTAMP = TInstant::Seconds(MIN_DELETE_INTERVAL_SEC + 1);

const auto& FIRST_SINGLE_OBLIVION_EVENT_SERIES = GetOblivionEvents({
    CreateOblivionEvent(FIRST_DELETE_TIMESTAMP, FIRST_OBFUSCATED)
});
const auto& SECOND_SINGLE_OBLIVION_EVENT_SERIES = GetOblivionEvents({
    CreateOblivionEvent(SECOND_DELETE_TIMESTAMP, SECOND_OBFUSCATED)
});

TEST(TDbState, Delete) {
    auto dbState = GetEmptyDbState(MIN_DELETE_INTERVAL);
    EXPECT_TRUE(dbState.GetPuidStates().empty());

    dbState.DeletePuid(PUID, FIRST_DELETE_TIMESTAMP - TDuration::Seconds(1));
    EXPECT_TRUE(dbState.GetPuidStates().empty());

    dbState.DeletePuid(PUID, FIRST_DELETE_TIMESTAMP);
    const auto& puidStates = dbState.GetPuidStates();
    EXPECT_EQ(1u, puidStates.size());

    const auto& puidState = puidStates.at(PUID);
    EXPECT_EQ(PUID, puidState.GetPuid());

    const auto& oblivionEvents = puidState.GetOblivionEvents();
    EXPECT_EQ(FIRST_SINGLE_OBLIVION_EVENT_SERIES, oblivionEvents);
}

TEST(TDbState, DeleteRepeated) {
    {
        auto dbState = GetEmptyDbState(MIN_DELETE_INTERVAL);
        dbState.DeletePuid(PUID, FIRST_DELETE_TIMESTAMP);
        dbState.DeletePuid(PUID, SECOND_DELETE_TIMESTAMP);

        const auto& puidStates = dbState.GetPuidStates();
        EXPECT_EQ(1u, puidStates.size());

        const auto& puidState = puidStates.at(PUID);
        EXPECT_EQ(SECOND_SINGLE_OBLIVION_EVENT_SERIES, puidState.GetOblivionEvents());
    }

    {
        auto dbState = GetEmptyDbState(MIN_DELETE_INTERVAL);
        dbState.DeletePuid(PUID, SECOND_DELETE_TIMESTAMP);
        dbState.DeletePuid(PUID, FIRST_DELETE_TIMESTAMP);

        EXPECT_EQ(SECOND_SINGLE_OBLIVION_EVENT_SERIES, dbState.GetPuidStates().at(PUID).GetOblivionEvents());
    }
}
