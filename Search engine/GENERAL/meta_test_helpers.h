#pragma once

#include <search/web/util/ut_mocks/meta_mock.h>

namespace NRearrUT {

    struct TRearrangeParamsMock: public IMetaRearrangeContext::TRearrangeParams {
        TRearrangeParamsMock(TMergedRes* result, const TMetaGroupingId& current)
            : IMetaRearrangeContext::TRearrangeParams(result, current)
        {}

        TMetaGrouping* GetMergedGrouping(const TMetaGroupingId& gId) override {
            return Result->GetGrouping(gId);
        }
    };


    struct TMetaRequestAdjusterStub : IMetaRequestAdjuster {
    private:
        TMetaGroupingId DoRequestGrouping(const TGroupingParams& gp, bool forceRequest, const ISourceFilter* sf) override;

        TMetaGroupingId DoRequestUserGrouping(const TGroupingParams& gp, bool forceRequest) override;

    public:
        TMetaRequestAdjusterStub(TStringBuf cldesc, bool isAux, TMetaSearchContext* ctx = nullptr, bool isPrimary = true);

        static TMetaRequestAdjusterRef New(TStringBuf cldesc, bool isAux, TMetaSearchContext* ctx = nullptr, bool isPrimary = true) {
            return new TMetaRequestAdjusterStub(cldesc, isAux, ctx, isPrimary);
        }

        static TMetaRequestAdjusterStub* Cast(TMetaRequestAdjusterRef adj) {
            return dynamic_cast<TMetaRequestAdjusterStub*>(adj.Get());
        }

        void ClientDontSendRequest() override {
            Disabled = true;
        }

        void ClientEnableSendRequest() override {
            Disabled = false;
        }

        void ClientSetOnlyExplicitGroupings() override {}
        void ClientCgiInsert(const TCgiParameters&) override {}
        void AskQualityInfo(const char*) override {}
        void ClientQualityInfoRemove(const char*) override {}

        const TCgiParameters* SourceCgiParameters() const override {
            return &Cgi;
        }

        const TString& SourceType() const override {
            return Descr;
        }

        const TString& ClientDescr() const override {
            return Descr;
        }

        void ClientFormFieldInsert(const TStringBuf k, const TStringBuf v) override {
            Cgi.InsertUnescaped(k, v);
        }

        void ClientFormFieldRemove(const TStringBuf k, int n) override {
            Cgi.Erase(k, n);
        }

        int  ClientFormFieldCount(const TStringBuf k) override {
            return Cgi.NumOfValues(k);
        }

        bool ClientFormFieldHas(const TStringBuf k, const TStringBuf v) override {
            return Cgi.Has(k, v);
        }

        void ClientFormFieldRemoveAll() override {
            Cgi.Flush();
        }

        const TString& ClientFormField(const TStringBuf k, int n) override {
            return Cgi.Get(k, n);
        }

        bool IsClientEphemeral() const override {
            return !IsPrimary;
        }

        bool IsMainSource() const override {
            return !IsAux;
        }

        void AskFactor(const TStringBuf name) override {
            Cgi.InsertUnescaped("gta", name);
        }

        void ClientAppendRelev(TStringBuf k, TStringBuf v) override;

        void AskDocAttribute(const TStringBuf attr) override {
            AskFactor(attr);
        }

        bool Enabled() const noexcept override {
            return true;
        }

        TReqEnv* ReqEnv() const noexcept override {
            return Ctx;
        }

        TMetaRequestAdjusterRef ClientCreateAuxRequest(const char* /*baseAttr*/, EGroupingMode /*mode*/, const TMetaGroupingId& /*aliasId*/) override {
            return ClientAddRequest();
        }

        TMetaGroupingId CreateUserGrouping(const TGroupingParams& gp) override {
            return Ctx ? Ctx->GetOrAddUserGrouping(gp) : TMetaGroupingId{};
        }

        TMetaRequestAdjusterRef ClientAddRequest() override {
            if (IsClientEphemeral()) {
                return nullptr;
            }
            Children.push_back(New(Descr, IsAux, Ctx, false));
            return Children.back();
        }

        TMetaGroupingId CreateAlias(const char* /*baseAttr*/, EGroupingMode /*mode*/, const char* aliasAttr) override {
            TGroupingParams gp(Ctx->RP().SP);
            gp.gAttr   = aliasAttr;
            gp.gMode   = GM_DEEP;

            return CreateUserGrouping(gp);
        }

        TVector<TMetaRequestAdjusterRef> Children;

        TCgiParameters Cgi;
        TString Descr;
        TMetaSearchContext* Ctx = nullptr;
        bool IsAux = false;
        bool IsPrimary = false;
        bool Disabled = false;
        bool RequestSent = false;
    };


    struct TMetaSourceAdjusterStub : IMetaSourceAdjuster {
    public:
        TMetaSourceAdjusterStub(TCgiParameters& cgi, TStringBuf cldesc, bool isAux, TMetaSearchContext* ctx = nullptr, bool isPrimary = true);

        static TMetaSourceAdjusterRef New(TCgiParameters& cgi, TStringBuf cldesc, bool isAux, TMetaSearchContext* ctx = nullptr, bool isPrimary = true) {
            return new TMetaSourceAdjusterStub(cgi, cldesc, isAux, ctx, isPrimary);
        }

        static TMetaSourceAdjusterStub* Cast(TMetaSourceAdjusterRef adj) {
            return dynamic_cast<TMetaSourceAdjusterStub*>(adj.Get());
        }

        void ClientDontSendRequest() override {
            Disabled = true;
        }

        void ClientEnableSendRequest() override {
            Disabled = false;
        }

        void ClientSetOnlyExplicitGroupings() override {}
        void AskQualityInfo(const char*) override {}
        void ClientQualityInfoRemove(const char*) override {}

        const TCgiParameters* SourceCgiParameters() const override {
            return &Cgi;
        }

        const TString& SourceType() const override {
            return Descr;
        }

        const TString& ClientDescr() const override {
            return Descr;
        }

        int  ClientFormFieldCount(const TStringBuf k) override {
            return Cgi.NumOfValues(k);
        }

        bool ClientFormFieldHas(const TStringBuf k, const TStringBuf v) override {
            return Cgi.Has(k, v);
        }

        const TString& ClientFormField(const TStringBuf k, int n) override {
            return Cgi.Get(k, n);
        }

        bool IsClientEphemeral() const override {
            return !IsPrimary;
        }

        bool IsMainSource() const override {
            return !IsAux;
        }

        bool Enabled() const noexcept override {
            return true;
        }

        TMetaSourceAdjusterRef ClientCreateAuxRequest(const char* /*baseAttr*/, EGroupingMode /*mode*/, const TMetaGroupingId& /*aliasId*/) override {
            return ClientAddRequest();
        }

        TMetaGroupingId CreateUserGrouping(const TGroupingParams& gp) override {
            return Ctx ? Ctx->GetOrAddUserGrouping(gp) : TMetaGroupingId{};
        }

        TMetaSourceAdjusterRef ClientAddRequest() override {
            if (IsClientEphemeral()) {
                return nullptr;
            }
            Children.push_back(New(Cgi, Descr, IsAux, Ctx, false));
            return Children.back();
        }

        TMetaGroupingId CreateAlias(const char* /*baseAttr*/, EGroupingMode /*mode*/, const char* aliasAttr) override {
            TGroupingParams gp(Ctx->RP().SP);
            gp.gAttr   = aliasAttr;
            gp.gMode   = GM_DEEP;

            return CreateUserGrouping(gp);
        }

        TVector<TMetaSourceAdjusterRef> Children;

        TCgiParameters& Cgi;
        TString Descr;
        TMetaSearchContext* Ctx = nullptr;
        bool IsAux = false;
        bool IsPrimary = false;
        bool Disabled = false;
        bool RequestSent = false;
    };


    struct TQualityPropsCollectionMock: public IQualityPropsCollection {
        NSc::TValue Scheme;
        NQueryData::TQueryData QueryData;
        NQueryData::TQueryData AuxQueryData;

        TQualityPropsCollectionMock()
            :   QueryDataWrapper(QueryData)
        {}

        void RegisterAggregationRules(TMetaSearchContext& /*ctx*/) override {
        }

        void FillFromRP(const TRequestParams& /*RP*/, bool /*isUpperSearch*/) override {
        }

        void Aggregate(TMetaSearchContext& /*context*/) override {
        }

        void MergeAuxQueryData(const NQueryData::TQueryData& /*qd*/) override {
        }

        const NQueryData::TQueryDataWrapper& GetQueryData() const override {
            return QueryDataWrapper;
        }

        const NQueryData::TQueryData& GetAuxQueryData() const override {
            return AuxQueryData;
        }

        bool GetProperty(const TString& /*name*/, double& /*value*/) const override {
            return false;
        }

    private:
        NQueryData::TQueryDataWrapper QueryDataWrapper;

        const NSc::TValue& DoGetScheme() const override {
            return Scheme;
        }
    };

    struct TUserDataContainerMock: public NPers::TUserDataContainer {
        TUserDataContainerMock()
            : NPers::TUserDataContainer()
        {
        }
    };

    struct TRearrangeEnvironmentMock: public NRearr::IRearrangeEnvironment {
        TMetaSearchContextMock& Ctx;
        NRearr::TAuxSearchRequestData AuxRequestData;
        TVector<TAuxDataRequestRef> AuxDataRequests;
        THashMap<TString, TQuerySearchAuxRequestRef> QuerySearchRequests;
        THashMap<TString, NQueryData::TQueryData> QuerySearchResponses;

        TRearrangeEnvironmentMock(TMetaSearchContextMock& ctx)
            : Ctx(ctx)
            , AuxRequestData(NRearr::ASST_QUERYSEARCH)
        {}

        TSharedDocumentStorage& DocStorage() override {
            return Ctx.DocSource.GetDocStorage();
        }

        TExternalDocStorage& DocStorage(size_t clientNum) noexcept override {
            Y_UNUSED(clientNum);
            return Ctx.DocSource.GetDocStorage().ExternalDocStorage();
        }

        TExternalDocStorage& DocStorage(TStringBuf) noexcept override {
            return Ctx.DocSource.GetDocStorage().ExternalDocStorage();
        }

        TSharedDocumentStorage& SharedDocStorage(TStringBuf) noexcept override {
            return Ctx.DocSource.GetDocStorage();
        }

        IMsPool& MemoryPool() override {
            return Ctx.DocSource.GetDocDataEnv().StringStorage();
        }

        TSelfFlushLogFramePtr GetEventLogFrame() const override {
            return Ctx.Ctx.GetEventLogFrame();
        }

        TUnistatFramePtr GetStatLogFrame() const override {
            return Ctx.Ctx.GetStatLogFrame();
        }

        const NQueryData::TQueryData& GetQuerySearchResponse(TStringBuf id) const override {
            if (auto* resp = QuerySearchResponses.FindPtr(id)) {
                return *resp;
            } else {
                return NQueryData::TQueryData::default_instance();
            }
        }

        NQueryData::TQueryData& GetQuerySearchResponseMutable(TStringBuf id) override {
            return QuerySearchResponses[id];
        }

        void AddQuerySearchRequestHandle(TStringBuf id, TQuerySearchAuxRequestRef req) override {
            QuerySearchRequests[id] = req;
        }

        TQuerySearchAuxRequestRef GetQuerySearchRequestHandle(TStringBuf id) override {
            if (auto* resp = QuerySearchRequests.FindPtr(id)) {
                return *resp;
            } else {
                return nullptr;
            }
        }

        void RegisterAuxSource(TStringBuf /*sourceName*/, IRearrangeRuleContext* /*ruleCtx*/) override {
        }

        NRearr::TAuxSearchRequestData& AuxSearchSource(TStringBuf /*source*/, NRearr::EAuxSearchSourceType, NRearr::ESearchPhase) override {
            return AuxRequestData;
        }

        void SendAuxRequest(TAuxDataRequestRef request) override {
            AuxDataRequests.push_back(request);
        }

        void WaitAuxRequests(const TVector<TAuxDataRequestRef>&, const TString&) override {

        }

        IMetaRequestAdjuster* GetMetaClientRequestAdjuster(TStringBuf sourceName) override {
            return Ctx.Ctx.GetMetaClientRequestAdjuster(sourceName.data());
        }

        TMetaGrouping* GetAuxGrouping(const TMetaGroupingId& auxiliaryGId, size_t needCount = 0, const char* baseAttr = "d") override;

        TDuration GetTimeoutByClient(const TStringBuf clientDescr) const override;

        TInstant GetRequestTimestamp() const override;
    };

    struct TQualityDataStorageMock: public IQualityDataStorage {
        TQualityPropsCollectionMock Props;
        TUserDataContainerMock UserDataCont;

        IQualityPropsCollection& PropsCollection() noexcept override {
            return Props;
        }

        const IQualityPropsCollection& PropsCollection() const noexcept override {
            return Props;
        }

        NPers::TUserDataContainer& UserDataContainer() override {
            return UserDataCont;
        }

        const NPers::TUserDataContainer& UserDataContainer() const override {
            return UserDataCont;
        }
    };

    struct TBlenderDataStorageMock : public IBlenderDataStorage {
        TDynamicFormulasStorage              FormulasStorage;
        NFacts::TFacts                       Facts;
        NBlender::TStorageManager::TStorages BlenderStorages;
        NBlender::TBlenderFactorsCalculator  BlenderFactorsCalculators;

        IQualityDataStorage&                 Parent;
        TDynamicFormulasStorageContext       FormulasContext;

        TBlenderDataStorageMock(IQualityDataStorage& parent)
            : Parent(parent)
            , FormulasContext(FormulasStorage)
        {
        }

        IQualityPropsCollection& PropsCollection() noexcept override {
            return Parent.PropsCollection();
        }

        const IQualityPropsCollection& PropsCollection() const noexcept override {
            return Parent.PropsCollection();
        }

        NPers::TUserDataContainer& UserDataContainer() override {
            return Parent.UserDataContainer();
        }

        const NPers::TUserDataContainer& UserDataContainer() const override {
            return Parent.UserDataContainer();
        }

        TBlender Blender() override {
            return TBlender(PropsCollection().Scheme(), BlenderStorages);
        }

        NFacts::TFacts& GetFacts() override {
            return Facts;
        }

        const NFacts::TFacts& GetFacts() const override {
            return Facts;
        }

        TDynamicFormulasStorageContext& GetFormulas() override {
            return FormulasContext;
        }

        const TDynamicFormulasStorageContext& GetFormulas() const override {
            return FormulasContext;
        }

        NBlender::TBlenderFactorsCalculator& GetBlenderFactors() override {
            return BlenderFactorsCalculators;
        }

        const NBlender::TBlenderFactorsCalculator& GetBlenderFactors() const override {
            return BlenderFactorsCalculators;
        }

        const NBlender::NDynamicFactors::TFactorBuilder& GetFactorBuilder() const override {
            return FormulasStorage.GetFactorBuilder();
        }

    };

}
