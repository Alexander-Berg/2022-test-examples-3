#include <search/begemot/server/proto/begemot.pb.h>
#include <search/idl/meta.pb.h>

#include <google/protobuf/arena.h>

#include <library/cpp/build_info/build_info.h>
#include <library/cpp/cgiparam/cgiparam.h>
#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/neh/rpc.h>
#include <library/cpp/streams/factory/factory.h>
#include <library/cpp/string_utils/base64/base64.h>
#include <library/cpp/svnversion/svnversion.h>
#include <library/cpp/threading/thread_local/thread_local.h>

#include <quality/personalization/big_rt/rapid_clicks_common/proto/states.pb.h>

using namespace NNeh;

namespace {

NProtoBuf::Arena CreateArena() {
    static NThreading::TThreadLocalValue<std::array<char, 1024*1024>> arenaInitialBlock;
    NProtoBuf::ArenaOptions opts;
    opts.initial_block = arenaInitialBlock.GetRef().data();
    opts.initial_block_size = arenaInitialBlock.GetRef().size();
    opts.start_block_size = 4096;
    opts.max_block_size = 128*1024;
    return NProtoBuf::Arena(opts);
}

struct TSingleRequest
{
    TString Request;
    TString Response;
};

class TSubsourceMocker;
struct TCalculatorSubSource
{
    void (TSubsourceMocker::*Processor)(const TCalculatorSubSource& data, IRequest* request) = nullptr;
    THashMap<TString, TSingleRequest> Requests;
};

class TSubsourceMocker : public IService
{
    IServices* Parent = nullptr;
    THashMap<TString, TString> FetchedDocData;
    THashMap<TString, TCalculatorSubSource> SubSources;
    THashMap<ui32, TCalculatorSubSource> DynTableRequests;

    TAtomic NumErrors = 0;

    void ProcessAdminRequest(IRequest* request) {
        TCgiParameters cgi(request->Data());
        TString action = cgi.Get("action");
        if (action == "shutdown") {
            Parent->Stop();
            TDataSaver reply;
            reply << "shutdown";
            request->SendReply(reply);
        } else if (action == "ping") {
            TDataSaver reply;
            reply << "pong";
            request->SendReply(reply);
        } else {
            AtomicIncrement(NumErrors);
            Cerr << "unknown admin request: " << request->Data() << '\n';
            request->SendError(IRequest::NotExistService);
        }
    }

    void ProcessFetchDocDataRequest(ui32 clientNum, IRequest* request);
    void ProcessRtModelsRequest(const TCalculatorSubSource& data, IRequest* request);
    void ProcessCfgModelsRequest(const TCalculatorSubSource& data, IRequest* request);
    void ProcessParsedBegemotRequest(const TCalculatorSubSource& data, IRequest* request, NProtoBuf::Arena& arena, const NBg::NProto::TBegemotRequest& req);
    void ProcessDynTableRequest(IRequest* request);

    void LoadFetchedDocData(const TString& fetchedDocDataFile);
    void LoadCalculatorResponses(const TString& requestsResponsesFile);

public:
    TSubsourceMocker(const TString& fetchedDocDataFile, const TString& requestsResponsesFile, IServices* parent)
        : Parent(parent)
    {
        SubSources["RTMODELS"].Processor = &TSubsourceMocker::ProcessRtModelsRequest;
        SubSources["RTMODELS_PERSONAL_CACHED"].Processor = &TSubsourceMocker::ProcessRtModelsRequest;
        SubSources["RTMODELS_FEATURES_PERSONAL_MODEL"].Processor = &TSubsourceMocker::ProcessRtModelsRequest;
        SubSources["CFG_MODELS"].Processor = &TSubsourceMocker::ProcessCfgModelsRequest;
        SubSources["CFG_MODELS_USERBODY"].Processor = &TSubsourceMocker::ProcessCfgModelsRequest;
        SubSources["CFG_MODELS_SPLIT"].Processor = &TSubsourceMocker::ProcessCfgModelsRequest;
        LoadFetchedDocData(fetchedDocDataFile);
        LoadCalculatorResponses(requestsResponsesFile);
    }

    void ServeRequest(const IRequestRef& request) override {
        try {
            TStringBuf service = request->Service();
            if (service == "admin") {
                ProcessAdminRequest(request.Get());
            } else if (service.SkipPrefix("FETCH_DOC_DATA/")) {
                ui32 clientNum = Max<ui32>();
                if (TryFromString(service, clientNum)) {
                    ProcessFetchDocDataRequest(clientNum, request.Get());
                } else {
                    AtomicIncrement(NumErrors);
                    Cerr << "invalid FETCH_DOC_DATA clientNum: " << service << '\n';
                    request->SendError(IRequest::NotExistService);
                }
            } else if (service == "DYNTABLE_HTTP_PROXY_SOURCE") {
                ProcessDynTableRequest(request.Get());
            } else if (const TCalculatorSubSource* calc = SubSources.FindPtr(service)) {
                (this->*(calc->Processor))(*calc, request.Get());
            } else {
                AtomicIncrement(NumErrors);
                Cerr << "unknown service: " << service << '\n';
                request->SendError(IRequest::NotExistService);
            }
        } catch (yexception& e) {
            AtomicIncrement(NumErrors);
            Cerr << "exception while processing request: " << e.what() << '\n';
            request->SendError(IRequest::InternalError, e.what());
        }
    }

    size_t GetNumErrors() const {
        return AtomicGet(NumErrors);
    }
};

void TSubsourceMocker::LoadFetchedDocData(const TString& fetchedDocDataFile)
{
    THolder<IInputStream> stream = OpenInput(fetchedDocDataFile);
    TString line;
    while (stream->ReadLine(line)) {
        TStringBuf v = line;
        TStringBuf docid = v.NextTok('\t');
        auto insertResult = FetchedDocData.emplace(TString(docid), Base64StrictDecode(v));
        Y_ENSURE(insertResult.second);
    }
}

void TSubsourceMocker::LoadCalculatorResponses(const TString& requestsResponsesFile)
{
    THolder<IInputStream> stream = OpenInput(requestsResponsesFile);
    TString line;
    while (stream->ReadLine(line)) {
        TStringBuf v = line;
        TStringBuf token;
        TString reqid;
        TVector<std::pair<TStringBuf, TStringBuf>> requests;
        TVector<std::pair<TStringBuf, TStringBuf>> responses;
        while (v.NextTok('\t', token)) {
            TStringBuf key, value;
            token.Split('=', key, value);
            if (key == "reqid") {
                Y_ENSURE(!reqid);
                Y_ENSURE(value);
                reqid = value;
            } else if (key.ChopSuffix(":request")) {
                requests.emplace_back(key, value);
            } else if (key.ChopSuffix(":response")) {
                responses.emplace_back(key, value);
            }
        }
        // maybe someday we will compare incoming requests with saved ones, but ignore them for now
        for (const auto& it : responses) {
            TCalculatorSubSource* s = nullptr;
            TStringBuf x = it.first;
            if (x.SkipPrefix("DYNTABLE_HTTP_PROXY_SOURCE:")) {
                s = &DynTableRequests[FromString<ui32>(x)];
            } else {
                s = SubSources.FindPtr(it.first);
            }
            Y_ENSURE(s, "unknown source: " << it.first);
            TSingleRequest r;
            r.Response = Base64StrictDecode(it.second);
            auto insertResult = s->Requests.emplace(reqid, std::move(r));
            if (!insertResult.second) Cerr << "duplicate reqid " << reqid << '\n';
            Y_ENSURE(insertResult.second);
        }
    }
}

void TSubsourceMocker::ProcessFetchDocDataRequest(ui32 clientNum, IRequest* request) {
    TCgiParameters cgi(request->Data());
    bool bertv1 = false, bertv2 = false;
    auto prons = cgi.equal_range("pron");
    for (auto it = prons.first; it != prons.second; ++it) {
        if (it->second == "fetch_doc_data_opts=bert_embedding")
            bertv1 = true;
        else if (it->second == "fetch_doc_data_opts=bert_embedding_v2")
            bertv2 = true;
    }
    if (bertv1 && bertv2) {
        AtomicIncrement(NumErrors);
        Cerr << "FetchDocData: two bert embeddings requested\n";
        request->SendError(IRequest::BadRequest);
        return;
    }
    NProtoBuf::Arena arena = CreateArena();
    NMetaProtocol::TReport& result = *NProtoBuf::Arena::CreateMessage<NMetaProtocol::TReport>(&arena);
    result.MutableHead()->SetVersion(1);
    // ignore BalancingInfo and SearcherProp
    result.MutableErrorInfo()->SetGotError(NMetaProtocol::TErrorInfo::NO);
    NMetaProtocol::TGrouping& grouping = *result.AddGrouping();
    grouping.SetIsFlat(NMetaProtocol::TGrouping::YES);
    auto requestedDocs = cgi.equal_range("dh");
    TString clientPrefix = ToString(clientNum) + "-";
    for (auto it = requestedDocs.first; it != requestedDocs.second; ++it) {
        TString docId = clientPrefix + it->second;
        if (bertv1)
            docId += ":v1";
        TString* data = FetchedDocData.FindPtr(docId);
        if (!data && !bertv1 && !bertv2) {
            // light fetch request for ytier frame; retry with :v1, light result is the same for v1/v2
            docId += ":v1";
            data = FetchedDocData.FindPtr(docId);
        }
        if (!data) {
            AtomicIncrement(NumErrors);
            Cerr << "FetchDocData: no data for docid " << docId << '\n';
            continue;
        }
        NMetaProtocol::TDocument& doc = *grouping.AddGroup()->AddDocument();
        Y_ENSURE(doc.ParseFromString(*data));
        if (!bertv1 && !bertv2) {
            // rtmodels requests have no meta_descr and no BinaryData
            doc.ClearBinaryData();
            auto& gtas = *doc.MutableArchiveInfo()->MutableGtaRelatedAttribute();
            for (int i = 0; i < gtas.size(); ) {
                if (gtas[i].GetKey() == "meta_descr") {
                    if (i != gtas.size() - 1)
                        gtas.SwapElements(i, gtas.size() - 1);
                    gtas.RemoveLast();
                } else {
                    ++i;
                }
            }
        }
    }
    TDataSaver reply;
    reply << result.SerializeAsString();
    request->SendReply(reply);
}

static void FilterWebResponseDocs(NBg::NProto::TWebResponse& target, const NBg::NProto::TWebResponse& source, const THashSet<std::pair<ui64, ui64>>& requestedDocs) {
    Y_ENSURE(!source.DataSize()); // deprecated
    Y_ENSURE(!source.HasDataDescription()); // deprecated
    for (const NBg::NProto::TWebResponse::TSlicedCalculatedData& doc : source.GetSlicedCalculatedData()) {
        if (requestedDocs.contains(std::make_pair(doc.GetDocHandle().GetHash(), doc.GetDocHandle().GetRoute()))) {
            *target.AddSlicedCalculatedData() = doc;
        }
    }
    *target.MutableSlicedDataDescription() = source.GetSlicedDataDescription();
    Y_ENSURE(!source.HasItdItpCompressedRequestEmbedding());
    target.MutableAppliedModels()->CopyFrom(source.GetAppliedModels());
    if (source.HasNumDocuments())
        target.SetNumDocuments(source.GetNumDocuments());
    if (source.HasFailedDocuments())
        target.SetFailedDocuments(source.GetFailedDocuments());
    if (source.HasSkippedDocuments())
        target.SetSkippedDocuments(source.GetSkippedDocuments());
    if (source.HasCachedDocuments())
        target.SetCachedDocuments(source.GetCachedDocuments());
}

void TSubsourceMocker::ProcessParsedBegemotRequest(const TCalculatorSubSource& data, IRequest* request, NProtoBuf::Arena& arena, const NBg::NProto::TBegemotRequest& req) {
    // TODO: support split models
    const TSingleRequest* r = data.Requests.FindPtr(req.GetWeb().GetReqId());
    if (!r) {
        AtomicIncrement(NumErrors);
        Cerr << "RtModelsRequest: no data for reqid " << req.GetWeb().GetReqId() << '\n';
        request->SendError(IRequest::BadRequest);
        return;
    }
    NBg::NProto::TBegemotResponse& fullResponse = *NProtoBuf::Arena::CreateMessage<NBg::NProto::TBegemotResponse>(&arena);
    Y_ENSURE(fullResponse.ParseFromString(r->Response));
    NBg::NProto::TBegemotResponse& result = *NProtoBuf::Arena::CreateMessage<NBg::NProto::TBegemotResponse>(&arena);
    THashSet<std::pair<ui64, ui64>> requestedDocs;
    for (const auto& doc : req.GetWeb().GetFetchedDocData().GetDocData()) {
        auto insertResult = requestedDocs.emplace(doc.GetDocHandle().GetHash(), doc.GetDocHandle().GetRoute());
        Y_ENSURE(insertResult.second);
    }
    if (fullResponse.HasWeb())
        FilterWebResponseDocs(*result.MutableWeb(), fullResponse.GetWeb(), requestedDocs);
    if (fullResponse.HasRealTimeTraining())
        FilterWebResponseDocs(*result.MutableRealTimeTraining(), fullResponse.GetRealTimeTraining(), requestedDocs);
    if (fullResponse.HasRealTimeTrainingWeb())
        FilterWebResponseDocs(*result.MutableRealTimeTrainingWeb(), fullResponse.GetRealTimeTrainingWeb(), requestedDocs);
    if (fullResponse.HasDocFPMResponse())
        FilterWebResponseDocs(*result.MutableDocFPMResponse(), fullResponse.GetDocFPMResponse(), requestedDocs);
    if (fullResponse.HasRapidClicksResponse()) {
        FilterWebResponseDocs(*result.MutableRapidClicksResponse(), fullResponse.GetRapidClicksResponse(), requestedDocs);
        // for split requests, also split DocsWithRapidClicksFactorsCount/DocsWithRapidPersClicksFactorsCount
        // we don't have enough information for the precise calculations, so just do something,
        // assign half of count to the response containing the first document and another half to the second document
        ui32 c1 = 0, c2 = 0;
        const auto& r = fullResponse.GetRapidClicksResponse();
        if (r.SlicedCalculatedDataSize() == 0 || requestedDocs.contains(
            std::make_pair(r.GetSlicedCalculatedData(0).GetDocHandle().GetHash(), r.GetSlicedCalculatedData(0).GetDocHandle().GetRoute())))
        {
            c1 += r.GetDocsWithRapidClicksFactorsCount() / 2;
            c2 += r.GetDocsWithRapidPersClicksFactorsCount() / 2;
        }
        if (r.SlicedCalculatedDataSize() <= 1 || requestedDocs.contains(
            std::make_pair(r.GetSlicedCalculatedData(1).GetDocHandle().GetHash(), r.GetSlicedCalculatedData(1).GetDocHandle().GetRoute())))
        {
            c1 += (r.GetDocsWithRapidClicksFactorsCount() + 1) / 2;
            c2 += (r.GetDocsWithRapidPersClicksFactorsCount() + 1) / 2;
        }
        result.MutableRapidClicksResponse()->SetDocsWithRapidClicksFactorsCount(c1);
        result.MutableRapidClicksResponse()->SetDocsWithRapidPersClicksFactorsCount(c2);
    }
    result.SetSuperMindMultiplier(fullResponse.GetSuperMindMultiplier());
    Y_ENSURE(!fullResponse.HasYabsHitModelsResponse());
    Y_ENSURE(!fullResponse.HasBert());
    Y_ENSURE(!fullResponse.SubSourceStatsSize());
    TDataSaver reply;
    reply << result.SerializeAsString();
    request->SendReply(reply);
}

void TSubsourceMocker::ProcessRtModelsRequest(const TCalculatorSubSource& data, IRequest* request) {
    NProtoBuf::Arena arena = CreateArena();
    NBg::NProto::TBegemotRequest& req = *NProtoBuf::Arena::CreateMessage<NBg::NProto::TBegemotRequest>(&arena);
    TStringBuf requestData = request->Data();
    Y_ENSURE(req.ParseFromArray(requestData.data(), requestData.size()));
    ProcessParsedBegemotRequest(data, request, arena, req);
}

void TSubsourceMocker::ProcessCfgModelsRequest(const TCalculatorSubSource& data, IRequest* request) {
    NProtoBuf::Arena arena = CreateArena();
    NBg::NProto::TBegemotRequest& req = *NProtoBuf::Arena::CreateMessage<NBg::NProto::TBegemotRequest>(&arena);
    TStringBuf requestData = request->Data();
    Y_ENSURE(req.ParseFromArray(requestData.data(), requestData.size()));
    if (req.GetWeb().GetQueryTimestamp()) {
        ProcessParsedBegemotRequest(SubSources.at("CFG_MODELS_USERBODY"), request, arena, req);
    } else if (req.GetWeb().HasSplitBertQueryEmbeddingStableHash()) {
        ProcessParsedBegemotRequest(SubSources.at("CFG_MODELS_SPLIT"), request, arena, req);
    } else {
        ProcessParsedBegemotRequest(data, request, arena, req);
    }
}

void TSubsourceMocker::ProcessDynTableRequest(IRequest* request) {
    NProtoBuf::Arena arena = CreateArena();
    NDynTableHttpProxy::TUserDataRequest& req = *NProtoBuf::Arena::CreateMessage<NDynTableHttpProxy::TUserDataRequest>(&arena);
    TStringBuf requestData = request->Data();
    Y_ENSURE(req.ParseFromArray(requestData.data(), requestData.size()));
    if (!req.KeysSize()) {
        AtomicIncrement(NumErrors);
        Cerr << "DynTableRequest: no keys in request " << req.GetReqID() << '\n';
        request->SendError(IRequest::BadRequest);
        return;
    }
    NRapidClicks::TKeys keys;
    Y_ENSURE(keys.ParseFromString(req.GetKeys(0)));
    if (keys.KeysSize() != 1) {
        AtomicIncrement(NumErrors);
        Cerr << "DynTableRequest: invalid key in request " << req.GetReqID() << '\n';
        request->SendError(IRequest::BadRequest);
        return;
    }
    ui32 keyType = keys.GetKeys(0).GetKeyType();
    for (size_t i = 1; i < req.KeysSize(); i++) {
        Y_ENSURE(keys.ParseFromString(req.GetKeys(i)));
        if (keys.KeysSize() != 1) {
            AtomicIncrement(NumErrors);
            Cerr << "DynTableRequest: invalid key in request " << req.GetReqID() << '\n';
            request->SendError(IRequest::BadRequest);
            return;
        }
        if (static_cast<ui32>(keys.GetKeys(0).GetKeyType()) != keyType) {
            AtomicIncrement(NumErrors);
            Cerr << "DynTableRequest: mismatched keytype in request " << req.GetReqID() << '\n';
            request->SendError(IRequest::BadRequest);
            return;
        }
    }
    TCalculatorSubSource* subsrc = DynTableRequests.FindPtr(keyType);
    TSingleRequest* r = subsrc ? subsrc->Requests.FindPtr(req.GetReqID()) : nullptr;
    if (!r) {
        AtomicIncrement(NumErrors);
        Cerr << "DynTableRequest: no data for reqid " << req.GetReqID() << ", keytype " << keyType << '\n';
        request->SendError(IRequest::BadRequest);
        return;
    }
    TDataSaver reply;
    reply << r->Response;
    request->SendReply(reply);
}

}

int main(int argc, const char* argv[])
{
    NLastGetopt::TOpts opts;
    ui16 port = 0;
    unsigned threads = 1;
    TString fetchedDocDataFile, requestsResponsesFile;
    opts.AddLongOption('p', "port", "port number").StoreResult(&port).Required().RequiredArgument("<INTEGER>");
    opts.AddLongOption('t', "threads", "number of threads").StoreResult(&threads).DefaultValue("1").RequiredArgument("<INTEGER>");
    opts.AddLongOption('f', "fetched-doc-data", "responses for FetchDocData requests").StoreResult(&fetchedDocDataFile).Required().RequiredArgument("<FILENAME>.tsv[.gz]");
    opts.AddLongOption('a', "aux-sources-data", "requests/responses for calculator requests").StoreResult(&requestsResponsesFile).Required().RequiredArgument("<FILENAME>.tskv[.gz]");
    opts.AddLongOption('v', "version", "print version and exit").NoArgument().Handler0([]{
        Cout << GetProgramSvnVersion() << Endl;
        Cout << GetBuildInfo() << Endl;
        exit(0);
    });
    opts.SetFreeArgsNum(0);

    NLastGetopt::TOptsParseResult cmdLine(&opts, argc, argv);

    IServicesRef server = CreateLoop();
    IServiceRef worker = new TSubsourceMocker(fetchedDocDataFile, requestsResponsesFile, server.Get());
    server->Add("http://localhost:" + ToString(port) + "/*", worker);
    server->Loop(threads);

    size_t numErrors = static_cast<TSubsourceMocker*>(worker.Get())->GetNumErrors();
    Cerr << "error count: " << numErrors;

    return numErrors ? 1 : 0;
}
