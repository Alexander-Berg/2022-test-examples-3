#include <crypta/lib/native/log/log.h>
#include <crypta/lib/native/log/proto/logs_config.pb.h>
#include <crypta/lib/native/pqlib/consumer.h>
#include <crypta/lib/native/pqlib/logger.h>
#include <crypta/lib/native/pqlib/producer.h>
#include <crypta/lib/native/stats/log/log_stats_sink.h>
#include <crypta/lib/native/stats/stats_writer.h>
#include <crypta/lib/native/yaml/parse_yaml_file.h>
#include <crypta/lib/native/yaml/yaml2proto.h>

#include <contrib/libs/yaml-cpp/include/yaml-cpp/yaml.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/singleton.h>
#include <util/stream/file.h>

using namespace NPersQueue;
using namespace NCrypta;
using namespace NCrypta::NPQ;

namespace {
    const TDuration MAX_FLUSH_INTERVAL = TDuration::Seconds(1);
    const TDuration READ_TIMEOUT = MAX_FLUSH_INTERVAL + TDuration::Seconds(1);

    template <typename TConfig>
    void SetServer(TConfig& config) {
        config.SetServer("localhost");
        config.SetPort(FromString<ui16>(TUnbufferedFileInput("ydb_endpoint.txt").ReadAll()));
    }

    TProducerConfig GetProducerConfig(const TString& topic) {
        TProducerConfig config;

        SetServer(config);
        config.SetTopic(topic);
        config.SetSourceIdPrefix("test");
        config.SetLogName("pqlib");
        config.SetProducersCount(10);
        config.SetMaxChunkSizeBytes(100);
        config.SetMaxFlushIntervalSeconds(MAX_FLUSH_INTERVAL.Seconds());

        return config;
    }

    TConsumerConfig GetConsumerConfig(const TString& topic) {
        TConsumerConfig config;

        SetServer(config);
        config.AddTopics(topic);
        config.SetClientId("test_client");
        config.SetUnpack(true);

        return config;
    }

    std::pair<TProducerConfig, TConsumerConfig> GetConfigs(const TString& topic) {
        return {GetProducerConfig(topic), GetConsumerConfig(topic)};
    }

    TAtomicSharedPtr<TPQLib> GetPQLib() {
        const auto yamlConfig = ParseYamlFile(ArcadiaSourceRoot() + "/crypta/lib/native/pqlib/test/data/config.yaml");
        const auto& config = Yaml2Proto<NLog::TLogsConfig>(yamlConfig);
        NLog::RegisterLogs(config.GetLogs());

        auto stats = Singleton<TStatsRegistry>();
        TStatsWriter statsWriter(*stats, TDuration::Minutes(1));
        statsWriter.AddSink(MakeHolder<TLogStatsSink>("graphite"));
        statsWriter.Start();

        TPQLibSettings pqLibSettings;
        pqLibSettings.ThreadsCount = 3;
        return MakeAtomicShared<TPQLib>(pqLibSettings);
    }

    struct TFixture {
        const TStats::TSettings StatsSettings;
        TStats Stats;
        TAtomicSharedPtr<TPQLib> PQLib;
        std::shared_ptr<ICredentialsProvider> CredentialsProvider;
        TIntrusivePtr<NPersQueue::ILogger> Logger;

        TFixture()
            : Stats("pqlib", StatsSettings)
            , PQLib(GetPQLib())
            , CredentialsProvider(NPersQueue::CreateInsecureCredentialsProvider())
            , Logger(NLogger::Create("pqlib"))
        {}
    };
}

Y_UNIT_TEST_SUITE(PQLib) {
    TString GetMessage(int i) {
        return TStringBuilder() << "message" << i;
    }

    void Test(TProducer& producer, TConsumer& consumer) {
        THashSet<TString> messages;

        for (auto i = 0; i < 1000; ++i) {
            auto msg = GetMessage(i);

            UNIT_ASSERT(producer.TryEnqueue(msg));

            messages.insert(msg);
        }

        auto result = consumer.GetNextData(READ_TIMEOUT);

        while (result.Defined()) {
            for (const auto& chunk : result->Data) {
                TStringStream stringStream(chunk);
                TString message;

                while (stringStream.ReadLine(message)) {
                    auto it = messages.find(message);
                    UNIT_ASSERT_C(it != messages.end(), chunk);

                    messages.erase(it);
                }
            }
            consumer.Commit({ result->EpochCookie });

            result = consumer.GetNextData(READ_TIMEOUT);
        }

        UNIT_ASSERT(messages.empty());
    }

    Y_UNIT_TEST(TProducer) {
        auto& fixture = *Singleton<TFixture>();
        const auto& [producerConfig, consumerSettings] = GetConfigs("TProducer");

        TProducer producer(fixture.PQLib, producerConfig, fixture.CredentialsProvider, fixture.StatsSettings);
        TConsumer consumer(fixture.PQLib, fixture.Logger, consumerSettings, fixture.CredentialsProvider, fixture.Stats);

        Test(producer, consumer);
    }

    Y_UNIT_TEST(TConsumerDoesNotLoseData) {
        auto& fixture = *Singleton<TFixture>();
        const auto& [producerConfig, consumerSettings] = GetConfigs("TConsumerDoesNotLoseData");

        TProducer producer(fixture.PQLib, producerConfig, fixture.CredentialsProvider, fixture.StatsSettings);
        TConsumer consumer(fixture.PQLib, fixture.Logger, consumerSettings, fixture.CredentialsProvider, fixture.Stats);

        UNIT_ASSERT(!consumer.GetNextData(READ_TIMEOUT).Defined());

        UNIT_ASSERT(producer.TryEnqueue("message"));
        UNIT_ASSERT(consumer.GetNextData(READ_TIMEOUT).Defined());
    }
}
