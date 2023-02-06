#pragma once

#include "meta_test_helpers.h"

#include <search/web/core/configuration/conf_json/cj_defs.h>
#include <search/web/core/configuration/conf_json/conf_json.h>
#include <search/web/core/rearrange.h>
#include <search/web/util/ut_mocks/meta_mock.h>

#include <search/reqparam/treat_cgi_request.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/unittest/registar.h>

namespace NRearrUT {

    class TRearrangeTestBase : public TTestBase {
    protected:
        TMetaSearchMock Search;

        struct TTestCtx {
            TMetaSearchContextMock Ctx;
            TRequestParams& RP;
            TQualityDataStorageMock QualityDataStorage;
            TRearrangeEnvironmentMock RearrangeEnvironment;
            TString RuleName;
            static size_t MockConfJsonRank;

            TTestCtx(const TMetaSearchMock& search, const NSc::TValue& localScheme = NSc::TValue::DefaultValue(), const TCgiParameters& cgiParams = TCgiParameters())
                : Ctx(search, localScheme, cgiParams)
                , RP(Ctx.Ctx.MutableRP())
                , RearrangeEnvironment(Ctx)
            {}

            void SetQuery(const TString& query) {
                TCgiParameters params(query);
                NCgiRequest::TreatCgiParams(RP, params);
                Ctx.Ctx.SetupContextCgi(params);
            }

            NSc::TValue& GetScheme() {
                return QualityDataStorage.Props.Scheme;
            }

            NSc::TValue& LocalScheme() {
                return GetScheme()["Local"][RuleName];
            }

            NSc::TValue& Clipboard(TStringBuf target) {
                return GetScheme()["Local"][target];
            }

            const NSc::TValue& LocalScheme() const {
                return const_cast<TTestCtx*>(this)->LocalScheme();
            }

            void FillLocalScheme(const TStringBuf searchType) {
                using namespace NRearr;

                static const TString CONFIG_PATH = "rearrs_conf.json";

                const TString configFile = NResource::Find(TString::Join("/", CONFIG_PATH));
                const NSc::TValue configJson = NSc::TValue::FromJsonThrow(configFile, NSc::TValue::JO_PARSER_STRICT);

                const NSc::TValue rules = GetMergedRearrangeRules(configJson, searchType);
                for (const auto& rule : rules.GetArray()) {
                    const TString name = TString{rule.Get(CJ_NAME).GetString()};
                    if (name == RuleName) {
                        const auto& baseOptions = rule[CJ_SUBCONFIGS]
                                                      [ToString(NRl::RL_UNIVERSE)]
                                                      [ToString(TRequestParams::EDevice::D_ANY)]
                                                      [ToString(TRequestParams::EPlatform::PL_ANY)]
                                                      [CJ_OPTIONS];
                        LocalScheme().MergeUpdate(baseOptions);
                        break;
                    }
                }
            }

            void EnableMetaSearchMode(TRearrangeRuleContextRef ctx) {
                ctx->DoEnableMetaSearchMode(Ctx.Ctx);
            }

            void InitRule(TRearrangeRuleContextRef ctx, const TString& ruleName, bool isDbg = false) {
                RuleName = ruleName;
                ctx->Init(&QualityDataStorage, &RearrangeEnvironment, ruleName, MockConfJsonRank++, NRl::RL_UNIVERSE,
                          TRequestParams::EDevice::D_ANY, TRequestParams::EPlatform::PL_ANY, isDbg);
            }

            void InitRuleParams(const TRearrangeRuleContextRef& ruleCtx) {
                const TRequestParams& rp = Ctx.Ctx.RP();
                const TRankModelsMapFactory* usedModelsFactory = Ctx.ModelsFactoryToUse;
                if (!usedModelsFactory && Ctx.Ctx.Parent) {
                    usedModelsFactory = Ctx.Ctx.Parent->GetRankEnv().GetRankModelsMapFactory();
                }
                IMetaRearrangeContext::TAdjustRuleParams adjParams(rp, Ctx.Ctx, Ctx.Ctx, nullptr, usedModelsFactory);

                ruleCtx->InitRuleParams(adjParams);
            }

            void PrepareRearrangeParams(const TRearrangeRuleContextRef& ruleCtx) {
                const TRequestParams& rp = Ctx.Ctx.RP();
                const TRankModelsMapFactory* usedModelsFactory = Ctx.ModelsFactoryToUse;
                if (!usedModelsFactory && Ctx.Ctx.Parent) {
                    usedModelsFactory = Ctx.Ctx.Parent->GetRankEnv().GetRankModelsMapFactory();
                }
                IMetaRearrangeContext::TAdjustRuleParams adjRuleParams(rp, Ctx.Ctx, Ctx.Ctx, nullptr, usedModelsFactory);

                ruleCtx->PrepareRearrangeParams(adjRuleParams);
            }

            void AdjustClientParams(const TRearrangeRuleContextRef& ruleCtx, const TMetaRequestAdjusterRef& adj) {
                const TRequestParams& rp = Ctx.Ctx.RP();
                const TRankModelsMapFactory* usedModelsFactory = Ctx.ModelsFactoryToUse;
                if (!usedModelsFactory && Ctx.Ctx.Parent) {
                    usedModelsFactory = Ctx.Ctx.Parent->GetRankEnv().GetRankModelsMapFactory();
                }
                IMetaRearrangeContext::TAdjustParams adjParams(adj, rp, Ctx.Ctx, Ctx.Ctx, nullptr, usedModelsFactory);

                ruleCtx->AdjustClientParams(adjParams);
            }

            void AdjustClientParams(const TMetaRequestAdjusterRef& adj, NAdjustClientParams::IClientParamsAdjuster& adjuster) {
                const TRequestParams& rp = Ctx.Ctx.RP();
                const TRankModelsMapFactory* usedModelsFactory = Ctx.ModelsFactoryToUse;
                if (!usedModelsFactory && Ctx.Ctx.Parent) {
                    usedModelsFactory = Ctx.Ctx.Parent->GetRankEnv().GetRankModelsMapFactory();
                }
                IMetaRearrangeContext::TAdjustParams adjParams(adj, rp, Ctx.Ctx, Ctx.Ctx, nullptr, usedModelsFactory);

                adjuster.AdjustClientParams(GetScheme(), adjParams);
            }

            void ProcessSourceAdjuster(const TRearrangeRuleContextRef& ruleCtx, const TMetaSourceAdjusterRef& adj) {
                const TRequestParams& rp = Ctx.Ctx.RP();
                const TRankModelsMapFactory* usedModelsFactory = Ctx.ModelsFactoryToUse;
                if (!usedModelsFactory && Ctx.Ctx.Parent) {
                    usedModelsFactory = Ctx.Ctx.Parent->GetRankEnv().GetRankModelsMapFactory();
                }
                IMetaRearrangeContext::TAdjustSourceParams adjParams(adj, rp, Ctx.Ctx, Ctx.Ctx, nullptr, usedModelsFactory);

                ruleCtx->DoProcessSourceAdjuster(adjParams);
            }

            void AdjustFactorRequests(const TRearrangeRuleContextRef& ruleCtx, const TMetaRequestAdjusterRef& adj) {
                const TRequestParams& rp = Ctx.Ctx.RP();
                const TRankModelsMapFactory* usedModelsFactory = Ctx.ModelsFactoryToUse;
                if (!usedModelsFactory && Ctx.Ctx.Parent) {
                    usedModelsFactory = Ctx.Ctx.Parent->GetRankEnv().GetRankModelsMapFactory();
                }
                IMetaRearrangeContext::TAdjustParams adjParams(adj, rp, Ctx.Ctx, Ctx.Ctx, nullptr, usedModelsFactory);

                ruleCtx->AdjustFactorRequests(adjParams);
            }

            const TSearcherProps& RearrangeAfterMetaFeatures(const TRearrangeRuleContextRef& ruleCtx, const TString& grp = "d", const TString& ruleName = "") {
                TRearrangeParamsMock rearrParams(Ctx.Ctx.MR(), Ctx.GetGroupingId(grp));
                rearrParams.SetCurrentRule(ruleName);
                rearrParams.SetCurrentRuleInfo(ruleCtx.Get());
                Ctx.Ctx.Properties()->clear();
                ruleCtx->RearrangeAfterMetaFeatures(rearrParams);
                return rearrParams.GetProperties();
            }

            const TSearcherProps& RearrangeAfterMetaRank(const TRearrangeRuleContextRef& ruleCtx, const TString& grp = "d", const TString& ruleName = "") {
                TRearrangeParamsMock rearrParams(Ctx.Ctx.MR(), Ctx.GetGroupingId(grp));
                rearrParams.SetCurrentRule(ruleName);
                rearrParams.SetCurrentRuleInfo(ruleCtx.Get());
                Ctx.Ctx.Properties()->clear();
                ruleCtx->RearrangeAfterMetaRank(rearrParams);
                return rearrParams.GetProperties();
            }

            const TSearcherProps& RearrangeAfterMerge(const TRearrangeRuleContextRef& ruleCtx, const TString& grp = "d", const TString& ruleName = "") {
                TRearrangeParamsMock rearrParams(Ctx.Ctx.MR(), Ctx.GetGroupingId(grp));
                rearrParams.SetCurrentRule(ruleName);
                rearrParams.SetCurrentRuleInfo(ruleCtx.Get());
                Ctx.Ctx.Properties()->clear();
                ruleCtx->RearrangeAfterMerge(rearrParams);
                return rearrParams.GetProperties();
            }

            const TSearcherProps& RearrangeAfterMergeKeepProperties(const TRearrangeRuleContextRef& ruleCtx, const TString& grp = "d", const TString& ruleName = "") {
                TRearrangeParamsMock rearrParams(Ctx.Ctx.MR(), Ctx.GetGroupingId(grp));
                rearrParams.SetCurrentRule(ruleName);
                rearrParams.SetCurrentRuleInfo(ruleCtx.Get());
                ruleCtx->RearrangeAfterMerge(rearrParams);
                return rearrParams.GetProperties();
            }

            const TSearcherProps& RearrangeAfterFetch(const TRearrangeRuleContextRef& ruleCtx, const TString& grp = "d", const TString& ruleName = "") {
                TRearrangeParamsMock rearrParams(Ctx.Ctx.MR(), Ctx.GetGroupingId(grp));
                rearrParams.SetCurrentRule(ruleName);
                rearrParams.SetCurrentRuleInfo(ruleCtx.Get());
                Ctx.Ctx.Properties()->clear();
                ruleCtx->RearrangeAfterFetch(rearrParams);
                TMetaGroupingId gId = Ctx.GetGroupingId(grp);
                if (gId.Grouping) {
                    rearrParams.FlushDocData(gId.Grouping);
                }
                return rearrParams.GetProperties();
            }

            const TSearcherProps& RearrangeBeforeFetch(const TRearrangeRuleContextRef& ruleCtx, const TString& grp = "d", const TString& ruleName = "") {
                TRearrangeParamsMock rearrParams(Ctx.Ctx.MR(), Ctx.GetGroupingId(grp));
                rearrParams.SetCurrentRule(ruleName);
                rearrParams.SetCurrentRuleInfo(ruleCtx.Get());
                Ctx.Ctx.Properties()->clear();
                ruleCtx->RearrangeBeforeFetch(rearrParams);
                TMetaGroupingId gId = Ctx.GetGroupingId(grp);
                if (gId.Grouping) {
                    rearrParams.FlushDocData(gId.Grouping);
                }
                return rearrParams.GetProperties();
            }

            const TSearcherProps& RearrangeAfterFetchKeepProperties(const TRearrangeRuleContextRef& ruleCtx, const TString& grp = "d", const TString& ruleName = "") {
                TRearrangeParamsMock rearrParams(Ctx.Ctx.MR(), Ctx.GetGroupingId(grp));
                rearrParams.SetCurrentRule(ruleName);
                rearrParams.SetCurrentRuleInfo(ruleCtx.Get());
                ruleCtx->RearrangeAfterFetch(rearrParams);
                TMetaGroupingId gId = Ctx.GetGroupingId(grp);
                if (gId.Grouping) {
                    rearrParams.FlushDocData(gId.Grouping);
                }
                return rearrParams.GetProperties();
            }

            const TSearcherProps& EnrichDocuments(const TRearrangeRuleContextRef& ruleCtx, const TString& grp = "d", const TString& ruleName = "") {
                TRearrangeParamsMock rearrParams(Ctx.Ctx.MR(), Ctx.GetGroupingId(grp));
                rearrParams.SetCurrentRule(ruleName);
                rearrParams.SetCurrentRuleInfo(ruleCtx.Get());
                Ctx.Ctx.Properties()->clear();
                ruleCtx->EnrichDocuments(rearrParams);
                TMetaGroupingId gId = Ctx.GetGroupingId(grp);
                if (gId.Grouping) {
                    rearrParams.FlushDocData(gId.Grouping);
                }
                return rearrParams.GetProperties();
            }

            const TSearcherProps& PostProcess(const TRearrangeRuleContextRef& ruleCtx, const TString& grp = "d", const TString& ruleName = "") {
                TRearrangeParamsMock rearrParams(Ctx.Ctx.MR(), Ctx.GetGroupingId(grp));
                rearrParams.SetCurrentRule(ruleName);
                rearrParams.SetCurrentRuleInfo(ruleCtx.Get());
                Ctx.Ctx.Properties()->clear();
                ruleCtx->PostProcess(rearrParams);
                TMetaGroupingId gId = Ctx.GetGroupingId(grp);
                if (gId.Grouping) {
                    rearrParams.FlushDocData(gId.Grouping);
                }
                return rearrParams.GetProperties();
            }

            const TSearcherProps& PostProcessKeepProperties(const TRearrangeRuleContextRef& ruleCtx, const TString& grp = "d", const TString& ruleName = "") {
                TRearrangeParamsMock rearrParams(Ctx.Ctx.MR(), Ctx.GetGroupingId(grp));
                rearrParams.SetCurrentRule(ruleName);
                rearrParams.SetCurrentRuleInfo(ruleCtx.Get());
                ruleCtx->PostProcess(rearrParams);
                TMetaGroupingId gId = Ctx.GetGroupingId(grp);
                if (gId.Grouping) {
                    rearrParams.FlushDocData(gId.Grouping);
                }
                return rearrParams.GetProperties();
            }

            void InsertRearrangeError(const TString& msg, const TString& callback) {
                Y_UNUSED(msg);
                Y_UNUSED(callback);
            }

            NRearr::IRearrangeEnvironment& GetRearrangeEnv() {
                return RearrangeEnvironment;
            }
        };


        struct TBlenderTestCtx: public TTestCtx {
            TBlenderDataStorageMock BlenderDataStorage;

            TBlenderTestCtx(const TMetaSearchMock& search, const NSc::TValue& localScheme = NSc::TValue::DefaultValue(), const TCgiParameters& cgiParams = TCgiParameters())
                : TTestCtx(search, localScheme, cgiParams)
                , BlenderDataStorage(QualityDataStorage)
            {}

            void InitRule(TRearrangeRuleContextRef ctx, const TString& ruleName, bool isDbg = false) {
                RuleName = ruleName;
                ctx->Init(&BlenderDataStorage, &RearrangeEnvironment, ruleName, MockConfJsonRank++, NRl::RL_UNIVERSE,
                          TRequestParams::EDevice::D_ANY, TRequestParams::EPlatform::PL_ANY, isDbg);
            }
        };
    };

}
