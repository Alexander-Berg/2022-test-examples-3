#include <library/cpp/testing/unittest/registar.h>

#include <crypta/graph/rt/sklejka/cid_resolver/future/wrapper.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <mapreduce/yt/util/wait_for_tablets_state.h>
#include <mapreduce/yt/util/ypath_join.h>

#include <yt/yt/client/api/rpc_proxy/config.h>
#include <yt/yt/client/api/rpc_proxy/connection.h>

#include <util/system/env.h>

using namespace NCrypta;


template <>
void Out<NIdentifiers::TGenericID>(IOutputStream& stream, const NIdentifiers::TGenericID& id) {
        stream << id.GetTypeString() << " " << id.GetValue();
}


NYT::NApi::IClientPtr make_client(const std::string url = GetEnv("YT_PROXY")) {
    NYT::NApi::NRpcProxy::TConnectionConfigPtr config = NYT::New<NYT::NApi::NRpcProxy::TConnectionConfig>();
    config->ClusterUrl = url;
    auto connection = NYT::NApi::NRpcProxy::CreateConnection(config);
    auto client_options = NYT::NApi::TClientOptions::FromToken(GetEnv("YT_TOKEN"));
    return connection->CreateClient(client_options);
}

TString make_test_table(const TString& table_name) {
    NYT::NTesting::TTabletFixture fixture;
    auto client = NYT::NTesting::CreateTestClient();

    const auto directory = NYT::NTesting::CreateTestDirectory(client);
    TString path = NYT::JoinYPaths(directory, table_name);

    auto attrs = NYT::TNode()
        ("dynamic", true)
        ("strict", true)
        ("schema", NYT::TNode::CreateList({
          NYT::TNode()("name", "Hash")("type", "uint64")("sort_order", "ascending")("required", false)("expression", "farm_hash(Id)"),
          NYT::TNode()("name", "Id")("type", "string")("sort_order", "ascending")("required", true),
          NYT::TNode()("name", "CryptaId")("type", "string")("required", true),
    }));

    client->Remove(path, NYT::TRemoveOptions().Recursive(true).Force(true));
    client->Create(path, NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Recursive(true).Attributes(attrs));
    client->MountTable(path);
    NYT::WaitForTabletsState(client, path, NYT::TS_MOUNTED);
    return path;
}


Y_UNIT_TEST_SUITE(TUnitLoaderSaver) {
    using TGenericID = NIdentifiers::TGenericID;
    using TCryptaId = NIdentifiers::TCryptaId;

    Y_UNIT_TEST(SaveLoad) {
        NYT::JoblessInitialize();

        TString path = make_test_table("test_table");

        auto client = make_client();
        TYtCryptaIdProtosSaver saver(path);


        TGenericID yuid{"yandexuid", "4682071601576680669"};
        TGenericID gaid{"gaid", "44f07d46-d179-43fe-87b1-0cd15c51f0d3"};
        TGenericID email{"email", "good@example.com"};
        TGenericID email_no_hit1{"email", "bad1@example.com"};
        TGenericID email_no_hit2{"email", "bad2@example.com"};

        TCryptaIdPairVector saveRows{
            {yuid, TCryptaId{"11"}},
            {gaid, TCryptaId{"22"}},
            {email, TCryptaId{"33"}},
        };
        saver.SaveRowset(saveRows, client);


        TYtCryptaIdProtosLoader loader(path, TDuration::Seconds(10), true);
        {
            THashMap<TGenericID, ui64> etalon, result;
            TVector<TGenericID> rows;
            for (const auto& item: saveRows) {
                rows.emplace_back(item.first);
                etalon[item.first] = static_cast<ui64>(item.second);
            }

            auto res = loader.LoadRowset(rows, client);
            for (const auto& item: res) {
                result[TGenericID(item.GetId())] = item.GetCryptaId().GetCryptaId().GetValue();

            }
            UNIT_ASSERT_VALUES_EQUAL(etalon, result);
        }


        TCryptaIdPairVector updateRows {
            {yuid, TCryptaId{"12"}},
        };
        saver.SaveRowset(updateRows, client);
        {
            THashMap<TGenericID, ui64> etalon, result;
            TVector<TGenericID> rows;
            etalon[yuid] = 12;
            etalon[gaid] = 22;

            auto res = loader.LoadRowset({yuid, gaid, email_no_hit1, email_no_hit2}, client);
            UNIT_ASSERT_VALUES_EQUAL(res.size(), 4);
            int empty = 0;
            for (const auto& item: res) {
                if (item.GetId().GetType()) {
                    result[TGenericID(item.GetId())] = item.GetCryptaId().GetCryptaId().GetValue();
                } else {
                    ++empty;
                }
            }
            UNIT_ASSERT_VALUES_EQUAL(empty, 2);
            UNIT_ASSERT_VALUES_EQUAL(etalon, result);
        }
    }
}
