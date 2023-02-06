#pragma once

#include <search/web/personalization/core/mlfeatures.h>
#include <search/web/personalization/core/rerank.h>
#include <search/web/util/common/common.h>

#include <search/meta/metasearch.h>
#include <search/meta/context.h>
#include <search/meta/fakedocsource.h>

#include <ysite/yandex/pers/userdata/io/userprofile/io.h>
#include <ysite/yandex/pers/userdata/io/rtuserdata/rtuserdata.h>
#include <ysite/yandex/pers/rerank/formula.h>

#include <library/cpp/scheme/scheme.h>

namespace NPers {
    //TODO: move most of this stuff to .cpp

    TAutoPtr<NRTUserData::TRTUserData> GetRTUserData(const TRequestParams& rp, const TMetaGrouping* g, time_t time, const NSc::TValue& v) {
        TAutoPtr<NRTUserData::TRTUserData> data = new NRTUserData::TRTUserData;
        for (ui32 i = 0; i < v.ArraySize(); i++) {
            data->Add(NRTUserData::FromJson(v[i]));
        }
        if (data->Size()) {
            TAutoPtr<NRTUserData::TRequest> currentRequest = new NRTUserData::TRequest(time, rp.UserRequest, "", rp.UserInterfaceLanguage, 0, "");
            for (ui32 i = 0; i < g->Size(); i++) {
                const TMetaGroup& group = g->GetMetaGroup(i);
                if (group.MetaDocs.size()) {
                    currentRequest->Serp.emplace_back();
                    NRTUserData::TSerpItem& doc = currentRequest->Serp.back();
                    doc.Url = TString{group.MetaDocs.front().Url()};
                }
            }
            data->Add(currentRequest.Release());
            data->Sort();
            return data;
        }
        return nullptr;
    }

    class TDocData {
    private:
        const NSc::TValue& Data;

    public:
        TDocData(const NSc::TValue& data) : Data(data) {}

    public:
        TString GetUrl() const {
            return TString{Data["url"].GetString()};
        }

        TString GetTitle() const {
            return TString{Data["title"].GetString()};
        }

        TString GetHandle() const {
            return TString{Data["handle"].GetString()};
        }

        TString GetCateg() const {
            return TString{Data["categ"].GetString()};
        }

        ui32 GetPassagesSize() const {
            return Data["passages"].ArraySize();
        }

        TString GetPassage(ui32 i) const {
            return TString{Data["passages"][i].GetString()};
        }

        TRelevance GetRelevance() const {
            return Data["relevance"].GetNumber();
        }

        const NSc::TDict& GetFeatures() const {
            return Data["feat"].GetDict();
        }
    };

    class TResp {
    private:
        const NSc::TValue& Data;

    public:
        TResp(const NSc::TValue& data) : Data(data) {}

    public:
        ui32 Size() const {
            return Data.ArraySize();
        }

        TDocData GetDoc(ui32 pos) const {
            return TDocData(Data[pos]);
        }
    };

    class TSearchData {
    private:
        const NSc::TValue& Data;

    public:
        TSearchData(const NSc::TValue& data) : Data(data) {}

    public:
        time_t GetReqTime() const {
            return Data["reqts"].GetNumber(0);
        }

        void FillRP(TRequestParams& rp) const {
            rp.UserReqIdTimestamp    = TInstant::Seconds(GetReqTime());
            rp.UserRequest           = TString{Data["query"].GetString()};
            rp.MostPropabilityLang   = LanguageByName(Data["qlang"].GetString("ru").data());
            rp.QueryClassesMask      = Data["qclass"].GetNumber(0);
            rp.IsForeignQuery        = Data["qforeign"].GetNumber(0);
            rp.CommercialMxFactor    = Data["qcomm"].GetNumber(0);
            rp.PopularityLevel       = Data["qpop"].GetNumber(0);
            rp.IsNav                 = Data["qnav"].GetNumber(0);
            rp.UserInterfaceLanguage = TString{Data["uilang"].GetString("ru")};

            const NSc::TDict& relevParams = Data["relevparams"].GetDict();
            for (NSc::TDict::const_iterator it = relevParams.begin(); it != relevParams.end(); ++it) {
                rp.RelevParams.Set(it->first, it->second);
            }
        }

        TResp GetResp() const {
            return TResp(Data["resp"]);
        }
    };

    class ICtx {
    public:
        virtual ~ICtx() {}

    public:
        virtual IMetaRearrangeContext::TRearrangeParams& GetRearrangeParams() = 0;
        virtual IMetaRearrangeContext::TAdjustParams GetAdjustParams() = 0;
        virtual THolder<TFeatureCalculator> CreateFeatureCalculator(TWsCache&) = 0;
        virtual TMetaGrouping& GetGrouping() = 0;
        virtual time_t GetReqTime() = 0;
    };

    class TFakeMetaRequestAdjuster : public IMetaRequestAdjuster {
    private:
        TSearchSource* SearchSource;

    public:
        TFakeMetaRequestAdjuster(TSearchSource* ss) : SearchSource(ss) {}

    public:
        virtual void ClientDontSendRequest() {}
        virtual const TString& ClientDescr() const { return Default<TString>(); ; }
        virtual void ClientFormFieldInsert(const TStringBuf, const TStringBuf) {}
        virtual void ClientFormFieldRemove(const TStringBuf, int) {}
        virtual int ClientFormFieldCount(const TStringBuf) { return 0; }
        virtual bool ClientFormFieldHas(const TStringBuf, const TStringBuf) { return false; }
        virtual void ClientFormFieldRemoveAll() {}
        virtual const TString& ClientFormField(const TStringBuf, int) { return Default<TString>(); }
        virtual void ClientSetOnlyExplicitGroupings() {}
        virtual bool IsClientEphemeral() const { return false; }
        virtual void AskFactor(const TStringBuf) {}
        virtual const TString& SourceType() const {
            return SearchSource->ProtoConfig_.GetServerDescr();
        }
    };

    class TCtxImpl : public ICtx {
    private:
        TMetaSearch& Ms;
        TFakeDocSource DocSource;
        TAutoPtr<TMetaSearchContext> Ctx;
        TAutoPtr<TMergedRes> Mr;
        TMetaGroupingId GId;
        TAutoPtr<IMetaRearrangeContext::TRearrangeParams> RearrangeParams;
        const TSearchData& SearchData;
        TAutoPtr<TSearchSource> SearchSource;
        TIntrusivePtr<IMetaRequestAdjuster> RequestAdjuster;
        TSearchFields SearchFields;

    public:
        TCtxImpl(TMetaSearch& ms, const TSearchData& searchData)
            : Ms(ms)
            , SearchData(searchData)
        {
            //init various contexts
            Ctx = new TMetaSearchContext(Ms);
            Mr = new TMergedRes(Ctx.Get());
            TGroupingParams gp;
            gp.gAttr = "d";
            gp.gMode = GM_DEEP;
            GId = Ctx->GetOrAddUserGrouping(gp);
            RearrangeParams = new IMetaRearrangeContext::TRearrangeParams(Mr.Get(), GId);
            SearchSource = new TSearchSource();
            SearchSource->ProtoConfig_.SetServerDescr("WEB");
            RequestAdjuster = new TFakeMetaRequestAdjuster(SearchSource.Get());

            //fill req params
            searchData.FillRP(RearrangeParams->Result->Context()->MutableRP());

            //fill cgi params
            Ctx->CgiParam.InsertUnescaped("user_request", RearrangeParams->Result->Context()->RP().UserRequest);

            //fill grouping
            TResp resp = searchData.GetResp();
            for (ui32 i = 0; i < resp.Size(); i++) {
                TMetaGroup& group = GId->second->AddMetaGroup();
                group.Categ = resp.GetDoc(i).GetCateg();
                group.SetRelevance(resp.GetDoc(i).GetRelevance());
                TDocDataHolderPtr doc = DocSource.GetDocStorage().ConstructDoc();
                doc->SetUrl(resp.GetDoc(i).GetUrl());
                TFetchedDocData* docData = doc->MutableFetchedData();
                docData->Title = resp.GetDoc(i).GetTitle();
                doc->Doc()->Handle.FromString(resp.GetDoc(i).GetHandle());
                doc->Doc()->SetRelevance(resp.GetDoc(i).GetRelevance());
                for (ui32 j = 0; j < resp.GetDoc(i).GetPassagesSize(); j++) {
                    docData->Passages.push_back(resp.GetDoc(i).GetPassage(j));
                }
                const NSc::TDict& feat = resp.GetDoc(i).GetFeatures();
                for (NSc::TDict::const_iterator iter = feat.begin(); iter != feat.end(); ++iter) {
                    doc->MutableFactors().InsertFactor(iter->first, iter->second.GetNumber());
                }
                group.MetaDocs.push_back(TMergedDoc(DocSource.GetDocDataEnv(), doc));
            }
        }

    public:
        IMetaRearrangeContext::TRearrangeParams& GetRearrangeParams() {
            return *RearrangeParams;
        }

        IMetaRearrangeContext::TAdjustParams GetAdjustParams() {
            TReqEnv* reqEnv = RequestAdjuster->ReqEnv();
            IMetaRearrangeContext::TAdjustParams adjustParams(RequestAdjuster, Ctx->RP(), SearchFields, *reqEnv, nullptr, Ms.GetRankEnv().GetRankModelsMapFactory());
            return adjustParams;
        }

        THolder<TFeatureCalculator> CreateFeatureCalculator(TWsCache& cache) {
            return THolder(new TFeatureCalculator(cache));
        }

        TMetaGrouping& GetGrouping() {
            TMetaGrouping* g = GId->second;
            if (!g) {
                ythrow yexception() << "GId->second is NULL";
            }
            return *g;
        }

        time_t GetReqTime() {
            return SearchData.GetReqTime();
        }
    };

    class TEnv {
    private:
        TMetaSearch::TParams MsParams;
        TMetaSearch Ms;

    public:
        TEnv() : Ms(&MsParams) {
            Ms.SearchOpen(new TSearchConfig, nullptr);
        }

        ~TEnv() {
            Ms.SearchClose();
        }

    public:
        TAutoPtr<ICtx> CreateContext(const TSearchData& searchData) {
            TAutoPtr<ICtx> ctx = new TCtxImpl(Ms, searchData);
            return ctx;
        }
    };
}
