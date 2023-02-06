#pragma once

#include <mapreduce/yt/interface/client.h>  // NYT::*
#include <util/stream/fwd.h>   // TFileOutput

#include <library/cpp/logger/global/global.h>  // INFO_LOG

#include <cstddef>  // std::size_t

struct TParams {
    TString Cluster;
    TString TablePath;
    TString TokenPath;
    TString OutputPath;
};

template <typename TTable, typename TRowType>
void SaveTable(TParams params, std::function<TRowType*(TTable&)> addRow) {
    // 1. Create client
    NYT::IClientPtr client = NYT::CreateClient(params.Cluster, NYT::TCreateClientOptions().TokenPath(params.TokenPath));

    // 2. Check if table exists
    if (!client->Exists(params.TablePath)) {
        Cerr << "The table does not exist" << Endl;
        exit(1);
    }

    // 3. Read the YT table entries into protobuf table
    auto reader = client->CreateTableReader<TRowType>(params.TablePath);
    std::size_t entry_count = 0;

    TTable result;
    while (reader->IsValid()) {
        *addRow(result) = reader->GetRow();
        entry_count++;
        reader->Next();
    }
    INFO_LOG << "Read a total of " << entry_count << " entries" << Endl;

    // 4. Write the serialized table to file
    TFileOutput out(params.OutputPath);
    result.Save(&out); // yandex-specific extension (look at generated .pb.h files for more info)
    out.Finish();
}
