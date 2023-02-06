#include <extsearch/images/robot/library/logger/linklogger.h>
#include <extsearch/images/robot/library/thdb/thdb.h>

#include <extsearch/images/kernel/cbir/geomindexlib/geomindex.h>
#include <extsearch/images/kernel/cbir/geomindexlib/misc.h>

#include <mapreduce/lib/all.h>

#include <library/cpp/on_disk/2d_array/array2d.h>
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
#include <util/system/byteorder.h>
#include <util/system/file.h>

#include <stdio.h>

using namespace NLastGetopt;
using namespace NImages;
using namespace NMR;
using namespace NGeomSearcher;

bool ProcessRequestChunk(const TQLDSChunk& chunk, TGeomIndexData& geomIndex, TString& result, int docID, int key) {
    if (chunk.ChunkHeader.ItemsCount < MIN_DESCRIPTORS_COUNT)
        return false;
    TGeomSearcherSettings settings;
    TGeomSearchResults neighbours;
    auto filterF = [](ui32)-> bool {
        return true;
    };
    geomIndex.Process(chunk, settings, filterF, neighbours);
    if (key == -1) {
        result = Sprintf("%i\t", docID);
    } else {
        result = Sprintf("%i (%i)\t", docID, key);
    }
    for (size_t i = 0; i < neighbours.size(); ++i) {
        const float val = neighbours[i].Dist;
        const ui32 docId = neighbours[i].DocId;
        result += Sprintf("%.0f,%u\t", val, docId);
    }
    result += "\n";
    return true;
}

int main(int argc, const char* argv[]) {
    Initialize(argc, argv);
    TString db, indexDir, thid2doc, thdbq;
    TString prefix = "index";
    int maxRecordsToLoad = 0;
    TString serverName;

    TOpts opts;
    opts.AddLongOption("thid2yid", "file-converter")
        .Optional()
        .RequiredArgument("PATH")
        .StoreResult(&thid2doc);
    opts.AddLongOption("thdataq", "input thdb file/table")
        .Required()
        .RequiredArgument("PATH")
        .StoreResult(&thdbq);
    opts.AddLongOption("num", "maximum records to load")
        .Optional()
        .RequiredArgument("NUM")
        .DefaultValue("0")
        .StoreResult(&maxRecordsToLoad);
    opts.AddLongOption("index", "index directory")
        .Required()
        .RequiredArgument("DIR")
        .StoreResult(&indexDir);
    opts.AddLongOption("prefix", "prefix of input index files")
        .Optional()
        .RequiredArgument("NAME")
        .StoreResult(&prefix);
    opts.AddLongOption("server", "MapReduce server")
        .Optional()
        .RequiredArgument("HOST:PORT")
        .StoreResult(&serverName);
    opts.AddHelpOption('?');
    TOptsParseResult optParseResult(&opts, argc, argv);
    bool mrMode = optParseResult.Has("server");

    TLinkLogger::LogStartMessage("CBIR search test v.2.0");

    TVector<int> thid2docVec;
    if (!thid2doc.empty()) {
        TIFStream ifsThid2Doc(thid2doc);
        Load(&ifsThid2Doc, thid2docVec);
    }

    TString indexPrefix = indexDir + "/" + prefix;
    TGeomIndexDataOptions options(indexPrefix);
    TGeomIndexData geomIndex(options);

    if (!mrMode) {
        NThDb::TReaderPtr thdbreader = NThDb::Open(thdbq);

        int chunks = 0;
        int prevDocID = -1;
        NThDb::TRec rec;
        while (thdbreader->Read(rec)) {
            int thid = rec.GetDocId();
            int docID = thid;
            if (!thid2docVec.empty())
                docID = thid2docVec[thid]; //picid;
            if (docID < 0 || docID == prevDocID)
                continue;
            prevDocID = docID;

            TString result;
            if (!rec.HasChunk<TQLDSChunk>())
                continue;
            const auto& chunk = rec.GetChunk<TQLDSChunk>();
            if (!ProcessRequestChunk(chunk, geomIndex, result, docID, -1))
                continue;
            Cout << result;
            ++chunks;
            if ((maxRecordsToLoad > 0) && (chunks >= maxRecordsToLoad))
                break;
        }
    } else {
        TServer server(serverName);
        TClient client(server);
        TTable thdb(client, thdbq.data());
        int docID = 0;
        int chunks = 0;
        for (TTableIterator it = thdb.Begin(); it.IsValid(); ++it) {
            ++docID;
            if (!it.GetValue().GetSize())
                continue;
            const auto rec = NThDb::ParseChunks(it.GetValue().GetData(), it.GetValue().GetSize());
            TString result;
            const ui8* key = reinterpret_cast<const ui8*>(it.GetKey().GetData());

            if (!NThDb::HasChunk(rec, TQLDSChunk::CHUNK_ID))
                continue;
            const auto& chunk = NThDb::GetChunk<TQLDSChunk>(rec);

            if (!ProcessRequestChunk(chunk, geomIndex, result, docID, *key))
                continue;
            Cout << result;
            ++chunks;
            if ((maxRecordsToLoad > 0) && (chunks >= maxRecordsToLoad))
                break;
        }
    }

    TLinkLogger::LogEndMessage("CBIR search test v.2.0");

    return 0;
}
