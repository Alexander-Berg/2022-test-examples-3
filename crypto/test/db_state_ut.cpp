#include <crypta/cm/services/common/data/id_utils.h>
#include <crypta/cm/services/common/db_state/db_state.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/utility.h>
#include <util/generic/vector.h>

#include <iterator>

Y_UNIT_TEST_SUITE(TDbState) {
    using namespace NCrypta;
    using namespace NCrypta::NCm;

    using TMatchMap = TDbState::TMatchMap;
    using TBackRefMap = TDbState::TBackRefMap;

    const TMatchMap::TMap EMPTY_MATCHES;
    const TBackRefMap::TMap EMPTY_BACK_REFS;

    const TString UNTRACKED_TAG = "untracked_tag";
    const TString TRACKED_TAG = "tracked_tag";
    const TString TRACKED_TAG_2 = "tracked_tag_2";
    const THashSet<TString> TRACKED_BACK_REF_TAGS({TRACKED_TAG, TRACKED_TAG_2});

    const TId TRACKED_EXT_ID(TRACKED_TAG, "tracked_ext_id");
    const TMatchedId TRACKED_MATCHED_ID(TId(YANDEXUID_TYPE, "tracked_yandexuid"));

    const TMatch MATCH_WITH_TRACKED(
        TRACKED_EXT_ID,
        TMatch::TMatchedIds({{YANDEXUID_TYPE, TRACKED_MATCHED_ID}})
    );

    TDbState GetEmptyDbState() {
        return TDbState(EMPTY_MATCHES, EMPTY_BACK_REFS, TRACKED_BACK_REF_TAGS);
    }

    TDbState GetDefaultDbState() {
        return TDbState(
            THashMap<TId, TMatch>({
                {TRACKED_EXT_ID, MATCH_WITH_TRACKED},
            }),
            THashMap<TId, TBackReference>({
                {TRACKED_MATCHED_ID.GetId(), TBackReference(TRACKED_MATCHED_ID.GetId(), {TRACKED_EXT_ID})},
            }),
            TRACKED_BACK_REF_TAGS
        );
    }

    TDbState GetLegacyDbState() {
        const TId anotherTrackedId(TRACKED_TAG, "another_ext_id");
        const TMatch anotherMatchWithTracked(
            anotherTrackedId,
            TMatch::TMatchedIds({{YANDEXUID_TYPE, TRACKED_MATCHED_ID}})
        );
        return TDbState(
            THashMap<TId, TMatch>({
                {TRACKED_EXT_ID, MATCH_WITH_TRACKED},
                {anotherTrackedId, anotherMatchWithTracked},
            }),
            THashMap<TId, TBackReference>({
                {
                    TRACKED_MATCHED_ID.GetId(),
                    TBackReference(TRACKED_MATCHED_ID.GetId(), {
                        TRACKED_EXT_ID,
                        anotherTrackedId,
                    })
                },
            }),
            TRACKED_BACK_REF_TAGS
        );
    }

    template <typename TChangeTrackingMap>
    typename TChangeTrackingMap::TIterator::difference_type GetUpdateCount(const TChangeTrackingMap& map) {
        return std::distance(map.UpdateBegin(), map.UpdateEnd());
    }

    template <typename TChangeTrackingMap>
    typename TChangeTrackingMap::TIterator::difference_type GetDeleteCount(const TChangeTrackingMap& map) {
        return std::distance(map.DeleteBegin(), map.DeleteEnd());
    }


    Y_UNIT_TEST(Empty) {
        const auto& dbState = GetEmptyDbState();

        const auto& matchMap = dbState.GetMatches();
        UNIT_ASSERT_EQUAL(0, GetUpdateCount(matchMap));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(matchMap));

        const auto& backRefMap = dbState.GetBackRefs();
        UNIT_ASSERT_EQUAL(0, GetUpdateCount(backRefMap));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(backRefMap));
    }
    
    Y_UNIT_TEST(TestDefaultDbState) {
        const auto& dbState = GetDefaultDbState();

        UNIT_ASSERT_EQUAL(0, GetUpdateCount(dbState.GetMatches()));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetMatches()));

        UNIT_ASSERT_EQUAL(0, GetUpdateCount(dbState.GetBackRefs()));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetBackRefs()));
    }

    Y_UNIT_TEST(WriteTheSame) {
        auto dbState = GetDefaultDbState();

        dbState.WriteMatch(MATCH_WITH_TRACKED);

        UNIT_ASSERT_EQUAL(1, GetUpdateCount(dbState.GetMatches()));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetMatches()));
        UNIT_ASSERT_EQUAL(MATCH_WITH_TRACKED, *dbState.GetMatches().UpdateBegin());

        UNIT_ASSERT_EQUAL(0, GetUpdateCount(dbState.GetBackRefs()));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetBackRefs()));
    }

    Y_UNIT_TEST(WriteUntrackedAndDelete) {
        auto dbState = GetDefaultDbState();

        TMatch newMatch(
            TId(UNTRACKED_TAG, "new_ext_id"),
            TMatch::TMatchedIds({
                {YANDEXUID_TYPE, TMatchedId(TId(YANDEXUID_TYPE, "new_yandexuid"))},
            })
        );

        dbState.WriteMatch(newMatch);

        UNIT_ASSERT_EQUAL(1, GetUpdateCount(dbState.GetMatches()));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetMatches()));
        UNIT_ASSERT_EQUAL(newMatch, *dbState.GetMatches().UpdateBegin());

        UNIT_ASSERT_EQUAL(0, GetUpdateCount(dbState.GetBackRefs()));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetBackRefs()));

        dbState.DeleteMatch(newMatch.GetExtId());

        UNIT_ASSERT_EQUAL(0, GetUpdateCount(dbState.GetMatches()));
        UNIT_ASSERT_EQUAL(1, GetDeleteCount(dbState.GetMatches()));
        UNIT_ASSERT_EQUAL(newMatch.GetExtId(), *dbState.GetMatches().DeleteBegin());

        UNIT_ASSERT_EQUAL(0, GetUpdateCount(dbState.GetBackRefs()));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetBackRefs()));
    }

    Y_UNIT_TEST(CommonYandexuidDifferentTags) {
        auto dbState = GetDefaultDbState();

        const auto& commonYandexuid = TRACKED_MATCHED_ID.GetId();
        const TMatch anotherTrackedMatch(
            TId(TRACKED_TAG_2, "tracked_ext_id_2"),
            TMatch::TMatchedIds({
                {YANDEXUID_TYPE, TMatchedId(commonYandexuid)}
            })
        );

        const TBackReference refBackRef(
            commonYandexuid,
            THashSet<TId>({
                TRACKED_EXT_ID,
                anotherTrackedMatch.GetExtId(),
            })
        );

        dbState.WriteMatch(anotherTrackedMatch);

        UNIT_ASSERT_EQUAL(1, GetUpdateCount(dbState.GetMatches()));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetMatches()));
        UNIT_ASSERT_EQUAL(anotherTrackedMatch, *dbState.GetMatches().UpdateBegin());

        UNIT_ASSERT_EQUAL(1, GetUpdateCount(dbState.GetBackRefs()));
        UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetBackRefs()));
        UNIT_ASSERT_EQUAL(refBackRef, *dbState.GetBackRefs().UpdateBegin());
    }

    Y_UNIT_TEST(CommonYandexuidCommonTag) {
        TVector<TDbState> dbStates = {
            GetDefaultDbState(),
            GetLegacyDbState(),
        };

        const auto& commonYandexuid = TRACKED_MATCHED_ID.GetId();
        const TMatch anotherTrackedMatch(
            TId(TRACKED_TAG, "tracked_ext_id_2"),
            TMatch::TMatchedIds({
                {YANDEXUID_TYPE, TMatchedId(commonYandexuid)}
            })
        );

        const TBackReference refBackRef(
            commonYandexuid,
            THashSet<TId>({anotherTrackedMatch.GetExtId()})
        );

        for (auto& dbState : dbStates) {
            dbState.WriteMatch(anotherTrackedMatch);

            UNIT_ASSERT_EQUAL(1, GetUpdateCount(dbState.GetMatches()));
            UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetMatches()));
            UNIT_ASSERT_EQUAL(anotherTrackedMatch, *dbState.GetMatches().UpdateBegin());

            UNIT_ASSERT_EQUAL(1, GetUpdateCount(dbState.GetBackRefs()));
            UNIT_ASSERT_EQUAL(0, GetDeleteCount(dbState.GetBackRefs()));
            UNIT_ASSERT_EQUAL(refBackRef, *dbState.GetBackRefs().UpdateBegin());
        }
    }
}
