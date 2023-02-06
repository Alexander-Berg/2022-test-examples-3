#include "writer.h"

#include <util/generic/strbuf.h>

#include <utility> // std::move

TFixtureTableWriter::TFixtureTableWriter(const TString& filePath)
    : OutFilePath(filePath)
    , OutFileStream(MakeHolder<TFileOutput>(OutFilePath))
{
}

TFixtureTableWriter::TFixtureTableWriter(TFixtureTableWriter&& source)
    : OutFilePath(std::move(source.OutFilePath))
    , OutFileStream(std::move(source.OutFileStream))
{
}

TFixtureTableWriter::~TFixtureTableWriter() {
    OutFileStream->Flush();
    OutFileStream->Finish();
}

void TFixtureTableWriter::AddRow(const NYT::TNode& node, size_t tableIndex) {
    Y_UNUSED(tableIndex);

    TStringStream strStream;
    node.Save(&strStream);
    strStream.Flush();
    strStream.Finish();

    *OutFileStream << strStream.Size() << Endl;
    *OutFileStream << TStringBuf(strStream.Data(), strStream.Size());

}

void TFixtureTableWriter::AddRow(NYT::TNode&& node, size_t tableIndex) {
    AddRow(node, tableIndex);
}


size_t TFixtureTableWriter::GetTableCount() const {
    return 1;
}

void TFixtureTableWriter::FinishTable(size_t) {
    OutFileStream->Finish();
}
