#include "remote_storage_proxy.h"

#include <library/cpp/testing/unittest/registar.h>

#include <library/cpp/http/client/client.h>
#include <library/cpp/http/client/fetch/fetch_result.h>
#include <library/cpp/http/fetch/exthttpcodes.h>
#include <library/cpp/iterator/zip.h>
#include <library/cpp/testing/common/network.h>

#include <google/protobuf/text_format.h>

#include <util/stream/file.h>

namespace NBlobStorage::NProxy::NMock {

class TRemoteStorageProxy::TImpl {
public:
    TImpl(TRemoteStorageProxyOptions options)
        : Port_{std::move(options.Port)}
        , AdminPort_{std::move(options.AdminPort)}
        , Root_{std::move(options.Root)}
        , BaseConfig_{std::move(options.Config)}
        , ServerThread_{[this] {
            RunServer();
        }}
    {
        Start();
    }

    ~TImpl() {
        try {
            Stop();
        } catch (...) {
            Cerr << "FAILED TO STOP SERVICE: " << CurrentExceptionMessage() << Endl;
            Y_VERIFY(false);
        }
    }

    void Rescan(TConstArrayRef<TResource> resources, TArrayRef<TStatus> statuses) {
        NDynamicStorage::TExtendedNotification notification;
        for (const TResource& res : resources) {
            auto* resource = notification.AddResources();

            resource->SetLocalPath(res.LocalPath.GetPath());
            resource->SetNamespace(res.Namespace);
        }

        auto res = MakeAdminRequest("/rescan", notification.SerializeAsString());
        UNIT_ASSERT(res->Success());

        NDynamicStorage::TExtendedStatus status;
        UNIT_ASSERT(::google::protobuf::TextFormat::ParseFromString(res->Data, &status));

        for (auto [resource, target, status] : Zip(status.GetResources(), resources, statuses)) {
            UNIT_ASSERT_VALUES_EQUAL(target.Namespace, resource.GetResource().GetNamespace());
            UNIT_ASSERT_VALUES_EQUAL(target.LocalPath.GetPath(), resource.GetResource().GetLocalPath());

            status.Valid = resource.GetValid();
            status.Message = resource.GetExtended();
        }
    }

    ui16 Port() const {
        return Port_;
    }

    ui16 AdminPort() const {
        return AdminPort_;
    }

    TFsPath Root() const {
        return Root_.Child("storage");
    }

    void Start() {
        ServerThread_.Start();
    }

    void Stop() {
        auto res = MakeAdminRequest("/admin?action=shutdown");
        UNIT_ASSERT(200 <= res->Code && res->Code < 300);
        UNIT_ASSERT_VALUES_EQUAL(res->Data, "Stopping server");

        ServerThread_.Join();
    }

private:
    void RunServer() {
        PrepareConfigs();

        NBlobStorage::NProxy::TConfig config = BaseConfig_;
        config.SetRemoteStorageConfig(RemoteStorageConfigPath().GetPath());
        config.SetPort(Port_);
        config.SetAdminPort(AdminPort_);
        config.SetNotificationCache(Root_ / "notification.cache.txt");
        config.SetStorageDirectory(Root_ / "storage");
        config.SetEventLog(Root_ / "logs" / "event.log");
        config.SetLog(Root_ / "logs" / "server.log");
        (Root_ / "logs").MkDirs();

        NBlobStorage::NProxy::TDaemon daemon{config};
        daemon.Start();
    }

    NHttpFetcher::TResultRef MakeAdminRequest(TStringBuf path, TString body = {}) {
        NHttp::TFetchOptions opts;
        if (body) {
            opts.SetPostData(std::move(body));
        }
        NHttp::TFetchQuery query{TStringBuilder{} << "http://localhost:" << AdminPort() << path, std::move(opts)};
        return NHttp::Fetch(query);
    }

private:
    TFsPath RemoteStorageConfigPath() {
        return Root_.Child("configs").Child("remote_storage.conf");
    }

    void PrepareConfigs() {
        WriteRemoteStorageConfig();
    }

    void WriteRemoteStorageConfig() {
        TFsPath path = RemoteStorageConfigPath();
        path.Parent().MkDirs();

        TFileOutput out{path};
        out.Write(TStringBuf{R"raw_(
BackendConfigs {
  Type: ""
  Codec: Identity
  SourceOptions {
    TaskOptions {
      ConnectTimeouts: "10ms"
      SendingTimeouts: "10ms"
    }
    BalancingOptions {
      MaxAttempts: 2
      AllowDynamicWeights: false
      EnableIpV6: true
      EnableUnresolvedHosts: true
      PingZeroWeight: false
      RandomGroupSelection: true
    }
    HedgedRequestOptions {
      HedgedRequestTimeouts: "1ms"
      HedgedRequestRateThreshold: 0.2
    }
    TimeOut: "10ms"
    AllowConnStat: true
    ConnStatFailThreshold: "10ms"
  }
  ConnStatOptions {
    FailThreshold: 10
    CheckTimeout: "15s"
    CheckInterval: 1000
  }
  EndpointSetOptions {
  }
}
        )raw_"});
    }

private:
    NTesting::TPortHolder Port_;
    NTesting::TPortHolder AdminPort_;

    TFsPath Root_;
    NBlobStorage::NProxy::TConfig BaseConfig_;

    TThread ServerThread_;
};

TRemoteStorageProxy::TRemoteStorageProxy(TRemoteStorageProxyOptions options)
    : Impl_{MakeHolder<TImpl>(std::move(options))}
{
}

void TRemoteStorageProxy::Rescan(TConstArrayRef<TResource> resources, TArrayRef<TStatus> statuses) {
    Impl_->Rescan(resources, statuses);
}

ui16 TRemoteStorageProxy::Port() const {
    return Impl_->Port();
}

ui16 TRemoteStorageProxy::AdminPort() const {
    return Impl_->AdminPort();
}

void TRemoteStorageProxy::Stop() {
    return Impl_->Stop();
}

TFsPath TRemoteStorageProxy::Storage() const {
    return Impl_->Root();
}

TRemoteStorageProxy::~TRemoteStorageProxy() = default;

} // namespace NBlobStorage::NProxy::NMock
