#include "buffering_processor.h"
#include "collector.h"
#include "parallel_processor.h"
#include "processing_graph.h"
#include "ut/common.h"

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <yt/yt/core/logging/log.h>

// #include <util/generic/fwd.h>
#include <util/generic/vector.h>
#include <util/generic/scope.h>

namespace NPlutonium::NProcessors::NUT {

namespace {

static NYT::NLogging::TLogger Logger{"processing_graph_test"};

class TBaseTestProcessor : public TThrRefBase {
public:
    TBaseTestProcessor(TString name): Logger(name) {
        YT_LOG_INFO("Created");
    };

    NYT::TError Flush() {
        YT_LOG_INFO("Flush()");
        bool expected = false;
        bool res = Flushed.compare_exchange_strong(expected, true);
        if (!res) {
            return NYT::TError("Already flushed");
        }
        return NYT::TError();
    }

    void OnError(NYT::TError) {
        YT_LOG_INFO("OnError()");
        ErrorHappend.store(true);
    }

    void Reset() {
        YT_LOG_INFO("Reset()");
        Flushed.store(false);
        ErrorHappend.store(false);
    }

    NYT::NLogging::TLogger Logger;
    std::atomic<bool> Flushed{false};
    std::atomic<bool> ErrorHappend{false};
};

class TPlusOneProcessor : public TBaseTestProcessor {
public:
    using TInputProfile = TA;
    using TOutputProfile = TA;

    TPlusOneProcessor(): TBaseTestProcessor("TPlusOneProcessor") {}

    auto AddInput(CBatchView<TA> auto inputView) {
        YT_LOG_INFO("AddInput()");
        return Transform<TA>(
            std::move(inputView), 
            [](TTransformMapperArg<decltype(inputView)> a) {
                return TA{a.X + 1};
            }
        );
    }
};


class TTimesTwoProcessor : public TBaseTestProcessor{
public:
    using TInputProfile = TA;
    using TOutputProfile = ui32;

    TTimesTwoProcessor(): TBaseTestProcessor("TTimesTwoProcessor") {}

    auto AddInput(CBatchView<TA> auto inputView) {
        YT_LOG_INFO("AddInput()");
        return Transform<TA>(
            std::move(inputView), 
            [] (TTransformMapperArg<decltype(inputView)> a) -> ui32 {
                return a.X * 2;
            }
        );
    }
};


class TToStringProcessor: public TBaseTestProcessor {
public:
    using TInputProfile = TA;
    using TOutputProfile = TString;

    TToStringProcessor(ui32 minValue): TBaseTestProcessor("TToStringProcessor"), MinValue(minValue) {}

    auto AddInput(CBatchView<TA> auto inputView) {
        YT_LOG_INFO("AddInput()");
        return Transform<TA>(
            std::move(inputView), 
            [] (TTransformMapperArg<decltype(inputView)> a) {
                return ToString(a.X);
            },
            [this] (TTransformFilterArg<decltype(inputView)> a) {
                return a.X >= MinValue;
            }
        );
    }

    ui32 MinValue;
};

}

TEST(ProcessingGraph, SyncProcessingGraph) {
    auto plusOneProcessor = MakeIntrusive<TPlusOneProcessor>();
    auto timesTwoProcessor = MakeIntrusive<TTimesTwoProcessor>();
    auto toStringProcessor = MakeIntrusive<TToStringProcessor>(3u);

    auto ui32Collector = MakeSyncCollector<ui32>();
    auto stringCollector = MakeSyncCollector<TString>();
    

    auto graph = MakeProcessingGraph(
        plusOneProcessor,
        MakeProcessingGraph(
            timesTwoProcessor, ui32Collector
        ),
        MakeProcessingGraph(
            toStringProcessor, stringCollector
        )
    );

    NYT::TError addInputError = graph->AddInput(TABatchView(0, 3));
    ASSERT_THAT(addInputError.IsOK(), testing::IsTrue());

    NYT::TError flushError = graph->Flush();
    ASSERT_THAT(flushError.IsOK(), testing::IsTrue());
    ASSERT_THAT(plusOneProcessor->Flushed.load(), testing::IsTrue());
    ASSERT_THAT(timesTwoProcessor->Flushed.load(), testing::IsTrue());
    ASSERT_THAT(toStringProcessor->Flushed.load(), testing::IsTrue());

    ASSERT_THAT(*ui32Collector->GetResult(), testing::ElementsAre(2u, 4u, 6u));
    ASSERT_THAT(*stringCollector->GetResult(), testing::ElementsAre(TString("3")));

    NYT::TError resetError = graph->Reset();
    ASSERT_THAT(flushError.IsOK(), testing::IsTrue());

    ASSERT_THAT(plusOneProcessor->Flushed.load(), testing::IsFalse());
    ASSERT_THAT(timesTwoProcessor->Flushed.load(), testing::IsFalse());
    ASSERT_THAT(toStringProcessor->Flushed.load(), testing::IsFalse());

    ASSERT_THAT(*ui32Collector->GetResult(), testing::IsEmpty());
    ASSERT_THAT(*stringCollector->GetResult(), testing::IsEmpty());

}

namespace {

struct TThrowingBatchView : TBaseBatchView<TA> {

    static constexpr TStringBuf Error = "TThrowingBatchView Error";

    bool HasNext() {
        return true;
    }

    TA Next() {
        throw NYT::TErrorException() <<= NYT::TError(TString{Error}) ;
    }

};

}

TEST(ProcessingGraph, ThrowingBatchView) {
    auto ui32Collector = MakeSyncCollector<ui32>();
    auto stringCollector = MakeSyncCollector<TString>();
    
    auto plusOneProcessor = MakeIntrusive<TPlusOneProcessor>();
    auto timesTwoProcessor = MakeIntrusive<TTimesTwoProcessor>();
    auto toStringProcessor = MakeIntrusive<TToStringProcessor>(3u);

    auto graph = MakeProcessingGraph(
        plusOneProcessor,
        MakeProcessingGraph(
            timesTwoProcessor, ui32Collector
        ),
        MakeProcessingGraph(
            toStringProcessor, stringCollector
        )
    );

    auto addInputError = graph->AddInput(TThrowingBatchView());
    ASSERT_THAT(addInputError.IsOK(), testing::IsFalse());
    ASSERT_THAT(addInputError.GetSkeleton(), testing::HasSubstr((TString(TThrowingBatchView::Error))));
}

namespace {

class TThrowingProcessor : public TBaseTestProcessor {
public:
    using TInputProfile = TA;
    using TOutputProfile = TA;

    static constexpr TStringBuf Error = "TThrowingProcessor Error";

    TThrowingProcessor(): TBaseTestProcessor("TThrowingProcessor") {}

    TEmptyBatchView<TA> AddInput(CBatchView<TA> auto&&) {
        YT_LOG_INFO("AddInput()");
        throw NYT::TErrorException() <<= NYT::TError(TString{Error});
    }
};

}

TEST(ProcessingGraph, ThrowingProcessor) {
    auto throwingProcessor = MakeIntrusive<TThrowingProcessor>();
    auto collector = MakeSyncCollector<TA>();

    auto graph = MakeProcessingGraph(throwingProcessor, collector);
    NYT::TError error = graph->AddInput(TABatchView(0, 1));
    ASSERT_THAT(error.IsOK(), testing::IsFalse());
    ASSERT_THAT(error.GetSkeleton(), testing::HasSubstr((TString(TThrowingProcessor::Error))));
    ASSERT_THAT(throwingProcessor->ErrorHappend.load(), testing::IsTrue());
}

TEST(ProcessingGraph, Buffering) {
    auto bufferingProcessor = MakeBufferingProcessor<TA>(3);
    auto summator = MakeIntrusive<TSummator>();
    auto collector = MakeSyncCollector<TA>();

    auto graph = MakeProcessingGraph(
        bufferingProcessor,
        MakeProcessingGraph(
            summator,
            collector
        )
    );

    const ui32 totalSize = 4;
    const ui32 viewSize = 2;
    for(ui32 i = 1; i <= totalSize; i += viewSize) {
        TABatchView aView(i, i + viewSize);
        NYT::TError addInputError = graph->AddInput(std::move(aView));
        ASSERT_THAT(addInputError.IsOK(), testing::IsTrue());
    }
    
    NYT::TError flushError = graph->Flush();
    ASSERT_THAT(flushError.IsOK(), testing::IsTrue());

    ASSERT_THAT(*collector->GetResult(), testing::ElementsAre(TA{6}, TA{4}));
    YT_LOG_INFO("Done");
}

TEST(ProcessingGraph, Parallel) {
    const TAsyncTaskPoolConfig config {
        .Name = "test_pool",
        .ParallelTasks_ = 2
    };

    auto summator = MakeIntrusive<TSummator>();
    auto parallelSummator = MakeParallel(summator, config);
    auto collector = MakeSyncCollector<TA>();

    auto graph = MakeProcessingGraph<TProcessorSettings{.AllowSyncPropagation = true}>(
        parallelSummator,
        collector
    );

    const ui32 totalSize = 6;
    const ui32 viewSize = 2;
    TVector<NYT::TFuture<void>> futures;
    for(ui32 i = 1; i <= totalSize; i += viewSize) {
        TABatchView aView(i, i + viewSize);
        NYT::TFuture<void> addInputFuture = graph->AddInput(std::move(aView));
        futures.push_back(addInputFuture);
    }

    NYT::TFuture<void> addInputsFuture = NYT::AllSucceeded(futures).AsVoid();
    ASSERT_THAT(addInputsFuture.Get().IsOK(), testing::IsTrue());
    
    NYT::TFuture<void> flushFuture = graph->Flush();
    ASSERT_THAT(flushFuture.Get().IsOK(), testing::IsTrue());

    ASSERT_THAT(*collector->GetResult(), testing::UnorderedElementsAre(TA{3}, TA{7}, TA{11}));
    YT_LOG_INFO("Done");
}

TEST(ProcessingGraph, ParallelWithAsyncCollector) {
    const TAsyncTaskPoolConfig config {
        .Name = "test_pool",
        .ParallelTasks_ = 2
    };

    auto summator = MakeIntrusive<TSummator>();
    auto parallelSummator = MakeParallel(summator, config);
    auto collector = MakeAsyncCollector<TA>();

    auto graph = MakeProcessingGraph(
        parallelSummator,
        collector
    );

    const ui32 totalSize = 6;
    const ui32 viewSize = 2;
    TVector<NYT::TFuture<void>> futures;
    for(ui32 i = 1; i <= totalSize; i += viewSize) {
        TABatchView aView(i, i + viewSize);
        NYT::TFuture<void> addInputFuture = graph->AddInput(std::move(aView));
        futures.push_back(addInputFuture);
    }

    NYT::TFuture<void> addInputsFuture = NYT::AllSucceeded(futures).AsVoid();
    ASSERT_THAT(addInputsFuture.Get().IsOK(), testing::IsTrue());
    
    NYT::TFuture<void> flushFuture = graph->Flush();
    ASSERT_THAT(flushFuture.Get().IsOK(), testing::IsTrue());

    ASSERT_THAT(*collector->GetResult(), testing::UnorderedElementsAre(TA{3}, TA{7}, TA{11}));
    YT_LOG_INFO("Done");
}

TEST(ProcessingGraph, Complex) {
    const TAsyncTaskPoolConfig config {
        .Name = "test_pool",
        .ParallelTasks_ = 2
    };

    auto bufferingProcessor = MakeBufferingProcessor<TA>(3);
    auto parallelSummator = MakeParallel(MakeIntrusive<TSummator>(), config);

    auto plusOneProcessor = MakeIntrusive<TPlusOneProcessor>();
    auto timesTwoProcessor = MakeIntrusive<TTimesTwoProcessor>();
    auto toStringProcessor = MakeIntrusive<TToStringProcessor>(15u);

    auto ui32Collector = MakeSyncCollector<ui32>();
    auto stringCollector = MakeSyncCollector<TString>();
    
    auto graph = MakeProcessingGraph(
        bufferingProcessor,
        MakeProcessingGraph<TProcessorSettings{.AllowSyncPropagation = true}>(
            parallelSummator, 
            MakeProcessingGraph(
                plusOneProcessor,
                MakeProcessingGraph(
                    timesTwoProcessor, ui32Collector
                ),
                MakeProcessingGraph(
                    toStringProcessor, stringCollector
                )
            )
        )
    );

    const ui32 totalSize = 10;
    const ui32 viewSize = 2;
    TVector<NYT::TFuture<void>> futures;
    for(ui32 i = 1; i <= totalSize; i += viewSize) {
        TABatchView aView(i, i + viewSize);
        NYT::TFuture<void> addInputFuture = graph->AddInput(std::move(aView));
        futures.push_back(addInputFuture);
    }

    NYT::TFuture<void> addInputsFuture = NYT::AllSet(futures).AsVoid();
    ASSERT_THAT(addInputsFuture.Get().IsOK(), testing::IsTrue());

    NYT::TFuture<void> flushFuture = graph->Flush();
    ASSERT_THAT(flushFuture.Get().IsOK(), testing::IsTrue());

    ASSERT_THAT(*ui32Collector->GetResult(), testing::UnorderedElementsAre(14u, 32u, 50u, 22u));

    ASSERT_THAT(*stringCollector->GetResult(), testing::UnorderedElementsAre(TString("16"), TString("25")));
}

}