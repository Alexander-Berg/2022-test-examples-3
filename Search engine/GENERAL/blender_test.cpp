#include "blender_test.h"

#include <search/web/util/formula/storage.h>
#include <library/cpp/json/json_prettifier.h>
#include <kernel/hosts/extowner/ext_owner.h>
#include <search/web/util/dup_checker/docschecker.h>

TString CalcVerticalPositions(TMetaSearch& metaSearch, NSc::TValue &scheme, const TString& verticalName) {
    TBlender::TDocs docStorage;
    TMetaSearchContext ctx(metaSearch);
    TFakeDocSource dsFake;

    TString ruleName("Blender");
    TBlender::TDocs::LoadFromScheme(docStorage, dsFake.GetDocStorage(), scheme["ut"]["add"], false);
    for (TBlender::TDocs::iterator it = docStorage.begin(); it != docStorage.end(); ++it) {
        it->second.RearrRule = scheme.Get(verticalName).Get("docs").Has(it->first)? verticalName :ruleName;
    }

    typedef NBlender::TStorageManager::TStorages TStorages;
    TStorages storages;
    NBlender::TStorage stor = NBlender::TStorage(scheme, docStorage);
    storages.insert(TStorages::value_type("blender", stor));

    TMetaGrouping mg(&ctx);
    TSchemeSerializer::Load(mg, dsFake.GetDocStorage(), scheme["ut"]["in"], false);

    TRearrangeDataStub rearrangeData;
    TCanAddResultStub canAddResultChecker(scheme["ut"]["restrictions"]);
    TBlender::TVerbosity silently(false, false, false);

    TBlender blender(scheme, storages);
    blender.DoRearrange(&mg, nullptr, nullptr, rearrangeData, silently, &canAddResultChecker, nullptr);

    const NSc::TArray& desiredRanking = scheme["ut"]["out"];
    size_t end = Min(desiredRanking.size(), mg.Size());
    TString buffer;
    for (size_t n = 0; n < end; ++n) {
        const TMetaGroup &g = mg.GetMetaGroup(n);
        if (g.Empty()) {
            continue;
        }
        const TMergedDoc& doc = g.MetaDocs[0];
        if (doc.FetchedData().IsFake()) {
            continue;
        }
        if (rearrangeData.GetDocMarker(doc, "Rule") == verticalName) {
            buffer += ":" + ToString(n + 1);
        }
    }
    return buffer;
}

void BlenderTest(IInputStream& in, IOutputStream& out, const TInitStub& init, const bool verbose) {
    InitBlenderFormulasStorage([] (TDynamicFormulasStorage& fs) {
            fs.AddDynamicFormulaFromDirectory("./");
            fs.Finalize();
        });

    TString s;
    TString ss;
    while (in.ReadLine(s)) {
        TStringBuf q = s;
        TStringBuf v = q.RNextTok('\t');

        if (!v)
            continue;

        ss.clear();
        Base64Decode(v, ss);

        NSc::TValue inputScheme;
        inputScheme["blender"] = NSc::TValue::FromJson(ss);
        NSc::TValue outputScheme;
        // In this test we ignore inputScheme["ut"]["out"] field (it may be even empty).
        // We just output what blender outputs and compare it with canonical result.
        UnitTestBlender(inputScheme, outputScheme, init, verbose);
        out << q << "\t" << NJson::PrettifyJson(outputScheme.ToJson(), true, 4, true) << Endl;
    }
}

void UnitTestBlender(NSc::TValue &scheme, NSc::TValue &utOut, const TInitStub& init, const bool verbose) {
    TConfigStub cfg(scheme, verbose);
    TBlender blender(scheme, cfg.Storages);

    TSharedDocumentStorage *anyDocStorage = cfg.DocSource.GetDocStorageRef().Get();
    const NQueryData::TQueryDataWrapper queryData;
    NSc::TValue trustedRules;
    TBlender::TDoRearrangeParams blenderDoRearrParams = {
        cfg.GetMsHandler().SnipClassifier.Get(),
        queryData,
        anyDocStorage,
        trustedRules,
        init.OwnerExtractor.Get(),
        nullptr,
        nullptr,
        TVector<NBlender::ICanAddResult*>{}
    };
    blender.DoRearrange(&cfg.MainGrouping, nullptr, nullptr, cfg.RearrangeData, cfg.Silently, blenderDoRearrParams);
    cfg.RearrangeData.FlushDocData(&cfg.MainGrouping);
    NSc::TValue res = verbose ? utOut["result"] : utOut;
    TSchemeSerializer::Save(cfg.MainGrouping, res);

    if (verbose) {
        utOut["not_inserted"] = cfg.RearrangeData.Root["Blender"]["#not_inserted.debug"];
        utOut["inserted"] = cfg.RearrangeData.Root["Blender"]["#inserted.debug"];
        utOut["nailed"] = cfg.RearrangeData.Root["Blender"]["#nailed.debug"];
    }
}


TConfigStub::TConfigStub(NSc::TValue& scheme, const bool verbose)
    : Ctx(TConfigStub::GetMsHandler().MetaSearch),
    Content(DocSource.GetDocStorage().DocDataStorage(), DocSource.GetDocDataEnv(), DocSource, 0),
    RuleName("Blender"),
    RearrangeData(RuleName),
    CanAddResultChecker(scheme["ut"]["restrictions"]),
    Silently(verbose, false, false),
    MainGrouping(&Ctx)
{
    NSc::TValue& mainNode = scheme["blender"];
    TBlender::TDocs::LoadFromScheme(DocStorage, DocSource.GetDocStorage(), mainNode["ut"]["add"], false);
    for(TBlender::TDocs::iterator it = DocStorage.begin(); it != DocStorage.end(); ++it)
        if (it->second.RearrRule.empty())
            it->second.RearrRule = RuleName;

    TSchemeSerializer::Load(MainGrouping, DocSource.GetDocStorage(), mainNode["ut"]["in"], false);

    typedef NBlender::TStorageManager::TStorages TStorages;
    NBlender::TStorage stor = NBlender::TStorage(mainNode, DocStorage);
    Storages.insert(TStorages::value_type("blender", stor));

    mainNode["cfg"]["glue_owner_mode"] = true;
}

TInitStub::TInitStub(const TString& areasPath)
    : OwnerExtractor(new TOwnerExtractor(areasPath))
{}
