#include <search/app_host_ops/parser.h>
#include <search/daemons/noapacheupper/search_main/search_main.h>
#include <search/fuzzing/lib/common/app_host.h>
#include <search/fuzzing/lib/common/common.h>
#include <search/fuzzing/lib/common/fuzz_env.h>
#include <search/pseudo_server/arg_builder.h>
#include <search/pseudo_server/env.h>

#include <apphost/lib/client/client_shoot.h>
#include <apphost/lib/grpc/client/grpc_client.h>
#include <apphost/lib/grpc/json/service_request.h>
#include <apphost/lib/grpc/json/service_response.h>
#include <apphost/lib/grpc/protos/service.pb.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/string_utils/base64/base64.h>

#include <util/datetime/cputimer.h>
#include <util/folder/path.h>
#include <util/generic/algorithm.h>
#include <util/generic/hash.h>
#include <util/generic/yexception.h>
#include <util/random/mersenne.h>
#include <util/random/shuffle.h>
#include <util/stream/file.h>
#include <util/string/builder.h>
#include <util/string/strip.h>
#include <util/string/vector.h>
#include <util/system/env.h>
#include <util/system/mutex.h>


using NAppHost::NGrpc::NClient::IServiceClient;

namespace {
    constexpr TStringBuf GRPC_ADDRESS = "localhost:12193";
    const TString GRPC_DELIM = "\t";

    class TTrace {
    public:
        TTrace(TStringBuf event)
            : Event_(event)
            , Timer_()
        {
        }

        ~TTrace() {
            static TMutex mutex;
            TGuard<TMutex> lock(mutex);
            Cerr << "[" << TInstant::Now() << "] " << Event_ << " " << Timer_.Get() << Endl;
        }
    private:
        TStringBuf Event_;
        TSimpleTimer Timer_;
    };

#define TRACE() TTrace __FUNCTION__##Trace(__FUNCTION__);

    TString DecodeChunk(const TString& chunk) {
        TString res = chunk;
        try {
            res = Base64Decode(res);
        } catch (...) {
            // raw context data, no base64 encoding
        }
        return res;
    }

    TVector<TString> GetContextDatas(const uint8_t* data, size_t size) {
        TString sdata((const char*)data, size);

        TVector<TString> contextDatas;
        StringSplitter(sdata).SplitBySet(GRPC_DELIM.data()).SkipEmpty().Collect(&contextDatas);

        return contextDatas;
    }

    class TCrossOverPair {
    public:
        TCrossOverPair() = default;

        void SetFirst(const NJson::TJsonValue& lhs) {
            Lhs_ = lhs;
        }

        void SetSecond(const NJson::TJsonValue& rhs) {
            Rhs_ = rhs;
        }

        NJson::TJsonValue CrossOver(unsigned int seed) const {
            if (Lhs_.Defined() && Rhs_.Defined()) {
                NJson::TJsonValue lhs = *Lhs_;
                NFuzzing::NJsonTools::CrossOver(lhs, *Rhs_, seed);
                return lhs;
            } else if (Lhs_.Defined()) {
                return *Lhs_;
            } else if (Rhs_.Defined()) {
                return *Rhs_;
            } else {
                return NJson::JSON_NULL;
            }
        }

    private:
        TMaybe<NJson::TJsonValue> Lhs_;
        TMaybe<NJson::TJsonValue> Rhs_;
    };

    TVector<TString> DoCrossOver(const TVector<TString>& contextDatas1, const TVector<TString>& contextDatas2, unsigned int seed) {
        THashMap<TString, TCrossOverPair> sourceQueries;
        TVector<TVector<TString>> order;
        size_t ruid = 0;

        for (const auto& data : contextDatas1) {
            order.push_back({});

            try {
                const auto json =  NFuzzing::NAppHostTools::ServiceRequestToJson(data);

                for (const auto& source : json["answers"].GetArray()) {
                    const auto& name = source["name"].GetString();

                    if (!ruid) {
                        ruid = source["ruid"].GetUInteger();
                    }

                    order.back().push_back(name);

                    sourceQueries[name].SetFirst(source);
                }
            } catch (...) {
                Cout << CurrentExceptionMessage() << Endl;
            }
        }

        for (const auto& data : contextDatas2) {
            order.push_back({});

            try {
                const auto json =  NFuzzing::NAppHostTools::ServiceRequestToJson(data);

                for (const auto& source : json["answers"].GetArray()) {
                    const auto& name = source["name"].GetString();

                    if (sourceQueries.contains(name)) {
                        sourceQueries[name].SetSecond(source);
                    } else {
                        order.back().push_back(name);
                        sourceQueries[name].SetFirst(source);
                    }
                }
            } catch (...) {
                Cout << CurrentExceptionMessage() << Endl;
            }
        }

        if (sourceQueries.empty()) {
            return {};
        }

        NJson::TJsonValue defaultChunk;
        defaultChunk["ruid"] = ruid;
        defaultChunk["codecs"].AppendValue("lz4");
        defaultChunk["answers"] = NJson::JSON_ARRAY;

        TVector<TString> result;
        for (const auto& sourcesInChunk : order) {
            if (sourcesInChunk.empty()) {
                continue;
            }

            auto now = defaultChunk;
            for (const auto& source : sourcesInChunk) {
                now["answers"].AppendValue(sourceQueries[source].CrossOver(seed));
            }

            try {
                result.emplace_back(NFuzzing::NAppHostTools::JsonToServiceRequest(now));
            } catch (...) {
                // ¯\_(ツ)_/¯
            }
        }

        return result;
    }

    void FixSize(TVector<TString>& result, TString& str, const size_t maxSize, unsigned int seed) {
        int iterCount = 0;
        while (str.size() > maxSize) {
            iterCount++;
            NFuzzing::NJsonTools::Shrink(result, (str.size() - maxSize) * 1.75, seed);
            str = JoinStrings(result, GRPC_DELIM);
            Y_ENSURE(iterCount < 50,  "in cycle");
        }
    }
} // namespace

class TNoapacheEnvironment : public NSearch::NPseudoServer::TSearchEnvironment {
    class TNoapacheRunner : public NSearch::NPseudoServer::ISearchRunner {
    public:
        TString SearchName() const override {
            return "noapache_fuzzer";
        }

        ui16 HttpPort() const {
            return 12191;
        }

        ui16 SearchPort() const override {
            return 12192;
        }

        TRequest ShutdownRequest() const override {
            return {HttpPort(), "admin?action=shutdown"};
        }

        int SearchMain() override {
            NSearch::NPseudoServer::TArgBuilder args;
            args.Add(SearchName());
            args.Add("-d");

            const TString envArgs = GetEnv("NOAPACHE_FUZZER_ARGV");
            if (envArgs) {
                args.AddMany(envArgs);
            } else {
                args.Add(GetEnv("NOAPACHE_FUZZER_CONFIG", "./noapache.cfg"));
                args.Add("-V", TStringBuilder() << "Port=" << ToString(HttpPort()));
                args.Add("-V", TStringBuilder() << "AppHostPort=" << ToString(SearchPort()));
            }

            return NSearch::NNoapache::SearchMain(args.Argc(), args.Argv());
        }
    };

public:
    bool InitEnv(bool muteSearchLog = false) {
        return NSearch::NPseudoServer::TSearchEnvironment::InitEnv(new TNoapacheRunner(), muteSearchLog);
    }

    TString Search(const TString& /* query */, const TString& data) override {
        Cout << "Size of data is " << data.size() << Endl;

        IServiceClient& client = Sys_.ProvideServiceClient(GRPC_ADDRESS);

        NAppHost::NGrpc::NClient::TServiceSessionPtr session = client.StartSession();

        TVector<TString> contextDatas;
        StringSplitter(data).SplitBySet(GRPC_DELIM.data()).SkipEmpty().Collect(&contextDatas);

        Transform(contextDatas.begin(), contextDatas.end(), contextDatas.begin(), DecodeChunk);

        TVector<NAppHostProtocol::TServiceRequest> chunks;
        for (size_t i = 0; i < contextDatas.size(); ++i) {
            NAppHostProtocol::TServiceRequest request;

            const auto& contextData = contextDatas[i];

            Y_PROTOBUF_SUPPRESS_NODISCARD request.ParseFromArray(contextData.data(), contextData.size());

            request.SetPath("/");
            request.SetLast(i == contextDatas.size() - 1);

            chunks.emplace_back(std::move(request));
        }

        for (auto&& request : chunks) {
            session->SendRequest(std::move(request));
        }

        return {};
    }

private:
    NAppHost::NGrpc::NClient::TGrpcCommunicationSystem Sys_;
};

using TNoapacheFuzzingEnv = NSearch::NFuzzing::NCommon::TFuzzEnv<TNoapacheEnvironment>;

extern "C" {
    int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
        TRACE();
        static bool initialized = TNoapacheFuzzingEnv::DoInit(true);
        if (initialized) {
            return TNoapacheFuzzingEnv::TestOneInput(TString(), TString((const char*)data, size));
        }
        return 0;
    }

    size_t LLVMFuzzerCustomMutator(uint8_t *data, size_t size, size_t maxSize, unsigned int seed) {
        TRACE();

        TVector<TString> contextDatas = GetContextDatas(data, size);

        TVector<TString> result;
        for (auto&& it : contextDatas) {
            try {
                auto json = NFuzzing::NAppHostTools::ServiceRequestToJson(it);

                NFuzzing::NJsonTools::LeafFuzzing(json, seed, /* ratio */ 0.1);

                result.emplace_back(NFuzzing::NAppHostTools::JsonToServiceRequest(json));
            } catch (...) {
                Cout << "errLLVMFuzCustMut " << CurrentExceptionMessage() << Endl;
            }
        }

        TString str = JoinStrings(result, GRPC_DELIM);

        FixSize(result, str, maxSize, seed);

        return NFuzzing::Copy(std::move(str), data, maxSize);
    }

    size_t LLVMFuzzerCustomCrossOver(const uint8_t *data1, size_t size1,
        const uint8_t *data2, size_t size2,
        uint8_t *out, size_t maxOutSize,
        unsigned int seed)
    {
        TRACE();

        const auto contextDatas1 = GetContextDatas(data1, size1);
        const auto contextDatas2 = GetContextDatas(data2, size2);

        auto result = DoCrossOver(contextDatas1, contextDatas2, seed);
        TString str = JoinStrings(result, GRPC_DELIM);

        FixSize(result, str, maxOutSize, seed);

        return NFuzzing::Copy(std::move(str), out, maxOutSize);
    }
}
