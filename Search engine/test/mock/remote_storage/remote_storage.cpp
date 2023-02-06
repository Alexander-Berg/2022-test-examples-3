#include "remote_storage.h"

#include <search/base/blob_storage/protos/remote_chunked_blob_storage_request.pb.h>
#include <search/base/blob_storage/protos/remote_chunked_blob_storage_response.pb.h>
#include <search/base/blob_storage/config/protos/remote_chunked_blob_storage_index_config.pb.h>

#include <balancer/server/server.h>

#include <library/cpp/http/client/client.h>
#include <library/cpp/http/client/fetch/fetch_result.h>
#include <library/cpp/iterator/zip.h>
#include <library/cpp/logger/log.h>
#include <library/cpp/testing/common/network.h>

#include <util/generic/hash.h>
#include <util/memory/blob.h>
#include <util/system/mutex.h>
#include <util/system/thread.h>

#include <google/protobuf/text_format.h>

namespace NBlobStorage::NProxy::NMock {

TFsPath SafeMakeDirs(const TFsPath& path) {
    path.Parent().MkDirs();
    return path;
}

class TRemoteStorage::TImpl {
    using TKey = std::tuple<TString, TString, ui8, ui32>;

public:
    TImpl(TRemoteStorageOptions options)
        : Port_{std::move(options.Port)}
        , Root_{std::move(options.Root)}
        , ChunkConfName_{std::move(options.ChunkConfName)}
        , ServerThread_{[this] {
            RunServer();
        }}
        , Log_{SafeMakeDirs(Root_ / "server.log")}
    {
        Log_.SetFormatter([](ELogPriority priority, TStringBuf message) {
            return TStringBuilder{} << Now() << ' ' << priority << ' ' << message;
        });
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
        THashMap<TKey, TBlob> newChunks;
        for (auto [resource, status] : Zip(resources, statuses)) {
            try {
                TFsPath chunkConfPath = resource.RealPath().Child(ChunkConfName_);
                if (!chunkConfPath.Exists()) {
                    status.Valid = false;
                    status.Message = "No chunk.conf found";
                    continue;
                }

                TString chunkConfProto = TFileInput{chunkConfPath}.ReadAll();
                TRemoteBlobStorageChunkConfig chunkConf;
                Y_ENSURE(::google::protobuf::TextFormat::ParseFromString(chunkConfProto, &chunkConf));

                TKey key;
                std::get<0>(key) = chunkConf.GetNamespace();
                std::get<1>(key) = chunkConf.GetStateId();
                std::get<2>(key) = chunkConf.GetItemType();
                std::get<3>(key) = chunkConf.GetId();

                TBlob chunk = TBlob::FromFileContentSingleThreaded(resource.RealPath().Child(chunkConf.GetPath()));

                newChunks[key] = std::move(chunk);
                status.Valid = true;

                Log_ << TLOG_INFO << "Opened chunk " << chunkConf.GetId() << " of item type " << chunkConf.GetItemType() << Endl;
            } catch (const std::exception& e) {
                status.Valid = false;
                status.Message = e.what();
            } catch (...) {
                status.Valid = false;
                status.Message = CurrentExceptionMessage();
            }
        }

        auto guard = Guard(Lock_);
        Chunks_ = std::move(newChunks);
    }

    ui16 Port() const {
        return Port_;
    }

    ui16 AdminPort() const {
        return Port_;
    }

    TFsPath Root() const {
        return Root_;
    }

    TMetrics Metrics() const {
        auto guard = Guard(Lock_);
        return Metrics_;
    }

    void ResetMetrics() {
        auto guard = Guard(Lock_);
        Metrics_ = TMetrics{};
    }

    void Start() {
        Server_.ConstructInPlace([this](NBalancerServer::THttpRequestEnv& env) {
            Log_ << TLOG_INFO << "Accepted request" << Endl;
            const TString& body = env.Body();
            TRemoteChunkedBlobStorageRequest request;
            Y_ENSURE(request.ParseFromString(body));
            Log_ << TLOG_INFO << "Parsed request" << Endl;

            TRemoteChunkedBlobStorageResponse response;
            try {
                response = HandleRequest(request);
            } catch (...) {
                Log_ << TLOG_ERR << "Failed: " << CurrentExceptionMessage() << Endl;
            }

            TString result = response.SerializeAsString();

            auto reply = env.GetReplyTransport();
            NSrvKernel::TResponse head{200, "Ok"};
            head.Props().ContentLength = result.size();

            reply->SendHead(std::move(head));
            reply->SendData(std::move(result));
            reply->SendEof();

            Log_ << TLOG_INFO << "Sent reply: " << response.DebugString() << Endl;

            return NSrvKernel::TError{};
        }, NBalancerServer::TOptions{}.SetPort(Port()));

        ServerThread_.Start();
    }

    void Stop() {
        if (Server_) {
            Server_->Stop();
            ServerThread_.Join();
        }
    }

private:
    void RunServer() {
        Y_VERIFY(Server_);
        Server_->Run();
    }

private:
    TRemoteChunkedBlobStorageResponse HandleRequest(const TRemoteChunkedBlobStorageRequest& req) {
        auto guard = Guard(Lock_);
        ++Metrics_.NumRequests;

        TRemoteChunkedBlobStorageResponse res;

        TKey baseKey;
        std::get<0>(baseKey) = req.GetNamespace();
        std::get<1>(baseKey) = req.GetStateId();

        for (auto&& blob : req.GetBlobs()) {
            auto* resp = res.AddBlobResponses();

            TKey key = baseKey;
            std::get<2>(key) = blob.GetItemType();
            std::get<3>(key) = blob.GetChunk();

            TBlob* chunk = Chunks_.FindPtr(key);
            if (!chunk) {
                Log_ << TLOG_ERR << "Unknown chunk " << blob.GetChunk() << " for item type " << blob.GetItemType() << Endl;
                resp->SetFailed(true);
                ++Metrics_.FailedLoads;
            } else {
                TBlob region = chunk->SubBlob(blob.GetOffset(), blob.GetOffset() + blob.GetSize());
                resp->SetBlob(region.AsCharPtr(), region.Size());
                ++Metrics_.SuccessLoads;
            }
        }

        return res;
    }

private:
    NTesting::TPortHolder Port_;
    TFsPath Root_;
    TString ChunkConfName_;

    TMutex Lock_;
    THashMap<TKey, TBlob> Chunks_;
    TMetrics Metrics_;

    TMaybe<NBalancerServer::TStandaloneServer> Server_;
    TThread ServerThread_;

    TLog Log_;
};

TRemoteStorage::TRemoteStorage(TRemoteStorageOptions options)
    : Impl_{MakeHolder<TImpl>(std::move(options))}
{
}

TRemoteStorage::TRemoteStorage(TRemoteStorage&& rhs) noexcept = default;
TRemoteStorage& TRemoteStorage::operator=(TRemoteStorage&& rhs) noexcept = default;

void TRemoteStorage::Rescan(TConstArrayRef<TResource> resources, TArrayRef<TStatus> statuses) {
    Impl_->Rescan(resources, statuses);
}

ui16 TRemoteStorage::Port() const {
    return Impl_->Port();
}

ui16 TRemoteStorage::AdminPort() const {
    return Impl_->AdminPort();
}

TMetrics TRemoteStorage::Metrics() const {
    return Impl_->Metrics();
}

void TRemoteStorage::ResetMetrics() const {
    return Impl_->ResetMetrics();
}

void TRemoteStorage::Stop() {
    return Impl_->Stop();
}

TFsPath TRemoteStorage::Storage() const {
    return Impl_->Root();
}

TRemoteStorage::~TRemoteStorage() = default;

} // namespace NBlobStorage::NProxy::NMock
