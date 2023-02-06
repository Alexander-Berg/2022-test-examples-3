#include "utils.h"

#include <crypta/cm/services/common/db_state/get_changes_batch.h>
#include <crypta/cm/services/common/serializers/id/string/id_string_serializer.h>
#include <crypta/cm/services/common/serializers/match/record/match_record_serializer.h>
#include <crypta/cm/services/mutator/lib/handlers/upload_handler.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/vector.h>

using namespace NCrypta::NCm;
using namespace NCrypta::NCm::NMutator;
using namespace NCrypta::NCm::NMutator::NTest;

namespace {
    const TId EXT_ID("ext", "1");
    const TId EXT_ID_CUSTOM(TAG_CUSTOM, "1");
    const TId YUID("yuid", "3");
    const TId ICOOKIE("icookie", "4");
    const TId YUID2("yuid", "5");

    const auto& TTL_CONFIG = GetTtlConfig();

    TStats STATS("upload", TStats::TSettings());

    class TStubProducer : public NCrypta::NPQ::IProducer {
        bool TryEnqueue(size_t partitioner, TString message) override {
            Y_UNUSED(partitioner);
            return TryEnqueue(message);
        }

        bool TryEnqueue(TString message) override {
            Y_UNUSED(message);
            return true;
        }
    };

    TStubProducer PRODUCER;

    TUploadHandler GetHandler(TUploadCommand command) {
        InitLogs();
        return TUploadHandler(TInstant::Now(), std::move(command), TRACKED_BACK_REFERENCES, TTL_CONFIG, PRODUCER, STATS);
    }

    TMatch GetIncomingMatch(const TId& extId, const TInstant touch) {
        return TMatch(extId, {}, touch);
    }

    const auto& TEST_UPDATE_TOUCH_AND_TTL = [](TInstant newTouch, bool isUpdateExpected, TInstant expectedTouch, TDuration expectedTtl) {
        auto state = NTest::GetDbState(EXT_ID, YUID, TOUCH_TS, DEFAULT_TTL, TRACKED_BACK_REFERENCES);

        TMatch sameMatch(*state.GetMatches().Get(EXT_ID));

        sameMatch.SetTouch(newTouch);
        auto handler = GetHandler({TUploadCommand(EXT_ID.Value, sameMatch)});
        handler.UpdateDbState(state);

        const auto& changesBatch = GetChangesBatch(state);
        ASSERT_EQ(0u, changesBatch.KeysToDelete.size());

        if (isUpdateExpected) {
            ASSERT_EQ(
                THashSet<TString>({NIdSerializer::ToString(EXT_ID)}),
                GetSetOfKeys(changesBatch.RecordsToUpdate)
            );

            const auto& matchToWrite = NMatchSerializer::FromRecord(changesBatch.RecordsToUpdate[0]);
            ASSERT_EQ(expectedTtl, matchToWrite.GetTtl());
            ASSERT_EQ(expectedTouch, matchToWrite.GetTouch());
        } else {
            ASSERT_EQ(0u, changesBatch.RecordsToUpdate.size());
        }
    };
}

TEST(TUploadHandler, NewMatch) {
    TDbState state({}, {}, TRACKED_BACK_REFERENCES);

    auto newMatch = GetIncomingMatch(EXT_ID, TOUCH_TS);
    newMatch.AddId(TMatchedId(YUID));
    newMatch.AddId(TMatchedId(ICOOKIE));

    auto handler = GetHandler({TUploadCommand(EXT_ID.Value, newMatch)});

    handler.UpdateDbState(state);

    const auto& changesBatch = GetChangesBatch(state);

    ASSERT_EQ(0u, changesBatch.KeysToDelete.size());
    ASSERT_EQ(
        THashSet<TString>({NIdSerializer::ToString(EXT_ID), NIdSerializer::ToString(YUID), NIdSerializer::ToString(ICOOKIE)}),
        GetSetOfKeys(changesBatch.RecordsToUpdate)
    );

    const auto& matchToWrite = NMatchSerializer::FromRecord(changesBatch.RecordsToUpdate[0]);
    ASSERT_EQ(DEFAULT_TTL, matchToWrite.GetTtl());
}

TEST(TUploadHandler, UpdateExternalIds) {
    auto state = NTest::GetDbState(EXT_ID, YUID, TOUCH_TS, DEFAULT_TTL, TRACKED_BACK_REFERENCES);

    auto newMatch = GetIncomingMatch(EXT_ID, TOUCH_TS);
    newMatch.AddId(TMatchedId(ICOOKIE));

    auto handler = GetHandler({TUploadCommand(EXT_ID.Value, newMatch)});
    handler.UpdateDbState(state);

    const auto& changesBatch = GetChangesBatch(state);

    ASSERT_EQ(
        THashSet<TString>({NIdSerializer::ToString(EXT_ID), NIdSerializer::ToString(ICOOKIE)}),
        GetSetOfKeys(changesBatch.RecordsToUpdate)
    );
    ASSERT_EQ(0u, changesBatch.KeysToDelete.size());

    const auto& updatedMatch = state.GetMatches().Get(EXT_ID);

    THashSet<TId> matchedIds;
    Transform(updatedMatch->GetInternalIds().begin(), updatedMatch->GetInternalIds().end(), std::inserter(matchedIds, matchedIds.end()), [](const auto& pair) {return pair.second.GetId(); });
    ASSERT_EQ(
        THashSet<TId>({YUID, ICOOKIE}),
        matchedIds
    );
}

TEST(TUploadHandler, DoNotUpdateNewerMatch) {
    auto state = NTest::GetDbState(EXT_ID, YUID, TOUCH_TS, DEFAULT_TTL, TRACKED_BACK_REFERENCES);

    const auto& olderTouchTs = TOUCH_TS - TOUCH_TIMEOUT;
    auto olderMatch = GetIncomingMatch(EXT_ID, olderTouchTs);
    olderMatch.AddId(TMatchedId(YUID2, olderTouchTs));

    auto handler = GetHandler({TUploadCommand(EXT_ID.Value, olderMatch)});
    handler.UpdateDbState(state);

    const auto& changesBatch = GetChangesBatch(state);

    ASSERT_EQ(0u, changesBatch.RecordsToUpdate.size());
    ASSERT_EQ(0u, changesBatch.KeysToDelete.size());
}

TEST(TUploadHandler, DoNotUpdateExistingMatch) {
    auto state = NTest::GetDbStateWithICookie(EXT_ID, YUID, ICOOKIE, TOUCH_TS, DEFAULT_TTL, TRACKED_BACK_REFERENCES);

    const auto& newerTs = TOUCH_TS + TOUCH_TIMEOUT;
    auto subMatch = GetIncomingMatch(EXT_ID, newerTs);
    subMatch.AddId(TMatchedId(YUID, newerTs));

    auto handler = GetHandler({TUploadCommand(EXT_ID.Value, subMatch)});
    handler.UpdateDbState(state);

    const auto& changesBatch = GetChangesBatch(state);

    ASSERT_EQ(0u, changesBatch.RecordsToUpdate.size());
    ASSERT_EQ(0u, changesBatch.KeysToDelete.size());
}

TEST(TUploadHandler, TouchTimeoutElapsed) {
    const auto& touchWithinTimeout = TOUCH_TS + TOUCH_TIMEOUT;
    TEST_UPDATE_TOUCH_AND_TTL(touchWithinTimeout, false, TInstant(), TDuration());

    const auto& touchBeyondTimeout = touchWithinTimeout + TDuration::Seconds(1);
    TEST_UPDATE_TOUCH_AND_TTL(touchBeyondTimeout, true, touchBeyondTimeout, DEFAULT_TTL);
}

TEST(TUploadHandler, ExtendTtlTimeoutElapsed) {
    const auto& touchWithinExtendTtlTimeout = TOUCH_TS + EXTEND_TTL_TIMEOUT;
    TEST_UPDATE_TOUCH_AND_TTL(touchWithinExtendTtlTimeout, true, touchWithinExtendTtlTimeout, DEFAULT_TTL);

    const auto& touchBeyondExtendTtlTimeout = touchWithinExtendTtlTimeout + TDuration::Seconds(1);
    TEST_UPDATE_TOUCH_AND_TTL(touchBeyondExtendTtlTimeout, true, touchBeyondExtendTtlTimeout, EXTENDED_TTL);
}

TEST(TUploadHandler, TtlItselfMustNotBeTakenIntoAccount) {
    auto state = NTest::GetDbState(EXT_ID, YUID, TOUCH_TS, DEFAULT_TTL, TRACKED_BACK_REFERENCES);

    TMatch sameMatch(*state.GetMatches().Get(EXT_ID));
    sameMatch.SetTtl(EXTENDED_TTL);

    auto handler = GetHandler({TUploadCommand(EXT_ID.Value, sameMatch)});
    handler.UpdateDbState(state);

    const auto& changesBatch = GetChangesBatch(state);

    ASSERT_EQ(0u, changesBatch.KeysToDelete.size());
    ASSERT_EQ(0u, GetSetOfKeys(changesBatch.RecordsToUpdate).size());
}

TEST(TUploadHandler, UploadCustomTag) {
    TDbState state({}, {}, TRACKED_BACK_REFERENCES);

    auto incomingMatch = GetIncomingMatch(EXT_ID_CUSTOM, TOUCH_TS);
    incomingMatch.AddId(TMatchedId(YUID, TOUCH_TS));

    auto handler = GetHandler({TUploadCommand(EXT_ID_CUSTOM.Value, incomingMatch)});
    handler.UpdateDbState(state);

    const auto& changesBatch = GetChangesBatch(state);

    ASSERT_EQ(0u, changesBatch.KeysToDelete.size());
    ASSERT_EQ(
        THashSet<TString>({NIdSerializer::ToString(EXT_ID_CUSTOM)}),
        GetSetOfKeys(changesBatch.RecordsToUpdate)
    );

    const auto& matchToWrite = NMatchSerializer::FromRecord(changesBatch.RecordsToUpdate[0]);
    ASSERT_EQ(TTL_CUSTOM, matchToWrite.GetTtl());
}

TEST(TUploadHandler, CustomTagTtlIsNotUpdated) {
    auto dbMatch = NTest::GetDbMatch(EXT_ID_CUSTOM, TOUCH_TS, TTL_CUSTOM);
    dbMatch.AddId(TMatchedId(YUID, TOUCH_TS));

    TDbState state(
        {{dbMatch.GetExtId(), dbMatch}},
        {},
        TRACKED_BACK_REFERENCES
    );

    TMatch sameMatch(dbMatch);
    sameMatch.SetTouch(TOUCH_TS + TOUCH_TIMEOUT + TDuration::Seconds(1));

    auto handler = GetHandler({TUploadCommand(EXT_ID_CUSTOM.Value, sameMatch)});
    handler.UpdateDbState(state);

    const auto& changesBatch = GetChangesBatch(state);

    ASSERT_EQ(0u, changesBatch.KeysToDelete.size());
    ASSERT_EQ(
        THashSet<TString>({NIdSerializer::ToString(EXT_ID_CUSTOM)}),
        GetSetOfKeys(changesBatch.RecordsToUpdate)
    );

    const auto& matchToWrite = NMatchSerializer::FromRecord(changesBatch.RecordsToUpdate[0]);
    ASSERT_EQ(TTL_CUSTOM, matchToWrite.GetTtl());
}
