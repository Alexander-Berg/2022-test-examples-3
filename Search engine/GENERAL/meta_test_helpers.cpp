#include "meta_test_helpers.h"

#include <search/grouping/foundstat.h>
#include <search/web/util/source_options/source_options.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/scheme/scheme_cast.h>

namespace NRearrUT {

    TMetaRequestAdjusterStub::TMetaRequestAdjusterStub(TStringBuf cldesc, bool isAux, TMetaSearchContext* ctx, bool isPrimary)
        : Descr(TString{cldesc})
        , Ctx(ctx)
        , IsAux(isAux)
        , IsPrimary(isPrimary)
    {}

    void TMetaRequestAdjusterStub::ClientAppendRelev(TStringBuf k, TStringBuf v) {
        if (!k)
            return;

        TStringStream val;
        val.Str() = Cgi.Get("relev", 0);
        val << ";" << k << "=" << v;
        Cgi.Erase("relev", 0);
        Cgi.InsertUnescaped("relev", val.Str());
    }

    TMetaGroupingId TMetaRequestAdjusterStub::DoRequestGrouping(const TGroupingParams& gp, bool /*forceRequest*/, const ISourceFilter*) {
        Cgi.EraseAll("g");
        Cgi.InsertUnescaped("g", TStringBuilder() << "1." << gp.gAttr << "." << gp.gGroups);
        RequestSent = true;
        return {};
    }

    TMetaGroupingId TMetaRequestAdjusterStub::DoRequestUserGrouping(const TGroupingParams& gp, bool forceRequest) {
        return DoRequestGrouping(gp, forceRequest, nullptr);
    }

    TMetaGrouping* TRearrangeEnvironmentMock::GetAuxGrouping(const TMetaGroupingId& auxiliaryGId, size_t needCount /*= 0*/, const char* baseAttr /*= "d"*/) {
        return GetAuxGroupingImpl(Ctx.Ctx, auxiliaryGId, needCount, baseAttr);
    }

    TDuration TRearrangeEnvironmentMock::GetTimeoutByClient(const TStringBuf clientDescr) const {
        return ::GetTimeoutByClient(&Ctx.Ctx, clientDescr);
    }

    TInstant TRearrangeEnvironmentMock::GetRequestTimestamp() const {
        return Ctx.Ctx.GetRequestTimestamp();
    }

    TMetaSourceAdjusterStub::TMetaSourceAdjusterStub(TCgiParameters& cgi, TStringBuf cldesc, bool isAux, TMetaSearchContext* ctx, bool isPrimary)
        : Cgi(cgi)
        , Descr(TString{cldesc})
        , Ctx(ctx)
        , IsAux(isAux)
        , IsPrimary(isPrimary)
    {}
}
