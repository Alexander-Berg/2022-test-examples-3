#include <extsearch/images/kernel/adult/common/classification_scorer.h>

#include <library/cpp/vowpalwabbit/vowpal_wabbit_model.h>
#include <library/cpp/vowpalwabbit/vowpal_wabbit_predictor.h>

#include <extsearch/images/robot/index/library/strutils/strutils.h>
#include <extsearch/images/robot/index/metadoc/lib/pornclassifiers/pornprobability2.h>
#include <extsearch/images/robot/index/metadoc/lib/pornclassifiers/pornprobabilityvw.h>

#include <library/cpp/getopt/small/last_getopt_opts.h>
#include <library/cpp/getopt/small/last_getopt_parser.h>
#include <library/cpp/getopt/small/last_getopt_parse_result.h>

#include <kernel/ethos/lib/metrics/binary_classification_metrics.h>

#include <util/stream/file.h>
#include <util/string/split.h>

int VowpalWabbitTestPackedModel(int argc, const char **argv) {
    auto opts = NLastGetopt::TOpts::Default();

    TString modelPath;
    opts.AddLongOption('m', "model", "Vowpal Wabbit model in converted binary")
            .Required()
            .RequiredArgument("MODEL")
            .StoreResult(&modelPath);

    TString dataPath;
    opts.AddLongOption('d', "data", "Path to dataset in Vowpal Wabbit fromat")
            .Required()
            .RequiredArgument("DATA")
            .StoreResult(&dataPath);

    TString outputPath;
    opts.AddLongOption('o', "out", "Optional file with VW predictions")
            .Optional()
            .RequiredArgument("OUT")
            .StoreResult(&outputPath);

    bool disableBias = false;
    opts.AddLongOption("disable-bias", "Do not use bias in resulting score")
            .Optional()
            .NoArgument()
            .StoreValue(&disableBias, true);

    NLastGetopt::TOptsParseResult{&opts, argc, argv};
    Y_VERIFY(modelPath);
    Y_VERIFY(dataPath);

    NVowpalWabbit::TPackedModel model(modelPath);
    NVowpalWabbit::TPredictor<NVowpalWabbit::TPackedModel> predictor(model);

    printf("Multiplier = %f\n", model.GetMultiplier());
    printf("Bias = %f\n", predictor.GetConstPrediction());
    if (disableBias) {
        printf("Note: bias will be ignored!\n");
    }

    TFileInput fileReader(dataPath);
    printf("Testing examples from %s...\n", dataPath.c_str());
    TClassificationScorer scorer;
    NEthos::TBcMetricsCalculator bcMetricsCalculator;
    TString line;
    TMaybe<TFixedBufferFileOutput> out;
    if (outputPath) {
        out.ConstructInPlace(outputPath);
    }

    while (fileReader.ReadLine(line)) {
        if (line.Empty()) {
            Y_VERIFY(fileReader.ReadLine(line) == 0);
            break;
        }

        try {
            TVector<TStringBuf> parts;
            StringSplitter(line).Split('|').AddTo(&parts);

            TVector<TStringBuf> header;
            StringSplitter(parts[0]).Split(' ').AddTo(&header);
            TStringBuf url = header[2];
            TVector<ui32> hashesAll(disableBias ? 0 : 1, NVowpalWabbit::VW_CONST_HASH);

            for (ui32 i = 1; i < parts.size(); ++i) {
                TVector<TStringBuf> words;
                StringSplitter(parts[i]).Split(' ').AddTo(&words);
                TStringBuf ns = words[0];
                words.erase(words.begin());
                TVector<ui32> hashes;
                NVowpalWabbit::THashCalcer::CalcHashes(ns, words, 2, hashes);
                hashesAll.insert(hashesAll.end(), hashes.begin(), hashes.end());
            }

            const float prediction = predictor.Predict(hashesAll);

            Y_VERIFY(header[0] == "1" || header[0] == "-1");
            scorer.Score(header[0] == "1", prediction);
            bcMetricsCalculator.Add(prediction, prediction > 0 ? NEthos::EBinaryClassLabel::BCL_POSITIVE : NEthos::EBinaryClassLabel::BCL_NEGATIVE,
                    header[0] == "1" ? NEthos::EBinaryClassLabel::BCL_POSITIVE : NEthos::EBinaryClassLabel::BCL_NEGATIVE);
            if (out) {
                out->Write(ToString(prediction) + "\t" + url + "\n");
            }

        } catch (std::exception& err) {
            printf("Error at line %s: %s\n", line.c_str(), err.what());
            throw;
        }
    }

    printf("Acc = %f\n", scorer.GetAccuracy());
    printf("Acc = %f\n", bcMetricsCalculator.Accuracy());
    printf("F1 = %f\n", scorer.GetF1());
    printf("F1 = %f\n", bcMetricsCalculator.F1());
    printf("Precision = %f\n", scorer.GetPrecision());
    printf("Precision = %f\n", bcMetricsCalculator.Precision());
    printf("Recall = %f\n", scorer.GetRecall());
    printf("Recall = %f\n", bcMetricsCalculator.Recall());
    printf("Mse = %f\n", scorer.GetMse());
    printf("AUC =%f\n", bcMetricsCalculator.AUC());

    return 0;
}

int VowpalWabbitCalcScoreForQueries(int argc, const char **argv) {
    auto opts = NLastGetopt::TOpts::Default();

    TString modelPath;
    opts.AddLongOption('m', "model", "Vowpal Wabbit model in converted binary")
            .Required()
            .RequiredArgument("MODEL")
            .StoreResult(&modelPath);

    TString queriesPath;
    opts.AddLongOption('q', "queries", "Path to a text file with queries")
            .Required()
            .RequiredArgument("QUERIES")
            .StoreResult(&queriesPath);

    bool disableBias = false;
    opts.AddLongOption("disable-bias", "Do not use bias in resulting score")
            .Optional()
            .NoArgument()
            .StoreValue(&disableBias, true);

    NLastGetopt::TOptsParseResult{&opts, argc, argv};
    Y_VERIFY(modelPath);
    Y_VERIFY(queriesPath);

    NVowpalWabbit::TPackedModel model(modelPath);
    TFileInput in(queriesPath);
    TString line;

    while (in.ReadLine(line)) {
        TVector<TString> tokes = ToSimpleLowerCaseTokens(line);
        TVector<ui32> hashes(disableBias ? 0 : 1, NVowpalWabbit::VW_CONST_HASH);
        NVowpalWabbit::THashCalcer::CalcHashesWithAppend(TStringBuf(), tokes, 2, hashes);
        printf("%f\t%s\n", model[hashes], line.c_str());
    }

    return 0;
}

int VowpalWabbitTestModel(int argc, const char **argv) {
    auto opts = NLastGetopt::TOpts::Default();

    TString modelPath;
    opts.AddLongOption('m', "model", "Vowpal Wabbit model in converted binary")
            .Required()
            .RequiredArgument("MODEL")
            .StoreResult(&modelPath);

    TString dataPath;
    opts.AddLongOption('d', "data", "Path to dataset in Vowpal Wabbit fromat")
            .Required()
            .RequiredArgument("DATA")
            .StoreResult(&dataPath);

    TString outputPath;
    opts.AddLongOption('o', "out", "Optional file with VW predictions")
            .Optional()
            .RequiredArgument("OUT")
            .StoreResult(&outputPath);

    NLastGetopt::TOptsParseResult{&opts, argc, argv};
    Y_VERIFY(modelPath);
    Y_VERIFY(dataPath);

    TVowpalWabbitModel model(modelPath);
    TVowpalWabbitPredictor predictor(model);
    printf("Bias = %f\n", predictor.GetConstPrediction());

    TFileInput fileReader(dataPath);

    printf("Testing examples from %s...\n", dataPath.c_str());
    TClassificationScorer scorer;
    TString line;
    TMaybe<TFixedBufferFileOutput> out;
    if (outputPath) {
        out.ConstructInPlace(outputPath);
    }

    while (fileReader.ReadLine(line)) {
        if (line.Empty()) {
            Y_VERIFY(fileReader.ReadLine(line) == 0);
            break;
        }

        try {
            TVector<TStringBuf> parts;
            StringSplitter(line).Split('|').AddTo(&parts);

            TVector<TStringBuf> header;
            StringSplitter(parts[0]).Split(' ').AddTo(&header);
            TStringBuf url = header[2];
            double prediction = predictor.GetConstPrediction();

            for (ui32 i = 1; i < parts.size(); ++i) {
                TVector<TStringBuf> words;
                StringSplitter(parts[i]).Split(' ').AddTo(&words);
                TStringBuf ns = words[0];
                words.erase(words.begin());
                prediction += predictor.Predict(ns, words, 2);
            }

            Y_VERIFY(header[0] == "1" || header[0] == "-1");
            scorer.Score(header[0] == "1", prediction);
            if (out) {
                out->Write(ToString(prediction) + "\t" + url + "\n");
            }

        } catch (std::exception& err) {
            printf("Error at line %s: %s\n", line.c_str(), err.what());
            throw;
        }
    }

    printf("Acc = %f\n", scorer.GetAccuracy());
    printf("F1 = %f\n", scorer.GetF1());
    printf("Precision = %f\n", scorer.GetPrecision());
    printf("Recall = %f\n", scorer.GetRecall());
    printf("Mse = %f\n", scorer.GetMse());

    return 0;
}

class TCalcQueryVwFeature : public NYT::IMapper<NYT::TTableReader<NYT::TYaMRRow>, NYT::TTableWriter<NYT::TYaMRRow>> {
    THashMap<ui64, float> QueruId2Feature;

public:
    Y_SAVELOAD_JOB(QueruId2Feature);

    TCalcQueryVwFeature() = default;
    explicit TCalcQueryVwFeature(THashMap<ui64, float> queruId2Fature)
            : QueruId2Feature(std::move(queruId2Fature))
    {}

    void Start(TWriter*) override {
        Y_VERIFY(!QueruId2Feature.empty());
    }

    void Do(TReader* reader, TWriter* writer) override {
        for (; reader->IsValid(); reader->Next()) {
            const NYT::TYaMRRow& row = reader->GetRow();
            const size_t queryId = FromString<ui64>(row.Key);
            TString features;
            features = row.Value;
            features += "\t" + ToString(QueruId2Feature.at(queryId));

            NYT::TYaMRRow result;
            result.Key = row.Key;
            result.Value = features;
            writer->AddRow(result);
        }
    }
};

int TestVowpalWabbitQueyFactor(int argc, const char **argv) {
    auto opts = NLastGetopt::TOpts::Default();

    TString modelPath;
    opts.AddLongOption('m', "model", "Vowapl Wabbit packed model file")
            .Required()
            .RequiredArgument("MODEL")
            .StoreResult(&modelPath);

    TString source;
    opts.AddLongOption("src", "Source pool table")
            .Required()
            .RequiredArgument("SRC")
            .StoreResult(&source);

    TString dst;
    opts.AddLongOption("dst", "Output table for pool with joined factor")
            .Required()
            .RequiredArgument("DST")
            .StoreResult(&dst);

    TString server;
    opts.AddLongOption("server", "YT proxy")
            .Required()
            .RequiredArgument("SERVER")
            .StoreResult(&server);

    TString queries;
    opts.AddLongOption("queries", "YT table with queries")
            .Required()
            .RequiredArgument("QUERIES")
            .StoreResult(&queries);

    NLastGetopt::TOptsParseResult{&opts, argc, argv};
    Y_VERIFY(modelPath);
    Y_VERIFY(source);
    Y_VERIFY(dst);
    Y_VERIFY(server);
    Y_VERIFY(queries);

    printf("Loading model...\n");
    NVowpalWabbit::TPackedModel model(modelPath);

    printf("Connecting...\n");
    NYT::IClientPtr client = NYT::CreateClient(server);
    THashMap<ui64, float> queruId2Factor;

    printf("Calculating factor...\n");
    for (NYT::TTableReaderPtr<NYT::TYaMRRow> reader = client->CreateTableReader<NYT::TYaMRRow>(queries); reader->IsValid(); reader->Next()) {
        const ui64 queryId = FromString<ui64>(reader->GetRow().Key);
        TString query = StringSplitter(reader->GetRow().Value).Split('\t').ToList<TString>()[0];
        const TVector<TString> tokens = ToSimpleLowerCaseTokens(query);
        TVector<ui32> hashes = { NVowpalWabbit::VW_CONST_HASH };
        NVowpalWabbit::THashCalcer::CalcHashesWithAppend(TStringBuf(), tokens, 2, hashes);
        Y_VERIFY(!queruId2Factor.contains(queryId));
        queruId2Factor[queryId] = model[hashes];
    }

    printf("Applying...\n");
    client->Map(NYT::TMapOperationSpec()
                        .AddInput<NYT::TYaMRRow>(source)
                        .AddOutput<NYT::TYaMRRow>(dst)
                        .MapperSpec(NYT::TUserJobSpec().MemoryLimit(5L * 1024L * 1024L * 1024L)),
                new TCalcQueryVwFeature(queruId2Factor));

    printf("Sorting...\n");
    client->Sort(NYT::TSortOperationSpec()
                         .AddInput(dst)
                         .Output(dst)
                         .SortBy("key"));

    return 0;
}

REGISTER_MAPPER(TCalcQueryVwFeature);
