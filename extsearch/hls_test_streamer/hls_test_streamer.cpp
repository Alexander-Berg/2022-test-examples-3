#include <extsearch/audio/generative/cpp/backend/library/handler/base/httphandlersregistry.h>
#include <extsearch/audio/generative/cpp/backend/library/sessionmanager/sessionmanager.h>

#include <library/cpp/http/coro/server.h>
#include <library/cpp/logger/stream.h>
#include <library/cpp/cgiparam/cgiparam.h>

using namespace NGenerative;

void WriteResp(THttpOutput& out, TStringBuf contentType, TStringBuf respBody, HttpCodes code = HTTP_OK,
               TVector<std::pair<TString, TString>> extraHeaders = {}) {
    THttpResponse resp(code);
    resp.AddHeader("Content-Type", contentType);
    for (auto& hdr: extraHeaders) {
        resp.AddHeader(hdr.first, hdr.second);
    }
    out << resp << respBody;
}

class TFakeRecommender : public IRecommenderApi {
public:
    NProto::TGetGenerativeRadioOutput
    GetGenerativeRadio(TStringBuf reqId, const NProto::TGetGenerativeRadioInput& input) const override {
        Y_UNUSED(reqId);
        Y_UNUSED(input);
        NProto::TGetGenerativeRadioOutput rv;
        rv.AddGenerativeTracks(1);
        rv.AddGenerativeTracks(1);
        rv.AddGenerativeTracks(1);
        rv.AddGenerativeTracks(1);
        rv.AddGenerativeTracks(1);
        rv.AddGenerativeTracks(1);
        rv.AddGenerativeTracks(1);
        rv.AddGenerativeTracks(1);
        rv.AddGenerativeTracks(1);
        rv.AddGenerativeTracks(1);
        return rv;
    }

    TVector<TString> GetFavoriteGenres(int64_t uid) const override {
        Y_UNUSED(uid);
        return {};
    }
};

class TFakeUserFeedback : public IUserFeedback {
public:
    TVector<TFeedbackEvent> FindUserEvents(uint64_t yandexUid) override {
        Y_UNUSED(yandexUid);
        return {};
    }

    TVector<TFeedbackEvent> FindSessionUserEvents(uint64_t yandexUid, const TString& sessionId) override {
        Y_UNUSED(yandexUid);
        Y_UNUSED(sessionId);
        return {};
    }

    void InsertEvent(const TFeedbackEvent& feedback) override {
        Y_UNUSED(feedback);
    }

    void InsertEventAsync(TFeedbackEvent&& feedback) override {
        Y_UNUSED(feedback);
    }

    void UpdateEvent(const TFeedbackEvent& feedback) override {
        Y_UNUSED(feedback);
    }

    void UpdateEventAsync(TFeedbackEvent&& feedback) override {
        Y_UNUSED(feedback);
    }
};

class TFakeBlobStorage : public IBlobStorage {
    void SaveBlob(const TString& sesionId, const TString& blob) override {
        Y_UNUSED(sesionId);
        Y_UNUSED(blob);
    }

    TString GetBlob(const TString& sessionId) override {
        Y_UNUSED(sessionId);
        return {};
    }

    void UpdateBlobTimestamp(const TString& sessionId) override {
        Y_UNUSED(sessionId);
    }
};

int main() {
    try {
        NCoroHttp::THttpServer srv;

        NCoroHttp::THttpServer::TConfig config;
        config.Port = 8880;
        NCoroHttp::THttpServer::TCallbacks cb;

        auto notFound = [](THttpRequestContext& ctx) {
            WriteResp(*ctx.Output, "text/plain", "Not found", HTTP_NOT_FOUND);
        };
        auto matchFail = [](THttpRequestContext& ctx) {
            WriteResp(*ctx.Output, "text/plain", "Invalid request", HTTP_BAD_REQUEST);
        };

        auto log = MakeHolder<TLog>(MakeHolder<TStreamLogBackend>(&Cout));

        TAudioConfig acfg;
        TBackendConfig bcfg;
        TPregeneratedIndex pindex(log.Get());
        NProto::TPregenIndexRecord rec;
        rec.SetId(1);
        rec.SetCrossbred(false);
        rec.SetProject("test");
        rec.SetGenre("rock");
        rec.SetTemperature(1);
        rec.SetMDSKey("16230/generative/preroll/aac_track.mp4");
        rec.SetMDSKeyFo("16230/generative/preroll/aac_track.mp4");
        NProto::TTrackPlan& plan = *rec.MutableTrackPlan();
        plan.SetDurationFrames(18630486);
        plan.SetAlignedDurationFrames(18630486);
        plan.SetGenre(NProto::TParameters_EGenre_Rock);
        plan.SetTemperature(1);
        TLoopInfoIndex lindex;
        pindex.AddToIndex(rec, lindex);
        TCrossbredExclusions excl;
        TFakeRecommender fakeRecommender;
        TSessionManager mngr(log.Get(), pindex, lindex, excl, fakeRecommender, acfg, bcfg);
        TString sid = "hello";
        TString reqId = "world";
        NProto::TGetStreamV2Input input;
        TVector<std::pair<TMetricsListener::EExtraTiming, TDuration>> timings;
        TFakeUserFeedback fakeUserFeedback;
        TFakeBlobStorage fakeBlobStorage;
        TTrackPlanGenerator gen(log.Get(), lindex, excl, acfg);
        auto session = mngr.BuildSession(sid, reqId, TInstant::Now(), input, timings, &fakeUserFeedback,
                                         &fakeBlobStorage);
        session.MutableUserInfo()->SetUser(1);

        auto playlist = [&](THttpRequestContext& ctx) {
            for(;;) {
                TGenerativeQueue queue(log.Get(), sid, NProto::TGenerativeSession(session), pindex, gen, bcfg, acfg);
                auto ts = TInstant::Now();
                auto updateTs = ts + TDuration::Seconds(queue.GetAudioConfig().PlaylistUpdateThresholdSec);
                if (queue.IsLastItemAt(updateTs)) {
                    TGenericLogger(log.Get()).Info() << "Update session";
                    mngr.AddGaplessContinuation(sid, session, ts, &fakeUserFeedback, &fakeBlobStorage);
                    continue;
                }

                TString vsid;
                if (ctx.Req.Cgi) {
                    vsid = TQuickCgiParam(ctx.Req.Cgi).Get("vsid");
                }
                TVector<std::pair<TString, TString>> h = {
                    {"Access-Control-Allow-Origin", "*"}
                };
                auto pl = queue.GetPlaylist(TInstant::Now(), vsid);
                WriteResp(*ctx.Output, "audio/mpegurl", pl.Render());
                break;
            }
        };

        THttpHandlersRegistry reg(notFound, matchFail);
        reg.RegisterHandler("/playlist", playlist);

        cb.OnRequestCb = [&](NCoroHttp::THttpServer::TRequestContext& ctx) {
            reg.HandleRequest(ctx.Input, ctx.Output);
        };
        srv.RunCycle(config, cb);
    } catch (...) {
        Cerr << "Exception:" << CurrentExceptionMessage() << Endl;
    }
}
