#include <util/string/split.h>
#include "input_output.h"

using namespace NImages::NLinkSelector;

void TFileReader::ReadColumnNames() {
    TString line;
    InputFile.ReadLine(line);
    Split(line, Splitter, ColumnNames);
}

void SetProtobufField(TLinkFactorsPBPtr package, const TString& token, const TString& fieldName) {
    if (token.length() == 0) {
        return;
    }

    const ::google::protobuf::FieldDescriptor* keyField = NImages::NLinkSelector::TLinkFactorsPB::descriptor()->FindFieldByName(fieldName);
    switch (keyField->cpp_type()) {
        case ::google::protobuf::FieldDescriptor::CPPTYPE_BOOL:
            package->GetReflection()->SetBool(package.Get(), keyField, FromString<bool>(token));
            break;
        case ::google::protobuf::FieldDescriptor::CPPTYPE_UINT32:
            package->GetReflection()->SetUInt32(package.Get(), keyField, FromString<ui32>(token));
            break;
        case ::google::protobuf::FieldDescriptor::CPPTYPE_INT32:
            package->GetReflection()->SetInt32(package.Get(), keyField, FromString<ui32>(token));
            break;
        case ::google::protobuf::FieldDescriptor::CPPTYPE_DOUBLE:
            package->GetReflection()->SetDouble(package.Get(), keyField, FromString<ui32>(token));
            break;
        case ::google::protobuf::FieldDescriptor::CPPTYPE_FLOAT:
            package->GetReflection()->SetFloat(package.Get(), keyField, FromString<ui32>(token));
            break;
        case ::google::protobuf::FieldDescriptor::CPPTYPE_STRING:
            package->GetReflection()->SetString(package.Get(), keyField, token);
            break;
        default:
            break;
    }
}

void TFileReader::GetRanksAndPackages() {
    TString line;
    while (InputFile.ReadLine(line)) {
        TVector<TString> splitResult;
        TLinkFactorsPBPtr package = MakeAtomicShared<TLinkFactorsPB>();
        TRankComparation newComparation;

        Split(line, Splitter, splitResult);
        for (size_t i = 0; i < ColumnNames.size(); i++) {
            if (i < TruthAspectsCount) {
                newComparation.AddTruth(ColumnNames[i], FromString<float>(splitResult[i]));
            } else if (splitResult[i].length() > 0) {
                SetProtobufField(package, splitResult[i], ColumnNames[i]);
            }
        }
        TruthLines.push_back(newComparation);
        Packages.push_back(package);
    }
}

TFileReader::TFileReader(
    const TString& inputPath, size_t ranksNumber)
    : InputFile(inputPath)
    , TruthAspectsCount(ranksNumber)
{
    ReadColumnNames();
    GetRanksAndPackages();
}

size_t TFileReader::size() const {
    return TruthLines.size();
}

const TRankComparation& TFileReader::operator[](size_t index) const {
    return TruthLines[index];
}

TLinkFactorsPBPtr TFileReader::GetPackage(size_t index) const {
    return Packages[index];
}

TVector<TString> TFileReader::GetColumnNames() const {
    return ColumnNames;
}

TRankApplier::TRankApplier(const TFileReader& fileData, ILinkRankerPtr ranker) {
    for (size_t i = 0; i < fileData.size(); i++) {
        RankedLines.push_back(fileData[i]);
        RankedLines.back().SetRankerResult(ranker->Rank(*fileData.GetPackage(i)));
    }
}

TRankAnswer TRankApplier::GetTruthAspect(const TString& key) const {
    NImages::NLinkSelector::TRankAnswer result;
    for (size_t i = 0; i < RankedLines.size(); i++) {
        result.Add(RankedLines[i].GetRank(key));
    }
    return result;
}

TRankAnswer TRankApplier::GetCompositeTruthAspect(
    const THashMap<TString, float>& weights) const {
    NImages::NLinkSelector::TRankAnswer result;
    for (size_t i = 0; i < RankedLines.size(); i++) {
        result.Add(RankedLines[i].GetRank(weights));
    }
    return result;
}

TFileWriter::TFileWriter(const TString& outputPath)
    : OutputFile(outputPath)
{
}

void TFileWriter::Print(float output) {
    OutputFile << output << Endl;
}

void TFileWriter::Print(const TVector<TString>& output) {
    for (auto it = output.begin(); it != output.end(); it++) {
        OutputFile << *it << ' ';
    }
    OutputFile << Endl;
}

void TFileWriter::Print(const TVector<TPoint>& output) {
    for (auto it = output.begin(); it != output.end(); it++) {
        OutputFile << it->X << ' ' << it->Y << Endl;
    }
}
