#pragma once

#include <market/idx/feeds/qparser/inc/feed_info.h>
#include <market/idx/feeds/qparser/inc/queue_iterator.h>
#include <market/idx/feeds/qparser/inc/writer.h>
#include <market/idx/feeds/qparser/src/writers/write_manager.h>

#include <market/library/quick_pipelines/yt_infrastructure.h>

#include <library/cpp/scheme/scheme.h>

#include <util/stream/str.h>


namespace NMarket {

template<typename TWriter, typename... Args>
THolder<TWriteManager> CreateWriteManager(Args&&... args) {
    auto mng = MakeHolder<TWriteManager>();
    mng->AddWriter(MakeHolder<TWriter>(std::forward<Args>(args)...));
    return mng;
}

template<typename TWriter, typename... Args>
void RunWriterTestWithParsingError(
        const TFeedInfo& feedInfo,
        const TVector<IWriter::TMsg>& items,
        bool parsingError,
        Args&&... args
) {
    TSizedLFQueue<IWriter::TConstMsgPtr> outputQueue(10000);
    for (const auto& item: items) {
        outputQueue.Enqueue(MakeAtomicShared<IWriter::TConstMsg>(item));
    }
    outputQueue.Close();

    auto writeManager = CreateWriteManager<TWriter>(std::forward<Args>(args)...);
    for (const auto& msgPtr : TQueueRange(outputQueue)) {
        writeManager->Write(feedInfo, msgPtr);
    }
    writeManager->Flush(parsingError);
}

template<typename TWriter, typename... Args>
void RunWriterTest(
        const TFeedInfo& feedInfo,
        const TVector<IWriter::TMsg>& items,
        Args&&... args
) {
    RunWriterTestWithParsingError<TWriter>(feedInfo, items, /*parsingError=*/false, std::forward<Args>(args)...);
}

}  // namespace NMarket
