#include <extsearch/images/base/cbir/test_data/protos/test_cbir_data.pb.h>
#include <extsearch/images/robot/library/identifier/document.h>
#include <extsearch/images/robot/library/identifier/indexdocument.h>
#include <mapreduce/yt/interface/client.h>
#include <kernel/searchlog/errorlog.h>
#include <library/cpp/getopt/last_getopt.h>

using NImages::NIndex::TDocumentId;
using NImages::NIndex::TIndexDocumentId;
using namespace NYT;

int main(int argc, const char* argv[]) {
    NYT::Initialize(argc, argv);

    TString ytProxy;
    TString ytToken;
    TString table;
    TString result;
    size_t shardCount = 0;

    using namespace NLastGetopt;
    TOpts opts;
    opts.AddOption(TOpt().AddLongName("yt_proxy").StoreResult(&ytProxy).Required());
    opts.AddOption(TOpt().AddLongName("token").StoreResult(&ytToken).Required());
    opts.AddOption(TOpt().AddLongName("shard_count").StoreResult(&shardCount));

    opts.AddOption(TOpt().AddLongName("table").StoreResult(&table).Required());
    opts.AddOption(TOpt().AddLongName("result").StoreResult(&result).Required());

    TOptsParseResult optsParseResult(&opts, argc, argv);

    auto client = NYT::CreateClient(ytProxy, NYT::TCreateClientOptions().Token(ytToken));
    SEARCH_INFO << "Starting" << Endl;

    auto reader = client->CreateTableReader<TNode>(table);

    TVector<NCbirTestData::TTestDocData> shards;
    shards.resize(shardCount);

    for (; reader->IsValid(); reader->Next()) {
        const TNode& row = reader->GetRow();
        TDocumentId documentId = TDocumentId::FromRaw(row["doc_id"].AsString());
        TIndexDocumentId localDocId = TIndexDocumentId::FromRaw(row["local_doc_id"].AsString());
        ui32 shard = localDocId.GetShardId().AsNumber();
        Y_ENSURE(documentId.GetLanguageId().AsNumber() == 0u);
        Y_ENSURE(shard < shardCount);
        shards[shard].AddSchemaOrgGroupId(documentId.GetGroupId().AsNumber());
    }
    for (size_t i = 0; i < shardCount; ++i) {
        TOFStream fOut(result + ToString(i) + "pb.bin");
        TString protoString;
        Y_ENSURE(shards[i].SerializeToString(&protoString));
        fOut.Write(protoString);
    }
    SEARCH_INFO << "Done" << Endl;
}

/*
 *
 * import json, subprocess
 * res = {}
 * for i in range(0, 432):
 *   res[i] = json.loads(subprocess.check_output("ya upload --json-output --ttl 7 ./data/comm_data_" + str(i) + "pb.bin", shell=True))["download_link"]
 * json.dump(res, open("mapping.json", "w"), indent=1)
 *
 *
 *
 */
