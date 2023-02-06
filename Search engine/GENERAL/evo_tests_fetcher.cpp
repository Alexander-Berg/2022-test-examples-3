#include "evo_tests_fetcher.h"
#include "library/cpp/yson/node/node.h"
#include "search/scraper_over_yt/mapper/lib/alice_binary_holder.h"
#include "search/scraper_over_yt/mapper/lib/deep_uniproxy_fetcher.h"
#include "search/scraper_over_yt/mapper/lib/fetcher.h"
#include "util/generic/fwd.h"
#include "util/generic/yexception.h"
#include "util/system/file.h"
#include "util/system/tempfile.h"
#include "util/system/types.h"
#include "utils.h"

#include <search/scraper_over_yt/worker/abort_exception.h>
#include <search/scraper_over_yt/mapper/lib/proto/voice_output.pb.h>

#include <alice/uniproxy/mapper/fetcher/lib/fill_result.h>
#include <alice/uniproxy/mapper/fetcher/lib/request.h>
#include <alice/uniproxy/mapper/library/logging/logging.h>

#include <alice/library/json/json.h>

#include <library/cpp/logger/filter.h>
#include <library/cpp/logger/stream.h>
#include <library/cpp/protobuf/yt/proto2yt.h>
#include <library/cpp/protobuf/yt/yt2proto.h>
#include <library/cpp/resource/resource.h>
#include <library/cpp/timezone_conversion/civil.h>
#include <library/cpp/yson/node/node_io.h>

#include <sys/stat.h>
#include <util/datetime/base.h>
#include <util/datetime/parser.h>
#include <util/generic/hash_set.h>

#include <string>
#include <optional>
#include <utility>

namespace NScraperOverYT {
    using namespace NAlice::NUniproxy;
    using namespace NAlice::NUniproxy::NFetcher;

    namespace {
        TString InitEvoBinaryHolder(TMaybe<TUniproxyBinaryHolder>& binaryHolder, const TUniproxyFetcherInputParams& params,
                                    TFlagsContainer* flags, TLog* logger, const TString& EvoTestsFilters, const TString& workerId) {
            SOY_CRITICAL_ENSURE(params.HasBinaryParams(), "Empty BinaryParams, invalid input");
            NJson::TJsonValue binParams;
            const auto status = NJson::ReadJsonFastTree(params.GetBinaryParams(), &binParams,
                                                        /* throwOnError */ false,
                                                        /* notClosedBracketIsError */ true);
            SOY_CRITICAL_ENSURE(status, "Can not parse json in BinaryParams, invalid input");

            TFsPath currentDir = NFs::CurrentWorkingDirectory();
            TFsPath evoDir = (currentDir / binParams["evo_dir"].GetString()).Fix();
            Mkdir(evoDir.GetName().data(), MODE0777);

            TString fromEvoPath = (evoDir / binParams["from_evo_path"].GetString()).Fix();
            TString toEvoPath = (evoDir / binParams["to_evo_path"].GetString()).Fix();
            fromEvoPath += workerId;
            toEvoPath += workerId;
            TString shellCommand = TStringBuilder{} << params.GetBinaryPath() << " --test-param scraper-mode=True --test-param debug=True --output-dir \"./\""
                                                    << " --test-param from-evo-fifo=" << fromEvoPath
                                                    << " --test-param to-evo-fifo=" << toEvoPath << EvoTestsFilters;

            binaryHolder.ConstructInPlace(params.GetBinaryPath(), std::move(binParams), fromEvoPath, toEvoPath, flags, logger, shellCommand, /*NeedReset*/ false);
            return shellCommand;
        }

        TString GetIsoTimestamp(const TMaybe<TString>& timezone = {}) {
            auto tz = NDatetime::GetUtcTimeZone();
            if (timezone) {
                tz = NDatetime::GetTimeZone(timezone.Get()->c_str());
            }
            return NDatetime::Format("%Y%m%dT%H%M%S", NDatetime::Convert(TInstant::Now(), tz), tz);
        }

        TString GetBasket(const NYT::TNode& row) {
            if (!row.IsString()) {
                const auto& requests = row["session_requests"].AsList();
                const TString basketKey = "basket";
                for (const auto& request : requests) {
                    if (request.HasKey(basketKey) && request[basketKey].HasValue() &&
                        !request[basketKey].IsNull() && !request[basketKey].AsString().Empty())
                    {
                        return request[basketKey].AsString();
                    }
                }
            }
            return "input_basket";
        }
    }

    TEvoTestsFetcher::TEvoTestsFetcher(const TFetcherCommonParams& commonParams, const TString& resolveTablePath, const TString& args)
        : TDeepUniproxyFetcher(commonParams, resolveTablePath, args)
    {
    }

    TEvoTestsFetcher::TEvoTestsFetcher(const TUniproxyFetcherInputParams& params)
        : TDeepUniproxyFetcher(params)
    {
    }

    void TEvoTestsFetcher::ConstructUniproxySettings(TMaybe<TUniproxyRequestPerformer>& performer, const TMaybe<NYT::TNode>& requestEVO) {
        TParseConfig parseConfig{};
        parseConfig.UseYtExtention = true;
        TVoiceInput request;
        YtNodeToProto((*requestEVO)["payload"], request, parseConfig);

        TString rowClientTime;
        TString updatedClientTime;
        auto isoTimestamp = GetIsoTimestamp();
        rowClientTime = request.GetClientTime();
        updatedClientTime = rowClientTime.empty() ? isoTimestamp : rowClientTime;

        // Mock callback function
        auto statisticsCallback = [&](const TAddressAndPort& remote) {
            if (remote.Port) {
                return;
            }
        };

        performer.ConstructInPlace(request, Params.GetUniproxyUrl(), Params.GetAuthToken(),
                                   Params.GetVinsUrl(), updatedClientTime, Params.GetAsrChunkSize(),
                                   Params.GetAsrChunkDelayMs(), &Log.Logger, &Flags, statisticsCallback);
    }

    void TEvoTestsFetcher::DebugRequest(const TMaybe<NYT::TNode>& requestEVO, const char requestMapper[]) {
        if (!requestEVO) {
            SOY_LOG_INFO("Request from EVO after \"%v\" is empty", requestMapper);
        } else {
            auto strRequest = NYT::NodeToYsonString(*requestEVO, NYson::EYsonFormat::Text);
            SOY_LOG_INFO("Request from EVO after \"%v\" is not empty. Request: %v", requestMapper, strRequest);
        }
    }

    void TEvoTestsFetcher::DebugFile(const TString& fileName) {
        TString out;
        auto inputFile = TFileInput(fileName);
        while (inputFile.ReadLine(out) > 0) {
            SOY_LOG_INFO("%v", out);
        }
    }

    void TEvoTestsFetcher::DebugEvoTestsEnd() {
        TString out;
        SOY_LOG_INFO("Output: ")
        TStringBuilder bufStdout;
        TString status = BinaryHolder->StdoutStatus();
        while (!status.Empty()) {
            bufStdout << status;
            status = BinaryHolder->StdoutStatus();
        }
        SOY_LOG_INFO("%v", bufStdout);

        SOY_LOG_INFO("Stderr: ")
        SOY_LOG_INFO("%v", BinaryHolder->StderrStatus());
        TStringBuilder bufStderr;
        status = BinaryHolder->StderrStatus();
        while (!status.Empty()) {
            bufStderr << status;
            status = BinaryHolder->StderrStatus();
        }
        SOY_LOG_INFO("%v", bufStderr);
    }

    TString TEvoTestsFetcher::ParseEvoRequest(const TMaybe<NYT::TNode>& requestEVO, TMaybe<TUniproxyRequestPerformer>& performer, TRowHandleResult& handleResult, const TString& basket) {
        if (!requestEVO) {
            ythrow TWrongEvoRequest() << "Evo request is empty";
        } else if (!(*requestEVO).HasKey("type")) {
            ythrow TWrongEvoRequest() << "Evo request has not key \"type\"";
        } else if ((*requestEVO)["type"] != "next_request" && (*requestEVO)["type"] != "end" &&
                   (*requestEVO)["type"] != "uniproxy_settings" && (*requestEVO)["type"] != "test_result") {
            ythrow TWrongEvoRequest() << "Evo request has unexpected \"type\": " << (*requestEVO)["type"].AsString();
        }

        if ((*requestEVO)["type"] == "uniproxy_settings") {
            SOY_LOG_INFO("Updating uniproxy settings");
            ConstructUniproxySettings(performer, requestEVO);
        } else if ((*requestEVO)["type"] == "test_result") {
            SOY_LOG_INFO("Add new testt result");
            constexpr int successOutputIndex = 0;
            constexpr int failedOutputIndex = 1;
            ++Statistics.Total;
            auto outputIndex = failedOutputIndex;
            if ((*requestEVO)["payload"]["test_status"] == "passed") {
                ++Statistics.Successes;
                outputIndex = successOutputIndex;
            }
            TFetchResult result;
            TVoiceOutput voiceOutput;
            voiceOutput.SetLog(NYT::NodeToYsonString((*requestEVO)["payload"], NYson::EYsonFormat::Text));
            result.SetFetchedResult(voiceOutput.SerializeAsString());
            result.SetValidationResult(outputIndex == successOutputIndex);
            MarkResourcesForScheduler(result, CachedBaskets.contains(basket));
            handleResult.OutputRows.push_back({result, outputIndex});
            SOY_LOG_INFO("Result was added");
        }

        return (*requestEVO)["type"].AsString();
    }

    TMaybe<NYT::TNode> TEvoTestsFetcher::GetUniproxyClientRespose(const TMaybe<NYT::TNode>& requestEVO, TUniproxyRequestPerformer& performer) {
        const TDuration baseThrottleTime = TDuration::MilliSeconds(500);
        TParseConfig parseConfig{};
        parseConfig.UseYtExtention = true;
        parseConfig.CastRobust = true;

        TFetchResult result;
        TVoiceInput request;
        YtNodeToProto((*requestEVO)["payload"], request, parseConfig);
        SOY_LOG_INFO("Text from evo request: %v", request.GetTextData());
        TVoiceOutput voiceOutput;
        auto retries = Params.GetRetriesPerRequest();
        ui32 tunnellerRetries = Params.GetTunnellerValidationRetriesPerRequest();
        while (retries > 0) {
            --retries;
            SOY_LOG_INFO("Send request to uniproxy");
            auto responses = PerformUniproxyRequestInsideSession(performer, request);
            SOY_LOG_INFO("Responses recieved. Count of responses: %v", responses.size());
            FillResult(responses, voiceOutput);

            std::pair<TString, bool> tunnellerValidation = CheckTunnellerAnswer(voiceOutput.GetVinsResponse(), result);
            if (!tunnellerValidation.second) {
                --tunnellerRetries;
                SOY_LOG_WARNING("Some problem with validation result! Error is: %v",
                                tunnellerValidation.first);
                if (tunnellerRetries <= 0) {
                    ythrow TWebUnanswerFail()
                        << "Retries for tunneller validation are over. Last error was: "
                        << tunnellerValidation.first;
                }
                Sleep(baseThrottleTime *
                      (Params.GetTunnellerValidationRetriesPerRequest() - tunnellerRetries));
                continue;
            }

            SOY_LOG_INFO("Done with %v retries", (Params.GetRetriesPerRequest() - retries));
            return ProtoToYtNode(voiceOutput);
        }
        return Nothing();
    }

    TRowHandleResult TEvoTestsFetcher::ProcessRequests(const NYT::TNode& row) {
        SOY_LOG_INFO("Row %v", Statistics.Rows);
        if (Flags.Has("test_flag")) {
            SOY_LOG_INFO("The flag \"test_flag\" set");
        }

        auto basket = GetBasket(row);
        constexpr int failedOutputIndex = 1;
        TRowHandleResult handleResult;
        ui32 retries = Params.GetRetriesPerRequest();
        TMaybe<TUniproxyRequestPerformer> performer;
        TMaybe<NYT::TNode> requestEVO;

        while (retries > 0) {
            --retries;
            try {
                requestEVO = RunBinHolder(BinaryHolder.GetRef(), "start", NYT::TNode(""), /*NeedWritePayload*/ false);
                DebugRequest(requestEVO, "start");
                if (!requestEVO) {
                    SOY_LOG_INFO("Evo tests didn't send any responses");
                    ythrow TBinaryHolderFail() << "No responses recieved from EVO after \"start\" command";
                }
                break;
            } catch (...) {
                auto const error = CurrentExceptionMessage();
                SOY_LOG_WARNING("Binary holder start and synchronize state failed: %v", error);
                if (retries <= 0) {
                    TFetchResult result;
                    Log.LogStream.Flush();
                    // SetErrorResult(result, error, row);
                    handleResult.OutputRows.push_back({std::move(result), failedOutputIndex});
                    DebugEvoTestsEnd();
                    return handleResult;
                }
            }
        }
        auto shouldStop = false;
        while (!shouldStop) {
            ui32 retries = Params.GetRetriesPerRequest();
            while (retries > 0) {
                --retries;
                try {
                    auto requestName = ParseEvoRequest(requestEVO, performer, handleResult, basket);
                    auto payload = TMaybe<NYT::TNode>("");
                    if (requestName == "next_request") {
                        payload = GetUniproxyClientRespose(requestEVO, performer.GetRef());
                        if (!payload) {
                            ythrow TEmptyUniproxyResponse() << "No responses received from uniproxy";
                        }
                        requestEVO = RunBinHolder(BinaryHolder.GetRef(), "continue", *payload, /*NeedWritePayload*/ true);
                        DebugRequest(requestEVO, "continue");
                    } else if (requestName != "end" && requestName != "exception" && requestName != "test_result") {
                        SOY_LOG_INFO("Run binHolder \"continue\" command");
                        requestEVO = RunBinHolder(BinaryHolder.GetRef(), "get_request", *payload, /*NeedWritePayload*/ false);
                        DebugRequest(requestEVO, "get_request");
                    } else {
                        SOY_LOG_INFO("Stop command from evo tests: %v", requestName);
                        shouldStop = true;
                    }
                    retries = 0;
                } catch (const TMapperCriticalException&) {
                    throw;
                } catch (const NDynamic::TWorkerAbortException&) {
                    throw;
                } catch (...) {
                    auto const error = CurrentExceptionMessage();
                    SOY_LOG_WARNING("Failed: %v. Retries: %v", error, retries);
                    if (retries <= 0) {
                        TFetchResult result;
                        // SetErrorResult(result, error, row);
                        handleResult.OutputRows.push_back({std::move(result), failedOutputIndex});
                        shouldStop = true;
                    }
                }
            }
            Log.LogStream.Flush();
            SOY_LOG_INFO("Full request log:");
            SOY_LOG_INFO(Log.LogData);
            Log.LogData.clear();
        } // while (!shouldStop)
        DebugEvoTestsEnd();
        return handleResult;
    }

    void TEvoTestsFetcher::StartEvoTests(const TString& evoTestsFilters) {
        SOY_LOG_INFO("%v", evoTestsFilters);
        auto p = InitEvoBinaryHolder(BinaryHolder, Params, &Flags, &Log.Logger, evoTestsFilters, WorkerId);
        SOY_LOG_INFO("Command: %v", p);
        auto& binaryHolder = BinaryHolder.GetRef();
        binaryHolder.Reset();

        auto retryDelay = TDuration::MilliSeconds(Params.GetEvoTestsStartDelayMs());
        ui32 totalRetriesCount = Params.GetEvoTestsStartRetries();
        auto retryCount = totalRetriesCount;
        while (retryCount > 0) {
            if (!binaryHolder.IsRunnable()) {
                ythrow TEvoTestsBinaryFailed() << "Binary holder not running. Count of retries: " << totalRetriesCount - retryCount << ". Total retries: " << totalRetriesCount;
            }
            SOY_LOG_INFO("Waiting. Retry iteration: %v", totalRetriesCount - retryCount);
            TMaybe<TString> rawEvoTestsOutput;
            try {
                rawEvoTestsOutput = binaryHolder.GetOutputData(retryDelay);
            } catch(...) {
                auto const error = CurrentExceptionMessage();
                SOY_LOG_WARNING("Failed to read request from evo. Error: %v.", error);
                Sleep(retryDelay);
            }
            if (!rawEvoTestsOutput) {
                SOY_LOG_INFO("Still no answer from the binary.");
                --retryCount;
                continue;
            }
            auto evoTestsOutput = NYT::NodeFromYsonString(TStringBuf(*rawEvoTestsOutput), ::NYson::EYsonType::Node);
            if (evoTestsOutput["type"] != "evo_tests_started") {
                ythrow TEvoTestsBinaryFailed() << "Evo tests send wrong request. Request type: " << evoTestsOutput["type"].AsString();
            }
            // Evo tests start successfully
            return;
        }

        if (retryCount <= 0) {
            ythrow TEvoTestsBinaryFailed() << "Evo tests build is too long. Count of retries: " << totalRetriesCount;
        }
    }

    void TEvoTestsFetcher::AddEvoTestsFilter(TStringBuilder& evoTestsFilters, const NYT::TNode& row) const {
        evoTestsFilters << " --test-filter " << row["session_requests"][0]["test_filter"].AsString() << "[*";
    }

    void TEvoTestsFetcher::BuildEvoTestsBinaryTesting(const NYT::TNode& rows, bool oneRow) {
        try {
            SOY_LOG_INFO("Start building evo tests");
            TStringBuilder evoTestsFilters;
            if (oneRow) {
                AddEvoTestsFilter(evoTestsFilters, rows);
            } else {
                for (auto& row : rows.AsList()) {
                    AddEvoTestsFilter(evoTestsFilters, row);
                }
            }
            StartEvoTests(evoTestsFilters);
        } catch (...) {
            auto const error = CurrentExceptionMessage();
            SOY_LOG_WARNING("Failed to start evo tests. Error: %v.", error);
            DebugEvoTestsEnd();
            throw;
        }
    }

    ui64 TEvoTestsFetcher::BuildEvoTestsBinary(NYT::TTableReader<NYT::TNode>* input) {
        try {
            SOY_LOG_INFO("Start building evo tests");
            ui64 testsCount = 0;
            TStringBuilder evoTestsFilters;
            for (auto& cursor : *input) {
                auto row = cursor.GetRow();
                testsCount += row["session_requests"][0]["tests_count"].AsInt64();
                AddEvoTestsFilter(evoTestsFilters, row);
            }
            StartEvoTests(evoTestsFilters);
            return testsCount;
        } catch (...) {
            auto const error = CurrentExceptionMessage();
            SOY_LOG_WARNING("Failed to start evo tests. Error: %v.", error);
            DebugEvoTestsEnd();
            throw;
        }
    }

    void TEvoTestsFetcher::Do(NYT::TTableReader<NYT::TNode>* input, NYT::TTableWriter<TFetchResult>* output) {
        if (Flags.Has("alice_evo_tests")) {
            ui64 testsCount = 0;
            try {
                testsCount = BuildEvoTestsBinary(input);
            } catch (...) {
                SOY_LOG_INFO("Failed to build evo tests binary");
                return;
            }
            SOY_LOG_INFO("Evo tests started successfully. Tests count: %v", testsCount);
            for (ui64 test = 0; test < testsCount; ++test) {
                SOY_LOG_INFO("Test: %v", test);
                auto result = ProcessRequests(NYT::TNode(""));
                SOY_LOG_INFO("Test finished: %v", test);
                if (result.Type == ERowHandleResultType::Error) {
                    SOY_LOG_INFO("ERowHandleResultType::Error");
                    return;
                }
                SOY_LOG_INFO("Check results");
                for (auto pair : result.OutputRows) {
                    SOY_LOG_INFO("New test result");
                    output->AddRow(pair.first, pair.second);
                }
                ++Statistics.Rows;
            }
        } else {
            SOY_LOG_INFO("Flag \"alice_evo_tests\" was not found");
        }

        StatisticsCollector.FlushStatistics(false);
        SOY_LOG_INFO("Successes: %v, total: %v", Statistics.Successes, Statistics.Total);
    }
}
