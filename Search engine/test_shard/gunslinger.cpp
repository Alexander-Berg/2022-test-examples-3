#include "gunslinger.h"

#include <search/tools/test_shard/common/zhash.h>
#include <search/tools/test_shard/common/attribute_tree.h>

#include <search/idl/meta.pb.h>

#include <library/cpp/neh/multiclient.h>

#include <util/generic/utility.h>
#include <util/string/vector.h>
#include <util/system/info.h>
#include <util/system/thread.h>

namespace NTestShard {

TGunslinger::TGunslinger(TOptions& opts)
    : Address_("http://" + opts.Host + ":" + ToString(opts.Port))
    , MaxRetries_(10)
    , Repeats_(opts.ShootingRepeats)
{
    LoadQueries(opts);
    MaxThreads_ = ClampVal<ui32>(opts.MaxThreads, 1, NSystemInfo::NumberOfCpus() - 1);
    Requesters_ = CreateThreadPool(MaxThreads_);
}

void TGunslinger::Start() {
    AtomicSet(Progress_, 0);
    auto updater = [this]() -> NProgressBar::TProgress<ui64> {
        ui64 progress = AtomicGet(Progress_);
        ui64 total = Queries_.size() * Repeats_;
        return { progress, total };
    };
    Bar_ = MakeHolder<NProgressBar::TProgressBar<ui64>>(Cerr, "Processing queries...", std::move(updater));

    Requesters_->Start(MaxThreads_);
    Responses_.resize(Queries_.size());
    for (ui32 i = 0; i < MaxThreads_; ++i) {
        ui32 l = Queries_.size() * i / MaxThreads_;
        ui32 r = Min(Queries_.size() * (i + 1) / MaxThreads_, Queries_.size());
        Requesters_->SafeAddFunc([this, l, r](){
            for (ui32 j = 0; j < this->Repeats_; ++j) {
                Worker(l, r);
            }
        });
    }
}

void TGunslinger::Join() {
    Requesters_->Stop();
    Bar_->Stop();
}

void TGunslinger::Validate() {
    ui32 fails = 0;
    ui32 oks = 0;
    size_t i = 0;
    for (const TResponse& responce : Responses_) {
        THashSet<TDocHash> docHashes;
        THashMap<TString, TString> filteringStatus;
        for (const auto& grouping : responce.Report.GetGrouping()) {
            for (const auto& group : grouping.GetGroup()) {
                for (const auto& doc : group.GetDocument()) {
                    docHashes.insert(doc.GetDocHash());
                }
            }
        }
        for (const auto& prop : responce.Report.GetSearcherProp()) {
            if (prop.GetKey() == "QLossBase.debug") {
                TVector<TString> splitted = SplitString(prop.GetValue(), "\t");
                if (splitted.size() >= 2) {
                    filteringStatus[splitted[0]] = splitted[1];
                }
            }
        }
        ui32 notFound = 0;
        ui32 found = 0;
        TDocHash firstLostHash = 0;
        for (auto& doc : Queries_[i].GetExpectedDocs()) {
            if (!docHashes.contains(doc.GetHash())) {
                ++notFound;
                firstLostHash = doc.GetHash();
            } else {
                ++found;
            }
        }
        if (notFound == 0) {
            ++oks;
        } else {
            TAttrSchemeTree query(Queries_[i].GetQuery());
            TString zhash = GetDocZHash(firstLostHash);
            Cout << "Found " << found << " / " << found + notFound << " documents" << Endl
                 << "query: " << query.Serialize() << Endl
                 << "reported docs size: " << docHashes.size() << Endl
                 << "filtering status: " << filteringStatus[zhash] << Endl
                 << "doc info:\n" << GetDocInfo(zhash) << Endl;

            ++fails;
        }
        ++i;
    }
    INFO_LOG << "Passed " << oks << " / " << oks + fails << Endl;
}

void TGunslinger::MutateQueries() {

}

void TGunslinger::PrintTimes() {
    TVector<ui32> idx(Responses_.size());
    Iota(idx.begin(), idx.end(), 0);
    Sort(idx.begin(), idx.end(), [&](ui32 lhs, ui32 rhs) {
        const TResponse& l = Responses_[lhs];
        const TResponse& r = Responses_[rhs];
        return l.SummaryDuration * r.SuccessRequests > r.SummaryDuration * l.SuccessRequests;
    });
    idx.resize(20);
    for (const ui32 i : idx) {
        if (Responses_[i].SuccessRequests == 0) {
            Cout << "Error: ";
        } else {
            Cout << Responses_[i].SummaryDuration / (1e6 * Responses_[i].SuccessRequests) << "s ";
        }
        TAttrSchemeTree query(Queries_[i].GetQuery());
        Cout << query.Serialize() << Endl;
    }
}

void TGunslinger::LoadQueries(TOptions& opts) {
    TQueryVector queries;
    if (opts.QueriesFile.empty() || opts.QueriesFile == "none") {
        if (!Generator_) {
            Generator_ = MakeHolder<TRequestsGenerator>(opts);
        }
        queries = Generator_->Generate(opts);
    } else {
        TFileInput in(opts.QueriesFile);
        if (!queries.ParseFromArcadiaStream(&in)) {
            FATAL_LOG << "Cannot parse queries file" << Endl;
        }
    }
    LoadQueries(queries);
}

void TGunslinger::LoadQueries(const TQueryVector& queries) {
    Queries_.resize(queries.QuerySize());
    Copy(queries.GetQuery().begin(), queries.GetQuery().end(), Queries_.begin());
    for (TQuery& query : Queries_) {
        TStringBuilder builder;
        builder << query.GetRequest();
        for (const NProto::TDoc& doc : query.GetExpectedDocs()) {
            builder << "&pron=doc_filtering_status_" << GetDocZHash(doc.GetHash());
        }
        query.SetRequest(builder);
    }
}

void TGunslinger::Worker(ui32 begin, ui32 end) {
    for (ui64 i = begin; i < end; ++i) {
        TString lastError;
        for (ui32 retry = 0; retry < MaxRetries_; ++retry) {
            if (Queries_[i].GetRequest().empty()) {
                break;
            }
            try {
                NNeh::TResponseRef resp = NNeh::Request(Address_ + Queries_[i].GetRequest())->Wait(TDuration::Seconds(3));
                if (!resp) {
                    lastError = "Timeout";
                } else if (resp->IsError()) {
                    lastError = "Error: " + resp->GetErrorText();
                } else {
                    ui32 microSeconds = resp->Duration.MicroSeconds();
                    if (Responses_[i].Report.ParseFromString(resp->Data)) {
                        NMetaProtocol::Decompress(Responses_[i].Report);

                        if (Responses_[i].Report.HasBalancingInfo()) {
                            microSeconds = Responses_[i].Report.GetBalancingInfo().GetElapsed();
                        } else {
                            WARNING_LOG << "Basesearch report does not contain balancing info" << Endl;
                        }
                    } else {
                        WARNING_LOG << "Cannot parse basesearch report" << Endl;
                    }
                    Responses_[i].SummaryDuration += microSeconds;
                    ++Responses_[i].SuccessRequests;
                    lastError.clear();
                    break;
                }
            } catch (...) {
                lastError = CurrentExceptionMessage();
            }
        }
        if (!lastError.empty()) {
            WARNING_LOG << "No retries left; last error: " << lastError << Endl;
            INFO_LOG << Queries_[i].GetInfo().GetQid() << Endl;
        }
        AtomicAdd(Progress_, 1);
    }
}

TString TGunslinger::GetDocInfo(const TString& zhash) {
    try {
        TStringBuilder request;
        request << Address_ << "/yandsearch?info=infobydocid:" << zhash;
        NNeh::TResponseRef resp = NNeh::Request(request)->Wait(TDuration::Seconds(3));
        if (!resp) {
            return "Cannot get doc info: timeout";
        } else if (resp->IsError()) {
            return "Cannot get doc info: " + resp->GetErrorText();
        } else {
            return resp->Data;
        }
    } catch (...) {
        return "Cannot get doc info: " + CurrentExceptionMessage();
    }
}

}
