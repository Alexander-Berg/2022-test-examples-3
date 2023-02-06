#pragma once

#include <util/stream/file.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>
#include <mail/so/libs/talkative_config/config.h>

enum class TPathType {
    ARCADIA,
};

static TString FormatJson(const TString& data) {
    NJson::TJsonValue value;
    NJson::ReadJsonTree(data, &value, true);
    return NJson::WriteJson(value, true, true, true);
}

class TTestFsPath {
public:
    static constexpr std::string_view ARCADIA_PREFIX = "arcadia/";
    TString ReadJson() const {
        TFileInput file(Path);
        return FormatJson(file.ReadAll());
    }

    explicit TTestFsPath(const NConfig::TConfig& config) {
        TStringBuf path = NTalkativeConfig::Get<TString>(config, "path");
        const TPathType type = [&config, &path](){
            if(const TString * t = NTalkativeConfig::Find<TString>(config, "type")) {
                return FromString<TPathType>(*t);
            } else if(path.SkipPrefix(ARCADIA_PREFIX)) {
                return TPathType::ARCADIA;
            } else {
                ythrow yexception() << "cannot parse path from " << config;
            }
        }();

        switch (type) {
            case TPathType::ARCADIA:
                Path = JoinFsPaths(ArcadiaSourceRoot(), path);
                break;
        }

        Path.CheckExists();
    }
private:
    TFsPath Path;
};

class TTestSampleConfig {
public:
    const TTestFsPath GetPathToSo2Request() const {
        return PathToSo2Request;
    }

    const TTestFsPath GetPathToSo2Response() const {
        return PathToSo2Response;
    }

    explicit TTestSampleConfig(const NConfig::TConfig& config)
    : PathToSo2Request(NTalkativeConfig::Get(config, "so2-request")) 
    , PathToSo2Response(NTalkativeConfig::Get(config, "so2-response")) {
    }
private:
    TTestFsPath PathToSo2Request;
    TTestFsPath PathToSo2Response;
};

class TSpDaemonTestConfig {
public:
    const std::vector<TTestSampleConfig> GetSamples() const {
        return Samples;
    }

    explicit TSpDaemonTestConfig(const NConfig::TConfig& config) {
        const NConfig::TArray& samplesConfigs = NTalkativeConfig::Get<NConfig::TArray>(config, "samples");
        Samples.reserve(samplesConfigs.size());

        for(const NConfig::TConfig& sampleConfig : samplesConfigs) {
            Samples.emplace_back(sampleConfig);
        }
    }
private:
    std::vector<TTestSampleConfig> Samples;
};
