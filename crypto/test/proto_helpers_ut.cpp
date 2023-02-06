#include <crypta/cm/services/common/data/attributes.h>
#include <crypta/cm/services/common/data/id.h>
#include <crypta/cm/services/common/data/proto_helpers.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/vector.h>
#include <util/string/builder.h>

#include <functional>
#include <limits>
#include <utility>

Y_UNIT_TEST_SUITE(ProtoHelpers) {
    using namespace NCrypta::NCm;

    const TVector<TString> TYPES = {"", "a", "eeeeeeeeeeeeeeeeeeeeeee"};
    const TVector<TString> VALUES = {"", "b", "fffffffffffffffffffffff"};
    const TVector<ui64> INTEGERS = {0, 1, std::numeric_limits<ui64>::max()};
    const TVector<TAttributes> ATTRIBUTES = {
        {},
        {{"", ""}},
        {{"", "a"}},
        {{"a", ""}},
        {{"a", "b"}},
        {{"a", "b"}, {"c", "d"}}};

    TMatchedIdProto MakeMatchedIdProto(const TIdProto& id, ui64 matchTs = 0, ui64 cas = 0, const TAttributes& attrs = TAttributes()) {
        TMatchedIdProto ret;
        *ret.MutableId() = id;
        ret.SetMatchTs(matchTs);
        ret.SetCas(cas);
        SerializeAttributes(attrs, *ret.MutableAttributes());
        return ret;
    }

    TMatchProto MakeMatchProto(const TIdProto& extId, const TVector<TMatchedIdProto>& ids) {
        TMatchProto ret;
        *ret.MutableExtId() = extId;
        for (const auto& id : ids) {
            *ret.MutableMatchedIds()->AddIds() = id;
        }
        return ret;
    }

    void RunTypeValueTest(std::function<void(const TString& type, const TString& value)> testBody) {
        for (const auto& type : TYPES) {
            for (const auto& value : VALUES) {
                testBody(type, value);
            }
        }
    }

    Y_UNIT_TEST(MakeIdProto) {
        RunTypeValueTest([](const TString& type, const TString& value) {
            TIdProto idProto = MakeIdProto(type, value);
            UNIT_ASSERT_EQUAL(type, idProto.GetType());
            UNIT_ASSERT_EQUAL(value, idProto.GetValue());

            TId id = TId(type, value);
            TIdProto idProtoFromId = MakeIdProto(id);
            UNIT_ASSERT_EQUAL(type, idProtoFromId.GetType());
            UNIT_ASSERT_EQUAL(value, idProtoFromId.GetValue());
            UNIT_ASSERT_EQUAL(id, MakeId(idProtoFromId));

            UNIT_ASSERT_EQUAL(id, MakeId(idProto));
        });
    }

    Y_UNIT_TEST(MakeBackRef) {
        const auto extId = MakeIdProto("ext_type", "ext_value");
        const auto intId = MakeIdProto("int_type", "int_value");

        auto backRef = MakeBackRef(intId, extId);
        UNIT_ASSERT_EQUAL(intId, backRef.GetId());
        UNIT_ASSERT_EQUAL(1, backRef.RefsSize());
        UNIT_ASSERT_EQUAL(extId, backRef.GetRefs(0));
    }

    Y_UNIT_TEST(IdProtoEqual) {
        RunTypeValueTest([](const TString& type, const TString& value) {
            TVector<TIdProto> cases = {
                MakeIdProto(type, value),
                MakeIdProto(type, value),
                MakeIdProto(TId(type, value))};

            for (size_t i = 0; i < cases.size(); ++i) {
                for (size_t j = i + 1; j < cases.size(); ++j) {
                    UNIT_ASSERT_EQUAL(cases[i], cases[j]);
                    UNIT_ASSERT_EQUAL(cases[j], cases[i]);
                    UNIT_ASSERT(!(cases[i] != cases[j]));
                    UNIT_ASSERT(!(cases[j] != cases[i]));
                }
            }
        });
    }

    Y_UNIT_TEST(IdProtoUnequal) {
        RunTypeValueTest([](const TString& type, const TString& value) {
            TVector<TIdProto> cases = {
                MakeIdProto(type, value),
                MakeIdProto(type + "z", value),
                MakeIdProto(type, value + "Z"),
                MakeIdProto(type + "z", value + "Z")};

            for (size_t i = 0; i < cases.size(); ++i) {
                for (size_t j = i + 1; j < cases.size(); ++j) {
                    UNIT_ASSERT(!(cases[i] == cases[j]));
                    UNIT_ASSERT(!(cases[j] == cases[i]));
                    UNIT_ASSERT(cases[i] != cases[j]);
                    UNIT_ASSERT(cases[j] != cases[i]);
                }
            }
        });
    }

    void RunMatchedIdTest(std::function<void(const TMatchedIdProto&)> testBody) {
        for (const auto& type : TYPES) {
            for (const auto& value : VALUES) {
                for (auto matchTs : INTEGERS) {
                    for (auto cas : INTEGERS) {
                        for (auto attributes : ATTRIBUTES) {
                            testBody(MakeMatchedIdProto(MakeIdProto(type, value), matchTs, cas, attributes));
                        }
                    }
                }
            }
        }
    }

    Y_UNIT_TEST(MatchedIdProtoEqual) {
        RunMatchedIdTest([](const TMatchedIdProto& matchedId) {
            UNIT_ASSERT_EQUAL(matchedId, matchedId);

            auto other = matchedId;
            UNIT_ASSERT_EQUAL(other, matchedId);
            UNIT_ASSERT_EQUAL(matchedId, other);
        });
    }

    Y_UNIT_TEST(MatchedIdProtoUnequal) {
        RunMatchedIdTest([](const TMatchedIdProto& matchedId) {
            TVector<TMatchedIdProto> cases(6, matchedId);
            cases[1].MutableId()->SetType("other_type");
            cases[2].MutableId()->SetValue("other_value");
            cases[3].SetCas(100500);
            cases[4].SetMatchTs(2 * 100500);

            cases[5].ClearAttributes();
            SerializeAttributes(TAttributes{{"other_key", "other_value"}}, *cases[5].MutableAttributes());

            for (size_t i = 0; i < cases.size(); ++i) {
                for (size_t j = i + 1; j < cases.size(); ++j) {
                    UNIT_ASSERT(!(cases[i] == cases[j]));
                    UNIT_ASSERT(!(cases[j] == cases[i]));
                    UNIT_ASSERT(cases[i] != cases[j]);
                    UNIT_ASSERT(cases[j] != cases[i]);
                }
            }
        });
    }

    void RunMatchTest(std::function<void(const TMatchProto&)> testBody) {
        auto extId = MakeIdProto("ext_type", "ext_value");
        auto matchedId1 = MakeMatchedIdProto(MakeIdProto("int_type", "int_value"), 100500, 2 * 100500, {{"attr", "value"}});
        auto matchedId2 = MakeMatchedIdProto(MakeIdProto("int_type_2", "int_value"), 100500, 2 * 100500, {});

        auto match_0 = MakeMatchProto(extId, {});
        testBody(match_0);

        auto match_1 = MakeMatchProto(extId, {matchedId1});
        testBody(match_1);

        auto match_2 = MakeMatchProto(extId, {matchedId1, matchedId2});
        testBody(match_2);
    }

    Y_UNIT_TEST(MatchProtoEqual) {
        RunMatchTest([](const TMatchProto& matchProto) {
            UNIT_ASSERT_EQUAL(matchProto, matchProto);

            auto other = matchProto;
            UNIT_ASSERT_EQUAL(matchProto, other);
            UNIT_ASSERT_EQUAL(other, matchProto);
        });
    }

    Y_UNIT_TEST(MatchProtoUnequal) {
        RunMatchTest([](const TMatchProto& matchProto) {
            TVector<TMatchProto> cases(5, matchProto);

            cases[1].MutableExtId()->SetType("other_type");

            cases[2].MutableMatchedIds()->ClearIds();
            *cases[2].MutableMatchedIds()->AddIds() = MakeMatchedIdProto(MakeIdProto("other_type", "other_value"), 100500, 2 * 100500, {});

            cases[3] = cases[2];
            *cases[3].MutableMatchedIds()->AddIds() = MakeMatchedIdProto(MakeIdProto("other_type_2", "other_value_2"), 999999, 2 * 999999, {});

            if (matchProto.GetMatchedIds().IdsSize() != 0) {
                cases[4].MutableMatchedIds()->ClearIds();
            } else {
                cases.pop_back();
            }

            for (size_t i = 0; i < cases.size(); ++i) {
                for (size_t j = i + 1; j < cases.size(); ++j) {
                    UNIT_ASSERT(!(cases[i] == cases[j]));
                    UNIT_ASSERT(!(cases[j] == cases[i]));
                    UNIT_ASSERT(cases[i] != cases[j]);
                    UNIT_ASSERT(cases[j] != cases[i]);
                }
            }
        });
    }
}
