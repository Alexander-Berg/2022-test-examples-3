#include "service.h"

#include <search/meta/scatter/options.h>

#include <util/datetime/base.h>
#include <util/stream/str.h>
#include <util/string/cast.h>


TTestService::TTestService(const TString& clientDescr, const TString& serverScheme, const TString& sourceScheme, const NScatter::TSourceOptions& opts, size_t backendsCount) {
    Loop = StartService(serverScheme, &Port);
    if (!Loop) {
        ythrow yexception() << "no free port";
    }

    const TString instanceScript = sourceScheme + TString("://localhost:") + ToString(Port) + "/yandsearch";

    TStringOutput script(SourceScript);

    script << instanceScript;
    for (size_t i = 1; i < (backendsCount ? backendsCount : opts.MaxAttempts); ++i) {
        script << " " << instanceScript;
    }

    Source = NScatter::CreateSimpleSource(clientDescr, SourceScript, opts);
}

void TTestService::Stop() {
    if (Loop) {
        Loop->SyncStopFork();
        Loop = nullptr;
    }
}


TTestService::~TTestService() {
    Y_VERIFY(!Loop);
}

NNeh::IServicesRef TTestService::StartService(const TString& scheme, size_t* port) {
    for (size_t p = 15000; p < 15500; ++p) {
        TStringStream ss;
        ss << scheme << "://*:" << p << "/yandsearch";

        TStringStream ssM;
        ssM << "udp://*:" << p << "/yandsearch/msg";

        try {
            NNeh::IServicesRef res = NNeh::CreateLoop();
            res->Add(ss.Str(), *this);

            if (scheme != "udp") {
                res->Add(ssM.Str(), [this](const NNeh::IRequestRef& req) {
                    this->DoServeMessage(req);
                });
            }

            res->ForkLoop(8);
            *port = p;
            Sleep(TDuration::MilliSeconds(100));
            return res;
        } catch (...) {
            continue;
        }
    }

    return nullptr;
}

void TTestService::ServeRequest(const NNeh::IRequestRef& req) {
    with_lock (Mutex_) {
        Requests.push_back(ToString(req->Data()));
    }
    DoServeRequest(req);

}
