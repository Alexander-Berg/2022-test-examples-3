#pragma once

#include "tarc_reader.h"
#include "tarc_iterator.h"

#include <search/tools/test_shard/options.h>
#include <search/tools/test_shard/common/random_subset.h>

#include <ysite/yandex/srchmngr/arcmgr.h>

#include <kernel/tarc/markup_zones/view_sent.h>

#include <library/cpp/logger/global/global.h>

#include <util/generic/ptr.h>
#include <util/folder/path.h>
#include <util/random/shuffle.h>

namespace NTestShard {

class TArcSearcher {
public:
    TArcSearcher(const TOptions& opts)
    : ArcManager_(
        JoinFsPaths(opts.FirstShard().GetPath(), "index"),
        AOM_FILE,
        AT_FLAT,
        /* cacheSize */ 0,
        /* replica = */ 0,
        /* repCoef = */ 1,
        Default<NRTYArchive::TMultipartConfig>(),
        nullptr,
        nullptr,
        true,
        false
    )
    , MaxDocs_(opts.Config.GetQuotes().GetMaxDocs())
    , MinWords_(opts.Config.GetQuotes().GetMinWords())
    , MaxWords_(opts.Config.GetQuotes().GetMaxWords())
{}

    THolder<IIndexIterator> GetQuoteIterator() const {
        return GetIterator<TArcQuoteReader>();
    }

    THolder<IIndexIterator> GetWordIterator() const {
        return GetIterator<TArcWordReader>();
    }

private:
    template<typename TReader>
    THolder<IIndexIterator> GetIterator() const {
        TVector<ui32> docs = GetRandomSubset<ui32>(ArcManager_.GetDocCount(), MaxDocs_);

        TReader reader(MinWords_, MaxWords_);
        for (ui32 i = 0; i < docs.size(); ++i) {
            try {
                ui32 doc = docs[i];
                TBlob docText = ArcManager_.GetDocText(doc)->UncompressBlob();
                TTArcViewSentReader sentReader(false, false);
                reader.SetDocId(doc);
                PrintDocText(reader, docText, false, false, false, false, sentReader, false);
                reader.Flush();
                if (i % 1000 == 0) {
                    INFO_LOG << "[quotes] Processed doc " << i << " / " << docs.size() << Endl;
                }
            } catch (...) {
                WARNING_LOG << "Failed doc " << docs[i] << ", exception: " << CurrentExceptionMessage() << Endl;
            }
        }
        return reader.Finish();
    }


private:
    TArchiveManager ArcManager_;
    ui32 MaxDocs_ = 100000u;
    ui32 MinWords_ = 8u;
    ui32 MaxWords_ = 16u;
};

}
