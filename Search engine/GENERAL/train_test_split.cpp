#include "train_test_split.h"
#include "common.h"

#include <quality/itditp/lib/utils/utils.h>

#include <mapreduce/yt/interface/client.h>

namespace NItdItp {

TTrainTestSplitOptions::TTrainTestSplitOptions(int argc, const char** argv) {
    NLastGetopt::TOpts opts;
    opts.AddLongOption("pool-input").Required().RequiredArgument("<ypath>").StoreResult(&PoolInput);
    opts.AddLongOption("train-output").Required().RequiredArgument("<ypath>").StoreResult(&TrainPoolOutput);
    opts.AddLongOption("test-output").Required().RequiredArgument("<ypath>").StoreResult(&TestPoolOutput);
    opts.AddLongOption("test-size").Optional().RequiredArgument("<float>").DefaultValue(0.01).StoreResult(&TestSizePercent);
    opts.AddLongOption("random-feature").Optional().RequiredArgument("<column name>").DefaultValue("Random").StoreResult(&RandomFeature);
    opts.AddLongOption("cluster").Optional().RequiredArgument("<ypath>").DefaultValue("arnold").StoreResult(&YtCluster);

    NLastGetopt::TOptsParseResult parseResult(&opts, argc, argv);
}

int TrainTestSplit(int argc, const char* argv[]) {
    const TTrainTestSplitOptions options(argc, argv);
    Cerr << "Creating client for " << options.YtCluster << Endl;
    NYT::IClientPtr client = NYT::CreateClient(options.YtCluster);

    Y_ENSURE(client->Exists(options.PoolInput));

    if (NItdItp::TableExistsAndSortedBy(client, options.PoolInput, { options.RandomFeature })) {
        Cerr << "Skip sort, table '" << options.PoolInput << "' is sorted" << Endl;
    } else {
        Cerr << "Start Sort '" << options.PoolInput << "'" << Endl;
        NYT::TSortOperationSpec spec;
        spec
            .AddInput(options.PoolInput)
            .SortBy({options.RandomFeature})
            .Output(options.PoolInput);
        client->Sort(spec);

        Cerr << "Sort success" << Endl;
    }
    NYT::ITransactionPtr tx = client->StartTransaction();

    auto mergeRangeAsync = [&tx, &options] (const TString& inputTable, const TString& outputTable, size_t lowerLimit, size_t upperLimit) -> NYT::IOperationPtr {
        NYT::TRichYPath tablePathRange = NYT::TRichYPath(inputTable)
            .AddRange(NYT::TReadRange::FromRowIndices(lowerLimit, upperLimit));
        Cerr << "Merge rows " << lowerLimit << ".." << upperLimit << " into '" << outputTable << Endl;
        NYT::TRichYPath outputPath = NYT::TRichYPath(outputTable)
            .SortedBy({options.RandomFeature});

        NYT::IOperationPtr mergeOp = tx->Merge(
            NYT::TMergeOperationSpec()
                .AddInput(tablePathRange)
                .Output(outputTable)
                .Mode(NYT::EMergeMode::MM_SORTED),
            NYT::TOperationOptions().Wait(false)
        );
        return mergeOp;
    };

    size_t inputSize = NCollaborativeDssm::RowCount(client, options.PoolInput);
    Y_ENSURE(inputSize);
    size_t testSize = ClampVal<size_t>(inputSize * options.TestSizePercent, 1, inputSize - 1);
    size_t trainSize = inputSize - testSize;

    NYT::IOperationPtr trainMerge = mergeRangeAsync(options.PoolInput, options.TrainPoolOutput, 0, trainSize);
    NYT::IOperationPtr testMerge = mergeRangeAsync(options.PoolInput, options.TestPoolOutput, trainSize, inputSize);

    NThreading::TFuture<void> trainMergeFeature = trainMerge->Watch();
    NThreading::TFuture<void> testMergeFeature = testMerge->Watch();

    Cerr << "Wait" << Endl;
    trainMergeFeature.Wait();
    testMergeFeature.Wait();
    Cerr << "Merge Success" << Endl;

    tx->Commit();
    Cerr << "Commit transaction" << Endl;
    return 0;
}

} // namespace NItdItp
