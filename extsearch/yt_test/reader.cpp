#include "reader.h"

#include <util/string/cast.h>

#include <utility> // std::move

TFixtureTableReader::TFixtureTableReader(const TString& filePath)
    : InFilePath(filePath)
    , InFileStream(MakeHolder<TFileInput>(filePath))
    , Content(InFileStream->ReadAll())
    , InStrStream(Content)
{
    Next();
}

TFixtureTableReader::TFixtureTableReader(TFixtureTableReader&& source)
    : InFilePath(std::move(source.InFilePath))
    , InFileStream(std::move(source.InFileStream))
    , Content(std::move(source.Content))
    , InStrStream(std::move(source.InStrStream))
    , InnerNode(std::move(source.InnerNode))
{
    InnerRowIndex = source.InnerRowIndex;
    source.InnerRowIndex = 0;
}

const NYT::TNode& TFixtureTableReader::GetRow() const {
    return InnerNode;
}

void TFixtureTableReader::MoveRow(NYT::TNode* row) {
    *row = std::move(InnerNode);
}

bool TFixtureTableReader::IsValid() const {
    return !InnerNode.IsNull();
}

void TFixtureTableReader::Next() {
    TString line;
    if (!InStrStream.ReadLine(line)) {
        InnerNode = NYT::TNode::CreateEntity();
        return;
    }
    const ui64 nodeSize = IntFromString<ui64, 10>(line);
    THolder<char, TDeleteArray> data(new char[nodeSize]);
    InStrStream.Load(data.Get(), nodeSize);
    TString nodeStr(data.Get(), nodeSize);
    TStringStream lineInputStr(nodeStr);
    InnerNode.Load(&lineInputStr);
}

ui32 TFixtureTableReader::GetTableIndex() const {
    return 1;
}

ui32 TFixtureTableReader::GetRangeIndex() const {
    return 0;
}

ui64 TFixtureTableReader::GetRowIndex() const {
    return InnerRowIndex;
}

void TFixtureTableReader::NextKey() {
    // TODO(mseifullin): Do not know what to do here
}
