#include <crypta/cm/services/calc_expire/lib/calc_expire_mapper.h>
#include <crypta/cm/services/calc_expire/lib/fields.h>
#include <crypta/cm/services/common/changes/change_commands.h>
#include <crypta/cm/services/common/changes/change_log_fields.h>
#include <crypta/cm/services/common/data/proto_helpers.h>
#include <crypta/cm/services/common/expiration/is_expired.h>
#include <crypta/cm/services/common/serializers/id/string/id_string_serializer.h>
#include <crypta/cm/services/common/serializers/match_proto/record/match_proto_record_serializer.h>
#include <crypta/lib/native/yt/utils/helpers.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <mapreduce/yt/util/ypath_join.h>

#include <util/string/builder.h>

using namespace NCrypta::NCm;
using namespace NCrypta::NCm::NCalcExpire;
using namespace NYT;
using namespace NYT::NTesting;

Y_UNIT_TEST_SUITE(TCalcExpireMapper) {
    static void CreateTable(IClientPtr client, const TString& path, const TVector<TNode>& records) {
        auto writer = client->CreateTableWriter<TNode>(path);
        for (const auto& record : records) {
            writer->AddRow(record);
        }
        writer->Finish();
    }

    const TDuration LONG_TTL = TDuration::Days(3);
    const TDuration SHORT_TTL = TDuration::Days(1);
    const TString NS_LONG = "ns_long";
    const TString NS_SHORT = "ns_short";
    const TString NS_ZERO = "ns_zero";
    const TString NS_YANDEXUID = "yandexuid";
    const TString NS_ICOOKIE = "icookie";
    const TString NS_ERRORS = "errors";

    TNode CreateSerializedRecord(const TId& id, TInstant touchTs, TDuration ttl) {
        TMatchProto matchProto;
        *matchProto.MutableExtId() = MakeIdProto(id);
        matchProto.SetTouch(touchTs.Seconds());
        matchProto.SetTtl(ttl.Seconds());

        auto record = NMatchProtoSerializer::ToRecord(std::move(matchProto));
        return TNode()("key", record.Key)("value", record.Value);
    }

    TNode CreateBrokenRecord(const TId& id) {
        return TNode()("key", NIdSerializer::ToString(id))("value", "bar");
    }

    Y_UNIT_TEST(SimpleTest) {
        NYT::JoblessInitialize();
        auto client = CreateTestClient();

        TString workdir = "//tmp/test/workdir";
        client->Create(workdir, NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Recursive(true).IgnoreExisting(true));

        TString source = JoinYPaths(workdir, "src");
        TString destination = JoinYPaths(workdir, "dst");
        TString errors = JoinYPaths(workdir, "errors");

        const auto START_TS = TInstant::Now();
        CreateTable(client, source, {
                                        CreateSerializedRecord(TId(NS_LONG, "fresh"), START_TS - LONG_TTL + TDuration::Seconds(1), LONG_TTL),
                                        CreateSerializedRecord(TId(NS_SHORT, "fresh"), START_TS - SHORT_TTL + TDuration::Seconds(1), SHORT_TTL),
                                        CreateSerializedRecord(TId(NS_LONG, "expired"), START_TS - LONG_TTL, LONG_TTL),
                                        CreateSerializedRecord(TId(NS_SHORT, "expired"), START_TS - SHORT_TTL, SHORT_TTL),
                                        CreateSerializedRecord(TId(NS_YANDEXUID, "internal_1"), START_TS, LONG_TTL),
                                        CreateSerializedRecord(TId(NS_ICOOKIE, "internal_2"), START_TS, LONG_TTL),
                                        CreateSerializedRecord(TId(NS_ZERO, "expired_1"), TInstant::Zero(), TDuration::Zero()),
                                        CreateSerializedRecord(TId(NS_ZERO, "expired_2"), START_TS, TDuration::Zero()),
                                        CreateSerializedRecord(TId(NS_ZERO, "expired_3"), TInstant::Zero(), LONG_TTL),
                                        CreateBrokenRecord(TId(NS_ERRORS, "broken_2")),
                                        CreateBrokenRecord(TId(NS_ERRORS, "broken_1")),
                                    });

        {
            auto outputBuilder = TCalcExpireMapper::PrepareOutput(destination, errors);
            NYT::TMapOperationSpec spec;
            spec.AddInput<NYT::TNode>(source);
            NCrypta::AddOutputs<NYT::TNode>(spec, outputBuilder.GetTables());
            spec.Ordered(true);
            client->Map(spec, new TCalcExpireMapper(outputBuilder.GetIndexes(), START_TS));
        }

        {
            auto reader = client->CreateTableReader<TNode>(destination);
            size_t count = 0;
            for (; reader->IsValid(); reader->Next()) {
                ++count;
                auto& row = reader->GetRow();
                UNIT_ASSERT_EQUAL(3, row.Size());

                TId id(row[NExpirationFields::TYPE].AsString(), row[NExpirationFields::VALUE].AsString());
                UNIT_ASSERT_STRING_CONTAINS_C(id.Value, "expired", id.Value);
            }
            UNIT_ASSERT_EQUAL_C(5, count, count);
        }

        {
            auto reader = client->CreateTableReader<TNode>(errors);
            size_t count = 0;
            for (; reader->IsValid(); reader->Next()) {
                ++count;
                auto& row = reader->GetRow();
                const auto& key = row[NExpirationFields::KEY].AsString();
                const auto& errorMsg = row[NExpirationFields::ERROR].AsString();
                UNIT_ASSERT_EQUAL(NS_ERRORS, NIdSerializer::FromString(key).Type);
                UNIT_ASSERT_EQUAL_C("inflate error(incorrect header check)", errorMsg, errorMsg);
            }
            UNIT_ASSERT_EQUAL_C(2, count, count);
        }

        client->Remove(workdir, NYT::TRemoveOptions().Recursive(true));
    }
}
