#include <extsearch/images/kernel/adult/common/classification_scorer.h>

#include <extsearch/images/robot/index/library/xgboost/xgboost_trees.h>
#include <extsearch/images/robot/index/metadoc/lib/pornclassifiers/pornprobability2.h>

#include <kernel/catboost/catboost_calcer.h>

#include <library/cpp/getopt/small/last_getopt_opts.h>
#include <library/cpp/getopt/small/last_getopt_parser.h>
#include <library/cpp/getopt/small/last_getopt_parse_result.h>

#include <util/stream/file.h>
#include <util/string/split.h>

int TestXgboostModel(int argc, const char **argv) {
    auto opts = NLastGetopt::TOpts::Default();

    TString modelPath;
    opts.AddLongOption('m', "model", "XGBoost model in text format")
            .Required()
            .RequiredArgument("MODEL")
            .StoreResult(&modelPath);

    TString dataPath;
    opts.AddLongOption('d', "data", "Path to dataset in LibSVM fromat")
            .Required()
            .RequiredArgument("DATA")
            .StoreResult(&dataPath);

    NLastGetopt::TOptsParseResult{&opts, argc, argv};
    Y_VERIFY(modelPath);
    Y_VERIFY(dataPath);

    TXgboostTrees model;
    TFileInput in(modelPath);
    model.LoadTextModel(in);

    TFileInput dataIn(dataPath);
    TVector<float> features(model.GetNumFeats());
    TVector<TStringBuf> parts;
    TClassificationScorer scorer;

    for (TString line; dataIn.ReadLine(line);) {
        TVector<TStringBuf> parts;
        StringSplitter(line).Split('\t').AddTo(&parts);
        Y_VERIFY(parts.size() >= features.size() + 2);
        Y_VERIFY(parts[0] == "0"sv || parts[0] == "1"sv);

        for (size_t i = 0; i < features.size(); ++i) {
            TStringBuf f = parts[i + 2];
            features[i] = f == "nan"sv ? std::numeric_limits<float>::quiet_NaN() : FromString<float>(f);
        }

        scorer.Score(parts[0] == "1"sv, model.DoCalcRelev(features.data()));
    }

    printf("Acc = %f\n", scorer.GetAccuracy());
    printf("F1 = %f\n", scorer.GetF1());
    printf("Precision = %f\n", scorer.GetPrecision());
    printf("Recall = %f\n", scorer.GetRecall());
    printf("Mse = %f\n", scorer.GetMse());

    return 0;
}

int TestCatboostModel(int argc, const char **argv) {
    auto opts = NLastGetopt::TOpts::Default();

    TString modelPath;
    opts.AddLongOption('m', "model", "XGBoost model in text format")
            .Required()
            .RequiredArgument("MODEL")
            .StoreResult(&modelPath);

    TString dataPath;
    opts.AddLongOption('d', "data", "Path to dataset in LibSVM fromat")
            .Required()
            .RequiredArgument("DATA")
            .StoreResult(&dataPath);

    NLastGetopt::TOptsParseResult{&opts, argc, argv};
    Y_VERIFY(modelPath);
    Y_VERIFY(dataPath);

    NCatboostCalcer::TCatboostCalcer calcer(ReadModel(modelPath, EModelType::CatboostBinary));

    TFileInput dataIn(dataPath);
    TVector<float> features(calcer.GetNumFeats());
    TVector<TStringBuf> parts;
    TClassificationScorer scorer;

    for (TString line; dataIn.ReadLine(line);) {
        TVector<TStringBuf> parts;
        StringSplitter(line).Split('\t').AddTo(&parts);
        Y_VERIFY(parts.size() >= features.size() + 2);
        Y_VERIFY(parts[0] == "0"sv || parts[0] == "1"sv);

        for (size_t i = 0; i < features.size(); ++i) {
            TStringBuf f = parts[i + 2];
            features[i] = f == "nan"sv ? std::numeric_limits<float>::quiet_NaN() : FromString<float>(f);
        }

        scorer.Score(parts[0] == "1"sv, calcer.DoCalcRelev(features.data()));
    }

    printf("Acc = %f\n", scorer.GetAccuracy());
    printf("F1 = %f\n", scorer.GetF1());
    printf("Precision = %f\n", scorer.GetPrecision());
    printf("Recall = %f\n", scorer.GetRecall());
    printf("Mse = %f\n", scorer.GetMse());

    return 0;
}
