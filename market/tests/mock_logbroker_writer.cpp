#include "mock_logbroker_writer.h"

#include <library/cpp/logger/global/global.h>

#include <util/random/random.h>


namespace NMarket::NQParser::NTest {

TLogBrokerMultiWriterMock::TLogBrokerMultiWriterMock(ui32 workersCount /*= 1*/) {
    WorkersData_.resize(workersCount);
}

void TLogBrokerMultiWriterMock::Write(TString&& data) {
    WriteNonBlocking(std::move(data));
}

NMarket::NLogBroker::TCommitResponse TLogBrokerMultiWriterMock::WriteNonBlocking(TString&& data, ui32) {
    WorkersData_[RandomNumber<ui32>(static_cast<ui32>(WorkersData_.size()))].push_back(data);
    return NThreading::MakeFuture(NPersQueue::TProducerCommitResponse(0, NPersQueue::TData(), NPersQueue::TWriteResponse()));
}

TVector<TString> TLogBrokerMultiWriterMock::GetWrittenData() const {
    TVector<TString> writtenData;
    for(const auto& worker : WorkersData_) {
        for(const auto& data : worker) {
            writtenData.push_back(data);
        }
    }
    return writtenData;
}

void TLogBrokerMultiPartitionWriterMock::Write(TString&& data, ui32 partition) {
    WriteNonBlocking(std::move(data), partition);
}

NMarket::NLogBroker::TCommitResponse TLogBrokerMultiPartitionWriterMock::WriteNonBlocking(TString&& data, ui32 partition) {
    WrittenData_.push_back({partition, data});
    return NThreading::MakeFuture(NPersQueue::TProducerCommitResponse(0, NPersQueue::TData(), NPersQueue::TWriteResponse()));
}

TVector<TLogBrokerMultiPartitionWriterMock::TWrittenData> TLogBrokerMultiPartitionWriterMock::GetWrittenData() const {
    return WrittenData_;
}

}  // namespace NMarket::NQParser::NTest
