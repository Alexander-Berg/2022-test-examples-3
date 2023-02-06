#include "attribute_queries.h"
#include "generate_requests.h"
#include "options.h"
#include "save_requests.h"

#include <search/tools/test_shard/common/attribute_tree.h>
#include <search/tools/test_shard/common/shard_info.h>
#include <search/tools/test_shard/proto/query.pb.h>
#include <search/tools/test_shard/proto/config.pb.h>

#include <util/folder/path.h>
#include <util/random/shuffle.h>
#include <util/system/fs.h>
#include <util/system/tempfile.h>
#include <library/cpp/deprecated/split/split_iterator.h>
#include <library/cpp/cgiparam/cgiparam.h>
#include <util/string/vector.h>

#include <google/protobuf/text_format.h>

namespace NTestShard {

TRequestsBuilder::TRequestsBuilder(const TOptions& opts)
    : RequestsCount_(opts.RequestsCount)
    , Builder_(opts)
{
}

void TRequestsBuilder::AddType(const TWeightedType& type) {
    i64 count = static_cast<i64>(type.Weight * RequestsCount_);
    if (count > 0) {
        Types_.push_back({type.Attributes, static_cast<ui32>(count)});
    }
}

void TRequestsBuilder::ClearTypes() {
    Types_.clear();
}

NProto::TQueryVector TRequestsBuilder::Generate() {
    TQueryVector result;
    THashSet<TStringBuf> types;
    for (const auto& type : Types_) {
        type.Attributes.TraverseLeaves([&types](const TString& attr) {
            types.insert(attr);
        });
    }
    INFO_LOG << "Building inverse index" << Endl;
    Builder_.PrepareCache(types);
    TSafeQueryVector output;
    output.Output = &result;
    for (const auto& type : Types_) {
        Builder_.Build(type.Count, type.Attributes, output);
    }
    Builder_.Join();
    INFO_LOG << "Total queries generated : " << result.QuerySize() << Endl;
    return result;
}

TRequestsGenerator::TRequestsGenerator(TOptions& opts)
    : Builder_(opts)
    , Fetcher_(opts)
{
    Y_ENSURE(opts.Shards.size() == 1, "Specify exactly one shard with --shard option");
}

NProto::TQueryVector TRequestsGenerator::Generate(const TOptions& opts) {
    float sum = 0;
    for (const auto& type : opts.Config.GetType()) {
        sum += type.GetWeight();
    }
    Y_ENSURE(sum > 0.f);
    for (const auto& type : opts.Config.GetType()) {
        TRequestsBuilder::TWeightedType wtype;
        wtype.Attributes = type.GetNode();
        wtype.Weight = type.GetWeight() / sum;
        Builder_.AddType(wtype);
    }
    TQueryVector queries = Builder_.Generate();
    Fetcher_.FillRequests(queries, opts);
    return queries;
}

TQueryVector GenerateRequests(TOptions& opts) {
    TRequestsGenerator generator(opts);
    return generator.Generate(opts);
}

TQueryVector MutateQueries(const TQueryVector& queries) {
    return queries;
}

void PrintSerialized(const TQueryVector& queries, IOutputStream& out) {
    queries.SerializeToArcadiaStream(&out);
}

int PrintRequests(TOptions& opts) {
    THolder<TFileOutput> fout;
    bool toStdout = (opts.OutputDir == ".");
    if (!toStdout) {
        fout = MakeHolder<TFileOutput>(opts.OutputDir);
    }
    IOutputStream& out = (toStdout ? Cout : *fout);

    TQueryVector queries = GenerateRequests(opts);
    PrintSerialized(queries, out);
    return EXIT_SUCCESS;
}

}
