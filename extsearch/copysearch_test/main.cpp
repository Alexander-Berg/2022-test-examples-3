#include <extsearch/images/robot/library/logger/linklogger.h>
#include <extsearch/images/robot/library/thdb/thdb.h>

#include <extsearch/images/kernel/cbir/geomindexlib/structs.h>
#include <extsearch/images/kernel/cbir/geomindexlib/misc.h>

#include <cv/imageproc/copysearcher/icopysearch.h>
#include <extsearch/images/kernel/cbir/localdescriptor_chunk/localdescriptors_chunk.h>

#include <library/cpp/getopt/last_getopt.h>

#include <util/datetime/cputimer.h>
#include <util/folder/path.h>
#include <util/generic/algorithm.h>
#include <util/generic/map.h>
#include <util/generic/set.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/stream/file.h>
#include <util/string/split.h>
#include <util/string/printf.h>
#include <util/system/byteorder.h>
#include <util/system/file.h>
#include <util/memory/blob.h>

#include <stdio.h>

using namespace NLastGetopt;
using namespace NImages;

using TBase64Structure = TVector<TString>;
using TValidationStructure = TVector<TBlob>;
using TThIds = TVector<int>;

void Quantization(ICopySearcher* searcher, const TBase64Structure& qdescr, TValidationStructure& records) {
    for (size_t i = 0; i < qdescr.size(); ++i) {
        TDescrBlob base64Descr((const char*)qdescr[i].data(), qdescr[i].size());
        searcher->QuantizeDescriptor(base64Descr);
    }
    records.reserve(qdescr.size());
    for (size_t i = 0; i < qdescr.size(); ++i) {
        TDescrBlob descr = searcher->GetQuantizedDescriptor(i);
        records.push_back(TBlob::NoCopy(descr.Descr, descr.Size));
    }
}

void AddRecordToSearcher(ICopySearcher* searcher, const TValidationStructure& records, const TThIds& thIds) {
    searcher->ReserveMemory();
    for (size_t i = 0; i < records.size(); ++i) {
        searcher->AddRecord(thIds[i], records[i].Data());
    }
}

void SearchCopies(ICopySearcher* searcher, const TBlob& record, int thid, TString& output) {
    TValidationResults result;
    searcher->Process(record.Data(), result);
    output = Sprintf("%i\t", thid);
    for (size_t i = 0; i < result.size(); ++i) {
        const ui32 val = result[i].MatchValue;
        const ui32 docId = result[i].DocId;
        output += Sprintf("%u,%u\t", val, docId);
    }
}

int main(int argc, const char* argv[]) {
    TString thdbq;
    ui32 maxRecordsToLoad = 0;
    ui32 maxRecordsToProcess = 0;

    TOpts opts;
    opts.AddLongOption("thdataq", "input thdb file/table")
        .Required()
        .RequiredArgument("PATH")
        .StoreResult(&thdbq);
    opts.AddLongOption("num", "maximum records to load")
        .Optional()
        .RequiredArgument("NUM")
        .DefaultValue("10")
        .StoreResult(&maxRecordsToLoad);
    opts.AddLongOption("num-proc", "maximum records to process")
        .Optional()
        .RequiredArgument("NUM")
        .DefaultValue("10")
        .StoreResult(&maxRecordsToProcess);
    opts.AddHelpOption('?');
    TOptsParseResult optParseResult(&opts, argc, argv);

    TLinkLogger::LogStartMessage("CopySearch test v.1.0");

    NThDb::TReaderPtr thdbreader = NThDb::Open(thdbq);

    TValidationOptions opt;
    opt.Records = maxRecordsToLoad;
    opt.MatchPercentage = 10;

    ++maxRecordsToLoad;

    TBase64Structure qdescr;
    TThIds thIds;
    NThDb::TRec rec;
    while (thdbreader->Read(rec)) {
        const int thid = rec.GetDocId();
        if (!rec.HasChunk<TQLDSChunk>())
            continue;
        const auto& chunk = rec.GetChunk<TQLDSChunk>();
        if (qdescr.size() < maxRecordsToLoad) {
            qdescr.push_back(ToBase64(chunk));
            thIds.push_back(thid);
            continue;
        }
    }

    ICopySearcher* searcher = CreateSearcher();
    searcher->SetOptions(opt);
    TValidationStructure records;
    // QuantizeDescriptor
    ui64 start = ::MicroSeconds();
    Quantization(searcher, qdescr, records);
    ui64 finish = ::MicroSeconds();
    Cout << "QuantizeDescriptor time: " << finish - start << "\n";

    // Batch mode
    start = ::MicroSeconds();
    AddRecordToSearcher(searcher, records, thIds);
    finish = ::MicroSeconds();
    Cout << "AddRecord time: " << finish - start << "\n";

    const ui32 itemsToProcess = Min<ui32>(maxRecordsToProcess, records.size());
    for (size_t j = 0; j < itemsToProcess; ++j) {
        TString result;
        SearchCopies(searcher, records[j], thIds[j], result);
        Cout << result << "\n";
    }
    finish = ::MicroSeconds();
    Cout << "Batch mode time: " << finish - start << "\n";
    searcher->Release();

    TLinkLogger::LogEndMessage("CopySearch test v.1.0");

    return 0;
}
