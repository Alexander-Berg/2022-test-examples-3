#include "utils.h"

#include <crypta/cm/services/common/data/id.h>
#include <crypta/cm/services/common/db_state/get_changes_batch.h>
#include <crypta/cm/services/common/serializers/id/string/id_string_serializer.h>
#include <crypta/cm/services/common/serializers/match/record/match_record_serializer.h>
#include <crypta/cm/services/mutator/lib/handlers/touch_handler.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NCm;
using namespace NCrypta::NCm::NMutator;
using namespace NCrypta::NCm::NMutator::NTest;

namespace {
    const TId EXT_ID("ext", "1");
    const auto& TTL_CONFIG = GetTtlConfig();

    TStats STATS("touch", TStats::TSettings());

    TTouchHandler GetHandler(TTouchCommand command) {
        InitLogs();
        return TTouchHandler(TInstant::Now(), std::move(command), TTL_CONFIG, STATS);
    }

    const auto& TEST_TOUCH = [](TDbState& dbState, TInstant touch, bool changeExpected, TDuration expectedTtl = TDuration::Zero()) {
        auto handler = GetHandler({TTouchCommand(EXT_ID.Value, EXT_ID, touch)});

        handler.UpdateDbState(dbState);

        const auto& changesBatch = GetChangesBatch(dbState);

        if (changeExpected) {
            ASSERT_EQ(
                THashSet<TString>({NIdSerializer::ToString(EXT_ID)}),
                GetSetOfKeys(changesBatch.RecordsToUpdate)
            );

            const auto& matchToWrite = NMatchSerializer::FromRecord(changesBatch.RecordsToUpdate[0]);
            ASSERT_EQ(expectedTtl, matchToWrite.GetTtl());
            ASSERT_EQ(touch, matchToWrite.GetTouch());
        } else {
            ASSERT_EQ(0u, changesBatch.RecordsToUpdate.size());
            ASSERT_EQ(0u, changesBatch.KeysToDelete.size());
        }
    };
}

TEST(TTouchHandler, EmptyStateIsNotUpdated) {
    TDbState dbState({}, {}, TRACKED_BACK_REFERENCES);
    TEST_TOUCH(dbState, TOUCH_TS, false);
}

TEST(TTouchHandler, TouchTimeoutIsNotReached) {
    auto dbState = NTest::GetDbState(EXT_ID, TId("yuid", "1"), TOUCH_TS, DEFAULT_TTL, TRACKED_BACK_REFERENCES);
    TEST_TOUCH(dbState, TOUCH_TS + TOUCH_TIMEOUT, false);
}

TEST(TTouchHandler, TouchTimeoutElapsed) {
    auto dbState = NTest::GetDbState(EXT_ID, TId("yuid", "1"), TOUCH_TS, DEFAULT_TTL, TRACKED_BACK_REFERENCES);
    TEST_TOUCH(dbState, TOUCH_TS + TOUCH_TIMEOUT + TDuration::Seconds(1), true, DEFAULT_TTL);
}

TEST(TTouchHandler, ExtendTtlTimeoutElapsed) {
    auto dbState = NTest::GetDbState(EXT_ID, TId("yuid", "1"), TOUCH_TS, DEFAULT_TTL, TRACKED_BACK_REFERENCES);
    TEST_TOUCH(dbState, TOUCH_TS + EXTEND_TTL_TIMEOUT + TDuration::Seconds(1), true, EXTENDED_TTL);
}
