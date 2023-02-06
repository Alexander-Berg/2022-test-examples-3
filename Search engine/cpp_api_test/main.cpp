#include <util/system/env.h>
#include <util/generic/string.h>

#include <library/cpp/getoptpb/getoptpb.h>
#include <search/plutonium/examples/yt_dynamic_tables/cpp_api_test/params.pb.h>

#include <kernel/yt/dynamic/client.h>
#include <yt/yt/core/misc/shutdown.h>

#include <library/cpp/protobuf/util/pb_io.h>
#include <search/plutonium/holder/file_system.h>
#include <search/plutonium/impl/holder/yt_dynamic_tables/file_system_holder.h>
#include <search/plutonium/impl/file_system/yt_dynamic_tables/protos/config.pb.h>
#include <search/base/blob_storage/config/protos/remote_chunked_blob_storage_index_config.pb.h>
#include <search/plutonium/deploy/chunks/proto/chunks.pb.h>


int main(int argc, const char* argv[]) {
    NYtDynamicTablesCppApiTest::TParams params = NGetoptPb::GetoptPbOrAbort(argc, argv);

    try {
        NYT::NApi::IClientPtr fsClient = NYT::NApi::CreateClientWithoutUser("arnold", GetEnv("YT_TOKEN"));

        NPlutonium::NDynTables::TFileSystemConfigProto fsConfig;
        fsConfig.SetContentTablePath("//home/saas2/robot-saas2-worker/testing/dsp_creative/runtime_fs/content");
        fsConfig.SetMetaTablePath("//home/saas2/robot-saas2-worker/testing/dsp_creative/runtime_fs/meta");

        auto fileSystemHolder = MakeHolder<NPlutonium::NDynTables::TFileSystemHolder>(fsClient, params.GetStream(), params.GetSnapshotId(), fsConfig);
        const NPlutonium::IReadOnlyFileSystem* fileSystem = fileSystemHolder->FileSystem();

        TVector<std::pair<TString, TRemoteBlobStorageChunkConfig>> configs;
        NPlutonium::ListFilesRecursively(
            *fileSystem, "",
            [&fileSystem, &configs](const NPlutonium::TFileSystemPath& path){
                if (path.Parts() && path.Parts().back() == "chunk.conf") {
                    TSimpleSharedPtr<IInputStream> inputStreamPtr = fileSystem->ReadFile(path);
                    configs.push_back({path.Parent().ToString(), ParseFromTextFormat<TRemoteBlobStorageChunkConfig>(*inputStreamPtr)});
                }
            }
        );

        for (auto& [dir, config] : configs) {
            Cerr << dir << ": " << config.GetNamespace() << " " << config.GetStateId() << " " << config.GetItemType() << " " << config.GetId() << Endl;
        }

        THashMap<std::pair<ui32, ui32>, NPlutonium::NDeploy::NProto::TPlannerChunkConfig> chunkConfigs;
        if (fileSystem->Exists("planner.conf")) {
            TSimpleSharedPtr<IInputStream> inputStreamPtr = fileSystem->ReadFile("planner.conf");
            NPlutonium::NDeploy::NProto::TPlannerConfig config = ParseFromTextFormat<NPlutonium::NDeploy::NProto::TPlannerConfig>(*inputStreamPtr);
            for (auto chunkConf : config.GetChunks()) {
                chunkConfigs[std::make_pair(chunkConf.GetItemType(), chunkConf.GetId())] = chunkConf;
            }
        }

        for (auto& [key, config] : chunkConfigs) {
            Cerr << "(" << key.first << ";" << key.second << "): " << config.GetItemType() << " " << config.GetId() << " " << config.GetType() << Endl;
        }
    } catch (...) {
        NYT::Shutdown();
        throw;
    }

    NYT::Shutdown();
    return 0;
}
