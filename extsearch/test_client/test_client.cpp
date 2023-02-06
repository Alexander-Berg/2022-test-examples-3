#include <library/cpp/getopt/modchooser.h>

#include <extsearch/audio/generative/cpp/backend/library/playlist/m3u8.h>
#include <extsearch/audio/generative/cpp/backend/library/pregenerated/pregeneratedindexload.h>
#include <extsearch/audio/generative/cpp/backend/library/proto/generative.pb.h>
#include <extsearch/audio/generative/cpp/backend/library/recommender/debugrecommenderapi.h>
#include <extsearch/audio/generative/cpp/backend/library/sampleloop/sampleloopindexload.h>
#include <extsearch/audio/generative/cpp/backend/library/sessionmanager/sessionmanager.h>
#include <extsearch/audio/generative/cpp/backend/library/util/wrapstdlib.h>

#include <library/cpp/http/client/client.h>
#include <library/cpp/http/server/http.h>
#include <library/cpp/logger/filter.h>
#include <library/cpp/logger/stream.h>
#include <library/cpp/sighandler/async_signals_handler.h>

#include <util/generic/cast.h>
#include <util/random/random.h>
#include <util/stream/file.h>

#include <thread>

using namespace NGenerative;

class TFakeUserFeedback : public IUserFeedback {
public:
    TVector<TFeedbackEvent> FindUserEvents(uint64_t) override {
        return TVector<TFeedbackEvent>();
    }

    TVector<TFeedbackEvent> FindSessionUserEvents(uint64_t, const TString&) override {
        return TVector<TFeedbackEvent>();
    }

    void InsertEvent(const TFeedbackEvent&) override {

    }

    void InsertEventAsync(TFeedbackEvent&&) override {
    }

    void UpdateEvent(const TFeedbackEvent&) override {

    }

    void UpdateEventAsync(TFeedbackEvent&&) override {
    }
};

class TFakeBlobStorage : public IBlobStorage {
public:
    void SaveBlob(const TString&, const TString&) override {

    }

    TString GetBlob(const TString&) override {
        return TString();
    }

    void UpdateBlobTimestamp(const TString&) override {

    }
};

class TTestClientRequestScheduler : private TGenericLogger {
private:
    static constexpr size_t ClientsPerFetcher = 64;
    static constexpr size_t CoRoutinesPerFetcher = 16;
public:

    TTestClientRequestScheduler(TLog* log, uint32_t numberOfClients, uint32_t minDelayMs, uint32_t maxDelayMs)
        : TGenericLogger(log, "test_client")
        , PregenIndex(GetParentLog())
        , Manager(GetParentLog(), PregenIndex, Index, Excl, FakeRecommender, AudioConfig, BackendConfig) {

        LoadLoopInfoIndex(BackendConfig.LoopsInfoDbPath, *this, Index);
        LoadPregeneratedIndex(BackendConfig.PregenInfoDbPath, *this, PregenIndex, Index);
        TFileInput in(BackendConfig.CrossbredExclusionsConfigPath);
        Excl.LoadFromJson(NJson::ReadJsonTree(&in));

        TInstant ts = TInstant::Now() + TDuration::Seconds(1);
        for (uint32_t c = 0; c < numberOfClients; ++c) {
            NProto::TGetStreamV2Input input;
            input.MutableAllowedGenres()->Add("rock");
            input.MutableAllowedGenres()->Add("pop");
            input.MutableAllowedGenres()->Add("electronic");
            input.MutableAllowedGenres()->Add("hiphop");
            input.SetMinTemperature(1);
            input.SetMaxTemperature(5);
            TVector<std::pair<TMetricsListener::EExtraTiming, TDuration>> et;
            auto sessId = ToString(c);
            auto session = Manager.BuildSession(sessId, "", ts, input, et, &FakeUserFeedback, &FakeBlobStorage);
            session.MutableUserInfo()->SetUser(c);
            Clients.emplace(ts, MakeHolder<TVirtualClient>(c, ToString(c), std::move(session)));
            ts += TDuration::MilliSeconds(minDelayMs + RandomNumber(maxDelayMs - minDelayMs));
        }
        for (size_t i = 0; i < (numberOfClients + ClientsPerFetcher - 1) / ClientsPerFetcher; ++i) {
            NHttp::TClientOptions fetchOptions;
            fetchOptions.SetFetchCoroutines(CoRoutinesPerFetcher);
            FetchClients.push_back(MakeHolder<NHttp::TFetchClient>(fetchOptions));
        }
    }

    void Process() {
        while (!ShutdownFlag.load(std::memory_order_relaxed)) {
            auto now = TInstant::Now();
            for (auto it = Clients.begin(); it->first < now; it = Clients.begin()) {
                auto ts = it->first;
                auto client = std::move(it->second);
                if (client->State == EVirtualClientState::Idle) {
                    ChangeState(*client, EVirtualClientState::Ready);
                }
                Clients.erase(it);
                ts += ProcessClient(now, *client, false);
                Clients.emplace(ts, std::move(client));
            }
            Sleep(now - Clients.begin()->first);
        }
    }

    void DebugProcess() {
        auto now = TInstant::Now();
        for (size_t cnt = 0; cnt < 500; ++cnt) {
            for (auto it = Clients.begin(); it->first < now; it = Clients.begin()) {
                auto ts = it->first;
                auto client = std::move(it->second);
                if (client->State == EVirtualClientState::Idle) {
                    ChangeState(*client, EVirtualClientState::Ready);
                }
                Clients.erase(it);
                ts += ProcessClient(now, *client, true);
                Clients.emplace(ts, std::move(client));
            }
            now = Clients.begin()->first + TDuration::MilliSeconds(5);
        }
    }

    void Shutdown() {
        ShutdownFlag.store(true, std::memory_order_release);
        ShutownEvent.Signal();
        FetchClients.clear();
    }

private:

    enum class EVirtualClientState {
        Idle,
        Ready,
        Downloading
    };

    struct TVirtualClient {
        size_t Idx;
        TString Id;
        EVirtualClientState State = EVirtualClientState::Idle;
        NProto::TGenerativeSession Session;
        uint32_t LastSeq = 0;

        TVirtualClient(size_t idx, TString id, NProto::TGenerativeSession&& session)
            : Idx(idx)
            , Id(std::move(id))
            , Session(std::move(session)) {
        }
    };

private:

    TDuration ProcessClient(TInstant now, TVirtualClient& client, bool debugMode) {
        TGenerativeQueue queue(GetParentLog(), client.Id, NProto::TGenerativeSession(client.Session),
                               PregenIndex, Manager.GetGenerator(), BackendConfig, AudioConfig);

        auto updateTs = now + TDuration::Seconds(AudioConfig.PlaylistUpdateThresholdSec);
        bool needUpdate = queue.IsLastItemAt(updateTs);
        if (needUpdate) {
            Manager.AddGaplessContinuation(client.Id, client.Session, now, &FakeUserFeedback, &FakeBlobStorage);
        }

        TString vsid;
        vsid.reserve(44 + 1 + 3 + 1 + 4 + 1 + 10 + 1);
        if (client.Id.length() > 44u) {
            vsid = TStringBuf(client.Id).substr(0u, 44u);
        } else {
            vsid.resize(44 - client.Id.length(), '0');
            vsid += client.Id;
        }
        vsid += "xGENx";
        vsid += "0000";
        vsid += 'x';
        vsid += ToString(queue.GetSessionStart().Seconds());
        auto pl = queue.GetPlaylist(now, vsid);
        if (pl.GetList().empty()) {
            return TDuration::MilliSeconds(10);
        }
        const auto& item = pl.GetList()[0];
        if (debugMode) {
            Y_ENSURE(client.LastSeq == 0 || pl.GetSeq() - client.LastSeq == 1);
            Info() << "Seq = " << pl.GetSeq();
            for (const auto& plItem:pl.GetList()) {
                Info() << "Duration: " << plItem.Duration << ", Url: " << plItem.Url;
            }
        } else {
            ChangeState(client, EVirtualClientState::Downloading);
            auto& fetcher = *FetchClients[client.Idx / ClientsPerFetcher];
            fetcher.Fetch(item.Url, [this, id = client.Id, url = item.Url, seq = pl.GetSeq(), now](const auto& result) {
                ProcessFetchResult(id, url, seq, result, now);
            });
        }
        return TDuration::Seconds(item.Duration);
    }

    void ProcessFetchResult(const TString& id, const TString& url, uint32_t seq,
                            const NHttpFetcher::TResultRef& result, TInstant start) {
        if (result->Code != 200) {
            Info() << "Id: " << id << ", Seq: " << seq << ", Code: " << result->Code << ", DataLength: "
                   << result->Data.length() << ", Url: " << url;
        } else {
            Info() << "Id: " << id << ", Seq: " << seq << ", Code: " << result->Code << ", DataLength: "
                   << result->Data.length() << ", DurationMs: " << (TInstant::Now() - start).MilliSeconds();
        }
    }

    void ChangeState(TVirtualClient& client, EVirtualClientState newState) {
        if (client.State == newState) {
            return;
        }
        Info() << "Changing state of " << client.Id << " from " << ToUnderlying(client.State) <<
               " to " << ToUnderlying(newState);
        client.State = newState;
    }

private:

    TPregeneratedIndex PregenIndex;
    TLoopInfoIndex Index;
    TCrossbredExclusions Excl;
    TDebugRecommenderApi FakeRecommender;
    TAudioConfig AudioConfig;
    TBackendConfig BackendConfig;
    TFakeBlobStorage FakeBlobStorage;
    TFakeUserFeedback FakeUserFeedback;
    TSessionManager Manager;
    TMap<TInstant, THolder<TVirtualClient>> Clients;
    TVector<THolder<NHttp::TFetchClient>> FetchClients;
    TSystemEvent ShutownEvent;
    std::atomic_bool ShutdownFlag{false};
};

class TTestClientHttpCallback : public THttpServer::ICallBack {
public:
    TClientRequest* CreateClient() override {
        return nullptr;
    }

private:
    class TClientRequestImpl : public TClientRequest {
    public:
    private:
        bool Reply(void*) override {
            return true;
        }
    };

private:
};

class TMain : public TMainClassArgs {
protected:
    void RegisterOptions(NLastGetopt::TOpts& opts) override {
        opts.AddLongOption('n', "clients")
            .Help("Number of concurrent virtual clients")
            .DefaultValue(100)
            .StoreResult(&NumberOfClients);
        opts.AddLongOption('m', "delay_min")
            .Help("Minimum delay between clients start in milliseconds")
            .DefaultValue(1000)
            .StoreResult(&StartDelayMinMs);
        opts.AddLongOption('x', "delay_max")
            .Help("Maximum delay between clients start in milliseconds")
            .DefaultValue(2000)
            .StoreResult(&StartDelayMaxMs);
        opts.AddLongOption('p', "http_port")
            .Help("Http port of internal control web server")
            .DefaultValue(9090)
            .StoreResult(&HttpPort);
        opts.AddLongOption('d', "debug")
            .Help("Enable debug mode")
            .StoreTrue(&DebugMode);
    }

    int DoRun(NLastGetopt::TOptsParseResult&&) override {
        const size_t numberOfThreads = 4;
        const size_t maxConns = 4;

        auto logOut = TFileOutput("test_client.log");
        auto outLog = MakeHolder<TStreamLogBackend>(&logOut);
        auto filter = MakeHolder<TFilteredLogBackend>(std::move(outLog),
                                                                         DebugMode ? TLOG_DEBUG : TLOG_INFO);
        auto logBase = MakeHolder<TLog>(std::move(filter));
        TTestClientRequestScheduler testClient(logBase.Get(), NumberOfClients, StartDelayMinMs, StartDelayMaxMs);
        if (DebugMode) {
            testClient.DebugProcess();
        } else {

            std::thread tcRunner([&testClient]() {
                testClient.Process();
            });

            TTestClientHttpCallback cb;
            THttpServer::TOptions opt(HttpPort);
            opt.SetThreads(numberOfThreads);
            opt.SetMaxConnections(maxConns);
            THttpServer httpServer(&cb, opt);

            auto shutdown = [&httpServer](int) {
                httpServer.Shutdown();
            };

            SetAsyncSignalFunction(SIGTERM, shutdown);
            SetAsyncSignalFunction(SIGINT, shutdown);

            Cout << "Listening http port " << HttpPort << Endl;
            httpServer.Start();
            Cout << "Running" << Endl;
            httpServer.Wait();
            testClient.Shutdown();
            tcRunner.join();
            Cout << "Shutdown" << Endl;
        }

        return EXIT_SUCCESS;
    }

private:
    uint32_t NumberOfClients;
    uint32_t StartDelayMinMs;
    uint32_t StartDelayMaxMs;
    uint16_t HttpPort;
    bool DebugMode = false;
};

int main(int argc, const char* argv[]) {
    try {
        return TMain().Run(argc, argv);
    } catch (...) {
        Cerr << "Exception: " << CurrentExceptionMessage() << Endl;
        return EXIT_FAILURE;
    }
}
