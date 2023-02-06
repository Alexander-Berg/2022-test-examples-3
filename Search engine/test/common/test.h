#pragma once

#include <library/cpp/testing/common/network.h>
#include <library/cpp/testing/unittest/registar.h>

#include <search/base_search/rs_proxy/protos/client_config.pb.h>
#include <search/base_search/rs_proxy/client/proxy.h>

#include <search/base_search/rs_proxy/test/mock/controller/controller.h>
#include <search/base_search/rs_proxy/test/mock/deploy/service.h>
#include <search/base_search/rs_proxy/test/mock/remote_storage/remote_storage.h>
#include <search/base_search/rs_proxy/test/mock/remote_storage_proxy/remote_storage_proxy.h>
#include <search/base_search/rs_proxy/test/mock/worker/worker.h>

#include <kernel/doom/item_storage/item_storage.h>
#include <kernel/doom/item_storage/test/generate.h>
#include <kernel/doom/item_storage/test/index.h>
#include <kernel/doom/item_storage/test/validate.h>
#include <kernel/doom/item_storage/test/write.h>

#include <util/folder/tempdir.h>
#include <util/system/sanitizers.h>

using namespace NBlobStorage::NProxy;
using namespace NDoom::NItemStorage::NTest;
using namespace NDoom::NItemStorage;

static constexpr TStringBuf kClientConfig = R"raw(
Namespace: "0"
SourceOptions {
  TaskOptions {
    ConnectTimeouts: "20ms"
    SendingTimeouts: "20ms"
  }
  BalancingOptions {
    MaxAttempts: 3
    AllowDynamicWeights: false
    RandomGroupSelection: true
    EnableIpV6: true
  }
  HedgedRequestOptions {
    HedgedRequestTimeouts: "7ms"
    HedgedRequestTimeouts: "10ms"
    HedgedRequestRateThreshold: 0.3
  }
  TimeOut: "30ms"
  AllowConnStat: true
}
)raw";

void PatchTimeouts(NBlobStorage::NProxy::TRemoteStorageProxyEngineConfig& config, double mult) {
    auto patch = [mult](TString* timeout) {
        TDuration duration = TDuration::Parse(*timeout);
        *timeout = ToString(duration * mult);
    };

    patch(config.MutableSourceOptions()->MutableTimeOut());
    auto hedgeds = config.MutableSourceOptions()->MutableHedgedRequestOptions();
    for (auto& timeout : *hedgeds->MutableHedgedRequestTimeouts()) {
        patch(&timeout);
    }
    patch(config.MutableSourceOptions()->MutableTaskOptions()->MutableConnectTimeouts(0));
    patch(config.MutableSourceOptions()->MutableTaskOptions()->MutableSendingTimeouts(0));
}

inline NBlobStorage::NProxy::TRemoteStorageProxyEngineConfig MakeConfig(const NMock::TSnapshot& snapshot, ui16 port) {
    NBlobStorage::NProxy::TRemoteStorageProxyEngineConfig config;
    Y_ENSURE(google::protobuf::TextFormat::ParseFromString(TString{kClientConfig}, &config));
    config.SetNamespace(snapshot.Stream);

#if defined(_asan_enabled_)
    double timeoutMultiplier = 3.0;
#elif defined(_msan_enabled_)
    double timeoutMultiplier = 5.0;
#else
    double timeoutMultiplier = 1.0;
#endif

    for (auto&& index : snapshot.Index.Items) {
        auto* cfg = config.AddItemTypes();
        cfg->SetItemType(index.ItemType);
        cfg->SetShardingModulo(1);
        cfg->MutableEndpointSetOptions();

        auto* shard = cfg->AddShards();
        shard->SetShardNumber(0);
        shard->MutableReplicas()->AddSearchScriptCgis(Sprintf("http://localhost:%d/proxy", port));
    }

    PatchTimeouts(config, timeoutMultiplier);

    return config;
}

inline THolder<IItemStorage> MakeItemStorage(const NMock::TSnapshot& snapshot, TRemoteStorageProxyEngineConfig config, IEventLogger* logger) {
    auto itemLumps = NBlobStorage::NProxy::MakeRemoteStorageProxyLumpsStorage(
        NBlobStorage::NProxy::TItemLumpsStorageOptions{
            .Config = std::move(config),
            .SdManager = nullptr,
            .EventLogger = logger,
        });

    // Build item storage
    auto builder = NDoom::NItemStorage::TItemStorageBuilder{};

    for (auto&& index : snapshot.Index.Items) {
        builder.AddBackend(NDoom::NItemStorage::TItemStorageBackendBuilder{}
            .SetItemType(index.ItemType)
            .SetItemLumps(itemLumps)
            .Build());
    }

    return builder.Build();
}

enum class ETest {
    Items = 0b01,
    LightRequest = 0b10,
};
Y_DECLARE_FLAGS(ETests, ETest);
Y_DECLARE_OPERATORS_FOR_FLAGS(ETests);

inline void CheckRemoteStorageProxy(ETests tests, const NMock::TSnapshot& snapshot, TConstArrayRef<NMock::TRemoteStorage> storages, const NMock::TRemoteStorageProxy& proxy) {
    auto config = MakeConfig(snapshot, proxy.Port());
    auto storage = MakeItemStorage(snapshot, std::move(config), GetFakeEventLogger());

    if (tests.HasFlags(ETest::Items)) {
        TestItemLumps(snapshot.Index, storage);

        ui32 totalRequests = 0;
        ui32 totalSuccessLoads = 0;
        for (auto&& storage : storages) {
            totalRequests += storage.Metrics().NumRequests;
            totalSuccessLoads += storage.Metrics().SuccessLoads;
            storage.ResetMetrics();
        }
        UNIT_ASSERT_GT(totalRequests, 0);
        UNIT_ASSERT_GT(totalSuccessLoads, 0);
    }

    return;
    if (tests.HasFlags(ETest::LightRequest)) {
        TestContainsItemCheck(snapshot.Index, storage);
        for (auto&& storage : storages) {
            Cerr << "Requests: " << storage.Metrics().NumRequests << Endl;
            Cerr << "Failed: " << storage.Metrics().FailedLoads << Endl;
            Cerr << "Success: " << storage.Metrics().SuccessLoads << Endl;
            UNIT_ASSERT_EQUAL(storage.Metrics().NumRequests, 0);
        }
    }
}

inline void TestRemoteStorageProxy(ETests tests, const TIndexParams& params) {
    TFsPath dir = TFsPath::Cwd() / "runtime";

    TVector<NMock::TRemoteStorage> storages;
    for ([[maybe_unused]] ui32 _ : xrange(10)) {
        auto portHolder = NTesting::GetFreePort();
        ui16 port = portHolder;
        storages.emplace_back(NMock::TRemoteStorageOptions{
            .Port = std::move(portHolder),
            .Root = dir / "rs" / ToString(port),
        });
        storages.back().WaitUntilReady();
    }

    NMock::TRemoteStorageProxy proxy{NMock::TRemoteStorageProxyOptions{
        .Root = dir / "rsproxy",
    }};
    proxy.WaitUntilReady();
    Cerr << "Started rs proxy at port " << proxy.Port() << Endl;

    NMock::TWorker worker{"test_stream", dir / "worker", params};
    NMock::TSnapshot snapshot = worker.RunIteration();

    NMock::TController controller{dir / "controller"};
    controller.RunIteration({snapshot}, storages, proxy);

    CheckRemoteStorageProxy(tests, snapshot, storages, proxy);
}
