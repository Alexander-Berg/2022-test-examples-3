#pragma once

#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/interface/io-inl.h>

#include <util/stream/fwd.h>

class TFixtureTableWriter final : public NYT::INodeWriterImpl {
private:
    const TString OutFilePath;
    THolder<TFileOutput> OutFileStream;

public:
    TFixtureTableWriter(const TString& filePath);
    TFixtureTableWriter() = delete;
    TFixtureTableWriter(const TFixtureTableWriter&) = delete;
    TFixtureTableWriter(TFixtureTableWriter&& source);

    virtual ~TFixtureTableWriter() final;
public:
    virtual void AddRow(const NYT::TNode& node, size_t tableIndex) final;
    virtual void AddRow(NYT::TNode&& node, size_t tableIndex) final;
    virtual size_t GetTableCount() const final;
    virtual void FinishTable(size_t) final;
};
