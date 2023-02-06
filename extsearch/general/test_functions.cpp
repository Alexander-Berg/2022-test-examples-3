#include "test_functions.h"

#include <util/generic/vector.h>
#include <util/stream/file.h>
#include <util/string/cast.h>
#include <util/string/split.h>

namespace NImages {
    namespace NRelevance {
        static TSignedPosting ReadSignedPosting(TFileInput& input) {
            TSignedPosting res = 0;
            ui32 brk, word, form;
            int relevLevel;

            input >> brk >> word >> relevLevel >> form;
            TWordPosition::SetBreak(res, brk);
            TWordPosition::SetWord(res, word);
            TWordPosition::SetRelevLevel(res, relevLevel);
            TWordPosition::SetWordForm(res, form);
            return res;
        }

        static TFullPositionEx ReadFullPositionEx(TFileInput& input) {
            TFullPositionEx res;
            res.Pos.Beg = ReadSignedPosting(input);
            res.Pos.End = ReadSignedPosting(input);
            input >> res.WordIdx;
            return res;
        }

        void ReadTestData(const TString& filePath, TVector<TVector<float>>& wordWeights, TVector<TVector<TDocumentHits>>& hits) {
            TFileInput input(filePath);
            while (true) {
                TString firstStr;
                size_t queryWordsCount;
                input.ReadLine(firstStr);
                if (!TryFromString(firstStr, queryWordsCount))
                    break;

                wordWeights.push_back(TVector<float>());
                hits.push_back(TVector<TDocumentHits>());

                for (size_t i = 0; i < queryWordsCount; ++i) {
                    float w;
                    input >> w;
                    wordWeights.back().push_back(w);
                }

                size_t documents;
                input >> documents;
                for (size_t i = 0; i < documents; ++i) {
                    hits.back().push_back(TDocumentHits());

                    size_t hitsCount;
                    input >> hitsCount;
                    for (size_t j = 0; j < hitsCount; ++j) {
                        TFullPositionEx pos = ReadFullPositionEx(input);
                        hits.back()[i].push_back(pos);
                    }
                }
            }
        }

        void ReadTestAns(const TString& filePath, TVector<TVector<float>>& ans, size_t testCount) {
            TFileInput input(filePath);
            for (size_t i = 0; i < testCount; ++i) {
                TString line;
                TVector<TStringBuf> nums;

                ans.push_back(TVector<float>());

                input.ReadLine(line);
                Split(line, " \t", nums);
                for (TStringBuf& num : nums) {
                    float val;
                    val = FromString(num);
                    ans.back().push_back(val);
                }
            }
        }

    } // namespace NRelevance
} // namespace NImages
