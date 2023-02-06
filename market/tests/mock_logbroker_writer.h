#pragma once

#include <market/library/logbroker/multi_writer.h>


namespace NMarket::NQParser::NTest {

class TLogBrokerMultiWriterMock : public NMarket::NLogBroker::IMultiWriter {
public:
    explicit TLogBrokerMultiWriterMock(ui32 workersCount = 1);
    void Connect() override {}
    void Write(TString&& data) override;
    NMarket::NLogBroker::TCommitResponse WriteNonBlocking(TString&& data, ui32 partitionHint = 0) override;
    void Finish() override {}

    TVector<TString> GetWrittenData() const;

private:
    using TWorkerData = TVector<TString>;
    TVector<TWorkerData> WorkersData_;
};

class TLogBrokerMultiPartitionWriterMock : public NMarket::NLogBroker::IMultiPartitionsWriter {
public:
    struct TWrittenData {
        ui32 Partition;
        TString Data;
    };

    void Connect() override {}
    void Write(TString&& data, ui32 partition = 0) override;
    NMarket::NLogBroker::TCommitResponse WriteNonBlocking(TString&& data, ui32 partition = 0) override;
    void Finish() override {}
    TVector<TWrittenData> GetWrittenData() const;

private:
    TVector<TWrittenData> WrittenData_;
};

}  // namespace NMarket::NQParser::NTest
