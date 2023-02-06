#include "input_output.h"
#include "quality.h"

#include <extsearch/images/robot/index/link_selector/library/ranker.h>

#include <extsearch/images/robot/library/opt/params.h>

struct TOptions {
    TString inputFile;
    TString outputFile;
    ui32 TruthAspectsCount;
};

void ParseArguments(int argc, const char* argv[], TOptions& options) {
    NImages::TCmdParams params;

    params.AddRequired("input", "Path to input file with all data", "<input>", &options.inputFile);
    params.AddRequired("output", "Output file for precision result", "<output>", &options.outputFile);
    params.AddRequired("ranks", "number of ranks in train data", "<ranks>", &options.TruthAspectsCount);

    params.Parse(argc, argv);
}

int main(int argc, const char* argv[]) {
    TOptions options;

    ParseArguments(argc, argv, options);

    NImages::NLinkSelector::TFileReader fileReader(options.inputFile, options.TruthAspectsCount);
    NImages::NLinkSelector::TFileWriter fileWriter(options.outputFile);

    TVector<TString> columnNames = fileReader.GetColumnNames();

//    NImages::NLinkSelector::ILinkRankerPtr ticRanker = NImages::NLinkSelector::MakeTicLinkRanker();
//    NImages::NLinkSelector::TRankApplier ticRankerAnswers(fileReader, ticRanker);
//    NImages::NLinkSelector::TRankAnswer ticRankAnswer = ticRankerAnswers.GetTruthAspect(columnNames[1]);
//
//    NImages::NLinkSelector::ILinkRankerPtr dummyRanker = NImages::NLinkSelector::MakeDummyLinkRanker();
//    NImages::NLinkSelector::TRankApplier dummyRankerAnswers(fileReader, dummyRanker);
//    NImages::NLinkSelector::TRankAnswer dummyRankAnswer = dummyRankerAnswers.GetTruthAspect(columnNames[1]);
//
//    NImages::NLinkSelector::TLinkRocQualityCounter rocCounter(ticRankAnswer);
//    NImages::NLinkSelector::TLinkRankerQualityCounter rankerCounter(ticRankAnswer);

//    NImages::NLinkSelector::ILinkRankerPtr catBoostRanker = NImages::NLinkSelector::MakeCatBoostRanker();
//    catBoostRanker->Init("catboost_model");
//    NImages::NLinkSelector::TRankApplier catBoostRankerAnswers(fileReader, catBoostRanker);
//    NImages::NLinkSelector::TRankAnswer catBoostRankAnswer = catBoostRankerAnswers.GetTruthAspect(columnNames[0]);

//    for (size_t i = 0; i < catBoostRankAnswer.size(); i++) {
//        fileWriter.Print(catBoostRankAnswer[i].Truth, catBoostRankAnswer[i].Result);
//    }

    //NImages::NLinkSelector::TLinkRocQualityCounter rocCounter(catBoostRankAnswer);

    //fileWriter.Print(fileReader.GetColumnNames());
//    fileWriter.Print(rankerCounter.GetInversionQuality());
//    fileWriter.Print(rankerCounter.GetDCGQuality());
    //fileWriter.Print(rocCounter.GetRocCurve());

    //fileWriter.Print(rocCounter.CountRocAucScore());

    return 0;
}
