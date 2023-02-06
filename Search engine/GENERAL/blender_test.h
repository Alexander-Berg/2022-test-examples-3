#pragma once

#include "util.h"

#include <search/web/blender/blender/blender.h>
#include <search/web/blender/blender/diversity_tools.h>

#include <search/web/util/dup_checker/dup_checker.h>
#include <search/web/util/scheme_serializer/scheme_serializer.h>
#include <search/meta/context.h>
#include <search/meta/metasearch.h>
#include <search/meta/fakedocsource.h>

#include <kernel/classify_results/snippet_classifier.h>
#include <kernel/hosts/extowner/ext_owner.h>

struct TMetaSearchHandler {
    TMetaSearch::TParams FakeParams;
    TMetaSearch MetaSearch;
    THolder<ISnippetClassifier> SnipClassifier;

    TMetaSearchHandler()
        : FakeParams()
        , MetaSearch(&FakeParams)
        , SnipClassifier(CreateSnipClassifier())
    {
        TAutoPtr<TSearchConfig> searchCfg(new TSearchConfig);
        searchCfg->ProtoCollection_.SetRequestThreads(1);
        MetaSearch.SearchOpen(searchCfg, nullptr);
    }

    ~TMetaSearchHandler() {
        MetaSearch.SearchClose();
    }
};

struct TConfigStub {
    static TMetaSearchHandler& GetMsHandler() {
        return *Singleton<TMetaSearchHandler>();
    }

    TMetaSearchContext Ctx;
    TFakeDocSource DocSource;
    TSearchContent Content;
    TString RuleName;
    TRearrangeDataStub RearrangeData;
    TCanAddResultStub CanAddResultChecker;
    TBlender::TVerbosity Silently;
    TBlender::TDocs DocStorage;
    TMetaGrouping MainGrouping;
    NBlender::TStorageManager::TStorages Storages;

    TConfigStub(NSc::TValue& scheme, const bool verbose = false);
};

struct TInitStub {
    THolder<TOwnerExtractor> OwnerExtractor;

    TInitStub(const TString& areasPath);
};


typedef std::pair<TMsString, TMsString> TGlueValues; //glue, glueType
typedef THashMap<TMsString, TGlueValues> TGlues ;//url, ..

void BlenderTest(IInputStream& in, IOutputStream& out, const TInitStub& init, const bool verbose = false);
void UnitTestBlender(NSc::TValue &scheme, NSc::TValue &utOut, const TInitStub& init, const bool verbose);

TString CalcVerticalPositions(TMetaSearch& metaSearch, NSc::TValue &scheme, const TString& verticalName);
