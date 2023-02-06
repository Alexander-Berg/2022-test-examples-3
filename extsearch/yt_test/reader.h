#pragma once

#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/interface/io-inl.h>

#include <util/stream/fwd.h>
#include <util/stream/str.h>

class TFixtureTableReader: public NYT::INodeReaderImpl {
private:
    const TString InFilePath;
    THolder<TFileInput> InFileStream;

    const TString Content;
    TStringStream InStrStream;

    NYT::TNode InnerNode;
    ui64 InnerRowIndex = 0;

public:
    TFixtureTableReader(const TString& filePath);
    TFixtureTableReader() = delete;
    TFixtureTableReader(const TFixtureTableReader&) = delete;
    TFixtureTableReader(TFixtureTableReader&& source);

// Implementation of INodeReaderImpl
public:
    virtual const NYT::TNode& GetRow() const final;
    virtual void MoveRow(NYT::TNode* row) final;

// Implementation of IReaderImplBase
public:
    virtual bool IsValid() const final;
    virtual void Next() final;
    virtual ui32 GetTableIndex() const final;
    virtual ui32 GetRangeIndex() const final;
    virtual ui64 GetRowIndex() const final;
    virtual void NextKey() final;
};
